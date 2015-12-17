'use strict';

angular.module('icgc.common.filters', [])
  .constant('filterConstants', {
    FILTER_NAME: 'filters',
    FILTER_EVENTS: {
      FILTER_UPDATE_EVENT: 'event.filter.updated'
    }
  })
  .service('FilterService', function($location, $rootScope, filterConstants) {
    /**
     * Initializes the filter service
     * @private
     */
    function _init() {
      $rootScope.$watch(function () {
          return _.get($location.search(), filterConstants.FILTER_NAME, '{}');
        },
        function (newFilterJSON) {

          try {
            _filtersObj = JSON.parse(newFilterJSON);
            //console.log('Watcher Update Filters to ', _filtersObj);
          }
          catch (e) {
            _filtersObj = {};
          }

          _notifyFilterChangeEvent();

        });
    }

    /**
     *
     * @param filters
     * @returns {copy of _filtersObj}
     * @private
     */
    function _filters(filters) {

      if (arguments.length === 1) {

        if (_.isObject(filters)) {
          _filtersObj = _.cloneDeep(filters);
        }
        else {
          _filtersObj = {};
        }

        //console.log('Update Filters to ', _filtersObj);
        _updateFilterParamsURL();
      }

      //console.log('Return Filters ', _filtersObj);
      return _.cloneDeep(_filtersObj); // Do not allow overwriting the original object!
    }

    /**
     * Removes all filters
     * @returns {*}
     * @private
     */
    function _removeFilters() {
      return _filters(null);
    }

    /**
     * Setter/Getter for a single filter parameter
     * @param filterKey
     * @param filterVal
     * @returns {_filtersObj[filterKey]|null}
     * @private
     */
    function _filterParam(filterKey, filterVal) {

      if (arguments.length === 2) {
        _filtersObj[filterKey] = filterVal;
        _updateFilterParamsURL();
      }

      return _filtersObj[filterKey] || null;
    }

    /**
     * Updates the url on filter based on the current state of the filter object
     * @private
     */
    function _updateFilterParamsURL() {
      var filterVal = null;

      if (! _.isEmpty(_filtersObj)) {
        filterVal = JSON.stringify(_filtersObj);
      }

      $location.search(filterConstants.FILTER_NAME, filterVal);
    }

    /**
     * Return a factory object which can be used to return a cached version of the filters - useful for long running
     * http requests that require multiple requests over a period of time.
     * @returns {{_cachedFilters: {}, filters: filterService.filters, updateCache: filterService.updateCache}}
     * @private
     */
    function _getCachedFiltersFactory() {
      var initialFilters = _filters();

      return {
          _cachedFilters: initialFilters,
          filters: function(filterObj) {

            if (arguments.length === 0) {
              return _.cloneDeep(this._cachedFilters);
            }
            else {
              this._cachedFilters = filterObj;
            }

            return this._cachedFilters;
        },
        updateCache: function() {
          //console.log('Updating cache from ', this._cachedFilters, ' to ', _filtersObj);
          this.filters(_filtersObj);
        }
      };
    }

    /**
     * Notifies observers of a filter change event.
     * @private
     */
    function _notifyFilterChangeEvent() {
      var filterNotifyObj = {
        currentFilters: _.cloneDeep(_filtersObj),
        currentSearchParams: $location.search(),
        currentPath: $location.path()
      };

      //console.log('Filter Change ', filterNotifyObj);
      $rootScope.$broadcast(filterConstants.FILTER_EVENTS.FILTER_UPDATE_EVENT, filterNotifyObj);
    }

    function _overwriteFiltersAtObjectLevel(obj, level) {
      return _merge(_filtersObj, obj, level); // Return a copy of the filters
    }

    function _mergeIntoFilters(obj) {
      return _merge(_filtersObj, obj); // Return a copy of the filters
    }

    function _merge(obj1, obj2, overwriteAt) {
      // Do not modify the original object - since we are modifying this object do a deep copy
      var o1 = _.cloneDeep(obj1),
          o2 = _.clone(obj2);

      function bools(type, facet) {
        for (var bool in o2[type][facet]) {
          if (o2[type][facet].hasOwnProperty(bool) &&
              (!o1[type][facet].hasOwnProperty(bool) || overwriteAt === 'bool')) {
            o1[type][facet][bool] = o2[type][facet][bool];
          }
        }
      }

      function facets(type) {
        for (var facet in o2[type]) {
          if (o2[type].hasOwnProperty(facet) && (!o1[type].hasOwnProperty(facet) || overwriteAt === 'facet')) {
            o1[type][facet] = o2[type][facet];
          } else {
            bools(type, facet);
          }
        }
      }

      for (var type in o2) {
        if (o2.hasOwnProperty(type) && (!o1.hasOwnProperty(type) || overwriteAt === 'type')) {
          o1[type] = o2[type];
        } else {
          facets(type);
        }
      }

      return o1;
    }

    /////////////////////////////////////////////////////
    var _service = this,
      _filtersObj = {};

    _service.filters = _filters;
    _service.removeFilters = _removeFilters;
    _service.getCachedFiltersFactory = _getCachedFiltersFactory;
    _service.filterParam = _filterParam;
    _service.mergeIntoFilters = _mergeIntoFilters;
    _service.overwriteFiltersAtObjectLevel = _overwriteFiltersAtObjectLevel;
    _service.merge = _merge;
    _service.constants = filterConstants;


    ////////////////////////////////////////////////////////////////

    _init();

  });