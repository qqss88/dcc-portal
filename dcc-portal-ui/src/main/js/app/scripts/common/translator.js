(function() {
  'use strict';

  var module = angular.module('icgc.common.translator', []);

  /**
   * Translating value/code to human readable text, this bascially acts as a 
   */
  module.service('ValueTranslator', function(Consequence, DataType, CodeTable) {
     
     function getTranslatorModule(type) {

       if (type === 'consequenceType') {
         return Consequence;
       } else if (type === 'availableDataTypes') {
         return DataType;
       } else {
         return CodeTable;
       }
     }

     function humanReadable(str) {
       var res = str;
       res = res.replace(/_/g, ' ').replace(/^\s+|\s+$/g, '');
       res = res.charAt(0).toUpperCase() + res.slice(1);
       return res;
     }

     this.translate = function(id, type) {
       if (!id) return '';
       if (id === '_missing') { return 'No Data'; }

       if (angular.isDefined(type)) {
         return getTranslatorModule(type).translate(id) || humanReadable(id);
       } else {
         return humanReadable(id);
       }
     };

     this.tooltip = function(id, type) {
       if (!id) return '';
       if (id === '_missing') { return 'No Data'; }

       if (angular.isDefined(type)) {
         return getTranslatorModule(type).tooltip(id);
       } else {
         return id;
       }
     };

     this.readable = humanReadable;

  });

  module.filter('readable', function(ValueTranslator) {
    return function (id) {
      return ValueTranslator.readable(id);
    };
  });


  module.filter('trans', function (ValueTranslator) {
    return function (id, type) {
      return ValueTranslator.translate(id, type);
    };
  });

  module.filter('define', function (ValueTranslator) {
    return function (id, type) {
      return ValueTranslator.tooltip(id, type);
    };
  });





})();
