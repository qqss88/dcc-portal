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

(function () {
	'use strict';
	
	var module = angular.module('icgc.repositories', []);
	
	module
		.config(function ($stateProvider) {
			$stateProvider
				.state('cloud-partners', {
					url: '/cloud-partners',
					templateUrl: 'scripts/repositories/views/home.html',
					controller: 'RepositoriesHomeController'
				})
				.state('repositories', {
					url: '/cloud-partners/repositories/{repoCode}/guide',
					templateUrl: function ($stateParams) {
						return 'scripts/repositories/views/guides/' + $stateParams.repoCode + '.html';
					},
					controller: 'RepositoriesGuideController'
				});
		});
	
	module
		.controller('RepositoriesHomeController', function($scope, Page) {
			Page.stopWork();
			Page.setPage('entity');
			Page.setTitle('ICGC in the Cloud');
		})
		.controller('RepositoriesGuideController', function($scope, Page) {
			Page.stopWork();
			Page.setPage('entity');
			Page.setTitle('Repositories');
		});
	
})();