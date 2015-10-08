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

angular.module('highcharts.services', []);

angular.module('highcharts.services').service('HighchartsService', function ($q, LocationService) {
  var _this = this;

  Highcharts.setOptions({
    chart: {
      backgroundColor: 'transparent'
    },
    colors: [
      '#1693C0', '#24B2E5',
      '#E9931C', '#EDA94A',
      '#166AA2', '#1C87CE',
      '#D33682', '#DC609C',
      '#6D72C5', '#9295D3',
      '#CE6503', '#FB7E09',
      '#1A9900', '#2C0'
    ],
    yAxis: {
      gridLineColor: '#E0E0E0',
      labels: {
        style: {
          fontSize: '10px'
        }
      }
    },
    xAxis: {
      gridLineColor: '#E0E0E0',
      labels: {
        style: {
          fontSize: '10px'
        }
      }
    },
    title: {
      style: {
        color: 'hsl(0, 0%, 20%)',
        fontFamily: '"Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif',
        fontWeight: 300,
        fontSize: '1.3rem'
      }
    },
    tooltip: {
      useHTML: true,
      borderWidth: 0,
      borderRadius: 0,
      backgroundColor: 'transparent',
      shadow: false,
      style: {
        //  fontSize: '1rem'
      }
    },
    legend: {
      enabled: false
    },
    loading: {
      style: {
        backgroundColor: '#f5f5f5'
      },
      labelStyle: {
        top: '40%'
      }
    }
  });

  this.colours = Highcharts.getOptions().colors;

  // this colouring scheme follows a even/odd sequence for picking colours
  this.primarySiteColours = {
    'Liver': this.colours[0],
    'Pancreas': this.colours[2],
    'Kidney': this.colours[4],
    'Head and neck': this.colours[6],
    'Brain': this.colours[8],
    'Blood': this.colours[10],
    'Prostate': this.colours[12],
    'Ovary': this.colours[1],
    'Lung': this.colours[3],
    'Colorectal': this.colours[5],
    'Breast': this.colours[7],
    'Uterus': this.colours[9],
    'Stomach': this.colours[11],
    'Esophagus': this.colours[13],
    'Skin': this.colours[0],
    'Cervix': this.colours[2],
    'Bone': this.colours[4],
    'Bladder': this.colours[6],
    'Mesenchymal': this.colours[8],
    'Nervous System': this.colours[10],
    'Gall Bladder': this.colours[12]
  };


  // new
  this.donut = function (params) {
    var innerPie = {}, innerHits = [], outerHits = [];

    // Check for required parameters
    [ 'data', 'type', 'innerFacet', 'outerFacet', 'countBy'].forEach(function (rp) {
      if (!params.hasOwnProperty(rp)) {
        throw new Error('Missing required parameter: ' + rp);
      }
    });
    if (!params.data) {
      return;
    }
    var countBy = params.countBy,
      innerFacet = params.innerFacet,
      outerFacet = params.outerFacet,
      type = params.type,
      data = params.data;

    // Creates outer ring
    function buildOuterRing(hit, idx) {
      var inner = hit[innerFacet],
        name = hit[outerFacet],
        count = hit[countBy] ? hit[countBy] : 0,
        inArray = inner.indexOf(iName) !== -1,
        inValue = inner === iName;

      if (inArray || inValue) {
        outerHits.push({
          name: name,
          y: count,
          type: type,
          facet: outerFacet,
          color: Highcharts.Color(_this.primarySiteColours[iName]).brighten(0.2).get()
        });
      }
    }

    // Gets the total counts for the inner ring
    function sumInnerPie(hit) {
      var name, count;

      name = hit[innerFacet];
      count = hit[countBy];

      if (!name) {
        name = 'No Data';
      }

      if (!innerPie.hasOwnProperty(name)) {
        innerPie[name] = 0;
      }
      innerPie[name] += count;
    }

    data.forEach(sumInnerPie);

    for (var iName in innerPie) {
      if (innerPie.hasOwnProperty(iName)) {
        innerHits.push({
          name: iName,
          y: innerPie[iName],
          type: type,
          facet: innerFacet,
          color: _this.primarySiteColours[iName]
        });
        data.forEach(buildOuterRing);
      }
    }

    return {
      inner: innerHits,
      outer: outerHits
    };
  };

  this.pie = function (params) {
    var filters, r = [], term, terms;

    // Check for required parameters
    [ 'type', 'facet', 'facets'].forEach(function (rp) {
      if (!params.hasOwnProperty(rp)) {
        throw new Error('Missing required parameter: ' + rp);
      }
    });

    filters = LocationService.filters();

    if (params.facets && params.facets[params.facet].hasOwnProperty('terms')) {
      terms = params.facets[params.facet].terms;
    } else {
      return r;
    }

    terms.forEach(function (item, idx) {
      term = {
        name: item.term,
        y: item.count,
        type: params.type,
        facet: params.facet
      };

      if (term.facet === 'primarySite') {
        term.color = _this.primarySiteColours[term.name];
      }

      // Only shows active terms if facet active
      // has filter type - else include
      if (filters.hasOwnProperty(params.type)) {
        // and facet - else include
        if (filters[params.type].hasOwnProperty(params.facet)) {
          // and active - else don't include
          if (filters[params.type][params.facet].is.indexOf(item.term) !== -1) {
            r.push(term);
          }
        } else {
          r.push(term);
        }
      } else {
        r.push(term);
      }
    });

    return r;
  };

  this.bar = function (params) {
    var r, xAxis, data;

    // Check for required parameters
    [ 'hits', 'xAxis', 'yValue'].forEach(function (rp) {
      if (!params.hasOwnProperty(rp)) {
        throw new Error('Missing required parameter: ' + rp);
      }
    });

    if (!params.hits) {
      return {};
    }

    xAxis = [];
    r = [];

    params.hits.forEach(function (hit) {
      data = {};

      xAxis.push(hit[params.xAxis]);

      data.y = hit[params.yValue];

      if (hit.colour) {
        data.color = hit.colour;
      }

      // Additional options
      if (params.options) {
        if (params.options.linkBase) {
          data.link = params.options.linkBase + hit.id;
        }
      }

      if (data.y > 0) {
        r.push(data);
      }
    });


    return {
      x: xAxis,
      s: r,
      hasData: r.length > 9
    };
  };

});
