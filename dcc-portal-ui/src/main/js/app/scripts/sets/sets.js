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

  module.directive('setUpload', function(SetService) {
    return {
      restruct: 'E',
      scope: {
        setModal: '=',
        setType: '=',
        setLimit: '@',
      },
      templateUrl: '/scripts/sets/views/sets.upload.html',
      link: function($scope) {

        $scope.setDescription = null;
        $scope.submitNewSet = function() {
          console.log('name', $scope.setName);
          console.log('description', $scope.setDescription);

          // TODO: real save
          var params = {};
          params.type = $scope.setType;
          params.name = $scope.setName;
          params.description = $scope.setDescription;
          params.filters = {};
          params.sort = '';
          params.order = '';

          SetService.addSet($scope.setType, params);

          // Reset
          $scope.setDescription = null;
          $scope.setType = null;
        };

        $scope.$watch('setType', function(n) {
          if (n) {
            $scope.setName = n;
          }
        });
      }
    };
  });

  module.directive('setOperation', function($location, $timeout, SetOperationService) {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/sets/views/sets.result.html',
      link: function($scope, $element) {
        var vennDiagram;

        $scope.selectedTotalCount = 0;
        $scope.current = [];
        $scope.selected = [];

        $scope.selectAll = function() {
          $scope.selected = [];
          $scope.selectedTotalCount = 0;

          $scope.data.forEach(function(set) {
            $scope.selected.push(set.intersections);
            vennDiagram.toggle(set.intersections, true);
            $scope.selectedTotalCount += set.count;
          });
        };

        $scope.selectNone = function() {
          $scope.data.forEach(function(set) {
            vennDiagram.toggle(set.intersections, false);
          });
          $scope.selected = [];
          $scope.selectedTotalCount = 0;
        };

        $scope.toggleSelection = function(item) {
          var ids = item.intersections;
          var existIdex = _.findIndex($scope.selected, function(subset) {
            return SetOperationService.isEqual(ids, subset);
          });

          if (existIdex === -1) {
            $scope.selected.push(ids);
            $scope.selectedTotalCount += item.count;
          } else {
            // FIXME: this is repeated, move out
            _.remove($scope.selected, function(subset) {
              return SetOperationService.isEqual(ids, subset);
            });
            if (SetOperationService.isEqual(ids, $scope.current) === true) {
              $scope.current = [];
            }
            $scope.selectedTotalCount -= item.count;
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
          return SetOperationService.isEqual($scope.current, ids);
        };

        $scope.displaySetOperation = SetOperationService.displaySetOperation;
        $scope.getSetShortHand = SetOperationService.getSetShortHand;

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
                intersections: ['AAA'],
                exclusions: ['BBB', 'CCC'],
                count: 150
             },
             {
                intersections: ['BBB'],
                exclusions: ['AAA', 'CCC'],
                count: 50
             },
             {
                intersections: ['CCC'],
                exclusions: ['BBB', 'AAA'],
                count: 5
             },
             // 2 int
             {
                intersections: ['AAA', 'BBB'],
                exclusions: ['CCC'],
                count: 50
             },
             {
                intersections: ['AAA', 'CCC'],
                exclusions: ['BBB'],
                count: 50
             },
             {
                intersections: ['BBB', 'CCC'],
                exclusions: ['AAA'],
                count: 50
             },
             // 3 int
             {
                intersections: ['AAA', 'BBB', 'CCC'],
                exclusions: [],
                count: 150
             },
          ];


          /*
          testData = [
             {
                intersections: ['dchang'],
                exclusions: ['jlam'],
                count: 100
             },
             {
                intersections: ['jlam'],
                exclusions: ['dchang'],
                count: 100
             },
             {
                intersections: ['dchang', 'jlam'],
                exclusions: [],
                count: 0
             },
          ];
          */


          var config = {
            // Because SVG urls are based on <base> tag, we need absolute path
            urlPath: $location.path(),

            mouseoverFunc: function(d) {
              $scope.$apply(function() {
                $scope.current = d.data;
              });
            },

            mouseoutFunc: function() {
              $scope.$apply(function() {
                $scope.current = [];
              });
            },

            clickFunc: function(d) {
              $scope.$apply(function() {
                if (d.selected === true) {
                  $scope.selected.push(d.data);
                  $scope.selectedTotalCount += d.count;
                } else {
                  _.remove($scope.selected, function(subset) {
                    return SetOperationService.isEqual(d.data, subset);
                  });
                  if (SetOperationService.isEqual(d.data, $scope.current) === true) {
                    $scope.current = [];
                  }
                  $scope.selectedTotalCount -= d.count;
                }
              });
            }
          };

          // TEST - Additional annotation to make life easier
          $scope.data = testData;
          $scope.vennData = SetOperationService.transform(testData);

          $scope.setList = [];
          $scope.data.forEach(function(set) {
            set.intersections.forEach(function(id) {
              if (_.contains($scope.setList, id) === false) {
                $scope.setList.push(id);
              }
            });
          });

          config.labelFunc = function(d) {
            return SetOperationService.getSetShortHand(d, $scope.setList);
          };

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

  module.service('SetOperationService', function() {
    var shortHandPrefix = 'S';


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


    function _getSetShortHand(setId, setList) {
      if (setList) {
        return shortHandPrefix + (setList.indexOf(setId) + 1);
      }
      return setId;
    }

    this.getSetShortHand = _getSetShortHand;


    /**
     * Transforms internal set prepresentation into UI display format
     * with proper set notations
     */
    this.displaySetOperation = function(item, setList) {
      var i = 0;
      var displayStr = '';
      var intersections = item.intersections;
      var exclusions = item.exclusions;

      // Intersection
      if (intersections.length > 1) {
        displayStr += '(';
        for (i=0; i < intersections.length; i++) {
          displayStr += _getSetShortHand(intersections[i], setList);
          if (i < intersections.length-1) {
            displayStr += ' &cap; ';
          }
        }
        displayStr += ')';
      } else {
        displayStr += _getSetShortHand(intersections[0], setList);
      }

      // Subtractions
      if (exclusions.length > 1) {
        displayStr += ' - ';
        displayStr += '(';
        for (i=0; i < exclusions.length; i++) {
          displayStr += _getSetShortHand(exclusions[i], setList);
          if (i < exclusions.length-1) {
            displayStr += ' &cup; ';
          }
        }
        displayStr += ')';
      } else if (exclusions.length > 0) {
        displayStr += ' - ';
        displayStr += _getSetShortHand(exclusions[0], setList);
      }
      return displayStr;
    };
  });



  /**
   * Abstracts CRUD operations on entity lists (gene, donor, mutation)
   */
  module.service('SetService', function(Restangular, localStorageService, toaster) {
    var LIST_ENTITY = 'entity';
    var _this = this;


    /**
    * params.filters
    * params.sort
    * params.order
    * params.name
    * params.description - optional
    * params.count - limit (max 1000) ???
    *
    * Create a new set from
    */
    this.addSet = function(type, params) {
      // TODO: untested

      var data = '', promise = null;
      data += 'type=' + type + '&';
      data += 'filters=' + JSON.stringify(params.filters) + '&';
      data += 'sort=' + params.sort + '&';
      data += 'order=' + params.order + '&';
      data += 'name=' + encodeURIComponent(params.name) + '&';
      if (angular.isDefined(params.description)) {
        data += 'description=' + encodeURIComponent(params.name);
      }

      promise = Restangular.one('list').withHttpConfig({transformRequest: angular.identity}).customPOST(data);
      promise.then(function(data) {
        if (! data.id) {
          return;
        }

        // FIXME: should poll to make sure the list is ready to be used before saving to storage

        // Success, now save it locally
        var localSet = {
          id: data.id,
          type: params.type,
          name: params.name,
          description: params.description,
          count: params.count
        };

        var lists = _this.getAll();
        lists.unshift(localSet);
        localStorageService.set(LIST_ENTITY, lists);
        toaster.pop('', localSet.name  + ' was saved', 'View in <a href="/analysis">Bench</a>', 4000, 'trustedHtml');
      }, function(err) {

        // FIXME: testing, remove
        console.log(err);
        var localSet = {
          id: (new Date()),
          type: params.type,
          name: params.name,
          description: params.description,
          count: params.count
        };

        var lists = _this.getAll();
        lists.unshift(localSet);
        localStorageService.set(LIST_ENTITY, lists);
        toaster.pop('', localSet.name  + ' was saved', 'View in <a href="/analysis">Bench</a>', 4000, 'trustedHtml');


      });
    };


    /**
    * params.union
    * params.name
    * params.description - optional
    *
    * Create a new set from the union of various subsets of the same type
    */
    this.addDerivedSet = function(type, params) {
      // TODO: stub
      var data = '', promise = null;

      data += 'type=' + type + '&';
      data += 'name=' + params.name;
      data += 'union' + JSON.stringify(params.union);

      if (angular.isDefined(params.description)) {
        data += 'description=' + encodeURIComponent(params.name);
      }

      promise = Restangular.one('list/union').withHttpConfig({transformRequest: angular.identity}).customPOST(data);
      promise.then(function(data) {
        if (! data.id) {
          return;
        }
      });
    };


    this.getMetaData = function( ids ) {
      // TODO: stub
      console.log('getting meta data for', ids);
    };


    this.exportSet = function(sets) {
      // TODO: stub
      console.log('exporting', sets);
    };



    /****** Local storage related API ******/
    this.addTest = function(list) {
      var lists = this.getAll();
      lists.unshift(list);
      localStorageService.set(LIST_ENTITY, lists);

      toaster.pop('', list.name  + ' was saved', 'View it in the <a href="/analysis">Bench</a>', 4000, 'trustedHtml');
      return true;
    };


    this.getAll = function() {
      var sets = localStorageService.get(LIST_ENTITY) || [];
      sets.forEach(function(set) {
        var prefix = set.type.charAt(0), filters = {};
        filters[set.type] = {
          entityListId: {is: [ set.id ]}
        };
        set.advLink = '/search/' + prefix + '?filters=' + JSON.stringify(filters);
      });
      return sets;
    };

    this.remove = function(id) {
      var lists = localStorageService.get(LIST_ENTITY);
      _.remove(lists, function(list) {
        return list.id === id;
      });
      localStorageService.set(LIST_ENTITY, lists);
      return true;
    };


  });




})();

