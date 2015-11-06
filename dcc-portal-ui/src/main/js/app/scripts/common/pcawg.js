(function() {
  'use strict';

  var module = angular.module('icgc.common.pcawg', []);

  // TODO: This service needs to be generalized as this does not deal with
  // just PCAWG alone it is also used in the general Repositories Data file
  // service. I am leaving (MMoncada) this alone right now because other
  // code in the application depends on this service.
  module.service('PCAWG', function() {

    var data = [
      {id: 'DNA-Seq', shortLabel: 'Whole Genomes'},
      {id: 'RNA-Seq', shortLabel: 'Whole Transcriptomes'},
      {id: 'SSM', shortLabel: 'Simple Somatic Mutations'},
      {id: 'CNSM', shortLabel: 'Copy Number Somatic Mutations'},
      {id: 'StSM', shortLabel: 'Structural Somatic Mutations'}
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

