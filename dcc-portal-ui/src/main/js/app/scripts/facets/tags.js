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
    function ($scope, $modal, Facets, LocationService, HighchartsService, FiltersUtil,
      Extensions, GeneSets, Genes, GeneSetNameLookupService, SetService ) {

    $scope.Extensions = Extensions;

    var _fetchNameForSelections = function ( selections ) {

      if ( selections ) {
        [ 'goTermId', 'pathwayId' ].forEach ( function (s) {
          if ( selections [s] ) {
            GeneSetNameLookupService.batchFetch ( selections[ s ].is );
          }
        });
      }
    };

    // This function is called by tags.html to prevent the File input box in
    // External Repo File page from displaying the "Uploaded donor set" label.
    $scope.shouldDisplayEntitySetId = function () {
      return $scope.type !== 'file' && $scope.facetName !== 'id';
    };

    function setup() {
      var type = $scope.proxyType || $scope.type, filters = LocationService.filters(), activeIds = [];

      _fetchNameForSelections ( filters.gene );

      $scope.actives = Facets.getActiveTags({
        type: type,
        facet: $scope.proxyFacetName || $scope.facetName
      });

      // There are only 'active' entity ids
      $scope.activeEntityIds = Facets.getActiveTags({
        type: type,
        facet: Extensions.ENTITY
      });

      // Fetch display names for entity lists
      $scope.entityIdMap = {};

      if ($scope.activeEntityIds.length > 0) {
        SetService.getMetaData ($scope.activeEntityIds).then (function (results) {
          $scope.entityIdMap = SetService.lookupTable (results);
        });
      }


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
        if (filters.hasOwnProperty('gene') && filters.gene.hasOwnProperty('pathwayId')) {
          pathwayTypeFilters = LocationService.filters();
        } else {
          pathwayTypeFilters = LocationService.mergeIntoFilters({'gene':{'hasPathway':true}});
        }

        Genes.handler.one('count').get({filters:pathwayTypeFilters}).then(function (result) {
          $scope.allPathwayCounts = result || 0;
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

    var _captureTermInfo = function ( term ) {
      if ( ! term ) { return; }

      var _type = term.type;
      var _id = term.id;
      var _name = term.name;

      var isGeneSet = function () {
        return _.contains ( ['go_term', 'pathway'], _type );
      };

      if ( isGeneSet () ) {
        GeneSetNameLookupService.put ( _id, _name );
      }
    };

    $scope.addTerm = function (term) {

      _captureTermInfo ( term );

      var type = $scope.proxyType? $scope.proxyType : $scope.type;
      var facet = $scope.proxyFacetName? $scope.proxyFacetName: $scope.facetName;

      Facets.addTerm({
        type: type,
        facet: facet,
        term: term.id
      });
    };

    $scope.removeTerm = function (term) {
      var type = $scope.proxyType? $scope.proxyType : $scope.type;
      var facet = $scope.proxyFacetName? $scope.proxyFacetName: $scope.facetName;

      Facets.removeTerm({
        type: type,
        facet: facet,
        term: term
      });
    };

    /* Used for special cases where the relation is one-to-many instead of one-to-one */
    $scope.removeSpecificTerm = function(type, facet, term) {
      // Special remapping for the 'Upload Donor Set' control in the External Repository File page.
      if ('file-donor' === type && Extensions.ENTITY === facet) {
        type = 'file';
      }

      Facets.removeTerm({
        type: type,
        facet: facet,
        term: term
      });
    };

    $scope.removeFacet = function () {
      var type = $scope.proxyType? $scope.proxyType : $scope.type;
      var facet = $scope.proxyFacetName? $scope.proxyFacetName: $scope.facetName;

      Facets.removeFacet({
        type: type,
        facet: facet
      });

      // Remove secondary facet - entity
      if (_.contains(['gene', 'donor', 'mutation'], type) === true && $scope.facetName === 'id') {
        Facets.removeFacet({
          type: type,
          facet: Extensions.ENTITY
        });
      }

      if ($scope.type === 'pathway') {
        Facets.removeFacet({
          type: type,
          facet: 'hasPathway'
        });
      }

      if ('file' === type && facet === 'donorId') {
        Facets.removeFacet({
          type: type,
          facet: Extensions.ENTITY
        });
      }
    };


    /* Add a gene set term to the search filters */
    $scope.addGeneSet = function() {
      $modal.open({
        templateUrl: '/scripts/genelist/views/upload.html',
        controller: 'GeneListController'
      });
    };

    $scope.uploadDonorSet = function() {
      $modal.open({
        templateUrl: '/scripts/donorlist/views/upload.html',
        controller: 'DonorListController'
      });
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
        placeholder: '@',

        proxyType: '@',
        proxyFacetName: '@'
      },
      templateUrl: function (elem, attr) {
        var path_ = function (s) {
          return 'scripts/facets/views/' + s + '.html';
        };
        var type = attr.type;

        if (type === 'go_term') {
          return path_ ('gotags');
        }
        if (type === 'pathway') {
          return path_ ('pathwaytags');
        }
        if (type === 'curated_set') {
          return path_ ('curatedtags');
        }

        var facetName = attr.facetName;

        if (type === 'gene' && facetName === 'id') {
          return path_ ('genetags');
        }
        if (_.contains (['donor', 'file-donor'], type) && facetName === 'id') {
          return path_ ('donorfacet');
        }

        return path_ ('tags');
      },
      controller: 'tagsFacetCtrl'
    };
  });
})();
