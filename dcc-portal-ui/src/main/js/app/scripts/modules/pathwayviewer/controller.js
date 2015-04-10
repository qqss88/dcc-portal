(function() {

  'use strict';

  window.dcc = window.dcc || {};

  var ReactomePathway = function (config) {
    this.config = config;
  };

  ReactomePathway.prototype.render = function (xml, zoomedOnElements) {
    var config = this.config;
    var model = new dcc.PathwayModel();
    this.model = model;
    model.parse(xml);

    var height = 0,
      width = 0,
      minHeight = 10000,
      minWidth = 10000;

    // Find out the size of the actual contents of the pathway so we can center it
    model.getNodes().forEach(function (node) {
      height = Math.max((+node.position.y) + (+node.size.height), height);
      width = Math.max((+node.position.x) + (+node.size.width), width);
      minHeight = Math.min(node.position.y, minHeight);
      minWidth = Math.min(node.position.x, minWidth);
    });

    // Calculate scale factor s, based on container size and size of contents
    var s = Math.min(config.height / (height - minHeight), config.width / (width - minWidth));
    
    // Set the zoom extents based on scale factor
    var zoom = d3.behavior.zoom().scaleExtent([s*0.9, s*17]);
    
    var svg = d3.select(config.container).append('svg')
      .attr('class', 'pathwaysvg')
      .attr('viewBox', '0 0 ' + config.width + ' ' + config.height)
      .attr('preserveAspectRatio', 'xMidYMid')
      .append('g')
      .call(zoom)
      .on('dblclick.zoom', null) // Make double click reset instead of zoom a level
      .append('g');
    
    zoom.on('zoom', function () {
      svg.attr('transform', 'translate(' + d3.event.translate + ')scale(' + d3.event.scale + ')');
    });

    // Set initial positioning and zoom out a little
    s = s * 0.95;
    var offsetX = (config.width - (width - minWidth) * s) / 2;
    var offsetY = (config.height - (height - minHeight) * s) / 2;

    zoom.scale(s).translate([-minWidth * s + offsetX, -minHeight * s + offsetY]);
    svg.attr('transform', 'translate(' + [-minWidth * s + offsetX, -minHeight * s + offsetY] + ')scale(' + s + ')');

    // So that the whole thing can be dragged around
    svg.append('rect').attr({
      'class': 'svg-invisible-backdrop',
      'x': 0,
      'y': 0,
      'width': width,
      'height': height,
    }).style('opacity', 0);

    // Reset view on double click
    d3.select('.pathwaysvg').on('dblclick', function () {
      zoom.scale(s).translate([-minWidth * s + offsetX, -minHeight * s + offsetY]);
      svg.transition().attr('transform', 'translate(' +
                            [-minWidth * s + offsetX, -minHeight * s + offsetY] + ')scale(' + s + ')');
    });

    // Render everything
    this.renderer = new dcc.Renderer(svg, {
      onClick: function (d) {
        config.onClick(d);
      },
      urlPath: config.urlPath
    });
    var rendererUtils = new dcc.RendererUtils();

    this.renderer.renderCompartments(_.filter(model.getNodes().slice(),
                                              function(n){return n.type==='RenderableCompartment';}));
    this.renderer.renderEdges(rendererUtils.generateLines(model));
    this.renderer.renderNodes(_.filter(model.getNodes().slice(),
                                       function(n){return n.type!=='RenderableCompartment';}));
    this.renderer.renderReactionLabels(rendererUtils.generateReactionLabels(model.getReactions()));
    
    
//    this.renderer.highlightEntity(_.filter(model.getNodes().slice(),
//                                           function(n){
//      return    (n.type==='RenderableProtein'||
//                 n.type==='RenderableEntity'||
//                 n.type==='RenderableComplex'||
//                 n.type==='RenderableEntitySet');
//    }));
    
    // Zoom in on the elements on interest if there are any
    if(zoomedOnElements[0].length !== 0){
      
      //Reset size calculations
      height = 0;
      width = 0;
      minHeight = 10000;
      minWidth = 10000;
      
      // For all zoomed in elements, go through their positions/size and form the zoomed in size
      _.filter(model.getReactions().slice(),function(n){return zoomedOnElements.indexOf(n.reactomeId)>=0;})
        .forEach(function (reaction) {
        reaction.nodes.forEach(function (node) {
          var modelNode = model.getNodeById(node.id);
          height = Math.max((+modelNode.position.y) + (+modelNode.size.height), height);
          width = Math.max((+modelNode.position.x) + (+modelNode.size.width), width);
          minHeight = Math.min(modelNode.position.y, minHeight);
          minWidth = Math.min(modelNode.position.x, minWidth);
        });
      });

      // Add some buffer to the zoomed in area
      width = width + 50;
      minWidth = minWidth - 50;

      // Recalcualte the scale factor and offset and the zoom and transition
      s = Math.min(this.config.height / (height - minHeight), this.config.width / (width - minWidth));
      offsetX = (this.config.width - (width - minWidth) * s) / 2;
      offsetY = (this.config.height - (height - minHeight) * s) / 2;
      zoom.scale(s).translate([-minWidth * s + offsetX, -minHeight * s + offsetY]);
      svg.transition().attr('transform', 'translate(' +
                            [-minWidth * s + offsetX, -minHeight * s + offsetY] + ')scale(' + s + ')');
    }
    
    var showingLegend = false;
    var legendSvg;
    var renderLegend = function(w, h){
      if(!legendSvg){
        legendSvg = d3.select('.pathway-legend').append('svg')
          .attr('viewBox', '0 0 ' +w+ ' ' + h)
          .attr('preserveAspectRatio', 'xMidYMid')
          .append('g');
        var legendrenderer = new dcc.Renderer(legendSvg, {
          onClick: function (d) {},
          urlPath: config.urlPath
        });
        legendrenderer.renderNodes(rendererUtils.getLegendNodes(20,0));
      }else{
        legendSvg.attr('opacity','1');
      }
    }
    
    d3.select('.pathway-legend-controller').on('click',function(){
      if(showingLegend){       
        legendSvg.transition().duration(600).attr('opacity','0');
        $('.pathway-legend').animate({left: '100%',},600);
        showingLegend = false;
      }else{  
        renderLegend($('.pathway-legend').css('width').substring(0,$('.pathway-legend').css('width').length-2),
                    $('.pathway-legend').css('height').substring(0,$('.pathway-legend').css('height').length-2));
        
        $('.pathway-legend').animate({left: '75%'});
        showingLegend = true;
      }
    });
  };

  ReactomePathway.prototype.highlight = function (ids) {
    this.renderer.highlightEntity(_.filter(this.model.getNodes().slice(),
                                           function(n){return ids.indexOf(n.reactomeId)>=0;}));
  };
  
  dcc.ReactomePathway = ReactomePathway;

})();
