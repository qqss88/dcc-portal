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

  /*
  module.directive('selectOnClick', function () {
    return {
      restrict: 'A',
      link: function (scope, element) {
        element.on('click', function () {
          this.select();
        });
      }
    };
  });
  */

  module.directive('shareButton', function ($compile) {
    return {
      restrict: 'E',
      replace: true,
      transclude: true,
      templateUrl: '/scripts/share/views/share.html',
      controller: 'shareCtrl as shareCtrl',
      link: function (scope, element) {
        element.after($compile('<share-popup></share-popup>')(scope));
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

  module.service('Share', function (Restangular) {
    this.getShortUrl = function (params) {
      var defaults = {
        url: window.location.href
      };

      return Restangular.one('short', '').get(angular.extend(defaults, params));
    };
  });

  module.controller('shareCtrl', function (Share) {
    var _ctrl= this;

    _ctrl.toggle = function(opt) {
      _ctrl.active = opt;
    };

    _ctrl.getShortUrl = function() {
      _ctrl.shortUrl = '';

      Share.getShortUrl().then(function(url) {
        _ctrl.shortUrl = url.shortUrl;
      });
    };
  });
})();
