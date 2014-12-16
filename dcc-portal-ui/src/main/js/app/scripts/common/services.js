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

  module.service('Settings', function (Restangular) {
    this.get = function () {
      return Restangular.one('settings').get();
    };
  });


  module.service('DefinitionService', function(Extensions) {
    var definitions = {
      // Projects
      'ALL-US': 'Acute Lymphoblastic Leukemia - US',
      'BLCA-CN': 'Bladder Cancer - CN',
      'BLCA-US': 'Bladder Urothelial Cancer - TGCA, US',
      'BOCA-UK': 'Bone Cancer - UK',
      'BRCA-EU': 'Breast ER+ and HER2- Cancer - EU/UK',
      'BRCA-FR': 'Breast Cancer - FR',
      'BRCA-KR': 'Breast Cancer - KR',
      'BRCA-MX': 'Breast Cancer - SIGMA, MX',
      'BRCA-UK': 'Breast Triple Negative/Lobular Cancer - UK',
      'BRCA-US': 'Breast Cancer - TCGA, US',
      'CESC-US': 'Cervical Squamous Cell Carcinoma - TCGA, US',
      'CLLE-ES': 'Chronic Lymphocyclic Leukemia - ES',
      'CMDI-UK': 'Chronic Myeloid Disorders - UK',
      'COAD-US': 'Colon Adenocarcinoma - TCGA, US',
      'COCA-CN': 'Colorectal Cancer - CN',
      'EOPC-DE': 'Early Onset Prostate Cancer - DE',
      'ESAD-UK': 'Esophageal Adenocarcinoma - UK',
      'ESCA-CN': 'Esophageal Cancer - CN',
      'GACA-CN': 'Gastric Cancer - CN',
      'GBM-US': 'Brain Glioblastoma Multiforme - TCGA, US',
      'HNCA-MX': 'Head and Neck Cancer - SIGMA, MX',
      'HNSC-US': 'Head and Neck Squamous Cell Carcinoma - TCGA, US',
      'KIRC-US': 'Kidney Renal Clear Cell Carcinoma - TCGA, US',
      'KIRP-US': 'Kidney Renal Papillary Cell Carcinoma - TCGA, US',
      'LAML-KR': 'Acute Myeloid Leukemia - KR',
      'LAML-US': 'Acute Myeloid Leukemia - TCGA, US',
      'LGG-US': 'Brain Lower Grade Gliona - TCGA, US',
      'LIAD-FR': 'Benign Liver Tumour - FR',
      'LICA-CN': 'Liver Cancer - CN',
      'LICA-FR': 'Liver Cancer - FR',
      'LIHC-US': 'Liver Hepatocellular carcinoma - TCGA, US',
      'LINC-JP': 'Liver Cancer - NCC, JP',
      'LIRI-JP': 'Liver Cancer - RIKEN, JP',
      'LUAD-US': 'Lung Adenocarcinoma - TCGA, US',
      'LUCA-DE': 'Lung Cancer - DE',
      'LUSC-KR': 'Lung Cancer - KR',
      'LUSC-US': 'Lung Squamous Cell Carcinoma - TCGA, US',
      'MALY-DE': 'Malignant Lymphoma - DE',
      'NACA-CN': 'Nasopharyngeal cancer - CN',
      'NBL-US': 'Brain Neuroblastoma - US',
      'NHLY-MX': 'Non Hodgkin Lymphoma - SIGMA, MX',
      'ORCA-IN': 'Oral Cancer - IN',
      'OV-AU': 'Ovarian Cancer - AU',
      'OV-US': 'Ovarian Serous Cystadenocarcinoma - TCGA, US',
      'PAAD-US': 'Pancreatic Cancer - TCGA, US',
      'PACA-AU': 'Pancreatic Cancer - AU',
      'PACA-CA': 'Pancreatic Cancer - CA',
      'PACA-IT': 'Pancreatic Cancer - IT',
      'PAEN-AU': 'Pancreatic Cancer Endocrine neoplasms - AU',
      'PBCA-DE': 'Pediatric Brain Cancer - DE',
      'PEME-CA': 'Pediatric Medulloblastoma - CA',
      'PRAD-CA': 'Prostate Adenocarcinoma - CA',
      'PRAD-UK': 'Prostate Adenocarcinoma - UK',
      'PRAD-US': 'Prostate Adenocarcinoma - TCGA, US',
      'PRCA-FR': 'Prostate Cancer - FR',
      'READ-US': 'Rectum Adenocarcinoma - TCGA, US',
      'RECA-CN': 'Renal cancer - CN',
      'RECA-EU': 'Renal Cell Cancer - EU/FR',
      'SKCA-BR': 'Skin Adenocarcinoma - BR',
      'SKCM-US': 'Skin Cutaneous melanoma - TCGA, US',
      'STAD-US': 'Gastric Adenocarcinoma - TCGA, US',
      'THCA-SA': 'Thyroid Cancer - SA',
      'THCA-US': 'Head and Neck Thyroid Carcinoma - TCGA, US',
      'UCEC-US': 'Uterine Corpus Endometrial Carcinoma- TCGA, US',

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
      'Consequence': 'SO term:consequence_type',
      'Frameshift': 'SO term: frameshift_variant',
      'Missense': 'SO term: missense',
      'Non Conservative Missense': 'SO term: non_conservative_missense_variant',
      'Initiator Codon': 'SO term: initiator_codon_variant',
      'Stop Gained': 'SO term: stop_gained',
      'Stop Lost': 'SO term: stop_lost',
      'Start Gained': 'SO term: start_gained',
      'Exon Lost': 'SO term: exon_lost',
      'Coding Seq': 'SO term: coding_sequence_variant',
      'Inframe Del': 'SO term: inframe_deletion',
      'Inframe Ins': 'SO term: inframe_insertion',
      'Splice': 'SO term: splice_region_variant',
      'Regulatory': 'SO term: regulatory_region_variant',
      'micro RNA': 'SO term: micro_rna',
      'NC Exon': 'SO term: non_coding_exon_variant',
      'NC Transcript': 'SO term: nc_transcript_variant',
      '5\' UTR': 'SO term: 5_prime_UTR_variant',
      'Upstream': 'SO term: upstream_gene_variant',
      'Syn': 'SO term: synonymous_variant',
      'Stop Retained': 'SO term: stop_retained_variant',
      '3\' UTR': 'SO term: 3_prime_UTR_variant',
      'Downstream': 'SO term: downstream_gene_variant',
      'Intron': 'SO term: intron_variant',
      'intergenic': 'SO term: intergenic_variant',
      'Intragenic': 'SO term: intragenic_variant',

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
      'missense': 'Missense',
      'non_conservative_missense_variant': 'Non Conservative Missense',
      'initiator_codon_variant': 'Initiator Codon',
      'stop_gained': 'Stop Gained',
      'stop_lost': 'Stop Lost',
      'start_gained': 'Start Gained',
      'exon_lost': 'Exon Lost',
      'coding_sequence_variant': 'Coding Seq',
      'inframe_deletion': 'Inframe Del',
      'inframe_insertion': 'Inframe Ins',
      'splice_region_variant': 'Splice',
      'regulatory_region_variant': 'Regulatory',
      'micro_rna': 'micro RNA',
      'non_coding_exon_variant': 'NC Exon',
      'nc_transcript_variant': 'NC Transcript',
      '5_prime_UTR_variant': '5\' UTR',
      'upstream_gene_variant': 'Upstream',
      'synonymous_variant': 'Syn',
      'stop_retained_variant': 'Stop Retained',
      '3_prime_UTR_variant': '3\' UTR',
      'downstream_gene_variant': 'Downstream',
      'intron_variant': 'Intron',
      'intergenic_region': 'Intergenic',
      'intragenic_variant': 'Intragenic',

      // Functional Impact prediction categories
      'TOLERATED': 'Tolerated',
      'DAMAGING': 'Damaging'
    };

    this.getTranslations = function() {
      return translations;
    };

  });


  module.service('FiltersUtil', function(Extensions) {

    // Gene id extensions
    var geneListKeys = _.pluck(Extensions.GENE_LISTS, 'id');

    this.removeExtensions = function(filters) {
      if (filters.hasOwnProperty('gene')) {

        geneListKeys.forEach(function(key) {
          if (filters.gene.hasOwnProperty(key)) {
            delete filters.gene[key];
          }
        });

        // delete filters.gene.inputGeneListId;
        if (_.isEmpty(filters.gene)) {
          delete filters.gene;
        }
      }
      return filters;
    };

    this.hasGeneListExtension = function(filters) {
      var hasGeneListExtension = false;
      if (filters.hasOwnProperty('gene')) {
        geneListKeys.forEach(function(key) {
          if (filters.gene.hasOwnProperty(key)) {
            hasGeneListExtension = true;
          }
        });
      }
      return hasGeneListExtension;
    };


    this.getGeneSetQueryType = function(type) {
      if (type === 'go_term') {
        return 'goTermId';
      } else if (type === 'curated_set') {
        return 'curatedSetId';
      } else if (type === 'pathway') {
        return 'pathwayId';
      }
      return 'geneSetId';
    };


    this.buildGeneSetFilterByType = function(type, geneSetIds) {
      var filter = {gene:{}};
      filter.gene[type] = {is: geneSetIds};
      console.log('building', JSON.stringify(filter));
      return filter;
    };


    this.buildUIFilters = function(filters) {
      var display = {};

      angular.forEach(filters, function(typeFilters, typeKey) {
        display[typeKey] = {};
        angular.forEach(typeFilters, function(facetFilters, facetKey) {
          var uiFacetKey = facetKey;

          // FIXME: no logic to handle "all" clause
          if (facetFilters.all) {
            return;
          }

          // Genelist expansion maps to gene id
          if (typeKey === 'gene' && (facetKey === Extensions.GENE_ID || _.contains(geneListKeys, facetKey))) {
            uiFacetKey = 'id';
          }

          // Remap gene ontologies
          if (uiFacetKey === 'hasPathway') {
            var uiTerm = 'Reactome Pathways';
            uiFacetKey = 'pathwayId';

            if (! display[typeKey].hasOwnProperty(uiFacetKey)) {
              display[typeKey][uiFacetKey] = {};
              display[typeKey][uiFacetKey].is = [];
            }
            display[typeKey][uiFacetKey].is.unshift({
              term: uiTerm,
              controlTerm: undefined,
              controlFacet: facetKey,
              controlType: typeKey
            });
            return;
          }

          // Allocate terms
          if (! display[typeKey].hasOwnProperty(uiFacetKey)) {
            display[typeKey][uiFacetKey] = {};
            display[typeKey][uiFacetKey].is = [];
          }


          facetFilters.is.forEach(function(term) {
            var uiTerm = term, isPredefined = false;
            if (typeKey === 'gene' && _.contains(geneListKeys, facetKey)) {

              uiTerm = _.find(Extensions.GENE_LISTS, function(gl) {
                return gl.id === facetKey;
              }).label;

              // 'Uploaded Gene List';
              isPredefined = true;
            } else if (typeKey === 'gene' && facetKey === 'goTermId') {
              var predefinedGO = _.find(Extensions.GENE_SET_ROOTS, function(set) {
                return set.id === term && set.type === 'go_term';
              });

              if (predefinedGO) {
                uiTerm = predefinedGO.name;
                isPredefined = true;
              }
            } else if (typeKey === 'gene' && facetKey === 'curatedSetId') {
              var predefinedCurated = _.find(Extensions.GENE_SET_ROOTS, function(set) {
                return set.id === term && set.type === 'curated_set';
              });
              if (predefinedCurated) {
                uiTerm = predefinedCurated.name;
                isPredefined = true;
              }
            }

            // Extension terms goes first
            if (isPredefined) {
              display[typeKey][uiFacetKey].is.unshift({
                term: uiTerm,
                controlTerm: term,
                controlFacet: facetKey,
                controlType: typeKey
              });
            } else {
              display[typeKey][uiFacetKey].is.push({
                term: uiTerm,
                controlTerm: term,
                controlFacet: facetKey,
                controlType: typeKey
              });
            }

          });
        });
      });
      return display;
    };

  });



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


  /*
  module.service('GeneSetService', function() {

    this.getGeneOntologies = function() {
    };

    this.getCuratedSets = function() {
    };

    this.getPathways = function() {
    };

    this.getAll = function() {
    };

    this.is
  });
  */

})();
