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


        function buildEnrichmentRequest() {
          var data, geneSortParam;

          data = 'params=' + JSON.stringify($scope.analysisParams) + '&' +
            'filters=' + JSON.stringify(LocationService.filters()) + '&' ;

          geneSortParam = LocationService.getJsonParam('genes');

          if (!_.isEmpty(geneSortParam)) {
            var sort, order;
            sort = geneSortParam['sort'];
            order = geneSortParam['order'] === 'asc'? 'ASC' : 'DESC';
            data += 'sort=' + sort + '&order=' + order;
          } else {
            data += 'sort=affectedDonorCountFiltered&order=ASC';
          }
          return data;
        }


        $scope.newGeneSetEnrichment = function() {
          var promise, data;

          data = buildEnrichmentRequest();
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


  angular.module('icgc.enrichment.directives').directive('enrichmentResult',
    function (Extensions, Restangular, EnrichmentService, ExportService) {

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

          // No results yet, can't do anything
          if (! enrichment.results) {
            return;
          }

          // Create links for overview
          $scope.overviewUniverseFilters = EnrichmentService.overviewUniverseFilters(enrichment);
          $scope.overviewInputFilters = EnrichmentService.overviewInputFilters(enrichment);
          $scope.overviewGeneOverlapFilters = EnrichmentService.overviewGeneOverlapFilters(enrichment);

          // Compute queries to go to advanced search page
          enrichment.results.forEach(function(row) {
            row.geneSetFilters = EnrichmentService.geneSetFilters(enrichment, row);
            row.geneSetOverlapFilters = EnrichmentService.geneSetOverlapFilters(enrichment, row);
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
      var filters = {};
      var universe = _.find(Extensions.GENE_SET_ROOTS, function(go) {
        return go.universe === enrichment.params.universe;
      });

      filters = ensureGeneExist(filters);

      // Add universe type specific conditions
      if (universe.type === 'go_term') {
        filters.gene.goTermId = {
          is: [universe.id]
        };
      } else if (universe.type === 'pathway') {
        filters.gene.hasPathway = true;
      }
      return filters;
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

      filters = ensureGeneExist(filters);

      // Disallow goTerm ids overlapping
      if (filters.gene && filters.gene.goTermId && universe.type === 'go_term') {
        return null;
      }

      // Disallow pathway ids overlapping
      if (filters.gene && filters.gene.pathwayId && universe.type === 'pathway') {
        return null;
      }

      // Replace list with input limit
      delete filters.gene.uploadGeneListId; //TODO: check name
      filters.gene.inputGeneListId = {
        is: [enrichment.id]
      };

      // Add universe type specific conditions
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


    this.geneSetFilters = function(enrichment, row) {
      var filters = {};
      var universe = _.find(Extensions.GENE_SET_ROOTS, function(go) {
        return go.universe === enrichment.params.universe;
      });
      filters = ensureGeneExist(filters);

      // Route based on universe type
      if (universe.type === 'go_term') {
        filters.gene.goTermId = {
          is: [row.geneSetId]
        };
      } else if (universe.type === 'pathway') {
        filters.gene.pathwayId = {
          is: [row.geneSetId]
        };
      }
      return filters;
    };


    this.geneSetOverlapFilters = function(enrichment, row) {
      var filters = angular.copy(enrichment.query.filters);
      var universe = _.find(Extensions.GENE_SET_ROOTS, function(go) {
        return go.universe === enrichment.params.universe;
      });

      filters = ensureGeneExist(filters);

      // Disallow goTerm ids overlapping
      if (filters.gene && filters.gene.goTermId && universe.type === 'go_term') {
        return null;
      }

      // Disallow pathway ids overlapping
      if (filters.gene && filters.gene.pathwayId && universe.type === 'pathway') {
        return null;
      }

      delete filters.gene.uploadGeneListId; //TODO: check name
      filters.gene.inputGeneListId = {
        is: [enrichment.id]
      };

      // Add universe type specific conditions
      if (universe.type === 'go_term') {
        filters.gene.goTermId = { 'all': [row.geneSetId] };
      } else if (universe.type === 'pathway') {
        filters.gene.pathwayId = { 'all': [row.geneSetId] };
      }
      return filters;
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

