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

  var module = angular.module('icgc.common.location', []);

  module.factory('LocationService', function ($location, Notify) {

    return {
      path: function () {
        return $location.path();
      },

      search: function () {
        return $location.search();
      },

      clear: function () {
        return $location.search({});
      },

      // Returns the current filters unadorned by the expansion elements that would alter the semantics
      basicFilters: function() {
        var filters = this.getJsonParam('filters');
        if (filters.hasOwnProperty('gene') && filters.gene.hasOwnProperty('uploadedGeneList')) {
          delete filters.gene.uploadedGeneList;
          if (_.isEmpty(filters.gene)) {
            delete filters.gene;
          }
        }
        return filters;
      },

      // Returns the current filters
      filters: function () {
        return this.getJsonParam('filters');
      },

      setFilters: function (filters) {
        var search = this.search();

        // Clear all 'from' params. Pagination should be reset if filters change
        delete search.from;
        ['donors', 'genes', 'mutations', 'occurrences'].forEach(function (type) {
          var soType = search[type];
          if (soType) {
            var so = angular.fromJson(soType);
            delete so.from;
            if (_.isEmpty(so)) {
              delete search[type];
            } else {
              search[type] = angular.toJson(so);
            }

          }
        });

        search.filters = angular.toJson(filters);
        $location.search(search);
      },
      removeFilters: function () {
        this.removeParam('filters');
      },
      mergeIntoFilters: function (obj) {
        return this.merge(this.filters(), obj);
      },
      overwriteFilters: function (obj, level) {
        return this.merge(this.filters(), obj, level);
      },
      merge: function (obj1, obj2, overwriteAt) {
        var o1 = angular.copy(obj1);
        var o2 = angular.copy(obj2);

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
      },
      getJsonParam: function (param) {
        try {
          return angular.fromJson(this.search()[param]) || {};
        } catch (e) {
          Notify.setMessage('Cannot parse: ' + param + '=' + this.search()[param] );
          Notify.showErrors();
        }
      },
      setJsonParam: function (param, data) {
        var s = $location.search();
        s[param] = angular.toJson(data);
        $location.search(s);
      },
      getParam: function (param) {
        return this.search()[param];
      },
      setParam: function (param, data) {
        var s = $location.search();
        s[param] = data;
        $location.search(s);
      },
      removeParam: function (param) {
        var s = this.search();
        delete s[param];
        $location.search(s);
      },

      hasGeneList: function() {
        var filters = this.getJsonParam('filters');
        if (filters.hasOwnProperty('gene')) {
          if (filters.gene.hasOwnProperty('uploadedGeneList')) {
            return true;
          }
        }
        return false;
      },

      getUIDisplayFilters: function() {
        var display = {}, filters = this.filters();

        angular.forEach(filters, function(typeFilters, typeKey) {
          display[typeKey] = {};
          angular.forEach(typeFilters, function(facetFilters, facetKey) {
            var uiFacetKey = facetKey;

            // Genelist expansion maps to gene id
            if (typeKey === 'gene' && (facetKey === 'id' || facetKey === 'uploadedGeneList')) {
              uiFacetKey = 'id';
            }

            // Allocate terms
            if (! display[typeKey].hasOwnProperty(uiFacetKey)) {
              display[typeKey][uiFacetKey] = {};
              display[typeKey][uiFacetKey].is = [];
            }

            facetFilters.is.forEach(function(term) {
              var uiTerm = term;
              if (typeKey === 'gene' && facetKey === 'uploadedGeneList') {
                uiTerm = 'Gene List'
              }

              if (facetKey === 'uploadedGeneList') {
                display[typeKey][uiFacetKey].is.unshift({
                  term: uiTerm,
                  controlTerm: term,
                  controlFacet: facetKey,
                  controlType: typeKey
                });
              } else {
                display[typeKey][uiFacetKey].is.push({
                  term: uiTerm,
                  controlTerm: term,
                  controlFacet: facetKey,
                  controlType: typeKey
                });
              }

            });
          });
        });
        return display;
      }

    };
  });
})();
