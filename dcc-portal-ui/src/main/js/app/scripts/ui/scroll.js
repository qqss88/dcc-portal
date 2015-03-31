'use strict';

angular.module('icgc.ui.scroll', [
  'icgc.ui.scroll.scrollto',
  'icgc.ui.scroll.scrollSpy'
]);


angular.module('icgc.ui.scroll.scrollto', []).directive('scrollto', function () {
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


angular.module('icgc.ui.scroll.scrollSpy', []).directive('scrollSpy', function ($window) {
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


