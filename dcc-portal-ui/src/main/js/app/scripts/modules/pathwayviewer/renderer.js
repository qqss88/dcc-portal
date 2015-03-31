var Renderer = function(svg, config) {
  this.svg = svg;
  this.config = config;
  defineDefs(svg);
}

var strokeColor = '#1693c0';

function defineDefs(svg){
  var defs = [
    {
      id:'Output',
      element:'path',
      attr:{
        d:'M0,-7L14,0L0,7L0,-7',
        stroke:strokeColor
      },
      style:{
        fill:strokeColor
      },
      viewBox:  '-10 -5 25 10',
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
        d:'M0,-5L10,0L0,5L0,-5',
        stroke:strokeColor
      },
      style:{
        fill:'white'
      },
      viewBox:'0 -6 13 13',
      refX:'24'
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
        'cx':5,'cy':0,'r':5
      },
      style:{
        fill:'white',
        stroke:strokeColor,
        'stroke-width':'1.5px'
      },
      viewBox:'0 -6 13 13',
      refX:'5'
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
  var svg = this.svg;
  var config = this.config;
  var octs = [];
  var rects = nodes.slice();
  for (var i = 0; i < rects.length; i++) {
    if (rects[i].type === 'RenderableComplex') {
      octs.push(rects[i]);
      rects.splice(i, 1);
      i = i - 1;
    }
  }

  svg.selectAll('.RenderableRect').data(rects).enter().append('rect').attr({
    'class': function (d) {
      console.log(d.reactomeId);return"RenderableRect " + d.type + " entity"+d.reactomeId;
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
      d3.select(this).attr('fill-old', d3.select(this).style('fill')).style('fill', 'white');
    }
  }).on('mouseout', function (e) {
    if (d3.select(this).attr('class').indexOf('RenderableCompartment') < 0) {
      d3.select(this).style('fill', d3.select(this).attr('fill-old'));
    }
  }).on('click',function(d){config.onClick(d);});

  var getPointsMap = function(x,y,w,h,a){
    var points = [{x:+x+ +a,y:+y},
                 {x:+x+ +w- +a,y:+y},
                 {x:+x+ +w,y:+y+ +a},
                 {x:+x+ +w,y:+y+ +h- +a},
                 {x:+x+ +w- +a,y:+y+ +h},
                 {x:+x+ +a,y:+y+ +h},
                 {x:+x,y:+y+ +h- +a},
                 {x:+x,y:+y+ +a}]
    var val = "";
    points.forEach(function (elem) {
      val= val+elem.x+","+elem.y+" ";
    });
    return val;
  }

  svg.selectAll('.RenderableOct').data(octs).enter().append('polygon')
    .attr({
      class: function(d){return 'RenderableOct RenderableComplex entity'+d.reactomeId;},
      points: function (d) {
        return getPointsMap(d.position.x, d.position.y, d.size.width, d.size.height, 3);
      },
      stroke: 'Red',
      'stroke-width': 1
    }).on('mouseover', function () {
      d3.select(this).attr('fill-old', d3.select(this).style('fill')).style('fill', 'white');
    }).on('mouseout', function () {
      d3.select(this).style('fill', d3.select(this).attr('fill-old'));
    }).on('click',function(d){config.onClick(d);});

  svg.selectAll('.RenderableText').data(nodes).enter().append('foreignObject').attr({
      'class':function(d){return d.type+"Text RenderableText";},
      'x':function(d){return d.type==='RenderableCompartment'?d.text.position.x:d.position.x;},
      'y':function(d){return d.type==='RenderableCompartment'?d.text.position.y:d.position.y;},
      'width':function(d){return d.type==='RenderableCompartment'?'100%':d.size.width;},
      'height':function(d){return d.type==='RenderableCompartment'?'100%':d.size.height;},
      'pointer-events':'none',
      'fill':'none'
    }).append("xhtml:body")//.append('div')
    .attr('class',function(d){return d.type==='RenderableCompartment'?'':'RenderableNodeText'})
    .html(function(d){return "<div class='RenderableNodeTextCell'>"+d.text.content+"</div>";});

};

Renderer.prototype.renderEdges = function (edges) {
  var svg = this.svg;
  var isStartMarker = function(type){return ['FlowLine','RenderableInteraction'].indexOf(type)>=0;}

  svg.selectAll('line').data(edges).enter().append('line').attr({
    'class':function(d){return 'RenderableStroke entity'+d.id},
    'x1':function(d){return d.x1;},
    'y1':function(d){return d.y1;},
    'x2':function(d){return d.x2;},
    'y2':function(d){return d.y2;},
  }).attr('stroke',strokeColor)//function(d){return d.color;})
    .style('marker-start',function(d){return d.isLast && isStartMarker(d.marker)?'url(#'+d.marker+')':'';})
    .style('marker-end',function(d){return d.isLast && !isStartMarker(d.marker)?'url(#'+d.marker+')':'';});
};

Renderer.prototype.highlightEntity = function (ids) {
  var svg = this.svg;
console.log("updating..");
  console.log(ids);
  ids.forEach(function (id) {
    var obj = svg.select(".entity"+id);
    var objType = obj[0][0] ? obj[0][0].nodeName : 'nothing';
      console.log("changing color");
    if(objType === 'line'){
      obj.attr("stroke-width","3px");
    }else{
      obj.style("fill","hotpink");
    }
  });
};
