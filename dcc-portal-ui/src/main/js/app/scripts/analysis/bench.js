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

      var data = ''; promise = null;
      data += 'type=' + params.type + '&';
      data += 'filters=' + JSON.stringify(params.filters) + '&';
      data += 'sort=' + params.sort + '&';
      data += 'order=' + params.order + '&';
      data += 'name=' + encodeURIComponent(params.name) + '&';
      if (angular.isDefined(params.description)) {
        data += 'description=' + encodeURIComponent(params.name) + '&';
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
        lists.unshift(list);
        localStorageService.set(LIST_ENTITY, lists);
        toaster.pop('', list.name  + ' was saved', 'View in the <a href="/analysis">Bench</a>', 4000, 'trustedHtml');

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
    };


    this.exportSet = function(sets) {
      // TODO: stub
    };




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

    this.seedTestData = function() {
      console.log('Seedning test data');
      var sampleList = [
        {
           id: 'sample-1',
           type: 'donor',
           name: 'sample name',
           description: 'sample note???',
           count: 80
        },
        {
           id: 'sample-2',
           type: 'donor',
           name: 'donor experiment from ',
           description: 'sample note???',
           count: 17
        },
        {
           id: 'sample-3',
           type: 'gene',
           name: 'cool genes',
           description: 'sample note???',
           count: 10
        },
        {
           id: 'sample-4',
           type: 'mutation',
           name: '500 mutations',
           description: 'sample note???',
           count: 500
        }
      ];
      localStorageService.set(LIST_ENTITY, sampleList);
    };

  });

})();

