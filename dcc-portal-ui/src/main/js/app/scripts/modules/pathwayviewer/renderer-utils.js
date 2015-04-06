(function() {

  'use strict';

  window.dcc = window.dcc || {};

  var RendererUtils = function (){};

  RendererUtils.prototype.unshiftCompartments = function (nodes) {
    for (var i = 0; i < nodes.length; i++) {
      if (nodes[i].type === 'RenderableCompartment') {
        nodes.unshift(nodes[i]);
        nodes.splice(i + 1, 1);
      }
    }
    return nodes;
  };

  RendererUtils.prototype.generateReactionLabels = function (reactions) {
    var labels = [];
    reactions.forEach(function (reaction) {
      var hasBase = false;
      reaction.nodes.forEach(function (node) {
        hasBase =  hasBase || (node.base && node.base.length > 0);
      });
      if(hasBase){
        labels.push({
          x:reaction.base[1].x,
          y:reaction.base[1].y,
          reactionType:reaction.type
        });
      }
    });
    return labels;
  };

  RendererUtils.prototype.generateLines = function (model) {
    var lines = [];
    var reactions = model.getReactions();

    var isLegalLineType = function(type){
      return ['entitysetandmemberlink','entitysetandentitysetlink'].indexOf(type) < 0;
    };

    var generateLine = function (points, color, type, id, lineType) {
      for (var j = 0; j < points.length - 1; j++) {
        lines.push({
          x1: points[j].x,
          y1: points[j].y,
          x2: points[j+1].x,
          y2: points[j+1].y,
          marked: j === points.length-2 && isLegalLineType(lineType),
          marker: type,
          color: color,
          id:id,
          type: lineType
        });
      }
    };

    var getNodeCenter = function(nodeId){
      var node = model.getNodeById(nodeId);
      return { x: ((+node.position.x) + (+node.size.width/2)),
               y: ((+node.position.y) + (+node.size.height/2))};
    };

    var getFirstInputNode =  function(nodes){
      var node;
      nodes.forEach(function (n) {
        if(n.type === 'Input'){
          node = n;
        }
      });
      return node;
    };

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
          base.push(reaction.base[(reaction.base.length - 1)]);
          base.reverse();
          generateLine(base, 'green', 'Output',reactionId,reactionClass);
          count.outputs = count.outputs + 1;
          break;
        case 'Activator':
          base.push(reaction.base[1]);
          base[0] = getNodeCenter(node.id);
          generateLine(base, 'blue', 'Activator',reactionId,reactionClass);
          break;
        case 'Catalyst':
          base.push(reaction.base[1]);
          base[0] = getNodeCenter(node.id);
          generateLine(base, 'purple', 'Catalyst',reactionId,reactionClass);
          break;
        case 'Inhibitor':
          base.push(reaction.base[1]);
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
      if(inputs === 0){
        reaction.base[0] = getNodeCenter(getFirstInputNode(reaction.nodes).id);
      }
      generateLine(reaction.base,
                   outputs===0?'hotpink':'black',
                   outputs === 0 ?'Output':reaction.type,id,reaction.class);
    });

    return lines;
  };

  dcc.RendererUtils = RendererUtils;

})();
