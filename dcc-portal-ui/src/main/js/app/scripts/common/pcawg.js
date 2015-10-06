(function() {
  'use strict';

  var module = angular.module('icgc.common.pcawg', []);


  module.service('PCAWG', function() {

    var data = [
      {id: 'DNA-Seq', shortLabel: 'Whole Genomes'},
      {id: 'RNA-Seq', shortLabel: 'Whole Transcriptomes'},
      {id: 'SSM', shortLabel: 'Simple Somatic Mutataions'},
      {id: 'CNSM', shortLabel: 'Copy Number Somatic Mutations'},
      {id: 'StSM', shortLabel: 'Structural Somatic Mutation'}
    ];

    var shortLabelMap = {};
    data.forEach(function(datatype) {
      shortLabelMap[datatype.id] = datatype.shortLabel;
    });


    this.translate = function(id) {
      return _.get (shortLabelMap, id, id);
    };

    this.precedence = function() {
      return _.pluck(data, 'id');
    };


    this.isPCAWGStudy = function(term) {
      return term === 'PCAWG';
    };

  });

})();

