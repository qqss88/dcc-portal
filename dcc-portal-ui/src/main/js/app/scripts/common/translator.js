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
       } 
       return CodeTable;
     }

     function humanReadable(str) {
       var res = str;
       if (_.isEmpty(res) === true) { return res; }
       res = res.replace(/_/g, ' ').replace(/^\s+|\s+$/g, '');
       res = res.charAt(0).toUpperCase() + res.slice(1);
       return res;
     }

     this.translate = function(id, type) {
       if (!id) { return ''; }
       if (id === '_missing') { return 'No Data'; }

       return getTranslatorModule(type).translate(id) || humanReadable(id);
       //if (angular.isDefined(type)) {
       //  return getTranslatorModule(type).translate(id) || humanReadable(id);
       //} else {
       //  return humanReadable(id);
       //}
     };

     this.tooltip = function(id, type) {
       if (!id) { return ''; }
       if (id === '_missing') { return 'No Data'; }

       return getTranslatorModule(type).tooltip(id) || id;
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

  module.filter('datatype', function (ValueTranslator) {
    return function (id) {
      return ValueTranslator.tooltip(id, 'availableDataTypes');
    };
  });

  module.filter('universe', function (Extensions) {
    return function (id) {
      return _.find(Extensions.GENE_SET_ROOTS, function(u) {
        return u.universe === id;
      }).name;
    };
  });


  module.filter('define', function (ValueTranslator) {
    return function (id, type) {
      return ValueTranslator.tooltip(id, type);
    };
  });





})();
