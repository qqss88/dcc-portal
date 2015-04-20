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
(function($) {
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
      template:'<div id="pathway-viewer-mini" class="pathwayviewercontainer text-center">'+
        '<div class="pathway-legend"><i class="fa fa-question-circle pathway-legend-controller"></i>'+
        '<h4>LEGEND</h4></div>'+
        '<div class="pathway-info"><i style="visibility:hidden" class="fa fa-times-circle pathway-info-controller"></i>'+
        '<h4>DETAILS</h4><div class="pathway-info-svg"></div><div class="pathway-info-content"></div></div>'+
        '</div>',
      link: function ($scope,_ctrl) {
        var showingLegend = false;
        var showingInfo = false;
        var rendered = false;
        
        var openNewSideBar = function(isLegend,isInfo){
          if(showingLegend){
            $('.pathway-legend').animate({left: '100%'});
          }else if(showingInfo){
            $('.pathway-info').animate({left: '100%'});
            $('.pathway-info-controller').css('visibility','hidden');
            $('.pathway-legend-controller').css('visibility','visible');
          }
          
          showingLegend = isLegend;
          showingInfo = isInfo;
          
          if(isLegend){
            $('.pathway-legend').animate({'left': '75%'});
            showingLegend = true;
          }else if(showingInfo){
            $('.pathway-info-controller').css('visibility','visible');
            $('.pathway-legend-controller').css('visibility','hidden');
            $('.pathway-info').animate({left: '70%'});
            showingInfo = true;
          }
        }
        
        var infoSvg = d3.select('.pathway-info-svg').append('svg')
              .attr('viewBox', '0 0 ' +100+ ' ' +50)
              .attr('preserveAspectRatio', 'xMidYMid')
              .append('g');
        var infoRenderer = new dcc.Renderer(infoSvg, {onClick: {},urlPath: ''});

        var controller = new dcc.ReactomePathway({
          width: 500,
          height: 300,
          container: '#pathway-viewer-mini',
          onClick: function (d,thing) {
            var padding = 3;
            var node = $.extend({}, d);
            if(!showingInfo){
              openNewSideBar(false,true);
            }
            $('.pathway-info-content').html(JSON.stringify(d,null,4).replace(/},/g,'},<br/>'));
            $('.pathway-info-svg svg g').html('');
            node.size={width:100-padding*2,height:50-padding*2};
            node.position={x:3,y:3};
            infoRenderer.renderNodes([node]);
          },
          urlPath: $location.path()
        });
        
        $('.pathway-legend-controller').on('click',function(){
          if(showingLegend){
            openNewSideBar(false,false);
          }else{
            openNewSideBar(true,false);
            var width = $('.pathway-legend').css('width');
            var height = $('.pathway-legend').css('height');
            controller.renderLegend(width.substring(0,width.length-2),height.substring(0,height.length-2));
          }
        });

        $('.pathway-info-controller').on('click',function(){
          openNewSideBar(false,false);
        });
        
        var zoomedOn, xml, highlights;
        
        var handleRender = function(){
          if(!xml || !zoomedOn){
            return;
          }else if(!rendered){
            controller.render(xml,zoomedOn);
            rendered = true;
          }
          
          if(highlights){
            controller.highlight(highlights);
          }
        }
        
        $scope.$watch('items', function (newValue) {
          xml = newValue;
          handleRender();
        }, true);
        
        $scope.$watch('zooms', function (newValue) {
          zoomedOn = newValue;
          handleRender();
        }, true);

        $scope.$watch('highlights', function (newValue) {
            highlights = newValue;
            handleRender();
        },true);
      }
    };
  });
})(jQuery);