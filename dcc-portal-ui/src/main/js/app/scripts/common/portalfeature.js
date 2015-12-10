(function() {
    'use strict';
    
    function PortalFeatureConstructor(features, $state, LocationService, Settings) {
      

      function _enable(feature) {
        if (features.hasOwnProperty(feature) === false) { return; }
        features[feature] = true;
        if ($state.current.name) {
          $state.go($state.current.name, {}, {reload: true});
        }
      }

      function _disable(feature) {
        if (features.hasOwnProperty(feature) === false) { return; }
        features[feature] = false;
        if ($state.current.name) {
          $state.go($state.current.name, {}, {reload: true});
        }
      }

  
      function init(settingsJson) {
        for (var featureName in settingsJson.featureFlags) {
          if (settingsJson.featureFlags[featureName] === true) {
            _enable(featureName);
          } else if (settingsJson.featureFlags[featureName] === false) {
            _disable(featureName);
          }
        }
      }

      // Allow features to be turned on via query param on application load

      Settings.get().then(init);

      this.get = function(s) {
        if (features.hasOwnProperty(s) === false) { return false; }
        return features[s];
      };

      this.enable = function(s) {
        _enable(s);
      };

      this.disable = function(s) {
        _disable(s);
      };

      this.list = function() {
        return features;
      };
  }

  /**
   * This serves as a debugging service to toggle features that are merged
   * but disabled.
   *
   * Note: This works automatically for views that are tied to a state, otherwise
   * it will be up to the callers to check for state change via watch/observe or other means.
   */
  angular.module('icgc.portalfeature', [])
    .provider('PortalFeature', function() {

       var _enabledFeatures = {
          AUTH_TOKEN: true,
          ICGC_CLOUD: true,
          SOFTWARE_PAGE: true
       };

      this.hasFeature = function(featureID) {
        return _.get(_enabledFeatures, featureID, false);
      };
      
      this.$get = ['$state', 'LocationService', 'Settings', function($state, LocationService, Settings) {
          return new PortalFeatureConstructor(_enabledFeatures, $state, LocationService, Settings);
      }];
  });



})();
