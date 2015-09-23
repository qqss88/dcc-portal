(function() {

  'use strict';

  window.dcc = window.dcc || {};
  
  var defaultConfig =
  {
    width: 500,
    height: 500,
    onClick: {},
    urlPath: '',
    strokeColor: 'black',
    highlightColor: 'red',
    subPathwayColor: 'blue',
    initScaleFactor: 0.95
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
    var nodesInPathway = [];
    this.model = model;
    model.parse(xml);
    
    var getBoundingBox = function(nodes,box){
      nodes.forEach(function (node) {
        box.height = Math.max(node.position.y + node.size.height, box.height);
        box.width = Math.max(node.position.x + node.size.width, box.width);
        box.minHeight = Math.min(node.position.y, box.minHeight);
        box.minWidth = Math.min(node.position.x, box.minWidth);
      });
      return box;
    };

    var pathwayBox = getBoundingBox(model.getNodes(),{height:0,width:0,minHeight:10000,minWidth:10000});

    // Find out the size of the actual contents of the pathway so we can center it
    model.getNodes().forEach(function (node) {
      pathwayBox.height = Math.max(node.position.y + node.size.height, pathwayBox.height);
      pathwayBox.width = Math.max(node.position.x + node.size.width, pathwayBox.width);
      pathwayBox.minHeight = Math.min(node.position.y, pathwayBox.minHeight);
      pathwayBox.minWidth = Math.min(node.position.x, pathwayBox.minWidth);
    });

    // Calculate scale factor s, based on container size and size of contents
    var scaleFactor = Math.min(config.height / (pathwayBox.height - pathwayBox.minHeight),
                               config.width / (pathwayBox.width - pathwayBox.minWidth));
    
    // Set the zoom extents based on scale factor
    var zoom = d3.behavior.zoom().scaleExtent([scaleFactor*0.9, scaleFactor*17]);
    
    var svg = d3.select(config.container).append('svg')
      .attr('class', 'pathwaysvg pathway-no-scroll')
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
    scaleFactor = scaleFactor * config.initScaleFactor;
    var offsetX = (config.width - (pathwayBox.width - pathwayBox.minWidth) * scaleFactor) / 2;
    var offsetY = (config.height - (pathwayBox.height - pathwayBox.minHeight) * scaleFactor) / 2;

    zoom.scale(scaleFactor).translate([-pathwayBox.minWidth * scaleFactor + offsetX,
                                       -pathwayBox.minHeight * scaleFactor + offsetY]);
    svg.attr('transform', 'translate(' + [-pathwayBox.minWidth * scaleFactor + offsetX,
                                          -pathwayBox.minHeight * scaleFactor + offsetY] + ')'+
                          'scale(' + scaleFactor + ')');

    // So that the whole thing can be dragged around
    svg.append('rect').attr({
      'class': 'svg-invisible-backdrop',
      'x': 0,
      'y': 0,
      'width': pathwayBox.width,
      'height': pathwayBox.height,
    }).style('opacity', 0);

    // Reset view on double click
    d3.select('.pathwaysvg').on('dblclick', function () {
      zoom.scale(scaleFactor).translate([-pathwayBox.minWidth * scaleFactor + offsetX,
                               -pathwayBox.minHeight * scaleFactor + offsetY]);
      svg.transition().attr('transform',
                            'translate(' + [-pathwayBox.minWidth * scaleFactor + offsetX,
                                            -pathwayBox.minHeight * scaleFactor + offsetY] + ')'+
                            'scale(' + scaleFactor + ')');
    });

    // Render everything
    this.renderer = new dcc.Renderer(svg, {
      onClick: function (d) {
        d.isPartOfPathway = (nodesInPathway.length<=0 || nodesInPathway.indexOf(d.reactomeId) >= 0);
        config.onClick(d);
      },
      urlPath: config.urlPath,
      strokeColor: config.strokeColor,
      highlightColor: config.highlightColor,
      subPathwayColor: config.subPathwayColor
    });
    this.rendererUtils = new dcc.RendererUtils();
    
    this.renderer.renderCompartments(_.where(model.getNodes(),{type:'RenderableCompartment'}));
    this.renderer.renderEdges(this.rendererUtils.generateLines(model));
    this.renderer.renderNodes(_.filter(model.getNodes(),
                                       function(n){return n.type!=='RenderableCompartment';}));
    this.renderer.renderReactionLabels(this.rendererUtils.generateReactionLabels(model.getReactions()));

    // Zoom in on the elements of interest if there are any
    if(zoomedOnElements[0].length !== 0){
      
      var subPathwayReactions = _.filter(model.getReactions(),
                                                  function(n){return _.contains(zoomedOnElements,n.reactomeId);});
      var renderer = this.renderer;
      
      pathwayBox = {height:0,width:0,minHeight:10000,minWidth:10000};
      
      // Go through all reactions: add their nodes and zoom in on them
      subPathwayReactions.forEach(function (reaction) {
        // Add nodes in this pathway to list
        nodesInPathway = nodesInPathway.concat(model.getNodeIdsInReaction(reaction.reactomeId));
        // Outline in pink
        renderer.outlineSubPathway(svg,reaction.reactomeId);
        // Get box
        pathwayBox = getBoundingBox(model.getNodesInReaction(reaction),pathwayBox);
      });
 
      // Add some buffer to the zoomed in area
      pathwayBox.width += 50;
      pathwayBox.minWidth -= 50;

      // Recalcualte the scale factor and offset and the zoom and transition
      scaleFactor = Math.min(config.height / (pathwayBox.height - pathwayBox.minHeight),
                             config.width / (pathwayBox.width - pathwayBox.minWidth));
                             
      scaleFactor = scaleFactor * config.initScaleFactor;
      offsetX = (config.width - (pathwayBox.width - pathwayBox.minWidth) * scaleFactor) / 2;
      offsetY = (config.height - (pathwayBox.height - pathwayBox.minHeight) * scaleFactor) / 2;
      zoom.scale(scaleFactor).translate([-pathwayBox.minWidth * scaleFactor + offsetX,
                                         -pathwayBox.minHeight * scaleFactor + offsetY]);
      svg.transition().attr('transform', 'translate(' + [-pathwayBox.minWidth * scaleFactor + offsetX,
                                                         -pathwayBox.minHeight * scaleFactor + offsetY] + ')'+
                            'scale(' + scaleFactor + ')');
    }

    this.nodesInPathway = nodesInPathway;
  };
  
  /**
  * Renders a legend svg in pathway-legend div given a width and height
  * Assumes the existance of a div with the class 'pathway-legend-svg'.
  *
  * On the other hand, if it already rendered it, it will simply set the opacity
  * of this div to 1.
  */
  ReactomePathway.prototype.renderLegend = function (w,h) {
    d3.select('.pathway-legend-svg').remove();
    var config =  this.config;
    var rendererUtils = this.rendererUtils;
    
    var legendSvg = d3.select('.pathway-legend').append('svg')
      .attr('class','pathway-legend-svg')
      .attr('viewBox', '0 0 ' +w+ ' ' + h)
      .attr('preserveAspectRatio', 'xMidYMid')
      .append('g');
    
    var legendRenderer = new dcc.Renderer(legendSvg, {
      onClick: function(){},
      urlPath: config.urlPath,
      strokeColor: config.strokeColor,
      highlightColor: config.highlightColor
    });
    
    var nodes = rendererUtils.getLegendNodes(20,0,legendSvg);
    
    legendRenderer.renderNodes(nodes);
    legendRenderer.renderEdges(rendererUtils.getLegendLines(40,h*0.34,legendSvg));
    legendRenderer.renderReactionLabels(rendererUtils.getLegendLabels(25,h*0.64,legendSvg),true);
    legendRenderer.highlightEntity(
      [{id:'Mutated',value:99}],
      {getNodesByReactomeId:function (){return [nodes[nodes.length-1]];}
    });
    legendSvg.selectAll('.reaction-sub-example')
          .attr('stroke',config.subPathwayColor)
          .classed('pathway-sub-reaction-line',true);
    legendSvg.selectAll('.reaction-failed-example')
          .classed('failed-reaction',true);
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
    var nodesInPathway = this.nodesInPathway;
    rawHighlights.forEach(function (rh) {
      rh.dbIds.forEach(function (dbId) {

        // Only highlight it if it's part of the pathway we're zooming in on
        // And only hide parts of it we are zooming in on a pathway
        if((nodesInPathway.length === 0 || _.contains(nodesInPathway,dbId)) && rh.value >= 0){
          highlights.push({id:dbId,value:rh.value});
        }
      });
    });
    this.renderer.highlightEntity(highlights, this.model);
  };
  
  dcc.ReactomePathway = ReactomePathway;

})();
