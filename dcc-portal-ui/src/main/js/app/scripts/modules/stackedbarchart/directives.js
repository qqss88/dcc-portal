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

  module.directive('stacked', function ($location, HighchartsService, $window) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        items: '=',
        isLoading: '=',
        alternateBrightness: '=',
        title: '@',
        subtitle: '@',
        yLabel: '@'
      },
      // TODO: Move this into a template file.
      template: '<div><div class="text-center graph_title">{{ title }}</div>' +
        '<div ng-show="showPlot" class="stackedsubtitle text-center">{{ subtitle }}</div>' +
        '<div ng-show="! showPlot" class="text-center" style="line-height: {{ defaultGraphHeight }}px;">' +
        '<strong>No mutations reported for the selected projects.</strong></div>' +
        '<div>' +
        '<div ng-show="isLoadingData" style="line-height: {{ defaultGraphHeight }}px; z-index: 1; position: absolute; margin-left: {{ defaultGraphWidth/2 + leftMargin}}px;">' +
        '<i class="icon-spinner icon-spin"></i></div>' +
        '<div ng-show="showPlot && ! isLoadingData" class="canvas"></div></div></div>',
      link: function ($scope, $element) {
        $scope.showPlot = false;
        $scope.defaultGraphHeight = 250;
        $scope.defaultGraphWidth = 500;
        $scope.leftMargin = 50;

        var chart;
        var config = {
          margin: {
             top: 5, right: 20, bottom: 50, left: $scope.leftMargin
          },
          height: $scope.defaultGraphHeight,
          width: $scope.defaultGraphWidth,
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

        $scope.isLoadingData = false;

        $scope.$watch ('isLoading', function (newValue) {
          $scope.isLoadingData = newValue;
          $scope.showPlot = newValue;
        });

        var svgMountPoint = _.first ($element.find ('.canvas'));

        function shouldShowPlot (data) {
          return ! _.isEmpty (data);
        }

        $scope.$watch ('items', function (newValue) {
          var showPlot = shouldShowPlot (newValue);
          $scope.showPlot = showPlot;
          if (! showPlot) {return;}

          if (newValue && typeof $scope.items[0] !== 'undefined') {
            if (!chart) {
              chart = new dcc.StackedBarChart(config);
            }

            // Adaptive margin based on char length of labels
            var max = _.max(_.pluck( $scope.items, 'key').map(function(d) { return d.length; }));
            if (max >= 10) {
              config.margin.bottom += 25;
            }

            chart.render (svgMountPoint, $scope.items);
          }
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
