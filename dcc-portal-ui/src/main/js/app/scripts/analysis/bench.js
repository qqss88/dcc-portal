(function () {
  'use strict';
  angular.module('icgc.bench', ['icgc.bench.services']);
})();


(function () {
  'use strict';
  var module = angular.module('icgc.bench.services', ['restangular', 'icgc.common.location']);

  /**
   * Abstracts CRUD operations on entity lists (gene, donor, mutation)
   */
  module.service('ListManagerService', function(Restangular, localStorageService, toaster) {

    var LIST_ENTITY = 'entity';

    this.add = function(/* some criteria ... */) {
    };

    this.addTest = function(list) {
      var lists = this.getAll();
      lists.unshift(list);
      localStorageService.set(LIST_ENTITY, lists);

      toaster.pop('', 'ICGC Portal', '<a href="/search">New list created</a>', 4000, 'trustedHtml');
      return true;
    };

    this.getAll = function() {
      return localStorageService.get(LIST_ENTITY);
    };

    this.exportList = function() {
      // TODO
    };

    this.remove = function(id) {
      var lists = localStorageService.get(LIST_ENTITY);
      _.remove(lists, function(list) {
        return list.id === id;
      });
      localStorageService.set(LIST_ENTITY, lists);
      return true;
    };

    this.seedTestData = function() {
      console.log('Seedning test data');
      var sampleList = [
        {
           id: 'sample-1',
           type: 'donor',
           name: 'sample name',
           note: 'sample note???',
           count: 80
        },
        {
           id: 'sample-2',
           type: 'donor',
           name: 'donor experiment from ',
           note: 'sample note???',
           count: 17
        },
        {
           id: 'sample-3',
           type: 'gene',
           name: 'cool genes',
           note: 'sample note???',
           count: 10
        },
        {
           id: 'sample-4',
           type: 'mutation',
           name: '500 mutations',
           note: 'sample note???',
           count: 500
        }
      ];
      localStorageService.set(LIST_ENTITY, sampleList);
    };

  });

})();

