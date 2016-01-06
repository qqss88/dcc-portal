(function() {
  'use strict';

  var module = angular.module('icgc.common.datatype', []);

  /**
   * Binds ICGC data types
   */
  module.service('DataType', function() {
    var data = [
      {id: 'donor', shortLabel: 'Clinical', label: 'Clinical Data'},
      {id: 'clinical', shortLabel: 'Clinical', label: 'Clinical Data'},
      {id: 'ssm', shortLabel: 'SSM', label: 'Simple Somatic Mutation'},
      {id: 'sgv', shortLabel: 'SGV', label: 'Simple Germline Variation'},
      {id: 'cnsm', shortLabel: 'CNSM', label: 'Copy Number Somatic Mutation'},
      {id: 'stsm', shortLabel: 'STSM', label: 'Structural Somatic Mutations'},
      {id: 'exp_array', shortLabel: 'EXP-A', label: 'Array-based Gene Expression'},
      {id: 'exp_seq', shortLabel: 'EXP-S', label: 'Sequencing-based Gene Expression'},
      {id: 'pexp', shortLabel: 'PEXP', label: 'Protein Expression'},
      {id: 'mirna_seq', shortLabel: 'miRNA-S', label: 'Sequence-based miRNA Expression'},
      {id: 'jcn', shortLabel: 'JCN', label: 'Exon Junctions'},
      {id: 'meth_array', shortLabel: 'METH-A', label: 'Array-based DNA Methylation'},
      {id: 'meth_seq', shortLabel: 'METH-S', label: 'Sequencing-based DNA Methylation'},
      {id: 'aligned reads', shortLabel:'Aligned Reads', label:'Aligned Sequencing Reads'},
      {id: 'stgv' , shortLabel: 'StGV', label:'Structural Germline Variants'}
    ];

    var shortLabelMap = {}, labelMap = {};
    data.forEach(function(datatype) {
      shortLabelMap[datatype.id] = datatype.shortLabel;
      labelMap[datatype.id] = datatype.label;
    });

    this.get = function() {
      return data;
    };

    this.precedence = function() {
      return _.pluck(data, 'id');
    };

    this.translate = function(id) {
      return shortLabelMap[id]; 
    };

    this.tooltip = function(id) {
      return labelMap[id.toLowerCase()];
    };

  });

})();


