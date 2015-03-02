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

	var module = angular.module('icgc.visualization.stackedarea', []);

	module.directive('donorHistory', function ($location, HighchartsService, $window) {
	    return {
		    restrict: 'E',
		    replace: true,
		    scope: {
		      items: '=',
          subtitle: '='
		    },
        template:'<div class="text-center graph_title">' +
                    'History of Life' +
                    '</div>',
		    link: function ($scope, $element) {
                var chart;
                $scope.$watch('items', function (newValue) {
                  if (newValue && !chart && typeof $scope.items[0] !== 'undefined') {
                    var config = {
                        margin:{top: 40, right: 40, bottom: 40, left: 40},
                        height: 600,
                        width: 1000,
                        colours: HighchartsService.projectColours,
                        yaxis:{label:'Donors',ticks:8},
                        onClick: function(project){
                          $scope.$emit('tooltip::hide');
                          $location.path('/projects/' + project).search({});
                          $scope.$apply();
                        },
                        tooltipShowFunc: function(elem, project, currentDonors) {
                          function getLabel() {
                            return '<strong>'+project+'</strong><br># of donors: '+currentDonors;
                          }

                          $scope.$emit('tooltip::show', {
                            element: angular.element(elem),
                            text: getLabel(),
                            placement: 'right',
                            sticky:true
                          });
                        },
                        tooltipHideFunc: function() {
                          $scope.$emit('tooltip::hide');
                        }
                      };
                    chart = new dcc.StackedAreaChart($scope.items,config);
                    chart.render($element[0]);
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
