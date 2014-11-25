// Light angular wrapper for render chart to display highly impacted mutation across projects
(function() {
  'use strict';

  var module = angular.module('icgc.visualization', []);

  module.directive('projectMutationDistribution', function ($location, $filter) {
    return {
      restrict: 'E',
      replace: 'true',
      template: '<div></div>',
      scope: {
        items: '=',
        selected: '='
      },
      link: function($scope, $element) {
        var chart, config;

        config = {
          clickFunc: function(d) {
            $scope.$apply(function() {
              $location.path('/projects/' + d.id).search({});
            });
          },
          tooltipShowFunc: function(elem, data, placement) {

            function getLabel() {
              return 'Project: ' + data.name + '<br>' +
                'Medium: ' + $filter('number')(data.medium) + '<br>' +
                '# Donors: ' + data.donorCount;
            }

            $scope.$emit('tooltip::show', {
              element: angular.element(elem),
              text: getLabel(),
              placement: 'top'
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
              medium = points[Math.floor(count/2) + 1];
            }

            chartData.push({
              id: projectKey,
              name: $filter('define')(projectKey),
              mean: mean,
              medium: medium,
              points: points
            });
          });
          chartData = chartData.sort(function(a, b) {
            //return a.mean - b.mean;
            return a.medium - b.medium;
          });
          return chartData;
        }

        $scope.$watch('items', function(newData) {
          console.log('watcher', newData);
          if (newData && !chart) {
            chart = new dcc.ProjectMutationChart(transform($scope.items), config);
            chart.render( $element[0] );
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
          chart.destroy();
        });
      }
    };
  });
})();
