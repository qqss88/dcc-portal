(function () {
  'use strict';

  var module = angular.module('icgc.analysis', [
    'icgc.analysis.controllers',
    'ui.router'
  ]);

  module.config(function ($stateProvider) {
    $stateProvider.state('analyses', {
      url: '/analysis',
      reloadOnSearch: false,
      templateUrl: 'scripts/analysis/views/analysis.html',
      controller: 'AnalysisController',
      data: {
        tab: 'analysis'
      }
    });
    $stateProvider.state('analyses.sets', {
      url: '/sets',
      reloadOnSearch: false,
      data : {
        tab: 'sets'
      }
    });
    $stateProvider.state('analyses.analysis', {
      url: '/:type/:id',
      //reloadOnSearch: false,
      data: {
        tab: 'analysis'
      }
    });
  });

})();


(function () {
  'use strict';

  var module = angular.module('icgc.analysis.controllers', ['icgc.analysis.services']);


  /**
   * Top level set analyses controller
   *
   * AnalysisController: view analysis
   *   - SavedSetController: manage saved sets
   *   - AnalysisListController: manage saved analysis
   *   - NewAnalysisController: creates new analysis
   */
  module.controller('AnalysisController', function ($scope, $timeout, $state, Page, AnalysisService) {

    Page.setPage('analysis');
    Page.setTitle('Analysis');


    $scope.currentTab = $state.current.data.tab || 'analysis';
    // $scope.analysisId = $state.params.id;
    // $scope.analysisType = $state.params.type === 'set'? 'union' : $state.params.type;


    $scope.$watch(function () {
      return $state.current.data.tab;
    }, function () {
      $scope.currentTab = $state.current.data.tab || 'analysis';
    });

    $scope.$watch(function() {
      return $state.params;
    }, function() {
      $scope.analysisId = $state.params.id;
      $scope.analysisType = $state.params.type === 'set'? 'union' : $state.params.type;
      init();
    });



    var analysisPromise;
    var pollTimeout;


    function wait(id, type) {
      $scope.error = null;

      var promise = AnalysisService.getAnalysis(id, type);
      promise.then(function(data) {
        var rate = 1000;

        if (data.state !== 'FINISHED') {
          $scope.analysisResult = data;

          if (data.state === 'POST_PROCESSING') {
            rate = 4000;
          }
          pollTimeout = $timeout(function() {
            wait(id, type);
          }, rate);
        } else if (data.state === 'FINISHED') {
          $scope.analysisResult = data;
        }
      }, function() {
        $scope.error = true;
      });
    }

    function init() {
      $timeout.cancel(pollTimeout);
      $scope.error = null;

      if (! $scope.analysisId || ! $scope.analysisType) {
        return;
      }

      var id = $scope.analysisId, type = $scope.analysisType;
      var promise = AnalysisService.getAnalysis(id, type);

      promise.then(function(data) {
        if (! _.isEmpty(data)) {
          AnalysisService.addAnalysis(data, type);
        } else {
          $scope.error = true;
          return;
        }

        if (data.state === 'FINISHED') {
          $scope.analysisResult = null;
          $timeout(function() {
            $scope.analysisResult = data;
          }, 150);
          return;
        }

        // Kick off polling if not finished
        wait(id, type);

      }, function() {
        // FIXME: test only
        AnalysisService.addAnalysis({
          id: 'test',
          type: type
        }, type);
        $scope.analysisResult = {
          state: 'FINISHED'
        };
        return;
        // end test

        //$scope.error = true;
      });
    }


    $scope.$on('$locationChangeStart', function() {
      // Cancel any remaining polling requests
      $timeout.cancel(pollTimeout);
    });

    // Clea up
    $scope.$on('destroy', function() {
      $timeout.cancel(analysisPromise);
    });


    // Start
    // init();

    // Only do synchronization on analysis home tab
    /*
    if (! $scope.analysisId || ! $scope.analysisType) {
      synchronizeSets(10);
    }*/

  });

})();



(function () {
  'use strict';

  var module = angular.module('icgc.analysis.services', ['restangular']);

  module.service('AnalysisService', function(RestangularNoCache, localStorageService) {
    var ANALYSIS_ENTITY = 'analysis';
    var analysisList = [];

    this.getAnalysis = function(id, type) {
      return RestangularNoCache.one('analysis/' + type , id).get();
    };

    this.getAll = function() {
      return analysisList;
    };

    this.removeAll = function() {
      analysisList = [];
      localStorageService.set(ANALYSIS_ENTITY, analysisList);
    };

    /**
     * Add analysis to local storage
     */
    this.addAnalysis = function(analysis, type) {
      var ids = _.pluck(analysisList, 'id');
      if (_.contains(ids, analysis.id) === true) {
        return;
      }

      var payload = {
        id: analysis.id,
        timestamp: analysis.timestamp || '--',
        type: type
      };

      if (type === 'enrichment') {
        payload.universe = analysis.params.universe;
        payload.maxGeneCount = analysis.params.maxGeneCount;
      } else {
        payload.dataType = analysis.type.toLowerCase();
        payload.inputSetCount = analysis.inputCount || '';
      }

      analysisList.unshift( payload );
      localStorageService.set(ANALYSIS_ENTITY, analysisList);
    };

    this.remove = function(id) {
      var ids = _.pluck(analysisList, 'id');

      if (_.contains(ids, id)) {
        var index = ids.indexOf(id);
        analysisList.splice(index, 1);
        localStorageService.set(ANALYSIS_ENTITY, analysisList);
        return true;
      }
      return false;
    };

    // Init service
    analysisList = localStorageService.get(ANALYSIS_ENTITY) || [];

  });

})();
