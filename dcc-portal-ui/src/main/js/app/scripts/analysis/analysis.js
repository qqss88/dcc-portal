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
        },
        analysisType: function() {
          return null;
        }
      }
    });

    /**
    * :id is a UUID generated server-side
    * :type can be one of "enrichment", "setop"
    */
    $stateProvider.state('analysis', {
      url: '/analysis/:type/:id',
      templateUrl: 'scripts/analysis/views/analysis.html',
      controller: 'AnalysisController',
      resolve: {
        analysisId: ['$stateParams', function($stateParams) {
          return $stateParams.id;
        }],
        analysisType: ['$stateParams', function($stateParams) {
          return $stateParams.type;
        }]
      }
    });
  });

})();


(function () {
  'use strict';

  var module = angular.module('icgc.analysis.controllers', ['icgc.analysis.services']);

  module.controller('AnalysisController', function ($scope, $location, $timeout, analysisId, analysisType,
    Restangular, RestangularNoCache, Page, SetService, AnalysisService) {

    Page.setPage('analysis');
    Page.setTitle('Analysis');

    $scope.entityLists = SetService.getAll();
    $scope.canBeDeleted = 0;

    // FIXME: Debug - remove
    $scope.debugReset = function() {
      window.localStorage.clear();
      window.location.reload();
    };

    $scope.exportSet = function(id) {
      SetService.exportSet(id);
    };


    // Send IDs generate new set operations analysis
    $scope.launchSetAnalysis = function() {
      var selected = _.filter($scope.entityLists, function(d) {
        return d.checked === true;
      });

      // FIXME: sync with terry
      var type = selected[0].type.toUpperCase();
      var ids  = _.pluck(selected, 'id');

      var payload = {
        lists: ids,
        type: type
      };
      var promise = Restangular.one('analysis').post('union', payload, {}, {'Content-Type': 'application/json'});

      promise.then(function(data) {
        if (!data.id) {
          console.log('cannot create set operation');
        }
        $location.path('analysis/union/' + data.id);
      });
    };

    $scope.launchEnrichmentAnalysis = function() {
      $scope.enrichmentFilters = {
        gene: {
          entityListId: {
            is: [ $scope.enrichmentSet ]
          }
        }
      };
      $scope.enrichmentModal = true;
    };


    $scope.update = function() {
      // Check if delete should be enabled
      $scope.canBeDeleted = _.filter($scope.entityLists, function(item) {
        if (item.readonly && item.readonly === true) {
          return false;
        }
        return item.checked === true;
      });

      var selected, uniqued;
      selected = _.filter($scope.entityLists, function(item) {
        return item.checked === true;
      });


      uniqued = _.uniq(_.pluck(selected, 'type'));

      $scope.enrichmentSet = null;
      $scope.setop = null;

      if (selected.length === 1 && uniqued[0] === 'gene') {
        $scope.enrichmentSet = selected[0].id;
        $scope.totalCount = selected[0].count;
      }
      if (selected.length > 1 && selected.length < 4 && uniqued.length === 1) {
        $scope.setop = true;
      }
    };


    $scope.removeLists = function() {
      var confirmRemove = window.confirm('Are you sure you want to remove selected sets?');
      if (!confirmRemove || confirmRemove === false) {
        return;
      }

      console.log('list length', $scope.entityLists.length);
      var toRemove = [];
      $scope.entityLists.forEach(function(d) {
        console.log(d.id, d.checked, d.readonly);
        if (d.checked === true && ! d.readonly) {
          toRemove.push(d.id);
        }
      });

      if (toRemove.length > 0) {
        SetService.removeSeveral(toRemove);
        $scope.update();
      }
    };

    $scope.removeList = function(id) {
      SetService.remove(id);
      $scope.entityLists = SetService.getAll();
      $scope.updateAvailableAnalysis();
    };


    var analysisPromise;


    // TODO: Move this out
    var REMOVE_ONE = 'Are you sure you want to remove Analysis?';
    var REMOVE_ALL = 'Are you sure you want to remove all Analyses?';

    $scope.analysisId = analysisId;
    $scope.analysisType = analysisType;
    $scope.analysisList = AnalysisService.getAll();


    /**
     * Check status of any entityLists that are not in FINISHED stated
     */
    function pollEntityLists() {
      var unfinished = 0;
      console.log($scope.entityLists);
      SetService.sync();

      unfinished = _.filter($scope.entityLists, function(d) {
        return d.status !== 'FINISHED';
      }).length;

      console.log('unfinished', unfinished);

      if (unfinished > 0) {
        $timeout(pollEntityLists, 4000);
      }
    }


    function getAnalysis() {
      $scope.error = null;
      // 1) Check if analysis exist in the backend
      // 2) If analysis cannot be found, delete from local-list and display error
      if (! $scope.analysisId) {
        return;
      }
      var resultPromise = AnalysisService.getAnalysis($scope.analysisId, $scope.analysisType);
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
        } else {
          $scope.error = '404';
          return;
        }

        // FIXME: sync with bob and terry
        var currentState = data.state || data.status;
        data.state = currentState;

        console.log('data state', data.state);

        // Check if we need to poll
        if (currentState !== 'FINISHED') {
          var pollRate = 1000;
          if (currentState === 'POST_PROCESSING') {
            pollRate = 4000;
          }
          analysisPromise = $timeout(getAnalysis, pollRate);
        }

      }, function(error) {
        $scope.error = error.status;
      });
    }

    function init() {
      // 1) If not already exist in local-list, prepend it to local-list
      // 2) Display list, ordered by something...
      // 3) Render analysis result in content panel
      if ($scope.analysisId && $scope.analysisType) {
        AnalysisService.add( $scope.analysisId, $scope.analysisType);
        $scope.analysisList = AnalysisService.getAll();
      }
    }

    $scope.getAnalysis = function(id, type) {
      $timeout.cancel(analysisPromise);
      if (id) {
        $scope.analysisId = id;
        $location.path('analysis/' + type + '/' + id);
      } else {
        $scope.analysisId = null;
        $location.path('analysis');
      }
    };


    /**
     * Remove all analyses, this includes both enrichment and set ops
     */
    $scope.removeAllAnalyses = function() {
      var confirmRemove;
      confirmRemove  = window.confirm(REMOVE_ALL);
      if (confirmRemove) {
        AnalysisService.removeAll();
        $scope.analysisList = AnalysisService.getAll();
        $location.path('analysis');
      }
    };


    /**
     * Remove a single analysis by UUID
     */
    $scope.remove = function(id) {
      var confirmRemove = window.confirm(REMOVE_ONE);
      if (! confirmRemove) {
        return;
      }

      if (AnalysisService.remove(id) === true) {
        $scope.analysis = null;
        $scope.analysisList = AnalysisService.getAll();
        $location.path('analysis');
      }
    };

    // Clea up
    $scope.$on('destroy', function() {
      $timeout.cancel(analysisPromise);
    });


    init();
    getAnalysis();
    pollEntityLists();

  });
})();



(function () {
  'use strict';
  var module = angular.module('icgc.analysis.services', ['restangular']);

  module.service('AnalysisService', function(RestangularNoCache, localStorageService) {

    var ANALYSIS_ENTITY = 'analysis';
    var _this = this;

    this.getAnalysis = function(id, type) {
      return RestangularNoCache.one('analysis/' + type , id).get();
    };

    this.getAll = function() {
      return localStorageService.get(ANALYSIS_ENTITY) || [];
    };

    this.removeAll = function() {
      localStorageService.set(ANALYSIS_ENTITY, []);
    };

    this.add = function(id, type) {
      var analysisList = this.getAll();
      var ids = _.pluck(analysisList, 'id');
      if (_.contains(ids, id) === false) {
        var newAnalysis = {
          id: id,
          timestamp: '--',
          type: type
        };
        analysisList.unshift(newAnalysis);
        localStorageService.set(ANALYSIS_ENTITY, analysisList);
        return true;
      }
      return false;
    };

    this.update = function(analysis) {
      var analysisList = _this.getAll();
      var cachedAnalysis = _.find(analysisList, function(d) {
        return d.id === analysis.id;
      });
      if (cachedAnalysis) {
        cachedAnalysis.timestamp = analysis.timestamp;
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
