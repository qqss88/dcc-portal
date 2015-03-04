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
      margin.left = margin.left +20; // to account for padding of ticks
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
          .orient('bottom').innerTickSize(-height).ticks(14).tickPadding(10);

      var yAxis = d3.svg.axis()
          .scale(y)
          .orient('left').innerTickSize(-width).ticks(config.yaxis.ticks).tickFormat(d3.format('.2s')).tickPadding(10);

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

      var hintLine = svg.selectAll('.stackedareahint')
        .data([width*2]).enter()
        .append('line')
        .attr('class','stackedareahint')
        .attr({
          'class':'horizontalGrid',
          'x1' : function(d){ return d;},
          'x2' : function(d){ return d;},
          'y1' : 0,
          'y2' : height,
          'fill' : 'none',
          'shape-rendering' : 'crispEdges',
          'stroke' : 'grey',
          'stroke-width' : '1px'
        });

      var hintHighlighter = svg.selectAll('.stackedareahinthighlight')
        .append('rect')
        .style('opacity','0')
        .attr('class','stackedareahinthighlight')
        .attr({
          'class':'horizontalGrid',
          'y1' : 0,
          'y2' : height,
          'fill' : 'none',
          'shape-rendering' : 'crispEdges',
          'stroke' : 'grey',
          'stroke-width' : '1px'
        });

      project.append('path')
            .attr('d', function(d) { return area(d.values); })
            .style('fill', function(d) {return color(d.key); })
            .style('sharp-rengering','crispEdges')
            .on('mousemove', function(d) {
                  var coords = d3.mouse(this);
                  var release = Math.round(xReverser(coords[0]))-4;
                  var actualRelease = Math.round(xReverser(coords[0]));
                  config.tooltipShowFunc(this,d.key,d.values[release].value, actualRelease);
                  hintLine.transition().duration(100).attr('x1',x(release + 4)).attr('x2',x(actualRelease));
                })
            .on('mouseout', function() {
                  config.tooltipHideFunc();
                  hintLine.transition().duration(500).attr('x1',width*2).attr('x2',width*2);
                  if(project.selectAll('path').style('fill') === 'none'){
                    project.selectAll('path').transition().duration(200).style('opacity','1');
                  }else{
                    project.selectAll('path')
                      .style('opacity','1')
                      .attr('stroke', 'none');
                  }
                })
            .on('click',function(d){
                  config.onClick(d.key);
                })
            .on('mouseover', function(data){
                if(project.selectAll('path').style('fill') === 'none'){
                  project.selectAll('path').transition().duration(200).style('opacity',function(d){return d.key === data.key?'1':'0.15';});
                }else{
                  project.selectAll('path')
                      .style('opacity',function(d){return d.key === data.key?'1':'0.15';})
                      .attr('stroke', 'white')
                      .attr('stroke-width','1px');
                }
              });

      svg.append('g')
          .attr('class', 'stackedarea x axis')
          .attr('transform', 'translate(0,' + height + ')')
          .call(xAxis);

      svg.append('g')
          .attr('class', 'stackedarea y axis')
          .call(yAxis);

      svg.select('.stackedarea.y.axis')
        .style('font-size','12')
        .style('fill','grey')
        .append('text')
        .attr('transform', 'rotate(-90)')
        .attr('y', -margin.left)
        .attr('x',-height / 2 + margin.top)
        .attr('dy', '1em')
        .style('text-anchor', 'middle')
        .text(config.yaxis.label);

      svg.select('.stackedarea.x.axis')
        .style('font-size','12')
        .style('fill','grey')
        .append('text')
        .attr('y',2*margin.bottom/3)
        .attr('x',width/2)
        .attr('dy', '1em')
        .style('text-anchor', 'middle')
        .text('Release');

      var change = function changeView(view){
        if(view === 'Line'){
          y.domain([0, d3.max(data, function(d) { return d.value; })]);
          svg.select('.stackedarea.y.axis').transition().duration(500)
            .call(yAxis);
          project.selectAll('path').transition().duration(500)
            .attr('d', function(d){return line(d.values);})
            .style('fill','none')
            .attr('stroke', function(d) {return color(d.key); })
            .attr('class','line')
            .attr('stroke-width','4px');

        }else if(view ==='Area'){
          y.domain([0, d3.max(data, function(d) { return d.y+d.y0; })]);
          svg.select('.stackedarea.y.axis').transition().duration(500)
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
