'use strict';

/**
 * select-on-click: Select content on click event
 * disable-events: Disable pointer events reduce opacity to give it a disabled look and feel
 * autofocus: Focus element
 */
angular.module('icgc.ui.events', [])
.directive('selectOnClick', function() {
  return {
    restrict: 'A',
    link: function(scope, element) {
      element.on('click', function() {
        this.select();
      });
    }
  };
})
.directive('disableEvents', function() {
  return {
    restrict: 'A',
    replace: true,
    scope: {
      disableEvents: '='
    },
    link: function(scope, element) {

      function toggleEvents(predicate) {
        if (predicate === true) {
          element.css('pointer-events', 'none');
          element.css('opacity', 0.65);
        } else {
          element.css('pointer-events', '');
          element.css('opacity', '1');
        }
      }

      scope.$watch('disableEvents', function(n) {
        if (angular.isDefined(n)) {
          toggleEvents(n);
        }
      });
    }
  };
})
.directive('autofocus', function ($timeout) {
  return {
    restrict: 'A',
    link: function($scope, $element) {
      $timeout(function() {
        $element[0].focus();
      });
    }
  };
});



