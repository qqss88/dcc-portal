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
  var module = angular.module('icgc.visualization.stackedbar', []);

  function ensureArray (a) {
    return _.isArray (a) ? a : [];
  }

  module.directive('stacked', function ($location, HighchartsService, $window) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        items: '=',
        alternateBrightness: '=',
        title: '@',
        subtitle: '@',
        yLabel: '@'
      },
      template: '<div><div class="text-center graph_title"> {{title}} </div>' +
        '<div class="stackedsubtitle text-center"> {{subtitle}} </div></div>',
      link: function ($scope, $element) {
        var isInitialized = false;
        var chart, config;

        config = {
          margin: {
             top: 5, right: 20, bottom: 50, left: 50
          },
          height: 250,
          width: 500,
          colours: HighchartsService.primarySiteColours,
          alternateBrightness: $scope.alternateBrightness === true? true : false,
          yaxis: {
            label: $scope.yLabel,
            ticks: 4
          },
          onClick: function(link){
            $scope.$emit('tooltip::hide');
            $location.path(link).search({});
            $scope.$apply();
          },
          tooltipShowFunc: function(elem, d) {
            function getLabel() {
              return '<strong>'+d.label+'</strong><br>'+(d.y1-d.y0) + ' ' + $scope.yLabel;
            }

            var position = {
              left:elem.getBoundingClientRect().left,
              top:elem.getBoundingClientRect().top + $window.pageYOffset,
              width: elem.getBoundingClientRect().width,
              height: elem.getBoundingClientRect().height
            };

            $scope.$emit('tooltip::show', {
              element: angular.element(elem),
              text: getLabel(),
              placement: 'right',
              elementPosition: position
            });
          },
          tooltipHideFunc: function() {
            $scope.$emit('tooltip::hide');
          }
        };

        $scope.$watch('items', function (newValue) {
          if (! chart) {
            chart = new dcc.StackedBarChart (config);
          }

          if (_.isEmpty (ensureArray (newValue))) {
            if (chart && isInitialized) {
              // FIXME: this displaysNoResultMessage() probably shouldn't be a method for the stackedbar class.
              // But due to some dependencies and time, this should suffice for now.
              // Will refactor this along with other improvements I wanted to make.
              chart.displaysNoResultMessage ($element[0]);
            }
            return;
          }

          // FIXME: Again, this is a band-aid solution for now.
          isInitialized = true;

          // Adaptive margin based on char length of labels
          var max = _.max (
            _.pluck (newValue, 'key').map (_.property ('length')));

          if (max >= 10) {
            config.margin.bottom += 25;
          }

          chart.render ($element[0], newValue);
        }, true);

        $scope.$on('$destroy', function () {
          if (chart) {
            chart.destroy();
          }
        });

      }
    };
  });
})();
