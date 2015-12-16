angular.module('icgc.common.filters', [])
  .constant('filterConstants', {
    FILTER_NAME: 'filters',
    FILTER_EVENTS: {
      FILTER_UPDATE_EVENT: 'event.filter.updated'
    }
  })
  .service('FilterService', function($location, $rootScope, filterConstants) {
    var _service = this,
        _filtersObj = {};

    _service.filters = _filters;
    _service.removeFilters = _removeFilters;
    _service.getCachedFiltersFactory = _getCachedFiltersFactory;
    _service.filterParam = _filterParam;
    _service.constants = filterConstants;

    _init();


    ////////////////////////////////////////////////////////////////

    /**
     * Initializes the filter service
     * @private
     */
    function _init() {
      $rootScope.$watch(function () {
          return _.get($location.search(), filterConstants.FILTER_NAME, '');
        },
        function (newFilterJSON) {

          try {
            _filtersObj = JSON.parse(newFilterJSON);
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

        if (_.isObject(filters) && ! _.isEmpty(filters)) {
          _filtersObj = _.cloneDeep(filters);
        }
        else {
          _filtersObj = {};
        }
        //console.log('Update Filters to ', _filtersObj);
        _updateFilterParamsURL();
      }

      return _.cloneDeep(_filtersObj);
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
      var initialFilters = _filtersObj;

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


  });