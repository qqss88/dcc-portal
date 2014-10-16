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
// TODO: Should add additional icons: upload, warning
// TODO: Extend LocationService to filter out extra stuff like genelist
// TODO: Clean up

(function () {
  'use strict';

  angular.module('icgc.genelist.controllers', []);

  angular.module('icgc.genelist.controllers')
    .controller('genelistController', function($scope, $timeout, $http, Restangular, LocationService) {
    var verifyPromise = null;
    var delay = 1000;

    function verify() {
      $scope.state = 'verifying';


      // TODO: remove, just keeping it around because this works
      // $http.post('http://localhost:8080/api/v1/ui/identifier/genes', 'geneIdentifiers=' + $scope.rawText, { transformRequest: angular.identity }).then(function(x) {
      //   $scope.verifying = false;
      //   $scope.verified = true;
      //   console.log('x', x);
      // });

      var data = 'geneIdentifiers=' + encodeURI($scope.rawText);
      Restangular.one('genelist').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data, 'validate').then(function(result) {

          result = Restangular.stripRestangular(result);
          $scope.state = 'verified';
          $scope.validIds = [];
          $scope.invalidIds = [];

          angular.forEach(result, function(value, key) {
            if (!value || value.length === 0) {
              $scope.invalidIds.push( key );
            } else {
              $scope.validIds = $scope.validIds.concat(value);
            }
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
      var data = 'geneIds=' + encodeURI($scope.validIds.join(','));
      console.log("sending ", data);
      Restangular.one('genelist').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data).then(function(result) {
          var filters = LocationService.filters();
          console.log("Gene list id is", result);

          if (! filters.hasOwnProperty('gene')) {
            filters.gene = {};
          }
          $scope.genelistModal = false;
          filters.gene.uploadedGeneList = [result.id];
          LocationService.setFilters(filters);
        });
    }


    $scope.rawText = '';
    $scope.state = '';

    $scope.newGeneList = function() {
      createNewGeneList();
    };

    // This may be a bit brittle, angularJS as of 1.2x does not seem to have any native/clean 
    // way of modeling [input type=file]. So to get file information, it is proxied through a 
    // directive that gets the file value (myFile) from input $element
    $scope.$watch('myFile', function(newValue, oldValue) {
      if (! newValue) {
        return;
      }
      verifyFile();
    }, true);


    $scope.updateGenelist = function() {
      // If content was from file, clear out the filename
      $scope.fileName = null;

      $timeout.cancel(verifyPromise);
      verifyPromise = $timeout(verify, delay, true);
    };

    $scope.reset = function() {
      $scope.state = '';
      $scope.fileName = null;
      $scope.rawText = '';
      $scope.validIds = [];
      $scope.invalidIds = [];
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
      templateUrl: '/scripts/genelist/views/upload.html',
      controller: 'genelistController'
    };
  });
})();

