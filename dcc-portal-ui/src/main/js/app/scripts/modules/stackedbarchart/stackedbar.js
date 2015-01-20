(function() {

  'use strict';

	window.dcc = window.dcc || {};
	
	var colours = [
    '#1693C0', '#24B2E5',
    '#E9931C', '#EDA94A',
    '#166AA2', '#1C87CE',
    '#D33682', '#DC609C',
    '#6D72C5', '#9295D3',
    '#CE6503', '#FB7E09',
    '#1A9900', '#2C0'
  ];
	
	var primarySiteColours = {
	    'Liver': colours[0],
	    'Pancreas': colours[2],
	    'Kidney': colours[4],
	    'Head and neck': colours[6],
	    'Brain': colours[8],
	    'Blood': colours[10],
	    'Prostate': colours[12],
	    'Ovary': colours[1],
	    'Lung': colours[3],
	    'Colorectal': colours[5],
	    'Breast': colours[7],
	    'Uterus': colours[9],
	    'Stomach': colours[11],
	    'Esophagus': colours[13],
	    'Skin': colours[0],
	    'Cervix': colours[2],
	    'Bone': colours[4],
	    'Bladder': colours[6]
	  };
	
    /*
	*   Brightens d3 colors in the same way Highcharts does to maintain consistency
	*/
	function brighten(color, alpha){
	    var c = [color.r, color.g, color.b];
	    for (var i = 0; i < 3; i++) {
        c[i] += Math.round(alpha * 255);
        if (c[i] < 0){
          c[i] = 0;
        }else if(c[i] > 255){
          c[i] = 255;
        }
	    }
	    return d3.rgb(c[0],c[1],c[2]);
    }
	
	var projectColours = {
	    'LIRI-JP': brighten(d3.rgb(primarySiteColours.Liver), 0.1),
	    'LINC-JP': brighten(d3.rgb(primarySiteColours.Liver), 0.2),
	    'LIHC-US': brighten(d3.rgb(primarySiteColours.Liver), 0.3),
	    'LICA-FR': brighten(d3.rgb(primarySiteColours.Liver), 0.4),
	    'LIAD-FR': brighten(d3.rgb(primarySiteColours.Liver), 0.5),
	    'PAEN-AU': brighten(d3.rgb(primarySiteColours.Pancreas), 0.1),
	    'PACA-CA': brighten(d3.rgb(primarySiteColours.Pancreas), 0.2),
	    'PACA-AU': brighten(d3.rgb(primarySiteColours.Pancreas), 0.3),
	    'PAAD-US': brighten(d3.rgb(primarySiteColours.Pancreas), 0.4),
	    'RECA-EU': brighten(d3.rgb(primarySiteColours.Kidney), 0.1),
	    'RECA-CN': brighten(d3.rgb(primarySiteColours.Kidney), 0.2),
	    'KIRP-US': brighten(d3.rgb(primarySiteColours.Kidney), 0.3),
	    'KIRC-US': brighten(d3.rgb(primarySiteColours.Kidney), 0.4),
	    'THCA-US': brighten(d3.rgb(primarySiteColours['Head and neck']), 0.1),
	    'THCA-SA': brighten(d3.rgb(primarySiteColours['Head and neck']), 0.2),
	    'ORCA-IN': brighten(d3.rgb(primarySiteColours['Head and neck']), 0.3),
	    'HNSC-US': brighten(d3.rgb(primarySiteColours['Head and neck']), 0.4),
	    'PBCA-DE': brighten(d3.rgb(primarySiteColours.Brain), 0.1),
	    'NBL-US': brighten(d3.rgb(primarySiteColours.Brain), 0.2),
	    'LGG-US': brighten(d3.rgb(primarySiteColours.Brain), 0.3),
	    'GBM-US': brighten(d3.rgb(primarySiteColours.Brain), 0.4),
	    'MALY-DE': brighten(d3.rgb(primarySiteColours.Blood), 0.1),
	    'LAML-US': brighten(d3.rgb(primarySiteColours.Blood), 0.2),
	    'CMDI-UK': brighten(d3.rgb(primarySiteColours.Blood), 0.3),
	    'CLLE-ES': brighten(d3.rgb(primarySiteColours.Blood), 0.4),
	    'ALL-US': brighten(d3.rgb(primarySiteColours.Blood), 0.5),
	    'LAML-KR': brighten(d3.rgb(primarySiteColours.Blood), 0.6),
	    'PRAD-US': brighten(d3.rgb(primarySiteColours.Prostate), 0.1),
	    'PRAD-CA': brighten(d3.rgb(primarySiteColours.Prostate), 0.2),
	    'EOPC-DE': brighten(d3.rgb(primarySiteColours.Prostate), 0.3),
	    'PRAD-UK': brighten(d3.rgb(primarySiteColours.Prostate), 0.4),
	    'OV-US': brighten(d3.rgb(primarySiteColours.Ovary), 0.1),
	    'OV-AU': brighten(d3.rgb(primarySiteColours.Ovary), 0.2),
	    'LUSC-US': brighten(d3.rgb(primarySiteColours.Lung), 0.1),
	    'LUAD-US': brighten(d3.rgb(primarySiteColours.Lung), 0.2),
	    'LUSC-KR': brighten(d3.rgb(primarySiteColours.Lung), 0.3),
	    'READ-US': brighten(d3.rgb(primarySiteColours.Colorectal), 0.1),
	    'COAD-US': brighten(d3.rgb(primarySiteColours.Colorectal), 0.2),
	    'BRCA-US': brighten(d3.rgb(primarySiteColours.Breast), 0.1),
	    'BRCA-UK': brighten(d3.rgb(primarySiteColours.Breast), 0.2),
	    'UCEC-US': brighten(d3.rgb(primarySiteColours.Uterus), 0.1),
	    'STAD-US': brighten(d3.rgb(primarySiteColours.Stomach), 0.1),
	    'GACA-CN': brighten(d3.rgb(primarySiteColours.Stomach), 0.2),
	    'SKCM-US': brighten(d3.rgb(primarySiteColours.Skin), 0.1),
	    'ESAD-UK': brighten(d3.rgb(primarySiteColours.Esophagus), -0.1),
	    'ESCA-CN': brighten(d3.rgb(primarySiteColours.Esophagus), -0.2),
	    'CESC-US': brighten(d3.rgb(primarySiteColours.Cervix), -0.1),
	    'BOCA-UK': brighten(d3.rgb(primarySiteColours.Bone), -0.1),
	    'BLCA-US': brighten(d3.rgb(primarySiteColours.Bladder), -0.1),
	    'BLCA-CN': brighten(d3.rgb(primarySiteColours.Bladder), -0.2)
	  };
	
	/* Create the stacked bar chart using d3 */
	
	var StackedBarChart = function(data, config){
		this.data = data;
		this.margin = config.margin || {top: 60, right: 20, bottom: 50, left: 50};
		this.width =  (config.width || 600) - this.margin.left - this.margin.right;
		this.height = (config.height || 300) - this.margin.top - this.margin.bottom;
	};
	
	StackedBarChart.prototype.render = function(element){
		this.element = element;
		
		// the scale for the x axis
		var x = d3.scale.ordinal()
		    .rangeRoundBands([0, this.width], 0.2);
		
		// the scale for the y axis
		var y = d3.scale.linear()
		    .rangeRound([this.height, 0]);
		
		// creates a color scale to turn the project id into its project color
		var color = d3.scale.ordinal()
		    .domain(d3.keys(projectColours))
		    .range(d3.values(projectColours));
		
		// create the x and y axis
		var xAxis = d3.svg.axis()
		    .scale(x)
		    .orient('bottom');
		var yAxis = d3.svg.axis()
		    .scale(y)
		    .orient('left').ticks(4);
		
		// create the svg
		var svg = d3.select(element).append('svg')
		    .attr('id','svgstackedbar')
		    .attr('viewBox','0 0 '+(this.width+this.margin.left+this.margin.right)+
                  ' '+(this.height + this.margin.top + this.margin.bottom))
		    .attr('preserveAspectRatio','xMidYMid')
		    .append('g')
		    .attr('transform', 'translate(' + this.margin.left + ',' + this.margin.top + ')');

		
		// create a div to function as the tooltip
		var div = d3.select(element).append('div')
		    .attr('class', 'stacked tooltip')
		    .style('opacity', 0);
		
	  //for each gene, create an array of donors and get the total affected donors count
	  this.data.forEach(function(d) {
	    var y0 = 0;
	    d.donors = d.uiFIProjects
	     .sort(function(a,b){return a.count-b.count;}) //sort so biggest is on top
	     .map(function(p) { return {name: p.id, y0: y0, y1: y0 += p.count,gene:d.symbol,geneLink:d.id,label:p.name}; });
	    d.total = d.donors[d.donors.length - 1].y1;
	  });
	
	  //sort so in descending order
	  this.data.sort(function(a, b) { return b.total - a.total; });
	
	  //create domain of x scale based off data
	  x.domain(this.data.map(function(gene) { return gene.symbol; }));
	  y.domain([0, d3.max(this.data, function(d) { return d.total; })]);
	
	  // add the x axis with tilted labels
	  svg.append('g')
	      .attr('class', 'stacked x axis')
	      .attr('transform', 'translate(0,' + this.height + ')')
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
	      .attr('y', -this.margin.left+5)
	      .attr('x',-this.height / 2)
	      .attr('dy', '1em')
	      .style('text-anchor', 'middle')
	      .text('Donors Affected');
	
	 //add gridlines
    var max = this.height;
    svg.selectAll('line.horizontalGrid').data(y.ticks(4)).enter()
     .append('line')
     .attr(
      {
      'class':'horizontalGrid',
      'x1' : '-5',
      'x2' : this.width,
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
    var gene = svg.selectAll('.gene')
        .data(this.data)
        .enter().append('g')
        .attr('class', 'stacked g');

     // create the columns
    gene.selectAll('rect')
        .data(function(d) { return d.donors; }) //goes through each of the projects of each gene
         .enter().append('rect')
         .style('fill', function(d) { return color(d.name); })
        .attr('width', x.rangeBand())
        .attr('x',function(d){return x(d.gene);})
        .attr('y', function(d) { return y(d.y1); })
        .attr('height', function(d) { return y(d.y0) - y(d.y1); })
        .on('mouseover', function(d) {
              div.transition()
                  .duration(20)
                  .style('opacity', 0.95);
              div.html('<strong>'+d.label+'</strong><br>'+(d.y1-d.y0)+' Donors Affected')
                  .style('left', (d3.event.layerX) + 'px')
                  .style('top', (d3.event.layerY - 40) + 'px');
            })
        .on('mouseout', function() {
              div.transition()
                  .duration(20)
                  .style('opacity', 0);
            })
        .on('click',function(d){   //link to the gene page on mouse click
              window.location.href='/genes/'+d.geneLink;
            });
  };

	StackedBarChart.prototype.destroy = function(){
		this.data = null;
    d3.select(this.element).selectAll('*').remove();
	};
	
	dcc.StackedBarChart = StackedBarChart;
})();