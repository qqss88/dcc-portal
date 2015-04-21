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
      var pql = search [pqlParameterName] || '';

      $log.debug ('The URL contains this PQL: [%s].', pql);
      return pql;
    }

    // Retrieves pql persisted in a query param in the URL.
    function setPql (pql) {
      $log.debug ('PQL is updated to [%s].', pql);
      $location.search (pqlParameterName, pql);
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
        build: function () {
          return buffer;
        }
      };
    };

    return {
      paramName: pqlParameterName,
      addTerm: function (categoryName, facetName, term) {
        var pql = service.addTerm (getPql(), categoryName, facetName, term);
        setPql (pql);
      },
      removeTerm: function (categoryName, facetName, term) {
        var pql = service.removeTerm (getPql(), categoryName, facetName, term);
        setPql (pql);
      },
      removeFacet: function (categoryName, facetName) {
        var pql = service.removeFacet (getPql(), categoryName, facetName);
        setPql (pql);
      },
      overwrite: function (categoryName, facetName, term) {
        var pql = service.overwrite (getPql(), categoryName, facetName, term);
        setPql (pql);
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
      convertPqlToQuery: service.getQuery,
      getQuery: function () {
        return service.getQuery (getPql());
      },
      getRawPql: getPql,
      getBuilder: function () {
        return new Builder();
      }
    };
  });
})();
