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

angular.module('icgc.ui.table', [
  'icgc.ui.table.size',
  'icgc.ui.table.counts'
]);

/* ************************************
 *   Table Size
 * ********************************* */
angular.module('icgc.ui.table.size', []);

angular.module('icgc.ui.table.size').controller('tableSizeController', function ($scope, LocationService) {
  var sog = LocationService.getJsonParam($scope.type);

  $scope.sizes = [10, 25, 50];

  $scope.selectedSize = sog.size ? sog.size : $scope.sizes[0];

  $scope.changeSize = function () {
    var so = LocationService.getJsonParam($scope.type);

    so.size = $scope.selectedSize;
    so.from = 1;

    LocationService.setJsonParam($scope.type, so);
  };
});

angular.module('icgc.ui.table.size').directive('tableSize', function () {
  return {
    restrict: 'A',
    scope: {
      type: '@'
    },
    replace: true,
    template: '<span>Showing <select data-ng-options="size for size in sizes"' +
              ' data-ng-model="selectedSize" data-ng-change="changeSize(size)"></select> rows</span>',
    controller: 'tableSizeController'
  };
});

/* ************************************
 *   Table Counts
 * ********************************* */
angular.module('icgc.ui.table.counts', []);

angular.module('icgc.ui.table.counts').directive('tableCounts', function () {
  return {
    restrict: 'A',
    scope: {
      page: '=',
      label: '@'
    },
    replace: true,
    template: '<span>' +
              'Showing <strong>{{page.from | number}}</strong> - ' +
              '<strong data-ng-if="page.count==page.size">' +
              '{{page.from + page.size - 1 | number}}</strong> ' +
              '<strong data-ng-if="page.count < page.size">{{page.total | number}}</strong> ' +
              'of <strong>{{page.total | number}}</strong> {{label}}' +
              '</span>'
  };
});

/* ************************************
 *   Table Sortable
 * ********************************* */
angular.module('icgc.ui.table.sortable', []);

angular.module('icgc.ui.table.sortable').controller('tableSortableController', function () {
  console.log('tableSortableController');
});

angular.module('icgc.ui.table.sortable').directive('tableSortable', function () {
  return {
    restrict: 'A',
    link: function (scope, elem, attrs) {
      console.log('tableSortable', attrs);
    }
  };
});
