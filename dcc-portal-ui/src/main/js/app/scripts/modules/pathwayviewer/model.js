var PathwayModel = function () {
  this.nodes = [];
  this.reactions = [];
  this.links = []
}

PathwayModel.prototype.parse = function (xml) {
  var model = this;

  // Parse all the nodes first
  var collection = $($.parseXML(xml)).find('Nodes')[0].children;

  for (var i = 0; i < collection.length; i++) {

    var bounds = collection[i].attributes['bounds'].nodeValue.split(' ');
    var textPosition = collection[i].attributes['textPosition'] ?
      collection[i].attributes['textPosition'].nodeValue.split(' ') :
      collection[i].attributes['bounds'].nodeValue.split(' ');

    this.nodes.push({
      position: {
        x: bounds[0],
        y: bounds[1]
      },
      size: {
        width: bounds[2],
        height: bounds[3]
      },
      type: collection[i].tagName.substring(collection[i].tagName.lastIndexOf('.') + 1),
      id: collection[i].attributes['id'].nodeValue,
      reactomeId: collection[i].attributes['reactomeId'] ? collection[i].attributes['reactomeId'].nodeValue : 'missing',
      text: {
        content: collection[i].textContent.trim(),
        position: {
          x: textPosition[0],
          y: textPosition[1]
        }
      },
      reactions: []
    });
  }

  // Parse all the reactions
  collection = $(xml).find('Edges')[0].children;

  for (var i = 0; i < collection.length; i++) {
    var points = collection[i].attributes['points'].nodeValue.split(',');

    var base = [], nodes=[], description;

    // Curated "base" line of the reaction
    for (var j = 0; j < points.length; j++) {
      var point = points[j].trim().split(' ');
      base.push({
        x: point[0],
        y: point[1]
      });
    }

    // Add nodes that are attached to reaction including their type
    for (var j = 0; j < $(collection[i].children).length; j++) {
      var name = $(collection[i].children[j]) ? $(collection[i].children[j].children[0])[0].localName : 'missing';

      if(name === 'displayname'){
        description = $(collection[i].children[j].children[0])[0].innerText;
      }

      if (['input', 'output', 'catalyst', 'activator', 'inhibitor'].indexOf(name) < 0) {
        continue;
      }
      var subReactions = Array.prototype.slice.call($(collection[i].children[j].children));

      // How a node connects is also a curated line, add that
      for (var k = 0; k < subReactions.length; k++) {
        var subBase = [];
        if (subReactions[k].getAttribute('points')) {
          subReactions[k].getAttribute('points').split(',').forEach(function (elem) {
            var point = elem.trim().split(' ');
            subBase.push({
              x: point[0],
              y: point[1]
            });
          });
        }

        nodes.push({
          type: name.substring(0,1).toUpperCase()+name.substring(1),
          base: subBase,
          id: subReactions[k].getAttribute('id')
        });
      }
    }

    this.reactions.push({
      base: base,
      nodes: nodes,
      reactomeId: collection[i].attributes['reactomeId'] ? collection[i].attributes['reactomeId'].nodeValue : 'missing',
      id: collection[i].attributes['id'].nodeValue,
      type: collection[i].attributes['reactionType'] ? collection[i].attributes['reactionType'].nodeValue : 'missing',
      description: description
    });
  }

  // Add list of reactions a node is involved in nodes array
  this.reactions.forEach(function (reaction) {
    reaction.nodes.forEach(function (reactionNode) {
      model.nodes.forEach(function (node) {
        if (node.id === reactionNode.id) node.reactions.push(reaction.id);
      });
    });
  });
};

PathwayModel.prototype.addReactionNodes = function () {
  var nodes = this.nodes;
  this.reactions.forEach(function (reaction) {
    nodes.push({
      position: {
        x: reaction.base[reaction.base.length > 2 ? reaction.base.length - 2 : 1].x,
        y: reaction.base[reaction.base.length > 2 ? reaction.base.length - 2 : 1].y
      },
      id: reaction.id,
      reactomeId: reaction.reactomeId,
      type: 'ReactionNode'
    });
    reaction.reactionNode = nodes[nodes.length - 1];
  });
};

PathwayModel.prototype.getNodeById = function (id) {
  var node;
  this.nodes.forEach(function (n) {
    if (n.id === id) node = n;
  });
  return node;
}

PathwayModel.prototype.getReactionById = function (id) {
  var reaction;
  this.reactions.forEach(function (r) {
    if (r.id === id) reaction = r;
  });
  return reaction;
}

PathwayModel.prototype.getNodes = function () {
  return this.nodes;
};

PathwayModel.prototype.getReactions = function () {
  return this.reactions;
};

/** UNUSED EXPERIMENTAL FEATURES TO SUPPORT FORCE LAYOUT */

PathwayModel.prototype.getLinks = function () {
  return this.links;
};

PathwayModel.prototype.addLineNodes = function () {
  var nodes = this.nodes,
    links = this.links,
    model = this;
  var addNode = function (x, y, id, reactomeId) {
    if(!x || !y) console.err("undefined x or y");
    nodes.push({
      position: {
        x: x,
        y: y
      },
      id: id,
      reactomeId: reactomeId,
      type: 'ReactionNode'
    });
  }

  var link = function (a, b) {
    links.push({
      source: a,
      target: b
    })
  }

  this.reactions.forEach(function (reaction) {

    console.log("Creating reaction of type '" + reaction.type + "': " +reaction.id);
    var baseStart, baseEnd, baseMid;

    for (var i = 0; i < reaction.base.length; i++) {
      addNode(reaction.base[i].x, reaction.base[i].y, reaction.id, reaction.reactomeId);
      if (i > 0) link(nodes[nodes.length - 1], nodes[nodes.length - 2]);

      if (i === 0) baseStart = nodes[nodes.length - 1];
      if (i === reaction.base.length - 1) baseEnd = nodes[nodes.length - 1];
      if (i === reaction.base.length - 2) baseMid = nodes[nodes.length - 1];
    }
    console.log("Added base start/end/mid: ");
    console.log(baseStart);
    console.log(baseEnd);
    console.log(baseMid);
    reaction.nodes.forEach(function (reactionNode) {
      console.log("Linking connected "+reactionNode.type+" node: "+reactionNode.id);
      var originNode = model.getNodeById(reactionNode.id);
      var reactionBase = reactionNode.base ? reactionNode.base.splice() : [];

      if (reactionBase.length <= 1) {
        console.log("Base of length of 1... linking base to node....");
        switch (reactionNode.type) {
        case 'Input':
          link(originNode, baseStart);
          break;
        case 'Output':
          link(originNode, baseEnd);
          break;
        default:
          link(originNode, baseMid);
        }
      } else {
        console.log("Base of length >1... diving deeper...");
        console.log(reactionBase);
        var reactionBaseStart, reactionBaseEnd;
        //either start at 1 or end at -1 IDK
        for (var i = 1; i < reactionBase.length; i++) {
          addNode(reactionBase[i].x, reactionBase[i].y, reaction.id, reaction.reactomeId);
          if (i > 1) link(nodes[nodes.length - 1], nodes[nodes.length - 2]);

          if (i === 1) reactionBaseStart = nodes[nodes.length - 1];
          if (i === reactionBase.length - 1) reactionBaseEnd = nodes[nodes.length - 1];
        }
        console.log("Linking sub base to node");
        console.log(originNode);
        console.log(reactionBaseStart);
        link(originNode, reactionBaseStart);
        console.log("Linking end of sub base to original base");
        switch (reactionNode.type) {
        case 'Input':
          link(reactionBaseEnd, baseStart);
          break;
        case 'Output':
          link(reactionBaseEnd, baseEnd);
          break;
        default:
          link(reactionBaseEnd, baseMid);
        }
      }

    });

  });
}
