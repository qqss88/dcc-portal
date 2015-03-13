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

  module.directive('phenotypeResult', function(PhenotypeService) {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/phenotype/views/phenotype.result.html',
      link: function($scope) {

        // From D3's cat10 scale
        $scope.seriesColours = ['#1f77b4', '#ff7f0e', '#2ca02c'];


        $scope.item = {
          id: 'abc-def-ghi',
          inputCount: 2,
          state: 'FINISHED',
          result: [
            {
              name: 'gender',
              data: [
                {
                  id: 'uuid-1',
                  missing: 100,
                  terms: [
                    {term:'female', count: 50}, {term: 'male', count: 100}
                  ],
                  summary: {
                     total: 250
                  }
                },
                {
                  id: 'uuid-2',
                  missing: 10,
                  terms: [
                    {term:'female', count: 150}, {term: 'male', count: 10}
                  ],
                  summary: {
                     total: 170
                  }
                },
                {
                  id: 'uuid-3',
                  missing: 1,
                  terms: [
                    {term:'female', count: 50}, {term: 'male', count: 50}
                  ],
                  summary: {
                     total: 101
                  }
                }


              ]
            },
            {
              name: 'vitalStatus',
              data: [
                {
                  id: 'uuid-1',
                  missing: 0,
                  terms: [
                    {term:'alive', count: 150}, {term: 'deceased', count: 100}
                  ],
                  summary: {
                     total: 250
                  }
                },
                {
                  id: 'uuid-2',
                  missing: 0,
                  terms: [
                    {term:'alive', count: 150}, {term: 'deceased', count: 20}
                  ],
                  summary: {
                     total: 170
                  }
                },
                {
                  id: 'uuid-3',
                  missing: 101,
                  terms: [
                    {term:'alive', count: 0}, {term: 'deceased', count: 0}
                  ],
                  summary: {
                     total: 101
                  }
                }


              ]
            },
            {
              name: 'ageAtDiagnosisGroup',
              data: [
                {
                  id: 'uuid-1',
                  missing: 0,
                  terms: [
                    {term:  '1-9',  count: 1},
                    {term: '10-19', count: 2},
                    {term: '20-29', count: 3},
                    {term: '30-39', count: 4},
                    {term: '40-49', count: 5},
                    {term: '50-59', count: 6},
                    {term: '60-69', count: 7},
                    {term: '70-79', count: 8},
                    {term: '80-89', count: 9},
                    {term: '90-99', count: 10}
                  ],
                  summary: {
                     total: 250
                  }
                },
                {
                  id: 'uuid-2',
                  missing: 0,
                  terms: [
                    {term:  '1-9',  count: 1},
                    {term: '10-19', count: 2},
                    {term: '20-29', count: 3},
                    {term: '30-39', count: 4},
                    {term: '40-49', count: 5},
                    {term: '50-59', count: 6},
                    {term: '60-69', count: 7},
                    {term: '70-79', count: 8},
                    {term: '80-89', count: 9},
                    {term: '90-99', count: 10}
                  ],
                  summary: {
                     total: 170
                  }
                },
                {
                  id: 'uuid-3',
                  missing: 0,
                  terms: [
                    {term:  '1-9',  count: 1},
                    {term: '10-19', count: 2},
                    {term: '20-29', count: 3},
                    {term: '30-39', count: 4},
                    {term: '40-49', count: 5},
                    {term: '50-59', count: 6},
                    {term: '60-69', count: 7},
                    {term: '70-79', count: 8},
                    {term: '80-89', count: 9},
                    {term: '90-99', count: 10}
                  ],
                  summary: {
                     total: 101
                  }
                }

              ]
            },

          ]
        };


        // 0) Normalize results: Sort by id, then sort by terms
        $scope.item.result.forEach(function(subAnalysis) {
          subAnalysis.data.forEach(function(d) {
            d.terms = _.sortBy(d.terms, function(term) {
              return term.term;
            });
          });
          subAnalysis.data = _.sortBy(subAnalysis.data, function(d) {
            return d.id;
          });
        });

        // Globals
        $scope.setIds = _.pluck($scope.item.result[0].data, 'id');
        $scope.setCounts = $scope.item.result[0].data.map(function(d) {
          return d.summary.total;
        });
        $scope.setFilters = $scope.setIds.map(function(id) {
          return PhenotypeService.entityFilters(id);
        });


        // Fetch analyses
        var gender = _.find($scope.item.result, function(subAnalysis) {
          return subAnalysis.name === 'gender';
        });
        var vital = _.find($scope.item.result, function(subAnalysis) {
          return subAnalysis.name === 'vitalStatus';
        });
        var age = _.find($scope.item.result, function(subAnalysis) {
          return subAnalysis.name === 'ageAtDiagnosisGroup';
        });

        $scope.gender = PhenotypeService.buildAnalysis(gender);
        $scope.vital = PhenotypeService.buildAnalysis(vital);
        $scope.age = PhenotypeService.buildAnalysis(age);



        $scope.testChart = {};
        $scope.testChart.series = [
          {
            name: 'S1',
            data: [40, 50, 60]
          },
          {
            name: 'S2',
            data: [80, 50, 20]
          },
          {
            name: 'S3',
            data: [10, 90, 20]
          }

        ];
        $scope.testChart.categories = ['Male', 'Female', 'No Data'];

        $scope.testChart2 = {};
        $scope.testChart2.series = [
          {
            name: 'S1',
            data: [40, 50, 60, 10, 20, 40, 60, 90]
          },
          {
            name: 'S2',
            data: [30, 40, 30, 20, 30, 40, 30, 20]
          },
          {
            name: 'S3',
            data: [10, 20, 40, 40, 80, 10, 10, 10]
          }
        ];
        $scope.testChart2.categories = ['1-10', '11-20', '21-30', '31-40', '41-50', '51-60', '61-70', '71-80'];

      }
    };
  });
})();



(function() {
  'use strict';

  var module = angular.module('icgc.phenotype.services', ['icgc.donors.models']);

  module.service('PhenotypeService', function(Extensions) {

    function getTermCount(analysis, term, donorSetId) {
      var data, term;
      data = _.find(analysis.data, function(set) {
        return donorSetId === set.id;
      });

      // Special case
      if (term === 'missing') {
        return data.missing || 0;
      }
      term = _.find(data.terms, function(t) {
        return t.term === term;
      });
      if (term) {
        return term.count;
      }
      return 0;
    }

    function getSummary(analysis, donorSetId) {
      var data, term;
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


    this.submitAnalysis = function() {
    };

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
           row[donorSetId].total = summary.total;
           row[donorSetId].percentage = count/summary.total;
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
