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

  function ensureArray (array) {
    return _.isArray (array) ? array : [];
  }
  var isEmptyArray = _.flow (ensureArray, _.isEmpty);

  function ensureString (string) {
    return _.isString (string) ? string.trim() : '';
  }
  var isEmptyString = _.flow (ensureString, _.isEmpty);

  function ensureObject (o) {
    return _.isPlainObject (o) ? o : {};
  }

  function stringOrDefault (s) {
    return isEmptyString (s) ? '--' : s;
  }

  var toJson = angular.toJson;
  var commaAndSpace = ', ';

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

  });

  /**
   * ICGC static repository controller
   */
  module.controller('ICGCRepoController',
    function($scope, $stateParams, Restangular, RepositoryService, ProjectCache, API, Settings) {
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

    $scope.selectedFiles = params.selectedFiles;
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    var p = {};
    p.size = 0;
    p.filters = LocationService.filters();
    if ($scope.selectedFiles && !_.isEmpty($scope.selectedFiles)) {
      if (! p.filters.file) {
        p.filters.file = {};
      }
      p.filters.file.id = {is: $scope.selectedFiles};
    }

    function findRepoData(list, term) {
      return _.find(list, function(t) { return t.term === term; }).count || 0;
    }

    ExternalRepoService.getList (p).then (function (data) {
      var facets = data.termFacets;
      var activeRepos = [];

      if (p.filters.file && p.filters.file.repoName) {
        activeRepos = p.filters.file.repoName.is;
      }

      // Build the repo table
      var repos = {};
      facets.repositoryNamesFiltered.terms.forEach(function(term) {
        var repoName = term.term;

        // Restrict to active repos if it is available
        if (!_.isEmpty(activeRepos) && !_.contains(activeRepos, repoName)) {
          return;
        }

        if (! repos[repoName]) {
          repos[repoName] = {};
        }

        repos[repoName].fileSize = findRepoData(facets.repositorySizes.terms, repoName);
        repos[repoName].donorCount = findRepoData(facets.repositoryDonors.terms, repoName);
        repos[repoName].fileCount = term.count;
      });

      $scope.repos = repos;
      $scope.selectedRepos = Object.keys(repos);
    });


    $scope.download = function() {
      if (_.isEmpty($scope.selectedFiles)) {
        ExternalRepoService.download(LocationService.filters(), $scope.selectedRepos);
      } else {
        ExternalRepoService.downloadSelected($scope.selectedFiles, $scope.selectedRepos);
      }
      $scope.cancel();
    };

  });

  /**
   * Controller for File Entity page
   */
  module.controller('ExternalFileInfoController', function (Page, ExternalRepoService, fileInfo) {

    Page.setTitle('External File Entity');
    Page.setPage('externalFileEntity');

    var slash = '/';

    this.fileInfo = fileInfo;
    this.stringOrDefault = stringOrDefault;

    // Private helpers
    function convertToString (input) {
      return _.isString (input) ? input : (input || '').toString();
    }

    function toUppercaseString (input) {
      return convertToString (input).toUpperCase();
    }

    function removeBookendingSlash (input) {
      var inputString = convertToString (input);

      if (inputString.length < 1) {
        return input;
      }

      return inputString.replace (/^\/+|\/+$/g, '');
    }

    function equalsIgnoringCase (test, expected) {
      return toUppercaseString (test) === toUppercaseString (expected);
    }

    // Public functions
    this.buildUrl = function (baseUrl, dataPath, entityId) {
      // Removes any opening and closing slash in all parts then concatenates.
      return _.map ([baseUrl, dataPath, entityId], removeBookendingSlash)
        .join (slash);
    };

    this.equalsIgnoringCase = equalsIgnoringCase;

    this.downloadManifest = function (fileId, repo) {
      ExternalRepoService.downloadSelected ([fileId], [repo]);
    };

    this.noNullConcat = function (values) {
      var result = isEmptyArray (values) ? '' : _.filter (values, _.negate (isEmptyString)).join (commaAndSpace);
      return stringOrDefault (result);
    };

    this.shouldShowMetaData = function (repoType) {
      return equalsIgnoringCase (repoType, 'GNOS') || equalsIgnoringCase (repoType, 'S3');
    };

  });

  /**
   * External repository controller
   */
  module.controller ('ExternalRepoController',
    function ($scope, $window, $modal, LocationService, Page, ExternalRepoService, SetService, ProjectCache) {

    var projectMap = {};
    var _ctrl = this;

    _ctrl.selectedFiles = [];
    _ctrl.summary = {};

    function toSummarizedString (values, name) {
      var size = _.size (values);
      return (size > 1) ? '' + size + ' ' + name + 's' :
        _.first (values);
    }

    function createFilter (category, ids) {
      return encodeURIComponent (toJson (_.set ({}, '' + category + '.id.is', ids)));
    }

    function buildDataInfo (data, property, paths, category, toolTip) {
      var ids = _(ensureArray (data))
        .map (property)
        .unique()
        .value();

      return isEmptyArray (ids) ? {} : {
        text: toSummarizedString (ids, category),
        tooltip: toolTip (ids),
        href: _.size (ids) > 1 ?
          paths.many + createFilter (category, ids) :
          paths.one + _.first (ids)
      };
    }

    _ctrl.buildDonorInfo = function (donors) {
      var toolTipMaker = function () {
        return '';
      };
      return buildDataInfo (donors, 'donorId', {one: '/donors/', many: '/search?filters='},
        'donor', toolTipMaker);
    };

    _ctrl.buildProjectInfo = function (donors) {
      var toolTipMaker = function (ids) {
        return _.size (ids) === 1 ? _.get (projectMap, _.first (ids), '') : '';
      };
      return buildDataInfo (donors, 'projectCode', {one: '/projects/', many: '/projects?filters='},
        'project', toolTipMaker);
    };

    function uniquelyConcat (fileCopies, property) {
      return _(fileCopies)
        .map (property)
        .unique()
        .join(commaAndSpace);
    }

    /**
     * Tablular display
     */
    _ctrl.repoNames = function (fileCopies) {
      return uniquelyConcat (fileCopies, 'repoName');
    };

    _ctrl.fileFormats = function (fileCopies) {
      return uniquelyConcat (fileCopies, 'fileFormat');
    };

    _ctrl.fileNames = function (fileCopies) {
      return _(fileCopies)
        .map ('fileName')
        .unique()
        .join ('<br>');
    };

    _ctrl.fileAverageSize = function (fileCopies) {
      var count = _.size (fileCopies);
      return (count > 0) ? _.sum (fileCopies, 'fileSize') / count : 0;
    };

    /**
     * Export table
     */
    _ctrl.export = function() {
      ExternalRepoService.export (LocationService.filters());
    };

    /**
     * View in Advanced Search
     */
    _ctrl.viewInSearch = function (limit) {
      var params = {};
      params.filters = LocationService.filters();
      params.size = limit;
      params.isTransient = true;
      SetService.createForwardRepositorySet ('donor', params, '/search');
    };

    /**
     * Save a donor set from files
     */
    _ctrl.saveDonorSet = function (type, limit) {
      _ctrl.setLimit = limit;
      _ctrl.setType = type;

      $modal.open ({
        templateUrl: '/scripts/sets/views/sets.upload.external.html',
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

    /**
     * Download manifest
     */
    _ctrl.downloadManifest = function() {
      $modal.open ({
        templateUrl: '/scripts/repository/views/repository.external.submit.html',
        controller: 'ExternalFileDownloadController',
        size: 'lg',
        resolve: {
          params: function() {
            return {
              selectedFiles: _ctrl.selectedFiles
            };
          }
        }
      });
    };

    _ctrl.isSelected = function (row) {
      return _.contains (_ctrl.selectedFiles, row.id);
    };

    _ctrl.toggleRow = function (row) {
      if (_ctrl.isSelected (row) === true) {
        _.remove (_ctrl.selectedFiles, function (r) {
          return r === row.id;
        });
      } else {
        _ctrl.selectedFiles.push (row.id);
      }
    };

    /**
     * Undo user selected files
     */
    _ctrl.undo = function() {
      _ctrl.selectedFiles = [];

      _ctrl.files.hits.forEach (function (f) {
        delete f.checked;
      });
    };


    function refresh() {
      var promise, params = {};
      var filesParam = LocationService.getJsonParam ('files');

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
      promise = ExternalRepoService.getList (params);
      promise.then (function (data) {
        _ctrl.files = data;

        // Sanity check, just reset everything
        _ctrl.undo();
      });

      // Get summary
      ExternalRepoService.getSummary (params).then (function (summary) {
        _ctrl.summary = summary;
      });

      // Get index creation time
      ExternalRepoService.getMetaData().then (function (metadata) {
        _ctrl.repoCreationTime = metadata.creation_date || '';
      });

      ProjectCache.getData().then (function (cache) {
        projectMap = ensureObject (cache);
      });

    }

    refresh();

    $scope.$watch (function() {return LocationService.filters();}, function (n) {
      if (n) {
        $scope.isActive = true;
      }

      if (n && _ctrl.selectedFiles.length > 0) {
        _ctrl.undo();
      }
    }, true);

    $scope.$on ('$locationChangeSuccess', function (event, next) {
      if (next.indexOf ('repository') !== -1) {
        // Undo existing selection if filters change
        refresh();
      }
    });
  });

})();
