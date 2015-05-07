(function() {
  'use strict';

  var module = angular.module('icgc.repository.controllers', ['icgc.repository.services']);

  /**
   * This just controllers overall state
   */
  module.controller('RepositoryController', function($state, Page) {
    var _ctrl = this;

    Page.setTitle('Data Repository');
    Page.setPage('repository');

    _ctrl.currentTab = $state.current.data.tab || 'icgc';
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
    };


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
      console.log('state', $stateParams);
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


  /**
   * External repository controller
   */
  module.controller('ExternalRepoController', function($scope, Restangular, RepositoryService, LocationService) {
    console.log('ExternalRepoController');
    var _ctrl = this;

    _ctrl.selectedFiles = [];


    /**
     * Export table
     */
    _ctrl.export = function() {
      console.log('export');
    };


    /**
     * Download manifest
     */
    _ctrl.downloadManifest = function() {
      console.log('downloading manifest');
    };


    _ctrl.toggleRow = function(row) {
      row.checked = !row.checked;
      if (row.checked) {
        _ctrl.selectedFiles.push({});
      } else {
        _ctrl.selectedFiles.pop();
      }
    };


    /**
     * Undo user selected files
     */
    _ctrl.undo = function() {
      _ctrl.selectedFiles = [];
      _ctrl.files.hits.forEach(function(f) {
        delete f.checked;
      });
    };

    function refresh() {
      var promise, query;

      query = {
        filters : LocationService.filters(),
        include: 'facets'
      };

      var filesParam = LocationService.getJsonParam('files');

      if (filesParam) {
        query.from = filesParam.from;
        query.size = filesParam.size || 10;
      }

      promise = Restangular.one('files').get(query);

      promise.then(function(data) {
        console.log('data', data);
        _ctrl.files = data;
      });


    }

    refresh();


    $scope.$on('$locationChangeSuccess', function (event, next) {
      if (next.indexOf('repository') !== -1) {
        refresh();
      }
    });

  });







})();
