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

      if (isNot(params)) {
        if (filters[params.type][params.facet].not.indexOf(params.term) === -1) {
          filters[params.type][params.facet].not.push(params.term);
        }
      } else {
        if (filters[params.type][params.facet].is.indexOf(params.term) === -1) {
          filters[params.type][params.facet].is.push(params.term);
        }
      }

      LocationService.setFilters(filters);
    }
    
    /** 
     * Make the facet an IS NOT   
     */
    function notFacet(params) {
      var filters;
      
      // Check for required parameters
      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });
      
      filters = LocationService.filters();
      if (!filters.hasOwnProperty(params.type)) {
        filters[params.type] = {};
      }
      if (!filters[params.type].hasOwnProperty(params.facet)) {
        filters[params.type][params.facet] = {not: []};
      }
      if (filters[params.type][params.facet].hasOwnProperty('is')) {
        filters[params.type][params.facet] = {not: filters[params.type][params.facet].is};
        delete filters[params.type][params.facet].is;
      }

      LocationService.setFilters(filters);
    }
    
    /**
     * Remove the IS NOT
     */
    function isFacet(params) {
      var filters;
      
      // Check for required parameters
      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });
      
      filters = LocationService.filters();
      if (!filters.hasOwnProperty(params.type)) {
        filters[params.type] = {};
      }
      if (!filters[params.type].hasOwnProperty(params.facet)) {
        filters[params.type][params.facet] = {is: []};
      }
      if (filters[params.type][params.facet].hasOwnProperty('not')) {
        filters[params.type][params.facet] = {is: filters[params.type][params.facet].not};
        delete filters[params.type][params.facet].not;
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
      
      if (isNot(params)) {
        index = filters[params.type][params.facet].not.indexOf(params.term);
        filters[params.type][params.facet].not.splice(index, 1);
        if (!filters[params.type][params.facet].not.length) {
          removeFacet(params);
        } else {
          LocationService.setFilters(filters);
        }
      } else {
        index = filters[params.type][params.facet].is.indexOf(params.term);
        filters[params.type][params.facet].is.splice(index, 1);
        if (!filters[params.type][params.facet].is.length) {
          removeFacet(params);
        } else {
          LocationService.setFilters(filters);
        }
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

      if (! filters.hasOwnProperty(params.type)) {
        return;
      }

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
        return _.find(params.terms, function (term) {
          return term.term === active;
        }) || { term: active, count: 0};
      }

      if (filters.hasOwnProperty(params.type) && filters[params.type].hasOwnProperty(params.facet)) {
        // TODO make is possible to use 'is' or 'not'
        if (filters[params.type][params.facet].hasOwnProperty('is')) {
          list = _.map(filters[params.type][params.facet].is, filterFn);
        } else {
          list = _.map(filters[params.type][params.facet].not, filterFn);
        }
      }

      return list;
    }
    
    /**
     * Determine if this facet is an "IS NOT"
     */
    function isNot(params) { 
      var filters;

      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = LocationService.filters();
      
      return _.has(filters, params['type']+'.'+params['facet']+'.not');
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
        list = filters[params.type][params.facet].is || [];
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
      notFacet: notFacet,
      isFacet: isFacet,
      removeFacet: removeFacet,
      removeAll: removeAll,
      getActiveTerms: getActiveTerms,
      isNot: isNot,
      getInactiveTerms: getInactiveTerms,
      getActiveTags: getActiveTags,
      getActiveLocations: getActiveLocations
    };
  });

})();
