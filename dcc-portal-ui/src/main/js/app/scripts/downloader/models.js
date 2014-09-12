/*
 * Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.
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

angular.module('app.downloader.model', []);

angular.module('app.downloader.model').factory('Downloader', function ($http) {
  return {

    requestDownloadJob: function (filters, info, email, downloadUrl) {
      var url;
      filters = JSON.stringify(filters);
      info = JSON.stringify(info);
      url = '/api/v1/download/submit?' +
            'filters=' + filters +
            '&info=' + info +
            '&email=' + email +
            '&downloadUrl=' + downloadUrl;
      return $http.get(url);
    },

    cancelJob: function (ids) {
      return $http.get('/api/v1/download/' + ids + '/cancel');
    },

    getJobMetaData: function (ids) {
      if (angular.isArray(ids)) {
        return $http.get('/api/v1/download/JobInfo?downloadId=' + ids.join(','));
      }
      return $http.get('/api/v1/download/JobInfo?downloadId=' + ids);
    },

    getJobStatus: function (ids) {
      if (angular.isArray(ids)) {
        return $http.get('/api/v1/download/' + ids.join(',') + '/status');
      }
      return $http.get('/api/v1/download/' + ids + '/status');
    }
  };
});
