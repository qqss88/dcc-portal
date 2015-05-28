/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
(function () {
  'use strict';

  var module = angular.module('app.common', [
    'app.common.services',
    'app.common.footer',
    'icgc.common.version',
    'icgc.common.notify',
    'icgc.common.location',
    'icgc.common.display',
    'icgc.common.external',

    // UI
    'icgc.common.codetable',
    'icgc.common.translator',

    // Biological modules
    'icgc.common.chromosome',
    'icgc.common.consequence',
    'icgc.common.datatype',

    // Query langauge
    'icgc.common.pql.translation',
    'icgc.common.pql.queryobject',
    'icgc.common.pql.utils'
  ]);



  // Translate project code into disease code. ie. BRCA-US => BRCA
  module.filter('diseaseCode', function() {
    return function(item) {
      return item.split('-')[0];
    };
  });



  module.filter('sum', function () {
    return function (items, param) {
      var ret = null;
      if (angular.isArray(items)) {
        ret = _.reduce(_.pluck(items, param), function (sum, num) {
          return sum + num;
        });
      }
      return ret;
    };
  });

  module.filter('startsWith', function () {
    return function (string, start) {
      var ret = null;
      if (angular.isString(string)) {
        ret = string.indexOf(start) === 0 ? string : null;
      }
      return ret;
    };
  });

  module.filter('numberPT', function ($filter) {
    return function (number) {
      if (angular.isNumber(number)) {
        return $filter('number')(number);
      } else {
        return number;
      }
    };
  });


  // a filter used to provide a tooltip (descriptive name) for a gene-set
  module.filter ( 'geneSetNameLookup', function (GeneSetNameLookupService) {
    return function (id) {
      return GeneSetNameLookupService.get ( id );
    };
  });


  module.filter('highlight', function () {
    return function (text, search, hide) {
      text = text || '';
      hide = hide || false;
      if (search) {
        text = angular.isArray(text) ? text.join(', ') : text.toString();
        // Shrink extra spaces, restrict to alpha-numeric chars and a few other special chars
        search = search.toString().replace(/\s+/g, ' ').replace(/[^a-zA-Z0-9:\s]/g, '').split(' ');
        for (var i = 0; i < search.length; ++i) {
          text = text.replace(new RegExp(search[i], 'gi'), '^$&$');
        }

        // if match return text
        if (text.indexOf('^') !== -1) {
          return text.replace(/\^/g, '<span class="match">').replace(/\$/g, '</span>');
        } else { // no match
          if (hide) {
            return '';
          } // hide
        }
      }

      // return base text if no match and not hiding
      return text;
    };
  });

  module.factory('debounce', function ($timeout, $q) {
    return function (func, wait, immediate) {
      var timeout, deferred;
      deferred = $q.defer();

      return function () {
        var context, later, callNow, args;

        context = this;
        args = arguments;
        later = function () {
          timeout = null;
          if (!immediate) {
            deferred.resolve(func.apply(context, args));
            deferred = $q.defer();
          }
        };
        callNow = immediate && !timeout;
        if (timeout) {
          $timeout.cancel(timeout);
        }
        timeout = $timeout(later, wait);
        if (callNow) {
          deferred.resolve(func.apply(context, args));
          deferred = $q.defer();
        }
        return deferred.promise;
      };
    };
  });

  /*
  module.filter('typecv', function () {
    return function (type) {
      var types = {
        'gene-centric': 'gene',
        'mutation-centric': 'mutation',
        'donor-centric': 'donor'
      };
      return types[type];
    };
  });
  */

  module.filter('unique', function () {
    return function (items) {
      var i, set, item;

      set = [];
      if (items && items.length) {
        for (i = 0; i < items.length; ++i) {
          item = items[i];
          if (set.indexOf(item) === -1) {
            set.push(item);
          }
        }
      }
      return set;
    };
  });

// Convert a non-array item into an array
  module.filter('makeArray', function () {
    return function (items) {
      if (angular.isArray(items)) {
        return items;
      }
      return [items];
    };
  });


// Join parallel arrays into a single array
// eg. [ ['a', 'b', 'c'], ['d', 'e', 'f'] ] | joinFields:''  => ['ad', 'be', 'cf']
/*
  module.filter('joinFields', function () {
    return function (items, delim, checkEmpty) {
      var i, j, list, tempList, joinedItem, hasEmpty;

      list = [];

      // Normalize
      for (i = 0; i < items.length; i++) {
        if (!angular.isArray(items[i])) {
          items[i] = [items[i]];
        }
      }

      // Join
      for (i = 0; i < items[0].length; i++) {
        tempList = [];
        hasEmpty = false;
        for (j = 0; j < items.length; j++) {
          tempList.push(items[j][i]);
          if (items[j][i] === '') {
            hasEmpty = true;
          }
        }

        // Skip join if one of the item is empty string
        if (checkEmpty && checkEmpty === true && hasEmpty === true) {
          continue;
        }

        joinedItem = tempList.join(delim).trim();
        if (joinedItem && joinedItem !== '') {
          list.push(joinedItem);
        }
      }
      return list;
    };
  });
  */

  module.filter('bytes', function () {
    return function (input) {
      var sizes = ['B', 'KB', 'MB', 'GB', 'TB'],
        postTxt = 0,
        bytes = input,
        precision = 2;

      if (bytes <= 1024) {
        precision = 0;
      }

      while (bytes >= 1024) {
        postTxt++;
        bytes = bytes / 1024;
      }

      return Number(bytes).toFixed(precision) + ' ' + sizes[postTxt];
    };
  });
})();
