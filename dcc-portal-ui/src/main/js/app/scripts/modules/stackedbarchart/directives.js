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
        title: '@',
        subtitle: '@'
      },
      template:'<div><div class="text-center graph_title">{{title}}</div>'+
        '<div class="stackedsubtitle text-center">{{subtitle}} </div></div>',
      link: function ($scope, $element) {
        var chart, config;

        config = {
          margin:{top: 5, right: 20, bottom: 50, left: 50},
          height: 250,
          width: 500,
          colours: HighchartsService.primarySiteColours,
          yaxis:{label:'Donors Affected',ticks:4},
          onClick: function(geneid){
            $scope.$emit('tooltip::hide');
            $location.path('/genes/' + geneid).search({});
            $scope.$apply();
          },
          tooltipShowFunc: function(elem, d) {
            function getLabel() {
              return '<strong>'+d.label+'</strong><br>'+(d.y1-d.y0)+' Donors Affected';
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
          },
          transform: function(data) {
	         // For each gene, create an array of donors and get the total affected donors count
            var copy = _.cloneDeep(data);
	         copy.forEach(function(d) {
	           var y0 = 0;
	           d.stack = d.uiFIProjects
	           .sort(function(a,b){return a.count-b.count;}) //sort so biggest is on top
	           .map(function(p) {
                return {
                  name: p.id,
                  y0: y0,
                  y1: y0 += p.count,
                  key: d.symbol,
                  link: d.id,
                  label: p.name,
                  primarySite: p.primarySite
                };
              });
	           d.total = d.stack[d.stack.length - 1].y1;
	         });
            return copy;
          }
        };

        $scope.$watch('items', function (newValue) {
          if (newValue && typeof $scope.items[0] !== 'undefined') {
            if (!chart) {
              chart = new dcc.StackedBarChart(config);
            }
            chart.render($element[0], config.transform($scope.items));
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
