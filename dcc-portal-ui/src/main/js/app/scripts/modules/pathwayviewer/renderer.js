(function() {

  'use strict';

  var Renderer = function(svg, config) {
    this.svg = svg;
    this.config = config;
    defineDefs(svg);
  }

  var strokeColor = '#696969';

  function defineDefs(svg){
    var defs = [
      {
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
        refX: '42'
      },
      {
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
        id:'Catalyst',
        element:'circle',
        attr:{
          'cx':15,'cy':0,'r':15
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
  }

  Renderer.prototype.renderNodes = function (nodes) {
    var svg = this.svg, config = this.config;

    var octs = _.filter(nodes,function(n){return n.type === 'RenerableComplex';})
    var rects = _.filter(nodes,function(n){return n.type !== 'RenerableComplex';})

    svg.selectAll('.RenderableRect').data(rects).enter().append('rect').attr({
      'class': function (d) {
        return 'RenderableRect ' + d.type + ' entity'+d.reactomeId;
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
      'stroke-dasharray': function (d) {
        if (d.type === 'RenderableGene')
          return 0 + ' ' + (+d.size.width + 1) + ' ' + (+d.size.height + +d.size.width) + ' 0';
        else
          return '';
      }
    }).on('mouseover', function (e) {
      if (d3.select(this).attr('class').indexOf('RenderableCompartment') < 0) {
        d3.select(this).attr('fill-old', d3.select(this).style('fill')).style('fill', 'gray');
      }
    }).on('mouseout', function (e) {
      if (d3.select(this).attr('class').indexOf('RenderableCompartment') < 0) {
        d3.select(this).style('fill', d3.select(this).attr('fill-old'));
      }
    }).on('click',function(d){config.onClick(d);});

    var getPointsMap = function(x,y,w,h,a){
      var points = [{x:x+a,   y:y},
                    {x:x+w-a, y:y},
                    {x:x+w,   y:y+a},
                    {x:x+w,   y:y+h-a},
                    {x:x+w-a, y:y+h},
                    {x:x+a,   y:y+h},
                    {x:x,     y:y+h-a},
                    {x:x,     y:y+a}]
      var val = '';
      points.forEach(function (elem) {
        val= val+elem.x+','+elem.y+' ';
      });
      return val;
    }

    svg.selectAll('.RenderableOct').data(octs).enter().append('polygon')
      .attr({
        class: function(d){return 'RenderableOct RenderableComplex entity'+d.reactomeId;},
        points: function (d) {
          return getPointsMap(+d.position.x, +d.position.y, +d.size.width, +d.size.height, 3);
        },
        stroke: 'Red',
        'stroke-width': 1
      }).on('mouseover', function () {
        d3.select(this).attr('fill-old', d3.select(this).style('fill')).style('fill', 'gray');
      }).on('mouseout', function () {
        d3.select(this).style('fill', d3.select(this).attr('fill-old'));
      }).on('click',function(d){config.onClick(d);});

    svg.selectAll('.RenderableText').data(nodes).enter().append('foreignObject').attr({
        'class':function(d){return d.type+'Text RenderableText';},
        'x':function(d){return d.type==='RenderableCompartment'?d.text.position.x:d.position.x;},
        'y':function(d){return d.type==='RenderableCompartment'?d.text.position.y:d.position.y;},
        'width':function(d){return d.size.width;},
        'height':function(d){return d.size.height;},
        'pointer-events':'none',
        'fill':'none'
      }).append('xhtml:body')
      .attr('class',function(d){return d.type==='RenderableCompartment'?'RenderableCompartmentText':'RenderableNodeText'})
      .html(function(d){return '<table class="RenderableNodeTextCell"><tr><td valign="middle">'+d.text.content+'</td></tr></table>';});

  };

  Renderer.prototype.renderEdges = function (edges) {
    var svg = this.svg, config = this.config;
    var isStartMarker = function(type){return ['FlowLine','RenderableInteraction'].indexOf(type)>=0;}

    svg.selectAll('line').data(edges).enter().append('line').attr({
      'class':function(d){return 'RenderableStroke entity'+d.id},
      'x1':function(d){return d.x1;},
      'y1':function(d){return d.y1;},
      'x2':function(d){return d.x2;},
      'y2':function(d){return d.y2;},
    }).attr('stroke',strokeColor)//function(d){return d.color;})
      .style('marker-start',function(d){return d.isLast && isStartMarker(d.marker)?'url('+config.urlPath+'#'+d.marker+')':'';})
      .style('marker-end',function(d){return d.isLast && !isStartMarker(d.marker)?'url('+config.urlPath+'#'+d.marker+')':'';});
  };

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
    })
    .style('fill',function(d){return filled.indexOf(d.reactionType)>=0?strokeColor:'white'})

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

  Renderer.prototype.highlightEntity = function (ids) {
    var svg = this.svg;
    console.log('updating..');
    console.log(ids);
    ids.forEach(function (id) {
      var obj = svg.select('.entity'+id);
      var objType = obj[0][0] ? obj[0][0].nodeName : 'nothing';
        console.log('changing color');
      if(objType === 'line'){
        obj.attr('stroke-width','3px');
      }else{
        obj.style('fill','hotpink');
      }
    });
  };

  dcc.Renderer = Renderer;

})();
