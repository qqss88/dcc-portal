/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

(function () {
  'use strict';

  var module = angular.module('app.common.services', []);

  module.factory('PCAWG', function() {
    return {
      isPCAWGStudy: function(term) {
        return term === 'PCAWG Study';
      }
    };
  });


  module.factory('RestangularNoCache', function(Restangular) {
    return Restangular.withConfig(function(RestangularConfigurer) {
      RestangularConfigurer.setDefaultHttpFields({cache: false});
    });
  });


  module.factory('Page', function () {
    var title = 'Loading...',
      page = 'home',
      error = false,
      working = 0,
      exporting = false;

    return {
      title: function () {
        return title;
      },
      setTitle: function (t) {
        if (angular.isDefined(t)) {
          title = t;
        }
      },
      page: function () {
        return page;
      },
      setPage: function (p) {
        if (angular.isDefined(p)) {
          page = p;
        }
      },
      startWork: function () {
        working++;
      },
      stopWork: function () {
        if (working > 0) {
          working--;
        }
      },
      working: function () {
        return working;
      },
      // For reseting state on error
      stopAllWork: function() {
        working = 0;
        angular.element(document.querySelector('article')).css('visibility', 'visible');
        angular.element(document.querySelector('aside')).css('visibility', 'visible');
      },
      setError: function(e) {
        error = e;
        if (error === true) {
          angular.element(document.querySelector('article')).css('visibility', 'hidden');
          angular.element(document.querySelector('aside')).css('visibility', 'hidden');
        }
      },
      getError: function() {
        return error;
      },
      startExport: function() {
        exporting = true;
      },
      stopExport: function() {
        exporting = false;
      },
      isExporting: function() {
        return exporting;
      }
    };
  });

  module.factory('Compatibility', function ($window) {

    function checkBase64() {
      if (!angular.isDefined($window.btoa)) {
        $window.btoa = base64.encode;
      }
      if (!angular.isDefined($window.atob)) {
        $window.atob = base64.decode;
      }
    }

    function checkLog() {
      if (!($window.console && console.log)) {
        $window.console = {
          log: function () {
          },
          debug: function () {
          },
          info: function () {
          },
          warn: function () {
          },
          error: function () {
          }
        };
      }
    }

    function checkTime() {
      if ($window.console && typeof($window.console.time === 'undefined')) {
        $window.console.time = function () {
        };
        $window.console.timeEnd = function () {
        };
      }
    }

    return {
      run: function () {
        checkBase64();
        checkLog();
        checkTime();
      }
    };
  });

  module.service('Settings', function (RestangularNoCache) {
    this.get = function () {
      return RestangularNoCache.one('settings').get();
    };
  });

  module.service('ProjectCache', function(Projects) {
    var promise = null;
    var cache = {};

    function getData() {
      if (promise !== null)  {
        return promise;
      }
      promise = Projects.getList().then(function(data) {
        data.hits.forEach(function(project) {
          cache[project.id] = project.name;
        });
        return cache;
      });
      return promise;
    }

    this.getData = getData;
  });


  /**
  * Centralized location for tooltip text
  */
  module.service('TooltipText', function() {
    this.ENRICHMENT = {
      OVERVIEW_GENES_OVERLAP: 'Intersection between genes involved in Universe and input genes.',
      INPUT_GENES: 'Number of genes resulting from original query with upper limit. <br>' +
        'Input genes for this enrichment analysis result.',
      FDR: 'False Discovery Rate',
      GENESET_GENES: 'Number of genes involved in this gene set.',
      GENESET_GENES_OVERLAP: 'Intersection between genes involved in this gene set and input genes.',
      GENESET_DONORS: 'Number of donors filtered by genes in overlap',
      GENESET_MUTATIONS: 'Number of simple somatic mutations filtered by genes in overlap.',
      // GENESET_EXPECTED: 'Number of genes expected by chance',
      GENESET_EXPECTED: 'Number of genes in overlap expected by chance',
      GENESET_PVALUE: 'P-Value using hypergeometric test',
      GENESET_ADJUSTED_PVALUE: 'Adjusted P-Value using the Benjamini-Hochberg procedure'
    };
  });


  module.service('DefinitionService', function(Extensions) {
    var definitions = {

      // Data Types
      'Clinical': 'Clinical Data',
      'SSM': 'Simple Somatic Mutation',
      'CNSM': 'Copy Number Somatic Mutation',
      'StSM': 'Structural Somatic Mutation',
      'SGV': 'Simple Germline Variation',
      'CNGV': 'Copy Number Germline Variation',
      'StGV': 'Structural Germline Variation',
      'PEXP': 'Protein Expression',
      'JCN': 'Exon Junction',
      'METH-A': 'Array-based DNA Methylation',
      'METH-S': 'Sequencing-based DNA Methylation',
      'miRNA-S': 'Sequencing-based miRNA Expression',
      'EXP-S': 'Sequencing-based Gene Expression',
      'EXP-A': 'Array-based Gene Expression',

      // SSM mutation types
      'Substitution': 'single base substitution',
      'Insertion': 'insertion of <=200bp',
      'Deletion': 'deletion of <=200bp',
      'MSub': 'multiple base substitution (>=2bp and <=200bp)',

      // SSM mutation consequences
      // FIXME: This gets translated/defined twice...
      'Consequence': 'SO term: consequence_type',
      'Frameshift': 'SO term: frameshift_variant',
      'Missense': 'SO term: missense_variant',
      'Start Lost': 'SO term: start_lost',
      'Initiator Codon': 'SO term: initiator_codon_variant',
      'Stop Gained': 'SO term: stop_gained',
      'Stop Lost': 'SO term: stop_lost',
      'Exon Loss': 'SO term: exon_loss_variant',
      'Splice Acceptor': 'SO term: splice_acceptor_variant',
      'Splice Donor': 'SO term: splice_donor_variant',
      'Splice Region': 'SO term: splice_region_variant',
      'Rare Amino Acid': 'SO term: rare_amino_acid_variant',
      'Start Gained': 'SO term: 5_prime_UTR_premature_start_codon_gain_variant',
      'Coding Sequence': 'SO term: coding_sequence_variant',
      '5 UTR Truncation': 'SO term: 5_prime_UTR_truncation',
      '3 UTR Truncation': 'SO term: 3_prime_UTR_truncation',
      'Non ATG Start': 'SO term: non_canonical_start_codon',
      'Disruptive Inframe Deletion': 'SO term: disruptive_inframe_deletion',
      'Inframe Deletion': 'SO term: inframe_deletion',
      'Disruptive Inframe Insertion': 'SO term: disruptive_inframe_insertion',
      'Inframe Insertion': 'SO term: inframe_insertion',
      'Regulatory Region': 'SO term: regulatory_region_variant',
      'miRNA': 'SO term: miRNA',
      'Conserved Intron': 'SO term: conserved_intron_variant',
      'Conserved Intergenic': 'SO term: conserved_intergenic_variant',
      '5 UTR': 'SO term: 5_prime_UTR_variant',
      'Upstream': 'SO term: upstream_gene_variant',
      'Synonymous': 'SO term: synonymous_variant',
      'Stop Retained': 'SO term: stop_retained_variant',
      '3 UTR': 'SO term: 3_prime_UTR_variant',
      'Exon': 'SO term: exon_variant',
      'Downstream': 'SO term: downstream_gene_variant',
      'Intron': 'SO term: intron_variant',
      'Transcript': 'SO term: transcript_variant',
      'Gene': 'SO term: gene_variant',
      'Intragenic': 'SO term: intragenic_variant',
      'Intergenic': 'SO term: intergenic_region',
      'Chromosome': 'SO term: chromosome',


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

    // Gene Set universe mapping
    Extensions.GENE_SET_ROOTS.forEach(function(root) {
      if (root.universe) {
        definitions[root.universe] = root.name;
      }
    });


    this.getDefinitions = function() {
      return definitions;
    };

  });

  module.service('TranslationService', function() {
    var translations = {
      // No Data
      '_missing': 'No Data',

      // GO Ontology
      'colocalizes_with': 'Colocalizes With',
      'contributes_to': 'Contributes To',

      // OOZIE worflow status
      'NOT_FOUND': 'Not Found',
      'RUNNING': 'Running',
      'SUCCEEDED': 'Succeeded',
      'FAILED': 'Failed',
      'KILLED': 'Cancelled',
      'PREP': 'In Preparation',
      'FINISHING': 'Cleaning Up', // This is an artificial state

      // Facet Titles
      'id': 'Project',
      'projectId': 'Project',
      'primarySite': 'Primary Site',
      'primaryCountries': 'Country',
      'tumourStageAtDiagnosis': 'Tumour Stage',
      'vitalStatus': 'Vital Status',
      'diseaseStatusLastFollowup': 'Disease Status',
      'relapseType': 'Relapse Type',
      'ageAtDiagnosisGroup': 'Age at Diagnosis',
      'availableDataTypes': 'Available Data Type',
      'analysisTypes': 'Donor Analysis Type',
      'list': 'Gene sets',
      'verificationStatus': 'Verification Status',
      'consequenceType': 'Consequence Type',
      'functionalImpact': 'Functional Impact',
      'sequencingStrategy': 'Analysis Type',

      // Data Types
      'clinical': 'Clinical',
      'ssm': 'SSM',
      'cnsm': 'CNSM',
      'stsm': 'StSM',
      'sgv': 'SGV',
      'cngv': 'CNGV',
      'stgv': 'StGV',
      'pexp': 'PEXP',
      'jcn': 'JCN',
      'meth_array': 'METH-A',
      'meth_seq': 'METH-S',
      'exp_array': 'EXP-A',
      'exp_seq': 'EXP-S',
      'mirna_seq': 'miRNA-S',

      // biotype
      'lincRNA': 'lincRNA',
      'miRNA': 'miRNA',
      'snRNA': 'snRNA',
      'snoRNA': 'snoRNA',
      'rRNA': 'rRNA',
      '3prime_overlapping_ncrna': '3\' Overlapping ncRNA',
      'Mt_rRNA': 'Mt rRNA',

      // SSM mutation types
      'mutation_type': 'Type',
      'single base substitution': 'Substitution',
      'insertion of <=200bp': 'Insertion',
      'deletion of <=200bp': 'Deletion',
      'multiple base substitution (>=2bp and <=200bp)': 'MSub',

      // SSM mutation consequences
      'consequence_type': 'Consequence',
      'frameshift_variant': 'Frameshift',
      'missense_variant': 'Missense',
      'start_lost': 'Start Lost',
      'initiator_codon_variant': 'Initiator Codon',
      'stop_gained': 'Stop Gained',
      'stop_lost': 'Stop Lost',
      'exon_loss_variant': 'Exon Loss',
      'splice_acceptor_variant': 'Splice Acceptor',
      'splice_donor_variant': 'Splice Donor',
      'splice_region_variant': 'Splice Region',
      'rare_amino_acid_variant': 'Rare Amino Acid',
      '5_prime_UTR_premature_start_codon_gain_variant': 'Start Gained',
      'coding_sequence_variant': 'Coding Sequence',
      '5_prime_UTR_truncation': '5 UTR Truncation',
      '3_prime_UTR_truncation': '3 UTR Truncation',
      'non_canonical_start_codon': 'Non ATG Start',
      'disruptive_inframe_deletion': 'Disruptive Inframe Deletion',
      'inframe_deletion': 'Inframe Deletion',
      'disruptive_inframe_insertion': 'Disruptive Inframe Insertion',
      'inframe_insertion': 'Inframe Insertion',
      'regulatory_region_variant': 'Regulatory Region',
      // 'miRNA': 'miRNA',   /* Same translation as biotype */
      'conserved_intron_variant': 'Conserved Intron',
      'conserved_intergenic_variant': 'Conserved Intergenic',
      '5_prime_UTR_variant': '5 UTR',
      'upstream_gene_variant': 'Upstream',
      'synonymous_variant': 'Synonymous',
      'stop_retained_variant': 'Stop Retained',
      '3_prime_UTR_variant': '3 UTR',
      'exon_variant': 'Exon',
      'downstream_gene_variant': 'Downstream',
      'intron_variant': 'Intron',
      'transcript_variant': 'Transcript',
      'gene_variant': 'Gene',
      'intragenic_variant': 'Intragenic',
      'intergenic_region': 'Intergenic',
      'chromosome': 'Chromosome',


      // Functional Impact prediction categories
      'TOLERATED': 'Tolerated',
      'DAMAGING': 'Damaging'
    };

    this.getTranslations = function() {
      return translations;
    };

  });

  // a function that returns an Angular 'structure' that represents a resuable
  // service which provides a simple hash/lookup function as well as fetching data via ajax.
  var KeyValueLookupServiceFactory = function ( fetch ) {

    return ['Restangular', '$log', function (Restangular, $log) {

      var _lookup = {};

      var _retrieve = function ( id ) {
        return _lookup [id];
      };
      var _echoOrDefault = function ( value, defaultValue ) {
        return ( value ) ?
          value :
          defaultValue || '';
      };
      var _noop = function () {};
      var _fetch = ( angular.isFunction (fetch) ) ? fetch : _noop;

      this.put = function ( id, name ) {
        if ( id && name ) {
          _lookup [id + ''] = name + '';

          $log.debug ( 'Updated lookup table is:' + JSON.stringify (_lookup) );
        }
      };
      this.get = function ( id ) {
        var result = _retrieve ( id );

        return _echoOrDefault ( result, id );
      };
      this.batchFetch = function ( ids ) {
        if ( angular.isArray (ids) ) {
          var missings = _.difference ( ids, _.keys (_lookup) );

          var setter = this.put;
          missings.forEach ( function (id) {
            _fetch ( Restangular, setter, id );
          });
        }
      };
    }];
  };

  // callback handler for gene-set name lookup
  var _fetchGeneSetNameById = function ( rest, setter, id ) {
    rest
      .one ( 'genesets', id )
      .get ( {field: ['id', 'name']} )
      .then ( function (geneSet) {

        if ( id === geneSet.id ) {
          setter ( id, geneSet.name );
        }
      });
  };

  module.service ( 'GeneSetNameLookupService', new KeyValueLookupServiceFactory (_fetchGeneSetNameById) );



  /**
   * Client side export of content using Blob and File, with fall back to server-side content echoing
   */
  module.service('ExportService', function() {
    this.exportData = function(filename, data) {
      if (window.Blob && window.File) {
        saveAs(new Blob([data], {type: 'text/plain;charset=utf-8'}), filename);
      } else {
        // Fallback (IE and other browsers that lack support), create a form and
        // submit a post request to bounce the download content against the server
        jQuery('<form method="POST" id="htmlDownload" action="/api/echo" style="display:none">' +
               '<input type="hidden" name="fileName" value="' + filename + '"/>' +
               '<input type="hidden" name="text" value="' + data + '"/>' +
               '<input type="submit" value="Submit"/>' +
               '</form>').appendTo('body');
        jQuery('#htmlDownload').submit();
        jQuery('#htmlDownload').remove();
      }

    };
  });

})();
