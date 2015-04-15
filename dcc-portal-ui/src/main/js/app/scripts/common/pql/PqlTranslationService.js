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

  var namespace = 'icgc.common.pqltranslation';
  var serviceName = 'PqlTranslationService';

  var module = angular.module(namespace, []);

  module.factory(serviceName, function ($log) {

    var noNestingOperators = [
      "exists", "missing", "select", "facets"
    ];

    function addQuotes (s) {
      return '"' + s + '"';
    }

    function appendCommaIfNeeded (s) {
        return s.length > 0 ? ',' : '';
    };
    
    function opValuesToString (op, values) {
      return '' + op + '(' + values.join() + ')';
    }

    function convertNodeToPqlString (unit) {
      if (! _.isObject(unit)) {
        return _.isString (unit) ? addQuotes (unit) : '' + unit;
      }
    
      var op = unit.op;
      if ("limit" === op) {
        return limitUnitToPql (unit);
      } 
      if ("sort" === op) {
        return sortUnitToPql (unit);
      }

      var vals = unit.values || [];
      var values = _.contains (noNestingOperators, op) ? 
        vals.join() : 
        vals.map(convertNodeToPqlString).join();

      var parameters = unit.field || '';
    
      if (values.length > 0) {
        parameters += appendCommaIfNeeded(parameters) + values;
      }

      var ending = ("count" === op) ? 
        ')' + appendCommaIfNeeded (parameters) + parameters :
        parameters + ')'; 

      return '' + op + '(' + ending;
    }

    function limitUnitToPql (limit) {
      var from = limit.from || 0;
      var values = _.isNumber (limit.size) ? [from, limit.size] : [from];

      return opValuesToString (limit.op, values);
    }

    function sortUnitToPql (sort) {
      var values = sort.values.map (function (obj) {
        return '' + obj.direction + obj.field;
      });

      return opValuesToString (sort.op, values);
    }

    return {
      fromPql: function (pql) {
      	try {
          return PqlPegParser.parse (pql);
        } catch (e) {
          $log.error ("Error parsing PQL [%s] with error message: [%s]", pql, e.message);
          throw e;
        }
      },
      toPql: function (parsedTree) {
        var result = _.isArray (parsedTree) ? 
    	  parsedTree.map(convertNodeToPqlString).join() :
          _.isObject (parsedTree) ? convertNodeToPqlString (parsedTree) : null;

        if (result === null) {
        	$log.warn ("The input is neither an array nor an object: [%s]. toPql() is returning an empty string.", 
        	  JSON.stringify (parsedTree));
        	result = '';
        }

        return result;
      }
    };
  });
})();
