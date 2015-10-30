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
                EnrichmentData: ['$q', '$stateParams', 'Restangular',

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
	

	module.controller('PathwaysController', function($scope, $q, Page, EnrichmentData, Restangular,
		GeneSetService, GeneSetHierarchy, GeneSets, GeneSetVerificationService, TooltipText, LocationService,
    EnrichmentService) {
				
		
		var _ctrl = this,
        _selectedPathway = null;

	
		function _init() {
			Page.stopWork();
			Page.setPage('entity');
			Page.setTitle('Enrichment Analysis Pathway Viewer');
      
      $scope.TooltipText = TooltipText
			
			$scope.pathways = EnrichmentData.results;
			$scope.analysis = {
						getID: function() {
							return EnrichmentData.id;	
						},
						getData: function() {
							return EnrichmentData;
						},
						getContext: function() {
							return 'pathways';
						}
			};
			
			
			// Select the first gene set in the pathway as the
			// default value if one exists...
      var firstGenesetPathway = _.first($scope.pathways);
	
			if ( firstGenesetPathway ) {
				$scope.setSelectedPathway(firstGenesetPathway);
			}
			
		}
    
    function _addFilters(pathway) {
      
      if (_.get(pathway, 'geneSetFilters', false)) {
        return;
      }
      
      pathway.geneSetFilters = EnrichmentService.geneSetFilters(EnrichmentData, pathway);
      pathway.geneSetOverlapFilters = EnrichmentService.geneSetOverlapFilters(EnrichmentData, pathway);
     
    }
		
		$scope.getSelectedPathway = function() {
      return _selectedPathway;
    };

		$scope.setSelectedPathway = function(pathway) {			
			$scope.pathway = {};
      
      _addFilters(pathway);
      
			_selectedPathway = pathway; 
     
      var id = pathway.geneSetId;
      
      
      var _geneSet = null,
        _pathwayId = null,
        _parentPathwayId = null,
        _uiParentPathways = null,
        _uniprotIds = null,
        _xml = null,
        _zooms = [''],
        _pathwayHighlights = [];
        
        
        
        
       Restangular.one("genesets").one(id).get()
       .then(function(geneSet){
          _geneSet = geneSet;
          _uiParentPathways = GeneSetHierarchy.uiPathwayHierarchy(_geneSet.hierarchy, _geneSet);
          _geneSet.showPathway = true;
          _pathwayId = _uiParentPathways[0].diagramId;
          _parentPathwayId= _uiParentPathways[0].geneSetId;
       })
       .then(function() {
         var deferred = $q.defer();
        
         GeneSetService.getPathwayXML(_pathwayId)  
          .then(function(xml) {
              _xml = xml;
              deferred.resolve();
          }).catch(function() {
            $scope.pathway = {xml: '', zooms: [''], highlights: [] };
          });
          
         return deferred.promise;
       })
       .then(function() {
          var deferred = $q.defer();
          
         // If the diagram itself isnt the one being diagrammed, get list of stuff to zoom in on
          if(_pathwayId !== _parentPathwayId) {
            GeneSetService.getPathwayZoom(_parentPathwayId).then(function(data) {
                _zooms = data;
                 deferred.resolve();
            });
          } 
          else {
            _zooms = [''];
            deferred.resolve();
          }
          
          return deferred.promise;
       })
       .then(function() {
          var deferred = $q.defer();
         
   
          GeneSetService.getPathwayProteinMap(_parentPathwayId, []).then(function(map) {
            
              // Normalize into array
                  _.forEach(map,function(value,id) {
                    if(value && value.dbIds) {
                      _pathwayHighlights.push({
                        uniprotId:id,
                        dbIds:value.dbIds.split(','),
                        value:value.value
                      });
                    }
                  });
      
              // Get ensembl ids for all the genes so we can link to advSearch page
              _uniprotIds = _.pluck(_pathwayHighlights, 'uniprotId');
              deferred.resolve();
          });
           return deferred.promise;
       })
       .then(function() {
          var deferred = $q.defer();
          
          GeneSetVerificationService.verify(_uniprotIds.join(',') )
          .then(function(data) {
            _.forEach(_pathwayHighlights,function(n){
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
                    pathwayId: {is: [_parentPathwayId]}
                  }
                });
                n.geneSymbol = uniprotObj[0].symbol;
                n.geneId = ensemblId;
            });
            
            deferred.resolve();
          });
          
          return deferred.promise;
       }).
       then(function(){
         $scope.geneSet = _geneSet;
         $scope.pathway = {xml: _xml, zooms: _zooms, highlights: _pathwayHighlights };
         $scope.uiParentPathways = _uiParentPathways;
         //console.log($scope.pathway);
       });
 
		};	
		
		// Initialize our controller and it's corresponding scope.
		_init();
		
	});
})();