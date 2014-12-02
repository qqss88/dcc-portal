(function () {
  'use strict';

  var module = angular.module('icgc.analysis', ['icgc.analysis.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('analyses', {
      url: '/analysis',
      templateUrl: 'scripts/analysis/views/analysis.html',
      controller: 'AnalysisController'
    });

    $stateProvider.state('analysis', {
      url: '/analysis/:id',
      templateUrl: 'scripts/analysis/views/analysis.html',
      controller: 'AnalysisController',
      resolve: {
        analysisId: ['$stateParams', function($stateParams) {
          return $stateParams.id;
        }]
      }
    });

  });

})();


(function () {
  'use strict';

  var module = angular.module('icgc.analysis.controllers', []);

  module.controller('AnalysisController',
    function ($scope, $state, Page, analysisId) {
       console.log('analysisId', analysisId);
    $scope.analysisId = analysisId;
  });
})();  

