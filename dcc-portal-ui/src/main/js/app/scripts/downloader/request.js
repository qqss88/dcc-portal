(function() {

  'use strict';

  var module = angular.module('icgc.downloader', []);

  /**
   * Requesting dynamic download
   */
  module.controller('DownloadRequestController',
    function($scope, $location, $modalInstance, $filter, Donors, LocationService,
    DownloadService, DownloaderService, DataTypes, filters) {

    var emailRegex = /.+@.+\..+/i;

    $scope.params = {};
    $scope.params.useEmail = false;
    $scope.params.isValidEmail = true;
    $scope.params.emailAddress = '';
    $scope.params.processing = false;
    $scope.params.dataTypes = [];

    $scope.totalSize = 0;

    if (! filters) {
      filters = LocationService.filters();
    }

    function sum(active, size) {
      if (active) {
        $scope.dlTotal += size;
        $scope.dlFile++;
      } else {
        $scope.dlTotal -= size;
        $scope.dlFile--;
      }
    }

    function reset() {
      $scope.dlTotal = 0;
      $scope.dlFile = 0;
      $scope.overallSize = 0;
    }

    function sortFunc(dataType) {
      var index = DataTypes.order.indexOf(dataType.label);
      if (index === -1) {
        return DataTypes.order.length + 1;
      }
      return index;
    }


    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.toggle = function (datatype) {
      if (datatype.sizes > 0) {
        datatype.active = !datatype.active;
        sum(datatype.active, datatype.sizes);
      }
    };

    $scope.sendRequest = function() {
      var i, item, actives, linkURL;

      // FIXME: We don't really need to check status anymore
      DownloadService.getStatus().then(function (serverStatus) {

        if (serverStatus.status === false) {
          return;
        }

        actives = [];
        for (i = 0; i < $scope.params.dataTypes.length; ++i) {
          item = $scope.params.dataTypes[i];
          if (item.active) {
            actives.push({key: item.label, value: 'TSV'});
          }
        }
        linkURL = $location.protocol() + '://' + $location.host() + ':' + $location.port() + '/download';

        DownloaderService
          .requestDownloadJob(filters, actives, $scope.params.emailAddress,
            linkURL, JSON.stringify(filters)).then(function (job) {
            $modalInstance.dismiss('cancel');
            $location.path('/download/' + job.downloadId).search('');
        });

      });
    };


    $scope.calculateSize = function () {
      $scope.params.processing = true;
      reset();

      $scope.dlFile = 0;
      $scope.dlTotal = 0;

      // Compute the total number of donors
      Donors.handler.get('count', {filters: filters}).then(function (data) {
        $scope.totalDonor = data;
      });

      DownloadService.getSizes(filters).then(function (response) {
        $scope.params.dataTypes = response.fileSize;
        $scope.params.dataTypes.forEach(function (dataType) {
          dataType.active = false;
          dataType.uiLabel = dataType.label;
          dataType.uiLabel = DataTypes.mapping[dataType.label] || dataType.label;
          $scope.overallSize += dataType.sizes;
        });

        // Re-order it based on importance
        $scope.params.dataTypes = $filter('orderBy')($scope.params.dataTypes, sortFunc);


        $scope.params.processing = false;
      });
    };


    $scope.calculateSize();

  });

})();

