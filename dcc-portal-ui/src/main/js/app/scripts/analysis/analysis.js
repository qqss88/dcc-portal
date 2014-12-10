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

  module.controller('AnalysisController', function ($scope, $state, $location, $timeout, $http,
    localStorageService, Page, Restangular, RestangularNoCache, analysisId) {

    var pollPromise;

    Page.setPage('analysis');
    Page.setTitle('Analysis');

    $scope.analysisId = analysisId;
    $scope.analysisList = localStorageService.get('analysis') || [];

    function fetch() {
      // 1) Check if analysis exist in the backend
      // 2) If analysis cannot be found, delete from local-list and display error
      if ($scope.analysisId) {
        console.log('fetching analysis ', $scope.analysisId);

        var resultPromise = Restangular.one('analysis/enrichment', $scope.analysisId).get();
        // var resultPromise = RestangularNoCache.one('analysis/enrichment', $scope.analysisId).get();

        // FIXME: Temporary, need to change restangular config
        // var resultPromise = $http.get('/api/v1/analysis/enrichment/' + $scope.analysisId);

        resultPromise.then(function(data) {
          data = Restangular.stripRestangular(data);

          if (! _.isEmpty(data)) {
            $scope.analysisResult = data;
          }

          // Check if we need to poll
          if (data.state === 'EXECUTING') {
            pollPromise = $timeout(fetch, 5000);
          }
        });
      }
    }


    function init() {
      // 1) If not already exist in local-list, prepend it to local-list
      // 2) Display list, ordered by something...
      // 3) Render analysis result in content panel
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


    $scope.getAnalysis = function(id) {
      $timeout.cancel(pollPromise);
      if (id) {
        $scope.analysisId = id;
        $location.path('analysis/'+id);
      } else {
        $scope.analysisId = null;
        $location.path('analysis');
      }
      fetch();
    };


    $scope.removeAllAnalyses = function() {
      var confirmRemove;
      confirmRemove  = window.confirm('This action will remove all analyses, it cannot be undone!');
      localStorageService.set('analysis', []);
      $scope.analysisList = localStorageService.get('analysis');
    }

    $scope.remove = function(id) {
      var confirmRemove, ids;

      confirmRemove  = window.confirm('This action will remove this analyis, it cannot be undone!');
      if (! confirmRemove) {
        return;
      }

      $scope.analysisList = localStorageService.get('analysis') || [];
      ids = _.pluck($scope.analysisList, 'id');

      if (_.contains(ids, id)) {
        var index = -1;
        index = ids.indexOf(id);
        $scope.analysisList.splice(index, 1);
        localStorageService.set('analysis', $scope.analysisList);
        $scope.analysisId = null;
        $location.path('analysis');
      }

    };

    // Clea up
    $scope.$on('destroy', function() {
      $timeout.cancel(pollPromise);
    });

    init();
    fetch();


  });
})();

