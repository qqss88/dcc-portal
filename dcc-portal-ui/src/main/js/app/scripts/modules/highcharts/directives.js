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

angular.module('highcharts', ['highcharts.directives', 'highcharts.services']);

angular.module('highcharts.directives', []);

angular.module('highcharts.directives').directive('pie', function (Facets, $filter) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '=',
      groupPercent: '@'
    },
    template: '<span style="margin: 0 auto">not working</span>',
    link: function ($scope, $element, $attrs) {
      var i, c, formatSeriesData, max, othersGroupCnt, otherGroup, newData, type, facet, chartsDefaults;

      formatSeriesData = function (data) {
        // Default to 5%
        $scope.groupPercent = $scope.groupPercent || 5;

        if (angular.isDefined(data) && data.length > 0) {
          max = 0;
          othersGroupCnt = 0;
          otherGroup = [];
          newData = [];
          type = data[0].type;
          facet = data[0].facet;

          for (i = 0; i < data.length; i++) {
            if (data[i].name !== null) {
              data[i].term = data[i].name;
              max = data[i].y > max ? data[i].y : max;
            } else {
              data[i].term = null;
            }
          }

          // Aggregate the interactive terms together so they can be applied on click event.
          for (i = 0; i < data.length; i++) {
            if (data[i].term !== null) {
              if (data[i].y / max < $scope.groupPercent / 100) {
                othersGroupCnt += data[i].y;
                otherGroup.push(data[i].term);
              } else {
                newData.push(data[i]);
              }
            } else {
              data[i].color = '#E0E0E0';
              newData.push(data[i]);
            }
          }

          // Replace series if 'others' has 2 or more, and less than data.length items
          if (otherGroup.length > 1 && otherGroup.length < data.length) {
            newData.push({
              name: 'Others (' + otherGroup.length + ' ' + $attrs.heading + ')',
              color: '#999',
              y: othersGroupCnt,
              type: type,
              facet: facet,
              term: otherGroup
            });
          }
        }
        return newData;
      };

      chartsDefaults = {
        credits: {enabled: false},
        chart: {
          renderTo: $element[0],
          type: 'pie',
          spacingTop: 2,
          spacingBottom: 2,
          marginTop: 12,
          height: $attrs.height || null,
          width: $attrs.width || null
        },
        title: {
          text: $attrs.heading,
          margin: 5,
          style: {
            fontSize: '1.25rem'
          }
        },
        plotOptions: {
          pie: {
            borderWidth: 1,
            animation: true,
            cursor: 'pointer',
            showInLegend: false,
            events: {
              click: function (e) {
                if (angular.isArray(e.point.term)) {
                  Facets.setTerms({
                    type: e.point.type,
                    facet: e.point.facet,
                    terms: e.point.term
                  });
                } else {
                  Facets.toggleTerm({
                    type: e.point.type,
                    facet: e.point.facet,
                    term: e.point.name
                  });
                }
                $scope.$apply();
              }
            },
            dataLabels: {
              enabled: false,
              distance: 10,
              connectorPadding: 7,
              formatter: function () {
                if (this.point.percentage > 5) {
                  if (this.point.y > 999) {
                    var v = this.point.y.toString();
                    v = v.substring(0, v.length - 3);
                    return $filter('number')(v) + 'k';
                  } else {
                    return $filter('number')(this.point.y);
                  }
                }
              }
            }
          }
        },
        tooltip: {
          shared: true,
          enabled: true,
          formatter: function () {
            var name = this.point.term ? $filter('trans')(this.point.name, true) : 'No Data';
            return '<div class="t_hc_tooltip">' +
                   '<strong>' + name + '</strong><br/>' +
                   Highcharts.numberFormat(this.point.y, 0) + ' ' + this.series.name +
                   '</div>';
          }
        },
        series: [
          {
            type: 'pie',
            size: '90%',
            name: $attrs.label,
            data: formatSeriesData($scope.items)
          }
        ]
      };

      $scope.$watch('items', function (newValue, oldValue) {
        if (!newValue || angular.equals(newValue, oldValue)) {
          return;
        }
        c.series[0].setData(formatSeriesData(newValue), true);
      });

      c = new Highcharts.Chart(chartsDefaults);

      $scope.$on('$destroy', function () {
        c.destroy();
      });
    }
  };
});

angular.module('highcharts.directives').directive('donut', function ($rootScope, $filter, $state, Facets) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '=',
      subTitle: '@'
    },
    template: '<div id="container" style="margin: 0 auto">not working</div>',
    link: function ($scope, $element, $attrs) {
      var c, renderChart, chartsDefaults, filter;

      renderChart = function (settings) {
        if (c) {
          c.destroy();
        }
        c = new Highcharts.Chart(settings);
      };

      chartsDefaults = {
        credits: {enabled: false},
        chart: {
          renderTo: $element[0],
          type: 'pie',
          height: $attrs.height || null,
          width: $attrs.width || null
        },
        title: {
          text: $attrs.heading,
          margin: 25,
          style: {
            fontSize: '1.25rem'
          }
        },
        subtitle: {
          text: '',
          style: {
            color: 'hsl(0, 0%, 60%)'
          }
        },
        plotOptions: {
          pie: {
            allowPointSelect: false,
            borderWidth: 1,
            animation: true,
            cursor: 'pointer',
            showInLegend: false,
            events: {
              click: function (e) {
                if ($attrs.home) {
                  filter = 'filters={"' + e.point.type + '":{"' + e.point.facet + '":{"is"' + '":["' + e.point.name +
                           '"]}}}';
                  var type = e.point.type;
                  var facet = e.point.facet;
                  var name = e.point.name;

                  var filters = {};
                  filters[type] = {};
                  filters[type][facet] = {};
                  filters[type][facet].is = [name];

                  $state.go('projects', {filters: angular.toJson(filters)});
                } else {
                  Facets.toggleTerm({
                    type: e.point.type,
                    facet: e.point.facet,
                    term: e.point.name
                  });
                }
                $scope.$apply();
              }
            }
          }
        },
        tooltip: {
          shared: true,
          enabled: true,
          useHTML: true,
          borderWidth: 0,
          borderRadius: 0,
          backgroundColor: 'none',
          shadow: false,
          formatter: function () {
            return '<div class="t_hc_tooltip">' +
                   '<strong>' + $filter('define')(this.point.name) + '</strong><br/>' +
                   Highcharts.numberFormat(this.point.y, 0) + ' ' + this.series.name +
                   '</div>';
          }
        },
        series: [
          {
            name: $attrs.innerLabel,
            size: '99%',
            dataLabels: {
              enabled: false,
              color: '#fff',
              connectorColor: '#000000',
              zIndex: 0,
              formatter: function () {
                if (this.point.percentage > 5) {
                  return this.point.y;
                }
              }
            }
          },
          {
            name: $attrs.outerLabel,
            size: '120%',
            innerSize: '95%',
            dataLabels: {
              enabled: false,
              overflow: 'justify',
              formatter: function () {
                if (this.point.percentage > 3) {
                  return this.point.y;
                }
              }
            }
          }
        ]
      };

      $scope.$watch('items', function (newValue) {
        var deepCopy, newSettings;
        if (!newValue) {
          return;
        }

        // We need deep copy in order to NOT override original chart object.
        // This allows us to override chart data member and still the keep
        // our original renderTo will be the same
        deepCopy = true;
        newSettings = {};
        jQuery.extend(deepCopy, newSettings, chartsDefaults);
        newSettings.series[0].data = newValue.inner;
        newSettings.series[1].data = newValue.outer;

        newSettings.subtitle.text = $scope.subTitle;

        renderChart(newSettings);
      });

      renderChart(chartsDefaults);

      $scope.$on('$destroy', function () {
        c.destroy();
      });
    }
  };
});

angular.module('highcharts.directives').directive('bar', function ($location) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '='
    },
    template: '<div id="container" style="margin: 0 auto">not working</div>',
    link: function ($scope, $element, $attrs) {
      var c, renderChart, chartsDefaults;

      renderChart = function (settings) {
        if (c) {
          c.destroy();
        }
        c = new Highcharts.Chart(settings);
      };

      chartsDefaults = {
        credits: {enabled: false},
        chart: {
          renderTo: $element[0],
          type: 'column',
          height: $attrs.height || null,
          width: $attrs.width || null
        },
        title: {
          text: $attrs.heading || '',
          margin: 25,
          style: {
            fontSize: '1.25rem'
          }
        },
        subtitle: {
          text: $attrs.subheading || '',
          style: {
            color: 'hsl(0, 0%, 60%)'
          }
        },
        xAxis: {
          labels: {
            rotation: -45,
            align: 'right',
            x: 10,
            formatter: function () {
              if (this.value.length > 15) {
                return this.value.substring(0, 15) + '...';
              } else {
                return this.value;
              }
            }
          },
          categories: angular.isDefined($scope.items) ? $scope.items.x : []
        },
        tooltip: {
          shared: true,
          enabled: true,
          useHTML: true,
          borderWidth: 0,
          borderRadius: 0,
          backgroundColor: 'none',
          shadow: false,
          formatter: function () {
            var num;
            if ($attrs.format && $attrs.format === 'percentage') {
              num = Number(this.y * 100).toFixed(2);
            } else {
              num = this.y;
            }

            return '<div class="t_hc_tooltip">' +
                   '<strong>' + this.x + '</strong><br/>' +
                   num + ' ' + $attrs.ylabel +
                   '</div>';
          }
        },
        yAxis: {
          //allowDecimals:false,
          min: 0,
          showFirstLabel: true,
          showLastLabel: true,
          title: {
            text: $attrs.ylabel,
            style: {
              color: 'hsl(0, 0%, 60%)'
            },
            margin: 25
          },
          labels: {
            enabled: true,
            formatter: function () {
              if ($attrs.format === 'percentage') {
                return this.value * 100;
              }
              return this.value;
            }
          }
        },
        series: angular.isDefined($scope.items) ? [
          {data: $scope.items.s}
        ] : [
          {data: []}
        ],
        plotOptions: {
          column: {
            events: {
              click: function (e) {
                if (e.point.link) {
                  $location.path(e.point.link);
                  $scope.$apply();
                }
              }
            }
          }
        }
      };

      $scope.$watch('items', function (newValue) {
        var deepCopy, newSettings;

        if (!newValue) {
          return;
        }
        // We need deep copy in order to NOT override original chart object.
        // This allows us to override chart data member and still the keep
        // our original renderTo will be the same
        deepCopy = true;
        newSettings = {};
        jQuery.extend(deepCopy, newSettings, chartsDefaults);
        newSettings.xAxis.categories = newValue.x;

        if (!$attrs.format || $attrs.format !== 'percentage') {
          if (newSettings.yAxis) {
            newSettings.yAxis.allowDecimals = false;
          }
        }

        newSettings.series = [
          {data: newValue.s}
        ];
        renderChart(newSettings);
      }, true);

      renderChart(chartsDefaults);

      $scope.$on('$destroy', function () {
        c.destroy();
      });
    }
  };
});

angular.module('highcharts.directives').directive('stacked', function ($location) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '=',
      subTitle: '@'
    },
    template: '<div id="container" style="margin: 0 auto">not working</div>',
    link: function ($scope, $element, $attrs) {
      var c, chartsDefaults;

      function renderChart(settings) {
        if (c) {
          c.destroy();
        }
        c = new Highcharts.Chart(settings);
        if (!settings.xAxis.categories) {
          c.showLoading('<i class="icon-spinner icon-spin"></i> Loading...');
        }
        if (settings.xAxis.categories && settings.xAxis.categories.length === 0) {
          c.showLoading('No Data');
        }
      }

      chartsDefaults = {
        credits: {enabled: false},
        loading: {
          style: {
            backgroundColor: null
          },
          labelStyle: {
            fontSize: '1.25rem'
          }
        },
        chart: {
          zoomType: 'x',
          renderTo: $element[0],
          type: 'column',
          height: $attrs.height || null,
          width: $attrs.width || null
        },
        subtitle: {
          text: '',
          style: {
            color: 'hsl(0, 0%, 60%)'
          }
        },
        title: {
          text: 'Top 20 Mutated Genes with High Functional Impact SSMs',
          style: {
            fontSize: '1.25rem'
          }
        },
        xAxis: {
          labels: {
            rotation: -45,
            align: 'right',
            x: 5
          },
          categories: []
        },
        yAxis: {
          allowDecimals: false,
          min: 0,
          title: {
            text: 'Donors Affected',
            margin: 30,
            style: {
              color: 'hsl(0, 0%, 60%)'
            }
          },
          labels: {
            enabled: true,
            formatter: function () {
              return this.value;
            }
          }
        },
        tooltip: {
          formatter: function () {
            var donors = this.y;

            return '<div class="t_hc_tooltip">' +
                   '<strong>' + this.series.name + '</strong><br/>' +
                   donors + ' ' + ' donors affected' +
                   '</div>';
          }
        },
        plotOptions: {
          column: {
            cursor: 'pointer',
            stacking: 'normal',
            borderWidth: 0,
            dataLabels: {
              enabled: false
            },
            events: {
              click: function (e) {
                $location.path('/genes/' + e.point.gene_id).search({});
                $scope.$apply();
              }
            }
          }
        }
      };

      $scope.$watch('items', function (newValue, oldValue) {
        var deepCopy, newSettings, dataCopy;
        if (!newValue) {
          return;
        }

        // We need deep copy in order to NOT override original chart object.
        // This allows us to override chart data member and still the keep
        // our original renderTo will be the same
        deepCopy = true;
        newSettings = {};
        dataCopy = {};

        jQuery.extend(deepCopy, newSettings, chartsDefaults);

        // Highcharts seem to change the internals, so we want to make
        // a deep copy to prevent angular watchers from firing over and over
        jQuery.extend(true, dataCopy, newValue);

        newSettings.xAxis.categories = dataCopy.x;
        newSettings.series = dataCopy.s;
        newSettings.subtitle.text = $scope.subTitle;

        renderChart(newSettings);
      }, true);

      renderChart(chartsDefaults);

      $scope.$on('$destroy', function () {
        c.destroy();
      });
    }
  };
});
