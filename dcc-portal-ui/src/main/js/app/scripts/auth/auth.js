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

  angular.module('icgc.auth', ['icgc.auth.controllers', 'icgc.auth.directives']);
})();

(function () {
  'use strict';

  angular.module('icgc.auth.models', []);

  angular.module('icgc.auth.models').factory('Auth', function ($window, $cookies, Restangular) {
    var user = {}, handler = Restangular.one('auth').withHttpConfig({cache: false});

    function hasSession() {
      console.debug('Checking for active session...');
      return !!$cookies.dcc_session;
    }

    function checkSession(succ) {
      user.verifying = true;
      handler.one('verify').get().then(succ, function(){
        user.verifying = false;
      });
    }

    function login(data) {
      delete $cookies.openid_error;
      user = {
        email: data.username,
        token: data.token,
        daco: data.daco,
        verifying: false
      };
    }

    function deleteCookies() {
      delete $cookies.dcc_session;
      delete $cookies.dcc_user;
      Restangular.setDefaultRequestParams({});
      $window.location.reload();
    }

    function logout() {
      user.verifying = false;
      handler.post('logout').then(function () {
        deleteCookies();
      });
    }

    function getUser() {
      return user;
    }

    return {
      hasSession: hasSession,
      checkSession: checkSession,
      login: login,
      logout: logout,
      deleteCookies: deleteCookies,
      getUser: getUser
    };
  });

  angular.module('icgc.auth.models').factory('CUD', function ($window, Settings) {
    function login() {
      Settings.get().then(function(settings) {
        redirect(settings.ssoUrl);
      });
    }

    function redirect(url) {
      $window.location = url + $window.location.href;
    }

    return {
      login: login,
      redirect: redirect
    };
  });

  angular.module('icgc.auth.models').factory('OpenID', function (Restangular, $window, $cookies) {
    var handler = Restangular.one('auth/openid');

    function provider(identifier) {
      return handler.post(
        'provider', {}, {identifier: identifier, currentUrl: $window.location.pathname + $window.location.search}
      ).then(function (response) {
          $window.location = response.replace(/"/g, '');
        });
    }

    function getErrors() {
      return $cookies.openid_error;
    }

    function hasErrors() {
      return !!$cookies.openid_error;
    }

    return {
      provider: provider,
      getErrors: getErrors,
      hasErrors: hasErrors
    };
  });
})();

(function () {
  'use strict';

  angular.module('icgc.auth.controllers', ['icgc.auth.models']);

  angular.module('icgc.auth.controllers').controller('authController',
    function ($window, $scope, Auth, CUD, OpenID, $state, $stateParams) {

      function setup() {
        $scope.user = Auth.getUser();
        // Check for errors
        if (OpenID.hasErrors()) {
          $scope.error = OpenID.getErrors();
          $scope.loginModal = true;
        } else {
          Auth.checkSession(
            function (data) {
              Auth.login(data);
              $scope.user = Auth.getUser();
              $state.transitionTo($state.current, $stateParams,
                { reload: true, inherit: false, notify: true });
              console.log('logged in as: ', $scope.user);
            });
        }
      }

      function errorMap(e) {
        switch (e.code) {
        case '1796':
          return  $scope.openIDUrl + ' is not a known provider';
        case '1798':
          return 'Could not connect to ' + $scope.openIDUrl;
        default:
          return e.message;
        }
      }

      function providerMap(provider) {
        switch (provider) {
        case 'google':
          return 'https://www.google.com/accounts/o8/id';
        case 'yahoo':
          return 'https://me.yahoo.com';
        case 'verisign':
          return 'https://' + $scope.verisignUsername + '.pip.verisignlabs.com/';
        default:
          return $scope.openIDUrl;
        }
      }

      $scope.tryLogin = function () {
        $scope.connecting = true;
        if ($scope.provider === 'icgc') {
          CUD.login({
            username: $scope.cudUsername,
            password: $scope.cudPassword
          });
        } else {
          OpenID.provider(providerMap($scope.provider)).then(
            function () {},
            function (response) {
              $scope.connecting = false;
              $scope.error = errorMap(response.data);
            });
        }
      };

      $scope.logout = function () {
        Auth.logout();
      };

      setup();
    });
})();

(function () {
  'use strict';

  angular.module('icgc.auth.directives', ['icgc.auth.controllers']);

  angular.module('icgc.auth.directives').directive('login', function ($compile) {
    return {
      restrict: 'E',
      replace: true,
      transclude: true,
      templateUrl: '/scripts/auth/views/login.html',
      controller: 'authController',
      link: function (scope, element) {
        element.after($compile('<login-popup></login-popup>')(scope));
        element.after($compile('<logout-popup></logout-popup>')(scope));
        element.after($compile('<auth-popup></auth-popup>')(scope));
      }
    };
  });

  angular.module('icgc.auth.directives').directive('loginPopup', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: '/scripts/auth/views/login.popup.html'
    };
  });

  angular.module('icgc.auth.directives').directive('logoutPopup', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: '/scripts/auth/views/logout.popup.html'
    };
  });

  angular.module('icgc.auth.directives').directive('authPopup', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: '/scripts/auth/views/auth.popup.html'
    };
  });

})();
