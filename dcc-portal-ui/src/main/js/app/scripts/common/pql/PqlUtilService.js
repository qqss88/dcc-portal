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

/*
 * This is the Angular service that provides lots of helper functions for PQL translations.
 * It hides the implementation details of PqlTranslationService and PqlQueryObjectService.
 */

(function () {
  'use strict';

  var namespace = 'icgc.common.pql.utils';
  var serviceName = 'PqlUtilService';

  var module = angular.module(namespace, []);

  module.factory(serviceName, function (PqlQueryObjectService, $location, $log) {
    // This is the parameter name for PQL in the URL query params.
    var pqlParameterName = 'query';
    var service = PqlQueryObjectService;

    // Here Pql is persisted in a query param in the URL.
    function getPql() {
      var search = $location.search();
      var pql = (search [pqlParameterName] || '').trim();

      $log.debug ('The URL contains this PQL: [%s].', pql);
      return pql;
    }

    // Retrieves pql persisted in a query param in the URL.
    function setPql (pql) {
      $location.search (pqlParameterName, pql);
      $log.debug ('PQL is updated to [%s].', pql);
    }

    function getSetPql () {
      var args = Array.prototype.slice.call (arguments);
      var func = _.head (args);
      var pql = func.apply (null, [getPql()].concat (_.tail (args)));
      setPql (pql);
    }

    // A builder to allow the UI to build a PQL programmatically.
    var Builder = function () {
      var buffer = '';

      return {
        addTerm: function (categoryName, facetName, term) {
          buffer = service.addTerm (buffer, categoryName, facetName, term);
          return this;
        },
        removeTerm: function (categoryName, facetName, term) {
          buffer = service.removeTerm (buffer, categoryName, facetName, term);
          return this;
        },
        removeFacet: function (categoryName, facetName) {
          buffer = service.removeFacet (buffer, categoryName, facetName);
          return this;
        },
        overwrite: function (categoryName, facetName, term) {
          buffer = service.overwrite (buffer, categoryName, facetName, term);
          return this;
        },
        includesFacets: function () {
          buffer = service.includesFacets (buffer);
          return this;
        },
        setLimit: function (limit) {
          buffer = service.setLimit (buffer, limit);
          return this;
        },
        setSort: function (sort) {
          buffer = service.setSort (buffer, sort);
          return this;
        },
        build: function () {
          return buffer;
        }
      };
    };

    return {
      paramName: pqlParameterName,
      reset: function () {
        setPql ('');
      },
      addTerm: function (categoryName, facetName, term) {
        getSetPql (service.addTerm, categoryName, facetName, term);
      },
      removeTerm: function (categoryName, facetName, term) {
        getSetPql (service.removeTerm, categoryName, facetName, term);
      },
      removeFacet: function (categoryName, facetName) {
        getSetPql (service.removeFacet, categoryName, facetName);
      },
      overwrite: function (categoryName, facetName, term) {
        getSetPql (service.overwrite, categoryName, facetName, term);
      },
      mergeQueries: function (query1, query2) {
        return service.mergeQueries (query1, query2);
      },
      mergePqls: function (pql1, pql2) {
        return service.mergePqls (pql1, pql2);
      },
      getSort: function () {
        return service.getSort (getPql());
      },
      getLimit: function () {
        return service.getLimit (getPql());
      },
      convertQueryToPql: service.convertQueryToPql,
      getFilters: function () {
        return service.getFilters (getPql());
      },
      convertPqlToQuery: service.convertPqlToQueryObject,
      includesFacets: function () {
        getSetPql (service.includesFacets);
      },
      setLimit: function (limit) {
        getSetPql (service.setLimit, limit);
      },
      setSort: function (sort) {
        getSetPql (service.setSort, sort);
      },
      getRawPql: getPql,
      getBuilder: function () {
        return new Builder();
      }
    };
  });
})();
