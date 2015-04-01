var RendererUtils = function () {

}

RendererUtils.prototype.unshiftCompartments = function (nodes) {
  for (var i = 0; i < nodes.length; i++) {
    if (nodes[i].type === 'RenderableCompartment') {
      nodes.unshift(nodes[i]);
      nodes.splice(i + 1, 1);
    }
  }
  return nodes;
}

RendererUtils.prototype.generateReactionLabels = function (reactions) {
  var labels = [];
  reactions.forEach(function (reaction) {
    var hasBase = false;
    reaction.nodes.forEach(function (node) {
      hasBase =  hasBase || (node.base && node.base.length > 0)
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
}

RendererUtils.prototype.generateLines = function (model) {
  var lines = [];
  var reactions = model.getReactions();

  var generateLine = function (points, color, type, id) {
    for (var j = 0; j < points.length - 1; j++) {
      lines.push({
        x1: points[j].x,
        y1: points[j].y,
        x2: points[j+1].x,
        y2: points[j+1].y,
        isLast: j === points.length-2,
        marker: type,
        color: color,
        id:id
      });
    }
  };

  var getNodeCenter = function(nodeId){
    var node = model.getNodeById(nodeId);
    return { x: (+node.position.x + +node.size.width/2),
             y: (+node.position.y + +node.size.height/2)};
  };

  var getFirstInputNode =  function(nodes){
    var node;
    nodes.forEach(function (n) {
      if(n.type === 'Input'){
        node = n;
      }
    });
    return node;
  }

  for (var i = 0; i < reactions.length; i++) {
    var outputs = 0, inputs=0;
    var id = reactions[i].reactomeId;

    reactions[i].nodes.forEach(function (node) {
      if(!node.base || node.base.length === 0) return;
      var base =  node.base.slice();
      switch (node.type) {
        case 'Input':
          base.push(reactions[i].base[0]);
          base[0] = getNodeCenter(node.id);
          generateLine(base, 'red', 'Input',id);
          inputs = inputs + 1;
          break;
        case 'Output':
          base.push(reactions[i].base[(reactions[i].base.length - 1)]);
          base.reverse();
          generateLine(base, 'green', 'Output',id);
          outputs =  outputs +1;
          break;
        case 'Activator':
          base.push(reactions[i].base[1]);
          base[0] = getNodeCenter(node.id);
          generateLine(base, 'blue', 'Activator',id);
          break;
        case 'Catalyst':
          base.push(reactions[i].base[1]);
          base[0] = getNodeCenter(node.id);
          generateLine(base, 'purple', 'Catalyst',id);
          break;
        case 'Inhibitor':
          base.push(reactions[i].base[1]);
          generateLine(base, 'orange', 'Inhibitor',id);
          break;
        }

    });
    if(inputs === 0){
      reactions[i].base[0] = getNodeCenter(getFirstInputNode(reactions[i].nodes).id);
    }
    generateLine(reactions[i].base,outputs==0?'black':'black',outputs==0?'Output':reactions[i].type,id);
  }

  return lines;
}
