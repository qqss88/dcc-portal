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

(function() {
  'use strict';

  var module = angular.module('icgc.repository.controllers', ['icgc.repository.services']);

  /**
   * This just controllers overall state
   */
  module.controller('RepositoryController', function($scope, $location, $state, Page) {
    var _ctrl = this;

    Page.setTitle('Data Repository');
    Page.setPage('repository');

    $scope.$watch(function () {
      return $state.current.data.tab;
    }, function () {
      _ctrl.currentTab = $state.current.data.tab || 'icgc';
    });

    console.log('RepoisitoryController init');
  });


  /**
   * ICGC static repository controller
   */
  module.controller('ICGCRepoController',
    function($scope, $stateParams, Restangular, RepositoryService, ProjectCache, API, Settings) {
    console.log('ICGCRepoController', $stateParams);
    var _ctrl = this;

    _ctrl.path = $stateParams.path || '';
    _ctrl.slugs = [];
    _ctrl.API = API;
    _ctrl.deprecatedReleases = ['release_15'];
    _ctrl.downloadEnabled = true;

    function buildBreadcrumbs() {
      var i, s, slug, url;

      url = '';
      s = _ctrl.path.split('/').filter(Boolean); // removes empty cells

      for (i = 0; i < s.length; ++i) {
        slug = s[i];
        url += slug + '/';
        _ctrl.slugs.push({name: slug, url: url});
      }
    }


    /**
     * Additional information for rendering
     */
    function annotate(file) {
      var name, tName, extension;

      // For convienence
      file.baseName = file.name.split('/').pop();

      // Check if there is a translation code for directories (projects)
      if (file.type === 'd') {
        name = (file.name).split('/').pop();

        ProjectCache.getData().then(function(cache) {
          tName = cache[name];
          if (tName) {
            file.translation = tName;
          }
        });
      }

      // Check file extension
      extension = file.name.split('.').pop();
      if (_.contains(['txt', 'md'], extension.toLowerCase())) {
        file.isText = true;
      } else {
        file.isText = false;
      }
    }


    function getFiles() {
      RepositoryService.folder(_ctrl.path).then(function (response) {
        var files = response;

        files.forEach(annotate);

        _ctrl.files = RepositoryService.sortFiles(files, _ctrl.slugs.length);


        // Grab text file (markdown)
        _ctrl.textFiles = _.filter(files, function(f) {
          return f.type === 'f' && f.isText === true;
        });
        _ctrl.textFiles.forEach(function(f) {
          Restangular.one('download').get( {'fn':f.name}).then(function(data) {
            f.textContent = data;
          });
        });

      });
    }

    // Check if download is disabled or not
    function refresh() {
      Settings.get().then(function(settings) {
        if (settings.downloadEnabled && settings.downloadEnabled === true) {
          buildBreadcrumbs();
          getFiles();
          _ctrl.downloadEnabled = true;
        } else {
          _ctrl.downloadEnabled = false;
        }
      });
    }

    // Initialize
    $scope.$watch(function() {
      return $stateParams.path;
    }, function() {
      _ctrl.path = $stateParams.path || '';
      _ctrl.slugs = [];
      refresh();
    });

  });


  module.controller('ExternalFileDownloadController',
    function($scope, $window, $modalInstance, ExternalRepoService, LocationService, params) {

    $scope.filters = LocationService.filters();
    $scope.selectedRepos = params.selectedRepos;
    $scope.selectedFiles = params.selectedFiles;
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    console.log('selected repos', $scope.selectedRepos);


    $scope.download = function() {
      if (_.isEmpty($scope.selectedFiles)) {
        ExternalRepoService.download(LocationService.filters(), $scope.selectedRepos);
      } else {
        ExternalRepoService.downloadSelected($scope.selectedFiles, $scope.selectedRepos);
      }
      $scope.cancel();
    };

  });

  module.controller('ExternalFileInfoController', function (Page, fileInfo) {
    Page.setTitle('External File Entity');
    Page.setPage('externalFileEntity');

    this.fileInfo = fileInfo;
  });

  /**
   * External repository controller
   */
  module.controller('ExternalRepoController',
    function($scope, $window, $modal, LocationService, Page, ExternalRepoService) {

    console.log('ExternalRepoController');
    var _ctrl = this;

    _ctrl.selectedFiles = [];
    _ctrl.selectedRepos = {}; // Used to track file repositories


    // FIXME: Need to convert to PQL
    function resetSelectedRepos() {
      var filterRepos = [], filters = LocationService.filters();
      if (filters.file && filters.file.repositoryNames) {
        filterRepos = filters.file.repositoryNames.is;
      }

      if (! _ctrl.files.termFacets.repositoryNamesFiltered.terms) {
        _ctrl.selectedRepos = {};
        return;
      }

      _ctrl.files.termFacets.repositoryNamesFiltered.terms.forEach(function(term) {
         if (_.contains(filterRepos, term.term) || _.isEmpty(filterRepos)) {
           _ctrl.selectedRepos[term.term] = term.count;
         }
      });
    }


    /**
     * Export table
     */
    _ctrl.export = function() {
      console.log('export');
      ExternalRepoService.export( LocationService.filters() );
    };


    /**
     * Download manifest
     */
    _ctrl.downloadManifest = function() {
      console.log('downloading manifest');

      $modal.open({
        templateUrl: '/scripts/repository/views/repository.external.submit.html',
        controller: 'ExternalFileDownloadController',
        size: 'lg',
        resolve: {
          params: function() {
            return {
              selectedFiles: _ctrl.selectedFiles,
              selectedRepos: _ctrl.selectedRepos,
              filters: LocationService.filters()
            };
          }
        }
      });

    };

    _ctrl.isSelected = function(row) {
      return _.contains(_ctrl.selectedFiles, row.id);
    };


    _ctrl.toggleRow = function(row) {
      var repos = _ctrl.selectedRepos;

      if (_ctrl.isSelected(row) === true) {
        _.remove(_ctrl.selectedFiles, function(r) {
          return r === row.id;
        });

        row.repositoryNames.forEach(function(repo) {
          repos[repo] -= 1;
          if (repos[repo] === 0) {
             delete repos[repo];
          }
        });

        if (_ctrl.selectedFiles.length === 0) {
          resetSelectedRepos();
        }

      } else {

        // Init
        if (_ctrl.selectedFiles.length === 0) {
          _ctrl.selectedRepos = {};
          repos = _ctrl.selectedRepos;
        }
        _ctrl.selectedFiles.push(row.id);

        var activeRepos = [], filters = LocationService.filters();
        if (filters.file && filters.file.repositoryNames) {
          activeRepos = filters.file.repositoryNames.is;
        }

        row.repositoryNames.forEach(function(repo) {
          if (_.contains(activeRepos, repo) || _.isEmpty(activeRepos)) {
            if (repos.hasOwnProperty(repo)) {
               repos[repo] += 1;
            } else {
               repos[repo] = 1;
            }
          }
        });
      }
      console.log('selected', _ctrl.selectedFiles, _ctrl.selectedRepos);
    };



    /**
     * Undo user selected files
     */
    _ctrl.undo = function() {
      _ctrl.selectedFiles = [];
      _ctrl.selectedRepos = {};

      _ctrl.files.hits.forEach(function(f) {
        delete f.checked;
      });
      resetSelectedRepos();
    };


    function refresh() {
      var promise, params = {};
      var filesParam = LocationService.getJsonParam('files');

      // Default
      params.size = 25;

      if (filesParam.from || filesParam.size) {
        params.from = filesParam.from;
        params.size = filesParam.size || 25;
      }
      if (filesParam.sort) {
        params.sort = filesParam.sort;
        params.order = filesParam.order;
      }

      params.include = 'facets';
      params.filters = LocationService.filters();

      // Get files that match query
      promise = ExternalRepoService.getList( params );
      promise.then(function(data) {
        _ctrl.files = data;

        // Sanity check, just reset everything
        _ctrl.undo();
      });

      // Get index creation time
      ExternalRepoService.getMetaData().then(function(metadata) {
        _ctrl.repoCreationTime = metadata.creation_date || '';
      });

    }

    refresh();

    $scope.$watch(function() { return LocationService.filters(); }, function(n) {
      if (n) {
        $scope.isActive = true;
      }
      if ( n && _ctrl.selectedFiles.length > 0) {
        _ctrl.undo();
      }
    }, true);

    $scope.$on('$locationChangeSuccess', function (event, next) {
      if (next.indexOf('repository') !== -1) {
        // Undo existing selection if filters change
        refresh();
      }
    });

  });


})();
