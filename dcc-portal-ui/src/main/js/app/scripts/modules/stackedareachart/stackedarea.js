(function() {

  'use strict';

	window.dcc = window.dcc || {};

	var StackedAreaChart = function(data, config){
		this.data = data;
		this.config = config;
    this.selectedView = 'area';
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
      var xReverser = d3.scale.linear()
          .domain([0, width]).range([4,18]);

      var y = d3.scale.linear()
          .range([height, 0]);

      var color = d3.scale.ordinal()
		    .domain(d3.keys(config.colours))
		    .range(d3.values(config.colours));

      var xAxis = d3.svg.axis()
          .scale(x)
          .orient('bottom').innerTickSize(-height).ticks(14);

      var yAxis = d3.svg.axis()
          .scale(y)
          .orient('left').innerTickSize(-width).ticks(config.yaxis.ticks).tickFormat(d3.format('.2s'));

      var stack = d3.layout.stack()
          .offset('zero')
          .values(function(d) { return d.values; })
          .x(function(d) { return d.release; })
          .y(function(d) { return d.value; });

      var nest = d3.nest()
          .key(function(d) { return d.project; });

      var area = d3.svg.area()
          .interpolate('linear')
          .x(function(d) { return x(d.release); })
          .y0(function(d) { return y(d.y0); })
          .y1(function(d) { return y(d.y0 + d.y); });

      var input = ['Area','Line'];
      var form = d3.select(element).append('form');

      var svg = d3.select(element).append('svg')
		    .attr('viewBox','0 0 '+(width + margin.left + margin.right)+
                  ' '+(height + margin.top + margin.bottom))
		    .attr('preserveAspectRatio','xMidYMid')
          .append('g')
          .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

      var layers = stack(nest.entries(data));

      y.domain([0, d3.max(data, function(d) { return d.y+d.y0; })]);

      var line = d3.svg.line()
            .x(function(d) { return x(d.release); })
            .y(function(d) { return y(d.value); });

      var project = svg.selectAll('.layer-project')
            .data(layers)
            .enter().append('g')
            .attr('class', 'layer-project');

      project.append('path')
            .attr('d', function(d) { return area(d.values); })
            .style('fill', function(d) {return color(d.key); })
            .style('sharp-rengering','crispEdges')
            .on('mousemove', function(d) {
                  var coords = d3.mouse(this);
                  config.tooltipShowFunc(this,d.key,d.values[Math.round(xReverser(coords[0]))-4].value);
                })
            .on('mouseout', function() {
                  config.tooltipHideFunc();
                  if(project.selectAll('path').style('fill') === 'none'){
                    project.selectAll('path').style('opacity','1')
                      .attr('stroke', function(d){return color(d.key);});
                  }else{
                    project.selectAll('path')
                      .style('fill', function(d){return color(d.key);})
                      .style('opacity','1')
                      .attr('stroke', 'none');
                  }
                })
            .on('click',function(d){
                  config.onClick(d.key);
                })
            .on('mouseover', function(data){
                if(project.selectAll('path').style('fill') === 'none'){
                  project.selectAll('path').style('opacity',function(d){return d.key === data.key?'1':'0.15';});
//                    .attr('stroke', function(d){
//                      return d.key == data.key?color(d.key):'lightgrey'});
                }else{
                  project.selectAll('path')
                      .style('opacity',function(d){return d.key === data.key?'1':'0.25';})
                 //     .style('fill', function(d){return d.key == data.key?color(d.key):'lightgrey'})
                      .attr('stroke', 'white')
                      .attr('stroke-width','1px');
                }
              });

      svg.append('g')
          .attr('class', 'x axis')
          .attr('transform', 'translate(0,' + height + ')')
          .call(xAxis);

      svg.append('g')
          .attr('class', 'stacked y axis')
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
        .text('Release');

      var change = function changeView(view){
        if(view === 'Line'){
          y.domain([0, d3.max(data, function(d) { return d.value; })]);
          svg.select('.stacked.y.axis').transition().duration(500)
            .call(yAxis);
          project.selectAll('path').transition().duration(500)
            .attr('d', function(d){return line(d.values)};)
            .style('fill','none')
            .attr('stroke', function(d) {return color(d.key); })
            .attr('class','line')
            .attr('stroke-width','4px');

        }else if(view ==='Area'){
          y.domain([0, d3.max(data, function(d) { return d.y+d.y0; })]);
          svg.select('.stacked.y.axis').transition().duration(500)
            .call(yAxis);
          project.selectAll('path').transition().duration(500)
            .attr('d', function(d) { return area(d.values); }).transition()
            .style('fill', function(d) {return color(d.key); })
            .attr('stroke','none')
            .attr('class','')
            .attr('stroke-width','0px');
        }
      };

      form.selectAll('label')
      .data(input).enter()
      .append('label')
      .text(function(d) {return d;})
      .style('margin-left','15px')
      .insert('input')
      .style('margin','5px')
      .attr({
        type: 'radio',
        class: 'shape',
        name: 'mode',
        value: function(d, i) {return i;}
      })
        .on('change',function(e){
          change(e);
        })
        .property('checked', function(d, i) {return i===0;});
    };

  StackedAreaChart.prototype.destroy = function(){
      this.data = null;
      d3.select(this.element).selectAll('*').remove();
    };

  dcc.StackedAreaChart = StackedAreaChart;

})();
