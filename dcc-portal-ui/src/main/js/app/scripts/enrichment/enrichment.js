(function () {
  'use strict';

  angular.module('icgc.enrichment', ['icgc.enrichment.controllers', 'icgc.enrichment.directives']);
})();



(function () {
  'use strict';

  angular.module('icgc.enrichment.controllers', []);


  /**
   * Validates the gene set enrichment submission and updates the current url
   */
  angular.module('icgc.enrichment.controllers')
    .controller('enrichmentUploadController', function($scope, Extensions) {
    $scope.Extensions = Extensions;

    // Default values
    $scope.analysisParams = {
      geneSetCount: 50,
      fdr: 0.05,
      geneCount: 1000
    };

    $scope.newGeneSetEnrichment = function() {
    };

    $scope.hasValidParams = function() {
      var params = $scope.analysisParams;

      if ($scope.hasValidGeneCount(parseInt(params.geneCount, 10)) === false ||
        $scope.hasValidFDR(parseFloat(params.fdr)) === false ||
        angular.isDefined(params.background) === false) {
        return false;
      }
      return true;
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
      if (angular.isNumber(val) === false) {
        console.log('blah');
        return false;
      }
      return true;
    };
  });


  /**
   * Displays gene set enrichment results
   */
  angular.module('icgc.enrichment.controllers')
    .controller('enrichmentResultController', function($scope) {
  });

})();



(function () {
  'use strict';

  angular.module('icgc.enrichment.directives', ['icgc.enrichment.controllers']);

  angular.module('icgc.enrichment.directives').directive('enrichmentUpload', function () {
    return {
      restrict: 'E',
      scope: {
        totalGenes: '@'
      },
      templateUrl: '/scripts/enrichment/views/enrichment.upload.html',
      controller: 'enrichmentUploadController'
    };
  });

  angular.module('icgc.enrichment.directives').directive('enrichmentResult', function () {
    return {
      restrict: 'E',
      scope: {
        collapsed: '='
      },
      templateUrl: '/scripts/enrichment/views/enrichment.result.html',
      controller: 'enrichmentResultController'
    };
  });

})();


(function () {
  'use strict';

  var module = angular.module('icgc.enrichment.models', []);

  module.service('Enrichment', function (Restangular) {
    // TODO:  API endpoints
  });



})();

