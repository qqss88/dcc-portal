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
    Restangular, Page, SetService, AnalysisService) {

    Page.setPage('analysis');
    Page.setTitle('Analysis');

    $scope.listItemTotal = 0;
    $scope.listItemUUIDs = [];
    $scope.entityLists = SetService.getAll();


    // Send IDs generate new set operations analysis
    $scope.launchSetAnalysis = function() {
      console.log('selected', $scope.listItemUUIDs);

      var selected = _.filter($scope.entityLists, function(d) {
        return d.checked === true;
      });

      // FIXME: sync with terry
      var type = selected[0].type.toUpperCase();
      var ids  = _.pluck(selected, 'id');

      console.log('about to subimit', type, ids);

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
      var geneList = $scope.entityLists[0];

      $scope.enrichmentFilters = {
        gene: {
          entityListId: {
            is: [ geneList.id ]
          }
        }
      };
      $scope.enrichmentModal = true;
    };


    $scope.updateCurrentState = function(item) {
      // FIXME: Might want to move this out
      if (item.checked === true) {
        $scope.listItemTotal += item.count;
        $scope.listItemUUIDs.push(item.id);
      } else {
        $scope.listItemTotal -= item.count;
        _.remove($scope.listItemUUIDs, function(id) {
          return id === item.id;
        });
      }
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
      if (selected.length > 1 && selected.length < 4 && uniqued.length === 1) {
        $scope.setop = true;
      }
    };

    $scope.removeList = function(id) {
      SetService.remove(id);
      $scope.entityLists = SetService.getAll();
      $scope.updateAvailableAnalysis();
    };


    var pollPromise;

    // TODO: Move this out
    var REMOVE_ONE = 'Are you sure you want to remove Analysis';
    var REMOVE_ALL = 'Are you sure you want to remove all Analyses';


    $scope.analysisId = analysisId;
    $scope.analysisType = analysisType;
    $scope.analysisList = AnalysisService.getAll();


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
          pollPromise = $timeout(getAnalysis, pollRate);
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
      $timeout.cancel(pollPromise);
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
      $timeout.cancel(pollPromise);
    });
    init();
    getAnalysis();

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
