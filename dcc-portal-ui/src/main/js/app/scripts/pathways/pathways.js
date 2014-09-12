/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU Pathwayral Public License along with this
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

  var module = angular.module('icgc.pathways', ['icgc.pathways.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('pathway', {
      url: '/pathways/:id',
      templateUrl: 'scripts/pathways/views/pathway.html',
      controller: 'PathwayCtrl as PathwayCtrl',
      resolve: {
        pathway: ['$stateParams', 'Pathways', function ($stateParams, Pathways) {
          return Pathways.one($stateParams.id).get({include: 'projects'}).then(function (pathway) {
            pathway.projects = _.map(pathway.projects, function (p) {
              p.uiAffectedDonorPercentage = p.affectedDonorCount / p.ssmTestedDonorCount;
              return p;
            });
            return pathway;
          });
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.pathways.controllers', ['icgc.pathways.models']);

  module.controller('PathwayCtrl',
    function ($scope, LocationService, HighchartsService, Page, Genes, Projects, Mutations, pathway) {
      var _ctrl = this, f = {gene: {pathwayId: {is: [pathway.id]}}};
      Page.setTitle(pathway.id);
      Page.setPage('entity');

      _ctrl.pathway = pathway;




      function refresh() {
        var _filter = LocationService.mergeIntoFilters(f);
        _ctrl.baseAdvQuery = _filter;

        Mutations.handler.one('count').get({filters: _filter}).then(function (count) {
          _ctrl.totalMutations = count;
        });

        Genes.handler.one('count').get({filters:_filter}).then(function (count) {
          _ctrl.totalGenes = count;
        });


        if (_ctrl.pathway.hasOwnProperty('projects') && _ctrl.pathway.projects.length) {
          _ctrl.pathway.projectIds = _.pluck(_ctrl.pathway.projects, 'id');
          Projects.one(_ctrl.pathway.projectIds.join(',')).handler.one('mutations',
            'counts').get({filters: _filter}).then(function (data) {
              _ctrl.pathway.projects.forEach(function (p) {
                p.mutationCount = data[p.id];
                p.advQuery = LocationService.mergeIntoFilters({
                  gene: {pathwayId:{is:[_ctrl.pathway.id]}},
                  donor: {projectId:{is:[p.id]}, availableDataTypes:{is:['ssm']}}
                });
              });
            });

          Projects.one(_ctrl.pathway.projectIds.join(',')).handler.one('donors',
            'counts').get({filters: _filter}).then(function (data) {
              _ctrl.totalDonors = 0;
              _ctrl.pathway.projects.forEach(function (p) {
                p.affectedDonorCount = data[p.id];
                _ctrl.totalDonors += p.affectedDonorCount;
                p.uiAffectedDonorPercentage = p.affectedDonorCount / p.ssmTestedDonorCount;
              });

              _ctrl.donorBar = HighchartsService.bar({
                hits: _.first(_.sortBy(_ctrl.pathway.projects, function (p) {
                  return -p.uiAffectedDonorPercentage;
                }), 10),
                xAxis: 'id',
                yValue: 'uiAffectedDonorPercentage'
              });

              _ctrl.pathway.fprojects = _.filter(_ctrl.pathway.projects, function (p) {
                return p.affectedDonorCount > 0;
              });
            });

          Projects.one(_ctrl.pathway.projectIds).handler.one('genes',
            'counts').get({filters: _filter}).then(function (data) {

              _ctrl.pathway.projects.forEach(function (p) {
                p.geneCount = data[p.id];
                p.uiAffectedGenePercentage = p.geneCount / _ctrl.pathway.geneCount;
              });

              _ctrl.geneBar = HighchartsService.bar({
                hits: _.first(_.sortBy(_ctrl.pathway.projects, function (p) {
                  return -p.uiAffectedGenePercentage;
                }),10),
                xAxis: 'id',
                yValue: 'uiAffectedGenePercentage'
              });

            });
        }

        var params = {
          filters: _filter,
          size: 0,
          include: ['facets']
        };
        Mutations.getList(params).then(function (d) {
          _ctrl.mutationFacets = d.facets;
        });
      }

      $scope.$on('$locationChangeSuccess', function (event, dest) {
        if (dest.indexOf('pathways') !== -1) {
          refresh();
        }
      });

      refresh();
    });

  module.controller('PathwayGenesCtrl', function ($scope, LocationService, Genes, Pathways) {
    var _ctrl = this, _pathway = '', _filter = {};

    function success(genes) {
      if (genes.hasOwnProperty('hits')) {
        _ctrl.genes = genes;

        Genes.one(_.pluck(_ctrl.genes.hits, 'id').join(',')).handler.one('mutations',
          'counts').get({filters: _filter}).then(function (data) {
            _ctrl.genes.hits.forEach(function (g) {
              g.mutationCount = data[g.id];
              g.advQuery = LocationService.mergeIntoFilters({
                gene: {pathwayId: {is: [_pathway.id]}, id:{is:[g.id]}}
              });
            });
          });
      }
    }

    function refresh() {
      Pathways.one().get({include: 'projects'}).then(function (pathway) {
        _pathway = pathway;
        _filter = LocationService.mergeIntoFilters({gene: {pathwayId: {is: pathway.id}}});
        Genes.getList({
          filters: _filter
        }).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('pathways') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('PathwayMutationsCtrl', function ($scope, Mutations, Pathways, Projects, LocationService, Donors) {
    var _ctrl = this, pathway;

    function success(mutations) {
      if (mutations.hasOwnProperty('hits')) {
        _ctrl.mutations = mutations;

        // Need to get SSM Test Donor counts from projects
        Projects.getList().then(function (projects) {
          _ctrl.mutations.hits.forEach(function (mutation) {
            Donors.getList({
              size: 0,
              include: 'facets',
              filters: LocationService.mergeIntoFilters({
                mutation: {id: {is: mutation.id}},
                gene: {pathwayId: {is: pathway.id}}
              })
            }).then(function (data) {
              mutation.uiDonors = data.facets.projectId.terms;
              mutation.advQuery = LocationService.mergeIntoFilters({
                mutation: {id: {is: [mutation.id] }},
                gene: {pathwayId: {is: [pathway.id] }}
              });

              if (mutation.uiDonors) {
                mutation.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  facet.advQuery = LocationService.mergeIntoFilters({
                    mutation: {id: {is: [mutation.id]}},
                    donor: {projectId: {is: [facet.term]}},
                    gene: {pathwayId: {is: [pathway.id] }}
                  });

                  facet.countTotal = p.ssmTestedDonorCount;
                  facet.percentage = facet.count / p.ssmTestedDonorCount;
                });
              }
            });
          });
        });
      }
    }

    function refresh() {
      Pathways.one().get({include: 'projects'}).then(function (p) {
        pathway = p;

        Mutations.getList({
          include: 'consequences',
          filters: LocationService.mergeIntoFilters({
            gene: {pathwayId: {is: pathway.id}}
          })
        }).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('pathways') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('PathwayDonorsCtrl', function ($scope, LocationService, Donors, Pathways) {
    var _ctrl = this, _pathway, _filter;

    function success(donors) {
      if (donors.hasOwnProperty('hits')) {
        _ctrl.donors = donors;

        Donors.one(_.pluck(_ctrl.donors.hits, 'id').join(',')).handler.one('mutations', 'counts').get({
          filters: _filter
        }).then(function (data) {
          _ctrl.donors.hits.forEach(function (d) {
            d.mutationCount = data[d.id];
            d.advQuery = LocationService.mergeIntoFilters({
              gene: {pathwayId: {is: [_pathway.id]}},
              donor: {id:{is:[d.id]}}
            });
          });
        });
      }
    }

    function refresh() {
      Pathways.one().get({include: 'projects'}).then(function (pathway) {
        _pathway = pathway;
        _filter = LocationService.mergeIntoFilters({gene: {pathwayId: {is: pathway.id}}});
        Donors.getList({filters: _filter}).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('pathways') !== -1) {
        refresh();
      }
    });

    refresh();
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.pathways.models', []);

  module.service('Pathways', function (Restangular, LocationService, Pathway) {
    this.handler = Restangular.all('pathways');

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: LocationService.filters()
      };

      return this.handler.get('', angular.extend(defaults, params));
    };

    this.one = function (id) {
      return id ? Pathway.init(id) : Pathway;
    };
  });

  module.service('Pathway', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('pathways', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };
  });
})();
