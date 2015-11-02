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

  var defaultString = '--';
  function stringOrDefault (s) {
    return isEmptyString (s) ? defaultString : s;
  }

  var toJson = angular.toJson;
  var commaAndSpace = ', ';

  var module = angular.module('icgc.repository.controllers', ['icgc.repository.services']);

// FIXME: No longer in use. To be removed.
  /**
   * This just controllers overall state
   */
/*
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
*/
  /**
   * ICGC static repository controller
   */
  module.controller('ICGCRepoController', function($scope, $stateParams, Restangular, RepositoryService,
    ProjectCache, API, Settings, Page, RouteInfoService) {
    var _ctrl = this;
    var dataReleasesRouteInfo = RouteInfoService.get ('dataReleases');

    Page.setTitle (dataReleasesRouteInfo.title);
    Page.setPage ('dataReleases');

    _ctrl.path = $stateParams.path || '';
    _ctrl.slugs = [];
    _ctrl.API = API;
    _ctrl.deprecatedReleases = ['release_15'];
    _ctrl.downloadEnabled = true;
    _ctrl.dataReleasesTitle = dataReleasesRouteInfo.title;
    _ctrl.dataReleasesUrl = dataReleasesRouteInfo.href;

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
    $scope.$watch (function() {
      return $stateParams.path;
    }, function() {
      _ctrl.path = $stateParams.path || '';
      _ctrl.slugs = [];
      refresh();
    });

  });

  module.controller('ExternalFileDownloadController',
    function($scope, $window, $document, $modalInstance, ExternalRepoService, SetService, LocationService, params) {

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

    $scope.createManifestId = function (repoName, fileCount, event) {
      var docRoot = $document [0];
      var canCopyToClipboard = docRoot.queryCommandSupported ('copy');
      var parentNode = event.target.parentNode;
      var domNode = jQuery (parentNode);
      domNode.html ('<i class="icon-spinner icon-spin pull-right" />');

      var selectedFiles = $scope.selectedFiles;
      var filters = LocationService.filters();

      if (! _.isEmpty (selectedFiles)) {
        filters = _.set (filters, 'file.id.is', selectedFiles);
      }

      filters = _.set (filters, 'file.repoName.is', [repoName]);

      var params = {
        size: fileCount,
        isTransient: true,
        filters: filters
      };

      SetService.createFileSet (params).then (function (data) {
        if (! data.id) {
          console.log('No Manifest UUID is returned from API call.');
          return;
        }

        var widgetHtml = '<input class="input_manifest' +
          (canCopyToClipboard ? '' : ' pull-right') +
          '" type="text" size="38" readonly ' +
          'onClick="this.setSelectionRange (0, this.value.length)" value="' +
           data.id + '" />';

        if (canCopyToClipboard) {
          widgetHtml += '<button style="border: 0; background: transparent;" title="Copy Manifest ID">' +
            '<span class="icon-clippy" /></button>';
        }

        domNode.html (widgetHtml);

        if (canCopyToClipboard) {
          domNode.children ('button').click (function () {
            domNode.children ('input[type=text]').select();
            docRoot.execCommand('copy');
          });
        }
        domNode.children ('input[type=text]').select();
     });
    };

  });

  /**
   * Controller for File Entity page
   */
  module.controller('ExternalFileInfoController',
    function (Page, ExternalRepoService, CodeTable, ProjectCache, PCAWG, fileInfo) {

    Page.setTitle('Repository File');
    Page.setPage('externalFileEntity');

    var slash = '/';
    var projectMap = {};

    function refresh () {
      ProjectCache.getData().then (function (cache) {
        projectMap = ensureObject (cache);
      });
    }
    refresh();

    this.fileInfo = fileInfo;
    this.stringOrDefault = stringOrDefault;
    this.isEmptyString = isEmptyString;
    this.defaultString = defaultString;

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

    function isS3 (repoType) {
      return equalsIgnoringCase (repoType, 'S3');
    }

    function isGnos (repoType) {
      return equalsIgnoringCase (repoType, 'GNOS');
    }

    // Public functions
    this.projectName = function (projectCode) {
      return _.get (projectMap, projectCode, '');
    };

    this.buildUrl = function (baseUrl, dataPath, entityId) {
      // Removes any opening and closing slash in all parts then concatenates.
      return _.map ([baseUrl, dataPath, entityId], removeBookendingSlash)
        .join (slash);
    };

    this.buildMetaDataUrl = function (fileCopy, fileInfo) {
      var parts = isS3 (fileCopy.repoType) ?
        [fileCopy.repoBaseUrl, fileCopy.repoMetadataPath] :
        [fileCopy.repoBaseUrl, fileCopy.repoMetadataPath, fileInfo.dataBundle.dataBundleId];

      return _.map (parts, removeBookendingSlash)
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
      return isGnos (repoType) || isS3 (repoType);
    };

    this.isS3 = isS3;

    this.translateDataType = function (dataType) {
      var longName = PCAWG.translate (dataType);

      return (longName === dataType) ? dataType : longName + ' (' + dataType + ')';
    };

    this.translateCountryCode = CodeTable.translateCountryCode;
    this.countryName = CodeTable.countryName;
  });

  /**
   * External repository controller
   */
  module.controller ('ExternalRepoController', function ($scope, $window, $modal, LocationService, Page,
    ExternalRepoService, SetService, ProjectCache, CodeTable, RouteInfoService) {

    var dataRepoTitle = RouteInfoService.get ('dataRepositories').title;

    Page.setTitle (dataRepoTitle);
    Page.setPage ('repository');

    var tabNames = {
      files: 'Files',
      donors: 'Donors'
    };
    var currentTabName = tabNames.files;
    var projectMap = {};
    var _ctrl = this;

    _ctrl.selectedFiles = [];
    _ctrl.summary = {};
    _ctrl.dataRepoTitle = dataRepoTitle;
    _ctrl.dataRepoFileUrl = RouteInfoService.get ('dataRepositoryFile').href;
    _ctrl.advancedSearchInfo = RouteInfoService.get ('advancedSearch');

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

    _ctrl.setTabToFiles = function() {
      currentTabName = tabNames.files;
    };
    _ctrl.setTabToDonors = function() {
      currentTabName = tabNames.donors;
    };

    _ctrl.isOnFilesTab = function() {
      return currentTabName === tabNames.files;
    };
    _ctrl.isOnDonorsTab = function() {
      return currentTabName === tabNames.donors;
    };

    _ctrl.donorInfo = function (donors) {
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

    function tooltipList (objects, property, oneItemHandler) {
      var uniqueItems = _(objects)
        .map (property)
        .unique();

      if (uniqueItems.size() < 2) {
        return _.isFunction (oneItemHandler) ? oneItemHandler() :
          '' + oneItemHandler;
      }
      return uniqueItems.map (function (s) {
          return '<li>' + s;
        })
        .join ('</li>');
    }

    _ctrl.fileNames = function (fileCopies) {
      return tooltipList (fileCopies, 'fileName', function () {
          return _.get (fileCopies, '[0].fileName', '');
        });
    };

    _ctrl.repoNamesInTooltip = function (fileCopies) {
      return tooltipList (fileCopies, 'repoName', '');
    };

    _ctrl.fileAverageSize = function (fileCopies) {
      var count = _.size (fileCopies);
      return (count > 0) ? _.sum (fileCopies, 'fileSize') / count : 0;
    };

    _ctrl.flagIconClass = function (projectCode) {
      var defaultValue = '';
      var last3 = _.takeRight (ensureString (projectCode), 3);

      if (_.size (last3) < 3 || _.first (last3) !== '-') {
        return defaultValue;
      }

      var last2 = _.rest (last3).join ('');

      return 'flag flag-' + CodeTable.translateCountryCode (last2.toLowerCase());
    };

    _ctrl.repoIconClass = function (repoName) {
      var repoCode = ExternalRepoService.getRepoCodeFromName (repoName);

      if (! _.isString (repoCode)) {
        return '';
      }

      if (_.startsWith (repoCode, 'aws-') || repoCode === 'collaboratory') {
        return 'icon-cloud';
      }

      return '';
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

    function removeCityFromRepoName (repoName) {
      if (_.contains (repoName, 'CGHub')) {
        return 'CGHub';
      }

      if (_.contains (repoName, 'TCGA DCC')) {
        return 'TCGA DCC';
      }

      return repoName;
    }

    function fixRepoNameInTableData (data) {
      _.forEach (data, function (row) {
        _.forEach (row.fileCopies, function (fileCopy) {
          fileCopy.repoName = removeCityFromRepoName (fileCopy.repoName);
        });
      });
    }

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
        // Vincent asked to remove city names from repository names for CGHub and TCGA DCC.
        fixRepoNameInTableData (data.hits);
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
      if (next.indexOf ('repositories') !== -1) {
        // Undo existing selection if filters change
        refresh();
      }
    });
  });

})();
