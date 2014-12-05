(function () {
  'use strict';

  var module = angular.module('icgc.analysis', ['icgc.analysis.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('analyses', {
      url: '/analysis',
      templateUrl: 'scripts/analysis/views/analysis.html',
      controller: 'AnalysisController',
      resolve: {
        analysisId: function() {
          return null;
        }
      }
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
    function ($scope, $state, $location, Page, analysisId) {
    Page.setPage('analysis');
    $scope.analysisId = analysisId;

    $scope.getAnalysis = function(id) {
      // Test
      if (id) {
        $scope.analysisId = id;
        $location.path('analysis/'+id);
      } else {
        $scope.analysisId = null;
        $location.path('analysis');
      }
    };



    // If has id in url
    // 1) Check if analysis exist in the backend
    // 1a) If analysis cannot be found, delete from local-list and display error
    // 2) If not already exist in local-list, prepend it to local-list
    // 3) Display list, ordered by something...
    // 4) Render analysis result in content panel

    // If no id in url
    // 1) Display list, ordered by something
    // 2) Default content panel (show and provide link to where one can start an analysis) ?



  });
})();

