(function() {

  'use strict';

	window.dcc = window.dcc || {};

	var StackedAreaChart = function(data, config){
	//	this.data = data;
		this.config = config;
	};

	StackedAreaChart.prototype.render = function(element){
      var go  = function (csv){
        var lines=csv.split("!");
        var result = [];
        for(var i=0;i<lines.length;i++){
            var currentline=lines[i].split(",");
            for(var j=1;j<currentline.length;j++){
              var release = j+3;
              var value = currentline[j] ? currentline[j] : 0;
                result.push({"project":currentline[0],"release":release.toString(),"value":parseInt(value)});
            }
        }
        return result; //JSON
      }
      var data  = (go("BRCA-US,0,0,358,358,358,860,865,865,869,881,881,977,984,1019,1045!BRCA-UK,24,24,24,24,124,124,117,117,117,117,141,141,141,141,141!GBM-US,0,0,460,460,460,564,565,565,565,565,565,577,579,583,589!LGG-US,0,0,0,0,0,144,161,161,208,208,208,266,305,402,441!PBCA-DE,0,0,0,0,125,125,125,135,126,126,306,306,382,382,301!KIRC-US,0,0,179,179,179,502,502,502,502,502,502,505,507,515,525!KIRP-US,0,0,17,17,17,95,95,95,103,104,104,138,164,202,223!OV-US,0,0,524,524,524,576,576,576,575,575,575,575,574,582,582!LUAD-US,0,0,44,44,44,291,292,292,333,395,395,461,466,473,485!LUSC-US,0,0,59,59,59,279,279,279,327,327,327,408,411,422,431!LAML-US,0,0,188,188,188,200,200,200,200,200,200,200,200,200,200!CLLE-ES,0,4,4,109,109,109,177,177,264,264,264,264,264,264,309!CMDI-UK,0,0,0,0,129,129,129,129,129,129,129,129,129,129,129!MALY-DE,0,0,0,0,0,0,0,10,23,23,53,53,53,53,53!HNSC-US,0,0,0,0,0,283,293,293,315,327,327,368,408,422,494!THCA-US,0,0,0,0,0,158,193,193,218,424,424,487,488,494,502!LINC-JP,1,1,11,11,11,11,11,161,161,161,213,244,244,244,244!LICA-FR,0,0,0,0,125,125,125,125,125,30,30,153,151,126,273!LIRI-JP,1,1,14,14,64,64,64,104,104,104,45,158,208,208,260!LIHC-US,0,0,0,0,0,55,62,62,62,73,73,127,151,176,317!UCEC-US,0,0,70,70,70,425,451,451,451,462,462,480,482,490,513!COAD-US,0,0,207,207,207,422,423,423,423,423,423,435,437,442,451!PACA-AU,5,5,5,5,67,67,67,141,141,141,427,351,468,462,431!PACA-CA,5,5,26,35,44,44,44,75,75,71,138,138,148,148,248!STAD-US,0,0,83,83,83,155,159,159,179,198,198,325,328,359,420!GACA-CN,0,0,0,10,10,10,10,10,10,10,10,0,9,9,9!READ-US,0,0,69,69,69,168,168,168,168,168,168,168,168,171,169!PRAD-US,0,0,0,0,0,0,127,127,148,156,156,175,199,263,384!PRAD-CA,0,0,0,0,0,0,0,10,10,10,10,10,10,10,124!EOPC-DE,0,0,0,0,0,0,0,9,9,9,17,17,17,17,11!PRAD-UK,0,0,0,0,0,0,0,2,2,2,2,0,3,21,10!SKCM-US,0,0,0,0,0,0,129,129,169,211,211,320,341,368,433!BLCA-US,0,0,0,0,0,65,68,68,117,140,140,186,198,218,299!CESC-US,0,0,0,0,0,26,31,31,32,40,40,82,127,201,261!ALL-US,,,,,,,,,,,229,0,420,420,207!BOCA-UK,,,,,,,,,,,69,69,69,69,69!ESAD-UK,,,,,,,,,,,22,16,16,95,100!ORCA-IN,,,,,,,,,,,50,50,50,50,106!LAML-KR,,,,,,,,,,,55,0,97,94,67!PAAD-US,,,,,,,,,,,26,58,73,93,148!PAEN-AU,,,,,,,,,,,,22,49,50,66!RECA-EU,,,,,,,,,,,,122,122,122,122!THCA-SA,,,,,,,,,,,,15,15,15,15!NBL-US,,,,,,,,,,,,389,573,573,42!RECA-CN,,,,,,,,,,,,10,10,10,10!OV-AU,,,,,,,,,,,,93,93,93,93!ESCA-CN,,,,,,,,,,,,,88,88,88!LUSC-KR,,,,,,,,,,,,,111,111,36!BLCA-CN,,,,,,,,,,,,,103,103,103!LIAD-FR,,,,,,,,,,,,,,30,30!BOCA-FR,,,,,,,,,,,,,,,98!COCA-CN,,,,,,,,,,,,,,,49!LUSC-CN,,,,,,,,,,,,,,,10!PACA-IT,,,,,,,,,,,,,,,37!LIHM-FR,,,,,,,,,,,,,,,4"));
      var config = this.config;

      var margin = config.margin,//{top: 20, right: 30, bottom: 30, left: 40},
          width = config.width - margin.left - margin.right,
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
          .orient("left").ticks(config.yaxis.ticks);



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

      var svg = d3.select(element).append("svg")
          .attr("width", width + margin.left + margin.right)
          .attr("height", height + margin.top + margin.bottom)
          .append("g")
          .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var layers = stack(nest.entries(data));

        y.domain([0, d3.max(data, function(d) { return d.y0 + d.y; })]);

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
          'stroke-width' :
           function(d){
              if(y(d) === max || y(d) ===0){
                return '0px';
              }
              return '1px';
            }
        });

        svg.selectAll(".layer")
            .data(layers)
          .enter().append("path")
            .attr("class", "layer")
            .attr("d", function(d) { return area(d.values); })
            .style("fill", function(d, i) {return color(d.key); })
            .on('mouseover', function(d) {
          console.log(d);
                  config.tooltipShowFunc(this,d.key,d.values[14].value);
                })
            .on('mouseout', function() {
                  config.tooltipHideFunc();
                })
            .on('click',function(d){
                  config.onClick(d.key);
                });

        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);

        svg.append("g")
            .attr("class", "y axis")
            .call(yAxis);

  };

	StackedAreaChart.prototype.destroy = function(){
//		this.data = null;
        d3.select(this.element).selectAll('*').remove();
	};

	dcc.StackedAreaChart = StackedAreaChart;



})();
