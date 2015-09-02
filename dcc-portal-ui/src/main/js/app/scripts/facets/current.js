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

  var module = angular.module ('icgc.facets.current', []);

  module.controller('currentCtrl',
    function ($scope, Facets, LocationService, FiltersUtil, Extensions, SetService, Page) {

    $scope.Page = Page;
    $scope.Facets = Facets;
    $scope.Extensions = Extensions;

    function refresh() {
      var currentFilters = LocationService.filters();
      var ids = LocationService.extractSetIds(currentFilters);

      if (ids.length > 0) {
        SetService.getMetaData(ids).then(function(results) {
          $scope.filters = FiltersUtil.buildUIFilters(currentFilters, SetService.lookupTable(results));
        });
      } else {
        $scope.filters = FiltersUtil.buildUIFilters(currentFilters, {});
      }

      //$scope.isActive = _.keys($scope.filters).length;
      $scope.isActive = _.keys(currentFilters).length;
    }

    /**
     * Proxy to Facets service, it does addtional handling of fields that behaves like
     * like facets but are structured in different ways
     */
    $scope.removeFacet = function (type, facet) {
      // Remove primary facet
      Facets.removeFacet({
        type: type,
        facet: facet
      });

      // Remove secondary facet - entity
      if (_.contains(['gene', 'donor', 'mutation'], type) === true && facet === 'id') {
        Facets.removeFacet({
          type: type,
          facet: Extensions.ENTITY
        });
      }

      if ('file' === type && facet === 'donorId') {
        Facets.removeFacet({
          type: type,
          facet: Extensions.ENTITY
        });
      }

      // Remove secondary facet - existing conditions
      if (type === 'gene' && facet === 'pathwayId') {
        Facets.removeFacet({
          type: type,
          facet: 'hasPathway'
        });
      }

    };

    /**
     * Proxy to Facets service, it does addtional handling of fields that behaves like
     * like facets but are structured in different ways
     */
    $scope.removeTerm = function (type, facet, term) {
      if (type === 'gene' && facet === 'hasPathway') {
        Facets.removeFacet({
          type: type,
          facet: facet
        });
      } else {
        if ('file' === type && 'donorId' === facet && term === 'Uploaded donor set') {
          facet = Extensions.ENTITY;
        }

        Facets.removeTerm({
          type: type,
          facet: facet,
          term: term
        });
      }

    };

    refresh();
    $scope.$on('$locationChangeSuccess', function (evt, next) {
      // FIXME: Only applicable on search page. Should have a cleaner solution
      if (next.indexOf('search') !== -1 || next.indexOf('projects') !== -1 || next.indexOf('repository/external') ) {
        refresh();
      }
    });

  });

  module.directive('current', function () {
    return {
      restrict: 'E',
      templateUrl: '/scripts/facets/views/current.html',
      controller: 'currentCtrl'
    };
  });

})();
