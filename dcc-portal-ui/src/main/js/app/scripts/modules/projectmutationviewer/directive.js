// Light angular wrapper for render chart to display highly impacted mutation across projects
(function() {
  'use strict';

  var module = angular.module('icgc.visualization', []);

  module.directive('projectMutationDistribution', function () {
    return {
      restrict: 'E',
      replace: 'true',
      template: '<span></span>',
      scope: {
        items: '=',
        selected: '=',
        title: '@'
      },
      link: function($scope, $element) {
        var chart, config;

        config = {
          title: $scope.title || 'The prevalence of somatic mutations across cancer projects'
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
