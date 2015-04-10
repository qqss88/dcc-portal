(function() {
  'use strict';

  var module = angular.module('icgc.common.chromosome', []);

  module.service('Chromosome', function() {

    // Chromosome lengths
    var lookup = {
      '1': 249250621,
      '2': 243199373,
      '3': 198022430,
      '4': 191154276,
      '5': 180915260,
      '6': 171115067,
      '7': 159138663,
      '8': 146364022,
      '9': 141213431,
      '10': 135534747,
      '11': 135006516,
      '12': 133851895,
      '13': 115169878,
      '14': 107349540,
      '15': 102531392,
      '16': 90354753,
      '17': 81195210,
      '18': 78077248,
      '19': 59128983,
      '20': 63025520,
      '21': 48129895,
      '22': 51304566,
      'X': 155270560,
      'Y': 59373566,
      'MT': 16569
    };

    this.get = function() {
      return lookup;
    };

    this.length = function(chromosome) {
      return lookup[chromosome];
    };

    /**
     * Validate against chromosome range
     *  Params: chr, start, end
     */
    this.validate = function() {
      var range, chr;

      if (arguments.length < 1 || arguments.length > 3) {
        return false;
      }
      chr = arguments[0].toUpperCase();
      if (lookup.hasOwnProperty(chr) === false) {
        return false;
      }
      range = lookup[chr];

      if (arguments[1] && (arguments[1] > range || arguments[1] < 1)) {
        return false;
      }
      if (arguments[2] && (arguments[2] > range || arguments[2] < 1)) {
        return false;
      }
      return true;
    };

  });

})();
