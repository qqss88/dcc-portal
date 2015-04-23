(function() {

  'use strict';

  window.dcc = window.dcc || {};
  
  var defaultConfig =
  {
    width: 500,
    height: 500,
    onClick:{},
    urlPath: '',
    strokeColor: 'black',
    highlightColor: 'red',
    subPathwayColor: 'blue'
  };

  var ReactomePathway = function (config) {
    this.config = config || defaultConfig;
  };

  /*
  * Takes in an xml of the pathway diagram and a list of reactions to zoom in 
  * on and highlight. The color of the reactions is set with config.subPathwayColor
  *
  */
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
      urlPath: config.urlPath,
      strokeColor: config.strokeColor,
      highlightColor: config.highlightColor
    });
    var rendererUtils = new dcc.RendererUtils();

    this.renderer.renderCompartments(_.filter(model.getNodes().slice(),
                                              function(n){return n.type==='RenderableCompartment';}));
    this.renderer.renderEdges(rendererUtils.generateLines(model));
    this.renderer.renderNodes(_.filter(model.getNodes().slice(),
                                       function(n){return n.type!=='RenderableCompartment';}));
    this.renderer.renderReactionLabels(rendererUtils.generateReactionLabels(model.getReactions()));

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
          svg.selectAll('.reaction'+reaction.reactomeId).attr('stroke',config.subPathwayColor);
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
  };
  
  /**
  * Renders a legend svg in pathway-legend div given a width and height
  * Assumes the existance of a div with the class 'pathway-legend-svg'.
  *
  * On the other hand, if it already rendered it, it will simply set the opacity
  * of this div to 1.
  */
  ReactomePathway.prototype.renderLegend = function (w,h) {
    var legendSvg = d3.select('.pathway-legend-svg')[0][0];
    
    if(!legendSvg){
      var config =  this.config;
      var rendererUtils = new dcc.RendererUtils();
      legendSvg = d3.select('.pathway-legend').append('svg')
        .attr('class','pathway-legend-svg')
        .attr('viewBox', '0 0 ' +w+ ' ' + h)
        .attr('preserveAspectRatio', 'xMidYMid')
        .append('g');
      var legendrenderer = new dcc.Renderer(legendSvg, {
        onClick: {},
        urlPath: config.urlPath,
        strokeColor: config.strokeColor,
        highlightColor: config.highlightColor
      });
      var nodes = rendererUtils.getLegendNodes(20,0);
      legendrenderer.renderNodes(nodes);
      legendrenderer.renderEdges(rendererUtils.getLegendLines(40,h*0.35,legendSvg));
      legendrenderer.renderReactionLabels(rendererUtils.getLegendLabels(25,h*0.6,legendSvg),true);
      legendrenderer.highlightEntity(
        [{id:'Mutated',value:99}],
        {getNodesByReactomeId:function (){return [nodes[nodes.length-1]];}
      });
    }else{
      d3.select('.pathway-legend-svg').attr('opacity','1');
    }
  };

  /**
  *  Takes in raw highlight data of the form:
  *  [{dbIds:[123,124,125],value:10, uniprotId: X0000}...]
  *
  *  and transforms and renders with the form:
  *  [{id:123, value:10},{id:124, value:10},...]
  */
  ReactomePathway.prototype.highlight = function (rawHighlights) {
    var highlights = [];
    rawHighlights.forEach(function (rh) {
      rh.dbIds.forEach(function (dbId) {
        highlights.push({id:dbId,value:rh.value});
      });
    });
    this.renderer.highlightEntity(highlights, this.model);
  };
  
  dcc.ReactomePathway = ReactomePathway;

})();
