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
	
	var module = angular.module('icgc.pathways', ['icgc.enrichment.directives']);
	
	module.config(function($stateProvider) {
		$stateProvider.state('pathways', {
			url: '/pathways/:entityID',
			templateUrl: '/scripts/modules/pathwayviewer/views/pathways.html',
			controller: 'PathwaysController',
			resolve: {
                enrichmentData: ['$q', '$stateParams', 'Restangular',

                function($q, $stateParams, Restangular) {
                    var entityID = $stateParams.entityID,
                        deferred = $q.defer();

                    Restangular.one('analysis/enrichment', entityID).get()
                        .then(function(rectangularEnrichmentData) {
                            deferred.resolve(rectangularEnrichmentData.plain());      
						},
						function(response) {
							deferred.reject(response);
						}
						);          

                    return deferred.promise;
                }]
            }
		});
	});
	

	module.controller('PathwaysController', function($scope, Page, enrichmentData, Restangular,
		GeneSetService, GeneSetHierarchy, GeneSets, GeneSetVerificationService, LocationService) {
				
		
		var _ctrl = this;

		
		function _init() {
			Page.stopWork();
			Page.setPage('entity');
			Page.setTitle('Pathway Viewer');
			
			console.log(enrichmentData.results);
				
			$scope.pathways = enrichmentData.results;
			$scope.analysis = {
						getID: function() {
							return enrichmentData.id;	
						},
						getData: function() {
							return enrichmentData;
						},
						getContext: function() {
							return 'pathways';
						}
			};
			
			
			// Select the first gene set in the pathway as the
			// default value if one exists...
			var firstGeneSetID = _( _.first($scope.pathways) ).get('geneSetId');
			
			if ( firstGeneSetID ) {
				$scope.showPathway(firstGeneSetID);
			}
			
		}		
		
		

		$scope.showPathway = function(id) {			
			$scope.pathway = {};
			$scope.selectedPathwayId = id;
			
			Restangular.one("genesets").one(id).get().then(function (geneSet) {
				$scope.geneSet = geneSet;
				$scope.uiParentPathways = GeneSetHierarchy.uiPathwayHierarchy($scope.geneSet.hierarchy, $scope.geneSet);
				$scope.geneSet.showPathway = true;
				
				var pathwayId = $scope.uiParentPathways[0].diagramId;
				var parentPathwayId = $scope.uiParentPathways[0].geneSetId;
				
				// Get pathway XML
				GeneSetService.getPathwayXML(pathwayId).then(function(xml) {
					$scope.pathway.xml = xml;
				});
				
				// If the diagram itself isnt the one being diagrammed, get list of stuff to zoom in on
				if(pathwayId !== parentPathwayId) {
					GeneSetService.getPathwayZoom(parentPathwayId).then(function(data) {
							$scope.pathway.zooms = data;
					});
				} else {
					$scope.pathway.zooms = [''];
				}
	
				var mutationImpact = [];
				
				GeneSetService.getPathwayProteinMap(parentPathwayId, mutationImpact).then(function(map) {
				var pathwayHighlights = [], uniprotIds;
	
				// Normalize into array
				_.forEach(map,function(value,id) {
					if(value && value.dbIds) {
						pathwayHighlights.push({
							uniprotId:id,
							dbIds:value.dbIds.split(','),
							value:value.value
						});
					}
				});
	
				// Get ensembl ids for all the genes so we can link to advSearch page
				uniprotIds = _.pluck(pathwayHighlights, 'uniprotId');
				GeneSetVerificationService.verify( uniprotIds.join(',') ).then(function(data) {
					_.forEach(pathwayHighlights,function(n){
	
						var geneKey = 'external_db_ids.uniprotkb_swissprot';
						if (! data.validGenes[geneKey]) {
							return;
						}
	
						var uniprotObj = data.validGenes[geneKey][n.uniprotId];
						if(!uniprotObj){
							return;
						}
						var ensemblId = uniprotObj[0].id;
						n.advQuery =  LocationService.mergeIntoFilters({
							gene: {
								id:  {is: [ensemblId]},
								pathwayId: {is: [parentPathwayId]}
							}
						});
						n.geneSymbol = uniprotObj[0].symbol;
						n.geneId = ensemblId;
					});
					});
	
					$scope.pathway.highlights = pathwayHighlights;
				});
			});
		};	
		
		// Initialize our controller and it's corresponding scope.
		_init();
		
	});
})();