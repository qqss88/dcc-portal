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


 /**
  * This module handles CRUD operations of accessibility tokens. These tokens are used in other projects
  * such as Collaboratory, and is thus technically speaking, has no influence on the ICGC Data Portal.
  *
  * Note: These token functions are only visible to users whom have logged in through the Data Portal.
  */

(function() {
  'use strict';

  angular.module('icgc.tokens', ['icgc.tokens.controllers', 'icgc.tokens.services']);

})();

(function() {
  'use strict';

  var module = angular.module('icgc.tokens.controllers', []);

  module.controller('TokenController', function($scope, $timeout, $modalInstance, TokenService) {

    // Transient 
    $scope.selected = [];
    $scope.newToken = '';
    $scope.processing = false;


    // From server
    $scope.activeTokens = [];
    $scope.availableScopes = [];

    function refresh() {
      console.log('refreshing...');
      $scope.selected = [];

      TokenService.getTokens().then(function(data) {
        console.log('tokens', data);
        $scope.activeTokens = data.tokens;

        $timeout(function() { $scope.processing = false; }, 300);
      });

      TokenService.getScopes().then(function(data) {
        console.log('scopes', data);
        $scope.availableScopes = data.scopes;
      });
    }

    $scope.isActive = function(s) {
      return _.contains($scope.selected, s);
    };

    $scope.toggleScope = function(s) {
      if (_.contains($scope.selected, s)) {
        _.remove($scope.selected, s);
      } else {
        $scope.selected.push(s);
      }
    };

    $scope.deleteToken = function(token) {
      $scope.processing = true;
      TokenService.deleteToken(token).then(function(data) {
        refresh();
      });
    };

    $scope.createToken = function() {
      $scope.processing = true;
      TokenService.createToken($scope.selected).then(function(data) {
        refresh();
      });
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    refresh();
  });

})();

(function() {
  'use strict';

  var module = angular.module('icgc.tokens.services', []);

  module.service('TokenService', function(RestangularNoCache) {
    this.getTokens = function() {
      return RestangularNoCache.one('settings/tokens').get({});
    };

    this.createToken = function(scopes) {
      console.log('creating tokens in service', scopes);
      var scopeStr = scopes.join(' ');
      return RestangularNoCache.one('settings').post('tokens', 'scope='+scopeStr, {}, {'Accept': 'text/plain'});
    };

    this.deleteToken = function(token) {
      return RestangularNoCache.one('settings').one('tokens', token).remove();
    };

    this.getScopes = function() {
      return RestangularNoCache.one('settings/tokens/scopes').get({});
    };

  });

})();
