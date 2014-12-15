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

  var module = angular.module('icgc.facets.current', []);

  module.controller('currentCtrl', function ($scope, Facets, LocationService, FiltersUtil) {
    $scope.Facets = Facets;

    function refresh() {
      $scope.filters = FiltersUtil.buildUIFilters(LocationService.filters());
      $scope.isActive = _.keys($scope.filters).length;
    }


    /**
     * Proxy to Facets service, it does addtional handling of fields that behaves like
     * like facets but are structured in different ways
     */
    $scope.removeFacet = function(type, facet) {
      Facets.removeFacet({
        type: type,
        facet: facet
      });

      if (type === 'gene' && facet === 'id' && FiltersUtil.hasGeneListExtension(LocationService.filters())) {

        // FIXME: Grab facet keys from Extensions
        Facets.removeFacet({
          type: type,
          facet: 'inputGeneListId'
        });

        Facets.removeFacet({
          type: type,
          facet: 'uploadGeneListId'
        });
      }
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
    $scope.removeTerm = function(type, facet, term) {
      console.log('current remove term', type, facet, term);

      if (type === 'gene' && facet === 'hasPathway') {
        Facets.removeFacet({
          type: type,
          facet: facet
        });
      } else {
        Facets.removeTerm({
          type: type,
          facet: facet,
          term: term
        });
      }

    };

    refresh();
    $scope.$on('$locationChangeSuccess', function () {
      refresh();

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
