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

(function() {
  'use strict';

  var module = angular.module('icgc.share', []);

  module.directive('shareButton', function () {
    return {
      restrict: 'E',
      replace: true,
      transclude: true,
      templateUrl: '/scripts/share/views/share.html',
      controller: 'shareCtrl as shareCtrl',
      link: function () {
      }
    };
  });

  module.directive('sharePopup', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: '/scripts/share/views/share.popup.html'
    };
  });

  module.service('Share', function (Restangular, $location) {
    this.getShortUrl = function (params) {
      var port = window.location.port ? ':' +  window.location.port : '',
          defaults = {
            url: window.location.protocol + '//' + window.location.hostname + 
            port + window.location.pathname
          };
      
      
      var requestParams = $location.search(),
          queryStr = '';
        
      for (var requestKey in requestParams) {
        
        if (! requestParams.hasOwnProperty(requestKey)) {
          continue;
        }
          
        if (queryStr !== '') {
          queryStr += '&';
        }
        // FIXME: The url shortner decodes the GET request params for some reason - 
        // I will file a bug with them but in the meantime
        // this double encoding will do...fail...
        queryStr += requestKey +  '=' + encodeURIComponent(encodeURIComponent(requestParams[requestKey]));
      }
      
      
      if (queryStr.length > 0) {
          defaults.url += '?' + queryStr;
      }
      
     
      return Restangular.one('short', '').get(angular.extend(defaults, params));
    };
  });


  module.controller('SharePopupController', function($scope, $modalInstance, shortUrl) {
    $scope.shortUrl = shortUrl;
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  });

  module.controller('shareCtrl', function ($modal, Share) {
    var _ctrl= this;

    _ctrl.toggle = function(opt) {
      _ctrl.active = opt;
    };

    _ctrl.getShortUrl = function() {
      _ctrl.shortUrl = '';

      Share.getShortUrl().then(function(url) {
        _ctrl.shortUrl = url.shortUrl;

        $modal.open({
          templateUrl: '/scripts/share/views/share.popup.html',
          controller: 'SharePopupController',
          resolve: {
            shortUrl: function() {
              return _ctrl.shortUrl;
            }
          }
        });
      });
    };

  });
})();
