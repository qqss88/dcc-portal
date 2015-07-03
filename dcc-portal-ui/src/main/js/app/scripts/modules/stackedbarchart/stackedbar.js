(function() {

  'use strict';

	window.dcc = window.dcc || {};

   /* Dfault configulration */


	/* Create the stacked bar chart using d3 */
	var StackedBarChart = function(config){
		this.config = config;
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
	StackedBarChart.prototype.render = function(element, data){
		this.element = element;
		this.data = data;

		var config = this.config;

		// the scale for the x axis
		var x = d3.scale.ordinal()
		    .rangeRoundBands([0, config.width], 0.2);

		// the scale for the y axis
		var y = d3.scale.linear()
		    .rangeRound([config.height, 0]);

		// creates a colour scale to turn the project id into its project colour
		var colour = d3.scale.ordinal()
		    .domain(d3.keys(config.colours))
		    .range(d3.values(config.colours));

		// Create the x and y axis
		var xAxis = d3.svg.axis()
		    .scale(x)
		    .orient('bottom');
		var yAxis = d3.svg.axis()
		    .scale(y)
		    .orient('left').ticks(config.yaxis.ticks);

		// Create the svg
      d3.select(element).select('#svgstackedbar').remove();
		var svg = d3.select(element).append('svg')
		    .attr('id','svgstackedbar')
		    .attr('viewBox','0 0 '+(config.width+config.margin.left+config.margin.right)+
                  ' '+(config.height + config.margin.top + config.margin.bottom))
		    .attr('preserveAspectRatio','xMidYMid')
		    .append('g')
		    .attr('transform', 'translate(' + config.margin.left + ',' + config.margin.top + ')');


	  // Create domain of x scale based off data
	  x.domain(this.data.map(function(d) { return d.key ; }));
	  y.domain([0, d3.max(this.data, function(d) { return d.total; })]);

	  // Add the x axis with tilted labels
	  svg.append('g')
	      .attr('class', 'stacked x axis')
	      .attr('transform', 'translate(0,' + config.height + ')')
	      .call(xAxis)
	      .selectAll('text')
	        .style('text-anchor', 'end')
	        .style('font-size','9px')
	        .style('font-family','Lucida Grande')
	        .style('fill','gray')
	        .attr('dx', '-.8em')
	        .attr('dy', '.15em')
	        .attr('transform', 'rotate(-65)' );

	  // add the y axis and the y axis label
    svg.append('g')
	   .attr('class', 'stacked y axis')
      .attr('transform', 'translate(-5,0)')
	   .call(yAxis)
	   .style('fill','gray')
	   .selectAll('text')
	   .style('font-size','8px');

    svg.select('.stacked.y.axis')
      .style('font-size','10px')
	   .append('text')
	   .attr('transform', 'rotate(-90)')
	   .attr('y', -config.margin.left+5)
	   .attr('x',-config.height / 2)
	   .attr('dy', '1em')
	   .style('text-anchor', 'middle')
	   .text(config.yaxis.label);

	 //add gridlines
    var max = config.height;
    svg.selectAll('line.horizontalGrid').data(y.ticks(config.yaxis.ticks)).enter()
     .append('line')
     .attr({
      'class':'horizontalGrid',
      'x1' : '-5',
      'x2' : config.width,
      'y1' : function(d){ return y(d);},
      'y2' : function(d){ return y(d);},
      'fill' : 'none',
      'shape-rendering' : 'crispEdges',
      'stroke' : '#DDD',
      'stroke-width' : function(d) {
        if(y(d) === max || y(d) ===0){
          return '0px';
        }
        return '1px';
      }
    });

    // create the empty column group that we will add projects to
    var bar = svg.selectAll('.gene')
        .data(this.data)
        .enter().append('g')
        .attr('class', 'stacked g');

     // create the columns
    bar.selectAll('rect')
        .data(function(d) {
          return d.stack;
        })
        .enter()
        .append('rect')
        .style('fill', function(d, i) {
          if (config.alternateBrightness) {
            return d3.rgb( colour(d.colourKey) ).brighter( i%2 * 0.3);
          }
          return colour(d.colourKey);
        })
        .attr('width', x.rangeBand())
        .attr('x',function(d){return x(d.key);})
        .attr('y', function(d) { return y(d.y1); })
        .attr('height', function(d) { return y(d.y0) - y(d.y1); })
        .on('mouseover', function(d) {
          d3.select(this).style('stroke', '#000').style('stroke-width', 2);
          config.tooltipShowFunc(this,d);
        })
        .on('mouseout', function() {
          d3.select(this).style('stroke', 'none');
          config.tooltipHideFunc();
        })
        .on('click',function(d){
          if (d.link) {
            config.onClick(d.link);
          }
        });
  };

	StackedBarChart.prototype.destroy = function(){
	  this.data = null;
     d3.select(this.element).selectAll('*').remove();
	};

	dcc.StackedBarChart = StackedBarChart;
})();
