(function() {

  'use strict';

  window.dcc = window.dcc || {};

  var RendererUtils = function (){};

  /*
  * Create an array of reaction labels for every reaction based on its type
  */
  RendererUtils.prototype.generateReactionLabels = function (reactions) {
    var labels = [];
    reactions.forEach(function (reaction) {
      var hasBase = false;
      reaction.nodes.forEach(function (node) {
        hasBase =  hasBase || (node.base && node.base.length > 0);
      });
      if(hasBase){
        labels.push({
          x:reaction.center.x,
          y:reaction.center.y,
          reactionType:reaction.type
        });
      }
    });
    return labels;
  };

  /*
  * Goes through the model's reactions and creates a large arrays of all lines
  *  based on the human-curated list of points.
  */
  RendererUtils.prototype.generateLines = function (model) {
    var lines = [];
    var reactions = model.getReactions();

    // Make sure arrow heads aren't added to special dashed lines
    var isArrowHeadLine = function(type){
      return ['entitysetandmemberlink','entitysetandentitysetlink'].indexOf(type) < 0;
    };

    // Adds a line to the lines array gives an array of points and description of the line
    var generateLine = function (points, color, type, id, lineType) {
      for (var j = 0; j < points.length - 1; j++) {
        lines.push({
          x1: points[j].x,
          y1: points[j].y,
          x2: points[j+1].x,
          y2: points[j+1].y,
          marked: j === points.length-2 && isArrowHeadLine(lineType),
          marker: type,
          color: color, // For debugging, every line type has a color
          id:id,
          type: lineType
        });
      }
    };

    // Gets the center of node with its position and size
    var getNodeCenter = function(nodeId){
      var node = model.getNodeById(nodeId);
      // Genes are special because they are not boxes.. just an arrow
      if(node.type === 'RenderableGene'){
        return {x: (+node.position.x) + (+node.size.width) + 5,y:node.position.y};
      }
      return { x: ((+node.position.x) + (+node.size.width/2)),
               y: ((+node.position.y) + (+node.size.height/2))};
    };

    // Gets the first input node in a reaction (used when the reaction
    //  has no human-curated node lines)
    var getFirstInputNode =  function(nodes){
      var node;
      nodes.forEach(function (n) {
        if(n.type === 'Input'){
          node = n;
        }
      });
      return node;
    };

    // Generate a line based on the type of reaction & node using human-curated points
    var getNodeLines = function (reaction, node, reactionId,reactionClass) {
      var count = {inputs:0,outputs:0};
      if(!node.base || node.base.length === 0){
        return 'missing';
      }
      var base =  node.base.slice();
      
      switch (node.type) {
      case 'Input':
        base.push(reaction.base[0]);
        base[0] = getNodeCenter(node.id);
        generateLine(base, 'red', 'Input',reactionId,reactionClass);
        count.inputs = count.inputs + 1;
        break;
      case 'Output':
        base.push(reaction.center);
        base.reverse(); // Make sure output points at the output
        generateLine(base, 'green', 'Output',reactionId,reactionClass);
        count.outputs = count.outputs + 1;
        break;
      case 'Activator':
        base.push(reaction.center);
        base[0] = getNodeCenter(node.id);
        generateLine(base, 'blue', 'Activator',reactionId,reactionClass);
        break;
      case 'Catalyst':
        base.push(reaction.center);
        base[0] = getNodeCenter(node.id);
        generateLine(base, 'purple', 'Catalyst',reactionId,reactionClass);
        break;
      case 'Inhibitor':
        base.push(reaction.center);
        base[0] = getNodeCenter(node.id);
        generateLine(base, 'orange', 'Inhibitor',reactionId,reactionClass);
        break;
      }

      return node.type;
    };

    reactions.forEach(function (reaction) {
      var id = reaction.reactomeId;
      var inputs = 0, outputs = 0;

      for(var i=0; i<reaction.nodes.length;i++){
        var type = getNodeLines(reaction,reaction.nodes[i],id);
        if(type === 'Input'){
          inputs ++;
        }else if(type === 'Output'){
          outputs++;
        }
      }
      // If it doesn't have human-curated input lines, "snap" line to first input node, if it has one
      if(inputs === 0 && getFirstInputNode(reaction.nodes)){
        reaction.base[0] = getNodeCenter(getFirstInputNode(reaction.nodes).id);
      }
      var baseLine = reaction.base.slice();
      if(outputs !== 0){
        baseLine.pop(); // It's duplicated
      }
      
      // This creates a base reaction line
      generateLine(baseLine,
                   outputs === 0 ?'hotpink':'black',
                   outputs === 0 ?'Output':reaction.type,id,reaction.class);
    });

    return lines;
  };
  
  /*
  * Create a grid of all components for legend
  */
  RendererUtils.prototype.getLegendNodes =  function(marginLeft,marginTop){
    var nodes = [];
    var x = marginLeft, y= marginTop;
    var types = ['Complex','Protein','EntitySet','Chemical','Compartment','ProcessNode','Mutated'];
    for(var i=0;i<types.length;i++){
      x = i%2===0?marginLeft:marginLeft+100+10;
      y = Math.floor(i/2)*40 + marginTop + 10*Math.floor(i/2);
      nodes.push({
        position:{x:x,y:y},
        size:{width:90,height:30},
        type:types[i]==='ProcessNode'?types[i]:'Renderable'+types[i],
        id:types[i]==='Mutated'?'mutated':'fake',
        reactomeId:'fake',
        text:{content:types[i],position:{x:x,y:y}}
      });
    }
    
    return nodes;
  };
  
  /*
  * Create a list of reaction lines for legend
  */
  RendererUtils.prototype.getLegendLines = function (marginLeft,marginTop,svg) {
    var lines = [];
    var y=marginTop;
    var markers = ['Output','Catalyst','Activator','Link'];
    markers.forEach(function (elem) {
      lines.push({
        x1: marginLeft,
        y1:y,
        x2: marginLeft+80,
        y2: y,
        marked: true,
        marker: elem+'-legend',
        color: 'black',
        id:'fake',
        type: elem==='Link'?'entitysetandmemberlink':'fake'
      });
      svg.append('foreignObject').attr({
        x: marginLeft+80,
        y:y-15,
        width:90,
        height:30,
        'fill':'none'
      }).append('xhtml:body')
      .attr('class','RenderableNodeText')
      .html('<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
          elem+'</td></tr></table>');
    
      y+=30;
    });

    return lines;
  };
  
   /*
  * Create a list of reaction lines for legend
  */
  RendererUtils.prototype.getLegendLabels = function (marginLeft,marginTop,svg) {
    var labels = [];
    var y=marginTop;
    var reactions = ['Association','Dissociation','Transition','Omitted Process','Uncertain'];
    reactions.forEach(function (elem) {
      labels.push({
        x: marginLeft+40,
        y:y,
        reactionType: elem
      });
      svg.append('foreignObject').attr({
        x: marginLeft+80,
        y:y-15,
        width:110,
        height:30,
        'fill':'none'
      }).append('xhtml:body')
      .attr('class','RenderableNodeText')
      .html('<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
          (elem==='Association'?'Association/Binding':elem)+'</td></tr></table>');
    
      y+=30;
    });

    return labels;
  };

  dcc.RendererUtils = RendererUtils;

})();
