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
       return null;
     };

     this.translate = function(id, type) {
       if (id === '_missing') { return 'No Data'; }
       var t = getTranslatorModule(type);
       if (t) { return t.translate(id); }

       return id;
     };

     this.tooltip = function(id, type) {
       if (id === '_missing') { return 'No Data'; }
       var t = getTranslatorModule(type);
       if (t) { return t.tooltip(id); }

       return id;
     };

  });

})();
