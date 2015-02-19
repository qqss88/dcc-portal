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
          if ($stateParams.type === 'set') {
            return 'union';
          }
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
    Restangular, RestangularNoCache, Page, SetService, AnalysisService, Extensions) {

    Page.setPage('analysis');
    Page.setTitle('Analysis');

    $scope.entityLists = SetService.getAll();
    $scope.canBeDeleted = 0;
    $scope.enrichment = {};
    $scope.syncError = false;

    // Selected sets
    $scope.selectedSets = [];
    $scope.addSelection = function(set) {
      // console.log('adding set', set);
      $scope.selectedSets.push(set);
    };

    $scope.removeSelection = function(set) {
      // console.log('removeng set', set);
      _.remove($scope.selectedSets, function(s) {
        return s.id === set.id;
      });
    };

    $scope.exportSet = function(id) {
      SetService.exportSet(id);
    };

    // Send IDs generate new set operations analysis
    $scope.launchSetAnalysis = function() {
      var selected = $scope.selectedSets;

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
        $location.path('analysis/set/' + data.id);
      });
    };

    $scope.launchEnrichmentAnalysis = function() {
      var filters = {
        gene: {}
      };
      filters.gene[Extensions.ENTITY] = { is: [$scope.enrichmentSet] };

      $scope.enrichment.filters = filters;
      $scope.enrichment.modal = true;
    };


    $scope.update = function() {

      // Check if delete button should be enabled
      $scope.canBeDeleted = _.filter($scope.selectedSets, function(set) {
        if (set.readonly && set.readonly === true) {
          return false;
        }
        return true;
      }).length;


      // Check which analyses are applicable
      var selected = $scope.selectedSets, uniqued = [];
      uniqued = _.uniq(_.pluck(selected, 'type'));

      $scope.enrichmentSet = null;
      $scope.setop = null;

      // Enrichment analysis takes only ONE gene set
      if (selected.length === 1 && uniqued[0] === 'gene') {
        $scope.enrichmentSet = selected[0].id;
        $scope.totalCount = selected[0].count;
      }

      // Set operations takes up to 3 sets of the same type
      if (selected.length > 1 && selected.length < 4 && uniqued.length === 1) {
        $scope.setop = true;
      }
    };


    $scope.removeLists = function() {
      var confirmRemove = window.confirm('Are you sure you want to remove selected sets?');
      if (!confirmRemove || confirmRemove === false) {
        return;
      }

      var toRemove = _.filter($scope.selectedSets, function(set) {
        return !angular.isDefined(set.readonly);
      });


      if (toRemove.length > 0) {

        _.remove($scope.selectedSets, function(set) {
          return _.pluck(toRemove, 'id').indexOf(set.id) >= 0;
        });

        SetService.removeSeveral(_.pluck(toRemove, 'id'));
        $scope.update();
      }
    };

    var analysisPromise;

    // TODO: Move this out
    var REMOVE_ONE = 'Are you sure you want to remove this analysis?';
    var REMOVE_ALL = 'Are you sure you want to remove all analyses?';

    $scope.analysisId = analysisId;
    $scope.analysisType = analysisType;
    $scope.analysisList = AnalysisService.getAll();


    function synchronizeSets(numTries) {
      console.log('synchronizing', numTries + ' tries remaining...');

      var pendingLists, pendingListsIDs, promise;
      pendingLists = _.filter($scope.entityLists, function(d) {
        return d.state !== 'FINISHED';
      });
      pendingListsIDs = _.pluck(pendingLists, 'id');

      if (pendingLists.length <= 0) {
        return;
      }

      if (numTries <= 0) {
        console.log('Stopping, numTries runs out');
        $scope.syncError = true;
        return;
      }

      promise = SetService.getMetaData(pendingListsIDs);
      promise.then(function(results) {
        SetService.updateSets(results);
        $timeout(function() {
          synchronizeSets(--numTries);
        }, 3000);
      });
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
          $scope.error = true;
          return;
        }

        // FIXME: sync with bob and terry
        var currentState = data.state;
        data.state = currentState;


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
      var routeType = type;
      $timeout.cancel(analysisPromise);

      if (type === 'union') {
        routeType = 'set';
      }

      if (id) {
        $scope.analysisId = id;
        $location.path('analysis/' + routeType + '/' + id);
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

    // Sync any unfinished sets that are still in the process of materialization
    synchronizeSets(5);
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
