(function () {
  'use strict';

  angular.module('icgc.dictionary', ['icgc.dictionary.controllers']);

})();

(function () {
  'use strict';

  var module = angular.module('icgc.dictionary.controllers', []);

  /**
   * View dictionary fields of fileType (injected)
   */
  module.controller('DictionaryController', function($scope, fileType) {
    $scope.fileType = fileType;
  });

})();
