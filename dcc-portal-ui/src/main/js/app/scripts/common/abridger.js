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

  _.mixin ({
    pairUp: function (arrays) {
      var resolved = _.map (arrays, function (o) {
        return o.value();
      });
      return _.zip.apply (_, resolved);
    }
  });

  function caseInsensitivelyContains (word, partial) {
    return _.contains (word.toUpperCase(), partial.toUpperCase());
  }

  // Abridger class
  // TODO: enforce the rule of calling 'new' to instantiate an instance
  function Abridger (maxLength) {
    var ellipsis = '...';
    var _this = this;

    this.maxLength = maxLength;

    this.find = function (sentence, keyword) {
      var words = _.words (sentence);
      var index = _.findIndex (words, function (word) {
        return caseInsensitivelyContains (word, keyword);
      });

      return {
        target: words [index],
        left: _(words).take (index).reverse(),
        right: _(words).slice (index).rest()
      };
    };
    var withinLimit = this.withinLimit = function (newElements) {
      var combined = _(_this.resultArray).concat (newElements);

      var numberOfCharacters = combined.map ('length').sum();
      var numberOfSpaces = combined.size() - 1;

      return (numberOfCharacters + numberOfSpaces) <= _this.maxLength;
    };

    this.processLeftAndRight = function (newElements) {
      var left = _.first (newElements);
      var right = _.last (newElements);

      if (withinLimit (newElements)) {
        _this.resultArray = [left].concat (_this.resultArray, right);
        return true;
      } else {

        if (_.size (left) >= _.size (right)) {
          if (withinLimit (left)) {
            _this.currentProcessor = _this.processLeftOnly;
            return _this.currentProcessor (newElements);
          } else if (withinLimit (right)) {
            _this.currentProcessor = _this.processRightOnly;
            return _this.currentProcessor (newElements);
          }
        } else {
          if (withinLimit (right)) {
            _this.currentProcessor = _this.processRightOnly;
            return _this.currentProcessor (newElements);
          } else if (withinLimit (left)) {
            _this.currentProcessor = _this.processLeftOnly;
            return _this.currentProcessor (newElements);
          }
        }

      }

      return false;
    };

    this.processLeftOnly = function (newElements) {
      var left = _.first (newElements);

      if (withinLimit (left)) {
        _this.resultArray = [left].concat (_this.resultArray);
        return true;
      }

      return false;
    };

    this.processRightOnly = function (newElements) {
      var right = _.last (newElements);

      if (withinLimit (right)) {
        _this.resultArray = _this.resultArray.concat (right);
        return true;
      }

      return false;
    };
    this.hasRoom = function (newElements) {
      return _this.currentProcessor (newElements);
    };

    this.format = function (fragments, sentence) {
      var joined = fragments.join (' ').trim();
      var dots = function (f) {
        return f (sentence, joined) ? '' : ellipsis;
      };

      return dots (_.startsWith) + joined + dots (_.endsWith);
    };

    this.abridge = function (sentence, keyword) {
      var finding = _this.find (sentence, keyword);

      _this.resultArray = [finding.target];
      _this.currentProcessor = _this.processLeftAndRight;

      _([finding.left, finding.right])
        .pairUp()
        .takeWhile (_this.hasRoom)
        .value();

      return _this.format (_this.resultArray, sentence);
    };

    this.currentProcessor = this.processLeftAndRight;
    this.resultArray = [];
  }


  var namespace = 'icgc.common.text.utils';
  var serviceName = 'Abridger';

  var module = angular.module (namespace, []);

  module.factory (serviceName, function () {
    return {
      Abridger: Abridger
    };
  });
})();
