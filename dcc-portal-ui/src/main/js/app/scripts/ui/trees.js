'use strict';

// Cannot use a recursive template, partially because angularJS caps the number of digest cycles
// to 10. While this can be modified (albeit globally), it may bring other problems...
angular.module('icgc.ui.trees', []).directive('pathwayTree', function($compile) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      tree: '='
    },
    template: '<div class="tree"></div>',
    link: function($scope, $element) {
      // D3 would've been easier...
      function addNesting(e, current) {
        var ul, li, span, anchor;
        ul = angular.element('<ul>');
        li = angular.element('<li>');
        span = angular.element('<span>');

        if (current.children) {
          anchor = angular.element('<a>').attr('data-ng-href', '/genesets/' + current.id).text(current.name);
          anchor.appendTo(span);
        } else {
          angular.element('<strong>').text(current.name).appendTo(span);
        }


        span.appendTo(li);
        li.appendTo(ul);
        ul.appendTo(e);

        if (current.children && current.children.length > 0) {
          current.children.forEach(function(child) {
            addNesting(li, child);
          });
        }
      }

      $scope.tree.forEach(function(child) {
        addNesting($element[0], child);
      });


      // Dynamically generated contents need compilation
      $compile($element.contents())($scope);

    }
  };
}).directive('inferredTree', function($compile) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      tree: '='
    },
    template: '<div class="tree"></div>',
    link: function($scope, $element) {

      // If grand child exist, this is inferred.
      function getRelation(relation, hasGrandChild) {
        var element = angular.element('<abbr>').attr('data-tooltip-placement', 'left');

        if (relation === 'is_a') {
          return element.text('I ')
            .attr('data-tooltip', hasGrandChild? 'Inferred is a' : 'Is a')
            .attr('class', 'goterm_is_a');
        } else if (relation === 'part_of') {
          return element.text('P')
            .attr('data-tooltip', hasGrandChild? 'Inferred part of' : 'Part of')
            .attr('class', 'goterm_part_of');
        } else if (relation === 'regulates') {
          return element.text('R')
            .attr('data-tooltip', hasGrandChild? 'Inferred regulates' : 'Regulates')
            .attr('class', 'goterm_regulates');
        } else if (relation === 'positively_regulates') {
          return element.text('R')
            .attr('data-tooltip', hasGrandChild? 'Inferred positively regulates' : 'Positively regulates')
            .attr('class', 'goterm_regulates');
        } else if (relation === 'negatively_regulates') {
          return element.text('R')
            .attr('data-tooltip', hasGrandChild? 'Inferred negatively regulates' : 'Negatively regulates')
            .attr('class', 'goterm_regulates');
        } else {
          // FIXME: need to find out how to resolve unknowns
          return element.text('?');
        }
        // return null;
      }

      function addNesting(e, current) {
        var ul, li, span, anchor, relation, label, hasGrandChild = false;

        ul = angular.element('<ul>');
        li = angular.element('<li>');
        span = angular.element('<span>');


        if (current.child && current.child.child) {
          hasGrandChild = true;
        }

        current.goTerms.forEach(function(goTerm) {
          label = ' ' + goTerm.id + ' ' + goTerm.name;
          anchor = angular.element('<a>').attr('data-ng-href', '/genesets/' + goTerm.id).text(label);
          relation = getRelation(goTerm.relation, hasGrandChild);

          if (relation) {
            relation.appendTo(span);
          }

          if (goTerm.level === 0) {
            angular.element('<strong>').text(label).appendTo(span);
          } else {
            anchor.appendTo(span);
          }

          // relation = angular.element('<strong>').text(goTerm.relation);
          angular.element('<br>').appendTo(span);
        });

        span.appendTo(li);
        li.appendTo(ul);
        ul.appendTo(e);

        if (current.child) {
          addNesting(li, current.child);
        }
      }

      addNesting($element[0], $scope.tree);

      // Dynamically generated contents need compilation
      $compile($element.contents())($scope);

    }
  };
});

