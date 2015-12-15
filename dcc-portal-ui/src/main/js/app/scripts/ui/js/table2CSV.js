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


/** 
 * Adapated to work with ICGC table double header format 
 */
jQuery.fn.table2CSV = function(options) {
  var options = jQuery.extend({
        separator: ',',
        header: [],
        delivery: 'popup' // popup, value
      },
      options);

  var csvData = [];
  var headerArr = [];
  var el = this;

  //header
  var numCols = options.header.length;
  var tmpRow = []; // construct header avalible array

  if (numCols > 0) {
    for (var i = 0; i < numCols; i++) {
      tmpRow[tmpRow.length] = formatData(options.header[i]);
    }
  } else {

    // portal-ui: support 2-level table header, dependent on the use of subhead class
    var subQueue = [];
    var idx = -1;
    //$(el).filter(':visible').find('tr').each(function() {
    $(el).find('tr').each(function() {
      // Second level processing
      if ($(this).hasClass('subhead')) {
        $(this).find('th').each(function() {
          //if ($(this).css('display') != 'none') {
            if (subQueue.length > 0) {
              idx = subQueue.splice(0, 1);
              tmpRow[idx] += ' ' + formatData($(this).html());
            }
          //}
        });
      } else {
        // Top level processing
        $(this).find('th').each(function() {
          //if ($(this).css('display') != 'none') {
            if ($(this).attr('colspan')) {
              var cols = $(this).attr('colspan');
              for (var i=0; i < cols; i++) {
                subQueue.push( tmpRow.length );
                tmpRow[tmpRow.length] = formatData($(this).html());
              }
            } else {
              tmpRow[tmpRow.length] = formatData($(this).html());
            }
          //}
        });
      }
    });


    /* Original
    $(el).filter(':visible').find('th').each(function() {
      if ($(this).css('display') != 'none') tmpRow[tmpRow.length] = formatData($(this).html());
    });
    */
  }

  row2CSV(tmpRow);

  // actual data
  $(el).find('tr').each(function() {
    var tmpRow = [];
    //$(this).filter(':visible').find('td').each(function() {
    $(this).find('td').each(function() {
      //if ($(this).css('display') != 'none') 
        tmpRow[tmpRow.length] = formatData($(this).html());
    });
    row2CSV(tmpRow);
  });
  if (options.delivery == 'popup') {
    var mydata = csvData.join('\n');
    return popup(mydata);
  } else {
    var mydata = csvData.join('\n');
    return mydata;
  }

  function row2CSV(tmpRow) {
    var tmp = tmpRow.join('') // to remove any blank rows
    // alert(tmp);
    if (tmpRow.length > 0 && tmp != '') {
      var mystr = tmpRow.join(options.separator);
      csvData[csvData.length] = mystr;
    }
  }
  function formatData(input) {
    var regexp, output;

    // replace " with â€œ
    regexp = new RegExp(/["]/g);
    output = input.replace(regexp, "â€œ");


    //HTML
    regexp = new RegExp(/\<[^\<]+\>/g);
    output = output.replace(regexp, "");

    // portal-ui: additional formatting
    // - Remove non-breaking space
    // - Decode symbols (i.e. <, >) 
    // - Trim leading/trailing spaces
    // - Trim internal spaces
    output = output.replace(/&nbsp;/g, '');
    output = output.replace('&lt;', '<')
    output = output.replace('&gt;', '>')
    output = output.replace(/^\s+|\s+$/g, '');
    output = output.replace(/\n/g, '');
    output = output.replace(/\s+/g, ' ');

    if (output == "") return '';

    // Do not wrap in quotes if format is tsv
    if (options.separator === '\t') {
      return output;
    }
    return '"' + output + '"';
  }
  function popup(data) {
    var generator = window.open('', 'csv', 'height=400,width=600');
    generator.document.write('<html><head><title>CSV</title>');
    generator.document.write('</head><body >');
    generator.document.write('<textArea cols=70 rows=15 wrap="off" >');
    generator.document.write(data);
    generator.document.write('</textArea>');
    generator.document.write('</body></html>');
    generator.document.close();
    return true;
  }
};
