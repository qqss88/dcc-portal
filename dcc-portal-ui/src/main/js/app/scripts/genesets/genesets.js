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

  var module = angular.module('icgc.genesets', ['icgc.genesets.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('geneset', {
      url: '/genesets/:id',
      templateUrl: 'scripts/genesets/views/geneset.html',
      controller: 'GeneSetCtrl as GeneSetCtrl',
      resolve: {
        geneSet: ['$stateParams', 'GeneSets', function ($stateParams, GeneSets) {
          return GeneSets.one($stateParams.id).get({include: 'projects'}).then(function (geneSet) {
            geneSet.projects = _.map(geneSet.projects, function (p) {
              p.uiAffectedDonorPercentage = p.affectedDonorCount / p.ssmTestedDonorCount;
              return p;
            });
            return geneSet;
          });
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.genesets.controllers', ['icgc.genesets.models']);

  module.controller('GeneSetCtrl',
    function ($scope, LocationService, HighchartsService, Page, Genes, Projects, Mutations, geneSet) {
      var _ctrl = this, f = {gene: {geneSetId: {is: [geneSet.id]}}};
      Page.setTitle(geneSet.id);
      Page.setPage('entity');

      _ctrl.geneSet = geneSet;

      // Builds an UI friendly inferred tree
      // A -> [B, C] -> D -> [E, F, G]
      function uiInferredTree(inferredTree) {
        var root = {}, node = root, current = null;

        if (! angular.isDefined(inferredTree) || _.isEmpty(inferredTree) ) {
          return {};
        }

        current = inferredTree[0].level;

        node.goTerms = [];
        inferredTree.forEach(function(goTerm) {
          if (goTerm.level !== current) {
            current = goTerm.level;
            node.child = {};
            node.child .goTerms = [];
            node = node.child;
          }
          node.goTerms.push({
            name: goTerm.name,
            id: goTerm.id,
            relation: goTerm.relation,
            level: parseInt(goTerm.level, 10)
          });
        });
        return root;
      }


      // Builds an UI friendly list of parent pathway hierarchies
      // [ [A->B], [C-D->E->F] ]
      function uiPathwayHierarchy(parentPathways) {
        var hierarchyList = [];
        if (! angular.isDefined(parentPathways) || _.isEmpty(parentPathways) ) {
          return hierarchyList;
        }

        parentPathways.forEach(function(path) {
          var root = {}, node = root;

          // Add all ancestors
          path.forEach(function(n, idx) {
            node.id = n.id;
            node.name = n.name;
            
            // Has children, swap
            if (idx < path.length) {
              node.children = [];
              node.children.push({});
              node = node.children[0];
            }
          });

          // Lastly, add self
          node.id = _ctrl.geneSet.id;
          node.name = _ctrl.geneSet.name;

          hierarchyList.push(root);
        });
        return hierarchyList;
      }


      // Builds the project-donor distribution based on thie gene set
      // 1) Create embedded search queries
      // 2) Project-donor breakdown
      // 3) Project-gene breakdwon
      function refresh() {
        var _filter = LocationService.mergeIntoFilters(f);
        _ctrl.baseAdvQuery = _filter;


        _ctrl.uiParentPathways = uiPathwayHierarchy(geneSet.hierarchy);
        _ctrl.uiInferredTree = uiInferredTree(geneSet.inferredTree);

        Mutations.handler.one('count').get({filters: _filter}).then(function (count) {
          _ctrl.totalMutations = count;
        });

        Genes.handler.one('count').get({filters:_filter}).then(function (count) {
          _ctrl.totalGenes = count;
        });


        if (_ctrl.geneSet.hasOwnProperty('projects') && _ctrl.geneSet.projects.length) {
          _ctrl.geneSet.projectIds = _.pluck(_ctrl.geneSet.projects, 'id');
          Projects.one(_ctrl.geneSet.projectIds.join(',')).handler.one('mutations',
            'counts').get({filters: _filter}).then(function (data) {
              _ctrl.geneSet.projects.forEach(function (p) {
                p.mutationCount = data[p.id];
                p.advQuery = LocationService.mergeIntoFilters({
                  gene: {geneSetId:{is:[_ctrl.geneSet.id]}},
                  donor: {projectId:{is:[p.id]}, availableDataTypes:{is:['ssm']}}
                });
              });
            });

          Projects.one(_ctrl.geneSet.projectIds.join(',')).handler.one('donors',
            'counts').get({filters: _filter}).then(function (data) {
              _ctrl.totalDonors = 0;
              _ctrl.geneSet.projects.forEach(function (p) {
                p.affectedDonorCount = data[p.id];
                _ctrl.totalDonors += p.affectedDonorCount;
                p.uiAffectedDonorPercentage = p.affectedDonorCount / p.ssmTestedDonorCount;
              });

              _ctrl.donorBar = HighchartsService.bar({
                hits: _.first(_.sortBy(_ctrl.geneSet.projects, function (p) {
                  return -p.uiAffectedDonorPercentage;
                }), 10),
                xAxis: 'id',
                yValue: 'uiAffectedDonorPercentage'
              });

              _ctrl.geneSet.fprojects = _.filter(_ctrl.geneSet.projects, function (p) {
                return p.affectedDonorCount > 0;
              });
            });

          Projects.one(_ctrl.geneSet.projectIds).handler.one('genes',
            'counts').get({filters: _filter}).then(function (data) {

              _ctrl.geneSet.projects.forEach(function (p) {
                p.geneCount = data[p.id];
                p.uiAffectedGenePercentage = p.geneCount / _ctrl.geneSet.geneCount;
              });

              _ctrl.geneBar = HighchartsService.bar({
                hits: _.first(_.sortBy(_ctrl.geneSet.projects, function (p) {
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
        if (dest.indexOf('genesets') !== -1) {
          refresh();
        }
      });

      refresh();
    });

  module.controller('GeneSetGenesCtrl', function ($scope, LocationService, Genes, GeneSets) {
    var _ctrl = this, _geneSet = '', _filter = {};

    function success(genes) {
      if (genes.hasOwnProperty('hits') && genes.hits.length > 0) {
        _ctrl.genes = genes;
        Genes.one(_.pluck(_ctrl.genes.hits, 'id').join(',')).handler.one('mutations',
          'counts').get({filters: _filter}).then(function (data) {
            _ctrl.genes.hits.forEach(function (g) {
              g.mutationCount = data[g.id];
              g.advQuery = LocationService.mergeIntoFilters({
                gene: {geneSetId: {is: [_geneSet.id]}, id:{is:[g.id]}}
              });
            });
          });
      }
    }

    function refresh() {
      GeneSets.one().get({include: 'projects'}).then(function (geneSet) {
        _geneSet = geneSet;
        _filter = LocationService.mergeIntoFilters({gene: {geneSetId: {is: geneSet.id}}});
        Genes.getList({
          filters: _filter
        }).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('genesets') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('GeneSetMutationsCtrl', function ($scope, Mutations, GeneSets, Projects, LocationService, Donors) {
    var _ctrl = this, geneSet;

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
                gene: {geneSetId: {is: geneSet.id}}
              })
            }).then(function (data) {
              mutation.uiDonors = data.facets.projectId.terms;
              mutation.advQuery = LocationService.mergeIntoFilters({
                mutation: {id: {is: [mutation.id] }},
                gene: {geneSetId: {is: [geneSet.id] }}
              });

              if (mutation.uiDonors) {
                mutation.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  facet.advQuery = LocationService.mergeIntoFilters({
                    mutation: {id: {is: [mutation.id]}},
                    donor: {projectId: {is: [facet.term]}},
                    gene: {geneSetId: {is: [geneSet.id] }}
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
      GeneSets.one().get({include: 'projects'}).then(function (p) {
        geneSet = p;

        Mutations.getList({
          include: 'consequences',
          filters: LocationService.mergeIntoFilters({
            gene: {geneSetId: {is: geneSet.id}}
          })
        }).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('genesets') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('GeneSetDonorsCtrl', function ($scope, LocationService, Donors, GeneSets) {
    var _ctrl = this, _geneSet, _filter;

    function success(donors) {
      if (donors.hasOwnProperty('hits')) {
        _ctrl.donors = donors;

        Donors.one(_.pluck(_ctrl.donors.hits, 'id').join(',')).handler.one('mutations', 'counts').get({
          filters: _filter
        }).then(function (data) {
          _ctrl.donors.hits.forEach(function (d) {
            d.mutationCount = data[d.id];
            d.advQuery = LocationService.mergeIntoFilters({
              gene: {geneSetId: {is: [_geneSet.id]}},
              donor: {id:{is:[d.id]}}
            });
          });
        });
      }
    }

    function refresh() {
      GeneSets.one().get({include: 'projects'}).then(function (geneSet) {
        _geneSet = geneSet;
        _filter = LocationService.mergeIntoFilters({gene: {geneSetId: {is: geneSet.id}}});
        Donors.getList({filters: _filter}).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('genesets') !== -1) {
        refresh();
      }
    });

    refresh();
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.genesets.models', []);

  module.service('GeneSets', function (Restangular, LocationService, GeneSet) {
    this.handler = Restangular.all('genesets');

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: LocationService.filters()
      };

      return this.handler.get('', angular.extend(defaults, params));
    };

    this.one = function (id) {
      return id ? GeneSet.init(id) : GeneSet;
    };
  });

  module.service('GeneSet', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('genesets', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };
  });
})();
