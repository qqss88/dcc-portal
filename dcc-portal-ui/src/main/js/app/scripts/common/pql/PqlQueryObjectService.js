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

  var namespace = 'icgc.common.pqlqueryobject';
  var serviceName = 'PqlQueryObjectService';

  var module = angular.module(namespace, []);

  module.factory(serviceName, function (PqlTranslationService) {

    function convertPqlToQueryObject (pql) {
      pql = (pql || '').trim();

      if (pql.length < 1) {return {};}

      var jsonTree = PqlTranslationService.fromPql (pql);

      return fromJsonTreeToQueryObject (jsonTree);
    }

    function isNode (nodeName, node) {
      node = node || {};
      return (node.op || '') === nodeName;
    }

    var isAndNode = _.partial (isNode, 'and');

    function parseIdentifier (id) {
      var splits = (id || '').split ('.');
      var count = splits.length;

      var category = (count > 0) ? splits[0] : null;
      var facet = (count > 1) ? splits[1] : null;

      return (category && facet) ? {category: category, facet: facet} : null;
    }

    function processTreeNode (result, node) {
      if (! node) {return result;}

      // For our current need, we should only expect two operators, namely 'in' and 'eq'.
      var op = node.op || '';
      if (op !== 'in' && op !== 'eq') {return result;}

      var values = node.values || [];
      if (values.length < 1) {return result;}

      var identifier = parseIdentifier (node.field);
      if (! identifier) {return result;}

      var categoryName = identifier.category;
      var category = result [categoryName] || {};
      category [identifier.facet] = {in: values};
      result [categoryName] = category;

      return result;
    }

    function fromJsonTreeToQueryObject (jsonTree) {
      // Currently we expect jsonTree to be an array.
      var nodes = _.filter (jsonTree, isAndNode);
      var values = (nodes.length > 0) ? (nodes[0].values || []) : jsonTree;

      var result = _.reduce (values, processTreeNode, {});

      return result;
    }

    function removeEmptyObject (collection) {
      return _.filter (collection, function (o) {
        return o !== {};
      });
    }

    function toJsonTreeFromQueryObject (query) {
      var categoryKeys = Object.keys (query || {});

      if (categoryKeys.length < 1) {return [];}

      var termArray = _.map (categoryKeys, function (categoryKey) {
        var category = query [categoryKey];
        var facetKeys = Object.keys (category || {});

        var termFilters = _.map (facetKeys, function (facetKey) {
          var facet = category [facetKey] || {};
          var inArray = facet.in || [];
          var inArrayLength = inArray.length;

          if (inArrayLength > 0) {
            var op = (inArrayLength > 1) ? 'in' : 'eq';
            var field = '' + categoryKey + '.' + facetKey;

            return {op: op, field: field, values: inArray};
          } else {
            return {};
          }
        });

        return termFilters;
      });

      var values = removeEmptyObject (_.flatten (termArray));

      return (values.length > 1) ? [{op: 'and', values: values}] : values;
    }

    function addTermToQuery (categoryName, facetName, term, query) {
      if (! term) {return query;}

      query = query || {};
      var category = query [categoryName] || {};
      var facet = category [facetName] || {};
      var inValueArray = facet.in || [];

      if (_.contains (inValueArray, term)) {return query;}

      inValueArray.push (term);
    
      // update the original query object.
      facet.in = inValueArray;
      category [facetName] = facet;
      query [categoryName] = category;
    
      return query;
    }

    function removeTermFromQuery (categoryName, facetName, term, query) {
      var categoryKeys = Object.keys (query || {});

      if (_.contains (categoryKeys, categoryName)) {
        var facetKeys = Object.keys (query [categoryName] || {});
        var inField = 'in';

        if (_.contains (facetKeys, facetName)) {
          var inValueArray = query [categoryName][facetName][inField] || [];
          query [categoryName][facetName][inField] = _.remove (inValueArray, function (s) {
            return s !== term;
          });
            
          if (query [categoryName][facetName][inField].length < 1) {
            query = removeFacetFromQuery (categoryName, facetName, null, query);
          }
        }
      }

      return query;
    }

    function removeFacetFromQuery (categoryName, facetName, term, query) {
      var categoryKeys = Object.keys (query || {});

      if (_.contains (categoryKeys, categoryName)) {
        var facetKeys = Object.keys ( query [categoryName] || {} );

        if (_.contains (facetKeys, facetName)) {
          delete query [categoryName][facetName];
            
          if (Object.keys (query [categoryName] || {}).length < 1) {
            delete query [categoryName];
          }
        }
      }

      return query;
    }

    function convertQueryObjectToPql (queryObject) {
      var jsonTree = toJsonTreeFromQueryObject (queryObject);

      return PqlTranslationService.toPql (jsonTree);
    }

    function updateQuery (pql, categoryName, facetName, term, updators) {
      var query = convertPqlToQueryObject (pql);
      var updatedQuery = _.reduce (updators || [], function (result, f) {
        return _.isFunction (f) ? f (categoryName, facetName, term, result) : result;
      }, query);
      
      return convertQueryObjectToPql (updatedQuery);
    }

    return {
      addTerm: function (pql, categoryName, facetName, term) {
        return updateQuery (pql, categoryName, facetName, term, [addTermToQuery]);
      },
      removeTerm: function (pql, categoryName, facetName, term) {
        return updateQuery (pql, categoryName, facetName, term, [removeTermFromQuery]);
      },
      removeFacet: function (pql, categoryName, facetName) {
        return updateQuery (pql, categoryName, facetName, null, [removeFacetFromQuery]);
      },
      overwrite: function (pql, categoryName, facetName, term) {
        return updateQuery (pql, categoryName, facetName, term, [removeFacetFromQuery, addTermToQuery]);
      },
      getQuery: function (pql) {
        return convertPqlToQueryObject (pql);
      }
    };
  });
})();
