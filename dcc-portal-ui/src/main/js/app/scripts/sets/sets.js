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
      link: function($scope, $element, $attrs) {
        var vennDiagram;

        $scope.current = [];
        $scope.selected = [];

        $scope.toggle = function(ids) {
          var existIdex = _.findIndex($scope.selected, function(subset) {
            return SetOperationService.isEqual(ids, subset);
          });

          if (existIdex === -1) {
            $scope.selected.push(ids);
          } else {
            // FIXME: this is repeated, move out
            _.remove($scope.selected, function(subset) {
              return SetOperationService.isEqual(ids, subset);
            });
            if (SetOperationService.isEqual(ids, $scope.current) === true) {
              $scope.current = [];
            };
          }
          vennDiagram.toggle(ids);
        };


        $scope.isSelected = function(ids) {
          var existIdex = _.findIndex($scope.selected, function(subset) {
            return SetOperationService.isEqual(ids, subset);
          });
          return existIdex >= 0;
        };


        $scope.checkRow = function(ids) {
          var existIdex = _.findIndex($scope.selected, function(subset) {
            return SetOperationService.isEqual(ids, subset);
          });

          if (existIdex >= 0) {
            return true;
          }
          return SetOperationService.isEqual($scope.current, ids);
        };

        $scope.displaySetOperation = function(item) {
          return SetOperationService.displaySetOperation(item);
        };

        $scope.tableMouseEnter = function(ids) {
          vennDiagram.toggleHighlight(ids, true);
          $scope.current = ids;
        };

        $scope.tableMouseOut = function(ids) {
          vennDiagram.toggleHighlight(ids, false);
          $scope.current = [];
        };

        function initVennDiagram() {
          var testData = [
             // 1 int
             {
                intersections: ['A'],
                exclusions: ['B', 'C'],
                count: 150
             },
             {
                intersections: ['B'],
                exclusions: ['A', 'C'],
                count: 50
             },
             {
                intersections: ['C'],
                exclusions: ['B', 'A'],
                count: 5
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
                intersections: ['B', 'C'],
                exclusions: ['A'],
                count: 50
             },
             // 3 int
             {
                intersections: ['A', 'B', 'C'],
                exclusions: [],
                count: 150
             },
          ];


          var config = {
            // Because SVG urls are based on <base> tag, we need absolute path
            urlPath: $location.path(),

            mouseoverFunc: function(d) {
              $scope.$apply(function() {
                $scope.current = d.data;
              });
            },

            mouseoutFunc: function(d) {
              $scope.$apply(function() {
                $scope.current = [];
              });
            },

            clickFunc: function(d) {
              $scope.$apply(function() {
                if (d.selected === true) {
                  $scope.selected.push(d.data);
                } else {
                  _.remove($scope.selected, function(subset) {
                    return SetOperationService.isEqual(d.data, subset);
                  });
                  if (SetOperationService.isEqual(d.data, $scope.current) === true) {
                    $scope.current = [];
                  };
                }
              });
            }
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
     * Check set/list equality ... is there a better way?
     */
    this.isEqual = function(s1, s2) {
      return (_.difference(s1, s2).length === 0 && _.difference(s2, s1).length === 0);
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
     * with proper set notations
     */
    this.displaySetOperation = function(item) {
      var displayStr = '';
      var intersections = item.intersections;
      var exclusions = item.exclusions;

      // Intersection
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

      // Subtractions
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

