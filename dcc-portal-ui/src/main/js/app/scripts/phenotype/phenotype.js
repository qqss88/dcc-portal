(function() {
  'use strict';

  angular.module('icgc.phenotype', [
    'icgc.phenotype.directives',
    'icgc.phenotype.services'
  ]);

})();


(function() {
  'use strict';

  var module = angular.module('icgc.phenotype.directives', ['icgc.phenotype.services']);

  module.directive('phenotypeResult', function(SetService, PhenotypeService) {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/phenotype/views/phenotype.result.html',
      link: function($scope) {

        // From D3's cat10 scale
        $scope.seriesColours = ['#1f77b4', '#ff7f0e', '#2ca02c'];

        function normalize() {
          // Normalize results: Sort by id, then sort by terms
          $scope.item.results.forEach(function(subAnalysis) {
            subAnalysis.data.forEach(function(d) {
              d.terms = _.sortBy(d.terms, function(term) {
                return term.term;
              });
            });
            subAnalysis.data = _.sortBy(subAnalysis.data, function(d) {
              return d.id;
            });
          });
        }


        function buildAnalyses() {

          // Globals
          $scope.setIds = _.pluck($scope.item.results[0].data, 'id');
          /*
          $scope.setCounts = $scope.item.results[0].data.map(function(d) {
            return d.summary.total;
          });
          */
          $scope.setFilters = $scope.setIds.map(function(id) {
            return PhenotypeService.entityFilters(id);
          });


          SetService.getMetaData($scope.setIds).then(function(results) {
            $scope.setMap = {};
            results.forEach(function(set) {
              set.advLink = SetService.createAdvLink(set);
              $scope.setMap[set.id] = set;
            });
          });


          // Fetch analyses
          var gender = _.find($scope.item.results, function(subAnalysis) {
            return subAnalysis.name === 'gender';
          });
          var vital = _.find($scope.item.results, function(subAnalysis) {
            return subAnalysis.name === 'vitalStatus';
          });
          var age = _.find($scope.item.results, function(subAnalysis) {
            return subAnalysis.name === 'ageAtDiagnosisGroup';
          });

          $scope.gender = PhenotypeService.buildAnalysis(gender);
          $scope.vital = PhenotypeService.buildAnalysis(vital);
          $scope.age = PhenotypeService.buildAnalysis(age);

          console.log($scope.gender);
        }

        $scope.$watch('item', function(n) {
          if (n) {
            console.log('lllll', n);
            normalize();
            buildAnalyses();
          }
        });

      }
    };
  });
})();



(function() {
  'use strict';

  var module = angular.module('icgc.phenotype.services', ['icgc.donors.models']);

  module.service('PhenotypeService', function(Extensions, Restangular) {

    function getTermCount(analysis, term, donorSetId) {
      var data, termObj;
      data = _.find(analysis.data, function(set) {
        return donorSetId === set.id;
      });

      // Special case
      if (term === 'missing') {
        //return data.missing || 0;
        return data.summary.missing || 0;
      }
      termObj = _.find(data.terms, function(t) {
        return t.term === term;
      });
      if (termObj) {
        return termObj.count;
      }
      return 0;
    }

    function getSummary(analysis, donorSetId) {
      var data;
      data = _.find(analysis.data, function(set) {
        return donorSetId === set.id;
      });
      return data.summary;
    }

    this.entityFilters = function(id) {
      var filters = {
        donor:{}
      };
      filters.donor[Extensions.ENTITY] = {
        is: [id]
      };
      return filters;
    };


    /*
    this.submitAnalysis = function(donorSetIds) {
      var payload = encodeURIComponent(donorSetIds);
      Restangular.one('
    };
    */


    this.getAnalysis = function() {
    };


    /**
     * Returns UI representation
     */
    this.buildAnalysis = function(analysis) {
      var uiTable = [];
      var uiSeries = [];
      var terms = _.pluck(analysis.data[0].terms, 'term');
      var setIds = _.pluck(analysis.data, 'id');

      terms.push('missing');

      // Build table row
      terms.forEach(function(term) {
        var row = {};
        row.term = term;

        setIds.forEach(function(donorSetId) {
          var count = getTermCount(analysis, term, donorSetId);
          var summary = getSummary(analysis, donorSetId);
          var advQuery = {};
          advQuery[Extensions.ENTITY] = {
            is: [donorSetId]
          };
          advQuery[analysis.name] = {
            is: [term === 'missing'? '_missing':term]
          };

          row[donorSetId] = {};
          row[donorSetId].count = count;
          row[donorSetId].total = (summary.total + summary.missing);
          row[donorSetId].percentage = count/(summary.total + summary.missing);
          row[donorSetId].advQuery = {
            donor: advQuery
          };
        });
        uiTable.push(row);
      });

      // Build graph series
      setIds.forEach(function(setId) {
        uiSeries.push({
          name: setId,
          data: _.pluck(uiTable.map(function(row) { return row[setId]; }), 'percentage')
        });
      });

      // Build final result
      return {
        uiTable: uiTable,
        uiGraph: {
          categories: terms,
          series: uiSeries
        }
      };
    };

  });


})();
