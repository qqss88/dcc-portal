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

  var module = angular.module('icgc.keyword', ['icgc.keyword.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('q', {
      url: '/q?q',
      reloadOnSearch: false,
      templateUrl: '/scripts/keyword/views/results.html',
      controller: 'KeywordController'
    });
  });
})();

(function () {
  'use strict';

  _.mixin ({
    pairUp: function (arrays) {
      var resolved = _.map (arrays, function (o) {
        return o.value();
      });
      return _.zip.apply (_, resolved);
    }
  });

  function ensureArray (array) {
    return _.isArray (array) ? array : [];
  }
  function ensureString (string) {
    return _.isString (string) ? string.trim() : '';
  }

  // TODO: move this out.
  function Abridger (maxLength) {
    this.maxLength = maxLength;

    var ellipsis = '...';
    var _this = this;

    this.find = function (sentence, keyword) {
      var words = _.words (sentence);
      var index = _.findIndex (words, function (word) {
        return _.contains (word, keyword);
      });

      return {
        target: words [index],
        left: _(words).take (index).reverse(),
        right: _(words).slice (index).rest()
      };
    };
    var withinLimit = this.withinLimit = function (newElements) {
      var combined = _(_this.resultArray).concat (newElements);

      var numberOfCharacters = combined.map ('length').sum();
      var numberOfSpaces = combined.size() - 1;

      return (numberOfCharacters + numberOfSpaces) <= _this.maxLength;
    };

    this.processLeftAndRight = function (newElements) {
      var left = _.first (newElements);
      var right = _.last (newElements);

      if (withinLimit (newElements)) {
        _this.resultArray = [left].concat (_this.resultArray, right);
        return true;
      } else {

        if (_.size (left) >= _.size (right)) {
          if (withinLimit (left)) {
            _this.currentProcessor = _this.processLeftOnly;
            return _this.currentProcessor (newElements);
          } else if (withinLimit (right)) {
            _this.currentProcessor = _this.processRightOnly;
            return _this.currentProcessor (newElements);
          }
        } else {
          if (withinLimit (right)) {
            _this.currentProcessor = _this.processRightOnly;
            return _this.currentProcessor (newElements);
          } else if (withinLimit (left)) {
            _this.currentProcessor = _this.processLeftOnly;
            return _this.currentProcessor (newElements);
          }
        }

      }

      return false;
    };

    this.processLeftOnly = function (newElements) {
      var left = _.first (newElements);

      if (withinLimit (left)) {
        _this.resultArray = [left].concat (_this.resultArray);
        return true;
      }

      return false;
    }

    this.processRightOnly = function (newElements) {
      var right = _.last (newElements);

      if (withinLimit (right)) {
        _this.resultArray = _this.resultArray.concat (right);
        return true;
      }

      return false;
    };
    this.hasRoom = function (newElements) {
      return _this.currentProcessor (newElements);
    };

    this.format = function (fragments, sentence) {
      var joined = fragments.join (' ').trim();
      var dots = function (f) {
        return f (sentence, joined) ? '' : ellipsis;
      };

      return dots (_.startsWith) + joined + dots (_.endsWith);
    };

    this.abridge = function (sentence, keyword) {
      var finding = _this.find (sentence, keyword);

      _this.resultArray = [finding.target];
      _this.currentProcessor = _this.processLeftAndRight;

      _([finding.left, finding.right])
        .pairUp()
        .takeWhile (_this.hasRoom)
        .value();

      return this.format (_this.resultArray, sentence);
    };

    this.currentProcessor = this.processLeftAndRight;
    this.resultArray = [];
  };

  var module = angular.module('icgc.keyword.controllers', ['icgc.keyword.models']);

  module.controller('KeywordController',
    function ($scope, Page, LocationService, debounce, Keyword, RouteInfoService) {
      var pageSize;

      $scope.from = 1;
      pageSize = 20;

      $scope.query = LocationService.getParam('q') || '';
      $scope.type = LocationService.getParam('type') || 'all';
      $scope.isBusy = false;
      $scope.isFinished = false;
      $scope.dataRepoFileUrl = RouteInfoService.get ('dataRepositoryFile').href;

      Page.setTitle('Results for ' + $scope.query);
      Page.setPage('q');

      $scope.clear = function () {
        $scope.query = '';
        LocationService.clear();
        $scope.quickFn();
      };

      $scope.next = function () {
        if ($scope.isBusy || $scope.isFinished) {
          return;
        }

        $scope.from += pageSize;
        getResults({scroll: true});
      };


      $scope.badgeStyleClass = function (type) {
        // FIXME: temp. mapping
        type = ('drug' === type) ? 'compound' : type;

        var definedType = _.contains (['pathway', 'go_term', 'curated_set'], type) ? 'geneset' : type;
        return 't_badge t_badge__' + definedType;
      };

      $scope.matchElements = function (array, target) {
        var matches = _.filter (ensureArray (array), function (element) {
          return _.contains (ensureString (element), target);
        });

        return matches.join (', ');
      };

      $scope.concatIfContains = function (array, target) {
        array = ensureArray (array);
        var contains = _.any (array, function (element) {
          return _.contains (ensureString (element), target);
        });

        return contains ? array.join (', ') : '';
      };

      var maxAbrigementLength = 60;
      var abridger = new Abridger (maxAbrigementLength);

      $scope.abridge = function (array, target) {
        var match = _.find (ensureArray (array), function (sentence) {
          return _.contains (ensureString (sentence), target);
        });

        return match ? abridger.abridge (match, target) : '';
      };

      $scope.quickFn = function () {
        $scope.from = 1;

        if ($scope.query && $scope.query.length >= 2) {
          LocationService.setParam('q', $scope.query);
          Page.setTitle('Results for ' + $scope.query);
          getResults();
        } else {
          $scope.results = null;
        }
      };

      $scope.quick = debounce($scope.quickFn, 200, false);

      function getResults(settings) {
        var saved = $scope.query;

        settings = settings || {};
        $scope.isBusy = true;

        Keyword.getList({q: $scope.query, type: $scope.type, size: pageSize, from: $scope.from})
          .then(function (response) {
            $scope.isFinished = response.pagination.total - $scope.from < pageSize;
            $scope.isBusy = false;
            $scope.activeQuery = saved;

            if (settings.scroll) {
              $scope.results.pagination = response.pagination ? response.pagination : {};
              for (var i = 0; i < response.hits.length; i++) {
                $scope.results.hits.push(response.hits[i]);
              }
            } else {
              $scope.results = response;
            }
          });
      }

      $scope.$watch(function () {
        return LocationService.search();
      }, function (n) {
        if (LocationService.path() === '/q') {
          $scope.query = n.q;
          $scope.type = n.type ? n.type : 'all';
          $scope.quickFn();
        }
      }, true);

      $scope.$watch('from', function (n) {
        $scope.limit = n > 100;
      });

      getResults();
    });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.keyword.models', ['restangular']);

  module.service('Keyword', function (Restangular) {

    this.all = function () {
      return Restangular.all('keywords');
    };

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        type: 'all'
      };

      return this.all().get('', angular.extend(defaults, params));
    };
  });
})();
