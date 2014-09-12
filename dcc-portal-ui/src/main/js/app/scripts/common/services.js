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

  var module = angular.module('app.common.services', []);

  module.factory('Page', function () {
    var title = 'Loading...',
      page = 'home',
      error = false,
      working = 0;

    return {
      title: function () {
        return title;
      },
      setTitle: function (t) {
        if (angular.isDefined(t)) {
          title = t;
        }
      },
      page: function () {
        return page;
      },
      setPage: function (p) {
        if (angular.isDefined(p)) {
          page = p;
        }
      },
      startWork: function () {
        working++;
      },
      stopWork: function () {
        if (working > 0) {
          working--;
        }
      },
      working: function () {
        return working;
      },
      // For reseting state on error
      stopAllWork: function() {
        working = 0;
        angular.element(document.querySelector('article')).css('visibility', 'visible');
        angular.element(document.querySelector('aside')).css('visibility', 'visible');
      },
      setError: function(e) {
        error = e;
        if (error === true) {
          angular.element(document.querySelector('article')).css('visibility', 'hidden');
          angular.element(document.querySelector('aside')).css('visibility', 'hidden');
        }
      },
      getError: function() {
        return error;
      }
    };
  });

  module.factory('Compatibility', function ($window) {

    function checkBase64() {
      if (!angular.isDefined($window.btoa)) {
        $window.btoa = base64.encode;
      }
      if (!angular.isDefined($window.atob)) {
        $window.atob = base64.decode;
      }
    }

    function checkLog() {
      if (!($window.console && console.log)) {
        $window.console = {
          log: function () {
          },
          debug: function () {
          },
          info: function () {
          },
          warn: function () {
          },
          error: function () {
          }
        };
      }
    }

    function checkTime() {
      if ($window.console && typeof($window.console.time === 'undefined')) {
        $window.console.time = function () {
        };
        $window.console.timeEnd = function () {
        };
      }
    }

    return {
      run: function () {
        checkBase64();
        checkLog();
        checkTime();
      }
    };
  });

  module.service('Settings', function (Restangular) {
    this.get = function () {
      return Restangular.one('settings').get();
    };
  });

})();
