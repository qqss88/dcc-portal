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

(function() {
  'use strict';

  var module = angular.module('icgc.common.display', []);

  module.service('FiltersUtil', function(Extensions) {

    this.removeExtensions = function(filters) {
      if (filters.hasOwnProperty('gene')) {

        if (filters.gene.hasOwnProperty(Extensions.ENTITY)) {
          delete filters.gene[Extensions.ENTITY];
        }

        // delete filters.gene.inputGeneListId;
        if (_.isEmpty(filters.gene)) {
          delete filters.gene;
        }
      }
      return filters;
    };

    this.hasGeneListExtension = function(filters) {
      if (filters.hasOwnProperty('gene')) {
        if (filters.gene.hasOwnProperty(Extensions.ENTITY)) {
          return true;
        }
      }
      return false;
    };


    this.getGeneSetQueryType = function(type) {
      if (type === 'go_term') {
        return 'goTermId';
      } else if (type === 'curated_set') {
        return 'curatedSetId';
      } else if (type === 'pathway') {
        return 'pathwayId';
      }
      return 'geneSetId';
    };


    this.buildGeneSetFilterByType = function(type, geneSetIds) {
      var filter = {gene:{}};
      filter.gene[type] = {is: geneSetIds};
      return filter;
    };


    /*
    * Builds a model that is is similar in strcuture to filters param, augmented
    * with information for UI-display and UI-interactions
    */
    this.buildUIFilters = function(filters, entityIDMap) {
      var display = {};

      entityIDMap = entityIDMap || {};

      angular.forEach(filters, function(typeFilters, typeKey) {
        display[typeKey] = {};
        angular.forEach(typeFilters, function(facetFilters, facetKey) {
          var uiFacetKey = facetKey;

          // FIXME: no logic to handle "all" clause
          if (facetFilters.all) {
            return;
          }

          // Genelist expansion maps to gene id
          if (facetKey === Extensions.ENTITY) {
            uiFacetKey = 'id';
          }

          // Remap gene ontologies
          if (uiFacetKey === 'hasPathway') {
            var uiTerm = 'Reactome Pathways';
            uiFacetKey = 'pathwayId';

            if (! display[typeKey].hasOwnProperty(uiFacetKey)) {
              display[typeKey][uiFacetKey] = {};
              display[typeKey][uiFacetKey].is = [];
            }
            display[typeKey][uiFacetKey].is.unshift({
              term: uiTerm,
              controlTerm: undefined,
              controlFacet: facetKey,
              controlType: typeKey
            });
            return;
          }

          // Allocate terms
          if (! display[typeKey].hasOwnProperty(uiFacetKey)) {
            display[typeKey][uiFacetKey] = {};
            display[typeKey][uiFacetKey].is = [];
          }


          facetFilters.is.forEach(function(term) {
            var uiTerm = term, isPredefined = false;
            // if (typeKey === 'gene' && facetKey === Extensions.ENTITY) {
            if (facetKey === Extensions.ENTITY) {
              //uiTerm = 'Gene List';
              uiTerm = entityIDMap[term] || term;
              isPredefined = true;
            } else if (typeKey === 'gene' && facetKey === 'goTermId') {
              var predefinedGO = _.find(Extensions.GENE_SET_ROOTS, function(set) {
                return set.id === term && set.type === 'go_term';
              });
              if (predefinedGO) {
                uiTerm = predefinedGO.name;
                isPredefined = true;
              }
            } else if (typeKey === 'gene' && facetKey === 'curatedSetId') {
              var predefinedCurated = _.find(Extensions.GENE_SET_ROOTS, function(set) {
                return set.id === term && set.type === 'curated_set';
              });
              if (predefinedCurated) {
                uiTerm = predefinedCurated.name;
                isPredefined = true;
              }
            }

            // Extension terms goes first
            if (isPredefined) {
              display[typeKey][uiFacetKey].is.unshift({
                term: uiTerm,
                controlTerm: term,
                controlFacet: facetKey,
                controlType: typeKey
              });
            } else {
              display[typeKey][uiFacetKey].is.push({
                term: uiTerm,
                controlTerm: term,
                controlFacet: facetKey,
                controlType: typeKey
              });
            }

          });
        });
      });
      return display;
    };

  });




})();
