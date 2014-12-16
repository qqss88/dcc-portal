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

  module.controller('tagsFacetCtrl',
    function ($scope, Facets, LocationService, HighchartsService, FiltersUtil, Extensions, GeneSets, Genes) {

    $scope.projects = HighchartsService.projectColours;


    function setup() {
      var type = $scope.type, filters = LocationService.filters(), activeIds = [];

      // Remap logical types to index type
      if (_.contains(['go_term', 'pathway', 'curated_set'], type)) {
        type = 'gene';
      }

      $scope.actives = Facets.getActiveTags({
        type: type,
        facet: $scope.facetName
      });


      // Grab predefined geneset fields: each gene set type require specialized logic
      //   go has predefined Ids, searchable Ids, and Id counts
      //   pathway has predefined type, searchableIds, Id counts and type counts
      //   curated_set has predefined Ids and Id counts
      if ($scope.type === 'go_term') {
        $scope.predefinedGO = _.filter(Extensions.GENE_SET_ROOTS, function(set) {
          return set.type === 'go_term';
        });
        $scope.predefinedGOIds = _.pluck($scope.predefinedGO, 'id');

        activeIds = $scope.actives.concat($scope.predefinedGOIds);

        GeneSets.several(activeIds.join(',')).get('genes/counts', {filters: filters}).then(function(result) {
          $scope.GOIdCounts = result;
        });
      } else if ($scope.type === 'pathway') {
        var pathwayTypeFilters = {};

        if (filters.hasOwnProperty('gene') && filters.gene.hasOwnProperty('hasPathway')) {
          $scope.hasPathwayTypePredicate = true;
        } else {
          $scope.hasPathwayTypePredicate = false;
        }

        activeIds = $scope.actives;
        pathwayTypeFilters = LocationService.mergeIntoFilters({'gene':{'hasPathway':true}});

        Genes.handler.one('count').get({filters:pathwayTypeFilters}).then(function (result) {
          $scope.allPathwayCounts = result;
        });
        if (activeIds && activeIds.length > 0) {
          GeneSets.several(activeIds.join(',')).get('genes/counts', {filters: filters}).then(function(result) {
            $scope.pathwayIdCounts = result;
          });
        }
      } else if ($scope.type === 'curated_set') {
        $scope.predefinedCurated = _.filter(Extensions.GENE_SET_ROOTS, function(set) {
          return set.type === 'curated_set';
        });
        $scope.predefinedCuratedIds = _.pluck($scope.predefinedCurated, 'id');

        activeIds = $scope.predefinedCuratedIds;

        GeneSets.several(activeIds.join(',')).get('genes/counts', {filters: filters}).then(function(result) {
          $scope.curatedIdCounts = result;
        });
      }

      // Check if there are extended element associated with this facet
      // i.e. : GeneList is a subse of Gene
      $scope.hasExtension = false;
      if ($scope.type === 'gene') {
        if (FiltersUtil.hasGeneListExtension(filters)) {
          $scope.hasExtension = true;
        }
      }
    }

    $scope.addGeneSetType = function(type) {
      var filters = LocationService.filters();
      if (! filters.hasOwnProperty('gene')) {
        filters.gene = {};
      }
      filters.gene[type] = true;
      LocationService.setFilters(filters);
    };

    $scope.removeGeneSetType = function(type) {
      var filters = LocationService.filters();
      if (filters.hasOwnProperty('gene')) {
        delete filters.gene[type];
        if (_.isEmpty(filters.gene)) {
          delete filters.gene;
        }
      }
      LocationService.setFilters(filters);
    };


    $scope.addTerm = function (term) {
      var type, name;
      if (_.contains(['go_term', 'pathway', 'curated_set'], $scope.type)) {
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
      var type = $scope.type, filters = LocationService.filters();
      if (_.contains(['pathway', 'go_term', 'curated_set'], type)) {
        type = 'gene';
      }

      Facets.removeFacet({
        type: type,
        facet: $scope.facetName
      });

      if ($scope.type === 'gene' && FiltersUtil.hasGeneListExtension(filters) === true) {
        Facets.removeFacet({
          type: type,
          facet: 'inputGeneListId'
        });
      }

      if ($scope.type === 'pathway') {
        Facets.removeFacet({
          type: type,
          facet: 'hasPathway'
        });
      }
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
        if (attr.type === 'curated_set') {
          return 'scripts/facets/views/curatedtags.html';
        }
        return 'scripts/facets/views/tags.html';
      },
      controller: 'tagsFacetCtrl'
    };
  });
})();
