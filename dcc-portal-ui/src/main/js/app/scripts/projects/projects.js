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

  var module = angular.module('icgc.projects', ['icgc.projects.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('projects', {
      url: '/projects?filters',
      reloadOnSearch: false,
      templateUrl: 'scripts/projects/views/projects.html',
      controller: 'ProjectsCtrl as ProjectsCtrl',
      data: {tab: 'summary'}
    });

    $stateProvider.state('projects.details', {
      url: '/details',
      reloadOnSearch: false,
      data: {tab:'details'}
    });

    $stateProvider.state('projects.summary', {
      url: '/summary',
      reloadOnSearch: false,
      data: {tab:'summary'}
    });

    $stateProvider.state('projects.history', {
      url: '/history',
      reloadOnSearch: false,
      data: {tab:'history'}
    });

    $stateProvider.state('project', {
      url: '/projects/:id',
      templateUrl: 'scripts/projects/views/project.html',
      controller: 'ProjectCtrl as ProjectCtrl',
      resolve: {
        project: ['$stateParams', 'Projects', function ($stateParams, Projects) {
          return Projects.one($stateParams.id).get();
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.projects.controllers', ['icgc.projects.models']);

  module.controller('ProjectsCtrl',
    function ($q, $scope, $state, ProjectState, Page, Projects,
               HighchartsService, Donors, Restangular, LocationService) {

    var _ctrl = this;
    Page.setTitle('Cancer Projects');
    Page.setPage('projects');

    _ctrl.Page = Page;
    _ctrl.state = ProjectState;

    _ctrl.setTab = function (tab) {
        _ctrl.state.setTab(tab);
      };
    _ctrl.setTab($state.current.data.tab);


    $scope.$watch(function () {
        return $state.current.data.tab;
      }, function () {
        _ctrl.setTab($state.current.data.tab);
      });

    // This is needed to translate projects filters to donor filters
    $scope.createAdvanceFilters = function(dataType) {
      var currentFilters = LocationService.filters();
      var filters = {};

      if (dataType || !_.isEmpty(currentFilters)) {
        filters.donor = {};
      }

      if (dataType) {
        filters.donor.availableDataTypes = {};
        filters.donor.availableDataTypes.is = [dataType];
      }
      if (!_.isEmpty(currentFilters)) {
        filters.donor.projectId = {};
        filters.donor.projectId.is = _ctrl.projectIds;
      }

      return JSON.stringify(filters);
    };


    function success(data) {
      if (data.hasOwnProperty('hits')) {
        var totalDonors = 0, ssmTotalDonors = 0;

        _ctrl.projects = data;
        _ctrl.projectIds = _.pluck(data.hits, 'id');

        data.hits.forEach(function (p) {
          totalDonors += p.totalDonorCount;
          ssmTotalDonors += p.ssmTestedDonorCount;
        });

        _ctrl.totalDonors = totalDonors;
        _ctrl.ssmTotalDonors = ssmTotalDonors;

        _ctrl.donut = HighchartsService.donut({
          data: data.hits,
          type: 'project',
          innerFacet: 'primarySite',
          outerFacet: 'id',
          countBy: 'totalDonorCount'
        });

        _ctrl.stacked = [];


        // Get project-donor-mutation distribution of exon impacted ssm
        Restangular.one('ui', '').one('projects/donor-mutation-counts', '').get({}).then(function(data) {
          // Remove restangular attributes to make data easier to parse
          data = Restangular.stripRestangular(data);
          _ctrl.distribution = data;
        });

        Projects.several(_.pluck(data.hits, 'id').join(',')).get('genes',{
            include: 'projects',
            filters: {mutation:{functionalImpact:{is:['High']}}},
            size: 20
          }).then(function (genes) {
            if ( !genes.hits || genes.hits.length === 0) {
              Page.stopWork();
              return;
            }

            var params = {
              mutation: {functionalImpact:{is:['High']}}
            };
            Page.stopWork();

            // FIXME: elasticsearch aggregation support may be more efficient
            Restangular.one('ui').one('gene-project-donor-counts', _.pluck(genes.hits, 'id'))
              .get({'filters': params}).then(function(geneProjectFacets) {

              genes.hits.forEach(function(gene) {
                var uiFIProjects = [];

                geneProjectFacets[gene.id].terms.forEach(function(t) {
                  var proj = _.findWhere( data.hits, function(p) {
                    return p.id === t.term;
                  });

                  if (angular.isDefined(proj)) {
                    uiFIProjects.push({
                      id: t.term,
                      name: proj.name,
                      count: t.count
                    });
                  }
                });

                gene.uiFIProjects = uiFIProjects;
              });

              _ctrl.stacked = genes.hits;
            });
          });

        Restangular.one('projects/history', '').get({}).then(function(data) {
          // Remove restangular attributes to make data easier to parse
          data = Restangular.stripRestangular(data);
          _ctrl.donorData = data;
        });
      }
    }

    function refresh() {
      Page.startWork();
      Projects.getList({include: 'facets'}).then(success);
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {

      function hasPath(p) {
        return dest.indexOf(p) >= 0;
      }

      // Sub tabs
      if (hasPath('/projects/details') || hasPath('/projects/summary') || hasPath('/projects/history') ) {
        refresh();
        return;
      }

      // Main
      if (dest.indexOf('projects') !== -1 && dest.indexOf('projects/') === -1) {
        refresh();
        return;
      }

    });

    refresh();
  });

  module.controller('ProjectCtrl', function ($scope, $window, Page, PubMed, project, Mutations, API, ExternalLinks) {
    var _ctrl = this;
    Page.setTitle(project.id);
    Page.setPage('entity');

    _ctrl.hasExp = !_.isEmpty(project.experimentalAnalysisPerformedSampleCounts);

    _ctrl.project = project;
    _ctrl.ExternalLinks = ExternalLinks;


    if (!_ctrl.project.hasOwnProperty('uiPublicationList')) {
      _ctrl.project.uiPublicationList = [];
    }

    function success(data) {
      _ctrl.project.uiPublicationList.push(data);
    }

    if (_ctrl.project.hasOwnProperty('pubmedIds')) {
      _ctrl.project.pubmedIds.forEach(function (pmid) {
        PubMed.get(pmid).then(success);
      });
    }

    _ctrl.downloadSample = function () {
      $window.location.href = API.BASE_URL + '/projects/' + project.id + '/samples';
    };

    function refresh() {
      var params = {
        filters: {donor: {projectId: {is: [project.id]}}},
        size: 0,
        include: ['facets']
      };
      Mutations.getList(params).then(function (d) {
        _ctrl.mutationFacets = d.facets;
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('projects') !== -1) {
        refresh();
      }
    });

    refresh();

  });

  module.controller('ProjectGeneCtrl', function ($scope, HighchartsService, Projects, Donors, LocationService) {
    var _ctrl = this;

    function success(genes) {
      if (genes.hasOwnProperty('hits') ) {
        var geneIds = _.pluck(genes.hits, 'id').join(',');
        _ctrl.genes = genes;

        if (_.isEmpty(_ctrl.genes.hits)) {
          return;
        }

        Projects.one().get().then(function (data) {
          var project = data;
          genes.advQuery = LocationService.mergeIntoFilters({donor: {projectId: {is: [project.id]}}});

          // Get Mutations counts
          Projects.one().handler
            .one('genes', geneIds)
            .one('mutations', 'counts').get({
              filters: LocationService.filters()
            }).then(function (data) {
              _ctrl.mutationCounts = data;
            });

          // Need to get SSM Test Donor counts from projects
          Projects.getList().then(function (projects) {
            _ctrl.genes.hits.forEach(function (gene) {
              gene.uiAffectedDonorPercentage = gene.affectedDonorCountFiltered / project.ssmTestedDonorCount;

              gene.advQuery =
              LocationService.mergeIntoFilters({donor: {projectId: {is: [project.id]}}, gene: {id: {is: [gene.id]}}});

              gene.advQueryAll = LocationService.mergeIntoFilters({gene: {id: {is: [gene.id]}}});

              Donors.getList({size: 0, include: 'facets', filters: gene.advQueryAll}).then(function (data) {
                gene.uiDonors = data.facets.projectId.terms;
                gene.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  facet.advQuery = LocationService.mergeIntoFilters({
                      donor: {projectId: {is: [facet.term]}},
                      gene: {id: {is: [gene.id]}}
                    }
                  );

                  facet.countTotal = p.ssmTestedDonorCount;
                  facet.percentage = facet.count / p.ssmTestedDonorCount;
                });
              });
            });

            _ctrl.bar = HighchartsService.bar({
              hits: _ctrl.genes.hits,
              xAxis: 'symbol',
              yValue: 'uiAffectedDonorPercentage'
            });
          });
        });
      }
    }

    function refresh() {
      Projects.one().getGenes({filters: LocationService.filters()}).then(success);
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('projects') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('ProjectMutationsCtrl', function ($scope, HighchartsService, Projects, Donors, LocationService) {
    var _ctrl = this, project = Projects.one();


    function success(mutations) {
      if (mutations.hasOwnProperty('hits')) {
        _ctrl.mutations = mutations;

        if ( _.isEmpty(_ctrl.mutations.hits)) {
          return;
        }

        mutations.advQuery = LocationService.mergeIntoFilters({donor: {projectId: {is: [project.id]}}});

        // Need to get SSM Test Donor counts from projects
        Projects.getList().then(function (projects) {
          _ctrl.mutations.hits.forEach(function (mutation) {

            mutation.advQuery = LocationService.mergeIntoFilters({
              donor: {projectId: {is: [project.id]}},
              mutation: {id: {is: [mutation.id]}}
            });

            mutation.advQueryAll = LocationService.mergeIntoFilters({mutation: {id: {is: [mutation.id]}}});

            Donors.getList({
              size: 0,
              include: 'facets',
              filters: mutation.advQueryAll
              //filters: {mutation: {id: {is: mutation.id}}}
            }).then(function (data) {
              mutation.uiDonors = data.facets.projectId.terms;
              mutation.uiDonors.forEach(function (facet) {
                var p = _.find(projects.hits, function (item) {
                  return item.id === facet.term;
                });

                facet.advQuery = LocationService.mergeIntoFilters({
                  donor: {projectId: {is: [facet.term]}},
                  mutation: {id: {is: [mutation.id]}}
                });

                facet.countTotal = p.ssmTestedDonorCount;
                facet.percentage = facet.count / p.ssmTestedDonorCount;
              });
            });
          });
        });

        _ctrl.bar = HighchartsService.bar({
          hits: _ctrl.mutations.hits,
          xAxis: 'id',
          yValue: 'affectedDonorCountFiltered'
        });
      }
    }

    function refresh() {
      project.getMutations({include: 'consequences', filters: LocationService.filters()}).then(success);
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('projects') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('ProjectDonorsCtrl', function ($scope, HighchartsService, Projects, Donors, LocationService) {
    var _ctrl = this, project = Projects.one();

    function success(donors) {
      if (donors.hasOwnProperty('hits')) {
        _ctrl.donors = donors;
        _ctrl.donors.advQuery = LocationService.mergeIntoFilters({donor: {projectId: {is: [project.id]}}});

        _ctrl.donors.hits.forEach(function (donor) {
          donor.advQuery = LocationService.mergeIntoFilters({donor: {id: {is: [donor.id]}}});
        });
        Donors
          .one(_.pluck(donors.hits, 'id').join(',')).handler.all('mutations')
          .one('counts').get({filters: LocationService.filters()}).then(function (data) {
            _ctrl.mutationCounts = data;
          });

        _ctrl.bar = HighchartsService.bar({
          hits: _ctrl.donors.hits,
          xAxis: 'id',
          yValue: 'ssmAffectedGenes'
        });
      }
    }

    function refresh() {
      Projects.one().getDonors({ filters: LocationService.filters()}).then(success);
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('projects') !== -1) {
        refresh();
      }
    });

    refresh();
  });

})();

(function () {
  'use strict';

  var module = angular.module('icgc.projects.models', ['restangular', 'icgc.common.location']);

  module.service('Projects', function (Restangular, LocationService, Project) {
    this.all = function () {
      return Restangular.all('projects');
    };

    this.several = function(list) {
      return Restangular.several('projects', list);
    };

    this.getList = function (params) {
      var defaults = {
        size: 100,
        from: 1,
        filters: LocationService.filters()
      };

      return this.all().get('', angular.extend(defaults, params)).then(function (data) {

        if (data.hasOwnProperty('facets') &&
            data.facets.hasOwnProperty('id') &&
            data.facets.id.hasOwnProperty('terms')) {
          data.facets.id.terms = data.facets.id.terms.sort(function (a, b) {
            if (a.term < b.term) {
              return -1;
            }
            if (a.term > b.term) {
              return 1;
            }
            return 0;
          });
        }

        return data;
      });
    };

    this.one = function (id) {
      return id ? Project.init(id) : Project;
    };
  });

  module.service('Project', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.id = id;
      this.handler = Restangular.one('projects', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };

    this.getGenes = function (params) {
      var defaults = {
        size: 10,
        from: 1
      };
      return this.handler.one('genes', '').get(angular.extend(defaults, params));
    };

    this.getDonors = function (params) {
      var defaults = {
        size: 10,
        from: 1
      };
      return this.handler.one('donors', '').get(angular.extend(defaults, params));
    };

    this.getMutations = function (params) {
      var defaults = {
        size: 10,
        from: 1
      };
      return this.handler.one('mutations', '').get(angular.extend(defaults, params));
    };

  });

  module.value('X2JS', new X2JS());

  module.service('PubMed', function (Restangular, X2JS) {
    this.handler = Restangular.oneUrl('pubmed', 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi');

    function format(xml) {
      var pub = {}, json = X2JS.xml_str2json(xml).eSummaryResult.DocSum;

      function get(field) {
        return _.findWhere(json.Item, function (o) {
          return o._Name === field;
        }).__text;
      }

      pub.id = json.Id;
      pub.title = get('Title');
      pub.journal = get('FullJournalName');
      pub.issue = get('Issue');
      pub.pubdate = get('PubDate');
      pub.authors = _.pluck(_.findWhere(json.Item, function (o) {
        return o._Name === 'AuthorList';
      }).Item, '__text');
      pub.refCount = parseInt(get('PmcRefCount'), 10);

      return pub;
    }

    this.get = function (id) {
      return this.handler.get({db: 'pubmed', id: id}).then(function (data) {
        return format(data);
      });
    };
  });

  module.service('ProjectState', function () {

    this.visitedTab = {};

    this.hasVisitedTab = function(tab) {
      return this.visitedTab[tab];
    };

    this.setTab = function (tab) {
      this.tab = tab;
      this.visitedTab[tab] = true;
    };
    this.getTab = function () {
      return this.tab;
    };
    this.isTab = function (tab) {
      return this.tab === tab;
    };

  });

})();
