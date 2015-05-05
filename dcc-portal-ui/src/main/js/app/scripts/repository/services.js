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

  var module = angular.module('icgc.repository.services', []);

  module.service('RepositoryService', function ($filter, Restangular) {

    this.getFiles = function (filters, actives) {
      return Restangular.one('download', '').get({
        filters: filters,
        info: actives
      });
    };

    this.folder = function (path) {
      return Restangular.one('download', 'info' + path).get();
    };

    this.getStatus = function () {
      return Restangular.one('download', 'status').get();
    };


    /**
     *  Order the files and folders, see DCC-1648, basically
     * readme > current > others releases > legacy releases
     */
    this.sortFiles = function( files, dirLevel ) {
      var firstSort;

      function logicalSort(file) {
        var pattern, name;
        name = file.name.split('/').pop();

        pattern = /notice\.(txt|md)$/i;
        if (pattern.test(name)) {
          return -4;
        }

        pattern = /readme\.(txt|md)$/i;
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

      if (dirLevel > 0) {
        files = $filter('orderBy')(files, 'name');
        firstSort = _.pluck(files, 'name');
        files = $filter('orderBy')(files, logicalSort);
      } else {
        files = $filter('orderBy')(files, 'date', 'reverse');
        firstSort = _.pluck(files, 'name');
        files = $filter('orderBy')(files, logicalSort);
      }

      return files;
    };

  });

})();
