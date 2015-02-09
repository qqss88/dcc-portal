/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 'AS IS' AND ANY EXPRESS OR
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

  
  var module = angular.module('icgc.beacon', [
    'icgc.beacon.controllers'
  ]);
   
  module.config(function ($stateProvider) {
    $stateProvider.state('beacon', {
      url: '/ga4gh/beacon',
      templateUrl: 'scripts/beacon/views/beacon.html',
      controller: 'BeaconCtrl as BeaconCtrl'
    });
  });

})();


(function() {
  'use strict';

  var module = angular.module('icgc.beacon.controllers', []);

  module.controller('BeaconCtrl', function ($scope, Page, Restangular) {
    Page.setTitle('Beacon');
    Page.setPage('beacon');

    var projectsPromise = Restangular.one('projects')
      .get({
        'field' : 'id',
        'size' : 100
      },{'Accept':'application/json'});

    projectsPromise.then(function(data){
      $scope.projects = data.hits;
      $scope.projects.push({id:'ALL'});
      $scope.params = {
        project:$scope.projects[$scope.projects.length-1],
        chr:'1',
        reference:'GRCh37',
      };
    });

    $scope.hasInvalidParams = true;
    $scope.result = {
      exists:false,
      value:''
    };

    $scope.checkParams = function(){
      //first if anything required is empty or missing, stop checking
      if(!($scope.params.reference && $scope.params.position)){
        $scope.hasInvalidParams = true;
        return;
      }

      $scope.hasInvalidParams = false;
    };

    $scope.errorMessage='error error';
    $scope.submitQuery = function() {
      var promise = Restangular.one('beacon', 'query')
      .get({
        'chromosome' : $scope.params.chr,
        'reference':$scope.params.reference,
        'position':$scope.params.position.replace(/^0+/,''),
        'allele':$scope.params.allele ? $scope.params.allele : ''
      },{'Accept':'application/json'});

      promise.then(function(data){
        $scope.result.exists = true;
        $scope.result.value = data.response.exists;
        var url = data.getRequestedUrl();
        $scope.requestedUrl = url.substring(0,url.indexOf('?'))+'/query'+url.substring(url.indexOf('?'));
      });
    };

    $scope.resetQuery = function() {
      $scope.params = {
        project:$scope.projects[$scope.projects.length-1],
        chr:'1',
        reference:'GRCh37',
      };
      $scope.hasEmptyParams = true;
      $scope.result = {
        exists:false,
        value:''
      };
      $scope.requestedUrl = null;
    };
  });

  module.directive('positionValidator', function() {
    return {
      require: '?ngModel',
      link: function(scope, element, attrs, ngModelCtrl) {
        if(!ngModelCtrl) {
          return;
        }

        ngModelCtrl.$parsers.push(function(val) {
          if (angular.isUndefined(val)) {
            val = '';
          }
          var clean = val.replace( /[^0-9]+/g, '');
          if (val !== clean) {
            ngModelCtrl.$setViewValue(clean);
            ngModelCtrl.$render();
          }
          return clean;
        });

        element.bind('keypress', function(event) {
          if(event.keyCode === 32) {
            event.preventDefault();
          }
        });
      }
    };
  });

})();
