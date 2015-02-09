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

  var module = angular.module('icgc.advanced', ['icgc.advanced.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('advanced', {
      url: '/search?filters',
      data: {
        tab: 'donor'
      },
      reloadOnSearch: false,
      templateUrl: '/scripts/advanced/views/advanced.html',
      controller: 'AdvancedCtrl as AdvancedCtrl'
    });
    $stateProvider.state('advanced.gene', {
      url: '/g',
      reloadOnSearch: false,
      data: {tab: 'gene'}
    });
    $stateProvider.state('advanced.mutation', {
      url: '/m',
      reloadOnSearch: false,
      data: {tab: 'mutation', subTab: 'mutation'}
    });
    $stateProvider.state('advanced.mutation.occurrence', {
      url: '/o',
      reloadOnSearch: false,
      data: {subTab: 'occurrence'}
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.advanced.controllers', ['icgc.advanced.services']);

  module.controller('AdvancedCtrl',
    function ($scope, $state, Page, State, LocationService, AdvancedDonorService, AdvancedGeneService,
              AdvancedMutationService) {
      Page.setTitle('Advanced Search');
      Page.setPage('advanced');

      var _ctrl = this;

      _ctrl.Page = Page;
      _ctrl.state = State;

      _ctrl.Donor = AdvancedDonorService;
      _ctrl.Gene = AdvancedGeneService;
      _ctrl.Mutation = AdvancedMutationService;
      _ctrl.Location = LocationService;

      function refresh() {
        _ctrl.Donor.refresh();
        _ctrl.Gene.refresh();
        _ctrl.Mutation.refresh();
        _ctrl.hasGeneFilter = LocationService.filters().hasOwnProperty('gene');
      }

      function ajax() {
        _ctrl.Donor.ajax();
        _ctrl.Gene.ajax();
        _ctrl.Mutation.ajax();
      }

      _ctrl.saveSet = function(type, limit) {
        _ctrl.setLimit = limit;
        _ctrl.setType = type;
        _ctrl.setModal = true;
      };

      _ctrl.setTab = function (tab) {
        _ctrl.state.setTab(tab);
        ajax();
      };

      _ctrl.setSubTab = function (tab) {
        _ctrl.state.setSubTab(tab);
      };

      // Setup
      _ctrl.setTab($state.current.data.tab);
      _ctrl.setSubTab($state.current.data.subTab);
      refresh();

      // Refresh when filters change
      // Data is cached so refreshing on tab switch
      // should be free
      $scope.$on('$locationChangeSuccess', function (event, next) {
        if (next.indexOf('search') !== -1) {
          refresh();
        }
      });

      // Tabs need to update when using browser buttons
      // Shouldn't have to worry about refreshing data here
      // since you cannot change tabs and filters in one movement
      // ... actually you can by clicking on counts in the tables
      // $scope.$on('$locationChangeSuccess') should take care of that anyway
      $scope.$watch(function () {
        return $state.current.data.tab;
      }, function () {
        _ctrl.setTab($state.current.data.tab);
      });
      $scope.$watch(function () {
        return $state.current.data.subTab;
      }, function () {
        _ctrl.setSubTab($state.current.data.subTab);
      });
    });

  module.service('AdvancedDonorService',
    function(Page, LocationService, HighchartsService, Donors, State, Extensions) {

    var _this = this;

    _this.ajax = function () {
      if (State.isTab('donor') && _this.donors && _this.donors.hits && _this.donors.hits.length) {
        _this.mutationCounts = null;
        Donors
          .one(_.pluck(_this.donors.hits, 'id').join(','))
          .handler
          .one('mutations', 'counts').get({filters: LocationService.filters()}).then(function (counts) {
            _this.mutationCounts = counts;
          });

        var facets = _this.donors.facets;
        _this.pieProjectId = HighchartsService.pie({
          type: 'donor',
          facet: 'projectId',
          facets: facets
        });
        _this.piePrimarySite = HighchartsService.pie({
          type: 'donor',
          facet: 'primarySite',
          facets: facets
        });
        _this.pieGender = HighchartsService.pie({
          type: 'donor',
          facet: 'gender',
          facets: facets
        });
        _this.pieTumourStage = HighchartsService.pie({
          type: 'donor',
          facet: 'tumourStageAtDiagnosis',
          facets: facets
        });
        _this.pieVitalStatus = HighchartsService.pie({
          type: 'donor',
          facet: 'vitalStatus',
          facets: facets
        });
        _this.pieStatusFollowup = HighchartsService.pie({
          type: 'donor',
          facet: 'diseaseStatusLastFollowup',
          facets: facets
        });
        _this.pieRelapseType = HighchartsService.pie({
          type: 'donor',
          facet: 'relapseType',
          facets: facets
        });
        _this.pieAge = HighchartsService.pie({
          type: 'donor',
          facet: 'ageAtDiagnosisGroup',
          facets: facets
        });
        _this.pieDataTypes = HighchartsService.pie({
          type: 'donor',
          facet: 'availableDataTypes',
          facets: facets
        });
        _this.pieAnalysisTypes = HighchartsService.pie({
          type: 'donor',
          facet: 'analysisTypes',
          facets: facets
        });
      }
    };

    _this.success = function (donors) {
      var filters = LocationService.filters();
      Page.stopWork();
      _this.loading = false;

      donors.hits.forEach(function (donor) {
        donor.embedQuery = LocationService.merge(filters, {donor: {id: {is: [donor.id]}}}, 'facet');

        // Remove gene entity set because gene id is the key
        if (donor.embedQuery.hasOwnProperty('donor')) {
          var donorFilters = donor.embedQuery.donor;
          delete donorFilters[Extensions.ENTITY];
        }

      });
      _this.donors = donors;
      _this.ajax();
    };

    _this.refresh = function () {
      Page.startWork();
      _this.loading = true;
      var params = LocationService.getJsonParam('donors');
      params.include = 'facets';
      Donors.getList(params).then(_this.success);
    };
  });

  module.service('AdvancedGeneService',
    function(Page, LocationService, Genes, Projects, Donors, State, FiltersUtil, Extensions) {

    var _this = this;

    _this.ajax = function () {
      if (State.isTab('gene') && _this.genes && _this.genes.hits && _this.genes.hits.length) {
        _this.mutationCounts = null;
        var geneIds = _.pluck(_this.genes.hits, 'id').join(',');
        var uniqueGeneFilter = FiltersUtil.removeExtensions(LocationService.filters());

        // Get Mutations counts
        Genes.one(geneIds).handler
          .one('mutations', 'counts').get({filters: LocationService.filters()}).then(function (data) {
            _this.mutationCounts = data;
          });


        // Need to get SSM Test Donor counts from projects
        Projects.getList().then(function (projects) {
          _this.genes.hits.forEach(function (gene) {
            Donors.getList({
              size: 0,
              include: 'facets',
              filters: LocationService.merge(uniqueGeneFilter, {gene: {id: {is: [gene.id]}}}, 'facet')
            }).then(function (data) {
              gene.uiDonors = [];
              if (data.facets.projectId.terms) {
                var _f = FiltersUtil.removeExtensions(LocationService.filters());
                if (_f.hasOwnProperty('donor')) {
                  delete _f.donor.projectId;
                  if (_.isEmpty(_f.donor)) {
                    delete _f.donor;
                  }
                }

                gene.uiDonorsLink = LocationService.merge(_f, {gene: {id: {is: [gene.id]}}}, 'facet');
                gene.uiDonors = data.facets.projectId.terms;
                gene.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  facet.countTotal = p.ssmTestedDonorCount;
                  facet.percentage = facet.count / p.ssmTestedDonorCount;
                });

                // This is just used for gene CSV export, it is unwieldly to do it in the view
                gene.uiDonorsExportString = gene.uiDonors.map(function(d) {
                  return d.term + ':' + d.count + '/' +  d.countTotal;
                }).join('|');
              }
            });
          });
        });
      }
    };

    _this.success = function (genes) {
      Page.stopWork();
      _this.loading = false;

      genes.hits.forEach(function (gene) {
        var uniqueGeneFilter = FiltersUtil.removeExtensions(LocationService.filters());
        gene.embedQuery = LocationService.merge(uniqueGeneFilter, {gene: {id: {is: [gene.id]}}}, 'facet');

        // Remove gene entity set because gene id is the key
        if (gene.embedQuery.hasOwnProperty('gene')) {
          var geneFilters = gene.embedQuery.gene;
          delete geneFilters[Extensions.ENTITY];
        }

      });
      _this.genes = genes;
      _this.ajax();
    };

    _this.refresh = function () {
      Page.startWork();
      _this.loading = true;
      var params = LocationService.getJsonParam('genes');
      params.include = 'facets';
      Genes.getList(params).then(_this.success);
    };
  });

  module.service('AdvancedMutationService',
    function (Page, LocationService, HighchartsService, Mutations, Occurrences, Projects, Donors, State, Extensions) {
      var _this = this;

      _this.ajax = function () {
        if (State.isTab('mutation') && _this.mutations && _this.mutations.hits && _this.mutations.hits.length) {
          // Need to get SSM Test Donor counts from projects
          Projects.getList().then(function (projects) {
            _this.mutations.hits.forEach(function (mutation) {
              Donors.getList({
                size: 0,
                include: 'facets',
                filters: LocationService.overwriteFilters({mutation: {id: {is: mutation.id}}}, 'facet')
              }).then(function (data) {
                mutation.uiDonors = [];
                if (data.facets.projectId.terms) {
                  var _f = LocationService.filters();
                  if (_f.hasOwnProperty('donor')) {
                    delete _f.donor.projectId;
                    if (_.isEmpty(_f.donor)) {
                      delete _f.donor;
                    }
                  }
                  mutation.uiDonorsLink = LocationService.merge(_f, {mutation: {id: {is: [mutation.id]}}}, 'facet');
                  mutation.uiDonors = data.facets.projectId.terms;
                  mutation.uiDonors.forEach(function (facet) {
                    var p = _.find(projects.hits, function (item) {
                      return item.id === facet.term;
                    });

                    facet.countTotal = p.ssmTestedDonorCount;
                    facet.percentage = facet.count / p.ssmTestedDonorCount;
                  });

                  // This is just used for mutation CSV export, it is unwieldly to do it in the view
                  mutation.uiDonorsExportString = mutation.uiDonors.map(function(d) {
                    return d.term + ':' + d.count + '/' + d.countTotal;
                  }).join('|');
                }
              });
            });
          });

          var facets = _this.mutations.facets;
          _this.pieConsequences = HighchartsService.pie({
            type: 'mutation',
            facet: 'consequenceType',
            facets: facets
          });
          _this.piePlatform = HighchartsService.pie({
            type: 'mutation',
            facet: 'platform',
            facets: facets
          });
          _this.pieVerificationStatus = HighchartsService.pie({
            type: 'mutation',
            facet: 'verificationStatus',
            facets: facets
          });
          _this.pieType = HighchartsService.pie({
            type: 'mutation',
            facet: 'type',
            facets: facets
          });
        }
      };

      _this.mSuccess = function (mutations) {
        Page.stopWork();
        _this.loading = false;

        mutations.hits.forEach(function (mutation) {
          var filters = LocationService.filters();
          mutation.embedQuery = LocationService.merge(filters, {mutation: {id: {is: [mutation.id]}}}, 'facet');

          // Remove gene entity set because gene id is the key
          if (mutation.embedQuery.hasOwnProperty('mutation')) {
            var mutationFilters = mutation.embedQuery.mutation;
            delete mutationFilters[Extensions.ENTITY];
          }

        });
        _this.mutations = mutations;
        _this.ajax();
      };

      _this.oSuccess = function (occurrences) {
        _this.occurrences = occurrences;
      };

      _this.refresh = function () {
        Page.startWork();
        _this.loading = true;
        var mParams = LocationService.getJsonParam('mutations');
        mParams.include = ['facets', 'consequences'];
        Mutations.getList(mParams).then(_this.mSuccess);
        Occurrences.getList(LocationService.getJsonParam('occurrences')).then(_this.oSuccess);
      };
    });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.advanced.services', []);

  module.service('State', function () {
    this.loading = false;
    this.visitedTab = {};
    this.visitedFacet = {};

    this.setTab = function (tab) {
      this.tab = tab;
      this.facetTab = tab;
      if (tab === 'mutation') {
        this.subTab = tab;
      }
      this.visitedTab[tab] = true;
      this.visitedFacet[tab] = true;
    };
    this.getTab = function () {
      return this.tab;
    };
    this.isTab = function (tab) {
      return this.tab === tab;
    };

    this.hasVisitedTab = function(tab) {
      return this.visitedTab[tab];
    };
    this.hasVisitedFacet = function(tab) {
      return this.visitedFacet[tab];
    };

    this.setFacetTab = function (tab) {
      this.facetTab = tab;
      this.visitedFacet[tab] = true;
    };
    this.getFacetTab = function () {
      return this.facetTab;
    };
    this.isFacetTab = function (tab) {
      return this.facetTab === tab;
    };

    this.setSubTab = function (tab) {
      this.subTab = tab;
    };
    this.getSubTab = function () {
      return this.subTab;
    };
    this.isSubTab = function (tab) {
      return this.subTab === tab;
    };
  });
})();
