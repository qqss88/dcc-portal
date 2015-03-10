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

  module.directive('phenotypeResult', function() {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/phenotype/views/phenotype.result.html',
      link: function($scope) {
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

  module.service('PhenotypeService', function() {

    this.submitAnalysis = function() {
    };

    this.getAnalysis = function() {
    };

  });
})();
