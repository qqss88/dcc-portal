// Light angular wrapper for render chart to display highly impacted mutation across projects
(function() {
  'use strict';

  var module = angular.module('icgc.visualization.projectmutationviewer', []);

  module.directive('projectMutationDistribution', function ($location, $filter, $window) {
    return {
      restrict: 'E',
      replace: 'true',
      // TODO: Move this into a template file.
      template: '<div>' +
        '<div class="text-center graph_title">' +
        'Number of Somatic Mutations in Donor\'s Exomes Across Cancer Projects' +
        '<span ng-show="showPlot" class="pull-right"><a href="">' +
        '<small><i class="icon-help" data-tooltip="{{helpText}}" data-tooltip-placement="left"></i>' +
        '</small></a></span>' +
        '</div>' +
        '<div ng-show="! showPlot" class="text-center" style="line-height: {{ defaultGraphHeight }}px;">' +
        '<strong>No mutations reported for the selected <ng-pluralize count="selected.length" when="{\'one\': \'project\', \'other\': \'projects\'}"></ng-pluralize>.</strong></div>' +
        '<div ng-show="showPlot">' +
        '<div class="canvas"></div>' +
        '<div class="text-right">' +
        '<small>Design inspired from Nature: ' +
        '<a href="http://www.nature.com/nature/journal/v499/n7457/full/nature12213.html" ' +
        'target="_blank">doi:10.1038/nature12213</a></small>' +
        '</div></div>' +
        '</div>',
      scope: {
        items: '=',
        selected: '='
      },
      link: function($scope, $element) {
        var chart, config;

        $scope.showPlot = false;

        $scope.helpText = 'Each dot represents the number of somatic mutations per megabase in ' +
          'a given donor\'s exome. Donors are grouped by cancer projects. <br>Horizontal red lines ' +
          'provide the median number of somatic and exomic mutations within each cancer project.';

        $scope.defaultGraphHeight = 230;

        config = {
          height: $scope.defaultGraphHeight,
          width: 950,
          clickFunc: function(d) {
            $scope.$emit('tooltip::hide');
            $scope.$apply(function() {
              $location.path('/projects/' + d.id).search({});
            });
          },
          tooltipShowFunc: function(elem, data) {
            function getLabel() {
              return '<strong>' + data.name + '</strong><br>' +
                'Median: ' + $filter('number')(data.medium) + '<br>' +
                '# Donors: ' + data.donorCount;
            }

            var position = {
              left:elem.getBoundingClientRect().left,
              top:elem.getBoundingClientRect().top + $window.pageYOffset,
              width: elem.getBoundingClientRect().width,
              height: elem.getBoundingClientRect().height
            };
            $scope.$emit('tooltip::show', {
              element: angular.element(elem),
              text: getLabel(),
              placement: 'top',
              elementPosition: position
            });
          },
          tooltipHideFunc: function() {
            $scope.$emit('tooltip::hide');
          }
        };

        function transform(rawData) {
          var chartData = [];

          Object.keys(rawData).forEach(function(projectKey) {
            var projectData = rawData[projectKey];
            var points = [];
            var medium = 0, mean = 0, count = 0;

            Object.keys(projectData).forEach(function(donorKey) {
              var mutationCount = projectData[donorKey];

              mutationCount = mutationCount / 30; // As per equation in DCC-2612

              mean += mutationCount;
              points.push( mutationCount );
              count ++;
            });
            mean = mean/count;
            points = points.sort(function(a, b) {
              return a - b;
            });

            if (count % 2 === 0) {
              medium = 0.5 * (points[count/2] + points[(count/2)+1]);
            } else {

              if (count === 1) {
                medium = points[0];
              } else {
                medium = points[Math.floor(count/2) + 1];
              }
            }

            chartData.push({
              id: projectKey,
              name: projectKey,
              mean: mean,
              medium: medium,
              points: points
            });
          });

          chartData = _.chain(chartData)
            .sortBy( function(d) {
              return d.id;
            })
            .sortBy( function(d) {
              return d.medium;
            })
            .value();

          return chartData;
        }

        $scope.$watch ('items', function (newData) {
          var showPlot = _.some ($scope.selected, function (projectId) {
            return _.has (newData, projectId);
          });

          $scope.showPlot = showPlot;
          if (! showPlot) {return;}

          if (newData && !chart) {
            chart = new dcc.ProjectMutationChart (transform ($scope.items), config);
            chart.render( $element.find('.canvas')[0] );

            if (angular.isDefined($scope.selected)) {
              chart.highlight($scope.selected);
            }
          }
        });

        $scope.$watch('selected', function(newData) {
          if (newData && chart) {
            $scope.selected = newData;
            chart.highlight($scope.selected);
          }
        });

        $scope.$on('$destroy', function() {
          $scope.items = null;
          $scope.selected = null;
          if (chart) {
            chart.destroy();
          }
        });
      }
    };
  });
})();
