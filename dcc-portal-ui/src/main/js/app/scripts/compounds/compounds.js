/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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

'use strict';

////////////////////////////////////////////////////////////////////////
// Primary Compound Module
////////////////////////////////////////////////////////////////////////
angular.module('icgc.compounds', ['icgc.compounds.controllers', 'icgc.compounds.services'])
  .config(function ($stateProvider) {
    $stateProvider.state('compound', {
      url: '/compound/:compoundId',
      templateUrl: 'scripts/compounds/views/compound.html',
      controller: 'CompoundCtrl as CompoundCtrl',
      resolve: {
        compoundManager: ['Page', '$stateParams', 'CompoundsService', function (Page, $stateParams, CompoundsService) {
          Page.startWork();
          Page.setTitle('Compounds');
          Page.setPage('entity');
          return CompoundsService.getCompoundManagerFactory($stateParams.compoundId);
        }]
      }
    });
  });

angular.module('icgc.compounds.controllers', ['icgc.compounds.services'])
  .controller('CompoundCtrl', function (compoundManager, CompoundsService, Page) {

    var _ctrl = this,
        _compound = compoundManager.getCompound(),
        _targetedCompoundGenes = null,
        _targetedCompoundGenesResultPerPage = 10,
        _targetCompoundResultPage = 0,
        _targetedCompoundIds = [];



    function _init() {


      Page.stopWork();

      compoundManager.getTargetedCompoundGenes(_targetCompoundResultPage, _targetCompoundResultPage)
        .then(function(targetGenes) {
          _targetedCompoundGenes = targetGenes;
          _targetedCompoundIds = compoundManager.getTargetedCompoundGeneIds();
          console.log(targetGenes);
        });
    }

    _init();


    //////////////////////////////////////////////////////////////////////
    // Controller API
    //////////////////////////////////////////////////////////////////////

    _ctrl.getTargetedCompoundGenesResultPerPage = function() {
      return Math.min(_targetedCompoundIds.length, _targetedCompoundGenesResultPerPage);
    };

    _ctrl.getPrettyExternalRefName = function(refName) {
      var referenceName = refName.toLowerCase();

      switch(referenceName) {
        case 'chembl':
          referenceName = 'ChEMBL';
          break;
        case 'drugbank':
          referenceName = 'Drugbank';
          break;
        default:
          break;
      }

      return referenceName;
    };

    _ctrl.getTargetedCompoundGenes = function() {
      return _targetedCompoundGenes;
    };

    _ctrl.getTargetedGeneCount = function() {
      return _compound.genes.length;
    };

    _ctrl.getFilter = function(filterType) {
      var filter = {};

      switch (filterType.toLowerCase()) {
        default:
          filter.gene = {
            id: {
              is: _compound.genes
            }
          };
          break;
      }

      return filter;
    };

    _ctrl.getAffectedDonorCountTotal = function() {
      return compoundManager.getAffectedDonorCountTotal();
    };

    _ctrl.getCompound = function() {
      return _compound;
    };

    _ctrl.goToModule = function(type, limit) {
      var params = {},
          setType = 'gene',
          url = null;

      params.filters = _ctrl.getFilter(type);
      params.size = limit || _ctrl.getTargetedGeneCount();
      params.isTransient = true;


      switch(type.toLowerCase()) {
        case 'genomeviewer':
          url = '/browser/g';
          break;
        case 'advancedsearch':
          url = '/search/g';
          break;
        default:
        break;
      }

      if (! url) {
        return;
      }

      CompoundsService.goToModule(setType, params, url);

    };

  });

angular.module('icgc.compounds.services', ['icgc.genes.models'])
  .service('CompoundsService', function($q, Gene, Page, $location, Restangular) {

    function _arrayOrEmptyArray(arr) {
      return angular.isArray(arr) ?  arr : [];
    }

    function _compoundEntityFactory(compound) {
      var _id = compound.zincId,
          _inchikey = compound.inchikey,
          _name = compound.name,
          _synonyms = _arrayOrEmptyArray(compound.synonyms),
          _externalReferences = compound.externalReferences,
          _imageURL = compound.imageUrl || null,
          _drugClass = compound.drugClass || '--',
          _cancerTrialCount = compound.cancerTrialCount || '--',
          _atcCodes = _arrayOrEmptyArray(compound.atcCodes),
          _genes = _.pluck(_arrayOrEmptyArray(compound.genes), 'ensemblGeneId'),
          _trials = _arrayOrEmptyArray(compound.trials);


      return {
        id: _id,
        inchiKey: _inchikey,
        name: _name,
        synonyms: _synonyms,
        externalReferences: _externalReferences,
        imageURL: _imageURL,
        drugClass: _drugClass,
        cancerTrialCount: _cancerTrialCount,
        atcCodes: _atcCodes,
        genes: _genes,
        trials: _trials
      };
    }

    function _geneEntityFactory(geneData) {

      var _id = geneData.id,
          _type = geneData.type,
          _symbol = geneData.symbol,
          _name = geneData.name,
          _chromosome = geneData.chromosome,
          _start = geneData.start,
          _end = geneData.end,
          _affectedDonorCountFiltered = geneData.affectedDonorCountFiltered,
          _affectedDonorCountTotal = geneData.affectedDonorCountTotal;


      return {
        id: _id,
        type: _type,
        symbol: _symbol,
        name: _name,
        chromosome: _chromosome,
        start: _start,
        end: _end,
        affectedDonorCountFiltered: _affectedDonorCountFiltered,
        affectedDonorCountTotal: _affectedDonorCountTotal
      };

    }

    var _srv = this;






    function CompoundManager(compoundId) {
      var _self = this,
          _compoundEntity = null,
          _compoundTargetedGenes = [],
          _compoundTargetedGeneIds = [],
          _affectedDonorCountTotal = 0;

      function _getCompoundGenesFilter(geneStartIndex, geneLimit) {
        var geneStartSliceIndex = geneStartIndex || 0,
            geneEndIndex = geneStartIndex + (geneLimit || 10),
            geneIDRequestSliceLength =  Math.min(_compoundEntity.genes.length, geneEndIndex);

       return  {
         from: 1,
         size: (geneLimit || 10),
         filters: {
           gene: {
             id: {
               is: _.slice(_compoundTargetedGeneIds, geneStartSliceIndex, geneIDRequestSliceLength)
             }
           }
         }
       };
      }

      _self.getCompoundDonors = function(geneStartIndex, geneLimit) {
        var params = _getCompoundGenesFilter(geneStartIndex, geneLimit);

        return Restangular
          .one('donors')
          .get(params)
          .then(function(restangularDonorsList) {
            _affectedDonorCountTotal = _.get(restangularDonorsList, 'pagination.total', 0);
          });
      };

      _self.getCompoundMutations = function(geneStartIndex, geneLimit) {
        var params = _getCompoundGenesFilter(geneStartIndex, geneLimit);
        delete params.from;
        delete params.filters;


        return Restangular
            .one('drugs')
            .one(compoundId)
            .one('genes')
            .one('mutations')
            .one('counts')
            .get(params);
      };

      _self.getTargetedCompoundGeneIds = function() {
        return _compoundTargetedGeneIds;
      };

      _self.getTargetedCompoundGenes = function(geneStartIndex, geneLimit) {

        var deferred = $q.defer();

        _self.getCompoundMutations()
          .then(function(restangularMutationCountData) {
            var mutationCountData = restangularMutationCountData.plain(),
                geneCount = mutationCountData.length,
                mutationGeneValueMap = {};

            if (geneCount === 0) {
              deferred.resolve(_compoundTargetedGenes);
              return deferred.promise;
            }

            for (var i = 0; i < geneCount; i++) {
              var mutationData  = mutationCountData[i],
                  geneId = _.get(mutationData, 'key', false);

              if (geneId) {
                _compoundTargetedGeneIds.push( geneId );
                mutationGeneValueMap[geneId] = +mutationData.value;
              }
            }

            var params = _getCompoundGenesFilter(geneStartIndex, geneLimit);

            Restangular
              .one('genes')
              .get(params)
              .then(function(geneList) {
                var geneListResults = _.get(geneList, 'hits', false);

                if (! geneListResults) {
                  deferred.resolve(_compoundTargetedGenes);
                }

                var geneListResultsLength = geneListResults.length;

                for (var i = 0; i < geneListResultsLength; i++) {
                  var gene = _geneEntityFactory( geneListResults[i] );
                  gene.mutationCountTotal = mutationGeneValueMap[gene.id];

                  _compoundTargetedGenes.push( gene );
                }

                _compoundTargetedGenes = _.sortByOrder(_compoundTargetedGenes, 'affectedDonorCountFiltered', false);

                deferred.resolve(_compoundTargetedGenes);

                _self.getCompoundDonors(geneStartIndex, geneLimit);

              });


          });

        return deferred.promise;
      };

      _self.init = function() {
          var defer = $q.defer(),
              deferPromise = defer.promise;

          Restangular
            .one('drugs', compoundId)
            .get()
            .then(function(compound) {
              _compoundEntity = _compoundEntityFactory(compound.plain());
              defer.resolve(_self);
            });

        return deferPromise;
      };

      _self.getTargetedGenes = function() {
        return _compoundTargetedGenes;
      };

      _self.getCompound = function() {
        return _compoundEntity;
      };

      _self.getAffectedDonorCountTotal = function() {
        return _affectedDonorCountTotal;
      };

    }

    // For application/json format
    function _params2JSON(type, params) {
      var data = {};
      data.filters = encodeURI(JSON.stringify(params.filters));
      data.type = type.toUpperCase();
      data.name = params.name;
      data.description = params.description || '';
      data.size = params.size || 0;

      if (params.isTransient) {
        data.isTransient = params.isTransient;
      }

      // Set default sort values if necessary
      if (angular.isDefined(params.filters) && !angular.isDefined(params.sortBy)) {
        if (type === 'donor') {
          data.sortBy = 'ssmAffectedGenes';
        } else if (type === 'gene') {
          data.sortBy = 'affectedDonorCountFiltered';
        } else {
          data.sortBy = 'affectedDonorCountFiltered';
        }
        data.sortOrder = 'DESCENDING';
      } else {
        data.sortBy = params.sortBy;
        data.sortOrder = params.sortOrder;
      }
      data.union = params.union;
      return data;
    }

    _srv.goToModule = function(type, params, forwardUrl) {
        Page.startWork();
        params.name = 'Input gene set';
        params.description = '';
        params.sortBy = 'affectedDonorCountFiltered';
        params.sortOrder = 'DESCENDING';

        var data = _params2JSON(type, params),
            promise = Restangular.one('entityset')
                        .customPOST(data, undefined, {async:'false'}, {'Content-Type': 'application/json'});

        promise.then(function(data) {
          Page.stopWork();
          if (! data.id) {
            console.warn('there is no id!!!!');
            return;
          }
          else {
            var newFilter = JSON.stringify({'gene': {entitySetId: {is: [data.id]}}});
            $location.path(forwardUrl).search('filters', newFilter);
          }
        });

        return promise;
    };

    _srv.getCompoundManagerFactory = function(id) {
      var _compoundManager = new CompoundManager(id);
      return _compoundManager.init();
    };

  });