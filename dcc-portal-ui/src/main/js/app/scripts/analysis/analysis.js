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

    // TODO: Move this out
    var REMOVE_ONE = 'Are you sure you want to remove Analysis';
    var REMOVE_ALL = 'Are you sure you want to remove all Analyses';

    Page.setPage('analysis');
    Page.setTitle('Analysis');

    $scope.analysisId = analysisId;
    $scope.analysisList = localStorageService.get('analysis') || [];

    function fetch() {
      $scope.error = null;
      // 1) Check if analysis exist in the backend
      // 2) If analysis cannot be found, delete from local-list and display error
      if ($scope.analysisId) {
        var resultPromise = RestangularNoCache.one('analysis/enrichment', $scope.analysisId).get();
        var sync = false;

        resultPromise.then(function(data) {
          data = Restangular.stripRestangular(data);

          if (! _.isEmpty(data)) {
            $scope.analysisResult = data;
            if (sync === false) {
              updateMetaData(data);
              sync = true;
            }
          }

          // Check if we need to poll
          if (data.state !== 'FINISHED') {
            pollPromise = $timeout(fetch, 5000);
          }
        }, function(error) {
          $scope.error = error.status;
        });
      }
    }

    function updateMetaData(data) {
      var id, analysis;
      id = data.id;
      analysis = _.find($scope.analysisList, function(d) {
        return d.id === id;
      });

      // Update
      if (analysis) {
        analysis.timestamp = data.timestamp;
      }
      localStorageService.set('analysis', $scope.analysisList);
    }

    function init() {
      // 1) If not already exist in local-list, prepend it to local-list
      // 2) Display list, ordered by something...
      // 3) Render analysis result in content panel
      if ($scope.analysisId) {
        var ids = _.pluck($scope.analysisList, 'id');
        if (!_.contains(ids, $scope.analysisId)) {
          var newAnalysis = {
            id: analysisId,
            timestamp: '--'
          };

          // Store
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
      confirmRemove  = window.confirm(REMOVE_ALL);
      localStorageService.set('analysis', []);
      $scope.analysisList = localStorageService.get('analysis');
    };


    $scope.remove = function(id) {
      var confirmRemove, ids;

      confirmRemove  = window.confirm(REMOVE_ONE);
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

