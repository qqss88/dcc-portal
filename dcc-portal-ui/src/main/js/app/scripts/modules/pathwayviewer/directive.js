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
        '<div class="pathway-info">'+
        '<i style="visibility:hidden" class="fa fa-times-circle pathway-info-controller"></i>'+
        '<h4>DETAILS</h4><div class="pathway-info-svg"></div><div class="pathway-info-content">'+
          '<table class="table pathway-gene-table" data-ng-if="geneList.length>0">'+
            '<tr>'+
                '<th class="pathway-gene-header-label pathway-gene-header">Gene</th>' +
                '<th class="pathway-gene-header-label pathway-gene-header">Mutations</th>' +
            '</tr>'+
            '<tr data-ng-repeat="gene in geneList">' +
              '<th class="pathway-gene-label">{{gene.id}}</th>' +
              '<th class="pathway-gene-label"><a href="/search/m?filters={{gene.advQuery}}">{{gene.value}}</a></th>' +
            '</tr></table>' +
            '<div data-ng-if="geneList.length==0">No associated genes</div>'+
        '</div></div>'+
        '</div>',
      link: function ($scope) {
        var showingLegend = false, showingInfo = false, rendered = false;
        var zoomedOn, xml, highlights;
        
        var openNewSideBar = function(isLegend,isInfo){
          if(showingLegend){
            $('.pathway-legend').animate({left: '100%'});
            $('.pathway-legend-controller').addClass('fa-question-circle').removeClass('fa-times-circle');
          }else if(showingInfo){
            $('.pathway-info').animate({left: '100%'});
            $('.pathway-info-controller').css('visibility','hidden');
            $('.pathway-legend-controller').css('visibility','visible');
          }
          
          showingLegend = isLegend;
          showingInfo = isInfo;
          
          if(isLegend){
            $('.pathway-legend').animate({'left': '75%'});
            $('.pathway-legend-controller').addClass('fa-times-circle').removeClass('fa-question-circle');
            showingLegend = true;
          }else if(showingInfo){
            $('.pathway-info-controller').css('visibility','visible');
            $('.pathway-legend-controller').css('visibility','hidden');
            $('.pathway-info').animate({left: '70%'});
            showingInfo = true;
          }
        };
        
        var infoSvg = d3.select('.pathway-info-svg').append('svg')
              .attr('viewBox', '0 0 ' +150+ ' ' +50)
              .attr('preserveAspectRatio', 'xMidYMid')
              .append('g');
        var infoRenderer = new dcc.Renderer(infoSvg, {onClick: function(){},highlightColor: '#9b315b'});

        var controller = new dcc.ReactomePathway({
          width: 500,
          height: 300,
          container: '#pathway-viewer-mini',
          onClick: function (d) {
            var padding = 7, displayedCount = '?';
            var node = $.extend({}, d);
            var geneList = [];
            
            if(!showingInfo){
              openNewSideBar(false,true);
            }
            
            // Create list of uniprot ids if we have any
            if(highlights && d.isPartOfPathway){
              highlights.forEach(function (highlight) {
                if(highlight.dbIds.indexOf(d.reactomeId) >= 0){
                  geneList.push({
                    id:'Uniprot:'+highlight.uniprotId,
                    value:highlight.value,
                    advQuery:highlight.advQuery
                  });
                }
              });
              if(geneList.length === 1){
                displayedCount = geneList[0].value;
              }
              $scope.geneList = _.sortBy(geneList,function(n){return -n.value;});
            }else{
              $scope.geneList = [];
            }
            $('.pathway-info-svg svg g').html('');
            node.size={width:100-padding*2,height:50-padding*2};
            node.position={x:padding+25,y:padding};
            
            infoRenderer.renderNodes([node]);
            if(geneList.length > 0){
              infoRenderer.highlightEntity([{id:d.reactomeId,value:displayedCount}],
                                           {getNodesByReactomeId:function (){return [node];}});
            }
          },
          urlPath: $location.url(),
          strokeColor: '#696969',
          highlightColor: '#9b315b',
          subPathwayColor: 'hotpink'
        });
        
        $('.pathway-legend-controller').on('click',function(){
          if(showingLegend){
            openNewSideBar(false,false);
          }else{
            openNewSideBar(true,false);
            var width = $('.pathway-legend').css('width');
            var height = $('.pathway-legend').css('height');
            
            // Create legend with width and height but with 'px' removed
            controller.renderLegend(width.substring(0,width.length-2),height.substring(0,height.length-2));
          }
        });

        $('.pathway-info-controller').on('click',function(){
          openNewSideBar(false,false);
        });
        
        var handleRender = function(){
          if(!xml || !zoomedOn){
            return;
          }else if(!rendered){
            controller.render(xml,zoomedOn);
            rendered = true;
          }else{
            openNewSideBar(false,false);
          }
          
          if(highlights){
            highlights.forEach(function (h) {
              if(!(h.dbIds instanceof Array)){
                h.dbIds = h.dbIds.split(',');
              }
            });
            controller.highlight(highlights);
          }
        };
        
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