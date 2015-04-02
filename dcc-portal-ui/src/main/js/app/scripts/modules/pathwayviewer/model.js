(function($) {

  'use strict';

  window.dcc = window.dcc || {};

  var PathwayModel = function () {
    this.nodes = [];
    this.reactions = [];
    this.links = [];
  };

  PathwayModel.prototype.parse = function (xml) {
    // Parse all the nodes first
    var collection = $($.parseXML(xml)).find('Nodes')[0].children;

    for (var i = 0; i < collection.length; i++) {

      var bounds = collection[i].attributes.bounds.nodeValue.split(' ');
      var textPosition = collection[i].attributes.textPosition ?
        collection[i].attributes.textPosition.nodeValue.split(' ') :
        collection[i].attributes.bounds.nodeValue.split(' ');

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
        id: collection[i].attributes.id.nodeValue,
        reactomeId: collection[i].attributes.reactomeId ?
          collection[i].attributes.reactomeId.nodeValue : 'missing',
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


    collection = $(xml).find('Edges')[0].children;

    var getPointsArray = function(pointString){
      var points = [];
      pointString.split(',').forEach(function (p) {
        var point = p.trim().split(' ');
        points.push({
          x:point[0],
          y:point[1]
        });
      });
      return points;
    };

    // Parse all the reactions
    var reactions =  this.reactions;
    $(collection).each(function(){
      var base = getPointsArray(this.attributes.points.nodeValue);
      var nodes=[];
      var displayName = $(this).find('displayName');

      $(this).find('input,output,catalyst,activator,inhibitor').each(function(){
        nodes.push({
            type: this.localName.substring(0,1).toUpperCase()+this.localName.substring(1),
            base: this.getAttribute('points') ?
              getPointsArray(this.getAttribute('points')) : [],
            id: this.id
          });
      });

      reactions.push({
        base: base,
        nodes: nodes,
        reactomeId: this.attributes.reactomeId ? this.attributes.reactomeId.nodeValue : 'missing',
        id: this.attributes.id.nodeValue,
        type: this.attributes.reactionType ? this.attributes.reactionType.nodeValue : 'missing',
        description: displayName ? displayName.innerText : 'no details'
      });
    });
  };

  PathwayModel.prototype.getNodeById = function (id) {
    var node;
    this.nodes.forEach(function (n) {
      if (n.id === id){
        node = n;
      }
    });
    return node;
  };

  PathwayModel.prototype.getReactionById = function (id) {
    var reaction;
    this.reactions.forEach(function (r) {
      if (r.id === id){
        reaction = r;
      }
    });
    return reaction;
  };

  PathwayModel.prototype.getNodes = function () {
    return this.nodes;
  };

  PathwayModel.prototype.getReactions = function () {
    return this.reactions;
  };

  PathwayModel.prototype.addReactionsToNodes = function() {
    var nodes = this.nodes;
    this.reactions.forEach(function (reaction) {
      reaction.nodes.forEach(function (reactionNode) {
        nodes.forEach(function (node) {
          if (node.id === reactionNode.id){
            node.reactions.push(reaction.id);
          }
        });
      });
    });
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
      if(!x || !y){
        console.err('undefined x or y');
      }
      nodes.push({
        position: {
          x: x,
          y: y
        },
        id: id,
        reactomeId: reactomeId,
        type: 'ReactionNode'
      });
    };

    var link = function (a, b) {
      links.push({
        source: a,
        target: b
      });
    };

    this.reactions.forEach(function (reaction) {

      console.log('Creating reaction of type ' + reaction.type + ': ' +reaction.id);
      var baseStart, baseEnd, baseMid;

      for (var i = 0; i < reaction.base.length; i++) {
        addNode(reaction.base[i].x, reaction.base[i].y, reaction.id, reaction.reactomeId);
        if (i > 0){
          link(nodes[nodes.length - 1], nodes[nodes.length - 2]);
        }

        if (i === 0){
          baseStart = nodes[nodes.length - 1];
        }else if (i === reaction.base.length - 1){
          baseEnd = nodes[nodes.length - 1];
        }else if (i === reaction.base.length - 2){
          baseMid = nodes[nodes.length - 1];
        }
      }
      console.log('Added base start/end/mid: ');
      console.log(baseStart);
      console.log(baseEnd);
      console.log(baseMid);
      reaction.nodes.forEach(function (reactionNode) {
        console.log('Linking connected '+reactionNode.type+' node: '+reactionNode.id);
        var originNode = model.getNodeById(reactionNode.id);
        var reactionBase = reactionNode.base ? reactionNode.base.splice() : [];

        if (reactionBase.length <= 1) {
          console.log('Base of length of 1... linking base to node....');
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
          console.log('Base of length >1... diving deeper...');
          console.log(reactionBase);
          var reactionBaseStart, reactionBaseEnd;
          //either start at 1 or end at -1 IDK
          for (var i = 1; i < reactionBase.length; i++) {
            addNode(reactionBase[i].x, reactionBase[i].y, reaction.id, reaction.reactomeId);
            if (i > 1){
              link(nodes[nodes.length - 1], nodes[nodes.length - 2]);
            }

            if (i === 1){
              reactionBaseStart = nodes[nodes.length - 1];
            }
            if (i === reactionBase.length - 1){
              reactionBaseEnd = nodes[nodes.length - 1];
            }
            console.log('Linking sub base to node');
            console.log(originNode);
            console.log(reactionBaseStart);
            link(originNode, reactionBaseStart);
            console.log('Linking end of sub base to original base');
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
        }

      });

    });
  };

  dcc.PathwayModel = PathwayModel;

})(jQuery);
