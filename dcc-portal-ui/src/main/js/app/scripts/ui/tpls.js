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

angular.module('app.ui.tpls', ['template/tsize.html', 'template/sortable.html', 'template/pagination.html',
  'template/dialog/message.html']);

angular.module('template/tsize.html', []).run(function ($templateCache) {
  $templateCache.put('template/tsize.html',
    '<select style="width:100px; margin-bottom:5px" ng-options="size +\' rows\' for size in sizes"' +
    ' ng-model="selectedSize" ng-change="update()"></select>'
  );
});

angular.module('template/sortable.html', []).run(function ($templateCache) {
  $templateCache.put('template/sortable.html',
    '<span ng-click="onClick()" style="cursor: pointer"><span ng-transclude></span> ' +
    '<i ng-if="!active" style="color:hsl(0,0%,80%)" class="icon-sort"></i>' +
    '<i ng-if="active" ng-class="{\'icon-sort-down\': reversed, \'icon-sort-up\':!reversed}"></i></span>');
});

angular.module('template/pagination.html', []).run(function ($templateCache) {
  $templateCache.put('template/pagination.html',
    '<div style="margin-top: 1rem"><div ng-if="data.hits.length"><span data-table-size data-type="{{ type }}"></span>' +
    '<span ng-if="data.pagination.pages > 1" class="pull-right">' +
    '<div class="pagination"><ul>' +
    '<li ng-repeat="page in pages" ng-class="{active: page.active, disabled: page.disabled}">' +
    '<a ng-click="selectPage(page.number)">{{page.text}}</a></li>' +
    '</ul></div>' +
    '</span></div></div>'
  );
});

angular.module('template/dialog/message.html', []).run(['$templateCache', function ($templateCache) {
  $templateCache.put('template/dialog/message.html',
    '<div class=\"modal-dialog\">' +
    '<div class=\"modal-content\">' +
    '<div class=\"modal-header\">' +
    '	<h1>{{ title }}</h1>' +
    '</div>' +
    '<div class=\"modal-body\">' +
    '	<p>{{ message }}</p>' +
    '</div>' +
    '<div class=\"modal-footer\">' +
    '	<button ng-repeat=\"btn in buttons\" ng-click=\"close(btn.result)\"' +
    'class=btn ng-class=\"btn.cssClass\">{{ btn.label }}</button>' +
    '</div></div></div>' +
    '');
}]);
