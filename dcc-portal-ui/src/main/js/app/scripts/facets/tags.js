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

  var module = angular.module('icgc.facets.tags', ['icgc.ui.suggest']);

  module.controller('tagsFacetCtrl', function ($scope, Facets, LocationService, HighchartsService, FiltersUtil, Extensions) {
    $scope.projects = HighchartsService.projectColours;

    function setup() {
      var type = $scope.type;

      // Remap logical types to index type
      if (_.contains(['go_term', 'pathway', 'curated_set'], type)) {
        type = 'gene';
      }

      $scope.actives = Facets.getActiveTags({
        type: type,
        facet: $scope.facetName
      });

      console.log('actives', $scope.type, $scope.actives);

      // Check if there are extended element associated with this facet
      // i.e. : GeneList is a subse of Gene
      $scope.hasExtension = false;
      if ($scope.type === 'gene') {
        if (FiltersUtil.hasGeneListExtension( LocationService.filters())) {
          $scope.hasExtension = true;
        }
      }
    }

    $scope.predefinedGO = Extensions.GENE_ONTOLOGY_ROOTS;
    $scope.predefinedGOIds = Extensions.GENE_ONTOLOGY_ROOTS.map(function(go) {
      return go.id;
    });


    $scope.addTerm = function (term) {
      var type, name;


      if ($scope.type === 'pathway') {
        type = 'gene';
        name = 'id';
      } else if ($scope.type === 'go_term') {
        type = 'gene';
        name = 'id';
      } else if ($scope.type === 'curated_set') {
        type = 'gene';
        name = 'id';
      } else {
        type = $scope.type;
        name = $scope.facetName;
      }

      Facets.addTerm({
        type: type,
        facet: $scope.facetName,
        term: term[name]
      });
    };

    $scope.removeTerm = function (term) {
      var type = $scope.type;
      if (_.contains(['pathway', 'go_term', 'curated_set'], type)) {
        type = 'gene';
      }

      Facets.removeTerm({
        type: type,
        facet: $scope.facetName,
        term: term
      });
    };

    $scope.removeFacet = function () {
      // var type = $scope.type === 'pathway' ? 'gene' : $scope.type;
      var type = $scope.type;
      if (_.contains(['pathway', 'go_term', 'curated_set'], type)) {
        type = 'gene';
      }

      Facets.removeFacet({
        type: type,
        facet: $scope.facetName
      });

      if ($scope.type === 'gene' && FiltersUtil.hasGeneListExtension(LocationService.filters()) === true) {
        Facets.removeFacet({
          type: type,
          facet: 'uploadedGeneList'
        });
      }

      // TODO: Reset pathways, reset goterms, reset curated

    };

    // Needed if term removed from outside scope
    $scope.$watch(function () {
      return LocationService.filters();
    }, function (n, o) {
      if (n === o) {
        return;
      }
      setup();
    }, true);

    setup();
  });

  module.directive('tagsFacet', function () {
    return {
      restrict: 'A',
      scope: {
        facetName: '@',
        label: '@',
        type: '@',
        example: '@',
        placeholder: '@'
      },
      templateUrl: function(elem, attr) {

        if (attr.type === 'gene') {
          if (attr.facetName === 'id') {
            return 'scripts/facets/views/genetags.html';
          }
        }
        if (attr.type === 'go_term') {
          return 'scripts/facets/views/gotags.html';
        }
        if (attr.type === 'pathway') {
          return 'scripts/facets/views/pathwaytags.html';
        }
        return 'scripts/facets/views/tags.html';
      },
      controller: 'tagsFacetCtrl'
    };
  });
})();
