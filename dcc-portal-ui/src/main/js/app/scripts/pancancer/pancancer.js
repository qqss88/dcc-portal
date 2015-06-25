(function() {
  'use strict';

  var module = angular.module('icgc.pancancer', [
    'icgc.pancancer.controllers',
    'icgc.pancancer.services'
  ]);

  module.config(function($stateProvider) {
    $stateProvider.state('pancancer', {
      url: '/pcawg',
      templateUrl: 'scripts/pancancer/views/pancancer.html',
      controller: 'PancancerController as PancancerController'
    });
  });

})();


(function() {
  'use strict';

  var module = angular.module('icgc.pancancer.controllers', []);

  module.controller('PancancerController', function($scope, Page) {
    Page.stopWork();
    Page.setPage('entity');

    function filler(p) {
      var symbols = ['T','C','G','A', ' '];
      var result = '';

      for (var i=0; i < p; i++) {
        for (var ii = 0; ii < Math.random()*1000+500; ii++) {
          result += symbols[ Math.floor(Math.random()*5)];
        }
        result += '. ';
      }
      return result;
    };

    $scope.tcgaIpsum = filler(3);
  });

})();

(function() {
  'use strict';

  var module = angular.module('icgc.pancancer.services', []);

  module.service('PancancerService', function(Restangular) {


    /**
     * Reorder for UI, the top 5 items are fixed, the remining are appended to the end
     * on a first-come-first-serve basis.
     */
    this.orderPancancerDatatypes = function(data) {
      var precedence = ['DNA-Seq', 'RNA-Seq', 'SSM', 'CNSM', 'STSM'];
    };


    /**
     * Get pancancer statistics - This uses ICGC's external repository end point
     * datatype
     *   - # donors
     *   - # files
     *   - file size
     */
    this.getPancancerStats = function() {
      return Restangular.one('repository/pcawg').get({});
    };
  });


})();
