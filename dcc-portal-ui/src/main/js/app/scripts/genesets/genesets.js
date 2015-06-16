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
          return GeneSets.one($stateParams.id).get().then(function (geneSet) {
            return geneSet;
          });
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.genesets.controllers', ['icgc.genesets.models', 'icgc.genesets.services']);

  module.controller('GeneSetCtrl',
    function ($scope, LocationService, HighchartsService, Page, GeneSetHierarchy, GeneSetService,
      Genes, Mutations, FiltersUtil, ExternalLinks, geneSet, Restangular) {

      var _ctrl = this, geneSetFilter = {gene: {geneSetId: {is: [geneSet.id]}}};
      Page.setTitle(geneSet.id);
      Page.setPage('entity');

      _ctrl.geneSet = geneSet;
      _ctrl.geneSet.queryType = FiltersUtil.getGeneSetQueryType(_ctrl.geneSet.type);

      _ctrl.ExternalLinks = ExternalLinks;

      // Build adv query based on type
      geneSetFilter = {};
      geneSetFilter[_ctrl.geneSet.queryType] = {is:[_ctrl.geneSet.id]};



      // Builds the project-donor distribution based on thie gene set
      // 1) Create embedded search queries
      // 2) Project-donor breakdown
      // 3) Project-gene breakdwon
      function refresh() {
        var _filter = LocationService.mergeIntoFilters({gene:geneSetFilter});
        _ctrl.baseAdvQuery = _filter;

        _ctrl.uiParentPathways = GeneSetHierarchy.uiPathwayHierarchy(geneSet.hierarchy, _ctrl.geneSet);
        _ctrl.uiInferredTree = GeneSetHierarchy.uiInferredTree(geneSet.inferredTree);

        Mutations.handler.one('count').get({filters: _filter}).then(function (count) {
          _ctrl.totalMutations = count;
        });

        Genes.handler.one('count').get({filters:_filter}).then(function (count) {
          _ctrl.totalGenes = count;
        });


        // Find out which projects are affected by this gene set, this data is used to generate cancer distribution
        // 1) Find the impacted projects: genesetId -> {projectIds} -> {projects}
        var geneSetProjectPromise = GeneSetService.getProjects(_filter);


        // 2) Add mutation counts
        geneSetProjectPromise.then(function(projects) {
          var ids, mutationPromise;
          if (! projects.hits || projects.hits.length === 0) {
            return;
          }

          ids = _.pluck(projects.hits, 'id');
          mutationPromise = GeneSetService.getProjectMutations(ids, _filter);

          mutationPromise.then(function(projectMutations) {
            projects.hits.forEach(function(proj) {
              proj.mutationCount = projectMutations[proj.id];
              proj.advQuery = LocationService.mergeIntoFilters({
                gene: geneSetFilter,
                donor: {projectId:{is:[proj.id]}, availableDataTypes:{is:['ssm']}}
              });
            });
          });
        });


        // 3) Add donor counts, gene counts
        geneSetProjectPromise.then(function(projects) {
          var ids, donorPromise, genePromise;
          if (! projects.hits || projects.hits.length === 0) {
            return;
          }

          ids = _.pluck(projects.hits, 'id');

          donorPromise = GeneSetService.getProjectDonors(ids, _filter);
          genePromise = GeneSetService.getProjectGenes(ids, _filter);

          _ctrl.totalDonors = 0;

          donorPromise.then(function(projectDonors) {
            projects.hits.forEach(function(proj) {
              proj.affectedDonorCount = projectDonors[proj.id];
              proj.uiAffectedDonorPercentage = proj.affectedDonorCount / proj.ssmTestedDonorCount;
              _ctrl.totalDonors += proj.affectedDonorCount;
            });

            _ctrl.donorBar = HighchartsService.bar({
              hits: _.take(_.sortBy(projects.hits, function (p) {
                return -p.uiAffectedDonorPercentage;
              }), 10),
              xAxis: 'id',
              yValue: 'uiAffectedDonorPercentage'
            });
          });

          genePromise.then(function(projectGenes) {
            projects.hits.forEach(function(proj) {
              proj.geneCount = projectGenes[proj.id];
              proj.uiAffectedGenePercentage = proj.geneCount / _ctrl.geneSet.geneCount;
            });

            _ctrl.geneBar = HighchartsService.bar({
              hits: _.take(_.sortBy(projects.hits, function (p) {
                return -p.uiAffectedGenePercentage;
              }),10),
              xAxis: 'id',
              yValue: 'uiAffectedGenePercentage'
            });
          });

        });

        // 4) if it's a reactome pathway, get diagram
        _ctrl.geneSet.showPathway = false;

        // FIXME: Disabled until reactome is ready
        if(_ctrl.geneSet.source === 'Reactome' && _ctrl.uiParentPathways[0] && 1 === 2 /* FIXME: Temporary !!! */) {
          _ctrl.pathway = {};
          _ctrl.geneSet.showPathway = true;

          var pathwayId = _ctrl.uiParentPathways[0].diagramId;
          var parentPathwayId = _ctrl.uiParentPathways[0].geneSetId;

          // get pathway xml
          Restangular.one('ui').one('reactome').one('pathway-diagram')
            .get({'pathwayId' : pathwayId},{'Accept':'application/xml'})
            .then(function(data){
              _ctrl.pathway.xml = data;
            });

          // if the diagram itself isnt the one being diagrammed, get list of stuff to zoom in on
          if(pathwayId !== parentPathwayId) {
            Restangular.one('ui').one('reactome').one('pathway-sub-diagram')
              .get({'pathwayId' : parentPathwayId},{'Accept':'application/json'})
              .then(function(data){
                _ctrl.pathway.zooms = data;
              });
          } else {
            _ctrl.pathway.zooms = [''];
          }

          Restangular.one('ui').one('reactome').one('protein-map')
            .get({pathwayId:parentPathwayId,
                  impactFilter:_filter.mutation?_filter.mutation.functionalImpact.is.join(','):''})
            .then(function(map){
              var pathwayHighlights = [];
              _.forEach(map,function(value,id) {
                if(value && value.dbIds) {
                  pathwayHighlights.push({
                    uniprotId:id,
                    dbIds:value.dbIds.split(','),
                    value:value.value
                  });
                }
              });

              // Get ensembl ids for all the genes so we can link to advSearch page
              Restangular.one('genelists').withHttpConfig({transformRequest: angular.identity})
                .customPOST('geneIds='+_.pluck(pathwayHighlights,'uniprotId').join(','),
                            undefined, {'validationOnly':true})
                .then(function(data){
                  _.forEach(pathwayHighlights,function(n){
                    var uniprotObj = data.validGenes['external_db_ids.uniprotkb_swissprot'][n.uniprotId];
                    if(!uniprotObj){
                      return;
                    }
                    var ensemblId = uniprotObj[0].id;
                    n.advQuery =  LocationService.mergeIntoFilters(
                      {
                        gene: {
                          id:  {is: [ensemblId]},
                          pathwayId: {is: [parentPathwayId]}
                        }
                      });
                    n.geneSymbol = uniprotObj[0].symbol;
                    n.geneId = ensemblId;
                  });
                });

              _ctrl.pathway.highlights = pathwayHighlights;
            });
        }

        // Assign projects to controller so it can be rendered in the view
        geneSetProjectPromise.then(function(projects) {
          _ctrl.geneSet.projects = projects.hits;
        });

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

  module.controller('GeneSetGenesCtrl', function ($scope, LocationService, Genes, GeneSets, FiltersUtil) {
    var _ctrl = this, _geneSet = '', _filter = {};

    function success(genes) {
      var geneSetQueryType = FiltersUtil.getGeneSetQueryType(_geneSet.type);

      if (genes.hasOwnProperty('hits')) {
        _ctrl.genes = genes;
        if (_.isEmpty(_ctrl.genes.hits)) {
          return;
        }

        Genes.one(_.pluck(_ctrl.genes.hits, 'id').join(',')).handler.one('mutations',
          'counts').get({filters: _filter}).then(function (data) {
            _ctrl.genes.hits.forEach(function (g) {

              var geneFilter = { id:{is:[g.id]}};
              geneFilter[geneSetQueryType] = {is:[_geneSet.id]};

              g.mutationCount = data[g.id];
              g.advQuery = LocationService.mergeIntoFilters({
                gene: geneFilter
              });
            });
          });
      }
    }

    function refresh() {
      GeneSets.one().get().then(function (geneSet) {
        _geneSet = geneSet;
        _filter = LocationService.mergeIntoFilters({gene: {geneSetId: {is: [geneSet.id]}}});
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

  module.controller('GeneSetMutationsCtrl',
    function ($scope, Mutations, GeneSets, Projects, LocationService, Donors, FiltersUtil, ProjectCache) {

    var _ctrl = this, geneSet;

    function success(mutations) {
      var geneSetQueryType = FiltersUtil.getGeneSetQueryType(geneSet.type);
      var geneFilter = {};
      geneFilter[geneSetQueryType] = {is:[geneSet.id]};

      if (mutations.hasOwnProperty('hits')) {
        var projectCachePromise = ProjectCache.getData();

        _ctrl.mutations = mutations;

        // Need to get SSM Test Donor counts from projects
        Projects.getList().then(function (projects) {
          _ctrl.mutations.hits.forEach(function (mutation) {
            Donors.getList({
              size: 0,
              include: 'facets',
              filters: LocationService.mergeIntoFilters({
                mutation: {id: {is: mutation.id}},
                gene: {geneSetId: {is: [geneSet.id] }}
              })
            }).then(function (data) {

              mutation.uiDonors = data.facets.projectId.terms;
              mutation.advQuery = LocationService.mergeIntoFilters({
                mutation: {id: {is: [mutation.id] }},
                gene: geneFilter
              });

              if (mutation.uiDonors) {
                mutation.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  facet.advQuery = LocationService.mergeIntoFilters({
                    mutation: {id: {is: [mutation.id]}},
                    donor: {projectId: {is: [facet.term]}},
                    gene: geneFilter
                  });

                  projectCachePromise.then(function(lookup) {
                    facet.projectName = lookup[facet.term] || facet.term;
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
      GeneSets.one().get().then(function (p) {
        geneSet = p;

        Mutations.getList({
          include: 'consequences',
          filters: LocationService.mergeIntoFilters({
            gene: {geneSetId: {is: [geneSet.id]}}
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

  module.controller('GeneSetDonorsCtrl', function ($scope, LocationService, Donors, GeneSets, FiltersUtil) {
    var _ctrl = this, _geneSet, _filter;


    function success(donors) {
      var geneSetQueryType = FiltersUtil.getGeneSetQueryType(_geneSet.type);
      var geneFilter = {};
      geneFilter[geneSetQueryType] = {is:[_geneSet.id]};

      if (donors.hasOwnProperty('hits')) {
        _ctrl.donors = donors;

        if (_.isEmpty(_ctrl.donors.hits)) {
          return;
        }

        Donors.one(_.pluck(_ctrl.donors.hits, 'id').join(',')).handler.one('mutations', 'counts').get({
          filters: _filter
        }).then(function (data) {
          _ctrl.donors.hits.forEach(function (d) {
            d.mutationCount = data[d.id];
            d.advQuery = LocationService.mergeIntoFilters({
              gene: geneFilter,
              donor: {id:{is:[d.id]}}
            });
          });
        });
      }
    }

    function refresh() {
      GeneSets.one().get().then(function (geneSet) {
        _geneSet = geneSet;
        _filter = LocationService.mergeIntoFilters({gene: {geneSetId: {is: [geneSet.id]}}});
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

    this.several = function(list) {
      return Restangular.several('genesets', list);
    };

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


(function() {
  'use strict';

  var module = angular.module('icgc.genesets.services', []);

  module.service('GeneSetService', function(Donors, Mutations, Genes, Projects) {


    /**
     * Find out which projects are affected by this gene set, this data is used to generate cancer distribution
     */
    this.getProjects = function(filters) {
      var promise = Donors.getList({
        size: 0,
        from: 1,
        include: ['facets'],
        filters: filters
      });

      return promise.then(function(data) {
        var ids = _.pluck(data.facets.projectId.terms, 'term');
        return Projects.getList({
          filters: {'project': {'id': { 'is': ids}}}
        });
      });
    };

    this.getProjectMutations = function(ids, filters) {
      return Projects.one(ids).handler.one('mutations', 'counts').get({filters: filters});
    };

    this.getProjectDonors = function(ids, filters) {
      return Projects.one(ids).handler.one('donors', 'counts').get({filters: filters});
    };

    this.getProjectGenes = function(ids, filters) {
      return Projects.one(ids).handler.one('genes', 'counts').get({filters: filters});
    };

  });


  /**
   * Generate hierarchical structure for gene-ontology
   * and reactome pathways.
   */
  module.service('GeneSetHierarchy', function() {

    /**
     * Builds an UI friendly inferred tree
     * A -> [B, C] -> D -> [E, F, G]
     */
    function uiInferredTree(inferredTree) {
      var root = {}, node = root, current = null;

      if (! angular.isDefined(inferredTree) || _.isEmpty(inferredTree) ) {
        return {};
      }
      current = inferredTree[0].level;

      node.goTerms = [];
      inferredTree.forEach(function(goTerm) {
        // FIXME: Temporary fix to get around the issue where root level can be 0 and self is also 0
        if ( (goTerm.level !== current || goTerm.relation === 'self') && !_.isEmpty(node.goTerms)) {
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
    function uiPathwayHierarchy(parentPathways, geneSet) {
      var hierarchyList = [];
      if (! angular.isDefined(parentPathways) || _.isEmpty(parentPathways) ) {
        return hierarchyList;
      }

      parentPathways.forEach(function(path) {
        var root = {}, node = root, diagramId = '';

        // Add all ancestors
        var geneSetId = geneSet.id;

        path.forEach(function(n, idx) {
          node.id = n.id;
          node.name = n.name;

          // FIXME: just make it bool in api?
          if (n.diagrammed === 'true') {
            diagramId = node.id;
          }

          // Has children, swap
          if (idx < path.length) {
            node.children = [];
            node.children.push({});
            node = node.children[0];
          }
        });

        // Lastly, add self
        node.id = geneSet.id;
        node.name = geneSet.name;

        if (geneSet.diagrammed === 'true') {
          diagramId = node.id;
        }


        hierarchyList.push({
          'root': root,
          'diagramId': diagramId,
          'geneSetId': geneSetId
        });
      });
      return hierarchyList;
    }


    this.uiInferredTree = uiInferredTree;
    this.uiPathwayHierarchy = uiPathwayHierarchy;
  });


})();
