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

angular.module('icgc.ui', [
  'icgc.ui.suggest',
  'icgc.ui.table',
  'icgc.ui.toolbar',
  'icgc.ui.openin',
  'icgc.ui.trees',
  'icgc.ui.lists',
  'icgc.ui.query',
  'icgc.ui.events',
  'icgc.ui.validators',
  'icgc.ui.tooltip',
  'icgc.ui.scroll',
  'icgc.ui.fileUpload',
  'icgc.ui.badges'
]);


angular.module('app.ui', [
  'app.ui.tpls',
  'app.ui.param', 'app.ui.nested', 'app.ui.mutation',
  'app.ui.hidetext', 'app.ui.exists'
]);



/**
 * File chooser detector
 *
 * See: https://github.com/angular/angular.js/issues/1375
 * See: http://uncorkedstudios.com/blog/multipartformdata-file-upload-with-angularjs
 */
angular.module('icgc.ui.fileUpload', []).directive('fileUpload', function($parse) {
  return {
    restrict: 'A',
    link: function($scope, $element, $attrs) {
      var model = $parse($attrs.fileUpload);
      var modelSetter = model.assign;

      $element.bind('change', function() {
        $scope.$apply(function() {
          modelSetter($scope, $element[0].files[0]);

          // Trick the input so the same file will trigger another 'change' event
          // Used for gene list upload to textarea
          if ($element[0].value) {
            $element[0].value = '';
          }

        });
      });
    }
  };
});


angular.module('app.ui.exists', []).directive('exists', function () {
  return {
    restrict: 'A',
    replace: true,
    scope: {
      exists: '='
    },
    template: '<span></span>',
    link: function(scope, element) {
      var iconOK = angular.element('<i>').addClass('icon-ok');

      function update() {
        element.empty();
        if (scope.exists) {
          element.append(iconOK);
        } else {
          element.append('--');
        }
      }
      update();

      scope.$watch('exists', function(n, o) {
        if (n === o) {
          return;
        }
        update();
      });

      scope.$on('$destroy', function() {
        iconOK.remove();
        iconOK = null;
      });
    }
  };
});

angular.module('app.ui.hidetext', []).directive('hideText', function () {
  return {
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {},
    template: '<div class="t_sh">' +
              '{{ text }}' +
              '<div ng-if="text.length>=limit" class="t_sh__toggle">' +
              '<a ng-click="toggle()" href="" class="t_tools__tool">' +
              '<span ng-if="!expanded"><i class="icon-caret-down"></i> more</span>' +
              '<span ng-if="expanded"><i class="icon-caret-up"></i> less</span>' +
              '</a>' +
              '</div>' +
              '</div>',
    link: function (scope, element, attrs) {
      var previous, next;

      scope.limit = 250;

      previous = attrs.text;
      next = attrs.text.length > scope.limit ? attrs.text.slice(0, scope.limit) + '...' : attrs.text;
      scope.text = next;

      scope.toggle = function () {
        previous = [next, next = previous][0];
        scope.text = next;
        scope.expanded = !scope.expanded;
      };
    }
  };
});

// This might be more confusing than helpful - DC
angular.module('app.ui.nested', []).directive('nested', function ($location) {
  return function (scope, element, attrs) {
    element.bind('click', function (e) {
      e.preventDefault();
      scope.$apply(function () {
        $location.path(attrs.href);
      });
    });
  };
});


angular.module('app.ui.mutation', []).directive('mutationConsequences', function ($filter, ConsequenceOrder) {
  return {
    restrict: 'E',
    scope: {
      items: '='
    },
    template: '<ul class="unstyled">' +
              '<li data-ng-repeat="c in consequences">' +
                '<abbr data-tooltip="SO term: {{ c.consequence }}">{{c.consequence | trans}}</abbr>' +
                '<span data-ng-repeat="(gk, gv) in c.data">' +
                  '<span>{{ $first==true? ": " : ""}}</span>' +
                  '<a href="/genes/{{gk}}"><em>{{gv.symbol}}</em></a> ' +
                  '<span data-ng-repeat="aa in gv.aaChangeList">' +
                    '<span class="t_impact_{{aa.FI | lowercase }}">{{aa.aaMutation}}</span>' +
                    '<span>{{ $last === false? ", " : ""}}</span>' +
                  '</span>' +
                  '<span>{{ $last === false? " - " : "" }}</span>' +
                '</span>' +
                '<span class="hidden">{{ $last === false? "|" : "" }}</span>' + // Separator for html download
              '</li>' +
              '</ul>',
    link: function (scope) {
      var consequenceMap;

      // Massage the tabular data into the following format:
      // Consequence1: gene1 [aa1 aa2] - gene2 [aa1 aa2] - gene3 [aa3 aa4]
      // Consequence2: gene1 [aa1 aa2] - gene5 [aa1 aa2]
      consequenceMap = {};
      scope.items.forEach(function (consequence) {
        var geneId, type;

        geneId = consequence.geneAffectedId;
        type = consequence.type;

        if (geneId) {
          if (!consequenceMap.hasOwnProperty(type)) {
            consequenceMap[type] = {};
          }

          var c = consequenceMap[type];
          if (!c.hasOwnProperty(geneId)) {
            c[geneId] = {};
            c[geneId].symbol = consequence.geneAffectedSymbol;
            c[geneId].id = geneId;
            c[geneId].aaChangeList = [];
          }
          c[geneId].aaChangeList.push({
            'aaMutation': consequence.aaMutation,
            'FI': consequence.functionalImpact
          });
        }

      });

      // Dump into a list, easier to format/sort
      scope.consequences = [];
      for (var k in consequenceMap) {
        if (consequenceMap.hasOwnProperty(k)) {
          scope.consequences.push({
            consequence: k,
            data: consequenceMap[k]
          });
        }
      }

      scope.consequences = $filter('orderBy')(scope.consequences, function (t) {
        var index = ConsequenceOrder.indexOf(t.consequence);
        if (index === -1) {
          return ConsequenceOrder.length + 1;
        }
        return index;
      });
    }
  };
});

// Used in keyword search, rename - DC
angular.module('app.ui.param', []).directive('param', function (LocationService) {
  return {
    restrict: 'A',
    transclude: true,
    replace: true,
    scope: {
      key: '@',
      value: '@',
      defaultSelect: '@'
    },
    template: '<span data-ng-class="{\'t_labels__label_inactive\': !active}" data-ng-click="update()">' +
              '<i data-ng-class="{\'icon-ok\': active}"></i> {{value | trans:true}}</span>',
    link: function (scope) {
      var type = LocationService.search()[scope.key];

      if (type) {
        scope.active = type === scope.value;
      } else {
        scope.active = !!scope.defaultSelect;
      }

      scope.update = function () {
        LocationService.setParam(scope.key, scope.value);
      };

      scope.$watch(function () {
        return LocationService.search()[scope.key];
      }, function (n) {
        if (n) {
          scope.active = n === scope.value;
        } else {
          scope.active = !!scope.defaultSelect;
        }
      }, true);
    }
  };
});

angular.module('icgc.ui.badges', []).directive('pcawgBadge', function() {
  return {
    restrict: 'E',
    replace: true,
    template: '<span class="badge pcawg-badge">PCAWG</span>'
  };
});
