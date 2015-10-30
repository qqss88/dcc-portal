(function() {
  'use strict';

  var module = angular.module('icgc.common.datatype', []);

  /**
   * Binds ICGC data types
   */
  module.service('DataType', function() {
    var data = [
      {id: 'donor', shortLabel: 'Clinical', label: 'Clinical data'},
      {id: 'clinical', shortLabel: 'Clinical', label: 'Clinical data'},
      {id: 'ssm', shortLabel: 'SSM', label: 'Simple somatic mutation'},
      {id: 'sgv', shortLabel: 'SGV', label: 'Simple Germline variation'},
      {id: 'cnsm', shortLabel: 'CNSM', label: 'Copy number somatic mutation'},
      {id: 'stsm', shortLabel: 'STSM', label: 'Structural somatic mutations'},
      {id: 'exp_array', shortLabel: 'EXP-A', label: 'Array-based Gene Expression'},
      {id: 'exp_seq', shortLabel: 'EXP-S', label: 'Sequencing-based Gene Expression'},
      {id: 'pexp', shortLabel: 'PEXP', label: 'Protein Expression'},
      {id: 'mirna_seq', shortLabel: 'miRNA-S', label: 'Sequence-based miRNA Expression'},
      {id: 'jcn', shortLabel: 'JCN', label: 'Exon junctions'},
      {id: 'meth_array', shortLabel: 'METH-A', label: 'Array-based DNA Methylation'},
      {id: 'meth_seq', shortLabel: 'METH-S', label: 'Sequencing-based DNA Methylation'},
      {id: 'aligned reads', shortLabel:'Aligned Reads', label:'Aligned Sequencing Reads'}
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


