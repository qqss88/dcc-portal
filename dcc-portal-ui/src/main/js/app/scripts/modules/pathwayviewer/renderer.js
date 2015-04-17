(function() {

  'use strict';
  
  var defaultConfig =
      {
        onClick:{},
        urlPath: ""
      };

  var Renderer = function(svg, config) {
    this.svg = svg;
    this.config = config;
    defineDefs(svg);
  };

  var strokeColor = '#696969';

  function defineDefs(svg){
    var markers = ['Output','Activator','ProcessNode','RenderableInteraction','GeneArrow','Catalyst',
                  'Catalyst-legend','Activator-legend','Output-legend'];
    var filled = function(type){return ['Output','RenderableInteraction','Output-legend','GeneArrow'].indexOf(type)>=0;};
    var circular = function(type){return ['Catalyst','Catalyst-legend'].indexOf(type)>=0;};
    var shifted = function(type){return ['Catalyst','Activator'].indexOf(type)>=0;};
//    var arrowed = function(type){
//      return ['Output','Activator','ProcessNode','RenderableInteraction','GeneArrow'].indexOf(type)>=0;
//    };
    
    var circle = {
      'element':'circle',
      'attr':{
        'cx':10,
        'cy':0,
        'r':10,
        'stroke-width':'2px',
        'markerWidth':'8',
        'markerHeight':'8'
      },
      'viewBox':'0 -14 26 26',
      'refX':'20'
    };
    
    var arrow = {
      'element':'path',
      'attr':{
        d:'M0,-5L10,0L0,5L0,-5',
        'stroke-width':'1px',
        markerWidth:'8',
        markerHeight:'8'
      },
      refX: '10',
      viewBox:'0 -6 12 11'
    };
   
    markers.forEach(function (elem) {
      var def = circular(elem)?circle:arrow;
      svg.append('svg:defs').append('svg:marker')
      .attr({
        'id': elem,
        'viewBox': def.viewBox,
        'refX': (+def.refX)*(shifted(elem)?1.5:1),
        'markerHeight':def.attr.markerHeight,
        'markerWidth':def.attr.markerWidth,
        'orient':'auto'
      }).append(def.element)
      .attr(def.attr)
      .attr('stroke',strokeColor)
      .style('fill',filled(elem)?strokeColor:'white');
    });
  }

  /*
  * Renders the background compartments along with its specially position text
  */
  Renderer.prototype.renderCompartments = function (compartments) {
    this.svg.selectAll('.RenderableCompartment').data(compartments).enter().append('rect').attr({
      'class': function (d) {
        return d.type + ' compartment'+d.reactomeId;
      },
      'x': function (d) {return d.position.x;},
      'y': function (d) {return d.position.y;},
      'width': function (d) {return d.size.width;},
      'height': function (d) {return d.size.height;},
      rx: 3,
      ry: 3
    });

    this.svg.selectAll('.RenderableCompartmentText').data(compartments).enter().append('foreignObject').attr({
        'class':function(d){return d.type+'Text RenderableCompartmentText';},
        'x':function(d){return d.text.position.x;},
        'y':function(d){return d.text.position.y;},
        'width':function(d){return d.size.width;},
        'height':function(d){return d.size.height;},
        'pointer-events':'none',
        'fill':'none'
      }).append('xhtml:body')
      .attr('class','RenderableCompartmentText')
      .html(function(d){
        return '<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
          d.text.content+'</td></tr></table>';
      });
  };

  /*
  * Render all the nodes and their text
  */
  Renderer.prototype.renderNodes = function (nodes) {
    var svg = this.svg, config = this.config;
    // Split into normal rectangles and octagons based on node type
    var octs = _.filter(nodes,function(n){return n.type === 'RenderableComplex';});
    var rects = _.filter(nodes,function(n){return n.type !== 'RenderableComplex';});

    // Create a point map for the octagons
    var getPointsMap = function(x,y,w,h,a){
      var points = [{x:x+a,   y:y},
                    {x:x+w-a, y:y},
                    {x:x+w,   y:y+a},
                    {x:x+w,   y:y+h-a},
                    {x:x+w-a, y:y+h},
                    {x:x+a,   y:y+h},
                    {x:x,     y:y+h-a},
                    {x:x,     y:y+a}];
      var val = '';
      points.forEach(function (elem) {
        val= val+elem.x+','+elem.y+' ';
      });
      return val;
    };

    // Render all complexes as octagons
    svg.selectAll('.RenderableOct').data(octs).enter().append('polygon')
      .attr({
        class: function(d){return 'RenderableOct RenderableComplex entity'+d.id;},
        points: function (d) {
          return getPointsMap(+d.position.x, +d.position.y, +d.size.width, +d.size.height, 4);
        },
        stroke: 'Red',
        'stroke-width': 1
      }).on('mouseover', function (d) {
        d.oldColor = d3.rgb(d3.select(this).style('fill'));
        d3.select(this).style('fill', d.oldColor.brighter(0.25));
      }).on('mouseout', function (d) {
        d3.select(this).style('fill', d.oldColor);
      }).on('click',function(d){config.onClick(d);});

    // Render all other normal rectangular nodes after octagons
    svg.selectAll('.RenderableRect').data(rects).enter().append('rect').attr({
      'class': function (d) {return 'RenderableRect ' + d.type + ' entity'+d.id;},
      'x': function (d) {return d.position.x;},
      'y': function (d) {return d.position.y;},
      'width': function (d) {return d.size.width;},
      'height': function (d) {return d.size.height;},
      'rx': function (d) {
        switch (d.type) {
        case 'RenderableGene':
        case 'RenderableEntitySet':
        case 'RenderableEntity':
          return 0;
        case 'RenderableChemical':
          return d.size.width / 2;
        default:
          return 3;
        }
      },
      'ry': function (d) {
        switch (d.type) {
        case 'RenderableGene':
        case 'RenderableEntitySet':
        case 'RenderableEntity':
          return 0;
        case 'RenderableChemical':
          return d.size.width / 2;
        default:
          return 3;
        }
      },
      'stroke-dasharray': function (d) { //Gene has border on bottom and right side
        if (d.type === 'RenderableGene'){
          return 0 + ' ' + ((+d.size.width) + 1) + ' ' + ((+d.size.height) + (+d.size.width)) + ' 0';
        }else{
          return '';
        }
      },
      'pointer-events':function(d){return d.type==='RenderableGene'?'none':'';}
    }).on('mouseover', function (d) {
      d.oldColor = d3.rgb(d3.select(this).style('fill'));
      d3.select(this).style('fill', d.oldColor.brighter(0.25));
    }).on('mouseout', function (d) {
      d3.select(this).style('fill', d.oldColor);
    }).on('click',function(d){config.onClick(d);});
    
    // Add a foreignObject to contain all text so that warpping is done for us
    svg.selectAll('.RenderableText').data(nodes).enter().append('foreignObject').attr({
        'class':function(d){return d.type+'Text RenderableText';},
        'x':function(d){return d.position.x;},
        'y':function(d){return d.position.y;},
        'width':function(d){return d.size.width;},
        'height':function(d){return d.size.height;},
        'pointer-events':'none',
        'fill':'none'
      }).append('xhtml:body')
      .attr('class','RenderableNodeText')
      .html(function(d){
        return '<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
          d.text.content+'</td></tr></table>';
      });
    
    // if it's a gene, we have to add a sepcial array in the top right corner
    var genes =  _.filter(nodes,function(n){return n.type === 'RenderableGene';});

    svg.selectAll('.RenderableGeneArrow').data(genes).enter().append('line').attr({
      'class':'RenderableGeneArrow',
      'x1':function(d){return (+d.position.x)+(+d.size.width) - 0.5;},
      'y1':function(d){return (+d.position.y) +1;},
      'x2':function(d){return (+d.position.x)+(+d.size.width)  + 3.5;},
      'y2':function(d){return (+d.position.y) + 1;},
    }).attr('stroke','black')
      .style('marker-end','url('+config.urlPath+'#GeneArrow)');
    
  };

  /*
  * Renders all connecting edges and their arrow heads where appropriate 
  */
  Renderer.prototype.renderEdges = function (edges) {
    var svg = this.svg, config = this.config;
    var isStartMarker = function(type){return ['FlowLine','RenderableInteraction'].indexOf(type)>=0;};

    svg.selectAll('line').data(edges).enter().append('line').attr({
      'class':function(d){return 'RenderableStroke reaction'+d.id+' '+d.type;},
      'x1':function(d){return d.x1;},
      'y1':function(d){return d.y1;},
      'x2':function(d){return d.x2;},
      'y2':function(d){return d.y2;},
      'stroke':strokeColor//function(d){return d.color;}
    }).style({
      'marker-start':function(d){
        return d.marked && isStartMarker(d.marker)?
          'url('+config.urlPath+'#'+d.marker+')':'';
      },
      'marker-end':function(d){
        return d.marked && !isStartMarker(d.marker)?
          'url('+config.urlPath+'#'+d.marker+')':'';
      }
    });
  };

  /*
  * Render a label in the middle of the line to indicate the type
  */
  Renderer.prototype.renderReactionLabels = function (labels, legend) {
    var size = 7, svg = this.svg;
    var circular = ['Association','Dissociation','Binding'];
    var filled = ['Association','Binding'];

    // Add lines for legend
    if(legend){
      svg.selectAll('.pathway-legend-line').data(labels).enter().append('line').attr({
        'class':'pathway-legend-line',
        'x1':function(d){return (+d.x)-30;},
        'y1':function(d){return d.y;},
        'x2':function(d){return (+d.x)+30;},
        'y2':function(d){return d.y;},
        'stroke':strokeColor
      });
    }
    
    svg.selectAll('.RenderableReactionLabel').data(labels).enter().append('rect')
    .attr({
      'class':'RenderableReactionLabel',
      'x':function(d){return +d.x - (size/2);},
      'y':function(d){return +d.y - (size/2);},
      'rx':function(d){return circular.indexOf(d.reactionType)>=0?(size/2):'';},
      'ry':function(d){return circular.indexOf(d.reactionType)>=0?(size/2):'';},
      'width':size,
      'height':size,
      'stroke':strokeColor
    }).style('fill',function(d){return filled.indexOf(d.reactionType)>=0?strokeColor:'white';})
      .on('mouseover',function(d){
        console.log(d.description);
      });

    svg.selectAll('.ReactionLabelText').data(labels).enter().append('text')
    .attr({
      'class':'ReactionLabelText',
      'x':function(d){return +d.x - (size/4);},
      'y':function(d){return +d.y + (size/4);},
      'font-weight':'bold',
      'font-size':'5px',
      'fill':strokeColor
    }).text(function(d){
      if(d.reactionType === 'Omitted Process'){
        return '\\\\';
      }else if(d.reactionType === 'Uncertain'){
        return '?';
      }else{
        return '';
      }
    });
  };

  /*
  * Highlights the given list of nodes with a red border and puts
  *   the 'value' of the node in a badge in the top right corner
  * 
  * Highlight: { id, value }
  *
  */
  Renderer.prototype.highlightEntity = function (highlights, model) {
    var svg = this.svg;
    
    highlights.forEach(function (highlight) {
      var nodes = model.getNodesByReactomeId(highlight.id);
      nodes.forEach(function (node) {
        var svgNode = svg.selectAll('.entity'+node.id);
      
        if(svgNode[0].length <= 0){
          return;
        }
        svgNode.style('stroke','red');
        svgNode.style('stroke-width','3px');

        svg.append('rect')
          .attr({
            class:'value-banner',
            x: (+node.position.x)+(+node.size.width) - 10,
            y: (+node.position.y)- 7,
            width:(highlight.value.toString().length*5)+10,
            height:15,
            rx: 7,
            ry: 7,
          }).style({
            fill:'red'
          });

        svg.append('text').attr({
          'class':'banner-text',
          'x':(+node.position.x)+(+node.size.width) - 5,
          'y':(+node.position.y)+4,
          'pointer-events':'none',
          'font-size':'9px',
          'font-weight':'bold',
          'fill':'white'
        }).text(highlight.value);
      });
    });
  };

  dcc.Renderer = Renderer;

})();
