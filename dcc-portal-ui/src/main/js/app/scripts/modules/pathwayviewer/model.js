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
    var parsedXml =  $($.parseXML(xml));
    var xmlNodes = parsedXml.find('Nodes')[0].children;
    var nodes = this.nodes;

    $(xmlNodes).each(function(){
      var attrs = this.attributes;

      var bounds = attrs.bounds.nodeValue.split(' ');
      var textPosition = attrs.textPosition ?
        attrs.textPosition.nodeValue.split(' ') :
        attrs.bounds.nodeValue.split(' ');

      nodes.push({
        position: {
          x: +bounds[0],
          y: +bounds[1]
        },
        size: {
          width: +bounds[2],
          height: +bounds[3]
        },
        type: this.tagName.substring(this.tagName.lastIndexOf('.') + 1),
        id: attrs.id.nodeValue,
        reactomeId: attrs.reactomeId ?
          attrs.reactomeId.nodeValue : 'missing',
        text: {
          content: this.textContent.trim(),
          position: {
            x: +textPosition[0],
            y: +textPosition[1]
          }
        }
      });
    });
    
    var edges = parsedXml.find('Edges')[0].children;

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
    $(edges).each(function(){
      var base = getPointsArray(this.attributes.points.nodeValue);
      var nodes=[];
      var description = $(this).children().find('properties').context;

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
        description: description ? description.textContent.trim() : 'no details',
        class: this.localName.substring(this.localName.lastIndexOf('.') + 1),
        center: getPointsArray(this.attributes.position.nodeValue)[0]
      });
    });
  };

  PathwayModel.prototype.getNodeById = function (id) {
    return _.find(this.nodes, {id: id});
  };
  
  PathwayModel.prototype.getNodesByReactomeId = function (reactomeId) {
    return _.where(this.nodes, {reactomeId: reactomeId});
  };

  PathwayModel.prototype.getNodes = function () {
    return this.nodes;
  };

  PathwayModel.prototype.getReactions = function () {
    return this.reactions;
  };
  
  PathwayModel.prototype.getNodeIdsInReaction = function (reactomeId){
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
  
  PathwayModel.prototype.getNodesInReaction = function (reaction){
    var nodes = [];
    var model = this;
    reaction.nodes.forEach(function (n) {
      nodes.push(model.getNodeById(n.id));
    });
    return nodes;
  };

  dcc.PathwayModel = PathwayModel;

})(jQuery);
