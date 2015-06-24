(function() {
  'use strict';

  var module = angular.module('icgc.pancancer', ['icgc.pancancer.controllers']);

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

  module.controller('PancancerController', function(Page) {
    Page.stopWork();
    Page.setPage('entity');
  });

})();
