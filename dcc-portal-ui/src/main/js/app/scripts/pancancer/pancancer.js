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

  module.controller('PancancerController', function($scope, Page, PancancerService) {
    Page.stopWork();
    Page.setPage('entity');

    function refresh() {
      // Get stats
      PancancerService.getPancancerStats().then(function(data) {
        $scope.pcawgDatatypes = PancancerService.orderPancancerDatatypes(data);
      });

      // Get overall summary
      PancancerService.getPancancerSummary().then(function(data) {
        $scope.summary = data;
      });
    }


    /* TCGA Ipsum */
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
    $scope.filters = PancancerService.buildRepoFilterStr();

    refresh();
  });

})();

(function() {
  'use strict';

  var module = angular.module('icgc.pancancer.services', []);

  module.service('PancancerService', function(Restangular) {

    function buildRepoFilterStr(datatype) {
      var filters = {
        file: {
          study: {is: ['PCAWG']}
        }
      }

      if (angular.isDefined(datatype)) {
        filters.file.dataType = {
          is: [datatype]
        };
      }

      return JSON.stringify(filters);
    }

    this.buildRepoFilterStr = buildRepoFilterStr;

    /**
     * Reorder for UI, the top 5 items are fixed, the remining are appended to the end
     * on a first-come-first-serve basis.
     */
    this.orderPancancerDatatypes = function(data) {
      var precedence = ['DNA-Seq', 'RNA-Seq', 'SSM', 'CNSM', 'STSM'];
      var list = [];

      // Scrub
      data = Restangular.stripRestangular(data);

      // Flatten and normalize for display
      Object.keys(data).forEach(function(key) {
        list.push({
          name: key,
          donorCount: +data[key].donorCount,
          fileCount: +data[key].fileCount,
          fileSize: +data[key].fileSize,
          fileFormat: data[key].dataFormat,
          filters: buildRepoFilterStr(key)
        });
      });

      // Sort
      return _.sortBy(list, function(d) {
        return precedence.indexOf(d.name) || 999;
      }).reverse();

    };


    /**
     * Get pancancer statistics - This uses ICGC's external repository end point
     * datatype
     *   - # donors
     *   - # files
     *   - file size
     */
    this.getPancancerStats = function() {
      return Restangular.one('repository/files/pcawg/stats').get({});
    };

    this.getPancancerSummary = function() {
      var param = {
        filters: {file: { study: {is: ['PCAWG']}}}
      };
      return Restangular.one('repository/files/summary').get(param);
    };

  });


})();
