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
 * This is the Angular service that translates a parse tree of PQL into an object model to be
 * consumed by UI-centric code.
*/

(function () {
  'use strict';

  var namespace = 'icgc.common.pql.queryobject';
  var serviceName = 'PqlQueryObjectService';

  var module = angular.module(namespace, []);

  module.factory(serviceName, function (PqlTranslationService) {

    function getEmptyQueryObject () {
      return {
        params: {
          // For query object, we always enforce the 'select' function.
          // In fact, this is translated to 'select(*)' in PQL as we don't support
          // column projection from the UI.
          select: true,
          facets: false,
          sort: [],
          limit: {}
        },
        filters: {}
      };
    }

    function convertPqlToQueryObject (pql) {
      pql = (pql || '').trim();

      if (pql.length < 1) {return getEmptyQueryObject();}

      var jsonTree = PqlTranslationService.fromPql (pql);

      return convertJsonTreeToQueryObject (jsonTree);
    }

    function isNode (nodeName, node) {
      node = node || {};
      return (node.op || '') === nodeName;
    }

    var isAndNode = _.partial (isNode, 'and');
    var isSortNode = _.partial (isNode, 'sort');
    var isLimitNode = _.partial (isNode, 'limit');
    var isFacetsNode = _.partial (isNode, 'facets');

    function parseIdentifier (id) {
      var splits = (id || '').split ('.');
      var count = splits.length;

      var category = (count > 0) ? splits[0] : null;
      var facet = (count > 1) ? splits[1] : null;

      return (category && facet) ? {category: category, facet: facet} : null;
    }

    function reduceFilterArrayToQueryFilters (result, node) {
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

    function getSpecialNodeFromTreeArray (treeArray, filterFunc) {
      var nodes = _.filter (treeArray, filterFunc);
      return (nodes.length > 0) ? nodes [0] : null;
    }

    function removeOp (jsonObject) {
      delete (jsonObject || {}).op;
      return jsonObject;
    }

    function convertJsonTreeToQueryObject (treeArray) {
      var result = getEmptyQueryObject();

      // Currently we expect the input (treeArray) to be an array,
      // namely we don't support/expect 'count' yet.
      var andNode = getSpecialNodeFromTreeArray (treeArray, isAndNode);
      // For our current need, there should always be one 'And' node.
      var filterValues = andNode ? (andNode.values || []) : treeArray;

      result.filters = _.reduce (filterValues, reduceFilterArrayToQueryFilters, {});

      // Again, currently the UI doesn't care about projection on facets so we treat any 'facets' as 'facets(*)'
      result.params.facets = (null !== getSpecialNodeFromTreeArray (treeArray, isFacetsNode));

      var sortNode = getSpecialNodeFromTreeArray (treeArray, isSortNode);
      // The values field in a 'sort' node contains an array of sort fields.
      result.params.sort = sortNode ? (sortNode.values || []) : [];

      var limitNode = getSpecialNodeFromTreeArray (treeArray, isLimitNode);
      result.params.limit = limitNode ? removeOp (limitNode) : {};

      return result;
    }

    function removeEmptyObject (collection) {
      return _.filter (collection, function (o) {
        return (! _.isEqual (o, {}));
      });
    }

    function convertQueryObjectToJsonTree (query) {
      // Result should be an array because, for now, the UI does not need/support 'count'
      var result = [];
      var queryParams = query.params || {};

      if (queryParams.select) {
        result.push ({
          op: 'select',
          values: ['*']
        });
      }

      if (queryParams.facets) {
        result.push ({
          op: 'facets',
          values: ['*']
        });
      }

      result = result.concat (convertQueryFilterToJsonTree (query.filters));

      var sort = queryParams.sort || [];
      if (! _.isEmpty (sort)) {
        result.push ({
          op: 'sort',
          values: sort
        });
      }

      var limit = queryParams.limit || {};
      if (! _.isEmpty (limit)) {
        limit.op = 'limit';
        result.push (limit);
      }

      return result;
    }

    function convertQueryFilterToJsonTree (queryFilter) {
      var categoryKeys = Object.keys (queryFilter || {});

      if (categoryKeys.length < 1) {return [];}

      var termArray = _.map (categoryKeys, function (categoryKey) {
        var category = queryFilter [categoryKey];
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

    function addTermToQueryFilter (categoryName, facetName, term, queryFilter) {
      if (! term) {return queryFilter;}

      return addMultipleTermsToQueryFilter (categoryName, facetName, [term], queryFilter);
    }

    function addMultipleTermsToQueryFilter (categoryName, facetName, terms, queryFilter) {
      if (! _.isArray (terms)) {return queryFilter;}
      if (_.isEmpty (terms)) {return queryFilter;}

      queryFilter = queryFilter || {};
      var category = queryFilter [categoryName] || {};
      var facet = category [facetName] || {};
      var inValueArray = facet.in || [];

      _.each (terms, function (term) {
        if (term && ! _.contains (inValueArray, term)) {
          inValueArray.push (term);
        }
      });

      // update the original filter.
      facet.in = inValueArray;
      category [facetName] = facet;
      queryFilter [categoryName] = category;

      return queryFilter;
    }

    function removeTermFromQueryFilter (categoryName, facetName, term, queryFilter) {
      var categoryKeys = Object.keys (queryFilter || {});

      if (_.contains (categoryKeys, categoryName)) {
        var facetKeys = Object.keys (queryFilter [categoryName] || {});
        var inField = 'in';

        if (_.contains (facetKeys, facetName)) {
          var inValueArray = queryFilter [categoryName][facetName][inField] || [];
          queryFilter [categoryName][facetName][inField] = _.remove (inValueArray, function (s) {
            return s !== term;
          });

          if (queryFilter [categoryName][facetName][inField].length < 1) {
            queryFilter = removeFacetFromQueryFilter (categoryName, facetName, null, queryFilter);
          }
        }
      }

      return queryFilter;
    }

    function removeFacetFromQueryFilter (categoryName, facetName, term, queryFilter) {
      var categoryKeys = Object.keys (queryFilter || {});

      if (_.contains (categoryKeys, categoryName)) {
        var facetKeys = Object.keys (queryFilter [categoryName] || {});

        if (_.contains (facetKeys, facetName)) {
          delete queryFilter [categoryName][facetName];

          if (Object.keys (queryFilter [categoryName] || {}).length < 1) {
            delete queryFilter [categoryName];
          }
        }
      }

      return queryFilter;
    }

    function includesFacets (pql) {
      return updateQueryParam (pql, 'facets', true);
    }

    function convertQueryObjectToPql (queryObject) {
      var jsonTree = convertQueryObjectToJsonTree (queryObject);
      var result = PqlTranslationService.toPql (jsonTree);
      return result;
    }

    function updateQueryFilter (pql, categoryName, facetName, term, updators) {
      return updateQueryWithCustomAction (pql, function (query) {
        query.filters = _.reduce (updators || [], function (result, f) {
          return _.isFunction (f) ? f (categoryName, facetName, term, result) : result;
        }, query.filters);
      });
    }

    function cleanUpArguments (args, func) {
      var argumentArray = Array.prototype.slice.call (args);
      return _.isFunction (func) ? _.map (argumentArray, func) : argumentArray;
    }

    function mergeQueryObjects (queryArray) {
      var queryObjects = removeEmptyObject (queryArray);
      var numberOfQueries = queryObjects.length;

      if (numberOfQueries < 1) {return {};}

      return (numberOfQueries < 2) ? queryObjects [0] : _.reduce (queryObjects, _.merge, {});
    }

    function mergeQueries () {
      var emptyValue = {};
      var args = cleanUpArguments (arguments, function (o) {
        return _.isPlainObject (o) ? o : emptyValue;
      });

      return _.isEmpty (args) ? emptyValue : mergeQueryObjects (args);
    }

    function mergePqlStatements () {
      var emptyValue = '';
      var args = cleanUpArguments (arguments, function (s) {
        return _.isString (s) ? s.trim() : emptyValue;
      });

      var pqlArray = _.unique (_.filter (args, function (s) {
        return s !== emptyValue;
      }));

      var numberOfPql = pqlArray.length;

      if (numberOfPql < 1) {return emptyValue;}
      if (numberOfPql < 2) {return pqlArray [0];}

      var resultObject = mergeQueryObjects (_.map (pqlArray, convertPqlToQueryObject));

      return _.isEmpty (resultObject) ? '' :
        PqlTranslationService.toPql (convertQueryObjectToJsonTree (resultObject));
    }

    function updateQueryWithCustomAction (pql, action) {
      var query = convertPqlToQueryObject (pql);
      action (query);
      return convertQueryObjectToPql (query);
    }

    function updateQueryParam (pql, param, value) {
      return updateQueryWithCustomAction (pql, function (query) {
        query.params [param] = value;
      });
    }

    return {
      addTerm: function (pql, categoryName, facetName, term) {
        return updateQueryFilter (pql, categoryName, facetName, term, [addTermToQueryFilter]);
      },
      addTerms: function (pql, categoryName, facetName, terms) {
        return updateQueryFilter (pql, categoryName, facetName, terms, [addMultipleTermsToQueryFilter]);
      },
      removeTerm: function (pql, categoryName, facetName, term) {
        return updateQueryFilter (pql, categoryName, facetName, term, [removeTermFromQueryFilter]);
      },
      removeFacet: function (pql, categoryName, facetName) {
        return updateQueryFilter (pql, categoryName, facetName, null, [removeFacetFromQueryFilter]);
      },
      overwrite: function (pql, categoryName, facetName, term) {
        return updateQueryFilter (pql, categoryName, facetName, term,
          [removeFacetFromQueryFilter, _.isArray (term) ? addMultipleTermsToQueryFilter : addTermToQueryFilter]);
      },
      includesFacets: includesFacets,
      convertQueryToPql: convertQueryObjectToPql,
      convertPqlToQueryObject: convertPqlToQueryObject,
      mergePqls: mergePqlStatements,
      mergeQueries: mergeQueries,
      getSort: function (pql) {
        var query = convertPqlToQueryObject (pql);
        return query.params.sort;
      },
      setSort: function (pql, sortArray) {
        return updateQueryParam (pql, 'sort', sortArray);
      },
      getLimit: function (pql) {
        var query = convertPqlToQueryObject (pql);
        return query.params.limit;
      },
      setLimit: function (pql, limit) {
        return updateQueryParam (pql, 'limit', limit);
      },
      getFilters: function (pql) {
        var queryObject = convertPqlToQueryObject (pql);
        return queryObject.filters;
      }
    };
  });
})();
