(function () {
  'use strict';

  angular.module('icgc.enrichment', ['icgc.enrichment.controllers', 'icgc.enrichment.directives']);
})();



(function () {
  'use strict';

  angular.module('icgc.enrichment.controllers', []);

  /**
   * Validate and submit gene set analysis input
   */
  angular.module('icgc.enrichment.controllers')
    .controller('EnrichmentUploadController', function($scope, Extensions, Restangular, LocationService, $location) {
    var _ctrl = this;

    _ctrl.hasValidParams = false;

    $scope.Extensions = Extensions;

    // Default values
    _ctrl.analysisParams = {
      maxGeneSetCount: 50,
      fdr: 0.05,
      maxGeneCount: 1000,
      universe: ''
    };

    _ctrl.newGeneSetEnrichment = function() {
      var promise, data;


      // FIXME: need real values
      data = 'params=' + JSON.stringify(_ctrl.analysisParams) + '&' +
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

    _ctrl.checkInput = function() {
      var params = _ctrl.analysisParams;

      if (_ctrl.hasValidGeneCount(parseInt(params.maxGeneCount, 10)) === false ||
        _ctrl.hasValidFDR(parseFloat(params.fdr)) === false ||
        angular.isDefined(params.universe) === false) {
        _ctrl.hasValidParams = false;
      }
      _ctrl.hasValidParams = true;
    };

    _ctrl.hasValidFDR = function(val) {
      if (angular.isNumber(val) === false) {
        return false;
      }
      if (val >= 0.005 && val <= 0.5) {
        return true;
      }
      return false;
    };

    _ctrl.hasValidGeneCount = function(val) {
      if (angular.isNumber(val) === false) {
        return false;
      }
      return true;
    };
  });


  /**
   * Displays gene set enrichment results
   */
  angular.module('icgc.enrichment.controllers')
    .controller('EnrichmentResultController', function() {
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
      controller: 'EnrichmentUploadController as EnrichmentUploadController'
    };
  });

  angular.module('icgc.enrichment.directives').directive('enrichmentResult', function () {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/enrichment/views/enrichment.result.html',
      link: function($scope) {
        $scope.predicate = 'adjustedPValue';
        $scope.reverse = false;

        console.log('enrichment result', $scope.item);
      }
    };
  });


  /*
  angular.module('icgc.enrichment.directives').directive('enrichmentResult', function () {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/enrichment/views/enrichment.result.html',
      controller: 'EnrichmentResultController as EnrichmentResultController'
    };
  });
  */

})();


(function () {
  'use strict';

  var module = angular.module('icgc.enrichment.models', []);

  module.service('Enrichment', function () {
    // TODO:  API endpoints
  });

})();

