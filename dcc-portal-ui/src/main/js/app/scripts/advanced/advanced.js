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

  var module = angular.module('icgc.advanced.controllers', ['icgc.advanced.services', 'icgc.sets.services']);

  module.controller('AdvancedCtrl',
    function ($scope, $state, $modal, Page, State, LocationService, AdvancedDonorService,
              AdvancedGeneService, AdvancedMutationService, SetService, CodeTable, Settings, RouteInfoService) {
      Page.setTitle('Advanced Search');
      Page.setPage('advanced');

      var _ctrl = this;
      var dataRepoRouteInfo = RouteInfoService.get ('dataRepositories');
      var dataRepoUrl = dataRepoRouteInfo.href;

      _ctrl.Page = Page;
      _ctrl.state = State;

      _ctrl.dataRepoTitle = dataRepoRouteInfo.title;
      _ctrl.Donor = AdvancedDonorService;
      _ctrl.Gene = AdvancedGeneService;
      _ctrl.Mutation = AdvancedMutationService;
      _ctrl.Location = LocationService;

      function refresh() {
        var filters = LocationService.filters();
        _ctrl.Donor.refresh();
        _ctrl.Gene.refresh();
        _ctrl.Mutation.refresh();
        _ctrl.hasGeneFilter = angular.isObject(filters) ?  filters.hasOwnProperty('gene') : false;
      }

      function ajax() {
        _ctrl.Donor.ajax();
        _ctrl.Gene.ajax();
        _ctrl.Mutation.ajax();
      }

      _ctrl.downloadDonorData = function() {
        $modal.open({
          templateUrl: '/scripts/downloader/views/request.html',
          controller: 'DownloadRequestController',
          resolve: {
            filters: function() { return undefined; }
          }
        });
      };

      _ctrl.saveSet = function(type, limit) {
        _ctrl.setLimit = limit;
        _ctrl.setType = type;

        $modal.open({
          templateUrl: '/scripts/sets/views/sets.upload.html',
          controller: 'SetUploadController',
          resolve: {
            setType: function() {
              return _ctrl.setType;
            },
            setLimit: function() {
              return _ctrl.setLimit;
            },
            setUnion: function() {
              return undefined;
            }
          }
        });
      };

      _ctrl.viewExternal = function(type, limit) {
        var params = {};
        params.filters = LocationService.filters();
        params.size = limit;
        params.isTransient = true;
        // Ensure scope is destroyed as there may be unreferenced watchers on the filter. (see: facets/tags.js)
        $scope.$destroy();
        SetService.createForwardSet (type, params, dataRepoUrl);
      };

      /**
       * Create new enrichment analysis
       */
      _ctrl.enrichmentAnalysis = function(limit) {
        $modal.open({
          templateUrl: '/scripts/enrichment/views/enrichment.upload.html',
          controller: 'EnrichmentUploadController',
          resolve: {
            geneLimit: function() {
              return limit;
            },
            filters: function() {
              return undefined;
            }
          }
        });
      };

      function ensureString (string) {
        return _.isString (string) ? string.trim() : '';
      }

      _ctrl.projectFlagIconClass = function (projectCode) {
        var defaultValue = '';
        var last3 = _.takeRight (ensureString (projectCode), 3);

        if (_.size (last3) < 3 || _.first (last3) !== '-') {
          return defaultValue;
        }

        var last2 = _.rest (last3).join ('');

        return 'flag flag-' + CodeTable.translateCountryCode (last2.toLowerCase());
      };

      /**
       * View observation/experimental details
       */
      _ctrl.viewObservationDetail = function(observation) {
        $modal.open({
          templateUrl: '/scripts/advanced/views/advanced.observation.popup.html',
          controller: 'ObservationDetailController',
          resolve: {
            observation: function() {
              return observation;
            }
          }
        });
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

      Settings.get().then(function(settings) {
        _ctrl.downloadEnabled = settings.downloadEnabled || false;
      });
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


  /**
   * Container to observation popup
   */
  module.controller('ObservationDetailController', function($scope, $modalInstance, observation) {
    $scope.observation = observation;
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
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

        // Remove donor entity set because donor id is the key
        if (donor.embedQuery.hasOwnProperty('donor')) {
          var donorFilters = donor.embedQuery.donor;
          delete donorFilters[Extensions.ENTITY];
        }

        // Proper encode
        donor.embedQuery = encodeURIComponent(JSON.stringify(donor.embedQuery));

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
    function(Page, LocationService, Genes, Projects, Donors, State, FiltersUtil, Extensions, ProjectCache) {

    var _this = this;

    _this.projectGeneQuery = function(projectId, geneId) {
      var f = LocationService.filters();
      if (f.hasOwnProperty('gene')) {
        delete f.gene.id;
        delete f.gene[Extensions.ENTITY];
      }
      if (f.hasOwnProperty('donor')) {
        delete f.donor.projectId;
      }

      if (f.hasOwnProperty('gene') === false) {
        f.gene = {};
      }
      if (f.hasOwnProperty('donor') === false) {
        f.donor = {};
      }
      f.gene.id = { is: [geneId] };
      f.donor.projectId = { is: [projectId] };

      return encodeURIComponent(JSON.stringify(f));
    };


    _this.ajax = function () {
      if (State.isTab('gene') && _this.genes && _this.genes.hits && _this.genes.hits.length) {
        _this.mutationCounts = null;
        var geneIds = _.pluck(_this.genes.hits, 'id').join(',');
        var projectCachePromise = ProjectCache.getData();


        // Get Mutations counts
        Genes.one(geneIds).handler
          .one('mutations', 'counts').get({filters: LocationService.filters()}).then(function (data) {
            _this.mutationCounts = data;
          });


        // Need to get SSM Test Donor counts from projects
        Projects.getList().then(function (projects) {
          _this.genes.hits.forEach(function (gene) {

            var geneFilter = LocationService.filters();
            if (geneFilter.hasOwnProperty('gene')) {
              delete geneFilter.gene[ Extensions.ENTITY ];
              delete geneFilter.gene.id;
              geneFilter.gene.id = {
                is: [gene.id]
              };
            } else {
              geneFilter.gene = {
                id: {
                  is: [gene.id]
                }
              };
            }

            Donors.getList({
              size: 0,
              include: 'facets',
              filters: geneFilter
            }).then(function (data) {
              gene.uiDonors = [];
              if (data.facets.projectId.terms) {

                var _f = LocationService.filters();
                if (_f.hasOwnProperty('donor')) {
                  delete _f.donor.projectId;
                  if (_.isEmpty(_f.donor)) {
                    delete _f.donor;
                  }
                }
                if (_f.hasOwnProperty('gene')) {
                  delete _f.gene[ Extensions.ENTITY ];
                  if (_.isEmpty(_f.gene)) {
                    delete _f.gene;
                  }
                }


                gene.uiDonorsLink = LocationService.toURLParam(
                                      LocationService.merge(_f, {gene: {id: {is: [gene.id]}}}, 'facet')
                                    );

                gene.uiDonors = data.facets.projectId.terms;
                gene.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  projectCachePromise.then(function(lookup) {
                    facet.projectName = lookup[facet.term] || facet.term;
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
        var filters = LocationService.filters();
        gene.embedQuery = LocationService.merge(filters, {gene: {id: {is: [gene.id]}}}, 'facet');

        // Remove gene entity set because gene id is the key
        if (gene.embedQuery.hasOwnProperty('gene')) {
          var geneFilters = gene.embedQuery.gene;
          delete geneFilters[Extensions.ENTITY];
        }

        // Proper encode
        gene.embedQuery = encodeURIComponent(JSON.stringify(gene.embedQuery));

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

  module.service('AdvancedMutationService', function (Page, LocationService, HighchartsService, Mutations,
    Occurrences, Projects, Donors, State, Extensions, ProjectCache) {

      var _this = this;
      var projectCachePromise = ProjectCache.getData();

      _this.projectMutationQuery = function(projectId, mutationId) {
        var f = LocationService.filters();
        if (f.hasOwnProperty('mutation')) {
          delete f.mutation.id;
          delete f.mutation[Extensions.ENTITY];
        }
        if (f.hasOwnProperty('donor')) {
          delete f.donor.projectId;
        }

        if (f.hasOwnProperty('mutation') === false) {
          f.mutation = {};
        }
        if (f.hasOwnProperty('donor') === false) {
          f.donor = {};
        }
        f.mutation.id = { is: [mutationId] };
        f.donor.projectId = { is: [projectId] };
        return encodeURIComponent(JSON.stringify(f));
      };


      _this.ajax = function () {
        if (State.isTab('mutation') && _this.mutations && _this.mutations.hits && _this.mutations.hits.length) {



          // Need to get SSM Test Donor counts from projects
          Projects.getList().then(function (projects) {
            _this.mutations.hits.forEach(function (mutation) {

              var mutationFilter = LocationService.filters();
              if (mutationFilter.hasOwnProperty('mutation')) {
                delete mutationFilter.mutation[ Extensions.ENTITY ];
                delete mutationFilter.mutation.id;
                mutationFilter.mutation.id = {
                  is: [mutation.id]
                };
              } else {
                mutationFilter.mutation = {
                  id: {
                    is: [mutation.id]
                  }
                };
              }

              Donors.getList({
                size: 0,
                include: 'facets',
                filters: mutationFilter
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
                  if (_f.hasOwnProperty('mutation')) {
                    delete _f.mutation[ Extensions.ENTITY ];
                    if (_.isEmpty(_f.mutation)) {
                      delete _f.mutation;
                    }
                  }

                  mutation.uiDonorsLink = LocationService.toURLParam(
                                            LocationService.merge(_f, {mutation: {id: {is: [mutation.id]}}}, 'facet')
                                          );
                  mutation.uiDonors = data.facets.projectId.terms;
                  mutation.uiDonors.forEach(function (facet) {
                    var p = _.find(projects.hits, function (item) {
                      return item.id === facet.term;
                    });

                    projectCachePromise.then(function(lookup) {
                      facet.projectName = lookup[facet.term] || facet.term;
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

          // Remove mutation entity set because mutation id is the key
          if (mutation.embedQuery.hasOwnProperty('mutation')) {
            var mutationFilters = mutation.embedQuery.mutation;
            delete mutationFilters[Extensions.ENTITY];
          }

          // Proper encode
          mutation.embedQuery = encodeURIComponent(JSON.stringify(mutation.embedQuery));

        });
        _this.mutations = mutations;
        _this.ajax();
      };

      _this.oSuccess = function (occurrences) {
        occurrences.hits.forEach(function(occurrence) {
          projectCachePromise.then(function(lookup) {
            occurrence.projectName = lookup[occurrence.projectId] || occurrence.projectId;
          });
        });
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
