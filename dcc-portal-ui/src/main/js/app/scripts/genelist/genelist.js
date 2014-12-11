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
          $scope.hasType = {};


          var uiResult = {}, uniqueEnsembl = {}, totalInput = 0;

          angular.forEach(verifyResult.validGenes, function(type, typeName) {
            angular.forEach(type, function(geneList, inputToken) {
              if (geneList && geneList.length > 0) {

                geneList.forEach(function(gene) {
                  var symbol = gene.symbol, row;

                  // Initialize row structure
                  if (! uiResult.hasOwnProperty(symbol)) {
                    uiResult[symbol] = {};
                  }
                  row = uiResult[symbol];

                  // Aggregate input ids that match to the same symbol
                  if (! row.hasOwnProperty(typeName)) {
                    row[typeName] = [];
                  }
                  if (row[typeName].indexOf(inputToken) === -1) {
                    row[typeName].push(inputToken);

                    // Mark it for visibility test on the view
                    $scope.hasType[typeName] = 1;
                  }

                  // Aggregate matched ensembl ids that match to the same symbol
                  if (! row.hasOwnProperty('matchedId')) {
                    row.matchedId = [];
                  }
                  if (row.matchedId.indexOf(gene.id) === -1) {
                    row.matchedId.push(gene.id);
                    $scope.validIds.push(gene.id);
                  }

                  // Total counts
                  uniqueEnsembl[gene.id] = 1;
                });
                totalInput ++;
              }
            });
          });
          $scope.uiResult = uiResult;
          $scope.totalInput = totalInput;
          $scope.totalMatch = Object.keys(uniqueEnsembl).length;
          $scope.totalColumns = Object.keys($scope.hasType).length;
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
      var data;
      data = 'geneIds=' + encodeURI($scope.rawText);

      Restangular.one('genelists').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data).then(function(result) {
          var filters = LocationService.filters(), search = LocationService.search();

          if (! filters.hasOwnProperty('gene')) {
            filters.gene = {};
          }
          if (! filters.gene.hasOwnProperty('inputGeneListId')) {
            filters.gene.inputGeneListId = {};
          }

          $scope.genelistModal = false;
          filters.gene.inputGeneListId.is = [result.geneListId];

          // Upload gene list redirects to gene tab, regardless of where we came from
          search.filters = angular.toJson(filters);
          $location.path('/search/g').search(search);
        });
    }

    function remove() {
      var filters = FiltersUtil.removeExtensions(LocationService.filters());
      LocationService.setFilters(filters);
    }

  
    // Initialize
    $scope.rawText = '';
    $scope.state = '';
    $scope.hasGeneList = false;
    $scope.typeNameMap = {
      'symbol': 'Gene Symbol',
      '_gene_id': 'Ensembl ID',
      'external_db_ids.uniprotkb_swissprot': 'UniProtKB/Swiss-Prot ID'
    };


    $scope.$watch(function () { return LocationService.search(); }, function() {
      var filters = LocationService.filters();
      $scope.hasGeneList = false;
      if (filters.hasOwnProperty('gene')) {
        if (filters.gene.hasOwnProperty('inputGeneListId')) {
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

    $scope.toggleGeneType = function(type) {
      if ($scope.validIds.length > 1) {
        type.selected = !type.selected;
      }
    };

    $scope.hasGeneTypeSelected = function() {
      var selected = _.filter($scope.validIds, function(type) {
        return type.selected === true;
      });
      return selected.length > 0;
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

