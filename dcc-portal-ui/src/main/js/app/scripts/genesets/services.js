(function () {
  'use strict';

  var module = angular.module('icgc.genesets.services', []);

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

  module.service('GeneSetService', function(Donors, Mutations, Genes, Projects, Restangular) {

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

        if (_.isEmpty(ids)) {
          return [];
        }

        return Projects.getList({
          filters: {'project': {'id': { 'is': ids}}}
        });
      });
    };


    ////////////////////////////////////////////////////////////////////////////////
    // Wrapper functions to make controller cleaner
    ////////////////////////////////////////////////////////////////////////////////
    this.getProjectMutations = function(ids, filters) {
      return Projects.one(ids).handler.one('mutations', 'counts').get({filters: filters});
    };

    this.getProjectDonors = function(ids, filters) {
      return Projects.one(ids).handler.one('donors', 'counts').get({filters: filters});
    };

    this.getProjectGenes = function(ids, filters) {
      return Projects.one(ids).handler.one('genes', 'counts').get({filters: filters});
    };

    this.getGeneCounts = function(f) {
      return Genes.handler.one('count').get({filters: f});
    };

    this.getMutationCounts = function(f) {
      return Mutations.handler.one('count').get({filters: f});
    };

    this.getMutationImpactFacet = function(f) {
      var params = {
        filters: f,
        size: 0,
        include: ['facets']
      };
      return Mutations.getList(params);
    };


    ////////////////////////////////////////////////////////////////////////////////
    // Reactome pathway only
    ////////////////////////////////////////////////////////////////////////////////
    this.getPathwayXML = function(pathwayId) {
      return Restangular.one('ui')
        .one('reactome').one('pathway-diagram')
        .get({'pathwayId' : pathwayId},{'Accept':'application/xml'});
    };

    this.getPathwayZoom = function(pathwayId) {
      return Restangular.one('ui')
        .one('reactome').one('pathway-sub-diagram')
        .get({'pathwayId' : pathwayId}, {'Accept':'application/json'});
    };

    this.getPathwayProteinMap = function(pathwayId, mutationImpacts) {
      return Restangular.one('ui')
        .one('reactome').one('protein-map')
        .get({
          pathwayId: pathwayId,
          impactFilter: _.isEmpty(mutationImpacts)? '': mutationImpacts.join(',')
        });
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
