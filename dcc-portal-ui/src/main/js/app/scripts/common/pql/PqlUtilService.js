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

  var namespace = 'icgc.common.pqlutils';
  var serviceName = 'PqlUtilService';

  var module = angular.module(namespace, []);

  module.factory(serviceName, function (PqlQueryObjectService, $location, $log) {
    // This is the parameter name for PQL in the URL query params.
    var pqlParameterName = "query";

    function getPql() {
      var search = $location.search();
      var pql = search [pqlParameterName] || '';

      $log.debug ("The URL contains this PQL: [%s].", pql);
      return pql;
    }

    function setPql (pql) {
      $log.debug ("PQL is updated to [%s].", pql);
      $location.search (pqlParameterName, pql);      
    }

    var builder = function () {
      var buffer = '';

      return {
        addTerm: function (categoryName, facetName, term) {
          buffer = PqlQueryObjectService.addTerm (buffer, categoryName, facetName, term);
          return this;
        },
        removeTerm: function (categoryName, facetName, term) {
          buffer = PqlQueryObjectService.removeTerm (buffer, categoryName, facetName, term);
          return this;
        },
        removeFacet: function (categoryName, facetName) {
          buffer = PqlQueryObjectService.removeFacet (buffer, categoryName, facetName);	
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
        var pql = PqlQueryObjectService.addTerm (getPql(), categoryName, facetName, term);
        setPql (pql);
      },
      removeTerm: function (categoryName, facetName, term) {
        var pql = PqlQueryObjectService.removeTerm (getPql(), categoryName, facetName, term);
        setPql (pql);
      },
      removeFacet: function (categoryName, facetName) {
        var pql = PqlQueryObjectService.removeFacet (getPql(), categoryName, facetName);	
        setPql (pql);
      },
      overwrite: function (categoryName, facetName, term) {
        var pql = PqlQueryObjectService.overwrite (getPql(), categoryName, facetName, term);
        setPql (pql);
      },
      getQuery: function () {
        return PqlQueryObjectService.getQuery (getPql());
      },
      getRawPql: function () {
        return getPql();
      },
      getBuilder: function () {
      	return new builder();
      }
    };
  });
})();
