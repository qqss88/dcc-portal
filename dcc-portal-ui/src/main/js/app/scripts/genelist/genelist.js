/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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
  angular.module('icgc.genelist', [
    'icgc.genelist.controllers'
  ]);
})();


// TODO: Probably want a service
(function () {
  'use strict';

  angular.module('icgc.genelist.controllers', []);

  angular.module('icgc.genelist.controllers').controller('GeneListController',
    function($scope, $timeout, $location, $modalInstance, Restangular, LocationService, FiltersUtil, Extensions) {

    var verifyPromise = null;
    var delay = 1000;

    // Input params
    $scope.params = {};
    $scope.params.rawText = '';
    $scope.params.state = '';
    $scope.params.myFile = null;
    $scope.params.fileName = '';

    // Output
    $scope.out = {};
    $scope.out.validIds = [];
    $scope.out.invalidIds = [];
    $scope.out.warnings = [];
    $scope.out.hasType = {};
    $scope.out.uiResult = {};
    $scope.out.totalInput = 0;
    $scope.out.totalMatch = 0;
    $scope.out.totalColumns = 0;


    function verify() {
      $scope.params.state = 'verifying';

      var data = 'geneIds=' + encodeURI($scope.params.rawText);

      Restangular.one('genelists').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data, undefined, {'validationOnly':true}).then(function(result) {

          var verifyResult = Restangular.stripRestangular(result);
          $scope.params.state = 'verified';
          $scope.out.validIds = [];
          $scope.out.invalidIds = [];
          $scope.out.warnings = [];


          if (verifyResult.warnings) {
            verifyResult.warnings.forEach(function(msg) {
              $scope.out.warnings.push(msg);
            });
          }
          $scope.out.invalidIds = verifyResult.invalidGenes;
          $scope.out.hasType = {};


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
                    $scope.out.hasType[typeName] = 1;
                  }

                  // Aggregate matched ensembl ids that match to the same symbol
                  if (! row.hasOwnProperty('matchedId')) {
                    row.matchedId = [];
                  }
                  if (row.matchedId.indexOf(gene.id) === -1) {
                    row.matchedId.push(gene.id);
                    $scope.out.validIds.push(gene.id);
                  }

                  // Total counts
                  uniqueEnsembl[gene.id] = 1;
                });
                totalInput ++;
              }
            });
          });
          $scope.out.uiResult = uiResult;
          $scope.out.totalInput = totalInput;
          $scope.out.totalMatch = Object.keys(uniqueEnsembl).length;
          $scope.out.totalColumns = Object.keys($scope.out.hasType).length;
        });
    }

    function verifyFile() {
      // Update UI
      $scope.params.state = 'uploading';
      $scope.params.fileName = $scope.params.myFile.name;

      // The $timeout is just to give sufficent time in order to convey system state
      $timeout(function() {
        var data = new FormData();
        data.append('filepath', $scope.params.myFile);
        Restangular.one('ui').withHttpConfig({transformRequest: angular.identity})
          .customPOST(data, 'file', {}, {'Content-Type': undefined}).then(function(result) {
            $scope.params.rawText = result.data;
            verify();
          });
      }, 1000);
    }

    function createNewGeneList() {
      var data;
      data = 'geneIds=' + encodeURI($scope.params.rawText);

      Restangular.one('genelists').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data).then(function(result) {
          var filters = LocationService.filters(), search = LocationService.search();

          if (! filters.hasOwnProperty('gene')) {
            filters.gene = {};
          }
          if (! filters.gene.hasOwnProperty(Extensions.ENTITY)) {
            filters.gene[Extensions.ENTITY] = {};
          }

          filters.gene[Extensions.ENTITY].is = [result.geneListId];

          // Upload gene list redirects to gene tab, regardless of where we came from
          search.filters = angular.toJson(filters);
          $location.path('/search/g').search(search);
        });
    }

    $scope.newGeneList = function() {
      createNewGeneList();
      $modalInstance.dismiss('cancel');
    };

    // This may be a bit brittle, angularJS as of 1.2x does not seem to have any native/clean
    // way of modeling [input type=file]. So to get file information, it is proxied through a
    // directive that gets the file value (myFile) from input $element
    $scope.$watch('params.myFile', function(newValue) {
      if (! newValue) {
        return;
      }
      verifyFile();
    }, true);


    $scope.updateGenelist = function() {
      // If content was from file, clear out the filename
      $scope.params.fileName = null;
      if ($scope.params.myFile) {
        $scope.params.myFile = null;
      }

      $timeout.cancel(verifyPromise);
      verifyPromise = $timeout(verify, delay, true);
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.reset = function() {
      $scope.params.state = '';
      $scope.params.fileName = null;
      $scope.params.rawText = '';
      $scope.out.validIds = [];
      $scope.out.invalidIds = [];
      if ($scope.params.myFile) {
        $scope.params.myFile = null;
      }
    };

    $scope.$on('$destroy', function() {
      if (verifyPromise) {
        $timeout.cancel(verifyPromise);
      }
      if ($scope.myFile) {
        $scope.myFile = null;
      }

      $scope.params.rawText = null;
      $scope.invalidIds = null;
      $scope.validIds = null;
    });

  });
})();

