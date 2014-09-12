/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

  var module = angular.module('icgc.facets.helpers', []);

  module.factory('Facets', function (LocationService) {

    function ensurePath(filters, params) {
      if (!filters.hasOwnProperty(params.type)) {
        filters[params.type] = {};
      }
      if (!filters[params.type].hasOwnProperty(params.facet)) {
        filters[params.type][params.facet] = {is: []};
      }
    }

    /*
     * TODO Set Terms
     */
    function setTerms(params) {
      var filters;

      // Check for required parameters
      ['type', 'facet', 'terms'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp) || !angular.isDefined(params[rp])) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();

      ensurePath(filters, params);

      // TODO make is possible to use 'is' or 'not'
      filters[params.type][params.facet].is = angular.isArray(params.terms) ? params.terms : [params.terms];
      LocationService.setFilters(filters);
    }

    /*
     * Add a Term
     */
    function addTerm(params) {
      var filters;

      // Check for required parameters
      [ 'type', 'facet', 'term'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();

      ensurePath(filters, params);

      // TODO make is possible to use 'is' or 'not'
      if (filters[params.type][params.facet].is.indexOf(params.term) === -1) {
        filters[params.type][params.facet].is.push(params.term);
      }

      LocationService.setFilters(filters);
    }

    /*
     * Remove a Term
     */
    function removeTerm(params) {
      var filters, index;

      // Check for required parameters
      [ 'type', 'facet', 'term'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();

      // TODO make is possible to use 'is' or 'not'
      index = filters[params.type][params.facet].is.indexOf(params.term);
      filters[params.type][params.facet].is.splice(index, 1);

      if (!filters[params.type][params.facet].is.length) {
        removeFacet(params);
      } else {
        LocationService.setFilters(filters);
      }
    }

    /*
     * Removes a facet
     */
    function removeFacet(params) {
      var filters;

      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();
      delete filters[params.type][params.facet];

      if (_.isEmpty(filters[params.type])) {
        delete filters[params.type];
      }

      if (_.isEmpty(filters)) {
        LocationService.removeFilters();
      } else {
        LocationService.setFilters(filters);
      }
    }

    /*
     * Removes all filters
     */
    function removeAll() {
      LocationService.removeFilters();
    }

    /*
     * TODO Toggle Term
     */
    function toggleTerm(params) {
      var filters;

      [ 'type', 'facet', 'term'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();
      console.info(filters);
      if (filters.hasOwnProperty(params.type) &&
          filters[params.type].hasOwnProperty(params.facet) &&
        // TODO support is/not
          filters[params.type][params.facet].is.indexOf(params.term) !== -1) {
        removeTerm(params);
      } else {
        addTerm(params);
      }
    }

    /*
     * Get list of active terms
     */
    function getActiveTerms(params) {
      var filters, list = [];

      [ 'type', 'facet', 'terms'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();

      function filterFn(active) {
        return _.findWhere(params.terms, function (term) {
          return term.term === active;
        }) || { term: active, count: 0};
      }

      if (filters.hasOwnProperty(params.type) && filters[params.type].hasOwnProperty(params.facet)) {
        // TODO make is possible to use 'is' or 'not'
        list = _.map(filters[params.type][params.facet].is, filterFn);
      }

      return list;
    }

    /*
     * Get list of inactive terms
     */
    function getInactiveTerms(params) {
      [ 'actives', 'terms'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      return _.difference(params.terms, params.actives);
    }

    /* Get a list of active tags */
    function getActiveTags(params) {
      var filters, list = [];

      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();

      if (filters.hasOwnProperty(params.type) && filters[params.type].hasOwnProperty(params.facet)) {
        // TODO make is possible to use 'is' or 'not'
        list = filters[params.type][params.facet].is;
      }

      return list;
    }

    /* Get a list of active tags */
    function getActiveLocations(params) {
      var filters, list = [];

      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();

      if (filters.hasOwnProperty(params.type) && filters[params.type].hasOwnProperty(params.facet)) {
        // TODO make is possible to use 'is' or 'not'
        list = filters[params.type][params.facet].is;
      }

      return list;
    }

    return {
      setTerms: setTerms,
      toggleTerm: toggleTerm,
      addTerm: addTerm,
      removeTerm: removeTerm,
      removeFacet: removeFacet,
      removeAll: removeAll,
      getActiveTerms: getActiveTerms,
      getInactiveTerms: getInactiveTerms,
      getActiveTags: getActiveTags,
      getActiveLocations: getActiveLocations
    };
  });

  module.directive('facetList', function () {
    return {
      restrict: 'A',
      replace: true,
      transclude: true,
      template: '<ul class="t_sh">' +
                '<li><ul ng-transclude></ul></li>' +
                '<li ng-if="show" class="t_sh__toggle">' +
                '<a ng-click="toggle()" href="" class="t_tools__tool">' +
                '<span ng-if="!expanded"><i class="icon-caret-down"></i> {{ more }} more</span>' +
                '<span ng-if="expanded"><i class="icon-caret-up"></i> less</span>' +
                '</a>' +
                '</li></ul>',
      link: function (scope, elem, attrs) {
        var previous, next, limit;

        // How many items to show in collapsed list
        limit = attrs.limit ? parseInt(attrs.limit, 10) : 1;

        function swap() {
          previous = [next, next = previous][0];
        }

        function list(value) {
          scope.list = value;
          // How many items are hidden
          scope.more = scope.list.length - limit;
          // If there is more than 1 item in the collapsed list show toggle
          scope.show = scope.more > 1;

          previous = scope.list;
          next = scope.show ? scope.list.slice(0, limit) : scope.list;

          // If list updates while expanded
          if (scope.expanded) {
            swap();
          }

          scope.list = next;
        }

        scope.toggle = function () {
          scope.expanded = !scope.expanded;
          swap();
          scope.list = next;
        };

        // Need to use observe instead of scope so list still
        // has access to parent scope events
        attrs.$observe('facetList', function (value) {
          if (value) {
            list(JSON.parse(value));
          }
        });
      }
    };
  });
})();
