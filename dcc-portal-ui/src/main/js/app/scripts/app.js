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

  var module = angular.module('icgc', [
    'ngSanitize',
    'ngAnimate',
    'ngCookies',

    // angular plugins
    'restangular',
    'ui.scrollfix',
    'ui.bootstrap.modal',
    'ui.bootstrap.position',
    'ui.router',
    'infinite-scroll',
    'angular-underscore',
    'angularytics',
    'chieffancypants.loadingBar',
    'btford.markdown',
    'LocalStorageModule',

    // 3rd party
    'highcharts',

    // modules
    'icgc.modules.genomeviewer',
    'proteinstructureviewer',

    // core
    // new
    'icgc.ui',
    'icgc.share',
    'icgc.facets',
    'icgc.projects',
    'icgc.donors',
    'icgc.genes',
    'icgc.mutations',
    'icgc.pathways',
    'icgc.advanced',
    'icgc.releases',
    'icgc.keyword',
    'icgc.browser',
    'icgc.genelist',
    'icgc.genesets',
    'icgc.visualization',
    'icgc.enrichment',
    'icgc.analysis',

    // old
    'app.ui',
    'app.common',
    'app.download',
    'app.downloader'
  ]);

  // Fix needed for loading subviews without jumping back to the
// top of the page:
// https://github.com/angular-ui/ui-router/issues/110#issuecomment-18348811
// modified for our needs
  module
    .value('$anchorScroll', angular.noop)
    .run(function($state, $stateParams, $window, $rootScope) {
      function scroll() {
        var state, offset, to;

        state = $state.$current;
        // Default behaviour is to scroll to top
        // Any string that isn't [top,none] is treated as a jq selector
        if (!state.scrollTo || state.scrollTo === 'none' || state.scrollTo === 'top') {
          $window.scrollTo(0, 0);
        } else {
          offset = jQuery(state.scrollTo).offset();
          if (offset) {
            to = offset.top - 40;
            jQuery('body,html').animate({ scrollTop: to }, 800);
          }
        }

      }

      $rootScope.$on('$viewContentLoaded', scroll);
      $rootScope.$on('$stateChangeSuccess', scroll);
    });

  module.config(function ($locationProvider, $stateProvider, $urlRouterProvider,
                          AngularyticsProvider, $httpProvider, RestangularProvider,
                          markdownConverterProvider, localStorageServiceProvider) {

    // Use in production or when UI hosted by API
    RestangularProvider.setBaseUrl('/api/v1');
    // Use to connect to production API regardless of setup
    // RestangularProvider.setBaseUrl('https://dcc.icgc.org/api/v1');
    // Use to connect to local API when running UI using JS dev server
    // RestangularProvider.setBaseUrl('http://localhost:8080/api/v1');
    // RestangularProvider.setBaseUrl('https://localhost:55555/api/v1');

    RestangularProvider.setDefaultHttpFields({cache: true});

    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';

    $locationProvider.html5Mode(true);

    AngularyticsProvider.setEventHandlers(['Google']);

    $stateProvider.state(
      'team', {
        url: '/team',
        templateUrl: '/views/static/team.html',
        controller: ['Page', function (Page) {
          Page.setTitle('The Team');
          Page.setPage('entity');
        }]
      });

    // All else redirect to home
    $urlRouterProvider.otherwise(function($injector, $location) {
      $injector.invoke(['Notify', function(Notify) {
        Notify.setMessage('Cannot find: ' + $location.url());
        Notify.showErrors();
      }]);
    });

    markdownConverterProvider.config({
      extensions: ['table']
    });

    localStorageServiceProvider.setPrefix('icgc');
  });

  module.run(function ($http, $state, $timeout, $interval, Restangular, Angularytics, Compatibility, Notify) {
    Restangular.setErrorInterceptor(function (response) {
        console.error('Response Error: ', response);

        if (response.status >= 500) {
          Notify.setMessage('' + response.data.message);
          Notify.showErrors();
        } else if (response.status === 404) {
          if (response.data.message) {
            Notify.setMessage(response.data.message);
            Notify.showErrors();
          }
        } else if (response.status === 400) {
          if (response.data.message) {
            Notify.setMessage('' + response.data.message);
          }
          Notify.showErrors();
        }
      }
    );

    Angularytics.init();
    // Browser compatibility tests
    Compatibility.run();
  });


  module.constant('Extensions', {
    GENE_LIST: 'uploadedGeneList',
    GENE_ID: 'id',
    CURATE_SET_ROOTS: [
      {id: 'GS1', name: 'Cancer Gene Census', universe: null}
    ],
    GENE_ONTOLOGY_ROOTS: [
      {id: 'GO:0003674', name: 'Molecular Function', universe: 'GO_MOLECULAR_FUNCTION'},
      {id: 'GO:0008150', name: 'Biological Process', universe: 'GO_BIOLOGICAL_PROCESS'},
      {id: 'GO:0005575', name: 'Cellular Component', universe: 'GO_CELLULAR_COMPONENT'}
    ],
    PATHWAY_ROOTS: [
      {id: null, name: 'Reactome Pathways', universe: 'REACTOME_PATHWAYS'}
    ]
  });


  module.constant('DataTypes', {
    'mapping': {
      'clinical': 'Clinical data',
      'ssm': 'Simple somatic mutations',
      'sgv': 'Simple Germline Variation',
      'cnsm': 'Copy number somatic mutations',
      'stsm': 'Structural somatic mutations',
      'exp_array': 'Array-based Gene Expression',
      'exp_seq': 'Sequencing-based Gene Expression',
      'pexp': 'Protein expression',
      'mirna_seq': 'Sequencing-based miRNA Expression',
      'jcn': 'Exon junctions',
      'meth_array': 'Array-based DNA Methylation',
      'meth_seq': 'Sequencing-based DNA Methylation'
    },
    'order': [
      'clinical',
      'ssm',
      'sgv',
      'cnsm',
      'stsm',
      'exp_seq',
      'exp_array',
      'pexp',
      'mirna_seq',
      'jcn',
      'meth_array',
      'meth_seq'
    ]
  });


  module.controller('AppCtrl', function ($scope, Page) {
    var _ctrl = this;
    _ctrl.appLoaded = true;
    _ctrl.Page = Page;

    // for document level clicks
    _ctrl.handleApplicationClick = function () {
      $scope.$broadcast('application:click');
    };
  });
})();
