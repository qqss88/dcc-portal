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

  var module = angular.module('app.common', [
    'app.common.services',
    'app.common.footer',
    'icgc.common.version',
    'icgc.common.notify',
    'icgc.common.location'
  ]);


  // Translate project code into disease code. ie. BRCA-US => BRCA
  module.filter('diseaseCode', function() {
    return function(item) {
      return item.split('-')[0];
    };
  });

  module.filter('sum', function () {
    return function (items, param) {
      var ret = null;
      if (angular.isArray(items)) {
        ret = _.reduce(_.pluck(items, param), function (sum, num) {
          return sum + num;
        });
      }
      return ret;
    };
  });

  module.filter('startsWith', function () {
    return function (string, start) {
      var ret = null;
      if (angular.isString(string)) {
        ret = string.indexOf(start) === 0 ? string : null;
      }
      return ret;
    };
  });

  module.filter('numberPT', function ($filter) {
    return function (number) {
      if (angular.isNumber(number)) {
        return $filter('number')(number);
      } else {
        return number;
      }
    };
  });

  module.filter('trans', function () {
    return function (text, capitalize) {
      var translations;

      capitalize = capitalize || false;

      function human(text) {
        var t, words, cWords = [];
        if (!angular.isDefined(text) || (angular.isString(text) && !text.length)) {
          return '--';
        }
        if (!angular.isString(text)) {
          return text;
        }

        t = text.replace(/_/g, ' ').replace(/^\s+|\s+$/g, '');
        if (capitalize) {
          words = t.split(' ');
          words.forEach(function (word) {
//            word = word.toLowerCase();
            cWords.push(word.charAt(0).toUpperCase() + word.slice(1));
          });
          t = cWords.join(' ');
        }
        return t;
      }

      translations = {
        // No Data
        '_missing': 'No Data',

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
//      'donor_sex': 'Gender',
        'tumourStageAtDiagnosis': 'Tumour Stage',
        'vitalStatus': 'Vital Status',
        'diseaseStatusLastFollowup': 'Disease Status',
        'relapseType': 'Release Type',
        'ageAtDiagnosisGroup': 'Age at Diagnosis',
        'availableDataTypes': 'Available Data Type',
        'analysisTypes': 'Donor Analysis Type',
        'list': 'Gene sets',
        'verificationStatus': 'Verification Status',
        'consequenceType': 'Consequence Type',
        'functionalImpact': 'Functional Impact',
        'sequencingStrategy': 'Analysis Type',
//      '_gene_id': 'Gene ID',
//      'biotype': 'Type',
//      'gene_location': 'Location',
//        'reactome_pathways.name': 'Reactome Pathway',

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
//      'five_prime_UTR': '5\' UTR',
        'upstream_gene_variant': 'Upstream',
        'synonymous_variant': 'Syn',
        'stop_retained_variant': 'Stop Retained',
        '3_prime_UTR_variant': '3\' UTR',
        'downstream_gene_variant': 'Downstream',
        'intron_variant': 'Intron',
        'intergenic_region': 'Intergenic',
//      'intergenic_variant': 'intergenic',
        'intragenic_variant': 'Intragenic',


        // Functional Impact prediction categories
        'TOLERATED': 'Tolerated',
        'DAMAGING': 'Damaging'
      };

      return translations.hasOwnProperty(text) ? translations[text] : human(text);
    };
  });

  module.filter('define', function () {
    return function (text, nullable) {
      var definitions;

      nullable = nullable || false;

      definitions = {
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

      // If translation found return that
      // else if nullable return null
      // else pass through original text
      return definitions.hasOwnProperty(text) ? definitions[text] : nullable ? null : text;
    };
  });

  module.filter('highlight', function () {
    return function (text, search, hide) {
      text = text || '';
      hide = hide || false;
      if (search) {
        text = angular.isArray(text) ? text.join(', ') : text.toString();
        // Shrink extra spaces, removes non alpha-numeric chars
        search = search.toString().replace(/\s+/g, ' ').replace(/[^a-zA-Z0-9\s]/g, '').split(' ');
        for (var i = 0; i < search.length; ++i) {
          text = text.replace(new RegExp(search[i], 'gi'), '^$&$');
        }

        // if match return text
        if (text.indexOf('^') !== -1) {
          return text.replace(/\^/g, '<span class="match">').replace(/\$/g, '</span>');
        } else { // no match
          if (hide) {
            return '';
          } // hide
        }
      }

      // return base text if no match and not hiding
      return text;
    };
  });

  module.factory('debounce', function ($timeout, $q) {
    return function (func, wait, immediate) {
      var timeout, deferred;
      deferred = $q.defer();

      return function () {
        var context, later, callNow, args;

        context = this;
        args = arguments;
        later = function () {
          timeout = null;
          if (!immediate) {
            deferred.resolve(func.apply(context, args));
            deferred = $q.defer();
          }
        };
        callNow = immediate && !timeout;
        if (timeout) {
          $timeout.cancel(timeout);
        }
        timeout = $timeout(later, wait);
        if (callNow) {
          deferred.resolve(func.apply(context, args));
          deferred = $q.defer();
        }
        return deferred.promise;
      };
    };
  });

  module.filter('typecv', function () {
    return function (type) {
      var types = {
        'gene-centric': 'gene',
        'mutation-centric': 'mutation',
        'donor-centric': 'donor'
      };
      return types[type];
    };
  });

  module.filter('unique', function () {
    return function (items) {
      var i, set, item;

      set = [];
      if (items && items.length) {
        for (i = 0; i < items.length; ++i) {
          item = items[i];
          if (set.indexOf(item) === -1) {
            set.push(item);
          }
        }
      }
      return set;
    };
  });

// Convert a non-array item into an array
  module.filter('makeArray', function () {
    return function (items) {
      if (angular.isArray(items)) {
        return items;
      }
      return [items];
    };
  });


// Join parallel arrays into a single array
// eg. [ ['a', 'b', 'c'], ['d', 'e', 'f'] ] | joinFields:''  => ['ad', 'be', 'cf']
  module.filter('joinFields', function () {
    return function (items, delim, checkEmpty) {
      var i, j, list, tempList, joinedItem, hasEmpty;

      list = [];

      // Normalize
      for (i = 0; i < items.length; i++) {
        if (!angular.isArray(items[i])) {
          items[i] = [items[i]];
        }
      }

      // Join
      for (i = 0; i < items[0].length; i++) {
        tempList = [];
        hasEmpty = false;
        for (j = 0; j < items.length; j++) {
          tempList.push(items[j][i]);
          if (items[j][i] === '') {
            hasEmpty = true;
          }
        }

        // Skip join if one of the item is empty string
        if (checkEmpty && checkEmpty === true && hasEmpty === true) {
          continue;
        }

        joinedItem = tempList.join(delim).trim();
        if (joinedItem && joinedItem !== '') {
          list.push(joinedItem);
        }
      }
      return list;
    };
  });

  module.filter('bytes', function () {
    return function (input) {
      var sizes = ['B', 'KB', 'MB', 'GB', 'TB'],
        postTxt = 0,
        bytes = input,
        precision = 2;

      if (bytes <= 1024) {
        precision = 0;
      }

      while (bytes >= 1024) {
        postTxt++;
        bytes = bytes / 1024;
      }

      return Number(bytes).toFixed(precision) + ' ' + sizes[postTxt];
    };
  });
})();
