(function() {
  'use strict';

  var module = angular.module('icgc.common.codetable', []);

  /**
   * Binds misc. code/value lookup 
   */
  module.service('CodeTable', function() {

    var translateLookup = {
      // Mutation type
      'single base substitution': 'Substitution',
      'insertion of <=200bp': 'Insertion',
      'deletion of <=200bp': 'Deletion',
      'multiple base substitution (>=2bp and <=200bp)': 'MSub',

      // OOZIE worflow status
      'NOT_FOUND': 'Not Found',
      'RUNNING': 'Running',
      'SUCCEEDED': 'Succeeded',
      'FAILED': 'Failed',
      'KILLED': 'Cancelled',
      'PREP': 'In Preparation',
      'FINISHING': 'Cleaning Up', // This is an artificial state

      // Functional Impact prediction categories
      'TOLERATED': 'Tolerated',
      'DAMAGING': 'Damaging',

      // GO Ontology
      'colocalizes_with': 'Colocalizes With',
      'contributes_to': 'Contributes To',

      // Biotype
      'lincRNA': 'lincRNA',
      'miRNA': 'miRNA',
      'snRNA': 'snRNA',
      'snoRNA': 'snoRNA',
      'rRNA': 'rRNA',
      '3prime_overlapping_ncrna': '3\' Overlapping ncRNA',
      'Mt_rRNA': 'Mt rRNA',
    };


    var tooltipLookup = {
      // Sequencing analysis types (Sequencing strategy)
      'WGS': 'Whole Genome Sequencing - random sequencing of the whole genome.',
      'WGA': 'Whole Genome Amplification followed by random sequencing.',
      'WXS': 'Random sequencing of exonic regions selected from the genome.',
      'RNA-Seq': 'Random sequencing of whole transcriptome, also known as Whole Transcriptome Shotgun Sequencing, ' +
                 'or WTSS',
      'miRNA-Seq': 'Micro RNA sequencing strategy designed to capture post-transcriptional RNA elements and ' +
                   'include non-coding functional elements.',
      'ncRNA-Seq': 'Capture of other non-coding RNA types, including post-translation modification types such as ' +
                   'snRNA (small nuclear RNA) or snoRNA (small nucleolar RNA), or expression regulation types such ' +
                   'as siRNA (small interfering RNA) or piRNA/piwi/RNA (piwi-interacting RNA).',
      'WCS': 'Random sequencing of a whole chromosome or other replicon isolated from a genome.',
      'CLONE': 'Genomic clone based (hierarchical) sequencing.',
      'POOLCLONE': 'Shotgun of pooled clones (usually BACs and Fosmids).',
      'AMPLICON': 'Sequencing of overlapping or distinct PCR or RT-PCR products. For example, metagenomic ' +
                  'community profiling using SSU rRNA.',
      'CLONEEND': 'Clone end (5\', 3\', or both) sequencing.',
      'FINISHING': 'Sequencing intended to finish (close) gaps in existing coverage.',
      'ChIP-Seq': 'chromatin immunoprecipitation.',
      'MNase-Seq': 'following MNase digestion.',
      'DNase-Hypersensitivity': 'Sequencing of hypersensitive sites, or segments of open chromatin that are more ' +
                                'readily cleaved by DNaseI.',
      'Bisulfite-Seq': 'MethylC-seq. Sequencing following treatment of DNA with bisulfite to convert cytosine ' +
                       'residues to uracil depending on methylation status.',
      'EST': 'Single pass sequencing of cDNA templates',
      'FL-cDNA': 'Full-length sequencing of cDNA templates',
      'CTS': 'Concatenated Tag Sequencing',
      'MRE-Seq': 'Methylation-Sensitive Restriction Enzyme Sequencing.',
      'MeDIP-Seq': 'Methylated DNA Immunoprecipitation Sequencing.',
      'MBD-Seq': 'Methyl CpG Binding Domain Sequencing.',
      'Tn-Seq': 'Quantitatively determine fitness of bacterial genes based on how many times a purposely seeded ' +
                'transposon gets inserted into each gene of a colony after some time.',
      'VALIDATION': 'CGHub special request: Independent experiment to re-evaluate putative variants.',
      'FAIRE-seq': 'Formaldehyde Assisted Isolation of Regulatory Elements',
      'SELEX': 'Systematic Evolution of Ligands by EXponential enrichment',
      'RIP-Seq': 'Direct sequencing of RNA immunoprecipitates (includes CLIP-Seq, HITS-CLIP and PAR-CLIP).',
      'ChIA-PET': 'Direct sequencing of proximity-ligated chromatin immunoprecipitates.',
      'OTHER': 'Library strategy not listed.'

    };

    
    this.translate = function(id) {
      return translateLookup[id]; 
    };

    this.tooltip = function(id) {
      return tooltipLookup[id]; 
    };

  });

})();
