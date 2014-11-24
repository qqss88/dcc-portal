(function() {

  'use strict';

  window.dcc = window.dcc || {};

  /*******************************************************************************
  *
  * Project mutation plot modelled after Gaddy Getz's mutation distrubtion chart
  *
  * data: [
  *   {  id: ProjId
  *      outliers: [ {D1: M1}, {D2, M2} ... {DN, Mn} ]
  *      points: [ P1, P2 ... Pk ]
  *   }
  *   ...
  * ]
  *
  *******************************************************************************/
  var ProjectMutationChart = function(data, config) {
    var defaultConfig = {
      width: 950,
      height: 230,
      margin: 5,
      paddingTop: 40,
      paddingBottom: 18,
      paddingLeft: 45,
      paddingRight: 20,
      title: 'High impact mutation across projects',
      mapFunc: function(data) {
        return data;
      }
    };

    config = config || {};
    Object.keys(defaultConfig).forEach(function (key) {
      if (! config.hasOwnProperty(key)) {
        config[key] = defaultConfig[key];
      }
    });


    // Helper functions
    this.translate = function(x, y) {
      return 'translate(' + x + ',' + y + ')';
    };

    this.tickValues = [0.01, 0.1, 1, 10, 100, 1000, 10000, 100000];
    this.data = data.map(config.mapFunc);

    // ---------------------------------------------------------------------
    // | <----------------------------- margin --------------------------> |
    // | <- margin -> | <--------------- vis -------------> | <- margin -> |
    // | <- margin -> |                                     | <- margin -> |
    // | <- margin -> | <- padding -> | chart | <-padding-> | <- margin -> |
    // | <- margin -> |                                     | <- margin -> |
    // | <----------------------------- margin --------------------------> |
    // ---------------------------------------------------------------------
    config.visWidth  = config.width - 2.0 * config.margin;
    config.visHeight = config.height - 2.0 * config.margin;
    config.chartWidth  = config.visWidth - (config.paddingLeft + config.paddingRight);
    config.chartHeight = config.visHeight - (config.paddingTop + config.paddingBottom);
    config.projectWidth = (config.chartWidth / this.data.length);


    // TODO: Should double check this and make it automatically itself out based on input
    this.yScale = d3.scale.log().base(10).domain([Math.pow(10, -2), Math.pow(10, 5)]).range([config.chartHeight, 0]);


    this.yAxis = d3.svg.axis().scale(this.yScale).orient('left').tickValues(this.tickValues).tickFormat(function(d) {
      // return d;
      return d3.format('s')(d); // SI format
    }).tickSize(4);

    this.config = config;
  };


  ////////////////////////////////////////////////////////////////////////////////
  //
  // The layout should be something like:
  //   low outliers -> normal range -> high outliers
  //
  ////////////////////////////////////////////////////////////////////////////////
  ProjectMutationChart.prototype.render = function(element) {
    var _this = this;
    var config = _this.config;
    var svg, vis, chart, point;

    _this.element = element;

    svg = d3.select(element).append('svg').attr('width' , config.width).attr('height', config.height);
    vis = svg.append('g').attr('transform', _this.translate(config.margin, config.margin));
    chart = vis.append('g').attr('transform', _this.translate(config.paddingLeft, config.paddingTop));

    chart.append('rect')
      .attr('x', 0)
      .attr('y', 0)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('stroke', '#999')
      .style('stroke-width', 1)
      .style('opacity', 0.5)
      .style('fill', 'none');

    // Register mouse
    chart.on('mousemove', function() {
      point = d3.mouse(this);
    });


    /*
    vis.append('g')
      .attr('transform', _this.translate(0.5*config.chartWidth, 11))
      .append('text')
      .classed('graph_title', true)
      .text(config.title);
    */

    vis.append('g')
      .attr('transform', _this.translate(5, (config.paddingTop + config.chartHeight)*0.85) + ' rotate(-90)')
      .append('text')
      .classed('axis_label', true)
      .text('Number of mutations per Mb');

    vis.append('g')
      .attr('class', 'axis')
      .attr('transform', _this.translate(config.paddingLeft, config.paddingTop))
      .call(_this.yAxis);

    _this.data.forEach(function(projData, idx) {
      console.log(projData);
      var stripe, labelX, labelY;
      var projectControl = chart.append('g')
        .datum({
          id: projData.id,
          medium: projData.medium,
          donorCount: projData.points.length
        })
        .attr('class', 'project_control')
        .attr('class', 'graph_interactive')
        .attr('transform', _this.translate(config.projectWidth*idx, 0));


      // Interactive
      projectControl.on('mouseover', function(d) {
        var yOffset = config.paddingTop + config.chartHeight + config.paddingBottom;

        d3.select(this).select('rect').style('stroke', '#228');
        svg.selectAll('.axis_label').filter(function(node) {
          return node && node.id === d.id;
        }).style('font-weight', 600);

        svg.append('text')
          .attr('class', 'project_summary axis_label')
          .attr('transform', _this.translate(20 + config.paddingLeft, yOffset))
          .text('Project: ' + d.id);

        svg.append('text')
          .attr('class', 'project_summary axis_label')
          .attr('transform', _this.translate(140 + config.paddingLeft, yOffset))
          .text('Medium: ' + d.medium.toFixed(2));

        svg.append('text')
          .attr('class', 'project_summary axis_label')
          .attr('transform', _this.translate(260 + config.paddingLeft, yOffset))
          .text('Donor Count: ' + d.donorCount);
      });

      projectControl.on('mouseout', function(d) {
        d3.select(this).select('rect').style('stroke', '#EEE');
        svg.selectAll('.axis_label').filter(function(node) {
          return node && node.id === d.id;
        }).style('font-weight', 400);
        svg.selectAll('.project_summary').remove();
      });


      // Alternate stripe
      stripe = (idx % 2) * 2 + 248;

      projectControl.append('rect')
        .datum( { fill: d3.rgb(stripe, stripe, stripe)})
        .attr('x', 0)
        .attr('y', 0)
        .attr('width', config.projectWidth)
        .attr('height', config.chartHeight)
        .style('stroke', '#EEE')
        .style('fill', function(d) { return d.fill; })
        .style('opacity', 0.75);

      // Non interactive points
      projectControl.selectAll('.non-interactive-point')
        .data( projData.points )
        .enter()
        .append('circle')
        .attr('class', 'non-interactive-point')
        .attr('cx', function(d, i) {
          // Clamp so the left/right sides are not used
          return 1 + i * ( (config.projectWidth-2) / projData.points.length);
        })
        .attr('cy', function(d) {
          return _this.yScale( d );
        })
        .attr('r', 1)
        .style('stroke', 'none')
        .style('fill', '#777')
        .style('opacity', 0.5);

      // Statistics - TODO: check with Junjun
      projectControl.append('rect')
        .attr('x', 2)
        .attr('y', _this.yScale(projData.medium))
        .attr('width', config.projectWidth - 4)
        .attr('height', 1)
        .style('stroke', 'none')
        .style('opacity', 0.7)
        .style('fill', '#EE2200');

      // Render labels
      labelX = (config.projectWidth / 2) + config.paddingLeft + (idx * config.projectWidth);
      // labelY =  6 + config.paddingTop + config.chartHeight;
      labelY = config.paddingTop - 5;
      vis.append('text')
        .datum( { id: projData.id })
        .attr('class', 'axis_label')
        .attr('text-anchor', 'end')
        .attr('transform', _this.translate(labelX, labelY) + ', rotate(45)')
        .text(projData.id);
    });
  };


  ProjectMutationChart.prototype.highlight = function(ids) {
    var _this = this;

    d3.select(_this.element).selectAll('.project_control').style('opacity', 1.0);
    d3.select(_this.element).selectAll('.axis_label').style('opacity', 1.0);

    if (!ids || ids.length === 0) {
      return;
    }

    d3.select(_this.element).selectAll('.project_control')
      .filter(function(node) {
        return ids.indexOf(node.id) === -1;
      })
      .transition()
      .duration(400)
      .style('opacity', 0.1);

    d3.select(_this.element).selectAll('.axis_label')
      .filter(function(node) {
        return node && ids.indexOf(node.id) === -1;
      })
      .transition()
      .duration(400)
      .style('opacity', 0.1);
  };


  ProjectMutationChart.prototype.destroy = function() {
    this.data = null;
    d3.select(this.element).selectAll('*').remove();
  };


  dcc.ProjectMutationChart = ProjectMutationChart;
})();
