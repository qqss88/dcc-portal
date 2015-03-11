(function () {
  'use strict';

  var module = angular.module('icgc.analysis.controllers');

  module.controller('SavedSetController', function($scope, $window, $timeout, $modal, SetService) {

    var _this = this;
    var syncSetTimeout;

    _this.syncError = false;
    _this.entityLists = SetService.getAll();
    _this.selectedSets = [];
    _this.checkAll = false;


    // Selected sets
    _this.selectedSets = [];

    _this.update = function() {
      _this.selectedSets = [];
      _this.entityLists.forEach(function(set) {
        if (set.checked === true) {
          _this.selectedSets.push(set);
        }
      });
    };

    /* Select all / de-select all */
    _this.toggleSelectAll = function() {
      _this.checkAll = !_this.checkAll;

      _this.entityLists.forEach(function(set) {
        set.checked = _this.checkAll;
      });
    };

    _this.exportSet = function(id) {
      SetService.exportSet(id);
    };

    _this.addCustomGeneSet = function() {
      var inst = $modal.open({
        templateUrl: '/scripts/genelist/views/upload.html',
        controller: 'GeneListController'
      });

      // Successful submit
      inst.result.then(function() {
        $timeout.cancel(syncSetTimeout);
        synchronizeSets(10);
      });
    };

    _this.removeLists = function() {
      var confirmRemove = $window.confirm('Are you sure you want to remove selected sets?');
      if (!confirmRemove || confirmRemove === false) {
        return;
      }

      var toRemove = _.filter(_this.selectedSets, function(set) {
        return !angular.isDefined(set.readonly);
      });

      if (toRemove.length > 0) {
        _.remove(_this.selectedSets, function(set) {
          return _.pluck(toRemove, 'id').indexOf(set.id) >= 0;
        });

        SetService.removeSeveral(_.pluck(toRemove, 'id'));
        _this.checkAll = false; // reset
      }
    };

    function synchronizeSets(numTries) {
      var pendingLists, pendingListsIDs, promise;
      pendingLists = _.filter(_this.entityLists, function(d) {
        return d.state !== 'FINISHED';
      });
      pendingListsIDs = _.pluck(pendingLists, 'id');

      if (pendingLists.length <= 0) {
        return;
      }

      if (numTries <= 0) {
        console.log('Stopping, numTries runs out');
        _this.syncError = true;
        return;
      }

      promise = SetService.getMetaData(pendingListsIDs);
      promise.then(function(results) {
        SetService.updateSets(results);
        syncSetTimeout = $timeout(function() {
          synchronizeSets(--numTries);
        }, 3000);
      });
    }

    $scope.$on('destroy', function() {
      $timeout.cancel(syncSetTimeout);
    });

    synchronizeSets(10);
  });


})();

