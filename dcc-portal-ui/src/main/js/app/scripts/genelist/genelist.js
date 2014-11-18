/*
 * Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


(function () {
  'use strict';
  angular.module('icgc.genelist', ['icgc.genelist.controllers', 'icgc.genelist.directives']);
})();


// TODO: Probably want a service
(function () {
  'use strict';

  angular.module('icgc.genelist.controllers', []);

  angular.module('icgc.genelist.controllers').controller('genelistController',
    function($scope, $timeout, $http, $location, Restangular, LocationService, FiltersUtil) {

    var verifyPromise = null;
    var delay = 1000;

    function verify() {
      $scope.state = 'verifying';

      var data = 'geneIds=' + encodeURI($scope.rawText);
      Restangular.one('genelists').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data, undefined, {'validationOnly':true}).then(function(result) {

          var verifyResult = Restangular.stripRestangular(result);
          $scope.state = 'verified';
          $scope.validIds = [];
          $scope.invalidIds = [];
          $scope.warnings = [];

          if (verifyResult.warnings) {
            verifyResult.warnings.forEach(function(msg) {
              $scope.warnings.push(msg);
            });
          }

          $scope.invalidIds = verifyResult.invalidGenes;
          $scope.totalMatches = 0;

          // Iterate over: search type -> search keys -> geneIds
          angular.forEach(verifyResult.validGenes, function(type, typeName) {
            var keys = [], idCount = 0, geneIdMap = {};
            if (_.isEmpty(type)) {
              return;
            }
            angular.forEach(type, function(list, idKey) {
              if (list && list.length > 0) {
                list.forEach(function(geneId) {
                  if (! geneIdMap[geneId]) {
                    idCount++;
                    geneIdMap[geneId] = 1;
                  }
                });
                $scope.totalMatches ++;
                keys.push(idKey);
              }
            });

            $scope.validIds.push({
              typeName: typeName,
              keys: keys,
              idCount: idCount,
              selected: true,
            });
          });
        });
    }

    function verifyFile() {
      // Update UI
      $scope.state = 'uploading';
      $scope.fileName = $scope.myFile.name;

      // The $timeout is just to give sufficent time in order to convey system state
      $timeout(function() {
        var data = new FormData();
        data.append('filepath', $scope.myFile);
        Restangular.one('ui').withHttpConfig({transformRequest: angular.identity})
          .customPOST(data, 'file', {}, {'Content-Type': undefined}).then(function(result) {
            $scope.rawText = result.data;
            verify();
          });
      }, 1000);
    }

    function createNewGeneList() {
      var data, text = '';
      $scope.validIds.forEach(function(validType, idx) {
        if (validType.selected === true) {
          text += validType.keys.join(',');
          if (idx < ($scope.validIds.length - 1)) {
            text += ',';
          }
        }
      });
      data = 'geneIds=' + encodeURI(text);

      Restangular.one('genelists').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data).then(function(result) {
          var filters = LocationService.filters();

          if (! filters.hasOwnProperty('gene')) {
            filters.gene = {};
          }
          if (! filters.gene.hasOwnProperty('uploadedGeneList')) {
            filters.gene.uploadedGeneList = {};
          }

          $scope.genelistModal = false;
          filters.gene.uploadedGeneList.is = [result.geneListId];

          $location.path('/search/g').search({'filters': angular.toJson(filters)});
          // LocationService.setFilters(filters);
        });
    }

    function remove() {
      var filters = FiltersUtil.removeExtensions(LocationService.filters());
      LocationService.setFilters(filters);
    }

  
    // Init
    $scope.rawText = '';
    $scope.state = '';
    $scope.hasGeneList = false;


    $scope.$watch(function () { return LocationService.search(); }, function() {
      var filters = LocationService.filters();
      $scope.hasGeneList = false;
      if (filters.hasOwnProperty('gene')) {
        if (filters.gene.hasOwnProperty('uploadedGeneList')) {
          $scope.hasGeneList = true;
        }
      }
    }, true);

    $scope.remove = function() {
      remove();
    };

    $scope.newGeneList = function() {
      createNewGeneList();
    };

    // This may be a bit brittle, angularJS as of 1.2x does not seem to have any native/clean 
    // way of modeling [input type=file]. So to get file information, it is proxied through a 
    // directive that gets the file value (myFile) from input $element
    $scope.$watch('myFile', function(newValue) {
      if (! newValue) {
        return;
      }
      verifyFile();
    }, true);


    $scope.updateGenelist = function() {
      // If content was from file, clear out the filename
      $scope.fileName = null;
      if ($scope.myFile) {
        $scope.myFile = null;
      }

      $timeout.cancel(verifyPromise);
      verifyPromise = $timeout(verify, delay, true);
    };

    $scope.reset = function() {
      $scope.state = '';
      $scope.fileName = null;
      $scope.rawText = '';
      $scope.validIds = [];
      $scope.invalidIds = [];
      if ($scope.myFile) {
        $scope.myFile = null;
      }
    };

    $scope.$on('$destroy', function() {
      if (verifyPromise) {
        $timeout.cancel(verifyPromise);
      }
      if ($scope.myFile) {
        $scope.myFile = null;
      }

      $scope.rawText = null;
      $scope.invalidIds = null;
      $scope.validIds = null;
    });

  });
})();


(function () {
  'use strict';

  angular.module('icgc.genelist.directives', ['icgc.genelist.controllers']);

  angular.module('icgc.genelist.directives').directive('uploadGenelist', function () {
    return {
      restrict: 'E',
      scope: {
        collapsed: '='
      },
      templateUrl: '/scripts/genelist/views/upload.html',
      controller: 'genelistController'
    };
  });
})();

