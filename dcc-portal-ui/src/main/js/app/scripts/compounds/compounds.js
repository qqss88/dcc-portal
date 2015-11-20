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
        compound: ['$stateParams', 'CompoundsService', function ($stateParams, CompoundsService) {
          return CompoundsService.getCompound($stateParams.compoundId);
        }]
      }
    });
  });

angular.module('icgc.compounds.controllers', ['icgc.compounds.services'])
  .controller('CompoundCtrl', function (compound, CompoundsService, Page) {

    var _ctrl = this,
        _compound = null,
      _targettedCompoundGenes = null;



    function _init() {
      Page.stopWork();
      Page.setTitle('Compounds');
      _compound = compound;
      _targettedCompoundGenes = CompoundsService.getTargettedCompoundGenes(_compound);
    }

    _init();


    //////////////////////////////////////////////////////////////////////
    // Controller API
    //////////////////////////////////////////////////////////////////////

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

    _ctrl.getTargettedCompoundGenes = function() {
      return _targettedCompoundGenes;
    };

    _ctrl.getCompound = function() {
      return _compound;
    };
  });

angular.module('icgc.compounds.services', ['icgc.genes.models'])
  .service('CompoundsService', function($q, Gene, Restangular) {

    function _arrayOrEmptyArray(arr) {
      return angular.isArray(arr) ?  arr : [];
    }

    function CompoundFactory(compound) {
      var _id = compound.zincId,
          _inchikey = compound.inchikey,
          _name = compound.name,
          _synonyms = _arrayOrEmptyArray(compound.synonyms),
          _externalReferences = compound.externalReferences,
          _imageURL = compound.imageUrl || null,
          _drugClass = compound.drugClass || '--',
          _cancerTrialCount = compound.cancerTrialCount || '--',
          _atcCodes = _arrayOrEmptyArray(compound.atcCodes),
          _genes = _arrayOrEmptyArray(compound.genes),
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

    function GeneEntity(geneId) {
      var _self = this,
          _genePromise = null,
          _geneData = null;


      function _init(geneData) {
        _geneData = geneData;
      }


      _self.get = function() {

        if (_genePromise === null) {
          _genePromise = $q.defer();

          Genes.one()
              .get(geneId)
              .then(function(geneData) {
                _init(geneData);
                _genePromise.resolve(_self);
              });

          return _genePromise.promise;
        }

        return _self;
      };

    }

    this.getCompound = function(id) {
      return Restangular
        .one('drugs', id)
        .get()
        .then(function(compound) {
          return CompoundFactory(compound.plain());
        });
    };

    this.getTargettedCompoundGenes = function(compound) {
      var geneCount = compound.genes.length,
          genes = [];

      for (var i = 0; i < geneCount; i++) {
        var geneId = _.get(compound, 'genes[' + i + '].ensemblGeneId', false);

        if (geneId) {
          genes.push( new GeneEntity(geneId) );
        }
      }
      // FIXME: FOR NOW CAP THE LIMIT FOR TESTING
      genes = _.slice(genes, 0, 10);
      return genes;
    };

  });