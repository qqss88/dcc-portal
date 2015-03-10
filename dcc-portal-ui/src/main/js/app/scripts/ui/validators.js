'use strict';

angular.module('icgc.ui.validators', []).directive('integerValidator', function() {
    return {
      require: '?ngModel',
      link: function(scope, element, attrs, ngModelCtrl) {
        if(!ngModelCtrl) {
          return;
        }

        ngModelCtrl.$parsers.push(function(val) {
          if (angular.isUndefined(val)) {
            val = '';
          }
          var clean = val.replace( /[^0-9]+/g, '');
          clean = clean.replace(/^0+/,'');
          if (val !== clean) {
            ngModelCtrl.$setViewValue(clean);
            ngModelCtrl.$render();
          }
          return clean;
        });
      }
    };
  });
