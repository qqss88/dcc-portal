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

////////////////////////////////////////////////////////////////////////
// Primary Repository Module
////////////////////////////////////////////////////////////////////////
(function () {
	'use strict';
	
	var module = angular.module('icgc.repositories', [
		'icgc.repositories.controllers',
		'icgc.repositories.services'
	]);
	
	function _normalizeRepoCode(repoCode) {
		return repoCode.toLowerCase().replace(/[^\w]+/i, '.');
	}
	
	module
		.config(function ($stateProvider) {
			$stateProvider
				.state('ICGCcloud', {
						url: '/icgc-in-the-cloud',
						template: 	'<div data-ui-view="home"></div>' +
									'<div data-ui-view="repositoryGuides"></div>' +
									'<div data-ui-view="repos" class="cloud-repository-container"></div>',
						abstract: true			
				})
				.state('ICGCcloud.home', {
						url: '/',
						views: {
							'home': {
								templateUrl: 'scripts/repositories/views/home.html',
								controller: 'RepositoriesHomeController' 
							}
						}
				})
				.state('ICGCcloud.repositories', {
					url: '/repositories/{repoCode}/',
					// UI-Router only instantiates RepositoriesController once
					// which is good for us in this context 
					views: {
						'repos': {
							templateUrl: 'scripts/repositories/views/repos/repos.html',
							controller: 'RepositoriesController as repositoryCtrl'
						},
						'bodyContent@ICGCcloud.repositories': {
							templateUrl: function ($stateParams) {
								return 	'scripts/repositories/views/repos/repos.' + 
										_normalizeRepoCode($stateParams.repoCode) + '.content.html';
							},
							controller: 'RepositoriesController as repositoryCtrl'
						},
						'dataContent@ICGCcloud.repositories': {
							templateUrl: function ($stateParams) {
								return 	'scripts/repositories/views/repos/repos.' + 
										_normalizeRepoCode($stateParams.repoCode) + '.content.data.html';
							},
							controller: 'RepositoriesController as repositoryCtrl'
						}
					}
				})
				.state('ICGCcloud.repositoryGuides', {
					url: '/repositories/{repoCode}/guide',
					views: {
						'repositoryGuides': {
							templateUrl: function ($stateParams) {
								return 	'scripts/repositories/views/guides/' + 
										_normalizeRepoCode($stateParams.repoCode) + '.html';
							},
							controller: 'RepositoriesGuideController'
						}
					}
				});
				
		});
		
})();

////////////////////////////////////////////////////////////////////////
// Controller Declaration
////////////////////////////////////////////////////////////////////////
(function () {
	'use strict';
	
	var module = angular.module('icgc.repositories.controllers', []);
	console.log('instantiated!');
	module
		.controller('RepositoriesHomeController', function($scope, Page) {
			Page.stopWork();
			Page.setPage('entity');
			Page.setTitle('ICGC in the Cloud');
		})
		.controller('RepositoriesGuideController', function($scope, Page) {
			Page.stopWork();
			Page.setPage('entity');
			Page.setTitle('Repositories - User Guides');
		})
		.controller('RepositoriesController', function($scope, Page, $stateParams) {
			var _ctrl = this,
				_repoContext = $stateParams.repoCode.toLowerCase();
				
			function _capitalizeWords(str) {
				return str.replace(/[^\s]+/g, function(word) {
					return word.replace(/^[a-z]/i, function(firstLetter) {
						return firstLetter.toUpperCase();
					});
				});
			}
		
			
			function _init() {
				Page.stopWork();
				Page.setPage('entity');
				Page.setTitle('ICGC in the cloud - ' + _capitalizeWords(_repoContext) +  ' Repository');
			}
			
			// Initialize the controller
			_init();
			
			////////////////////////////////////////////////////////////////////////
			// Controller Public API
			////////////////////////////////////////////////////////////////////////
			_ctrl.getRepoContext = function() {
				return _repoContext;
			}; 
		});
	
})();

////////////////////////////////////////////////////////////////////////
// Services Declaration
////////////////////////////////////////////////////////////////////////
(function () {
	'use strict';
	
	var module = angular.module('icgc.repositories.services', []);
	
	module.service('RepositoriesService', function(/* Restangular, HighchartsService */) {
		
  	});
	
})();