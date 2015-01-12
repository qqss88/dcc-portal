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

  var module = angular.module('icgc.analysis.controllers', ['icgc.analysis.services']);

  module.controller('AnalysisController', function ($scope, $location, $timeout, analysisId,
    Page, ListManagerService, AnalysisService, toaster) {


    // Testing
    ListManagerService.seedTestData();
    $scope.entityLists = ListManagerService.getAll();

    // toaster.pop('', 'Test', '<a href="/search">hello link</a>', 4000, 'trustedHtml');
    // toaster.pop('', 'ICGC Portal', '<a href="/search">New list created</a>', 4000, 'trustedHtml');

    $scope.createTestList = function(type) {
      var list = {
         id: new Date(),
         type: type,
         name: type + ' ' + new Date(),
         note: 'This is a test',
         count: Math.floor(Math.random()*100)
      };
      ListManagerService.addTest(list);
      $scope.entityLists = ListManagerService.getAll()
      $scope.updateAvailableAnalysis();
    };

    $scope.updateAvailableAnalysis = function() {
      var selected, uniqued;
      selected = _.filter($scope.entityLists, function(item) {
        return item.checked === true;
      });
      uniqued = _.uniq(_.pluck(selected, 'type'));


      $scope.enrichment = null;
      $scope.setop = null;

      if (selected.length === 1 && uniqued[0] === 'gene') {
        $scope.enrichment = true;
      }
      if (selected.length > 1 && uniqued.length === 1) {
        $scope.setop = true;
      }
    };

    $scope.removeList = function(id) {
      ListManagerService.remove(id);
      $scope.entityLists = ListManagerService.getAll();
      $scope.updateAvailableAnalysis();
    };


    var pollPromise;

    // TODO: Move this out
    var REMOVE_ONE = 'Are you sure you want to remove Analysis';
    var REMOVE_ALL = 'Are you sure you want to remove all Analyses';

    Page.setPage('analysis');
    Page.setTitle('Analysis');

    $scope.analysisId = analysisId;
    $scope.analysisList = AnalysisService.getAll();

    function fetch() {
      $scope.error = null;
      // 1) Check if analysis exist in the backend
      // 2) If analysis cannot be found, delete from local-list and display error
      if (! $scope.analysisId) {
        return;
      }
      var resultPromise = AnalysisService.getAnalysis($scope.analysisId);
      var sync = false;

      resultPromise.then(function(data) {
        // data = Restangular.stripRestangular(data);
        if (! _.isEmpty(data)) {
          $scope.analysisResult = data;
          if (sync === false) {
            AnalysisService.update(data);
            $scope.analysisList = AnalysisService.getAll();
            sync = true;
          }
        }

        // Check if we need to poll
        if (data.state !== 'FINISHED') {
          var pollRate = 1000;
          if (data.state === 'POST_PROCESSING') {
            pollRate = 4000;
          }
          pollPromise = $timeout(fetch, pollRate);
        }
      }, function(error) {
        $scope.error = error.status;
      });
    }

    function init() {
      // 1) If not already exist in local-list, prepend it to local-list
      // 2) Display list, ordered by something...
      // 3) Render analysis result in content panel
      if ($scope.analysisId) {
        AnalysisService.add( $scope.analysisId);
        $scope.analysisList = AnalysisService.getAll();
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
      if (confirmRemove) {
        AnalysisService.removeAll();
        $scope.analysisList = AnalysisService.getAll();
      }
    };


    $scope.remove = function(id) {
      var confirmRemove = window.confirm(REMOVE_ONE);
      if (! confirmRemove) {
        return;
      }

      if (AnalysisService.remove(id) === true) {
        $scope.analysis = null;
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



(function () {
  'use strict';
  var module = angular.module('icgc.analysis.services', ['restangular', 'icgc.common.location']);

  module.service('AnalysisService', function(RestangularNoCache, localStorageService) {

    var ANALYSIS_ENTITY = 'analysis';

    this.getAnalysis = function(id) {
      return RestangularNoCache.one('analysis/enrichment', id).get();
    };

    this.getAll = function() {
      return localStorageService.get(ANALYSIS_ENTITY) || [];
    };

    this.removeAll = function() {
      localStorageService.set(ANALYSIS_ENTITY, []);
    };

    this.add = function(id) {
      var analysisList = this.getAll();
      var ids = _.pluck(analysisList, 'id');
      if (_.contains(ids, id) === false) {
        var newAnalysis = {
          id: id,
          timestamp: '--'
        };
        analysisList.unshift(newAnalysis);
        localStorageService.set(ANALYSIS_ENTITY, analysisList);
        return true;
      }
      return false;
    };

    this.update = function(analysis) {
      var analysisList = this.getAll();
      var analysis = _.find(analysisList, function(d) {
        return d.id === analysis.id;
      });
      if (analysis) {
        analysis.timestamp = data.timestamp;
        localStorageService.set(ANALYSIS_ENTITY, analysisList);
        return true;
      }
      return false;
    };

    this.remove = function(id) {
      var analysisList = this.getAll();
      var ids = _.pluck(analysisList, 'id');

      if (_.contains(ids, id)) {
        var index = ids.indexOf(id);
        analysisList.splice(index, 1);
        localStorageService.set(ANALYSIS_ENTITY, analysisList);
        return true;
      }
      return false;
    };
  });

})();
