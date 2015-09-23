(function($) {

  'use strict';

  window.dcc = window.dcc || {};

  var PathwayModel = function () {
    this.nodes = [];
    this.reactions = [];
    this.links = [];
  };

  PathwayModel.prototype.parse = function (xml) {
    var parsedXml =  $($.parseXML(xml));

    // Parse all the nodes first
    var xmlNodes = parsedXml.find('Nodes')[0].children;
    var nodes = this.nodes;
    
    // Find if there are any crossed out
    var crossedList = [];
    var crossedComponents = parsedXml.find('crossedComponents');
    if (typeof crossedComponents !== 'undefined' && typeof crossedComponents[0] !== 'undefined') {
      var crossedText = crossedComponents[0].textContent;
      crossedList = crossedList.concat(crossedText.split(','));
    }
    
    // Find if there are any loss of function nodes
    var lofList = [];
    var lofNodes = parsedXml.find('lofNodes');
    if (typeof lofNodes !== 'undefined' && typeof lofNodes[0] !== 'undefined') {
      var lofText = lofNodes[0].textContent;
      lofList = lofList.concat(lofText.split(','));
    }
    
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
        crossed: (crossedList.indexOf(attrs.id.nodeValue) >= 0 ) ? true : false,
        lof: (lofList.indexOf(attrs.id.nodeValue) >= 0 ) ? true : false,
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
      
      var schemaClass = this.attributes.schemaClass;
      var failedReaction = false;
      if (!(typeof schemaClass === 'undefined') && schemaClass.nodeValue === 'FailedReaction') {
        failedReaction = true;
      }

      $(this).find('input,output,catalyst,activator,inhibitor').each(function(){
        nodes.push({
            type: this.localName.substring(0,1).toUpperCase()+this.localName.substring(1),
            base: this.getAttribute('points') ?
              getPointsArray(this.getAttribute('points')) : [],
            id: this.id,
          });
      });
      
      reactions.push({
        base: base,
        nodes: nodes,
        failedReaction: failedReaction,
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
    return _.map(reaction.nodes, function(node){ return this.getNodeById(node.id);}, this);
  };

  dcc.PathwayModel = PathwayModel;

})(jQuery);
