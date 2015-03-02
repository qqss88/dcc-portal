(function() {

  'use strict';

	window.dcc = window.dcc || {};

	var StackedAreaChart = function(data, config){
		this.data = data;
		this.config = config;
        this.selectedView = "area";
	};

	StackedAreaChart.prototype.render = function(element){
      var data = this.data;
      var config = this.config;

      var margin = config.margin;
      margin.left = margin.left +10;
      var width = config.width - margin.left - margin.right,
          height = config.height - margin.top - margin.bottom;

      var x = d3.scale.linear()
          .range([0, width]).domain([4,18]);

      var y = d3.scale.linear()
          .range([height, 0]);

      var color = d3.scale.ordinal()
		    .domain(d3.keys(config.colours))
		    .range(d3.values(config.colours));

      var xAxis = d3.svg.axis()
          .scale(x)
          .orient("bottom").ticks(14);

      var yAxis = d3.svg.axis()
          .scale(y)
          .orient("left").ticks(config.yaxis.ticks).tickFormat(d3.format(".2s"));;



      var stack = d3.layout.stack()
          .offset("zero")
          .values(function(d) { return d.values; })
          .x(function(d) { return d.release; })
          .y(function(d) { return d.value; });

      var nest = d3.nest()
          .key(function(d) { return d.project; });

      var area = d3.svg.area()
          .interpolate("linear")
          .x(function(d) { return x(d.release); })
          .y0(function(d) { return y(d.y0); })
          .y1(function(d) { return y(d.y0 + d.y); });

      var svg = d3.select(element).append('svg')
		    .attr('viewBox','0 0 '+(width + margin.left + margin.right)+
                  ' '+(height + margin.top + margin.bottom))
		    .attr('preserveAspectRatio','xMidYMid')
          .append("g")
          .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var layers = stack(nest.entries(data));

        y.domain([0, d3.max(data, function(d) { return d.y+d.y0; })]); //vs d.value

        var max = config.height;
        svg.selectAll('line.horizontalGrid').data(y.ticks(config.yaxis.ticks)).enter()
         .append('line')
         .attr(
          {
          'class':'horizontalGrid',
          'x1' : '-5',
          'x2' : config.width - margin.left - margin.right,
          'y1' : function(d){ return y(d);},
          'y2' : function(d){ return y(d);},
          'fill' : 'none',
          'shape-rendering' : 'crispEdges',
          'stroke' : 'lightgray',
          'stroke-width' :'1px'
        });

      var line = d3.svg.line()
      .x(function(d) { return x(d.release); })
      .y(function(d) { return y(d.value); });

        var project = svg.selectAll(".layer-project")
            .data(layers)
            .enter().append("g")
            .attr("class", "layer-project");

        project.append("path")
            .attr("d", function(d) { return area(d.values); })
            .style("fill", function(d, i) {return color(d.key); })
            .on('mouseover', function(d) {
                  config.tooltipShowFunc(this,d.key,d.values[14].value);
                })
            .on('mouseout', function() {
                  config.tooltipHideFunc();
                })
            .on('click',function(d){
                 config.onClick(d.key);
                });
//
//      project.append("path").attr("class","line")
//            .attr("stroke","black")
    //      .attr("stroke-width","1px")
    //      .attr("fill","none")
//            .attr("d", function(d){console.log(d);return line(d.values)})
//      .on('mouseover', function(d) {
//                  config.tooltipShowFunc(this,d.key,d.values[14].value);
//                })
//            .on('mouseout', function() {
//                  config.tooltipHideFunc();
//                });

        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);

        svg.append("g")
            .attr("class", "stacked y axis")
            .call(yAxis);

       svg.select('.stacked.y.axis')
          .style('font-size','12')
	      .append('text')
	      .attr('transform', 'rotate(-90)')
	      .attr('y', -margin.left)
	      .attr('x',-height / 2 + margin.top)
	      .attr('dy', '1em')
	      .style('text-anchor', 'middle')
	      .text(config.yaxis.label);

      svg.select('.x.axis')
          .style('font-size','12')
	      .append('text')
	      .attr('y',2*margin.bottom/3)
	      .attr('x',width/2)
	      .attr('dy', '1em')
	      .style('text-anchor', 'middle')
	      .text("Releases");

      var change = function changeView(view){
        console.log('changinggg');
        if(view === 'line'){
          y.domain([0, d3.max(data, function(d) { return d.value; })]);
          project.selectAll('path').transition().duration(1000)
            .attr("d", function(d){return line(d.values)})
          .transition().duration(1000)
            .style("fill","none")
            .attr("stroke","black")
            .attr("class","line")
            .attr("stroke-width","1px")
            ;

        }else{
          y.domain([0, d3.max(data, function(d) { return d.y+d.y0; })]);
           project.selectAll('path').transition().duration(1000)
           .style("fill", function(d, i) {return color(d.key); }).transition().duration(1000)
            .attr("stroke","none")
           .attr("class","")
            .attr("stroke-width","0px").transition().duration(1000)
            .attr("d", function(d) { return area(d.values); });
        }
      }

      setInterval(function() {
        if(this.selectedView == 'area'){
          change('line');
          this.selectedView = 'line';
        }else{
          change('area');
          this.selectedView = 'area';
        }
      }, 3000);

  };

  StackedAreaChart.prototype.destroy = function(){
      this.data = null;
      d3.select(this.element).selectAll('*').remove();
  };

  dcc.StackedAreaChart = StackedAreaChart;

})();
