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
(function() {
  'use strict';

	var module = angular.module('icgc.visualization.stackedbar', []);
	
	module.directive('stacked', function ($location, HighchartsService) {
	    return {
		    restrict: 'E',
		    replace: true,
		    scope: {
		      items: '=',
          subtitle: '='
		    },
        template:'<div><div class="text-center graph_title">' +
                    'Top 20 Mutated Genes with High Functional Impact SSMs' +
                    '</div>'+
                    '<div class="stackedsubtitle text-center">{{subtitle | number}} ' +
                    'Unique SSM-Tested Donors</div></div>',
		    link: function ($scope, $element) {
                var chart;
                $scope.$watch('items', function (newValue) {
                  if (newValue && !chart && typeof $scope.items[0] !== 'undefined') {
                    var config = {
                        margin:{top: 5, right: 20, bottom: 50, left: 50},
                        height: 250,
                        width: 500,
                        colours: HighchartsService.projectColours,
                        yaxis:{label:'Donors Affected',ticks:4},
                        onClick: function(geneid){
                          $location.path('/genes/' + geneid).search({});
                          $scope.$apply();
                        }
                      };
                    chart = new dcc.StackedBarChart($scope.items,config);
                    chart.render($element[0]);
                  }
                }, true);
	
                $scope.$on('$destroy', function () {
                  chart.destroy();
                });
              }
      };
    });
})();
