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
      var description = $(this).find('properties');

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
        description: description[0] ? description[0].innerText.trim() : 'no details',
        class: this.localName.substring(this.localName.lastIndexOf('.') + 1),
        center: getPointsArray(this.attributes.position.nodeValue)[0]
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
  
  PathwayModel.prototype.getNodesByReactomeId = function (reactomeId) {
    var nodes = [];
    this.nodes.forEach(function (n) {
      if (n.reactomeId === reactomeId){
        nodes.push(n);
      }
    });
    return nodes;
  };

  PathwayModel.prototype.getNodes = function () {
    return this.nodes;
  };

  PathwayModel.prototype.getReactions = function () {
    return this.reactions;
  };
  
  PathwayModel.prototype.getNodesInReaction = function (reactomeId){
    var nodes = [];
    var model = this;
    this.reactions.forEach(function (reaction) {
      if(reaction.reactomeId === reactomeId){
        reaction.nodes.forEach(function (elem) {
          nodes.push(model.getNodeById(elem.id).reactomeId);
        });
      }
    });
    return nodes;
  };

  dcc.PathwayModel = PathwayModel;

})(jQuery);
