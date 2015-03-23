(function () {
  'use strict';

  var module = angular.module('icgc.analysis.controllers');


  /**
   * Manages the launch of an analysis, one of
   * - gene set enrichment
   * - set operations
   * - phenotype analysis
   */
  module.controller('NewAnalysisController',
    function($scope, $modal, $location, AnalysisService, Restangular, SetService, Extensions) {

    var _this = this;

    _this.analysisType = null; // One of "enrichment", "set", "phenotype" or "coverage"
    _this.filteredList = [];
    _this.filteredSetType = '';
    _this.selectedIds = [];

    _this.allSets = SetService.getAll();

    // Pass-thru
    _this.analysisName = AnalysisService.analysisName;
    _this.analysisDescription = AnalysisService.analysisDescription;

    _this.toggle = function(setId) {
      if (_this.selectedIds.indexOf(setId) >= 0) {
        _.remove(_this.selectedIds, function(id) {
          return id === setId;
        });
      } else {
        _this.selectedIds.push(setId);
      }

      // Apply filer to disable irrelevant results
      if (_this.selectedIds.length === 0) {
        _this.filteredSetType = '';
      }
      _this.applyFilter(_this.analysisType);
    };

    _this.isInFilter = function(set) {
      return _.some(_this.filteredList, function(s) {
        return s.id === set.id;
      });
    };

    _this.applyFilter = function(type) {

      if (type === 'enrichment') {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          return set.type === 'gene' && set.count <= 10000;
        });
      } else if (type === 'set') {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          if (_this.filteredSetType !== '') {
            return set.type === _this.filteredSetType;
          }
          return true;
        });
      } else if (type === 'phenotype') {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          return set.type === 'donor';
        });
      } else {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          return set.type === 'donor';
        });
      }
    };


    /* Phenotype comparison only takes in donor set ids */
    _this.launchPhenotype = function(setIds) {
      console.log('launching phenotype analysis with', setIds);

      var payload = setIds;
      var promise = Restangular.one('analysis').post('phenotype', payload, {}, {'Content-Type': 'application/json'});
      promise.then(function(data) {
        if (data.id) {
          $location.path('analysis/view/phenotype/' + data.id);
        }
      });
    };


    _this.launchSet = function(type, setIds) {
      var payload = {
        lists: setIds,
        type: type.toUpperCase()
      };
      console.log('payload', payload);
      var promise = Restangular.one('analysis').post('union', payload, {}, {'Content-Type': 'application/json'});
      promise.then(function(data) {
        if (!data.id) {
          console.log('cannot create set operation');
        }
        $location.path('analysis/view/set/' + data.id);
      });
    };

    _this.launchEnrichment = function(setId) {
      var set = _.filter(_this.filteredList, function(set) {
        return set.id === setId;
      })[0];
      var filters = {
        gene: {}
      };
      filters.gene[Extensions.ENTITY] = { is: [set.id] };

      $modal.open({
        templateUrl: '/scripts/enrichment/views/enrichment.upload.html',
        controller: 'EnrichmentUploadController',
        resolve: {
          geneLimit: function() {
            return set.count;
          },
          filters: function() {
            return filters;
          }
        }
      });
    };

    /*
    $scope.$on('analysis::reload', function() {
      console.log('here');
      _this.analysisType = null;
      _this.selectedIds = [];
      _this.filteredSetType = 'donor';
      _this.filteredList = [];
    });
    */


    $scope.$on('$locationChangeSuccess', function() {
      _this.filteredList = [];
      _this.selectedIds = [];
      _this.analysisType = null;
      _this.filteredSetType = '';
    });


    $scope.$watch(function() {
      return _this.analysisType;
    }, function(n) {
      if (!n) {
        return;
      }
      _this.selectedIds = [];
      _this.applyFilter(n);
    });
  });
})();


