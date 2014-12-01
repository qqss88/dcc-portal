(function () {
  'use strict';

  angular.module('icgc.enrichment', ['icgc.enrichment.controllers', 'icgc.enrichment.directives']);
})();



(function () {
  'use strict';

  angular.module('icgc.enrichment.controllers', []);

  angular.module('icgc.enrichment.controllers').controller('enrichmentUploadController', function($scope, Extensions) {
    $scope.Extensions = Extensions;
    $scope.analysisParams = {
      geneSetCount: 50,
      fdr: 0.05,
      geneCount: 1000
    };

    $scope.newGeneSetEnrichment = function() {
    };

    $scope.hasValidParams = function() {
    };

  });

  angular.module('icgc.enrichment.controllers').controller('enrichmentResultController', function($scope) {
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
      templateUrl: '/scripts/enrichment/views/enrichment.upload.html',
      controller: 'enrichmentResultController'
    };
  });



})();


