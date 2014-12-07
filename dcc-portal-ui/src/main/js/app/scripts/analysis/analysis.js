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
    function ($scope, $state, $location, localStorageService, Page, Restangular, analysisId) {
    Page.setPage('analysis');

    $scope.analysisId = analysisId;
    $scope.analysisList = localStorageService.get('analysis') || [];

    $scope.getAnalysis = function(id) {
      // Test
      if (id) {
        $scope.analysisId = id;
        $location.path('analysis/'+id);
      } else {
        $scope.analysisId = null;
        $location.path('analysis');
      }
      fetch();
    };

    init();
    fetch();


    function fetch() {
      // 1) Check if analysis exist in the backend
      // 2) If analysis cannot be found, delete from local-list and display error
      if ($scope.analysisId) {
        var resultPromise = Restangular.one('analysis/enrichment', $scope.analysisId).get();

        resultPromise.then(function(data) {
          data = Restangular.stripRestangular(data);
          $scope.analysisResult = data;
        });

      }
    }


    function init() {
      // 2) If not already exist in local-list, prepend it to local-list
      // 3) Display list, ordered by something...
      // 4) Render analysis result in content panel
      if ($scope.analysisId) {
        var ids = _.pluck($scope.analysisList, 'id');
        if (!_.contains(ids, $scope.analysisId)) {
          var newAnalysis = {
            id: analysisId
          };
          $scope.analysisList.unshift(newAnalysis);
          localStorageService.set('analysis', $scope.analysisList);
        }
      }
    }


  });
})();

