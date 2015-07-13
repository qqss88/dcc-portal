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

(function () {
  'use strict';

  var module = angular.module('icgc.donors', ['icgc.donors.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('donor', {
      url: '/donors/:id',
      templateUrl: 'scripts/donors/views/donor.html',
      controller: 'DonorCtrl as DonorCtrl',
      resolve: {
        donor: ['$stateParams', 'Donors', function ($stateParams, Donors) {
          return Donors.one($stateParams.id).get({include: 'specimen'});
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.donors.controllers', ['icgc.donors.models']);

  module.controller('DonorCtrl',
    function ($scope, $modal, Page, donor, Projects, Mutations, Settings, ExternalRepoService, PCAWG) {

    var _ctrl = this, promise;

    Page.setTitle(donor.id);
    Page.setPage('entity');


    _ctrl.hasSupplementalFiles = function(donor) {
      return donor.family || donor.exposure || donor.therapy;
    };

    _ctrl.isPCAWG = function(donor) {
      return _.any(donor.studies, PCAWG.isPCAWGStudy);
    };

    _ctrl.donor = donor;

    _ctrl.isPendingDonor = _.isUndefined (_.get(donor, 'primarySite'));

    var donorFilter = {
      file: {
        donorId: {
          is: [donor.id]
        }
      }
    };
    _ctrl.urlToExternalRepository = '/repository/external?filters=' + angular.toJson (donorFilter);

    _ctrl.donor.clinicalXML = null;
    promise = ExternalRepoService.getList({
      filters: {
        file: {
          donorId: {is: [_ctrl.donor.id]},
          dataFormat: { is: ['XML']}
        }
      }
    });
    promise.then(function(results) {
      if (results.hits && results.hits[0]) {
        var file = results.hits[0];
        var repo = file.repository;
        _ctrl.donor.clinicalXML = repo.repoServer[0].repoBaseUrl.replace(/\/$/, '') +
          repo.repoDataPath + repo.repoEntityId;
      }
    });


    _ctrl.downloadDonorData = function() {
      $modal.open({
        templateUrl: '/scripts/downloader/views/request.html',
        controller: 'DownloadRequestController',
        resolve: {
          filters: function() {
            return {
              donor: { id: { is: [_ctrl.donor.id] } }
            };
          }
        }
      });
    };

    Projects.getList().then(function (projects) {
      var p = _.find(projects.hits, function (item) {
        return item.id === donor.projectId;
      });

      if (p) {
        _ctrl.donor.ssmTestedDonorCount = p.ssmTestedDonorCount;
      }
    });


    function refresh() {
      var params = {
        filters: {'donor': {'id': {'is': [_ctrl.donor.id]}}},
        size: 0,
        include: ['facets']
      };
      Mutations.getList(params).then(function (d) {
        _ctrl.mutationFacets = d.facets;
      });

      Settings.get().then(function(settings) {
        _ctrl.downloadEnabled = settings.downloadEnabled || false;
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('donors') !== -1) {
        refresh();
      }
    });

    refresh();

  });

  module.controller('DonorMutationsCtrl', function ($scope, Donors, Projects, LocationService, ProjectCache) {
    var _ctrl = this, donor;

    function success(mutations) {
      if (mutations.hasOwnProperty('hits')) {
        var projectCachePromise = ProjectCache.getData();

        _ctrl.mutations = mutations;

        _ctrl.mutations.advQuery = LocationService.mergeIntoFilters({donor: {id: {is: [donor.id]}}});

        // Need to get SSM Test Donor counts from projects
        Projects.getList().then(function (projects) {
          _ctrl.mutations.hits.forEach(function (mutation) {
            Donors.getList({
              size: 0,
              include: 'facets',
              filters: LocationService.mergeIntoFilters({mutation: {id: {is: mutation.id}}})
            }).then(function (data) {
              mutation.advQuery = LocationService.mergeIntoFilters(
                {donor: {projectId: {is: [donor.projectId]}},
                  mutation: {id: {is: [mutation.id]}}}
              );

              mutation.advQueryAll = LocationService.mergeIntoFilters(
                {mutation: {id: {is: [mutation.id]}}}
              );

              mutation.uiDonors = data.facets.projectId.terms;
              mutation.uiDonors.forEach(function (facet) {
                var p = _.find(projects.hits, function (item) {
                  return item.id === facet.term;
                });

                projectCachePromise.then(function(lookup) {
                  facet.projectName = lookup[facet.term] || facet.term;
                });

                facet.advQuery = LocationService.mergeIntoFilters(
                  {mutation: {id: {is: [mutation.id]}},
                    donor: {projectId: {is: [facet.term]}}}
                );

                facet.countTotal = p.ssmTestedDonorCount;
                facet.percentage = facet.count / p.ssmTestedDonorCount;
              });
            });
          });
        });
      }
    }

    function refresh() {
      Donors.one().get({include: 'specimen'}).then(function (d) {
        donor = d;
        Donors.one().getMutations({
          include: 'consequences',
          filters: LocationService.filters(),
          scoreFilters: {donor: {projectId: {is: donor.projectId }}}
        }).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('donors') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('DonorSpecimenCtrl', function (Donors, PCAWG) {
    var _ctrl = this;

    _ctrl.PCAWG = PCAWG;

    _ctrl.isPCAWG = function(specimen) {
      return _.any(_.pluck(specimen.samples, 'study'), PCAWG.isPCAWGStudy);
    };

    _ctrl.setActive = function (id) {
      Donors.one().get({include: 'specimen'}).then(function (donor) {
        if (donor.hasOwnProperty('specimen')) {
          _ctrl.active = id || donor.specimen[0].id;
          _ctrl.specimen = _.find(donor.specimen, function (s) {
            return s.id === _ctrl.active;
          });
        }
      });
    };

    _ctrl.setActive(null);
  });

})();

(function () {
  'use strict';

  var module = angular.module('icgc.donors.models', ['restangular', 'icgc.common.location']);

  module.service('Donors', function (Restangular, LocationService, Donor) {
    this.handler = Restangular.all('donors');

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: LocationService.filters()
      };


      // Sanitize filters, we want to enforce donor.state == 'live'
      var liveFilters = angular.extend(defaults, _.cloneDeep(params));
      if (! liveFilters.filters.donor) {
        liveFilters.filters.donor = {};
      }
      liveFilters.filters.donor.state = { is: ['live']};

      return this.handler.one('', '').get(liveFilters).then(function (data) {
        if (data.hasOwnProperty('facets')) {
          for (var facet in data.facets) {
            if (data.facets.hasOwnProperty(facet) && data.facets[facet].missing) {
              var f = data.facets[facet];
              if (f.hasOwnProperty('terms')) {
                f.terms.push({term: '_missing', count: f.missing});
              } else {
                f.terms = [
                  {term: '_missing', count: f.missing}
                ];
              }
            }
          }
          if (data.facets.hasOwnProperty('projectId') && data.facets.projectId.hasOwnProperty('terms')) {
            data.facets.projectId.terms = data.facets.projectId.terms.sort(function (a, b) {
              if (a.term < b.term) {
                return -1;
              }
              if (a.term > b.term) {
                return 1;
              }
              return 0;
            });
          }
        }

        return data;
      });
    };

    this.one = function (id) {
      return id ? Donor.init(id) : Donor;
    };
  });

  module.service('Donor', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.id = id;
      this.handler = Restangular.one('donors', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };

    this.getMutations = function (params) {
      var defaults = {
        size: 10,
        from: 1
      };

      return this.handler.one('mutations', '').get(angular.extend(defaults, params));
    };
  });
})();
