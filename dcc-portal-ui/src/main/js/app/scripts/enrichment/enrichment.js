(function () {
  'use strict';

  angular.module('icgc.enrichment', ['icgc.enrichment.directives', 'icgc.enrichment.services']);
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


  angular.module('icgc.enrichment.directives').directive('enrichmentResult', function (Extensions, Restangular, EnrichmentService, ExportService) {
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
          var baseFilter = angular.copy(enrichment.query.filters);

          // No results yet, can't do anything
          if (! enrichment.results) {
            return;
          }

          // Create links for overview
          var overviewUniverseFilter = {};
          var overviewGeneOverlapFilter = {};

          $scope.overviewInputFilters = EnrichmentService.overviewInputFilters(enrichment);
          $scope.overviewGeneOverlapFilters = EnrichmentService.overviewGeneOverlapFilters(enrichment);



          // Compute queries to go to advanced search page
          // 1) Add inputGeneList
          // 2) Add genesetId
          enrichment.results.forEach(function(row) {
            var geneFilter, geneSetIdFilter, geneInputGeneListIdFilter;

            if (! baseFilter.gene) {
              baseFilter.gene = {};
            }
            geneFilter = baseFilter.gene;


            // Set filter
            if (! geneFilter.geneSet) {
              geneFilter.geneSetId = {};
            }
            geneSetIdFilter = geneFilter.geneSetId;
            if (! geneSetIdFilter.all) {
              geneSetIdFilter.all = [];
            }
            geneSetIdFilter.all.push( row.geneSetId );
            row.advFilterNoOverlap = angular.copy(baseFilter);


            // List filter
            if (! geneFilter.inputGeneListId) {
              geneFilter.inputGeneListId = {};
            }
            geneInputGeneListIdFilter = geneFilter.inputGeneListId;
            if (! geneInputGeneListIdFilter.is) {
              geneInputGeneListIdFilter.is = [];
            }
            geneInputGeneListIdFilter.is.push(id);
            row.advFilter = baseFilter;
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

  var module = angular.module('icgc.enrichment.services', []);

  /**
   * Encapsulates some of the cringe-worthy filter manipulations in one place
   */
  module.service('EnrichmentService', function(Extensions) {

    function ensureGeneExist(filters) {
      if (! filters.gene) {
        filters.gene = {};
      }
      return filters;
    }


    this.overviewUniverseFilters = function(enrichment) {
    };


    /**
     * FIXME: 
     * Return empty filters if there are overlapping pathway ids or goTerm ids
     */
    this.overviewGeneOverlapFilters = function(enrichment) {
      var filters = angular.copy(enrichment.query.filters);
      var universe = _.find(Extensions.GENE_SET_ROOTS, function(go) {
        return go.universe === enrichment.params.universe; 
      });

      // Disallow goTerm ids overlapping
      if (filters.gene && filters.gene.goTermId && universe.type === 'go_term') {
        return null;
      }

      // Disallow pathway ids overlapping
      if (filters.gene && filters.gene.pathwayId && universe.type === 'pathway') {
        return null;
      }


      // Replace list with input limit
      delete filters.gene.uploadGeneList; //FIXME: check name
      filters.gene.inputGeneListId = {
        is: [enrichment.id]
      };

      // Add type specific conditions
      if (universe.type === 'go_term') {
        filters.gene.goTermId = { 'all': [universe.id] };
      } else if (universe.type === 'pathway') {
        filters.gene.hasPathway = true;
      }

      return filters;
    };


    this.overviewInputFilters = function(enrichment) {
      var filters = angular.copy(enrichment.query.filters);
      filters = ensureGeneExist(filters);
      filters.gene.inputGeneListId = {
        is: [ enrichment.id]
      };
      return filters;
    };


    this.resultFilters = function() {
    };

    this.resultOverlapFilters = function() {
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

