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

angular.module('icgc.ui', ['icgc.ui.suggest', 'icgc.ui.table', 'icgc.ui.toolbar', 'icgc.ui.openin']);

angular.module('app.ui', [
  'app.ui.sortable',
  'app.ui.scrollto',
  'app.ui.pagination',
  'app.ui.tpls',
  'app.ui.synonyms',
  'app.ui.dl',
  'app.ui.scrolled', 'app.ui.focus', 'app.ui.blur',
  'app.ui.param', 'app.ui.nested', 'app.ui.mutation', 'app.ui.hidetext', 'app.ui.lists',
  'app.ui.es', 'app.ui.exists', 'app.ui.scrollSpy', 'app.ui.tooltip', 'app.ui.tooltipControl', 'app.ui.exists2',
  'app.ui.fileUpload'
]);


// See: https://github.com/angular/angular.js/issues/1375
// See: http://uncorkedstudios.com/blog/multipartformdata-file-upload-with-angularjs
angular.module('app.ui.fileUpload', []).directive('fileUpload', function($parse) {
  return {
    restrict: 'A',
    link: function($scope, $element, $attrs) {
      var model = $parse($attrs.fileUpload);
      var modelSetter = model.assign;

      $element.bind('change', function() {
        $scope.$apply(function() {
          modelSetter($scope, $element[0].files[0]);
        });
      });
    }
  };
});


// Centralized tooltip directive. There should be only one per application
angular.module('app.ui.tooltipControl', [])
  .directive('tooltipControl', function ($position, $rootScope, $sce) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
      },
      templateUrl: 'template/tooltip.html',
      link: function (scope, element) {
        scope.placement = 'right';
        scope.html = '???';

        function calculatePlacement(placement, target) {
          var position = $position.offset(target);
          var result = {};

          var ttWidth = element.prop('offsetWidth');
          var ttHeight = element.prop('offsetHeight');

          switch(placement) {
          case 'right':
            result = {
              top: position.top + position.height / 2 - ttHeight / 2,
              left: position.left + position.width
            };
            break;
          case 'left':
            result = {
              top: position.top + position.height / 2 - ttHeight / 2,
              left: position.left - ttWidth
            };
            break;
          case 'top':
            result = {
              top: position.top - ttHeight,
              left: position.left > ttWidth / 4 ? (position.left + position.width / 2 - ttWidth / 2) : 0
            };
            break;
          default:
            result = {
              top: position.top,
              left: position.left + position.width / 2
            };
          }
          return result;
        }

        $rootScope.$on('tooltip::show', function(evt, params) {
          scope.$apply(function() {
            if (params.text) {
              scope.html = $sce.trustAsHtml(params.text);
            }
            if (params.placement) {
              scope.placement = params.placement;
            }
          });

          var position = calculatePlacement(params.placement, params.element);
          element.css('top', position.top);
          element.css('left', position.left);
        });
        $rootScope.$on('tooltip::hide', function() {
          element.css('top', -999);
          element.css('left', -999);
        });
      }
    };
  });


// Light weight directive for request tooltips
angular.module('app.ui.tooltip', [])
  .directive('tooltip', function($timeout) {
    return {
      restrict: 'A',
      replace: false,
      scope: {
      },
      link: function(scope, element, attrs) {
        var tooltipPromise;

        element.bind('mouseenter', function() {
          var placement = attrs.tooltipPlacement || 'top';

          if (! attrs.tooltip) {
            return;
          }

          // If placement = overflow, check if there is actually overflow
          if (attrs.tooltipPlacement === 'overflow') {
            if (element.context.scrollWidth <= element.context.clientWidth) {
              return;
            } else {
              placement = 'top';
            }
          }

          tooltipPromise = $timeout(function() {
            scope.$emit('tooltip::show', {
              element: element,
              text: attrs.tooltip || '???',
              placement: placement || 'top'
            });
          }, 500);
        });

        element.bind('mouseleave', function() {
          $timeout.cancel(tooltipPromise);
          scope.$emit('tooltip::hide');
        });

        element.bind('click', function() {
          $timeout.cancel(tooltipPromise);
          scope.$emit('tooltip::hide');
        });

        scope.$on('$destroy', function() {
          element.off();
          element.unbind();
        });
      }
    };
  });



angular.module('app.ui.exists2', []).directive('exists2', function () {
  return {
    restrict: 'A',
    replace: true,
    scope: {
      exists2: '='
    },
    template: '<span><i data-ng-if="exists2" class="icon-ok"></i><span data-ng-if="!exists2">--</span></span>'
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


angular.module('app.ui.es', []).directive('expandSearch', function () {
  return {
    restrict: 'A',
    replace: true,
    transclude: true,
    template: '<a ng-class="{\'open\':o.open==true}" ng-transclude=""></a>',
    link: function (scope, elem) {
      scope.o = {};
      scope.expand = function () {
        scope.o.open = true;
        jQuery(elem).find('input').focus();
      };
    }
  };
});

angular.module('app.ui.lists', []).directive('hideSumList', function (Projects) {
  return {
    restrict: 'A',
    //replace: true,
    transclude: true,
    template: '<div class="t_sh">' +
              '<span data-ng-if="!hasItems"><i class="icon-spin icon-spinner"></i></span>' +
              '<span data-ng-if="!show&&list.length === 0">--</span>' +
              '<div data-ng-if="show" class="text-right">' +
              '<a href="{{ link }}">{{sum | number}}</a>' +
              ' / ' +
              '<a href=\'/search?filters={"donor":{"availableDataTypes":{"is":["ssm"]}}}\'>{{sumTotal | number}}</a>' +
              ' <em>({{sum/sumTotal*100|number:2}}%)</em>' +
              '<span data-ng-click="toggle()" class="t_tools__tool">' +
              '<i data-ng-class="expanded ? \'icon-caret-down\' : \'icon-caret-left\'"></i>' +
              '</span>' +
              '</div>' +
              '<div data-ng-transclude></div>' +
              '</div>',
    link: function (scope, elem, attrs) {
      var previous, next, limit;

      scope.hasItems = false;
      // How many items to show in collapsed list
      limit = attrs.limit ? parseInt(attrs.limit, 10) : 0;

      function swap() {
        previous = [next, next = previous][0];
      }

      function list(value) {
        scope.hasItems = true;
        scope.list = value;
        // How many items are hidden
        scope.more = scope.list.length - limit;
        // If there is more than 1 item in the collapsed list show toggle
        scope.show = scope.more > 0;

        previous = scope.list;
        next = scope.show ? scope.list.slice(0, limit) : scope.list;

        // If list updates while expanded
        if (scope.expanded) {
          swap();
        }

        scope.list = next;

        scope.sum = _.reduce(_.pluck(value, 'count'), function (s, n) {
          return s + n;
        });

        scope.sumTotal = Projects.totalSsmTestedDonorCount;
      }

      scope.toggle = function () {
        scope.expanded = !scope.expanded;
        swap();
        scope.list = next;
      };

      // Need to use observe instead of scope so list still
      // has access to parent scope events
      attrs.$observe('hideSumList', function (value) {
        if (value) {
          list(angular.fromJson(value));
        }
      });

      attrs.$observe('sum', function (value) {
        if (value) {
          scope.sum = angular.fromJson(value);
        }
      });

      attrs.$observe('link', function (value) {
        if (value) {
          scope.link = value;
        }
      });
    }
  };
});


angular.module('app.ui.lists').directive('hideLinkList', function () {
  return {
    restrict: 'A',
    //replace: true,
    transclude: true,
    template: '<div class="t_sh">' +
              '<span data-ng-if="!hasItems"><i class="icon-spin"></i></span>' +
              '<span data-ng-if="!show&&hasItems">--</span>' +
              '<div data-ng-if="show" class="text-right">' +
              '<a data-ng-href="{{ link }}">{{ more }}</a> ' +
              '<span data-ng-if="!expanded">' +
              '<span data-ng-click="toggle()" class="t_tools__tool"><i class="icon-caret-left"></i></span>' +
              '</span>' +
              '<span data-ng-if="expanded" data-ng-click="toggle()" class="t_tools__tool">' +
              '<i class="icon-caret-down"></i></span>' +
              '</div>' +
              '<div data-ng-transclude></div>' +
              '</div>',
    link: function (scope, elem, attrs) {
      var previous, next, limit;

      scope.hasItems = false;
      // How many items to show in collapsed list
      limit = attrs.limit ? parseInt(attrs.limit, 10) : 0;

      function swap() {
        previous = [next, next = previous][0];
      }

      function list(value) {
        scope.hasItems = true;
        scope.list = value;
        // How many items are hidden
        scope.more = scope.list.length - limit;
        // If there is more than 1 item in the collapsed list show toggle
        scope.show = scope.more > 0;

        previous = scope.list;
        next = scope.show ? scope.list.slice(0, limit) : scope.list;

        // If list updates while expanded
        if (scope.expanded) {
          swap();
        }

        scope.list = next;
      }

      scope.toggle = function () {
        scope.expanded = !scope.expanded;
        swap();
        scope.list = next;
      };

      // Need to use observe instead of scope so list still
      // has access to parent scope events
      attrs.$observe('hideLinkList', function (value) {
        if (value) {
          list(JSON.parse(value));
        }
      });

      attrs.$observe('link', function (value) {
        if (value) {
          scope.link = value;
        }
      });
    }
  };
});

angular.module('app.ui.lists').directive('hideList', function () {
  return {
    restrict: 'A',
    replace: true,
    transclude: true,
    template: '<ul class="t_sh">' +
              '<li data-ng-if="list.length == 0">--</li>' +
              '<li><ul ng-transclude></ul></li>' +
              '<li ng-if="show" class="t_sh__toggle">' +
              '<a ng-click="toggle()" href="" class="t_tools__tool">' +
              '<span ng-if="!expanded"><i class="icon-caret-down"></i> {{ more }} more</span>' +
              '<span ng-if="expanded"><i class="icon-caret-up"></i> less</span>' +
              '</a>' +
              '</li></ul>',
    link: function (scope, elem, attrs) {
      var previous, next, limit;

      // How many items to show in collapsed list
      limit = attrs.limit ? parseInt(attrs.limit, 10) : 1;

      function swap() {
        previous = [next, next = previous][0];
      }

      function list(value) {
        scope.list = value;
        // How many items are hidden
        scope.more = scope.list.length - limit;
        // If there is more than 1 item in the collapsed list show toggle
        scope.show = scope.more > 1;

        previous = scope.list;
        next = scope.show ? scope.list.slice(0, limit) : scope.list;

        // If list updates while expanded
        if (scope.expanded) {
          swap();
        }

        scope.list = next;
      }

      scope.toggle = function () {
        scope.expanded = !scope.expanded;
        swap();
        scope.list = next;
      };

      // Need to use observe instead of scope so list still
      // has access to parent scope events
      attrs.$observe('hideList', function (value) {
        if (value) {
          list(JSON.parse(value));
        }
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
                '<abbr data-tooltip="{{ c.consequence | trans | define }}">{{c.consequence | trans}}</abbr>' +
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

angular.module('app.ui.focus', []).directive('focus', function ($parse) {
  return function (scope, element, attr) {
    var fn = $parse(attr.focus);

    function callback(event) {
      scope.$apply(function () {
        fn(scope, {$event: event});
      });
    }

    element.on('focus', callback);
  };
});

angular.module('app.ui.blur', []).directive('blur', function ($parse) {
  return function (scope, element, attr) {
    var fn = $parse(attr.blur);

    function callback(event) {
      scope.$apply(function () {
        fn(scope, {$event: event});
      });
    }

    element.on('blur', callback);
  };
});

angular.module('app.ui.scrolled', []).directive('scrolled', function ($window) {
  return function (scope, elem) {
    var w, oy;

    w = angular.element($window);
    oy = w.scrollTop();

    function checkDelta() {
      var top = w.scrollTop();

      // only scroll down after top 100px
      // only scroll if d > 50px
      if (top > 150 && top > oy + 50) {
        //scroll down
        elem.stop(true, true).animate({
          top: '-51'
        }, 100);
        oy = top;
      } else if (top < oy - 50) {
        //scroll up
        elem.stop(true, true).animate({
          top: '0'
        }, 100);
        oy = top;
      }
    }

    w.on('scroll', checkDelta);
    scope.$on('$destroy', function () {
      w.off('scroll', checkDelta);
    });
  };
});

angular.module('app.ui.synonyms', []).directive('synonyms', function () {
  return {
    restrict: 'E',
    scope: {
      item: '='
    },
    template: '<span>{{syn}}</span>',
    link: function (scope) {
      scope.syn = angular.isArray(scope.item) ? scope.item.join(', ') : scope.item;
    }
  };
});

angular.module('app.ui.scrollto', []).directive('scrollto', function () {
  return function (scope, elm, attrs) {
    elm.bind('click', function (e) {
      var top;

      e.preventDefault();
      if (attrs.href) {
        attrs.scrollto = attrs.href;
      }

      top = jQuery(attrs.scrollto).offset().top - 40;
      jQuery('body,html').animate({ scrollTop: top }, 800);
    });
  };
});

angular.module('app.ui.sortable', []).directive('sortable', function ($location, LocationService) {
  return {
    restrict: 'A',
    scope: {
      active: '@',
      reversed: '@',
      field: '@',
      type: '@',
      callback: '&'
    },
    transclude: true,
    templateUrl: 'template/sortable.html',
    link: function (scope) {
      var defaultActive, defaultReversed;

      defaultActive = scope.active;
      defaultReversed = scope.reversed;

      scope.$watch(function () {
        return LocationService.getJsonParam(scope.type);
      }, function (so) {
        scope.active = defaultActive;
        scope.reversed = defaultReversed;
        if (so.sort) {
          scope.active = so.sort === scope.field;
          scope.reversed = so.order === 'desc';
        }

        if (angular.isDefined(scope.callback) && angular.isFunction(scope.callback)) {
          scope.callback();
        }
      }, true);

      scope.onClick = function () {
        var so = LocationService.getJsonParam(scope.type);

        if (so.hasOwnProperty('sort') && so.sort === scope.field) {
          scope.reversed = !scope.reversed;
          so.order = scope.reversed ? 'desc' : 'asc';
        } else {
          if (scope.active) {
            scope.reversed = !scope.reversed;
          } else {
            scope.reversed = true;
          }
          so.sort = scope.field;
          so.order = scope.reversed ? 'desc' : 'asc';
        }

        // reset to first page
        if (so.from) {
          so.from = 1;
        }

        LocationService.setJsonParam(scope.type, so);
      };
    }
  };
});

angular.module('app.ui.pagination', [])

  .constant('paginationConfig', {
    boundaryLinks: true,
    directionLinks: true,
    firstText: '<<<',
    previousText: '<',
    nextText: '>',
    lastText: '>>>',
    maxSize: 5
  })

  .directive('paginationControls', function (paginationConfig, LocationService, $filter) {
    return {
      restrict: 'E',
      scope: {
        type: '@',
        data: '=',
        //maxSize: '=',
        onSelectPage: '&'
      },
      templateUrl: 'template/pagination.html',
      replace: true,
      link: function (scope, element, attrs) {
        var boundaryLinks, directionLinks, firstText, previousText, nextText, lastText;

        // Setup configuration parameters
        boundaryLinks = angular.isDefined(attrs.boundaryLinks) ?
          scope.$eval(attrs.boundaryLinks) : paginationConfig.boundaryLinks;
        directionLinks = angular.isDefined(attrs.directionLinks) ?
          scope.$eval(attrs.directionLinks) : paginationConfig.directionLinks;
        firstText = angular.isDefined(attrs.firstText) ? attrs.firstText : paginationConfig.firstText;
        previousText = angular.isDefined(attrs.previousText) ? attrs.previousText : paginationConfig.previousText;
        nextText = angular.isDefined(attrs.nextText) ? attrs.nextText : paginationConfig.nextText;
        lastText = angular.isDefined(attrs.lastText) ? attrs.lastText : paginationConfig.lastText;

        scope.maxSize = angular.isDefined(attrs.maxSize) ? attrs.maxSize : paginationConfig.maxSize;

        // Create page object used in template
        function makePage(number, text, isActive, isDisabled) {
          if (angular.isNumber(text)) {
            text = $filter('number')(text);
          }

          return {
            number: number,
            text: text,
            active: isActive,
            disabled: isDisabled
          };
        }

        scope.$watch('data.pagination', function () {
          var max, maxSize, startPage, number, page, previousPage, nextPage, firstPage, lastPage;

          if (!scope.data) {
            return;
          }

          scope.pages = [];

          //set the default maxSize to pages
          maxSize = (scope.maxSize && scope.maxSize < scope.data.pagination.pages) ?
            scope.maxSize : scope.data.pagination.pages;
          startPage = scope.data.pagination.page - Math.floor(maxSize / 2);

          //adjust the startPage within boundary
          if (startPage < 1) {
            startPage = 1;
          }
          if ((startPage + maxSize - 1) > scope.data.pagination.pages) {
            startPage = startPage - ((startPage + maxSize - 1) - scope.data.pagination.pages);
          }

          // Add page number links
          for (number = startPage, max = startPage + maxSize; number < max; number++) {
            page = makePage(number, number, scope.isActive(number), false);
            scope.pages.push(page);
          }

          // Add previous & next links
          if (directionLinks) {
            previousPage = makePage(scope.data.pagination.page - 1, previousText, false, scope.noPrevious());
            scope.pages.unshift(previousPage);

            nextPage = makePage(scope.data.pagination.page + 1, nextText, false, scope.noNext());
            scope.pages.push(nextPage);
          }

          // Add first & last links
          if (boundaryLinks) {
            firstPage = makePage(1, firstText, false, scope.noPrevious());
            scope.pages.unshift(firstPage);

            lastPage = makePage(scope.data.pagination.pages, lastText, false, scope.noNext());
            scope.pages.push(lastPage);
          }

          if (scope.data.pagination.page > scope.data.pagination.pages) {
            scope.selectPage(scope.data.pagination.pages);
          }
        });
        scope.noPrevious = function () {
          return scope.data.pagination.page === 1;
        };
        scope.noNext = function () {
          return scope.data.pagination.page === scope.data.pagination.pages;
        };
        scope.isActive = function (page) {
          return scope.data.pagination.page === page;
        };

        scope.selectPage = function (page) {
          if (!scope.isActive(page) && page > 0 && page <= scope.data.pagination.pages) {
            scope.data.pagination.page = page;
            scope.updateParams(scope.type, page);
            scope.onSelectPage();
          }
        };

        scope.updateParams = function (type, page) {
          var sType, from = (scope.data.pagination.size * (page - 1) + 1);

          if (type) {
            sType = LocationService.getJsonParam(type);
            if (sType) {
              sType.from = from;
              LocationService.setJsonParam(type, sType);
            } else {
              LocationService.setJsonParam(type, '{"from":"' + from + '"}');
            }
          } else {
            LocationService.setJsonParam('from', from);
          }
        };
      }
    };
  });

angular.module('app.ui.scrollSpy', []).directive('scrollSpy', function ($window) {
  return {
    restrict: 'A',
    controller: function ($scope) {
      $scope.spies = [];
      this.addSpy = function (spyObj) {
        return $scope.spies.push(spyObj);
      };
    },
    link: function (scope, elem) {
      var spyElems, w;
      spyElems = [];
      w = jQuery($window);

      function scrl() {
        var highlightSpy, pos, spy, _i, _len, _ref;
        highlightSpy = null;
        _ref = scope.spies;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          spy = _ref[_i];
          spy.out();
          if ((pos = spyElems[spy.id].offset().top) - $window.scrollY <= 65) {
            spy.pos = pos;
            if (!highlightSpy) {
              highlightSpy = spy;
            }
            if (highlightSpy.pos < spy.pos) {
              highlightSpy = spy;
            }
          }
        }
        return highlightSpy ? highlightSpy['in']() : scope.spies[0]['in']();
      }

      scope.$watch('spies', function (spies) {
        var spy, _i, _len, _results;
        _results = [];
        for (_i = 0, _len = spies.length; _i < _len; _i++) {
          spy = spies[_i];
          if (!spyElems[spy.id]) {
            _results.push(spyElems[spy.id] = elem.find('#' + spy.id));
          } else {
            _results.push(void 0);
          }
        }
        return _results;
      });

      w.on('scroll', scrl);
      scope.$on('$destroy', function () {
        w.off('scroll', scrl);
      });
    }
  };
})
  .directive('spy', function () {
    return {
      restrict: 'A',
      require: '^scrollSpy',
      link: function (scope, elem, attrs, affix) {
        return affix.addSpy({
          id: attrs.spy,
          'in': function () {
            return elem.addClass('current');
          },
          out: function () {
            return elem.removeClass('current');
          }
        });
      }
    };
  });

