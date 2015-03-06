(function () {
  'use strict';

  var module = angular.module('icgc.analysis.controllers');


  module.controller('NewAnalysisController', function($scope, $modal, $location, Restangular, SetService, Extensions) {
    var _this = this;

    _this.analysisType = null; // One of "enrichment", "set", "phenotype" or "coverage"
    _this.filteredList = [];
    _this.filteredSetType = 'donor';
    _this.selectedIds = [];

    _this.toggle = function(setId) {
      if (_this.selectedIds.indexOf(setId) >= 0) {
        console.log('remove', setId);
        _.remove(_this.selectedIds, function(id) {
          return id === setId;
        });
      } else {
        console.log('add', setId);
        _this.selectedIds.push(setId);
      }
    };

    _this.applyFilter = function(type) {
      if (type === 'enrichment') {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          // FIXME: also do gene limit (< 1000)
          return set.type === 'gene';
        });
      } else if (type === 'set') {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          return set.type === _this.filteredSetType;
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
        $location.path('analysis/set/' + data.id);
      });
    };

    _this.launchEnrichment = function(setId) {
      var filters = {
        gene: {}
      };
      filters.gene[Extensions.ENTITY] = { is: [setId] };

      $modal.open({
        templateUrl: '/scripts/enrichment/views/enrichment.upload.html',
        controller: 'EnrichmentUploadController',
        resolve: {
          geneLimit: function() {
            return undefined;
          },
          filters: function() {
            return filters;
          }
        }
      });
    };


    $scope.$watch(function() {
      return _this.analysisType;
    }, function(n) {
      if (!n) return;
      _this.applyFilter(n);
    });
  });
})();
  

