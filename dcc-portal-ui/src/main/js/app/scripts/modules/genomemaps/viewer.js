/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

'use strict';

angular.module('icgc.modules.genomeviewer', ['icgc.modules.genomeviewer.header', 'icgc.modules.genomeviewer.service']);

angular.module('icgc.modules.genomeviewer').controller('GenomeViewerController', function () {});

angular.module('icgc.modules.genomeviewer').directive('genomeViewer', function (GMService, $location) {
  return {
    restrict: 'A',
    template: '<div id="genome-viewer" style="border:1px solid #d3d3d3;border-top-width: 0px;"></div>',
    replace: true,
    controller: 'GenomeViewerController',
    link: function (scope) {
      var genomeViewer, navigationBar,
        regionObj = new Region({chromosome: 1, start: 1, end: 1}),
        tracks = {},
        done = false;

      function setup() {
        genomeViewer = genomeViewer || new GenomeViewer({
          cellBaseHost: GMService.getConfiguration().cellBaseHost,
          cellBaseVersion: 'v3',
          target: 'genome-viewer',
          width: 1135,
          region: regionObj,
          defaultRegion: regionObj,
          sidePanel: false,
          drawNavigationBar: false,
          navigationBarConfig: {
            componentsConfig: {
              restoreDefaultRegionButton:false,
              regionHistoryButton:false,
              speciesButton:false,
              chromosomesButton:false,
//                karyotypeButton:false,
//                chromosomeButton:false,
//                regionButton:false,
//                zoomControl:false,
              windowSizeControl:false,
              positionControl:false,
//                moveControl:false,
//                autoheightButton:false,
//                compactButton:false,
              searchControl:false
            }
          },
          drawKaryotypePanel: true,
          drawChromosomePanel: true,
          drawRegionOverviewPanel: true,
          karyotypePanelConfig: {
            hidden:true,
            collapsed: false,
            collapsible: false
          },
          chromosomePanelConfig: {
            hidden:true,
            collapsed: false,
            collapsible: false
          },
          version: 'Powered by ' +
            '<a target="_blank" href="http://www.genomemaps.org/">Genome Maps</a>'
        });

        navigationBar = new IcgcNavigationBar({
          zoom: genomeViewer.zoom,
          handlers: {
            'region:change': function (event) {
              genomeViewer._regionChangeHandler(event);
            },
            'region:move': function (event) {
              genomeViewer._regionMoveHandler(event);
            },
            'zoom:change': function (event) {
              genomeViewer._zoomChangeHandler(event);
            }
          }
        });
        genomeViewer.on('region:change', function (event) {
          if (event.sender !== navigationBar) {
            navigationBar.setRegion(event.region);
          }
        });
        genomeViewer.on('region:move', function (event) {
          if (event.sender !== navigationBar) {
            navigationBar.moveRegion(event.region);
          }
        });

        genomeViewer.setNavigationBar(navigationBar);

        // Adding tracks
        tracks.sequence = new SequenceTrack({
          title:'Sequence',
          height: 30,
          visibleRegionSize: 200,
          renderer: new SequenceRenderer({tooltipContainerID: '#gv-application'}),
          dataAdapter: new SequenceAdapter({
            category: 'genomic',
            subCategory: 'region',
            resource: 'sequence',
            species: genomeViewer.species
          })
        });
        genomeViewer.addTrack(tracks.sequence);

        var icgcGeneOverviewRenderer = new FeatureRenderer(FEATURE_TYPES.gene);
        icgcGeneOverviewRenderer.on({
          'feature:click': function (e) {
            scope.$apply(function () {
              $location.path('/genes/' + e.feature.id).search({});
            });
          }
        });
        tracks.icgcGeneOverviewTrack = new IcgcGeneTrack({
          minHistogramRegionSize: 20000000,
          maxLabelRegionSize: 10000000,
          height: 100,
          renderer: icgcGeneOverviewRenderer,
          dataAdapter: new IcgcGeneAdapter({
            resource: 'gene',
            featureCache: {
              chunkSize: 50000
            }
          })
        });
        genomeViewer.addOverviewTrack(tracks.icgcGeneOverviewTrack);


        tracks.icgcGeneTrack = new IcgcGeneTrack({
          title:'ICGC Genes',
          minHistogramRegionSize: 20000000,
          maxLabelRegionSize: 10000000,
          minTranscriptRegionSize: 300000,
          height: 100,
          renderer: new GeneRenderer({
            tooltipContainerID: '#gv-application',
            handlers: {
              'feature:click': function (e) {
                var path = '/genes/' + e.feature[e.featureType === 'gene' ? 'id' : 'geneId'];
                scope.$apply(function () {
                  $location.path(path).search({}).search({});
                });
              }
            }
          }),

          dataAdapter: new IcgcGeneAdapter({
            resource: 'gene',
            featureCache: {
              chunkSize: 50000
            }
          })
        });
        genomeViewer.addTrack(tracks.icgcGeneTrack);

        tracks.icgcMutationsTrack = new IcgcMutationTrack({
          title: 'ICGC Mutations',
          minHistogramRegionSize: 10000,
          maxLabelRegionSize: 3000,
          height: 100,

          renderer: new FeatureRenderer({
            tooltipContainerID: '#gv-application',
            label: function (f) {
              return f.id;
            },
            tooltipTitle: function (f) {
              return '<span class="gmtitle">ICGC mutation' + ' - ' + f.id + '</span>';
            },
            tooltipText: function (f) {
              var consequences = GMService.tooltipConsequences(f.consequences), fi;
              fi = (f.functionalImpact && _.contains(f.functionalImpact, 'High'))? 'High' : 'Low';

              return '<span class="gmkeys">mutation:&nbsp;</span>' + f.mutation + '<br>' +
                     '<span class="gmkeys">reference allele:&nbsp;</span>' + f.refGenAllele + '<br>' +
                     '<span class="gmkeys">mutation type:&nbsp;</span>' + f.mutationType + '<br>' +
                     '<span class="gmkeys">project info:</span><br>' + f.projectInfo.join('<br>') + '<br>' +
                     '<span class="gmkeys">consequences:<br></span>' + consequences + '<br>' +
                     '<span class="gmkeys">source:&nbsp;</span>ICGC<br>' +
                     '<span class="gmkeys">start-end:&nbsp;</span>' + f.start + '-' + f.end + '<br>' +
                     '<span class="gmkeys">functional impact:&nbsp;</span>' + fi;
            },
            color: function (feat) {
              switch (feat.mutationType) {
              case 'single base substitution':
                return 'Chartreuse';
              case 'insertion of <=200bp':
                return 'orange';
              case 'deletion of <=200bp':
                return 'red';
              case 'multiple base substitution (>=2bp and <=200bp)':
                return 'lightblue';
              default:
                return 'black';
              }
            },
            infoWidgetId: 'mutationCds',
            height: 8,
            histogramColor: 'orange',
            handlers: {
              'feature:mouseover': function (e) {
              },
              'feature:click': function (e) {
                scope.$apply(function () {
                  $location.path('/mutations/' + e.feature.id).search({});
                });
              }
            }
          }),

          dataAdapter: new IcgcMutationAdapter({
            resource: 'mutation',
            featureCache: {
              chunkSize: 10000
            }
          })
        });
        genomeViewer.addTrack(tracks.icgcMutationsTrack);

        genomeViewer.draw();
        genomeViewer.enableAutoHeight();

        genomeViewer.karyotypePanel.hide();
        genomeViewer.chromosomePanel.hide();
      }

      scope.$watch('[genes, mutations, tab]', function (params) {
        var genes, mutations, tab;

        genes = params[0];
        mutations = params[1];
        tab = params[2];

        if (!done) {
          if (tab === 'genes' &&
              genes.hasOwnProperty('data') &&
              genes.data.hasOwnProperty('hits') &&
              genes.data.hits.length) {
            done = true;
            var gene = genes.data.hits[0];
            regionObj = new Region({chromosome: gene.chromosome, start: gene.start - 1500, end: gene.end + 1500});
            setup();
          }
          else if (tab === 'mutations' &&
                   mutations.hasOwnProperty('data') &&
                   mutations.data.hasOwnProperty('hits') &&
                   mutations.data.hits.length) {
            done = true;
            var mutation = mutations.data.hits[0];
            regionObj = new Region({
              chromosome: mutation.chromosome,
              start: mutation.start,
              end: mutation.start
            });
            setup();
          }

        }
      }, true);
      
      var fullScreenHandler = function() {
        if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
          jQuery('#gv-fullscreen-ctrl').removeClass('icon-resize-small');
          jQuery('#gv-fullscreen-ctrl').addClass('icon-resize-full');
        } else {
          jQuery('#gv-fullscreen-ctrl').removeClass('icon-resize-full');
          jQuery('#gv-fullscreen-ctrl').addClass('icon-resize-small');
        }
        
        setTimeout( function() {         
          // Ensure our setWidth triggers after the genome viewers interal resize callbacks
          genomeViewer.setWidth(jQuery('.t_gv__navbar').width());}, 100);
      };
      
      if (document.addEventListener){
        document.addEventListener('webkitfullscreenchange', fullScreenHandler);
        document.addEventListener('mozfullscreenchange', fullScreenHandler);
        document.addEventListener('fullscreenchange', fullScreenHandler);
      }

      scope.$on('gv:set:region', function (e, params) {
        if (genomeViewer) {
          genomeViewer.setRegion(params);
          navigationBar._handleZoomSlider( genomeViewer.zoom );
        }
      });
      scope.$on('gv:toggle:panel', function (e, params) {
        var action = params.active ? 'show' : 'hide';
        genomeViewer[params.panel + 'Panel'][action]();
      });
      scope.$on('gv:zoom:set', function (e, val) {
        navigationBar._handleZoomSlider(val);
      });
      scope.$on('gv:reset', function (e) {
        navigationBar._handleRestoreDefaultRegion(e);
      });
      scope.$on('gv:autofit', function (e) {
        genomeViewer.enableAutoHeight();
      });
      scope.$on('$destroy', function() {
        if (genomeViewer) {
          genomeViewer.destroy();
        }
        document.removeEventListener('webkitfullscreenchange');
        document.removeEventListener('mozfullscreenchange');
        document.removeEventListener('fullscreenchange');
      });
    }
  };
});

angular.module('icgc.modules.genomeviewer').directive('gvembed', function (GMService, $location) {
  return {
    restrict: 'E',
    replace: true,
    transclude: true,
    template: '<div id="gv-application" style="border:1px solid #d3d3d3;border-top-width: 0px;"></div>',

    link: function (scope, element, attrs) {
      var genomeViewer, navigationBar, tracks = {};

      function setup(regionObj) {
        genomeViewer = genomeViewer || new GenomeViewer({
          cellBaseHost: GMService.getConfiguration().cellBaseHost,
          cellBaseVersion: 'v3',
          target: 'gv-application',
          width: 1135,
          region: regionObj,
          defaultRegion: regionObj,
          sidePanel: false,
          drawNavigationBar: false,
          navigationBarConfig: {
            componentsConfig: {
              restoreDefaultRegionButton:false,
              regionHistoryButton:false,
              speciesButton:false,
              chromosomesButton:false,
//                karyotypeButton:false,
//                chromosomeButton:false,
//                regionButton:false,
//                zoomControl:false,
              windowSizeControl:false,
              positionControl:false,
//                moveControl:false,
//                autoheightButton:false,
//                compactButton:false,
              searchControl:false
            }
          },
          drawKaryotypePanel: false,
          drawChromosomePanel: false,
          drawRegionOverviewPanel: true,
          karyotypePanelConfig: {
            collapsed: false,
            collapsible: false
          },
          chromosomePanelConfig: {
            collapsed: false,
            collapsible: false
          },
          version: 'Powered by ' +
            '<a target="_blank" href="http://www.genomemaps.org/">Genome Maps</a>'
        });
        window.gv = genomeViewer;

        navigationBar = new IcgcNavigationBar({
          zoom: genomeViewer.zoom,
          handlers: {
            'zoom:change': function (event) {
              genomeViewer._zoomChangeHandler(event);
            },
            'region:change': function (event) {
              genomeViewer._regionChangeHandler(event);
            },
            'restoreDefaultRegion:click': function (event) {
              Utils.setMinRegion(genomeViewer.defaultRegion, genomeViewer.getSVGCanvasWidth());
              event.region = genomeViewer.defaultRegion;
              genomeViewer.trigger('region:change', event);
            }
          }
        });
        genomeViewer.on('region:change', function (event) {
          navigationBar.setRegion(event.region, genomeViewer.zoom);
        });
        genomeViewer.on('region:move', function (event) {
          if (event.sender !== navigationBar) {
            navigationBar.moveRegion(event.region);
          }
        });
        genomeViewer.setNavigationBar(navigationBar);

        // Adding tracks required
        tracks.sequence = new SequenceTrack({
          title:'Sequence',
          height: 30,
          visibleRegionSize: 200,
          renderer: new SequenceRenderer({tooltipContainerID: '#gv-application'}),
          dataAdapter: new SequenceAdapter({
            category: 'genomic',
            subCategory: 'region',
            resource: 'sequence',
            species: genomeViewer.species
          })
        });
        genomeViewer.addTrack(tracks.sequence);

        var featureGeneType = _.extend({}, FEATURE_TYPES.gene);
        var icgcGeneOverviewRenderer = new FeatureRenderer(_.extend(featureGeneType, {tooltipContainerID: '#gv-application'}));

        icgcGeneOverviewRenderer.on({
          'feature:click': function (e) {
            scope.$apply(function () {
              $location.path('/genes/' + e.feature.id).search({});
            });
          }
        });
        tracks.icgcGeneOverviewTrack = new IcgcGeneTrack({
          minHistogramRegionSize: 20000000,
          maxLabelRegionSize: 10000000,
          height: 100,
          renderer: icgcGeneOverviewRenderer,
          dataAdapter: new IcgcGeneAdapter({
            resource: 'gene',
            featureCache: {
              chunkSize: 50000
            }
          })
        });
        genomeViewer.addOverviewTrack(tracks.icgcGeneOverviewTrack);


        tracks.icgcGeneTrack = new IcgcGeneTrack({
          title:'ICGC Genes',
          minHistogramRegionSize: 20000000,
          maxLabelRegionSize: 10000000,
          minTranscriptRegionSize: 300000,
          height: 100,
          renderer: new GeneRenderer({
            tooltipContainerID: '#gv-application',
            handlers: {
              'feature:click': function (e) {
                var path = '/genes/' + e.feature[e.featureType === 'gene' ? 'id' : 'geneId'];
                scope.$apply(function () {
                  $location.path(path).search({}).search({});
                });
              }
            }
          }),

          dataAdapter: new IcgcGeneAdapter({
            resource: 'gene',
            featureCache: {
              chunkSize: 50000
            }
          })
        });
        genomeViewer.addTrack(tracks.icgcGeneTrack);

        tracks.icgcMutationsTrack = new IcgcMutationTrack({
          title: 'ICGC Mutations',
          minHistogramRegionSize: 10000,
          maxLabelRegionSize: 3000,
          height: 100,

          renderer: new FeatureRenderer({
            tooltipContainerID: '#gv-application',
            label: function (f) {
              return f.id;
            },
            tooltipTitle: function (f) {
              return '<span class="gmtitle">ICGC mutation' + ' - ' + f.id + '</span>';
            },
            tooltipText: function (f) {
              var consequences = GMService.tooltipConsequences(f.consequences), fi;
              fi = (f.functionalImpact && _.contains(f.functionalImpact, 'High'))? 'High' : 'Low';

              return '<span class="gmkeys">mutation:&nbsp;</span>' + f.mutation + '<br>' +
                     '<span class="gmkeys">reference allele:&nbsp;</span>' + f.refGenAllele + '<br>' +
                     '<span class="gmkeys">mutation type:&nbsp;</span>' + f.mutationType + '<br>' +
                     '<span class="gmkeys">project info:</span><br>' + f.projectInfo.join('<br>') + '<br>' +
                     '<span class="gmkeys">consequences:<br></span>' + consequences + '<br>' +
                     '<span class="gmkeys">source:&nbsp;</span>ICGC<br>' +
                     '<span class="gmkeys">start-end:&nbsp;</span>' + f.start + '-' + f.end + '<br>' +
                     '<span class="gmkeys">functional impact:&nbsp;</span>' + fi;
            },
            color: function (feat) {
              switch (feat.mutationType) {
              case 'single base substitution':
                return 'Chartreuse';
              case 'insertion of <=200bp':
                return 'orange';
              case 'deletion of <=200bp':
                return 'red';
              case 'multiple base substitution (>=2bp and <=200bp)':
                return 'lightblue';
              default:
                return 'black';
              }
            },
            infoWidgetId: 'mutationCds',
            height: 8,
            histogramColor: 'orange',
            handlers: {
              'feature:mouseover': function (e) {
              },
              'feature:click': function (e) {
                scope.$apply(function () {
                  $location.path('/mutations/' + e.feature.id).search({});
                });
              }
            }
          }),

          dataAdapter: new IcgcMutationAdapter({
            resource: 'mutation',
            featureCache: {
              chunkSize: 10000
            }
          })
        });
        genomeViewer.addTrack(tracks.icgcMutationsTrack);
        genomeViewer.draw();

        genomeViewer.enableAutoHeight();
        
        var fullScreenHandler = function() {
          if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
            jQuery('#gv-fullscreen-ctrl').removeClass('icon-resize-small');
            jQuery('#gv-fullscreen-ctrl').addClass('icon-resize-full');
          } else {
            jQuery('#gv-fullscreen-ctrl').removeClass('icon-resize-full');
            jQuery('#gv-fullscreen-ctrl').addClass('icon-resize-small');
          }
        
          // Ensure our setWidth triggers after the genome viewers interal resize callbacks
          setTimeout( function() {
            genomeViewer.setWidth(jQuery('.t_gv__navbar').width());}, 100);
        };
        
        if (document.addEventListener){
          document.addEventListener('webkitfullscreenchange', fullScreenHandler);
          document.addEventListener('mozfullscreenchange', fullScreenHandler);
          document.addEventListener('fullscreenchange', fullScreenHandler);
        }

        scope.$on('gv:set:region', function (e, params) {
          genomeViewer.setRegion(params);
        });
        scope.$on('gv:zoom:set', function (e, val) {
          navigationBar._handleZoomSlider(val);
        });
        scope.$on('gv:reset', function (e) {
          navigationBar._handleRestoreDefaultRegion(e);
        });
        scope.$on('gv:autofit', function (e) {
          genomeViewer.enableAutoHeight();
        });
        scope.$on('$destroy', function() {
          if (genomeViewer) {
            genomeViewer.destroy();
          }
          document.removeEventListener('webkitfullscreenchange');
          document.removeEventListener('mozfullscreenchange');
          document.removeEventListener('fullscreenchange');
        });

      }

      attrs.$observe('region', function (region) {
        if (!region) {
          return;
        }

        var regionObj = new Region();
        regionObj.parse(region);

        scope.isValidChromosome = GMService.isValidChromosome(regionObj.chromosome);
        if (! scope.isValidChromosome ) {
          return;
        }

        var offset = (regionObj.end - regionObj.start) * 0.05;
        regionObj.start -= offset;
        regionObj.end += offset;
        setup(regionObj);
      });
    }
  };
});
