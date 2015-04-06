(function() {

  'use strict';

  var Renderer = function(svg, config) {
    this.svg = svg;
    this.config = config;
    defineDefs(svg);
  };

  var strokeColor = '#696969';

  function defineDefs(svg){
    var defs = [
      {
        // An arrow head
        id:'Output',
        element:'path',
        attr:{
          d:'M0,-10L20,0L0,10L0,-10', 
          stroke:strokeColor
        },
        style:{
          fill:strokeColor
        },
        viewBox:  '-10 -10 30 20',
        refX: '5'
      },
      {
        // A smaller arrow head
        id:'FlowLine',
        element:'path',
        attr:{
          d:'M0,-5L-10,0L0,5',
          stroke:strokeColor
        },
        style:{
          fill:strokeColor
        },
        viewBox:  '-10 -5 15 10',
        refX: '-7'
      },
      {
        // A "hollow" arrow head
        id:'Activator',
        element:'path',
        attr:{
          d:'M0,-10L20,0L0,10L0,-10',
          stroke:strokeColor
        },
        style:{
          fill:'white'
        },
        viewBox:  '-10 -10 30 20',
        refX: '35'
      },
      {
        // A smaller "hollow" arrow head
        id:'RenderableInteraction',
        element:'path',
        attr:{
          d:'M0,-5L-10,0L0,5L0,-5',
          stroke:strokeColor
        },
        style:{
          fill:'white'
        },
        viewBox:  '-10 -5 15 10',
        refX: '-7'
      },
      {
        // A white circle with a border
        id:'Catalyst',
        element:'circle',
        attr:{
          'cx':15,
          'cy':0,
          'r':15
        },
        style:{
          fill:'white',
          stroke:strokeColor,
          'stroke-width':'2px'
        },
        viewBox:'0 -17 35 35',
        refX:'67'
      },
      {
        // Special right pointing arrow for genes
        id:'GeneArrow',
        element:'path',
        attr:{
          d:'M0,-8L17,0L0,8',
          stroke:'black'
        },
        style:{
          fill:'black'
        },
        viewBox:'0 -10 20 20',
        refX:'5'
      }
    ];

    for(var i=0;i<defs.length;i++){
      svg.append('svg:defs').append('svg:marker')
      .attr('id', defs[i].id)
      .attr('viewBox', defs[i].viewBox)
      .attr('refX', defs[i].refX)
      .attr('markerWidth', 8)
      .attr('markerHeight', 6)
      .attr('orient', 'auto')
      .append(defs[i].element)
      .attr(defs[i].attr).style(defs[i].style);
    }
    
    // Drop shadow
    var filter = svg.append('svg:defs').append("filter")
        .attr("id", "drop-shadow").attr("x","-90%").attr("y","-100%").attr("height","300%").attr("width","240%");

    filter.append("feGaussianBlur")
        .attr("in", "SourceAlpha")
        .attr("stdDeviation", 20)
        .attr("result", "blur");
    filter.append("feOffset")
        .attr("in", "blur")
        .attr("dx", 0)
        .attr("dy", 0)
        .attr("result", "offsetBlur");
    filter.append("feFlood")
        .attr("in", "offsetBlur")
        .attr("flood-color", "red")
        .attr("flood-opacity", "0.6")
        .attr("result", "offsetColor");
    filter.append("feComposite")
        .attr("in", "offsetColor")
        .attr("in2", "offsetBlur")
        .attr("operator", "in")
        .attr("result", "offsetBlur");

    var feMerge = filter.append("feMerge");

    feMerge.append("feMergeNode")
        .attr("in", "offsetBlur")
    feMerge.append("feMergeNode")
        .attr("in", "SourceGraphic");
  }

  /*
  * Renders the background compartments along with its specially position text
  */
  Renderer.prototype.renderCompartments = function (compartments) {
    this.svg.selectAll('.RenderableCompartment').data(compartments).enter().append('rect').attr({
      'class': function (d) {
        return d.type + ' compartment'+d.reactomeId;
      },
      'x': function (d) {
        return d.position.x;
      },
      'y': function (d) {
        return d.position.y;
      },
      'width': function (d) {
        return d.size.width;
      },
      'height': function (d) {
        return d.size.height;
      }
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

    svg.selectAll('.RenderableRect').data(rects).enter().append('rect').attr({
      'class': function (d) {
        return 'RenderableRect ' + d.type + ' entity'+d.id;
      },
      'x': function (d) {
        return d.position.x;
      },
      'y': function (d) {
        return d.position.y;
      },
      'width': function (d) {
        return d.size.width;
      },
      'height': function (d) {
        return d.size.height;
      },
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
      }
    }).on('mouseover', function () {
      d3.select(this).style("filter", 'url('+config.urlPath+'#drop-shadow)');
    }).on('mouseout', function () {
      d3.select(this).style("filter", '');
    }).on('click',function(d){config.onClick(d);});

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

    svg.selectAll('.RenderableOct').data(octs).enter().append('polygon')
      .attr({
        class: function(d){return 'RenderableOct RenderableComplex entity'+d.id;},
        points: function (d) {
          return getPointsMap(+d.position.x, +d.position.y, +d.size.width, +d.size.height, 4);
        },
        stroke: 'Red',
        'stroke-width': 1
      }).on('mouseover', function () {
        d3.select(this).style("filter", 'url('+config.urlPath+'#drop-shadow)');
      }).on('mouseout', function () {
        d3.select(this).style("filter", '');
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

  };

  /*
  * Renders all connecting edges and their arrow heads where appropriate 
  */
  Renderer.prototype.renderEdges = function (edges) {
    var svg = this.svg, config = this.config;
    var isStartMarker = function(type){return ['FlowLine','RenderableInteraction'].indexOf(type)>=0;};

    svg.selectAll('line').data(edges).enter().append('line').attr({
      'class':function(d){return 'RenderableStroke entity'+d.id+' '+d.type;},
      'x1':function(d){return d.x1;},
      'y1':function(d){return d.y1;},
      'x2':function(d){return d.x2;},
      'y2':function(d){return d.y2;},
      'stroke':strokeColor
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
  Renderer.prototype.renderReactionLabels = function (labels) {
    var size = 8, svg = this.svg;
    var circular = ['Association','Dissociation','Binding'];
    var filled = ['Association','Binding'];

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
    }).style('fill',function(d){return filled.indexOf(d.reactionType)>=0?strokeColor:'white';});

    svg.selectAll('.ReactionLabelText').data(labels).enter().append('text')
    .attr({
      'class':'ReactionLabelText',
      'x':function(d){return +d.x - (size/4);},
      'y':function(d){return +d.y + (size/4);},
      'font-weight':'bold',
      'font-size':'6px',
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
  *   the "value" of the node in a badge in the top right corner
  */
  Renderer.prototype.highlightEntity = function (nodes) {
    var svg = this.svg;
    
    nodes.forEach(function (node) {
      var svgNode = svg.selectAll('.entity'+node.id);
      if(svgNode[0].length <= 0){
        return;
      }
      svgNode.style('stroke','red');
      svgNode.style('stroke-width','3px');
      
      var value = Math.round(Math.random()*200 + 1);
      
      // generate a bunch of random numbers
      svg.append('rect')
        .attr({
          class:'value-banner',
          x: (+node.position.x)+(+node.size.width) - 10,
          y: (+node.position.y)- 7,
          width:(value.toString().length*5)+10,
          height:15,
          rx: 3,
          ry: 3
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
      }).text(value);
    });
  };

  dcc.Renderer = Renderer;

})();
