(function() {
  'use strict';

  window.dcc = window.dcc || {};

  /********************************************************************************
  *
  * Venn diagram for 2 or 3 sets.
  *
  * This version uses SVG clipping areas to achieve the effect of indivisual 'exploded'
  * pieces. The position of the circles are place into SVG-defs and clipped against each
  * other.
  *
  * Use arc diagrams for 4 or more sets, it scales much better and should be much easier
  * to interact with. Also note arc diagrams do not suffer from degenerated cases of using
  * circular geometry.
  *
  * Venn23 rendering consumes a data array of arrays, where each subarray is used to
  * denote specific ownership groups. For exaple:
  *
  *   [ {id:A, count:10}, {id:B, count:10}]
  *
  * indicates that there are 10 elments that exists in BOTH A and B, and
  *
  *   [ {id:B, count:20}]
  *
  * indicates that there are 20 elements that exists only in B
  *
  * So for set A, B the expected data structure will look like
  *
  * [
  *    [ {id:A, count: X}],
  *    [ {id:B, count: Y}],
  *    [ {id:A, count: Z}, {id:B, count:Z} ]
  * ]
  *
  ********************************************************************************/
  var Venn23 = function(data, config) {
    var defaultConfig = {
      width: 500,
      height: 500,
      margin: 5,
      paddingTop: 10,
      paddingBottom: 10,
      paddingLeft: 10,
      paddingRight: 10,
      outlineColour: '#555',
      hoverColour: '#e9931c',
      urlPath: '',
      mapFunc: function(data) {
        return data;
      },
      clickFunc: function() {
      },
      mouseoverFunc: function() {
      },
      mouseoutFunc: function() {
      },
      labelFunc: function(d) {
        return d;
      }
    };


    config = config || {};
    Object.keys(defaultConfig).forEach(function (key) {
      if (! config.hasOwnProperty(key)) {
        config[key] = defaultConfig[key];
      }
    });

    config.visWidth  = config.width - 2.0 * config.margin;
    config.visHeight = config.height - 2.0 * config.margin;
    config.chartWidth  = config.visWidth - (config.paddingLeft + config.paddingRight);
    config.chartHeight = config.visHeight - (config.paddingTop + config.paddingBottom);

    this.data = data;
    this.config = config;
    this.translate = function(x, y) {
      return 'translate(' + x + ',' + y + ')';
    };

    this.getValueBySetIds = function(ids) {
      var val = 0;
      this.data.forEach(function(group) {
        var groupIds = _.pluck(group, 'id');
        if (_.difference(groupIds, ids).length === 0 && _.difference(ids, groupIds).length === 0) {
          val = group[0].count;
        }
      });
      return val;
    };

    this.max = 0;
    for (var i=0; i < this.data.length; i++) {
      if (this.data[i][0].count > this.max) {
        this.max = this.data[i][0].count;
      }
    }
    //console.log('max', this.max);

    // Scale function - FIXME: need to find max
    // this.colours = ['#B8D0DE', '#9FC2D6', '#86B4CF', '#73A2BD', '#6792AB'];
    this.colours = ['rgb(241,238,246)','rgb(189,201,225)','rgb(116,169,207)','rgb(43,140,190)','rgb(4,90,141)'];
    this.colours = ['rgb(158,202,225)','rgb(107,174,214)','rgb(66,146,198)','rgb(33,113,181)','rgb(8,69,148)'];

    this.colours = ['rgb(180,180,180)']
    this.ramp = d3.scale.linear().domain([0, this.max]).range([0, this.colours.length-1]);
    this.getColourBySetIds = function(ids) {
      return this.colours[Math.ceil(this.ramp(this.getValueBySetIds(ids)))];
    };

    this.svg = undefined;
    this.vis = undefined;
    this.chart = undefined;
  };


  Venn23.prototype.render2 = function() {
    var _this = this;
    var config = _this.config;
    // var cy = 0.5 * config.chartHeight;
    var cy = 0.3 * config.chartHeight;
    var cx = 0.5 * config.chartWidth;
    var svg = _this.svg;
    var defs = svg.append('svg:defs');
    var radius = 100;
    var factor = 0.60;
    var uniqueIds = this.getDistinctIds();

    defs.append('svg:clipPath')
      .attr('id', 'circle1-set2')
      .append('svg:circle')
      .attr('cx', cx - radius * factor)
      .attr('cy', cy)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', 'circle2-set2')
      .append('svg:circle')
      .attr('cx', cx + radius * factor)
      .attr('cy', cy)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', 'circle1_out-set2')
      .append('svg:circle')
      .attr('cx', cx - radius * factor)
      .attr('cy', cy)
      .attr('r', radius+3);

    defs.append('svg:clipPath')
      .attr('id', 'circle2_out-set2')
      .append('svg:circle')
      .attr('cx', cx + radius * factor)
      .attr('cy', cy)
      .attr('r', radius+3);


    // 1 intersection
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1_out-set2)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2_out-set2)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);

    svg.append('svg:rect')
      .datum({selected: false, data:[uniqueIds[0]]})
      .attr('clip-path', 'url(' + config.urlPath + '#circle1-set2)')
      .attr('class', 'inner')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });
    svg.append('svg:rect')
      .datum({selected:false, data:[uniqueIds[1]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2-set2)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });


    // 2 intersections
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1_out-set2)')
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2_out-set2)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1-set2)')
      .append('svg:rect')
      .datum({selected: false, data:[uniqueIds[0], uniqueIds[1]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2-set2)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });


    // Label - name
    svg.append('text')
      .attr('x', cx - 2.8*radius * factor)
      .attr('y', cy)
      .attr('text-anchor', 'end')
      .style('fill', '#333333')
      .text(config.labelFunc(uniqueIds[0]));

    svg.append('text')
      .attr('x', cx + 2.8*radius * factor)
      .attr('y', cy)
      .attr('text-anchor', 'start')
      .style('fill', '#333333')
      .text(config.labelFunc(uniqueIds[1]));


    // Label - value
    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx -  1.1*radius * factor)
      .attr('y', cy)
      .attr('text-anchor', 'end')
      .text(_this.getValueBySetIds([uniqueIds[0]]));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + 1.1*radius * factor)
      .attr('y', cy)
      .attr('text-anchor', 'start')
      .text(_this.getValueBySetIds([uniqueIds[1]]));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx)
      .attr('y', cy)
      .attr('text-anchor', 'middle')
      .text(_this.getValueBySetIds([uniqueIds[0], uniqueIds[1]]));



  };

  Venn23.prototype.render3 = function() {
    var _this = this;
    var config = _this.config;
    // var cy = 0.5 * config.chartHeight;
    var cy = 0.4 * config.chartHeight;
    var cx = 0.5 * config.chartWidth;
    var svg = _this.svg;
    var defs = svg.append('svg:defs');
    var radius = 100;
    var factor = 0.75;
    var uniqueIds = this.getDistinctIds();

    defs.append('svg:clipPath')
      .attr('id', 'circle1')
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 300/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 300/180) * radius * factor)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', 'circle2')
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 60/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 60/180) * radius * factor)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', 'circle3')
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 180/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 180/180) * radius * factor)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', 'circle1_out')
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 300/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 300/180) * radius * factor)
      .attr('r', radius+3);

    defs.append('svg:clipPath')
      .attr('id', 'circle2_out')
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 60/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 60/180) * radius * factor)
      .attr('r', radius+3);

    defs.append('svg:clipPath')
      .attr('id', 'circle3_out')
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 180/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 180/180) * radius * factor)
      .attr('r', radius+3);


    // 1 intersection
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1_out)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2_out)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle3_out)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);

    svg.append('svg:rect')
      .datum({selected:false, data:[uniqueIds[0]]})
      .attr('clip-path', 'url(' + config.urlPath + '#circle1)')
      .attr('class', 'inner')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });
    svg.append('svg:rect')
      .datum({selected:false, data:[uniqueIds[1]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });
    svg.append('svg:rect')
      .datum({selected:false, data:[uniqueIds[2]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + '#circle3)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });

    // 2 intersections
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1_out)')
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2_out)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1)')
      .append('svg:rect')
      .datum({selected:false, data:[uniqueIds[0], uniqueIds[1]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });

    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2_out)')
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle3_out)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2)')
      .append('svg:rect')
      .datum({selected:false, data:[uniqueIds[1], uniqueIds[2]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + '#circle3)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });

    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle3_out)')
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1_out)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle3)')
      .append('svg:rect')
      .datum({selected:false, data:[uniqueIds[2], uniqueIds[0]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });


    // 3 intersections
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle3_out)')
      .append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2_out)')
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1_out)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle3)')
      .append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + '#circle2)')
      .append('svg:rect')
      .datum({selected:false, data:[uniqueIds[0], uniqueIds[1], uniqueIds[2]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + '#circle1)')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function(d) {
        return _this.getColourBySetIds(d.data);
      });

    // Label - name
    svg.append('text')
      .attr('x', cx + Math.sin(Math.PI * 300/180) * 2.5*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 300/180) * 2.5*radius * factor)
      .attr('text-anchor', 'end')
      .style('fill', '#333333')
      .text(function() {
        return config.labelFunc(uniqueIds[0]);
      });

    svg.append('text')
      .attr('x', cx + Math.sin(Math.PI * 60/180) * 2.5*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 60/180) * 2.5*radius * factor)
      .attr('text-anchor', 'start')
      .style('fill', '#333333')
      .text(function() {
        return config.labelFunc(uniqueIds[1]);
      });

    svg.append('text')
      .attr('x', cx + Math.sin(Math.PI * 180/180) * 2.6*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 180/180) * 2.6*radius * factor)
      .attr('text-anchor', 'middle')
      .style('fill', '#333333')
      .text(function() {
        return config.labelFunc(uniqueIds[2]);
      });


    // Label - value
    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 300/180) * 1.1*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 300/180) * 1.1*radius * factor)
      .attr('text-anchor', 'end')
      .text(_this.getValueBySetIds([uniqueIds[0]]));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 60/180) * 1.1*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 60/180) * 1.1*radius * factor)
      .attr('text-anchor', 'start')
      .text(_this.getValueBySetIds([uniqueIds[1]]));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 180/180) * 1.1*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 180/180) * 1.1*radius * factor)
      .attr('text-anchor', 'middle')
      .text(_this.getValueBySetIds([uniqueIds[2]]));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 360/180) * 0.85 * radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 360/180) * 0.85 * radius * factor)
      .attr('text-anchor', 'middle')
      .text(_this.getValueBySetIds([uniqueIds[0], uniqueIds[1]]));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 120/180) * 0.85 * radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 120/180) * 0.85 * radius * factor)
      .attr('text-anchor', 'middle')
      .text(_this.getValueBySetIds([uniqueIds[1], uniqueIds[2]]));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 240/180) * 0.85 * radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 240/180) * 0.85 * radius * factor)
      .attr('text-anchor', 'middle')
      .text(_this.getValueBySetIds([uniqueIds[2], uniqueIds[0]]));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx)
      .attr('y', cy)
      .attr('text-anchor', 'middle')
      .text(_this.getValueBySetIds([uniqueIds[0], uniqueIds[1], uniqueIds[2]]));
  };


  Venn23.prototype.getDistinctIds = function() {
    var _this = this;
    var result = [];
    _this.data.forEach(function(subset) {
      subset.forEach(function(item) {
        if (result.indexOf(item.id) === -1) {
          result.push(item.id);
        }
      });
    });
    return result;
  };


  Venn23.prototype.render = function(element) {
    var _this = this;
    var config = _this.config;

    var uniqueIds = this.getDistinctIds();
    //console.log('uniqueIds', uniqueIds);

    _this.svg = d3.select(element).append('svg')
      .attr('viewBox', '0 0 ' + config.width + ' ' + config.height)
      .attr('preserveAspectRatio', 'xMidYMid');

    _this.vis = _this.svg.append('g').attr('transform', _this.translate(config.margin, config.margin));
    _this.chart = _this.vis.append('g').attr('transform', _this.translate(config.paddingLeft, config.paddingTop));

    if (uniqueIds.length === 2) {
      this.render2();
    } else if (uniqueIds.length === 3) {
      this.render3();
    }

    // Add interactions
    _this.svg.selectAll('.inner')
      .on('mouseover', function(d) {
        d3.select(this).style('fill', config.hoverColour);
        config.mouseoverFunc(d);
      })
      .on('mouseout', function(d) {
        if (d.selected === false) {
          d3.select(this).style('fill', _this.getColourBySetIds(d.data));
        }
        config.mouseoutFunc(d);
      })
      .on('click', function(d) {
        d.selected = !d.selected;
        _this.toggleHighlight(d.data, d.selected);
        config.clickFunc(d);
      });


    // Global setting
    _this.svg.selectAll('text').style('pointer-events', 'none');
    _this.svg.selectAll('.inner').style('cursor', 'pointer');
    _this.svg.selectAll('.inner').each(function(d) {
      d.count = _this.getValueBySetIds(d.data);
    });


    // Add legend
    /*
    _this.svg.append('text').attr('x', 25).attr('y', 15).text(0);
    _this.svg.append('text').attr('x', 25 + _this.colours.length*15).attr('y', 15).text(_this.max);
    _this.svg.selectAll('.legend')
      .data(_this.colours)
      .enter()
      .append('rect')
      .classed('legend', true)
      .attr('x', function(d, i) {
        return 30 + 15*i;
      })
      .attr('y', function() {
        return 15;
      })
      .attr('width', 15)
      .attr('height', 15)
      .style('stroke', '#FFFFFF')
      .style('fill', function(d) {
        return d;
      });
      */

  };


  Venn23.prototype.toggle = function(ids, forcedState){
    var _this = this;

    d3.selectAll('.inner')
      .filter(function(d) {
        return _.difference(d.data, ids).length === 0 && _.difference(ids, d.data).length === 0;
      })
      .each(function(d) {
        if (typeof forcedState === 'undefined') {
          d.selected = !d.selected;
        } else {
          d.selected = forcedState;
        }
        _this.toggleHighlight(d.data, d.selected);
      });
  };


  Venn23.prototype.toggleHighlight = function(ids, bool) {
    var _this = this;
    var config = _this.config;

    d3.selectAll('.inner')
      .filter(function(d) {
        return _.difference(d.data, ids).length === 0 && _.difference(ids, d.data).length === 0;
      })
      .style('fill', function(d) {
        if (d.selected === true) {
          return config.hoverColour;
        }

        if (bool === true) {
          return config.hoverColour;
        } else {
          return _this.getColourBySetIds(d.data);
        }
      });
  };


  Venn23.prototype.update = function() {
    // TODO
  };

  dcc.Venn23 = Venn23;

})();
