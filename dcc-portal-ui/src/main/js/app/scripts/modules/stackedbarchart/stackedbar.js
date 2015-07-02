(function() {

  'use strict';

	window.dcc = window.dcc || {};

	/* Create the stacked bar chart using d3 */

	var StackedBarChart = function(config){
		this.config = config;
	};

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

		// creates a color scale to turn the project id into its project color
		var color = d3.scale.ordinal()
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


	  // For each gene, create an array of donors and get the total affected donors count
     /*
	  this.data.forEach(function(d) {
	    var y0 = 0;
	    d.stack = d.uiFIProjects
	    .sort(function(a,b){return a.count-b.count;}) //sort so biggest is on top
	    .map(function(p) {
         return {
           name: p.id,
           y0: y0,
           y1: y0 += p.count,
           key: d.symbol,
           link: d.id,
           label: p.name,
           primarySite: p.primarySite
         };
       });
	    d.total = d.stack[d.stack.length - 1].y1;
	  });
     */

	  // Sort so in descending order
	  this.data.sort(function(a, b) { return b.total - a.total; });

	  // Create domain of x scale based off data
	  x.domain(this.data.map(function(d) { return d.symbol; }));
	  y.domain([0, d3.max(this.data, function(d) { return d.total; })]);

	  // Add the x axis with tilted labels
	  svg.append('g')
	      .attr('class', 'stacked x axis')
	      .attr('transform', 'translate(0,' + config.height + ')')
	      .call(xAxis)
	      .selectAll('text')
	        .style('text-anchor', 'end')
	        .style('font-size','8px')
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
      'stroke' : 'lightgray',
      'stroke-width' :
       function(d){
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
        .data(function(d) { return d.stack; }) //goes through each of the projects of each gene
        .enter().append('rect')
        .style('fill', function(d) { return color(d.primarySite); })
        .attr('width', x.rangeBand())
        .attr('x',function(d){return x(d.key);})
        .attr('y', function(d) { return y(d.y1); })
        .attr('height', function(d) { return y(d.y0) - y(d.y1); })
        .on('mouseover', function(d) {
          config.tooltipShowFunc(this,d);
        })
        .on('mouseout', function() {
          config.tooltipHideFunc();
        })
        .on('click',function(d){   
          config.onClick(d.link);
        });
  };

	StackedBarChart.prototype.destroy = function(){
	  this.data = null;
     d3.select(this.element).selectAll('*').remove();
	};

	dcc.StackedBarChart = StackedBarChart;
})();
