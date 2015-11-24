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
(function ($) {
  'use strict';

  var module = angular.module('icgc.repository.directives', []);

  module.directive('bamstats', function () {
    return {
      replace: true,
      restrict: 'AE',
      scope: {
        bamId: '='
      },
      templateUrl: 'scripts/repository/views/bamiobio.html',
      link: function (scope) {

        $('#length-distribution input[type=\'checkbox\']').change(
          function () {
            var outliers = $(this).parent().hasClass('checked'),
              dataId = $(
                '#length-distribution .chart-chooser .selected')
              .attr('data-id'),
              h = window.sampleStats[dataId],
              d = Object.keys(h).map(function (k) {
                return [+k, +h[k]];
              }),
              selection = d3.select('#length-distribution svg');
            selection.datum(d);
            window.lengthChart(selection, {
              'outliers': outliers
            });
          });

        $('#depth-distribution input[type=\'checkbox\']').change(
          function () {
            goSampling({
              sequenceNames: [getSelectedSeqId()]
            });
          });

        // initialize charts
        // get height width of histogram charts and set viewboxes
        var width = $('#read-coverage-distribution-chart').width();
        var height = $('#read-coverage-distribution-chart').height();
        var dists = document.getElementsByClassName('focus'); // viewboxes
        // need to
        // be
        // set at runtime to get
        // accurate height/width
        for (var i = 0; i < dists.length; i++) {
          dists[i].setAttribute('viewBox', '0 0 ' + width + ' ' + height);
        }

        // setup main window read depth chart
        window.depthChart = movingLineD3('#read-depth-container');

        // setup read coverage histogram chart
        window.readCoverageChart = histogramViewFinderD3().width(width)
          .height(height);
        window.readCoverageChart.yAxis().tickFormat(function (d) {
          return d * 100 + '%';
        });

        function tickFormatter(d) {
          if ((d / 1000000) >= 1){
            d = d / 1000000 + 'M';
          }
          else if ((d / 1000) >= 1){
            d = d / 1000 + 'K';
          }
          return d;
        }
        
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
        window.readRegionChart = bamD3('#read-depth', 0.85, '#33A7E4');

        // setup donut chart
        window.sampleDonutChart = donutD3().radius(100).klass(
          'sampleArc');

        // hold onto stats
        window.sampleStats = undefined;

        // default sampling values
        window.samplingBinSize = 40000;
        window.binNumber = 20;
        window.binSize = 40000;
        window.sampleMultiplier = 1;
        window.sampleMultiplierLimit = 4;

        window.bam = undefined;

        function getUrlParameter(sParam) {
          var sPageURL = window.location.search.substring(1);
          var sURLVariables = sPageURL.split('&');
          for (var i = 0; i < sURLVariables.length; i++) {
            var sParameterName = sURLVariables[i].split('=');
            if (sParameterName[0] === sParam) {
              return sParameterName[1];
            }
          }
        }

        window.sampling = getUrlParameter('sampling');


        scope.chromosomes = [];

        function goBam() {
          // get read depth
          window.bam
            .estimateBaiReadDepth(function (id) {
              scope.chromosomes.push({
                id: id,
                selected: false
              });
              // setup first time and sample
              if ($('.seq-buttons').length === 0) {
                // turn off read depth loading msg
                $('#ref-label').css('visibility', 'visible');
                $('#readDepthLoadingMsg').css('display', 'none');
                // turn on sampling message
                $('.samplingLoader').css('display', 'block');

                // update depth distribution
                window.depthChart.on('brushend', function (x, brush) {
                  var options = {
                    sequenceNames: [getSelectedSeqId()]
                  };
                  if (!brush.empty()) {
                    options.start = parseInt(brush.extent()[0]);
                    options.end = parseInt(brush.extent()[1]);
                    scope.region = {
                      chr: getSelectedSeqId(),
                      'start': options.start,
                      'end': options.end
                    };
                  }

                  goSampling(options);
                });

                if (scope.region && scope.region.chr === id) {
                  scope.setSelectedSeq($('.seq-buttons[data-id=\'' + scope.region.chr + '\']')[0],
                    scope.region.start, scope.region.end);
                }
              }
              scope.$apply();
            });
        }
        var bamId = scope.bamId;
        // check if url to bam file is supplied in url and sample it
        if (bamId !== undefined) {
          // remove https if present
          // if (bamUrl.slice(0,5) == 'https')
          // bamUrl = 'http' + bamUrl.slice(5,bamUrl.length);
          window.bam = new Bam(bamId);
          var r = getUrlParameter('region');
          var region;
          if (r !== undefined) {
            if (r.split(':').length === 1) {
              region = {
                chr: r.split(':')[0]
              };
            } else {
              region = {
                chr: r.split(':')[0],
                start: parseInt(r.split(':')[1].split('-')[0]),
                end: parseInt(r.split(':')[1].split('-')[1])
              };
            }
          }

          goBam(region);
        }

        scope.checkboxCheck = function (event) {
          var checkbox = $(event.target);
          if (checkbox.hasClass('checked')) {
            checkbox.removeClass('icon-ok');
            checkbox.addClass('icon-check-empty');
          } else {
            checkbox.removeClass('icon-check-empty');
            checkbox.addClass('icon-ok');
          }
        };
        
        scope.highlightSelectedSeq = function (chrId) {
          scope.chromosomes.forEach(function (chr) {
            if (chr.id === chrId) {
              chr.selected = true;
            } else {
              chr.selected = false;
            }
          });
          scope.setSelectedSeq(chrId);
        };

        function resetBrush() {
          var brush = window.depthChart.brush();
          var g = d3.selectAll('#depth-distribution .brush');
          brush.clear();
          brush(g);
          // no need to trigger event? since setSelected sq will do it
          // brush.event(g);
        }
        
        function updateTotalReads(totalReads, rand) {
          // update total reads
          console.log(rand + '::::: ' + getSelectedSeqId());
          
          var reads = shortenNumber(totalReads);
          $('#total-reads>#value').html(reads[0]);
          $('#total-reads>#base>#number').html(reads[1] || '');
        }

        scope.setSelectedSeq = function (selected, start, end) {
          var dataId = selected;
          window.depthChart(window.bam.readDepth[dataId]);
          // reset brush
          resetBrush();
          // start sampling
          if (start !== undefined && end !== undefined) {
            goSampling({
              sequenceNames: [dataId],
              'start': start,
              'end': end
            });
            var brush = window.depthChart.brush();
            // set brush region
            d3.select('#depth-distribution .brush').call(
              brush.extent([start, end]));
          } else {
            goSampling({
              sequenceNames: [dataId]
            });
          }
        };

        function goSampling(options) {
          // add default options
          options = $.extend({
            exomeSampling: 'checked' === $('#depth-distribution input')
              .attr('checked'),
            bed: window.bed,
            onEnd: function () {
              NProgress.done();
            }
          }, options);
          var rand = Math.random().toString(36).substr(2, 9);
          // turn on sampling message and off svg
          $('section#middle svg').css('display', 'none');
          $('.samplingLoader').css('display', 'block');
          updateTotalReads(0);
          NProgress.start();
          // update selected stats
          window.bam
            .sampleStats(
              function (data, seq) {
                if (getSelectedSeqId() !== seq) {
                  return;
                }
                // turn off sampling message
                $('.samplingLoader').css('display', 'none');
                $('section#middle svg').css('display', 'block');
                $('div#percents svg').css('padding-left', '9%');
                window.sampleStats = data;
                // update progress bar
                var length, percentDone;
                if (options.start !== null && options.end !== null) {
                  length = options.end - options.start;
                  percentDone = Math
                    .round(((data.last_read_position - options.start) / length) * 100) / 100;
                } else {
                  length = window.bam.header.sq.reduce(
                    function (prev, curr) {
                      if (prev){
                        return prev;
                      }
                      if (curr.name === options.sequenceNames[0]){
                        return curr;
                      }
                    }, false).end;
                  percentDone = Math
                    .round((data.last_read_position / length) * 100) / 100;
                }
                if (NProgress.status < percentDone){
                  NProgress.set(percentDone);
                }
                // update charts
                updatePercentCharts(data, window.sampleDonutChart);
                updateTotalReads(data.total_reads, rand);
                updateHistogramCharts(data, undefined, 'sampleBar');
              }, options);
        }

        scope.sampleMore = function () {
          if (window.sampleMultiplier >= window.sampleMultiplierLimit) {
            window.alert('You\'ve reached the sampling limit');
            return;
          }
          window.sampleMultiplier += 1;
          var options = {
            sequenceNames: [getSelectedSeqId()],
            binNumber: window.binNumber + parseInt(window.binNumber / 4 * window.sampleMultiplier),
            binSize: window.binSize + parseInt(window.binSize / 4 * window.sampleMultiplier)
          };
          if (window.depthChart.brush().extent().length !== 0 &&
              window.depthChart.brush().extent().toString() !== '0,0') {
            options.start = parseInt(window.depthChart.brush().extent()[0]);
            options.end = parseInt(window.depthChart.brush().extent()[1]);
          }
          goSampling(options);
        };

        function getSelectedSeqId() {
          return $('.seq-buttons.selected').attr('data-id');
        }

        function updatePercentCharts(stats, donutChart) {
          var pie = d3.layout.pie().sort(null);
//          var value;

          // update percent charts
          var keys = ['mapped_reads', 'proper_pairs',
                      'forward_strands', 'singletons', 'both_mates_mapped',
                      'duplicates'];
            // var colors = ["rgb(45,143,193)", 'rgb(231, 76, 60)',
            // "rgb(243, 156, 18)",
            // "rgb(155, 89, 182)", "rgb(46, 204, 113)", "rgb(241, 196,
            // 15)"];
          keys.forEach(function (key) {
            var stat = stats[key];
            var data = [stat, stats.total_reads - stat];
            var arc = d3.select('#' + key + ' svg').selectAll('.arc')
              .data(pie(data));
            donutChart(arc);
          });

        }

        function updateHistogramCharts(histograms) {

          // check if coverage is zero
          if (Object.keys(histograms.coverage_hist).length === 0){
            histograms.coverage_hist[0] = '1.0';
          }
          // update read coverage histogram
          var d, selection;
          d = Object.keys(histograms.coverage_hist).filter(
            function (i) {
              return histograms.coverage_hist[i] !== '0';
            }).map(function (k) {
            return [+k, +histograms.coverage_hist[k]];
          });
          selection = d3
            .select('#read-coverage-distribution-chart').datum(d);
          window.readCoverageChart(selection);
          if (histograms.coverage_hist[0] > 0.65) {
            // most likely exome
            // exclude <5 values b\c they are not informative dominating
            // the chart
            var min = 5;
            var max = window.readCoverageChart.globalChart().x()
              .domain()[1];
            window.readCoverageChart.setBrush([min, max]);
          }

          // update read length distribution
          if ($('#length-distribution .selected').attr('data-id') === 'frag_hist'){
            d = Object.keys(histograms.frag_hist).filter(
              function (i) {
                return histograms.frag_hist[i] !== '0';
              }).map(function (k) {
              return [+k, +histograms.frag_hist[k]];
            });
          }
          else {
            d = Object.keys(histograms.length_hist).map(
              function (k) {
                return [+k, +histograms.length_hist[k]];
              });
          }
          // remove outliers if outliers checkbox isn't explicity
          // checked
          var outliers = $('#length-distribution .checkbox').hasClass(
            'checked');
          selection = d3.select('#length-distribution-chart')
            .datum(d);
          window.lengthChart(selection, {
            'outliers': outliers
          });

          // update map quality distribution
          if ($('#mapping-quality-distribution .selected').attr(
              'data-id') === 'mapq_hist') {
            d = Object.keys(histograms.mapq_hist).map(function (k) {
              return [+k, +histograms.mapq_hist[k]];
            });
          }
          else {
            d = Object.keys(histograms.baseq_hist).map(function (k) {
              return [+k, +histograms.baseq_hist[k]];
            });
          }
          selection = d3.select(
            '#mapping-quality-distribution-chart').datum(d);
          window.qualityChart(selection);
        }

        scope.toggleChart = function (event, chartId) {
          var elem = event.target;
          if ($(elem).hasClass('selected')){
            return;
          }
          // toggle selected
          var pair = [elem, $(elem).siblings()[0]];
          $(pair).toggleClass('selected');

          // redraw chart
          var dataId = elem.getAttribute('data-id');
            // var outlier = elem.getAttribute('data-outlier') === 'true'
            // ||
            // elem.getAttribute('data-outlier') == null;
          var h = window.sampleStats[dataId];
          var d = Object.keys(h).map(function (k) {
            return [+k, +h[k]];
          });
          var selection = d3.select($(elem).parent().parent().parent()
            .find('svg')[0]);
          selection.datum(d);
          window[chartId](selection);
        };

        function shortenNumber(num) {
          if (num.toString().length <= 3) {
            return [num];
          }
          else if (num.toString().length <= 6) {
            return [Math.round(num / 1000), 'thousand'];
          }
          else {
            return [Math.round(num / 1000000), 'million'];
          }
        }

      }
    };
  });
})(jQuery);