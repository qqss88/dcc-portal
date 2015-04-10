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

  var module = angular.module('icgc.pathwayviewer', []);

  module.directive('pathwayViewer', function ($location) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        items: '=',
        highlights: '=',
        zooms: '='
      },
      template:'<div id="pathway-viewer-mini" class="pathwayviewercontainer text-center">'
                + '<div class="pathway-legend"><i class="fa fa-question-circle pathway-legend-controller"></i>'
                + '<h4>LEGEND</h4></div></div>',
      link: function ($scope) {

        var controller = new dcc.ReactomePathway({
          width: 500,
          height: 300,
          container: '#pathway-viewer-mini',
          onClick: function (d) {
            console.log(d);
          },
          urlPath: $location.path()
        });
        var zoomedOn = [];
        var items = '';

        $scope.$watch('items', function (newValue) {
          if(newValue && zoomedOn.length > 0){
            controller.render(newValue,zoomedOn);
          }else if(newValue){
            items = newValue;
          }
        }, true);
        
        $scope.$watch('zooms', function (newValue) {
          if(newValue && items.length > 0){
            controller.render(items,newValue);
          }else if(newValue){
            zoomedOn = newValue;
          }
        }, true);

        $scope.$watch('highlights', function (newValue) {
          if(newValue){
            console.log('new value!!');
            controller.highlight(newValue);
          }
        },true);

      }
    };
  });
})();
