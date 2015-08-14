(function() {

  'use strict';

	window.dcc = window.dcc || {};

  /* Dfault configulration */

  var contentElementId = 'svgstackedbar';

  function removeContentElement (parentElement) {
    d3.select(parentElement)
      .select('#' + contentElementId)
      .remove();
  }

	/* Create the stacked bar chart using d3 */
	var StackedBarChart = function (config) {
    var margin = config.margin;

    this.config = config;
    this.viewport = {
      x: 0,
      y: 0,
      width: config.width + margin.left + margin.right,
      height: config.height + margin.top + margin.bottom
    };
	};

  /**
    * Main renderer
    * data is an array, where each of the array of object:
    *
    * [
    *    {
    *       total:
    *       stack: [
    *         name
    *         y0
    *         y1
    *         label
    *         colourKey
    *         link
    *       ]
    *    }
    *    ...
    * ]
    */

	StackedBarChart.prototype.render = function (element, data) {
		this.element = element;
    this.data = data;

		var config = this.config;

		// the scale for the x axis
		var x = d3.scale.ordinal()
		  .rangeRoundBands ([0, config.width], 0.2);

		// the scale for the y axis
		var y = d3.scale.linear()
		  .rangeRound ([config.height, 0]);

    var colors = config.colours;
		// creates a colour scale to turn the project id into its project colour
		var colour = d3.scale.ordinal()
		  .domain (d3.keys (colors))
		  .range (d3.values (colors));

		// Create the x and y axis
		var xAxis = d3.svg.axis()
		    .scale(x)
		    .orient('bottom');
		var yAxis = d3.svg.axis()
		    .scale(y)
		    .orient('left')
        .ticks (config.yaxis.ticks);

    var margin = config.margin;
    var viewport = this.viewport;

    removeContentElement (element);

    // Create the svg element
		var svg = d3.select (element)
      .append ('svg')
      .attr ('id', contentElementId)
	    .attr ('viewBox', [viewport.x, viewport.y, viewport.width, viewport.height].join (' '))
	    .attr ('preserveAspectRatio','xMidYMid')
	    .append ('g')
	    .attr ('transform', 'translate(' + margin.left + ',' + margin.top + ')');

	  // Set the accessors for the two domains
	  x.domain (data.map (_.property ('key')));
    y.domain ([0, d3.max (data, _.property ('total'))]);

	  // Adds the X axis with tilted labels
	  svg.append ('g')
      .attr('class', 'stacked x axis')
      .attr('transform', 'translate(0,' + config.height + ')')
      .call(xAxis)
      .selectAll('text')

      .style ('text-anchor', 'end')
      .style ('font-size','9px')
      .style ('font-family','Lucida Grande')
      .style ('fill','gray')
      .attr ('dx', '-.8em')
      .attr ('dy', '.15em')
      .attr ('transform', 'rotate(-65)' );

	  // Adds the Y axis
    svg.append ('g')
      .attr('class', 'stacked y axis')
      .attr('transform', 'translate(-5,0)')
      .call(yAxis)
      .style('fill','gray')
      .selectAll('text')
      .style('font-size','8px');

    // Adds label on the Y axis
    svg.select('.stacked.y.axis')
      .style('font-size', '10px')
	    .append('text')
	    .attr('transform', 'rotate(-90)')
	    .attr('y', -margin.left + 5)
	    .attr('x', -config.height / 2)
	    .attr('dy', '1em')
	    .style('text-anchor', 'middle')
	    .text(config.yaxis.label);

    // Adds horizontal gridlines
    svg.selectAll ('line.horizontalGrid')
      .data(y.ticks (config.yaxis.ticks))
      .enter()
      .append('line')
      .attr({
        'class': 'horizontalGrid',
        'x1': '-5',
        'x2': config.width,
        'y1': function (d) {return y(d);},
        'y2': function (d) {return y(d);},
        fill: 'none',
        'shape-rendering': 'crispEdges',
        'stroke': '#DDD',
        'stroke-width': function (d) {
          var height = y(d);
          return (height === config.height || height === 0) ? '0px' : '1px';
        }
    });

    // Creates the empty column group that we will add projects to
    var bar = svg.selectAll('.gene')
      .data(this.data)
      .enter()
      .append('g')
      .attr('class', 'stacked g');

    // Creates the stacked columns
    bar.selectAll('.stack')
      .data (_.property ('stack'))
      .enter()
      .append('rect')
      .classed('stack', true)
      .style ('fill', function (d, i) {
        var fillColor = colour (d.colourKey);

        return config.alternateBrightness ?
          d3.rgb (fillColor).brighter (i%2 * 0.3) :
          fillColor;
      })

      .attr('x', function (d) {return x (d.key);})
      .attr('y', function (d) {return y (d.y1);})
      .attr('width', x.rangeBand())
      .attr('height', function (d) {return y(d.y0) - y(d.y1);})

      // event handlers
      .on ('mouseover', function (d) {
        svg.append('rect')
          .classed('chart-focus', true)
          .attr('x', x (d.key))
          .attr('y', y (d.y1))
          .attr('width', x.rangeBand())
          .attr('height', function() {return y(d.y0) - y(d.y1);})
          .attr('fill', 'none')
          .attr('stroke', '#283e5d')
          .attr('stroke-width', 2);

        config.tooltipShowFunc (this,d);
      })

      .on ('mouseout', function() {
        svg.selectAll('.chart-focus').remove();

        config.tooltipHideFunc();
      })

      .on ('click',function (d) {
        if (d.link) {
          config.onClick(d.link);
        }
      });

  };

  StackedBarChart.prototype.displaysNoResultMessage = function (element, message) {
    message = _.isString (message) ? message : 'No mutations found';
    var width = this.viewport.width + 'px';
    var height = this.viewport.height + 'px';

    removeContentElement (element);

    var messageDiv = d3.select (element)
      .append ('div')
      .attr ('id', contentElementId)
      .style ('width', width)
      .style ('height', height)
      .style ('line-height', height)
      .style ('text-align', 'center')
      .html ('<span><strong>' + message + '</strong></span>');
  };

	StackedBarChart.prototype.destroy = function(){
	  this.data = null;
    removeContentElement (this.element);
	};

	dcc.StackedBarChart = StackedBarChart;
})();
