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

angular.module('app.ui.dl', []).directive('dlButton',
  function ($filter, $window, $location, LocationService, DownloadService, DataTypes, Donors,
            DownloaderService) {
    return {
      restrict: 'E',
      scope: {
        dlFilter: '@'
      },
      templateUrl: 'views/common/dl.html',
      link: function (scope) {

        var emailRegex = /.+@.+\..+/i;

        scope.opts = {
          keyboard: true,
          backdropClick: true,
          backdropFade: true,
          dialogFade: true
        };


        scope.show = true;

        scope.overallSize = 0;
        scope.dlTotal = 0;
        scope.dlFile = 0;

        scope.useEmail = false;
        scope.emailAddress = '';
        scope.isValidEmail = true;

        function sum(active, size) {
          if (active) {
            scope.dlTotal += size;
            scope.dlFile++;
          } else {
            scope.dlTotal -= size;
            scope.dlFile--;
          }

        }


        scope.validateEmail = function () {
          // No email provided
          if (scope.emailAddress === '') {
            scope.isValidEmail = true;
            return;
          }

          if (scope.emailAddress.match(emailRegex)) {
            scope.isValidEmail = true;
          } else {
            scope.isValidEmail = false;
          }
        };

        scope.toggleEmail = function () {
          scope.useEmail = !scope.useEmail;
          if (scope.useEmail === false) {
            scope.emailAddress = '';
          }
        };

        scope.toggle = function (type) {
          if (type.sizes > 0) {
            type.active = !type.active;
            sum(type.active, type.sizes);
          }
        };

        scope.calculateSize = function () {
          var filters;

          scope.reset();

          scope.dlFile = 0;
          scope.dlTotal = 0;
          scope.calc = true;

          if (scope.dlFilter) {
            filters = JSON.parse(scope.dlFilter);
          } else {
            filters = LocationService.filters();
          }

          // Compute the total number of donors
          Donors.handler.get('count', {filters: filters}).then(function (data) {
            scope.totalDonor = data;
          });

          DownloadService.getSizes(filters).then(function (response) {

            scope.dataTypes = response.fileSize;
            scope.dataTypes.forEach(function (dataType) {
              dataType.active = false;

              dataType.uiLabel = dataType.label;
              dataType.uiLabel = DataTypes.mapping[dataType.label] || dataType.label;
              scope.overallSize += dataType.sizes;
            });

            scope.calc = false;
            scope.modal = true;


            // Re-order it based on importance
            scope.dataTypes = $filter('orderBy')(scope.dataTypes, function (dataType) {
              var index = DataTypes.order.indexOf(dataType.label);
              if (index === -1) {
                return DataTypes.order.length + 1;
              }
              return index;
            });

          });
        };

        scope.reset = function () {
          scope.dlTotal = 0;
          scope.dlFile = 0;
          scope.overallSize = 0;
          scope.errorConcurrentLimit = false;
        };

        scope.dlFiles = function () {
          var i, item, actives, filters;

          // Check for server load
          scope.errorConcurrentLimit = false;
          DownloadService.getStatus().then(function (serverStatus) {
            var linkURL;
            if (serverStatus.status === false) {
              scope.errorConcurrentLimit = true;
            }

            if (scope.errorConcurrentLimit === false) {
              actives = [];
              for (i = 0; i < scope.dataTypes.length; ++i) {
                item = scope.dataTypes[i];
                if (item.active) {
                  actives.push({key: item.label, value: 'TSV'});
                }
              }

              if (scope.dlFilter) {
                filters = JSON.parse(scope.dlFilter);
              } else {
                filters = LocationService.filters();
              }

              linkURL = $location.protocol() + '://' + $location.host() + ':' + $location.port() + '/downloader';

              DownloaderService
                .requestDownloadJob(filters, actives, scope.emailAddress, linkURL).then(function (job) {

                  // Update cache
                  /* Remove local history for now until better user management is in place
                   var current;
                   current = DownloaderService.getCurrentJobIds();
                   if (current.indexOf(job.downloadId) === -1) {
                   current.push(job.downloadId);
                   DownloaderService.setCurrentJobIds(current);
                   }
                   */

                  scope.modal = false;
                  scope.reset();

                  $location.path('/downloader/' + job.data.downloadId).search('');
                });


              /*
               $location.path('/downloader');
               */
            }
          });
        };
      }
    };
  });
