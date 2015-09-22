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
        '<i class="fa fa-expand pathway-fullscreen-controller"></i>' +
        '<div class="pathway-legend"><i class="fa fa-question-circle pathway-legend-controller"></i>'+
        '<h4>LEGEND</h4></div>'+
        '<div class="pathway-info">'+
        '<i style="visibility:hidden" class="fa fa-chevron-circle-right pathway-info-controller"></i>'+
        '<h4>DETAILS</h4><div>{{entityType}}</div><div class="pathway-info-svg"></div>'+
          '<div class="pathway-info-content">'+
          '<table class="table pathway-gene-table" data-ng-if="geneList.length>0">'+
            '<tr>'+
                '<th class="pathway-gene-header-label pathway-gene-header">Gene</th>' +
                '<th class="pathway-gene-header-label pathway-gene-header"># ICGC Mutations</th>' +
            '</tr>'+
            '<tr data-ng-repeat="gene in geneList">' +
              '<th class="pathway-gene-label"><a href="/genes/{{gene.id}}">{{gene.symbol}}</a></th>' +
              '<th class="pathway-gene-label"><a href="/search/m?filters={{gene.advQuery}}">{{gene.value}}</a></th>' +
            '</tr></table>' +
        '</div></div>'+
        '</div>',
      link: function ($scope, element) {
        var showingLegend = false,  rendered = false;
        var zoomedOn, xml, highlights;
        
        var scrollTimer;
        
        element.bind("mouseenter", function() {
          scrollTimer = setTimeout(function() {
            $('.pathwaysvg').attr('class', 'pathwaysvg');
          }, 500);
        });
        
        element.bind("mouseleave", function() {
          clearTimeout(scrollTimer);
          $('.pathwaysvg').attr('class', 'pathwaysvg pathway-no-scroll');
        });
            
        var typeMap = {
          'RenderableComplex': 'Complex',
          'RenderableProtein': 'Protein',
          'RenderableEntitySet': 'EntitySet',
          'RenderableChemical': 'Chemical',
          'RenderableCompartment': 'Compartment',
          'ProcessNode': 'ProcessNode',
          'RenderableMutated Gene(s)': 'Mutated Gene(s)'
        };
        
        var showLegend = function(){
          $('.pathway-legend').animate({'left': '75%'});
          $('.pathway-legend-controller').addClass('fa-chevron-circle-right').removeClass('fa-question-circle');
          showingLegend = true;
        };
        
        var showInfo = function(){
          $('.pathway-info-controller').css('visibility','visible');
          $('.pathway-legend-controller').css('visibility','hidden');
          $('.pathway-info').animate({left: '70%'});
        };
        
        var hideLegend = function(){
          $('.pathway-legend').animate({left: '100%'});
          $('.pathway-legend-controller').addClass('fa-question-circle').removeClass('fa-chevron-circle-right');
          showingLegend = false;
        };
        
        var hideInfo = function(){
          $('.pathway-info').animate({left: '100%'});
          $('.pathway-info-controller').css('visibility','hidden');
          $('.pathway-legend-controller').css('visibility','visible');
        };
        
        var renderinfo = function(node,mutationCount,isMutated){
          $('.pathway-info-svg').html('');
          
          var padding = 7;
          var infoSvg = d3.select('.pathway-info-svg').append('svg')
              .attr('viewBox', '0 0 ' +150+ ' ' +50)
              .attr('preserveAspectRatio', 'xMidYMid')
              .append('g');
          var infoRenderer = new dcc.Renderer(infoSvg, {onClick: function(){},highlightColor: '#9b315b', strokeColor: '#696969'});
          
          node.size={width:100-padding*2,height:50-padding*2};
          node.position={x:padding+25,y:padding};
          infoRenderer.renderNodes([node]);
          
          if(isMutated){
            infoRenderer.highlightEntity([{id:node.reactomeId,value:mutationCount}],
                                         {getNodesByReactomeId:function (){return [node];}});
          }
        };

        var controller = new dcc.ReactomePathway({
          width: 500,
          height: 300,
          container: '#pathway-viewer-mini',
          onClick: function (d) {
            var mutationCount = '*';
            var node = $.extend({}, d);
            var geneList = [];

            // Reset data
            $scope.geneList = [];
            $scope.entityType = typeMap[d.type];
            
            hideLegend();
            showInfo();
            
            // Create list of uniprot ids if we have any
            if(highlights && node.isPartOfPathway){
              highlights.forEach(function (highlight) {
                
                if(_.contains(highlight.dbIds,d.reactomeId)){
                  
                  if(!highlight.advQuery){
                    return;
                  }
                  
                  geneList.push({
                    symbol:highlight.geneSymbol,
                    id:highlight.geneId,
                    value:highlight.value,
                    advQuery:highlight.advQuery
                  });
                }
              });
              
              if(geneList.length === 1){
                mutationCount = geneList[0].value;
              }
              
              $scope.geneList = _.sortBy(geneList,function(n){return -n.value;});
            }
            
            renderinfo(node,mutationCount,geneList.length>0);
          },
          urlPath: $location.url(),
          strokeColor: '#696969',
          highlightColor: '#9b315b',
          initScaleFactor: 0.95,
          subPathwayColor: 'navy'
        });
        
        // Render legend last to ensure all dependancies are initialized. Timeout of 0 does not work in firefox.
        setTimeout(function() {
          var rect = $('.pathway-legend')[0].getBoundingClientRect();
          controller.renderLegend(rect.width,rect.height);
        }, 500);
        
        $('.pathway-legend-controller').on('click', function(){
          if(showingLegend){
            hideLegend();
          }else{
            showLegend();
          }
        });

        $('.pathway-info-controller').on('click',function(){
          hideInfo();
        });
        
        var requestFullScreen = function(element) {
          if (element.requestFullscreen) {
            element.requestFullscreen();
          } else if (element.mozRequestFullScreen) {
            element.mozRequestFullScreen();
          } else if (element.webkitRequestFullScreen) {
            element.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
          }
        };
        
        var exitFullScreen = function() {
          if (document.exitFullscreen) {
              document.exitFullscreen();
            } else if (document.mozCancelFullScreen) {
              document.mozCancelFullScreen();
            } else if (document.webkitExitFullscreen) {
              document.webkitExitFullscreen();
            }
        };
        
        var fullScreenHandler = function() {
          if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
            $('.pathway-fullscreen-controller').removeClass('fa-compress');
            $('.pathway-fullscreen-controller').addClass('fa-expand');
          } else {
            $('.pathway-fullscreen-controller').removeClass('fa-expand');
            $('.pathway-fullscreen-controller').addClass('fa-compress');
          }
        };
        
        $('.pathway-fullscreen-controller').on('click', function() {       
          if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
            requestFullScreen(document.getElementById('pathway-viewer-mini'));
          } else {
            exitFullScreen();
          }
        });
        
        if (document.addEventListener){
            document.addEventListener('webkitfullscreenchange', fullScreenHandler);
            document.addEventListener('mozfullscreenchange', fullScreenHandler);
            document.addEventListener('fullscreenchange', fullScreenHandler);
        }
        
        var handleRender = function(){
          if(!xml || !zoomedOn){
            return;
          }else if(!rendered){
            controller.render(xml,zoomedOn);
            rendered = true;
          }else{
            hideInfo();
            hideLegend();
          }
          
          if(highlights){
            controller.highlight(highlights);
          }
        };
        
        $scope.$watch('items', function (newValue) {
          xml = newValue;
          handleRender();
        });
        
        $scope.$watch('zooms', function (newValue) {
          zoomedOn = newValue;
          handleRender();
        });

        $scope.$watch('highlights', function (newValue) {
          highlights = newValue;
          handleRender();
        });
        
        $scope.$on('$destroy', function () {
          element.unbind();
          
          document.removeEventListener('webkitfullscreenchange', fullScreenHandler);
          document.removeEventListener('mozfullscreenchange', fullScreenHandler);
          document.removeEventListener('fullscreenchange', fullScreenHandler);
        });
      }
    };
  });
})(jQuery);