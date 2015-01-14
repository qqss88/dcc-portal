(function () {
  'use strict';

  angular.module('icgc.sets', [
    'icgc.sets.directives',
    'icgc.sets.services']
  );
})();


(function () {
  'use strict';

  var module = angular.module('icgc.sets.directives', []);

  module.directive('setOperation', function($location, $timeout, SetOperationService) {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/sets/views/sets.html',
      // template: '<div><span class="canvas"></span></div>',
      link: function($scope, $element, $attrs) {
        var vennDiagram;

        $scope.displaySetOperation = function(item) {
          return SetOperationService.displaySetOperation(item);
        };

        function initVennDiagram() {

          // TEST
          var data3 = [
             [{id: '1', count: 180}],
             [{id: '2', count: 110}],
             [{id: '3', count: 90}],
             [{id: '3', count: 60}, {id: '2', count: 60}],
             [{id: '1', count: 20}, {id: '2', count: 20}],
             [{id: '1', count: 50}, {id: '3', count: 50}],
             [{id: '3', count: 10}, {id: '1', count: 10}, {id: '2', count: 10}]
          ];
          var data2 = [
             [{id: '1', count: 100}],
             [{id: '2', count: 150}],
             [{id: '1', count: 50}, {id: '2', count:50}]
          ];

          var testData = [
             // 1 int
             {
                intersections: ['A'],
                exclusions: ['B', 'C'],
                count: 50
             },
             {
                intersections: ['B'],
                exclusions: ['A', 'C'],
                count: 50
             },
             {
                intersections: ['C'],
                exclusions: ['B', 'A'],
                count: 50
             },
             // 2 int
             {
                intersections: ['A', 'B'],
                exclusions: ['C'],
                count: 50
             },
             {
                intersections: ['A', 'C'],
                exclusions: ['B'],
                count: 50
             },
             {
                intersections: ['A', 'B'],
                exclusions: ['C'],
                count: 50
             },
             // 3 int
             {
                intersections: ['A', 'B', 'C'],
                exclusions: [],
                count: 50
             },
          ];


          var config = {
            urlPath: $location.path() // Because SVG urls are based on <base> tag, we need absolute path
          };
          

          // TEST
          $scope.data = testData;
          $scope.vennData = SetOperationService.transform(testData);


          vennDiagram = new dcc.Venn23($scope.vennData, config);
          vennDiagram.render( $element.find('.canvas')[0]);
          
        }

        // Force a digest cycle first so we can locate canvas, not the best way to do it, but it works
        $timeout(function() {
          initVennDiagram();
        }, 10);
      }
    };
  });
})();


(function () {
  'use strict';

  var module = angular.module('icgc.sets.services', []);

  module.service('SetOperationService', function(Restangular) {
    this.getSetAnalysis = function(id) {
    };

    this.saveNewList = function(sets) {
    };


    /**
     * Transform data array to be consumed by venn-diagram visualization
     */
    this.transform = function(data) {
      var result = [];

      data.forEach(function(set) {
        var subset = [];
        set.intersections.forEach(function(sid) {
          subset.push({
            id: sid,
            count: set.count
          });
        });
        result.push(subset);
      });
      return result;
    };


    /**
     * Transforms internal set prepresentation into UI display format
     */
    this.displaySetOperation = function(item) {
      var displayStr = '';
      var intersections = item.intersections;
      var exclusions = item.exclusions;

      if (intersections.length > 1) {
        displayStr += '( ';
        for (var i=0; i < intersections.length; i++) {
          displayStr += intersections[i];
          if (i < intersections.length-1) {
            displayStr += ' &cap; ';
          }
        }
        displayStr += ' )';
      } else {
        displayStr += intersections[0];
      }

      if (exclusions.length > 1) {
        displayStr += ' - ';
        displayStr += '(';
        for (var i=0; i < exclusions.length; i++) {
          displayStr += exclusions[i];
          if (i < exclusions.length-1) {
            displayStr += ' &cup; ';
          }
        }
        displayStr += ')';
      } else if (exclusions.length > 0) {
        displayStr += ' - ';
        displayStr += exclusions[0];
      }

      return displayStr;
    };

  });

})();


