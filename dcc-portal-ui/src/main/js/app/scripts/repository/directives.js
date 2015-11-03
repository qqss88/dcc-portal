/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
(function($) {
  'use strict';

  var module = angular.module('icgc.repository.directives', []);

  module.directive('bamstats', function() {
            return {
              replace : true, 
              restrict : 'AE',
              scope: {
                bamId: '='
              },
              templateUrl : 'scripts/repository/views/bamiobio.html',
              link : function(scope, element) {
                
                console.log("this is the element: ", element);
                
                $("#length-distribution input[type='checkbox']").change(
                    function() {
                      var outliers = $(this).parent().hasClass("checked"),
                        dataId = $(
                          "#length-distribution .chart-chooser .selected")
                          .attr("data-id"),
                          h = window.sampleStats[dataId],
                          d = Object.keys(h).map(function(k) {
                              return [ +k, +h[k] ]
                          }),
                          selection = d3.select("#length-distribution svg")
                      selection.datum(d);
                      window.lengthChart(selection, {
                        'outliers' : outliers
                      });
                    });

                $("#depth-distribution input[type='checkbox']").change(
                    function() {
                      goSampling({
                        sequenceNames : [ getSelectedSeqId() ]
                      });
                    });

                $("#url-input").keyup(function(event) {
                  if (event.keyCode == 13) {
                    $("#bam-url-go-button").click();
                  }
                });

                $('#url-input').focus(function() {
                  if (this.setSelectionRange) {
                    this.setSelectionRange(7, 7);
                  } else if (this.createTextRange) {
                    // IE
                    var range = this.createTextRange();
                    range.collapse(true);
                    range.moveStart('character', 7);
                    range.moveEnd('character', 7);
                    range.select();
                  }
                });

                $("#id-input").keyup(function(event) {
                  if (event.keyCode == 13) {
                    $("#bam-id-go-button").click();
                  }
                });

                $('#id-input').focus(function() {
                  if (this.setSelectionRange) {
                    this.setSelectionRange(0, 0);
                  } else if (this.createTextRange) {
                    // IE
                    var range = this.createTextRange();
                    range.collapse(true);
                    range.moveStart('character', 0);
                    range.moveEnd('character', 0);
                    range.select();
                  }
                });
                
                $(document).on('click', '.seq-buttons', function(event) {
                    setSelectedSeq(event.toElement);
                });
                  
                
                $(document).on('click', '.sample-more', function(event) {
                   sampleMore(); 
                });
                  
                $(document).on('click', '.length-chart', function(event) {
                    toggleChart(event.toElement, 'lengthChart');
                });
                  
                $(document).on('click', '.quality-chart', function(event) {
                    toggleChart(event.toElement, 'qualityChart');
                });
                  
                // initialize charts
                // get height width of histogram charts and set viewboxes
                var width = $("#read-coverage-distribution-chart").width();
                var height = $("#read-coverage-distribution-chart").height();
                var dists = document.getElementsByClassName("focus") // viewboxes
                                                                      // need to
                                                                      // be
                // set at runtime to get
                // accurate height/width
                for (var i = 0; i < dists.length; i++) {
                  dists[i].setAttribute('viewBox', "0 0 " + width + " "
                      + height)
                }
                
                // setup main window read depth chart
                window.depthChart = movingLineD3("#depth-distribution");

                // setup read coverage histogram chart
                window.readCoverageChart = histogramViewFinderD3().width(width)
                    .height(height);
                window.readCoverageChart.yAxis().tickFormat(function(d) {
                  return d * 100 + '%'
                });

                // setup length histrogram chart
                // window.lengthChart = histogramD3()
                window.lengthChart = histogramViewFinderD3().width(width)
                    .height(height);
                window.lengthChart.xAxis().tickFormat(tickFormatter);
                window.lengthChart.yAxis().tickFormat(tickFormatter);

                // setup quality histogram chart
                window.qualityChart = histogramD3().width(width).height(height);
                window.qualityChart.xAxis().tickFormat(tickFormatter);
                window.qualityChart.yAxis().tickFormat(tickFormatter);

                // setup main panel read chart
                window.readRegionChart = bamD3("#read-depth", 0.85, '#33A7E4');

                // setup donut chart
                window.sampleDonutChart = donutD3().radius(61).klass(
                    "sampleArc");

                // hold onto stats
                window.sampleStats = undefined;

                // default sampling values
                window.samplingBinSize = 40000;
                window.binNumber = 20;
                window.binSize = 40000;
                window.sampleMultiplier = 1;
                window.sampleMultiplierLimit = 4;

                window.bam = undefined;
                window.sampling = getUrlParameter('sampling')
                
                //TODO here
//                var bamUrl = getUrlParameter('bam')
                var bamId = scope.bamId;
//                var bamId = 'http://s3.amazonaws.com/iobio/NA12878/NA12878.autsome.bam'
                // check if url to bam file is supplied in url and sample it
                if (bamId != undefined) {
                  // remove https if present
                  // if (bamUrl.slice(0,5) == 'https')
                  // bamUrl = 'http' + bamUrl.slice(5,bamUrl.length);
                  window.bam = new Bam(bamId);
                  var r = getUrlParameter('region');
                  var region = undefined;
                  if (r != undefined) {
                    if (r.split(":").length == 1)
                      region = {
                        chr : r.split(":")[0]
                      }
                    else
                      region = {
                        chr : r.split(":")[0],
                        start : parseInt(r.split(":")[1].split('-')[0]),
                        end : parseInt(r.split(":")[1].split('-')[1])
                      };
                  }

                  goBam(region);
                }

                function getUrlParameter(sParam) {
                  var sPageURL = window.location.search.substring(1);
                  var sURLVariables = sPageURL.split('&');
                  for (var i = 0; i < sURLVariables.length; i++) {
                    var sParameterName = sURLVariables[i].split('=');
                    if (sParameterName[0] == sParam) {
                      return sParameterName[1];
                    }
                  }
                }

                function removeBedFile() {
                  $("#remove-bedfile-button").css('visibility', 'hidden');
                  $("#default-bedfile-button").css('visibility', 'visible');
                  $("#add-bedfile-button").css('visibility', 'visible');
                  window.bed = undefined;
                  goSampling({
                    sequenceNames : [ getSelectedSeqId() ]
                  });
                }

                function addDefaultBedFile() {
                  var bedurl = '/20130108.exome.targets.bed';

                  // clear brush on read coverage chart
                  window.readCoverageChart.setBrush([ 0, 0 ]);

                  // hide add bed / show remove bed buttons
                  $("#add-bedfile-button").css('visibility', 'hidden');
                  $("#default-bedfile-button").css('visibility', 'hidden');
                  $("#remove-bedfile-button").css('visibility', 'visible');

                  // turn on sampling message and off svg
                  // turn it on here b\c the bed file is so big it takes a while
                  // to download
                  $("section#middle svg").css("display", "none");
                  $(".samplingLoader").css("display", "block");

                  // grab bed from url
                  $.ajax({
                    // XDomainRequest protocol (IE 8 & 9) must be the same
                    // scheme as the
                    // calling page
                    url : bedurl,
                    dataType : 'text'
                  }).done(function(data) {
                    data = data.replace(/chr/g, '');
                    window.bed = data;
                    goSampling({
                      sequenceNames : [ getSelectedSeqId() ]
                    });
                  });

                }

                function openBedFile(event) {

                  if (event.target.files.length != 1) {
                    alert('must select a .bed file');
                    return;
                  }

                  // check file extension
                  var fileType = /[^.]+$/.exec(event.target.files[0].name)[0];
                  if (fileType != 'bed') {
                    alert('must select a .bed file');
                    return;
                  }
                  // clear brush on read coverage chart
                  window.readCoverageChart.setBrush([ 0, 0 ]);

                  // hide add bed / show remove bed buttons
                  $("#add-bedfile-button").css('visibility', 'hidden');
                  $("#default-bedfile-button").css('visibility', 'hidden');
                  $("#remove-bedfile-button").css('visibility', 'visible')

                  // read bed file and store
                  var reader = new FileReader();
                  reader.onload = function(theFile) {
                    window.bed = this.result;
                    goSampling({
                      sequenceNames : [ getSelectedSeqId() ]
                    });
                  }
                  reader.readAsText(event.target.files[0])
                }

                function openBamFile(event) {

                  if (event.target.files.length != 2) {
                    alert('must select both a .bam and .bai file');
                    return;
                  }

                  var fileType0 = /[^.]+$/.exec(event.target.files[0].name)[0];
                  var fileType1 = /[^.]+$/.exec(event.target.files[1].name)[0];

                  if (fileType0 == 'bam' && fileType1 == 'bai') {
                    bamFile = event.target.files[0];
                    baiFile = event.target.files[1];
                  } else if (fileType1 == 'bam' && fileType0 == 'bai') {
                    bamFile = event.target.files[1];
                    baiFile = event.target.files[0];
                  } else {
                    alert('must select both a .bam and .bai file');
                  }

                  window.bam = new Bam(bamFile, {
                    bai : baiFile
                  });
                  goBam();
                }

                function goBam(region) {
                  $("#selectData").css("display", "none");
                  $("#showData").css("visibility", "visible");

                  // get read depth
                  window.bam
                      .estimateBaiReadDepth(function(id, points) {
                        // setup first time and sample
                        if ($('.seq-buttons').length == 0) {
                          // turn off read depth loading msg
                          $("#readDepthLoadingMsg").css("display", "none");
                          // turn on sampling message
                          $(".samplingLoader").css("display", "block");

                          // update depth distribution
                          window.depthChart.on("brushend", function(x, brush) {
                            var options = {
                              sequenceNames : [ getSelectedSeqId() ]
                            };
                            if (!brush.empty()) {
                              options.start = parseInt(brush.extent()[0]);
                              options.end = parseInt(brush.extent()[1]);
                              var region = {
                                chr : getSelectedSeqId(),
                                'start' : options.start,
                                'end' : options.end
                              };
                              setUrlRegion(region);
                              // if (window.bam.sourceType == 'url') {

                              // window.history.pushState({'index.html' :
                              // 'bar'},null,"?bam=" + window.bam.bamUri +
                              // "&region=" +
                              // region);
                              // }
                            } else if (window.bam.sourceType == 'url')
                              setUrlRegion({
                                chr : getSelectedSeqId()
                              });

                            goSampling(options);
                          });

                          // create seq buttons
                          $('#depth-distribution')
                              .append(
                                  "<div style='width: 100%; overflow-x: scroll;' id='seq-list'></div>");
                          var htmlstr = "<span class='seq-buttons' data-id='"
                              + id + "'>" + id + "</span>";
                          $('#seq-list').append(htmlstr);
                          $('#depth-distribution')
                              .append(
                                  "<div id='ref-label' style='font-size:13px;color:rgb(80,80,80);font-weight:400;margin-top:2px; width:100px;margin-left:auto;margin-right:auto'>Reference(s)</div>");

                          if (region == undefined)
                            setSelectedSeq($(".seq-buttons[data-id='" + id
                                + "']")[0]);

                        } else {
                          // create seq buttons
                          var htmlstr = "<span class='seq-buttons' data-id='"
                              + id + "'>" + id + "</span>";
                          $('#seq-list').append(htmlstr);
                        }

                        if (region && region.chr == id)
                          setSelectedSeq($(".seq-buttons[data-id='"
                              + region.chr + "']")[0], region.start, region.end);
                      });

                }

                function resetRegionStats() {
                  window.regionStats = {
                    'Total reads' : {
                      number : 0
                    }
                  };
                }

                function resetBrush() {
                  var brush = window.depthChart.brush();
                  var g = d3.selectAll("#depth-distribution .brush")
                  brush.clear();
                  brush(g);
                  // remove brush region from url
                  setUrlRegion({
                    chr : getSelectedSeqId()
                  });
                  // no need to trigger event? since setSelected sq will do it
                  // brush.event(g);
                }

                function updateTotalReads(totalReads) {
                  // update total reads
                  var reads = shortenNumber(totalReads);
                  $("#total-reads>#value").html(reads[0]);
                  $("#total-reads>#base>#number").html(reads[1] || "");
                }

                function setSelectedSeq (selected, start, end) {
                  console.log('selected: ', selected);
                  var dataId = selected.getAttribute("data-id");
                  $(".seq-buttons.selected").removeClass("selected");
                  $(selected).addClass("selected");
                  window.depthChart(window.bam.readDepth[dataId]);
                  // reset brush
                  resetBrush();
                  setUrlRegion({
                    chr : dataId,
                    'start' : start,
                    'end' : end
                  });
                  // start sampling
                  if (start != undefined && end != undefined) {
                    goSampling({
                      sequenceNames : [ dataId ],
                      'start' : start,
                      'end' : end
                    });
                    var brush = window.depthChart.brush();
                    // set brush region
                    d3.select("#depth-distribution .brush").call(
                        brush.extent([ start, end ]));
                  } else {
                    goSampling({
                      sequenceNames : [ dataId ]
                    });
                  }
                }

                function setUrlRegion(region) {
                  if (window.bam.sourceType == 'url' && region != undefined) {
                    if (region.start != undefined && region.end != undefined) {
                      var regionStr = region.chr + ':' + region.start + '-'
                          + region.end;
                    } else {
                      var regionStr = region.chr;
                    }
                    var extraParams = '';
                    if (window.sampling)
                      extraParams += '&sampling=' + window.sampling
                    window.history.pushState({
                      'index.html' : 'bar'
                    }, null, "?bam=" + window.bam.bamUri + "&region="
                        + regionStr + extraParams);
                  }
                }

                function goSampling(options) {
                  // add default options
                  options = $.extend({
                    exomeSampling : 'checked' == $("#depth-distribution input")
                        .attr("checked"),
                    bed : window.bed,
                    onEnd : function() {
                      NProgress.done();
                    }
                  }, options);

                  // turn on sampling message and off svg
                  $("section#middle svg").css("display", "none");
                  $(".samplingLoader").css("display", "block");
                  updateTotalReads(0);
                  NProgress.start();
                  // update selected stats
                  window.bam
                      .sampleStats(
                          function(data) {
                            // turn off sampling message
                            $(".samplingLoader").css("display", "none");
                            $("section#middle svg").css("display", "block");
                            window.sampleStats = data;
                            // update progress bar
                            if (options.start != null && options.end != null) {
                              var length = options.end - options.start;
                              var percentDone = Math
                                  .round(((data.last_read_position - options.start) / length) * 100) / 100;
                            } else {
                              var length = window.bam.header.sq.reduce(
                                  function(prev, curr) {
                                    if (prev)
                                      return prev;
                                    if (curr.name == options.sequenceNames[0])
                                      return curr;
                                  }, false).end;
                              var percentDone = Math
                                  .round((data.last_read_position / length) * 100) / 100;
                            }
                            if (NProgress.status < percentDone)
                              NProgress.set(percentDone);
                            // update charts
                            updatePercentCharts(data, window.sampleDonutChart)
                            updateTotalReads(data.total_reads);
                            updateHistogramCharts(data, undefined, "sampleBar")
                          }, options);
                }

                function sampleMore() {
                  if (window.sampleMultiplier >= window.sampleMultiplierLimit) {
                    alert("You've reached the sampling limit");
                    return;
                  }
                  window.sampleMultiplier += 1;
                  var options = {
                    sequenceNames : [ getSelectedSeqId() ],
                    binNumber : window.binNumber
                        + parseInt(window.binNumber / 4
                            * window.sampleMultiplier),
                    binSize : window.binSize
                        + parseInt(window.binSize / 4 * window.sampleMultiplier)
                  }
                  if (window.depthChart.brush().extent().length != 0
                      && window.depthChart.brush().extent().toString() != "0,0") {
                    options.start = parseInt(window.depthChart.brush().extent()[0]);
                    options.end = parseInt(window.depthChart.brush().extent()[1]);
                  }
                  goSampling(options);
                }

                function getSelectedSeqId() {
                  return $(".seq-buttons.selected").attr("data-id");
                }

                function updatePercentCharts(stats, donutChart) {
                  var pie = d3.layout.pie().sort(null);
                  var value = undefined;

                  // update percent charts
                  var keys = [ 'mapped_reads', "proper_pairs",
                      "forward_strands", "singletons", "both_mates_mapped",
                      "duplicates" ]
                  // var colors = ["rgb(45,143,193)", 'rgb(231, 76, 60)',
                  // "rgb(243, 156, 18)",
                  // "rgb(155, 89, 182)", "rgb(46, 204, 113)", "rgb(241, 196,
                  // 15)"];
                  keys.forEach(function(key, i) {
                    var stat = stats[key];
                    var data = [ stat, stats['total_reads'] - stat ];
                    var arc = d3.select('#' + key + " svg").selectAll(".arc")
                        .data(pie(data));
                    donutChart(arc);
                  });

                }

                function updateHistogramCharts(histograms, otherMinMax, klass) {

                  // check if coverage is zero
                  if (Object.keys(histograms.coverage_hist).length == 0)
                    histograms.coverage_hist[0] = '1.0';
                  // update read coverage histogram
                  var d = Object.keys(histograms.coverage_hist).filter(
                      function(i) {
                        return histograms.coverage_hist[i] != "0"
                      }).map(function(k) {
                    return [ +k, +histograms.coverage_hist[k] ]
                  });
                  var selection = d3
                      .select("#read-coverage-distribution-chart").datum(d);
                  window.readCoverageChart(selection);
                  if (histograms.coverage_hist[0] > 0.65) {
                    // most likely exome
                    // exclude <5 values b\c they are not informative dominating
                    // the chart
                    var min = 5;
                    var max = window.readCoverageChart.globalChart().x()
                        .domain()[1];
                    window.readCoverageChart.setBrush([ min, max ]);
                  }

                  // update read length distribution
                  if ($("#length-distribution .selected").attr("data-id") == "frag_hist")
                    var d = Object.keys(histograms.frag_hist).filter(
                        function(i) {
                          return histograms.frag_hist[i] != "0"
                        }).map(function(k) {
                      return [ +k, +histograms.frag_hist[k] ]
                    });
                  else
                    var d = Object.keys(histograms.length_hist).map(
                        function(k) {
                          return [ +k, +histograms.length_hist[k] ]
                        });
                  // remove outliers if outliers checkbox isn't explicity
                  // checked
                  var outliers = $("#length-distribution .checkbox").hasClass(
                      "checked");
                  var selection = d3.select("#length-distribution-chart")
                      .datum(d);
                  window.lengthChart(selection, {
                    'outliers' : outliers
                  });

                  // update map quality distribution
                  if ($("#mapping-quality-distribution .selected").attr(
                      "data-id") == "mapq_hist")
                    var d = Object.keys(histograms.mapq_hist).map(function(k) {
                      return [ +k, +histograms.mapq_hist[k] ]
                    });
                  else
                    var d = Object.keys(histograms.baseq_hist).map(function(k) {
                      return [ +k, +histograms.baseq_hist[k] ]
                    });
                  var selection = d3.select(
                      "#mapping-quality-distribution-chart").datum(d);
                  window.qualityChart(selection);
                }

                function toggleChart(elem, chartId) {
                  if ($(elem).hasClass("selected"))
                    return;
                  // toggle selected
                  var pair = [ elem, $(elem).siblings()[0] ];
                  $(pair).toggleClass('selected');

                  // redraw chart
                  var dataId = elem.getAttribute("data-id")
                  // var outlier = elem.getAttribute('data-outlier') === 'true'
                  // ||
                  // elem.getAttribute('data-outlier') == null;
                  var h = window.sampleStats[dataId];
                  var d = Object.keys(h).map(function(k) {
                    return [ +k, +h[k] ]
                  });
                  var selection = d3.select($(elem).parent().parent().parent()
                      .find('svg')[0])
                  selection.datum(d);
                  window[chartId](selection);
                }

                function shortenNumber(num) {
                  if (num.toString().length <= 3)
                    return [ num ];
                  else if (num.toString().length <= 6)
                    return [ Math.round(num / 1000), "thousand" ];
                  else
                    return [ Math.round(num / 1000000), "million" ];
                }

                // function hist2array(hist) {
                // var a = [];
                // for ( var i in hist) {
                // a.push([ i,hist[i] ]);
                // // var position = i;
                // // var instances = hist[i];
                // // for ( var j=0; j < instances; j++)
                // // a.push( parseInt(position) );
                // }
                // return a;
                // }

                function displayBamUrlBox() {
                  if ($('#bam-url').css('visibility') == "hidden") {
                    $('#bam-url').css('position',
                        $("#bam-url-button").position().left);
                    $('#bam-url').css('visibility', 'visible');
                    $('#info').css('visibility', 'hidden');
                    $("#bam-url").children("input").focus();
                  } else {
                    $('#bam-url').css('visibility', 'hidden');
                    $('#info').css('visibility', 'visible');
                    $("#bam-url").children("input").blur();
                  }
                }

                function displayBamIdBox() {
                  if ($('#bam-id').css('visibility') == "hidden") {
                    $('#bam-id').css('visibility', 'visible');
                    $('#info').css('visibility', 'hidden');
                    $("#bam-id").children("input").focus();
                  } else {
                    $('#bam-id').css('visibility', 'hidden');
                    $('#info').css('visibility', 'visible');
                    $("#bam-id").children("input").blur();
                  }
                }

                function openBamUrl() {
                  var url = $("#url-input").val();
                  // remove https if present
                  // if (url.slice(0,5) == 'https')
                  // url = 'http' + url.slice(5,url.length);
                  window.history.pushState({
                    'index.html' : 'bar'
                  }, null, "?bam=" + url);
                  window.bam = new Bam(url);
                  goBam();
                }

//                function openBamId() {
//                  //TODO here
////                  var id = $("#id-input").val();
//                  var id = "c342c10f-65a3-5dbf-8811-d7af835cf606";
//                  // remove https if present
//                  // if (url.slice(0,5) == 'https')
//                  // url = 'http' + url.slice(5,url.length);
//                  
//                  //TODO here
////                  window.history.pushState({
////                    'index.html' : 'bar'
////                  }, null, "?bam=" + id);
//                  window.bam = new Bam(id);
//                  goBam();
//                }

                function tickFormatter(d) {
                  if ((d / 1000000) >= 1)
                    d = d / 1000000 + "M";
                  else if ((d / 1000) >= 1)
                    d = d / 1000 + "K";
                  return d;
                }

              }
            }
          })
})(jQuery);