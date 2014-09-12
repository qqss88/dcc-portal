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

'use strict';

angular.module('app.keyword.controllers', ['app.keyword.services']);

angular.module('app.keyword.controllers').controller('KeywordController',
  function ($scope, $location, debounce, KeywordService) {
    var pageSize;

    $scope.from = 1;
    pageSize = 20;

    $scope.query = $location.search().q || '';
    $scope.types = $location.search().types || 'all';
    $scope.isBusy = false;
    $scope.isFinished = false;

    $scope.setTitle('Results for ' + $scope.query);

    $scope.clear = function () {
      $scope.query = '';
      $location.search({});
      $scope.quickFn();
    };

    $scope.next = function () {
      if ($scope.isBusy || $scope.isFinished) {
        return;
      }

      $scope.from += pageSize;
      getResults({scroll: true});
    };

    $scope.quickFn = function () {
      var s;

      $scope.from = 1;

      if ($scope.query && $scope.query.length >= 2) {
        s = $location.search();
        s.q = $scope.query;
        $location.search(s);
        $scope.setTitle('Results for ' + $scope.query);

        getResults();
      } else {
        $scope.results = null;
      }
    };

    $scope.quick = debounce($scope.quickFn, 200, false);

    function getResults(settings) {
      var saved = $scope.query;

      settings = settings || {};
      $scope.isBusy = true;

      KeywordService.search({q: $scope.query, types: $scope.types, size: pageSize, from: $scope.from})
        .then(function (response) {
          $scope.isFinished = response.pagination.total - $scope.from < pageSize;
          $scope.isBusy = false;
          $scope.activeQuery = saved;

          if (settings.scroll) {
            $scope.results.pagination = response.pagination ? response.pagination : {};
            for (var i = 0; i < response.hits.length; i++) {
              $scope.results.hits.push(response.hits[i]);
            }
          } else {
            $scope.results = response;
          }
        });
    }

    $scope.$watch(function () {
      return $location.search();
    }, function (n) {
      $scope.query = n.q;
      $scope.types = n.types ? n.types : 'all';
      $scope.quickFn();
    }, true);

    $scope.$watch('from', function (n) {
      $scope.limit = n > 100;
    });

    getResults();
  });

