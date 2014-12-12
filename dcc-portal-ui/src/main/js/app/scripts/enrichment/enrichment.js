(function () {
  'use strict';

  angular.module('icgc.enrichment', ['icgc.enrichment.directives']);
})();



(function () {
  'use strict';

  angular.module('icgc.enrichment.directives', []);

  angular.module('icgc.enrichment.directives').directive('enrichmentUpload',
    function (Extensions, Restangular, LocationService, $location) {

    return {
      restrict: 'E',
      scope: {
        enrichmentModal: '=',
        total: '@'
      },
      templateUrl: '/scripts/enrichment/views/enrichment.upload.html',
      link: function($scope) {

        $scope.hasValidParams = false;
        $scope.Extensions = Extensions;

        // Default values
        $scope.analysisParams = {
          maxGeneSetCount: 50,
          fdr: 0.05,
          maxGeneCount: $scope.total || 1000
        };


        $scope.newGeneSetEnrichment = function() {
          var promise, data;

          // FIXME: need real values
          data = 'params=' + JSON.stringify($scope.analysisParams) + '&' +
            'sort=affectedDonorCountFiltered' + '&' +
            'order=ASC' + '&' +
            'filters=' + JSON.stringify(LocationService.filters());

          console.log('payload', data);

          promise = Restangular.one('analysis')
            .withHttpConfig({transformRequest: angular.identity})
            .customPOST(data, 'enrichment');

          // Send and forget, we really just need to get the analysis id
          // to start the redirection
          promise.then(function(result) {
            var id = result.id;
            $scope.enrichmentModal = false;
            $location.path('/analysis/' + id).search({});
          });
        };

        $scope.checkInput = function() {
          var params = $scope.analysisParams;

          if ($scope.hasValidGeneCount(parseInt(params.maxGeneCount, 10)) === false ||
            $scope.hasValidFDR(parseFloat(params.fdr)) === false ||
            angular.isDefined(params.universe) === false) {
            $scope.hasValidParams = false;
          } else {
            $scope.hasValidParams = true;
          }
        };

        $scope.hasValidFDR = function(val) {
          if (angular.isNumber(val) === false) {
            return false;
          }
          if (val >= 0.005 && val <= 0.5) {
            return true;
          }
          return false;
        };

        $scope.hasValidGeneCount = function(val) {
          if (angular.isNumber(val) === false ||
              val > $scope.total) {
            return false;
          }
          return true;
        };

        $scope.$watch('total', function(n) {
          if (n) {
            $scope.total = Math.min(n, 1000);
            $scope.analysisParams.maxGeneCount = n;
          }
        });

      }
    };
  });


  angular.module('icgc.enrichment.directives').directive('enrichmentResult', function (Restangular, ExportService) {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/enrichment/views/enrichment.result.html',
      link: function($scope) {
        $scope.predicate = 'adjustedPValue';
        $scope.reverse = false;

        function refresh() {
          console.log('enrichment refreshing', $scope.item);

          var enrichment = $scope.item;
          var id = enrichment.id;
          var universe = enrichment.params.universe;

          // No results yet, can't do anything
          if (! enrichment.results) {
            return;
          }


          // Compute queries to go to advanced search page
          // 1) Add inputGeneList
          // 2) Add genesetId
          enrichment.results.forEach(function(row) {
            var geneFilter, geneSetIdFilter, geneInputGeneListIdFilter;
            var baseFilter = angular.copy(enrichment.query.filters);

            if (! baseFilter.gene) {
              baseFilter.gene = {};
            }

            geneFilter = baseFilter.gene;
            if (! geneFilter.geneSet) {
              geneFilter.geneSetId = {};
            }
            if (! geneFilter.inputGeneListId) {
              geneFilter.inputGeneListId = {};
            }

            geneSetIdFilter = geneFilter.geneSetId;
            if (! geneSetIdFilter.all) {
              geneSetIdFilter.all = [];
            }

            geneInputGeneListIdFilter = geneFilter.inputGeneListId;
            if (! geneInputGeneListIdFilter.is) {
              geneInputGeneListIdFilter.is = [];
            }

            geneSetIdFilter.all.push( row.geneSetId );
            geneInputGeneListIdFilter.is.push(id);

            console.log('baseFilter', JSON.stringify(baseFilter));
          });
          
        }

        $scope.exportEnrichment = function(id) {
          Restangular.one('analysis/enrichment', id).get({}, {'Accept': 'text/tsv'}).then(function(data) {
            var filename = id + '.tsv';
            ExportService.exportData(filename, data);
          });
        };

        $scope.$watch('item', function(n) {
          if (angular.isDefined(n) && !_.isEmpty(n)) {
            refresh();
          }
        });

        console.log('enrichment result', $scope.item);
      }
    };
  });

})();


(function () {
  'use strict';

  var module = angular.module('icgc.enrichment.models', []);

  module.service('Enrichment', function () {
    // TODO:  API endpoints
  });

})();

