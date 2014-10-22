/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

angular.module('app.download.controllers', ['app.download.services']);

// Note download may result in unwarranted warnings, see:
// http://stackoverflow.com/questions/15393210/chrome-showing-canceled-on-successful-file-download-200-status
angular.module('app.download.controllers').controller('DownloadController',
  function ($window, $filter, $scope, Page, $stateParams, DownloadService, Restangular) {
    Page.setTitle('Data Repository');
    Page.setPage('repository');

    $scope.path = $stateParams.path;
    $scope.slugs = [];

    function buildBreadcrumbs() {
      var i, s, slug, url;

      url = '';
      s = $scope.path.split('/').filter(Boolean); // removes empty cells

      for (i = 0; i < s.length; ++i) {
        slug = s[i];
        url += slug + '/';
        $scope.slugs.push({name: slug, url: url});
      }
    }

    buildBreadcrumbs();

    DownloadService.folder($scope.path).then(function (response) {
      var files, firstSort;
      files = response;

      files.forEach(function (file) {
        var name, tName, extension;

        // For convienence
        file.baseName = file.name.split('/').pop();

        // Check if there is a translation code for directories (projects)
        if (file.type === 'd') {
          name = (file.name).split('/').pop();
          tName = $filter('define')(name);
          if (name !== tName) {
            file.translation = tName;
          }
        }

        // Check file extension
        extension = file.name.split('.').pop();
        if (_.contains(['txt', 'me'], extension.toLowerCase())) {
          file.isText = true;
        } else {
          file.isText = false;
        }

      });

      // Order the files and folders, see DCC-1648, basically
      // readme > current > others releases > legacy releases
      function logicalSort(file) {
        var pattern, name;

        name = file.name.split('/').pop();

        pattern = /notice\.txt$/i;
        if (pattern.test(name)) {
          return -4;
        }

        pattern = /readme\.txt$/i;
        if (pattern.test(name)) {
          return -3;
        }

        pattern = /^current/i;
        if (pattern.test(name)) {
          return -2;
        }

        pattern = /^summary/i;
        if (pattern.test(name)) {
          return -1;
        }

        pattern = /^legacy_data_releases/i;
        if (pattern.test(name)) {
          return files.length + 1;
        }
        return firstSort.indexOf(file.name);
      }

      if ($scope.slugs.length > 0) {
        files = $filter('orderBy')(files, 'name');
        firstSort = _.pluck(files, 'name');
        files = $filter('orderBy')(files, logicalSort);
      } else {
        files = $filter('orderBy')(files, 'date', 'reverse');
        firstSort = _.pluck(files, 'name');
        files = $filter('orderBy')(files, logicalSort);
      }

      $scope.files = files;

      // Fetch data content if file is plain text
      $scope.textFiles = _.filter(files, function(f) {
        return f.type === 'f' && f.isText === true;
      });
      $scope.textFiles.forEach(function(f) {
        Restangular.one('download').get( {'fn':f.name}).then(function(data) {
          f.textContent = data;
        });
      });

    });

  });
