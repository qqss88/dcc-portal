'use strict';

/**
 * The following features are still outstanding: animation as a
 * function, placement as a function, inside, support for more triggers than
 * just mouse enter/leave, html tooltips, and selector delegation.
 */
angular.module('icgc.ui.tooltip', [
  'ui.bootstrap.position',
  'template/tooltip/tooltip-html-unsafe-popup.html',
  'template/tooltip/tooltip-popup.html'
])

/**
 * The $tooltip service creates tooltip- and popover-like directives as well as
 * houses global options for them.
 */
  .provider('$tooltip', function () {
    // The default options tooltip and popover.
    var defaultOptions = {
      placement: 'top',
      animation: true,
      popupDelay: 0
    };

    // Default hide triggers for each show trigger
    var triggerMap = {
      'mouseenter': 'mouseleave',
      //'click': 'click',
      'focus': 'blur'
    };

    // The options specified to the provider globally.
    var globalOptions = {};

    /**
     * `options({})` allows global configuration of all tooltips in the
     * application.
     *
     *   var app = angular.module( 'App', ['ui.bootstrap.tooltip'], function( $tooltipProvider ) {
   *     // place tooltips left instead of top by default
   *     $tooltipProvider.options( { placement: 'left' } );
   *   });
     */
    this.options = function (value) {
      angular.extend(globalOptions, value);
    };

    /**
     * This allows you to extend the set of trigger mappings available. E.g.:
     *
     *   $tooltipProvider.setTriggers( 'openTrigger': 'closeTrigger' );
     */
    this.setTriggers = function setTriggers(triggers) {
      angular.extend(triggerMap, triggers);
    };

    /**
     * This is a helper function for translating camel-case to snake-case.
     */
    function snake_case(name) {
      var regexp = /[A-Z]/g;
      var separator = '-';
      return name.replace(regexp, function (letter, pos) {
        return (pos ? separator : '') + letter.toLowerCase();
      });
    }

    /**
     * Returns the actual instance of the $tooltip service.
     * TODO support multiple triggers
     */
    this.$get = function ($window, $compile, $timeout, $parse, $document, $position, $interpolate) {
      return function $tooltip(type, prefix, defaultTriggerShow) {
        var options = angular.extend({}, defaultOptions, globalOptions);

        /**
         * Returns an object of show and hide triggers.
         *
         * If a trigger is supplied,
         * it is used to show the tooltip; otherwise, it will use the `trigger`
         * option passed to the `$tooltipProvider.options` method; else it will
         * default to the trigger supplied to this directive factory.
         *
         * The hide trigger is based on the show trigger. If the `trigger` option
         * was passed to the `$tooltipProvider.options` method, it will use the
         * mapped trigger from `triggerMap` or the passed trigger if the map is
         * undefined; otherwise, it uses the `triggerMap` value of the show
         * trigger; else it will just use the show trigger.
         */
        function getTriggers(trigger) {
          var show = trigger || options.trigger || defaultTriggerShow;
          var hide = triggerMap[show] || show;
          return {
            show: show,
            hide: hide
          };
        }

        var directiveName = snake_case(type);

        var startSym = $interpolate.startSymbol();
        var endSym = $interpolate.endSymbol();
        var template =
          '<' + directiveName + '-popup ' +
          'title="' + startSym + 'tt_title' + endSym + '" ' +
          'content="' + startSym + 'tt_content' + endSym + '" ' +
          'placement="' + startSym + 'tt_placement' + endSym + '" ' +
          'animation="tt_animation()" ' +
          'is-open="tt_isOpen"' +
          '>' +
          '</' + directiveName + '-popup>';

        return {
          restrict: 'EA',
          scope: true,
          link: function link(scope, element, attrs) {
            var tooltip = $compile(template)(scope);
            var transitionTimeout;
            var popupTimeout;
            var $body;
            var appendToBody = angular.isDefined(options.appendToBody) ? options.appendToBody : false;
            var triggers = getTriggers(undefined);
            var hasRegisteredTriggers = false;
            var ellipsed = false;

            // By default, the tooltip is not open.
            // TODO add ability to start tooltip opened
            scope.tt_isOpen = false;

            function toggleTooltipBind() {
              if (!scope.tt_isOpen) {
                showTooltipBind();
              } else {
                hideTooltipBind();
              }
            }

            // Show the tooltip with delay if specified, otherwise show it immediately
            function showTooltipBind() {
              if (scope.tt_popupDelay) {
                popupTimeout = $timeout(show, scope.tt_popupDelay);
              } else {
                scope.$apply(show);
              }
            }

            function hideTooltipBind() {
              scope.$apply(function () {
                hide();
              });
            }


            function calculatePlacement(placement, position, ttWidth, ttHeight) {
              var ttPosition = {};
              // Calculate the tooltip's top and left coordinates to center it with
              // this directive.
              switch (placement) {
              case 'right':
                ttPosition = {
                  top: position.top + position.height / 2 - ttHeight / 2,
                  left: position.left + position.width
                };
                break;
              case 'bottom':
                ttPosition = {
                  top: position.top + position.height,
                  left: position.left + position.width / 2 - ttWidth / 2
                };
                break;
              case 'left':
                ttPosition = {
                  top: position.top + position.height / 2 - ttHeight / 2,
                  left: position.left - ttWidth
                };
                break;
              case 'parent':
              case 'replace':
                ttPosition = {
                  top: position.top - 5,
                  left: position.left - 2
                };
                break;
              default:
                ttPosition = {
                  top: position.top - ttHeight,
                  left: position.left > ttWidth / 4 ? (position.left + position.width / 2 - ttWidth / 2) : 0
                };
                break;
              }
              ttPosition.top += 'px';
              ttPosition.left += 'px';

              return ttPosition;
            }

            // Show the tooltip popup element.
            function show() {
              var position,
                ttWidth,
                ttHeight,
                ttPosition;

              // Don't show empty tooltips.
              if (!scope.tt_content) {
                return;
              }

              // If there is a pending remove transition, we must cancel it, lest the
              // tooltip be mysteriously removed.
              if (transitionTimeout) {
                $timeout.cancel(transitionTimeout);
              }

              // Set the initial positioning.
              tooltip.css({ top: 0, left: 0, display: 'block' });

              // Now we add it to the DOM because need some info about it. But it's not
              // visible yet anyway.
              if (appendToBody) {
                $body = $body || $document.find('body');
                $body.append(tooltip);
              } else {
                //element.after(tooltip);
                // @icgc keeps to tooltip within the element so it stays open
                element.append(tooltip);
              }

              // Get the position of the directive element.
              if (scope.tt_placement === 'parent') {
                position = appendToBody ? $position.offset(element.parent()) : $position.position(element.parent());
              } else {
                position = appendToBody ? $position.offset(element) : $position.position(element);
              }

              // Get the height and width of the tooltip so we can center it.
              ttWidth = tooltip.prop('offsetWidth');
              ttHeight = tooltip.prop('offsetHeight');

              ttPosition = calculatePlacement(scope.tt_placement, position, ttWidth, ttHeight);

              // Now set the calculated positioning.
              tooltip.css(ttPosition);

              // And show the tooltip.
              scope.tt_isOpen = true;
            }

            // Hide the tooltip popup element.
            function hide() {
              // First things first: we don't show it anymore.
              scope.tt_isOpen = false;

              //if tooltip is going to be shown after delay, we must cancel this
              $timeout.cancel(popupTimeout);

              // And now we remove it from the DOM. However, if we have animation, we
              // need to wait for it to expire beforehand.
              // FIXME: this is a placeholder for a port of the transitions library.
//              if (angular.isDefined(scope.tt_animation) && scope.tt_animation()) {
//                transitionTimeout = $timeout(function() {
//                  tooltip.remove();
//                }, 500);
//              } else {
              tooltip.remove();
//              }
            }

            /**
             * Observe the relevant attributes.
             */
            attrs.$observe(type, function (val) {
              scope.tt_content = val;
            });

            attrs.$observe(prefix + 'Title', function (val) {
              scope.tt_title = val;
            });

            attrs.$observe(prefix + 'Placement', function (val) {
              scope.tt_placement = angular.isDefined(val) ? val : options.placement;
            });

            attrs.$observe(prefix + 'Animation', function (val) {
              scope.tt_animation = angular.isDefined(val) ? $parse(val) : function () {
                return options.animation;
              };
            });

            attrs.$observe(prefix + 'PopupDelay', function (val) {
              var delay = parseInt(val, 10);
              scope.tt_popupDelay = !isNaN(delay) ? delay : options.popupDelay;
            });

            attrs.$observe(prefix + 'Trigger', function (val) {

              if (hasRegisteredTriggers) {
                element.unbind(triggers.show, showTooltipBind);
                element.unbind(triggers.hide, hideTooltipBind);
              }

              triggers = getTriggers(val);

              if (triggers.show === triggers.hide) {
                element.bind(triggers.show, toggleTooltipBind);
              } else {
                element.bind(triggers.show, showTooltipBind);
                element.bind(triggers.hide, hideTooltipBind);

                // @ICGC, remove tooltip when click happens in an attempt to get around
                // event firing issues that cause sticky tooltips
                element.bind('click', hideTooltipBind);
              }

              hasRegisteredTriggers = true;
            });

            attrs.$observe(prefix + 'AppendToBody', function (val) {
              appendToBody = angular.isDefined(val) ? $parse(val)(scope) : appendToBody;
            });

            function ellipseCheck(hoverEvent) {
              if (ellipsed) {
                if (element.context.scrollWidth <= element.context.clientWidth) {
                  if (scope.tt_placement === 'parent') {
                    element.after(element.html());
                    element.remove();
                  } else {
                    // @icgc if not parent then probably facet, cannot remove since that will will all the facet
                    // bindings
                    element.unbind(triggers.show, showTooltipBind);
                    element.unbind(triggers.hide, hideTooltipBind);
                    element.bind(triggers.show, ellipseCheck);
                  }
                } else {
                  element.unbind(triggers.show, showTooltipBind);
                  element.unbind(triggers.hide, hideTooltipBind);
                  element.unbind(triggers.show, ellipseCheck);
                  element.bind(triggers.show, showTooltipBind);
                  element.bind(triggers.hide, hideTooltipBind);

                  if (hoverEvent) {
                    showTooltipBind();
                  }
                }
              }
            }

            attrs.$observe(prefix + 'Ellipsed', function (val) {
              ellipsed = angular.isDefined(val);
              // @icgc If used for displaying cut-off text, only show when needed
              ellipseCheck();
            });

            // if a tooltip is attached to <body> we need to remove it on
            // location change as its parent scope will probably not be destroyed
            // by the change.
            if (appendToBody) {
              scope.$on('$locationChangeSuccess', function closeTooltipOnLocationChangeSuccess() {
                if (scope.tt_isOpen) {
                  hide();
                }
              });
            }

            // Make sure tooltip is destroyed and removed.
            scope.$on('$destroy', function onDestroyTooltip() {
              if (scope.tt_isOpen) {
                hide();
              } else {
                tooltip.remove();
              }
            });
          }
        };
      };
    };
  })

  .directive('tooltipPopup', function () {
    return {
      restrict: 'E',
      replace: true,
      scope: { content: '@', placement: '@', animation: '&', isOpen: '&' },
      templateUrl: 'template/tooltip/tooltip-popup.html'
    };
  })

  .directive('tooltip', function ($tooltip) {
    return $tooltip('tooltip', 'tooltip', 'mouseenter');
  })

  .directive('tooltipHtmlUnsafePopup', function ($sce) {
    return {
      restrict: 'E',
      replace: true,
      scope: { content: '@', placement: '@', animation: '&', isOpen: '&' },
      templateUrl: 'template/tooltip/tooltip-html-unsafe-popup.html',
      link: function (scope) {
        scope.html = $sce.trustAsHtml(scope.content);
      }
    };
  })

  .directive('tooltipHtmlUnsafe', function ($tooltip) {
    return $tooltip('tooltipHtmlUnsafe', 'tooltip', 'mouseenter');
  });

angular.module('template/tooltip/tooltip-html-unsafe-popup.html', []).run(function ($templateCache) {
  $templateCache.put('template/tooltip/tooltip-html-unsafe-popup.html',
      '<div class="tooltip {{ placement }}" data-ng-class="{ in: isOpen(), fade: animation() }">\n' +
      '  <div class="tooltip-arrow"></div>\n' +
      '  <div class="tooltip-inner" data-ng-bind-html="html"></div>\n' +
      '</div>\n' +
      '');
});

angular.module('template/tooltip/tooltip-popup.html', []).run(['$templateCache', function ($templateCache) {
  $templateCache.put('template/tooltip/tooltip-popup.html',
      '<div class="tooltip {{ placement }}" data-ng-class="{ in: isOpen(), fade: animation() }">\n' +
      '  <div class="tooltip-arrow"></div>\n' +
      '  <div class="tooltip-inner" data-ng-bind="content"></div>\n' +
      '</div>\n' +
      '');
}]);
