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
	
	module.controller('PathwaysController', function($scope, Page, enrichmentData) {
				
		
		
		
		function _init() {
			Page.stopWork();
			Page.setPage('entity');
			Page.setTitle('Pathway Viewer');
			console.log(enrichmentData.results);
			$scope.pathways = enrichmentData.results;
			$scope.analysis = {
						getData: function() {
							return enrichmentData;
						},
						getContext: function() {
							return 'pathways';
						}
			};
		}
		
		_init();
		
	});
})();