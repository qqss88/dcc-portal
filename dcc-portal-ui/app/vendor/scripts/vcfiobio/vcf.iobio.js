//
//  vcfiobio
//  Tony Di Sera
//  October 2014
//
//  This is a data manager class for the variant summary data.
// 
//  Two file are used to generate the variant data: 
//    1. the bgzipped vcf (.vcf.gz) 
//    2. its corresponding tabix file (.vcf.gz.tbi).  
//
//  The variant summary data come in 3 main forms:  
//    1. reference names and lengths 
//    2. variant density (point data), 
//    3. vcf stats (variant types, tstv ration allele frequency, mutation spectrum,
//       insertion/deletion distribution, and qc distribution).  
//  The reference names and lengths as well as the variant density data obtained from 
//  the tabix file; the vcf stats are determined by parsing the vcf file in sampled regions.
//
//  The files can be hosted remotely, specified by a URL, or reside on the client, accesssed as a local
//  file on the file system. When the files are on a remote server, vcfiobio communicates with iobio services 
//  to obtain the metrics data.  When the files are accessed locally, a client-side javascript library
//  is used to a) read the tabix file to obtain the reference names/lengths and the variant density data 
//  and b) parse the vcf records from sampled regions.  This mini vcf file is then streamed to iobio services
//  to obtain the vcf metrics.  
//  
//  The follow example code illustrates the method calls to make
//  when the vcf file is served remotely (a URL is entered)
//
//  var vcfiobio = vcfiobio();
//  vcfiobio.loadRemoteIndex(vcfUrl, function(data) {
//     // Filter out the short (<1% median reference length) references
//     vcfiobio.getReferenceData(.01, 100);
//     // Show all the references (example: in a pie chart) here....
//     // Render the variant density data here....
//  });
//  vcfiobio.getEstimatedDensity(refName);
//  vcfiobio.getStats(refs, options, function(data) {
//     // Render the vcf stats here....
//  }); 
'use strict';

var Vcfiobio = function() {
  var debug = false;
  var exports = {};
  var dispatch = d3.dispatch('dataReady', 'dataLoading');

  var SOURCE_TYPE_URL = "URL";
  var sourceType = "url";

  //var vcfstatsAliveServer    = "ws://localhost:7070";
  //var tabixServer            = "ws://localhost:7090";
  //var vcfReadDeptherServer   = "ws://localhost:7062";

  // var vcfstatsAliveServer    = "wss://vcfstatsalive.iobio.io";
  // var tabixServer            = "wss://tabix.iobio.io";
  // var vcfReadDeptherServer   = "wss://vcfreaddepther.iobio.io";

  var vcfstatsAliveServer = "wss://nv-green.iobio.io/vcfstatsalive/";
  var tabixServer = "wss://nv-green.iobio.io/tabix/";
  var vcfReadDeptherServer = "wss://nv-green.iobio.io/vcfdepther/";

  var vcfURL;
  var vcfReader;
  var vcfFile;
  var tabixFile;
  var size16kb = Math.pow(2, 14);
  var refData = [];
  var refDensity = [];
  var refName = "";

  var regions = [];
  var regionIndex = 0;
  var stream = null;

  exports.openVcfUrl = function(url) {
    sourceType = SOURCE_TYPE_URL;
    vcfURL = url;
  }

  exports.loadRemoteIndex = function(theVcfUrl, callback) {
    vcfURL = theVcfUrl;
    sourceType = SOURCE_TYPE_URL;

    var client = BinaryClient(vcfReadDeptherServer);
    var url = encodeURI(vcfReadDeptherServer + '?cmd=' + vcfURL + ".tbi");

    client.on('open', function(stream) {
      var stream = client.createStream({ event: 'run', params: { 'url': url } });
      var currentSequence;
      var refName;
      stream.on('data', function(data, options) {
        data = data.split("\n");
        for (var i = 0; i < data.length; i++) {
          if (data[i][0] == '#') {

            var tokens = data[i].substr(1).split("\t");
            refIndex = tokens[0];
            refName = tokens[1];
            var refLength = tokens[2];


            refData.push({ "name": refName, "value": +refLength, "refLength": +refLength, "idx": +refIndex });
            refDensity[refName] = { "idx": refIndex, "points": [], "intervalPoints": [] };
          }
          else {
            if (data[i] != "") {
              var d = data[i].split("\t");
              var point = [parseInt(d[0]), parseInt(d[1])];
              refDensity[refName].points.push(point);
              refDensity[refName].intervalPoints.push(point);

            }
          }
        }
      });
      stream.on("error", function(error) {

      });
      stream.on('end', function() {
        callback.call(this, refData);
      });
    });
  };

  exports.getReferences = function(minLengthPercent, maxLengthPercent) {
    var references = [];

    // Calculate the total length
    var totalLength = +0;
    for (var i = 0; i < refData.length; i++) {
      var refObject = refData[i];
      totalLength += refObject.value;
    }

    // Only include references with length within percent range
    for (var i = 0; i < refData.length; i++) {
      var refObject = refData[i];
      var lengthPercent = refObject.value / totalLength;
      if (lengthPercent >= minLengthPercent && lengthPercent <= maxLengthPercent) {
        references.push(refObject);
      }
    }
    return references;
  }

  exports.getEstimatedDensity = function(ref, useLinearIndex, removeTheDataSpikes, maxPoints, rdpEpsilon) {
    var points = useLinearIndex ? refDensity[ref].intervalPoints.concat() : refDensity[ref].points.concat();

    if (removeTheDataSpikes) {
      var filteredPoints = this._applyCeiling(points);
      if (filteredPoints.length > 500) {
        points = filteredPoints;
      }
    }

    // Reduce point data to to a reasonable number of points for display purposes
    if (maxPoints) {
      var factor = d3.round(points.length / maxPoints);
      points = this.reducePoints(points, factor, function(d) { return d[0]; }, function(d) { return d[1] });
    }

    // Now perform RDP
    if (rdpEpsilon) {
      points = this._performRDP(points, rdpEpsilon, function(d) { return d[0] }, function(d) { return d[1] });
    }
    return points;
  }

  exports.getGenomeEstimatedDensity = function(useLinearIndex, removeTheDataSpikes, maxPoints, rdpEpsilon) {
    var allPoints = [];
    var offset = 0;
    for (var i = 0; i < refData.length; i++) {
      var points = useLinearIndex ? refDensity[refData[i].name].intervalPoints.concat() : refDensity[refData[i].name].points.concat();

      var offsetPoints = [];
      for (var x = 0; x < points.length; x++) {
        offsetPoints.push([points[x][0] + offset, points[x][1]]);
      }
      allPoints = allPoints.concat(offsetPoints);
      // We are making a linear representation of all ref density.
      // We will add the length of the ref to the 
      // next reference's positions.
      offset = offset + refData[i].value;
    }
    if (removeTheDataSpikes) {
      allPoints = this._applyCeiling(allPoints);
    }

    // Reduce point data to to a reasonable number of points for display purposes
    if (maxPoints) {
      var factor = d3.round(allPoints.length / maxPoints);
      allPoints = this.reducePoints(allPoints, factor, function(d) { return d[0]; }, function(d) { return d[1] });
    }

    // Now perform RDP
    if (rdpEpsilon) {
      allPoints = this._performRDP(allPoints, rdpEpsilon, function(d) { return d[0] }, function(d) { return d[1] });
    }
    return allPoints;
  }

  exports.getStats = function(refs, options, callback) {
    this._getRemoteStats(refs, options, callback);
  }

  exports._getRemoteStats = function(refs, options, callback) {
    var me = this;
    me._getRegions(refs, options);

    // This is the tabix url.  Here we send the regions as arguments.  tabix
    // output (vcf header+records for the regions) will be piped
    // to the vcfstatsalive server.
    var regionStr = "";
    regions.forEach(function(region) {
      regionStr += " " + region.name + ":" + region.start + "-" + region.end
    });
    var tabixUrl = tabixServer + "?cmd=-h " + vcfURL + regionStr + "&encoding=binary";

    // This is the full url for vcfstatsalive server which is piped its input from tabixserver
    var url = encodeURI(vcfstatsAliveServer + '?cmd=-u 1000 ' + encodeURIComponent(tabixUrl));
    // Connect to the vcfstatsaliveserver    
    var client = BinaryClient(vcfstatsAliveServer);

    var buffer = "";
    client.on('open', function(stream) {

      // Run the command
      var stream = client.createStream({ event: 'run', params: { 'url': url } });

      // Listen for data to be streamed back to the client
      stream.on('data', function(datas, options) {
        datas.split(';').forEach(function(data) {
          if (data == undefined) {
            return;
          }
          var success = true;
          try {
            var obj = JSON.parse(buffer + data);
          } catch (e) {
            success = false;
            buffer += data;
          }
          if (success) {
            buffer = "";
            callback(obj);
          }
        });
      });
      stream.on('end', function() {
        if (options.onEnd != undefined)
          options.onEnd();
      });
    });
  };

  exports._getRegions = function(refs, options) {
    regionIndex = 0;
    regions.length = 0;

    var bedRegions;
    for (var j = 0; j < refs.length; j++) {
      var ref = refData[refs[j]];
      var start = options.start;
      var end = options.end ? options.end : ref.refLength;
      var length = end - start;
      if (length < options.binSize * options.binNumber) {
        regions.push({
          'name': ref.name,
          'start': start,
          'end': end
        });
      } else {
        // create random reference coordinates
        for (var i = 0; i < options.binNumber; i++) {
          var s = start + parseInt(Math.random() * length);
          regions.push({
            'name': ref.name,
            'start': s,
            'end': s + options.binSize
          });
        }
        // sort by start value
        regions = regions.sort(function(a, b) {
          var x = a.start; var y = b.start;
          return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });
      }
    }
    return regions;
  }

  exports.jsonToArray = function(_obj, keyAttr, valueAttr) {
    var theArray = [];
    for (prop in _obj) {
      var o = new Object();
      o[keyAttr] = prop;
      o[valueAttr] = _obj[prop];
      theArray.push(o);
    }
    return theArray;
  };

  exports.jsonToValueArray = function(_obj) {
    var theArray = [];
    for (var key in _obj) {
      theArray.push(_obj[key]);
    }
    return theArray;
  };

  exports.jsonToArray2D = function(_obj) {
    var theArray = [];
    for (prop in _obj) {
      var row = [];
      row[0] = +prop;
      row[1] = +_obj[prop];
      theArray.push(row);
    }
    return theArray;
  };

  exports.reducePoints = function(data, factor, xvalue, yvalue) {
    if (factor <= 1) {
      return data;
    }
    var i, j, results = [], sum = 0, length = data.length, avgWindow;

    if (!factor || factor <= 0) {
      factor = 1;
    }
    // Create a sliding window of averages
    for (i = 0; i < length; i += factor) {
      // Slice from i to factor
      avgWindow = data.slice(i, i + factor);
      for (j = 0; j < avgWindow.length; j++) {
        var y = yvalue(avgWindow[j]);
        sum += y != null ? d3.round(y) : 0;
      }
      results.push([xvalue(data[i]), sum])
      sum = 0;
    }
    return results;
  };

  /**
   * PRIVATE
   */
  exports._makeid = function() {
    // make unique string id;
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for (var i = 0; i < 5; i++)
      text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
  };

  exports._performRDP = function(data, epsilon, pos, depth) {
    var smoothedData = properRDP(data, epsilon);
    return smoothedData;
  }

  exports._applyCeiling = function(someArray) {
    if (someArray.length < 5) {
      return someArray;
    }

    // Copy the values, rather than operating on references to existing values
    var values = someArray.concat();

    // Then sort
    values.sort(function(a, b) {
      return a[1] - b[1];
    });

    /* Then find a generous IQR. This is generous because if (values.length / 4) 
     * is not an int, then really you should average the two elements on either 
     * side to find q1.
     */
    var q1 = values[Math.floor((values.length / 4))][1];
    // Likewise for q3. 
    var q3 = values[Math.ceil((values.length * (3 / 4)))][1];
    var iqr = q3 - q1;
    var newValues = [];
    if (q3 != q1) {
      // Then find min and max values
      var maxValue = d3.round(q3 + iqr * 1.5);
      var minValue = d3.round(q1 - iqr * 1.5);

      // Then filter anything beyond or beneath these values.
      var changeCount = 0;
      values.forEach(function(x) {
        var value = x[1];
        if (x[1] > maxValue) {
          value = maxValue;
          changeCount++;
        }
        newValues.push([x[0], value]);
      });
    } else {
      newValues = values;
    }

    newValues.sort(function(a, b) {
      return a[0] - b[0];
    });

    // Then return
    return newValues;
  }

  // Allow on() method to be invoked on this class
  // to handle data events
  d3.rebind(exports, dispatch, 'on');

  // Return this scope so that all subsequent calls
  // will be made on this scope.
  return exports;
}