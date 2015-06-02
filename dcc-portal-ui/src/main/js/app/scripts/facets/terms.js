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

  var module = angular.module('icgc.facets.terms', ['icgc.facets.helpers']);

  module.controller('termsCtrl', function ($scope, $filter, Facets, HighchartsService, ProjectCache, ValueTranslator) {
    function setActiveTerms() {
      $scope.actives = Facets.getActiveTerms({
        type: $scope.type,
        facet: $scope.facetName,
        terms: $scope.facet.terms
      });

      // Transltion on UI is slow, do in here
      $scope.actives.forEach(function(term) {
        term.label = ValueTranslator.translate(term.term, $scope.facetName);

        if ($scope.facetName === 'projectId') {
          ProjectCache.getData().then(function(cache) {
            term.tooltip = cache[term.term];
          });
        } else  {
          term.tooltip = ValueTranslator.tooltip(term.term, $scope.facetName);
        }

      });

    }

    function setInactiveTerms() {
      $scope.inactives = Facets.getInactiveTerms({
        actives: $scope.actives,
        terms: $scope.facet.terms
      });

      // Transltion on UI is slow, do in here
      $scope.inactives.forEach(function(term) {
        term.label = ValueTranslator.translate(term.term, $scope.facetName);

        if ($scope.facetName === 'projectId') {
          ProjectCache.getData().then(function(cache) {
            term.tooltip = cache[term.term];
          });
        } else  {
          term.tooltip = ValueTranslator.tooltip(term.term, $scope.facetName);
        }
      });
    }

    function splitTerms() {
      setActiveTerms();
      setInactiveTerms();
    }

    function refresh() {
      if ($scope.facet) {
        splitTerms();
      }
    }

    $scope.displayLimit = 5;

    $scope.addTerm = function (term) {
      Facets.addTerm({
        type: $scope.type,
        facet: $scope.facetName,
        term: term
      });
    };

    $scope.removeTerm = function (term) {
      Facets.removeTerm({
        type: $scope.type,
        facet: $scope.facetName,
        term: term
      });
    };

    $scope.removeFacet = function () {
      Facets.removeFacet({
        type: $scope.type,
        facet: $scope.facetName
      });
    };

    $scope.bar = function (count) {
      return {width: (count / ($scope.facet.total + $scope.facet.missing) * 100) + '%'};
    };

    $scope.toggle = function() {
      $scope.expanded = !$scope.expanded;
      if (!$scope.collapsed) {
        $scope.displayLimit = $scope.expanded === true? $scope.inactives.length : 5;
      }
    };


    // $scope.projects = HighchartsService.projectColours;
    $scope.sites = HighchartsService.primarySiteColours;

    refresh();
    $scope.$watch('facet', refresh);
  });

  module.directive('terms', function () {
    return {
      restrict: 'E',
      scope: {
        facetName: '@',
        label: '@',
        capitalize: '@',
        hideCount: '=',
        hideText: '@',
        defined: '@',
        type: '@',
        facet: '=',
        collapsed: '@'
      },
      transclude: true,
      templateUrl: '/scripts/facets/views/terms.html',
      controller: 'termsCtrl'
    };
  });

  module.directive('activeTerm', function () {
    return {
      restrict: 'A',
      //require: '^terms',
      link: function (scope, element) {

        scope.mouseOver = function () {
          element.find('i').removeClass('icon-ok').addClass('icon-cancel');
          element.find('.t_facets__facet__terms__active__term__label__text span').css({textDecoration: 'line-through'});
        };

        scope.mouseLeave = function () {
          element.find('i').removeClass('icon-cancel').addClass('icon-ok');
          element.find('.t_facets__facet__terms__active__term__label__text span').css({textDecoration: 'none'});
        };
      }
    };
  });
})();
