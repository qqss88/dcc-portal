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

      $scope.enrichmentSet = null;
      $scope.setop = null;

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


      // If there are unfinished, do not proceed
      if (_.some(selected, function(s) { return s.state != 'FINISHED'; })) {
        return;
      }


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

    var pollTimeout;

    // TODO: Move this out
    var REMOVE_ONE = 'Are you sure you want to remove this analysis?';
    var REMOVE_ALL = 'Are you sure you want to remove all analyses?';

    $scope.analysisId = analysisId;
    $scope.analysisType = analysisType;
    $scope.analysisList = AnalysisService.getAll();


    function synchronizeSets(numTries) {
      // console.log('synchronizing', numTries + ' tries remaining...');

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
          $timeout(function() {
            $scope.analysisResult = data;
          }, 150);
          return;
        }

        // Kick off polling if not finished
        wait(id, type);

      }, function() {
        $scope.error = true;
      });
    }


    $scope.getAnalysis = function(id, type) {
      var routeType = type;
      $timeout.cancel(pollTimeout);

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
        $location.path('analysis');
      }
    };

    // Clea up
    $scope.$on('destroy', function() {
      $timeout.cancel(analysisPromise);
    });



    // Start
    $scope.analysisList = AnalysisService.getAll();
    init();
    synchronizeSets(10);

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
