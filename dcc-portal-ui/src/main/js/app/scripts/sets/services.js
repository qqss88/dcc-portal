/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

(function () {
  'use strict';

  var module = angular.module('icgc.sets.services', []);

  module.service('SetOperationService', function() {
    var shortHandPrefix = 'S';

    /**
     * Check set/list equality ... is there a better way?
     */
    this.isEqual = function(s1, s2) {
      return (_.difference(s1, s2).length === 0 && _.difference(s2, s1).length === 0);
    };


    /**
     *  Pull out the unique sets involved in the set operations analysis result
     */
    this.extractUniqueSets = function(items) {
      var result = [];
      items.forEach(function(set) {
        set.intersection.forEach(function(id) {
          if (_.contains(result, id) === false) {
            result.push(id);
          }
        });
      });
      return result;
    };


    /**
     * Sort set operations analysis results
     */
    this.sortData = function(items) {
      items.forEach(function(subset) {
        subset.intersection.sort();
        subset.exclusions.sort();
      });
      return _.sortBy(items, function(subset) {
          var secondary = subset.exclusions.length > 0 ? subset.exclusions[0] : '';
          return subset.intersection.length + '' + secondary;
        }).reverse();
    };

    /**
     * Transform data array to be consumed by venn-diagram visualization
     */
    this.transform = function(data) {
      var result = [];

      data.forEach(function(set) {
        var subset = [];
        set.intersection.forEach(function(sid) {
          subset.push({
            id: sid,
            count: set.count
          });
        });
        result.push(subset);
      });
      return result;
    };

    function _getSetShortHand(setId, setList) {
      if (setList) {
        return shortHandPrefix + (setList.indexOf(setId) + 1);
      }
      return setId;
    }

    this.getSetShortHand = _getSetShortHand;


  });



  /**
   * Abstracts CRUD operations on entity lists (gene, donor, mutation)
   */
  module.service('SetService',
    function($window, Restangular, RestangularNoCache, API, localStorageService, toaster, Extensions) {

    var LIST_ENTITY = 'entity';
    var _this = this;


    // For application/json format
    function params2JSON(type, params) {
      var data = {};
      data.filters = encodeURI(JSON.stringify(params.filters));
      data.type = type.toUpperCase();
      data.name = params.name;
      data.description = params.description || '';
      data.size = params.size || 0;

      if (params.isTransient) {
        data.isTransient = params.isTransient;
      }

      /*
      data.name = encodeURIComponent(params.name);
      if (angular.isDefined(params.description)) {
        data.description = encodeURIComponent(params.description);
      } */

      // Set default sort values if necessary
      if (angular.isDefined(params.filters) && !angular.isDefined(params.sortBy)) {
        if (type === 'donor') {
          data.sortBy = 'ssmAffectedGenes';
        } else if (type === 'gene') {
          data.sortBy = 'affectedDonorCountFiltered';
        } else {
          data.sortBy = 'affectedDonorCountFiltered';
        }
        data.sortOrder = 'DESCENDING';
      } else {
        data.sortBy = params.sortBy;
        data.sortOrder = params.sortOrder;
      }
      data.union = params.union;
      return data;
    }


    /*
    this.saveAll = function(lists) {
      localStorageService.set(LIST_ENTITY, lists);
    };
    */


    this.createAdvLink = function(set) {
      var type = set.type.toLowerCase(), filters = {};
      filters[type] = {};
      filters[type][Extensions.ENTITY] = {is: [set.id]};

      if (['gene', 'mutation'].indexOf(type) >= 0) {
        return '/search/' + type.charAt(0) + '?filters=' + angular.toJson(filters);
      } else {
        return '/search?filters=' + angular.toJson(filters);
      }

    };



    this.materialize = function(type, params) {
      var data = params2JSON(type, params);
      return Restangular.one('entityset').post('union', data, {}, {'Content-Type': 'application/json'});
    };


    /**
    * params.filters
    * params.sort
    * params.order
    * params.name
    * params.description - optional
    * params.count - limit (max 1000) ???
    *
    * Create a new set from
    */
    this.addSet = function(type, params) {
      var promise = null;
      var data = params2JSON(type, params);
      promise = Restangular.one('entityset').post(undefined, data, {}, {'Content-Type': 'application/json'});

      promise.then(function(data) {
        if (! data.id) {
          console.log('there is no id!!!!');
          return;
        }

        // If flagged as transient, don't save to local storage
        // FIXME: use subtype when it is available
        if (params.isTransient === true) {
          return;
        }

        data.type = data.type.toLowerCase();
        setList.splice(1, 0, data);
        localStorageService.set(LIST_ENTITY, setList);
        toaster.pop('', 'Saving ' + data.name,
          'View in <a href="/analysis/sets">Set Analysis</a>', 4000, 'trustedHtml');
      });

      return promise;
    };


    /**
    * params.union
    * params.name
    * params.description - optional
    *
    * Create a new set from the union of various subsets of the same type
    */
    this.addDerivedSet = function(type, params) {
      var promise = null;
      var data = params2JSON(type, params);

      promise = Restangular.one('entityset').post('union', data, {}, {'Content-Type': 'application/json'});
      promise.then(function(data) {
        if (! data.id) {
          console.log('there is an error in creating derived set');
          return;
        }

        data.type = data.type.toLowerCase();
        //setList.unshift(data);
        setList.splice(1, 0, data);
        localStorageService.set(LIST_ENTITY, setList);
        toaster.pop('', 'Saving ' + data.name,
          'View in <a href="/analysis/sets">Set Analysis</a>', 4000, 'trustedHtml');
      });

      return promise;
    };




    /*
     * Attemp to sync with the server - fires only once, up to controller to do polling
     */
    this.sync = function() {
      var pendingLists, pendingListsIDs, promise;

      pendingLists = _.filter(setList, function(d) {
        return d.state !== 'FINISHED';
      });
      pendingListsIDs = _.pluck(pendingLists, 'id');

      // No need to update
      if (pendingListsIDs.length === 0) {
        return;
      }

      promise = _this.getMetaData(pendingListsIDs);
      promise.then(function(updatedList) {
        updatedList.forEach(function(item) {
          var index = _.findIndex(setList, function(d) {
            return item.id === d.id;
          });
          if (index >= 0) {
            setList[index].count = item.count;
            setList[index].state = item.state;
          }
        });

        // Save update back
        localStorageService.set(LIST_ENTITY, setList);
        _this.refreshList();
      });
      return promise;
    };



    this.refreshList = function() {
      setList.forEach(function(set) {
        var filters = {};
        filters[set.type] = {};
        filters[set.type][Extensions.ENTITY] = {is: [set.id]};

        if (['gene', 'mutation'].indexOf(set.type) !== -1) {
          set.advLink = '/search/' + set.type.charAt(0) + '?filters=' + JSON.stringify(filters);
        } else {
          set.advLink = '/search?filters=' + JSON.stringify(filters);
        }
      });
    };


    // FIXME: Add cached version
    this.getMetaData = function( ids ) {
      return RestangularNoCache.several('entityset/sets', ids).get('', {});
    };

    this.lookupTable = function(metaData) {
      var map = {};
      metaData.forEach(function(d) {
        map[d.id] = d.name;
      });
      return map;
    };

    this.exportSet = function(id) {
      $window.location.href = API.BASE_URL + '/entityset/' + id + '/export';
    };


    /****** Local storage related API ******/
    this.getAll = function() {
      return setList;
    };

    this.getAllGeneSets = function() {
      return _.filter(setList, function(s) {
        return s.type === 'gene';
      });
    };

    this.initService = function() {
      setList = localStorageService.get(LIST_ENTITY) || [];

      // Reset everything to PENDNG
      setList.forEach(function(set) {
        set.state = 'PENDING';
      });

      _this.refreshList();

      return setList;
    };


    this.updateSets = function(sets) {
      sets.forEach(function(item) {
        var index = _.findIndex(setList, function(d) {
          return item.id === d.id;
        });
        if (index >= 0) {
          setList[index].count = item.count;
          setList[index].state = item.state;
        }
      });
      localStorageService.set(LIST_ENTITY, setList);
      _this.refreshList();
    };


    this.removeSeveral = function(ids) {
      _.remove(setList, function(list) {
        return ids.indexOf(list.id) >= 0;
      });
      localStorageService.set(LIST_ENTITY, setList);
      return true;
    };

    this.remove = function(id) {
      _.remove(setList, function(list) {
        return list.id === id;
      });
      localStorageService.set(LIST_ENTITY, setList);
      return true;
    };


    // Make sure the demo is in place
    this.initDemo = function() {
      var settingsPromise = Restangular.one('settings').get();

      function addDemo(demo) {
        demo.type = demo.type.toLowerCase();
        demo.readonly = true;

        // Check if already exist
        var exist = _.some(setList, function(set) {
          return set.id === demo.id;
        });
        if (exist === false) {
          setList.unshift(demo); // Demo always goes first
          localStorageService.set(LIST_ENTITY, setList);
        } else {
          setList[0] = demo; // Always overwrite demo in order to get updates
          localStorageService.set(LIST_ENTITY, setList);
        }
        // console.log(setList.length, setList);
      }

      settingsPromise.then(function(settings) {
        if (settings.hasOwnProperty('demoListUuid')) {
          var uuid = settings.demoListUuid;
          var demoPromise = _this.getMetaData([uuid]);

          demoPromise.then(function(results) {
            addDemo(results[0]);
            _this.refreshList();
          });
        }
      });

    };


    // Initialize
    var setList = _this.initService();
    _this.initDemo();
  });

})();

