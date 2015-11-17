// Generated on 2013-07-10 using generator-angular 0.3.0
'use strict';
var LIVERELOAD_PORT = 35729;
var modRewrite = require('connect-modrewrite');
var lrSnippet = require('connect-livereload')({ port: LIVERELOAD_PORT });
var mountFolder = function (connect, dir) {
  return connect.static(require('path').resolve(dir));
};





// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to recursively match all subfolders:
// 'test/spec/**/*.js'

module.exports = function (grunt) {
  // load all grunt tasks
  require('matchdep').filterDev('grunt-*').forEach(grunt.loadNpmTasks);

  // configurable paths
  var yeomanConfig = {
    app: 'app',
    dist: '../../../target/app',
    developIndexFile: 'develop/html/index.develop.html'
  };

// The purpose of the provider is to ensure that the appropriate configs 
// (for those registered via setConfigForTask() public method)
// are made available for the given environment (i.e. production vs development)
function ICGCGruntConfigProvider() {
    
    var _CONFIG_CONSTANTS = {
        BUILD_ENV: {DEV: 'development', PRODUCTION: 'production'}
      },
      _currentConfigBuildEnvironment = _CONFIG_CONSTANTS.BUILD_ENV.DEV,
      _configFunctionMap = {},
      _self = this;
    
    
    function _initTasks() {
    
       grunt.registerTask('ICGC-setBuildEnv', 'Sets the target build environment (default: ' +
                           _CONFIG_CONSTANTS.BUILD_ENV.DEV + ')', function(env) {
         var message = 'Setting GRUNT build environment to ';
         
          switch(env.toLowerCase()) {
            case _CONFIG_CONSTANTS.BUILD_ENV.PRODUCTION:
              _currentConfigBuildEnvironment = env;
            break;
            default:
              _currentConfigBuildEnvironment = _CONFIG_CONSTANTS.BUILD_ENV.DEV;
            break;
          }
          
          grunt.log.oklns(message + _currentConfigBuildEnvironment); 
          
          _updateAllTaskConfigs();
          
        });
    }
    
    function _updateAllTaskConfigs() {
      
      // Loop through the registered tasks and ensure we have the appropriate configs for them
      // (given the current set environment _currentConfigBuildEnvironment )
      for (var taskName in _configFunctionMap) {
        
        if ( _updateGruntConfigForTaskAndCurrentBuildEnv(taskName) ) {
          grunt.log.oklns('Assigning config for task \'' + taskName + 
                          '\' (Build Env: ' + _currentConfigBuildEnvironment + ')');
        }
        
      }
    }
    
    function _updateGruntConfigForTaskAndCurrentBuildEnv(taskName) {
      
      var currentGruntConfigForTask = grunt.config(taskName);
      
      if (typeof currentGruntConfigForTask === 'undefined') {
        return false;
      }
      
      var configuration = _self.getConfigForTask(taskName);
      
      if (typeof configuration !== 'undefined' && configuration !== null) { 
        grunt.config(taskName, configuration);
      }
      
      return true;  
    }
    
    function _init() {
        _initTasks();
    }
    
     _init();
     
     
    // Public APIs
    this.getConfigForTask = function(taskName) {
      var config = {},
          task = typeof taskName === 'string' ? taskName : null;
      
      if (typeof _configFunctionMap[task]  === 'undefined' || task === null) {
        return null;
      }
      
      config = _configFunctionMap[task].call(_self);
      
      return config;
    };
    
    this.setConfigForTask = function(taskName, config) {
      
      if (typeof taskName !== 'string' || config === null) {
        return null; 
      } 
      
      var task = taskName;
      
      // Handle the different "config" parameter types...
      switch(typeof config) {
        case 'function':
          _configFunctionMap[task] = config;
        break;
        case 'object': 
          _configFunctionMap[task] = function() { return config; };
        break;
        default:
        break;
      }
      
      return this;
    };
    
    this.isProductionBuild = function() {
      return _currentConfigBuildEnvironment === _CONFIG_CONSTANTS.BUILD_ENV.PRODUCTION;
    };
    
    
    
    return this;
  }
  
  var configProvider = new ICGCGruntConfigProvider();




  try {
    yeomanConfig.app = require('./bower.json').appPath || yeomanConfig.app;
  } 
  catch (e) {
  }



  grunt.initConfig({
    'bower-install-simple': configProvider.setConfigForTask('bower-install-simple', function() {
      
        /**
        * Bower configuration
        * See: https://www.npmjs.com/package/grunt-bower-install-simple
        */
        /*var bowerConfig = {
          options: {
            color: true
          },
          prod: {
            options: { production: true }
          },
          dev: {
            options: { production: false}
          }
        };*/
        
        var config =  {options: { color: true } };
        
        if (configProvider.isProductionBuild()) {
          config.prod = { options: { production: true, interactive: false, forceLatest: false } };
        }
        else {
          config.dev = { options: { production: false,  interactive: true, foceLatest: false } };
        }
            
        return config;  
    })
    // Gets the default dev config object in this context because
    // we have yet to set a default
    .getConfigForTask('bower-install-simple'), 
    peg: {
      pql: {
        src: './app/scripts/pegjs/pql.pegjs',
        dest: './app/scripts/common/pql/pqlparser.js',
        options: {
          exportVar: 'PqlPegParser'
        }
      }
    },
    yeoman: yeomanConfig,
    watch: {
      compass: {
        files: ['<%= yeoman.app %>/styles/{,*/}*.{scss,sass}'],
        tasks: ['compass:server']
      },
      livereload: {
        options: {
          livereload: LIVERELOAD_PORT
        },
        files: [
          '<%= yeoman.app %>/{,*/}*.html',
          '{.tmp,<%= yeoman.app %>}/styles/{,*/}*.css',
          '{.tmp,<%= yeoman.app %>}/scripts/{,*/}*.js',
          '<%= yeoman.app %>/develop/scripts/{,*/}*.js',
          '<%= yeoman.app %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}'
        ]
      }
    },
    connect: {
      options: {
        port: 9000,
        protocol: 'http',
        // Change this to '0.0.0.0' to access the server from outside.
        hostname: 'localhost'
      },
      livereload: {
        options: {
          middleware: function (connect) {
            return [
              modRewrite([
                '!\\.html|\\images|\\.js|\\.css|\\.png|\\.jpg|\\.woff|\\.ttf|\\.svg ' + 
                '/' + yeomanConfig.developIndexFile + ' [L]'
              ]),
              lrSnippet,
              mountFolder(connect, '.tmp'),
              mountFolder(connect, yeomanConfig.app)
            ];
          }
        }
      },
      test: {
        options: {
          port: 9009,
          middleware: function (connect) {
            return [
              mountFolder(connect, '.tmp'),
              mountFolder(connect, 'test')
            ];
          }
        }
      },
      dist: {
        options: {
          middleware: function (connect) {
            return [
              modRewrite([
                '!\\.html|\\images|\\.js|\\.css|\\.png|\\.jpg|\\.woff|\\.ttf|\\.svg ' +
                '/' + yeomanConfig.developIndexFile + ' [L]'
              ]),
              mountFolder(connect, yeomanConfig.dist)
            ];
          }
        }
      }
    },
    open: {
      server: {
        url: 'http://localhost:<%= connect.options.port %>'
      }
    },
    clean: {
      options: { force: true },
      dist: {
        files: [
          {
            dot: true,
            src: [
              '.tmp',
              '<%= yeoman.dist %>/*',
              '!<%= yeoman.dist %>/.git*'
            ]
          }
        ]
      },
      server: '.tmp'
    },
    jshint: {
      options: {
        jshintrc: '.jshintrc'
      },
      all: [
        'Gruntfile.js',
        '<%= yeoman.app %>/scripts/{,*/}*.js'
      ]
    },
    compass: {
      options: {
        sassDir: '<%= yeoman.app %>/styles',
        cssDir: '<%= yeoman.app %>/styles',
        generatedImagesDir: '<%= yeoman.app %>/images/generated',
        imagesDir: '<%= yeoman.app %>/images',
        javascriptsDir: '<%= yeoman.app %>/scripts',
        fontsDir: '<%= yeoman.app %>/styles/fonts',
        importPath: '<%= yeoman.app %>/vendor/styles',
        httpImagesPath: '/images',
        httpGeneratedImagesPath: '/images/generated',
        httpFontsPath: '/styles/fonts',
        relativeAssets: false,
        require: ['compass', 'bootstrap-sass', 'singularitygs', 'singularity-extras']
      },
      dist: {},
      server: {
        options: {
          //debugInfo: true
        }
      }
    },
    // not used since Uglify task does concat,
    // but still available if needed
    /*concat: {
     dist: {}
     },*/
    rev: {
      dist: {
        files: {
          src: [
            '<%= yeoman.dist %>/scripts/{,*/}*.js',
            '<%= yeoman.dist %>/styles/{,*/}*.css',
            '<%= yeoman.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
            '<%= yeoman.dist %>/styles/fonts/*'
          ]
        }
      }
    },
    useminPrepare: {
      html: '<%= yeoman.app %>/index.html',
      options: {
        dest: '<%= yeoman.dist %>'
      }
    },
    usemin: {
      html: ['<%= yeoman.dist %>/{,*/}*.html'],
      css: ['<%= yeoman.dist %>/styles/{,*/}*.css'],
      options: {
        dirs: ['<%= yeoman.dist %>']
      }
    },
    imagemin: {
      dist: {
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.app %>/images',
            // TODO: looks like imagemin is not processing module image deps
            // copy is doing this instead -
            // will research proper way to fix this 
            src: '{,*/}*.{png,jpg,jpeg}',
            dest: '<%= yeoman.dist %>/images'
          }
        ]
      }
    },
    cssmin: {
      // By default, your `index.html` <!-- Usemin Block --> will take care of
      // minification. This option is pre-configured if you do not wish to use
      // Usemin blocks.
      // dist: {
      //   files: {
      //     '<%= yeoman.dist %>/styles/main.css': [
      //       '.tmp/styles/{,*/}*.css',
      //       '<%= yeoman.app %>/styles/{,*/}*.css'
      //     ]
      //   }
      // }
    },
    htmlmin: {
      dist: {
        options: {
          /*removeCommentsFromCDATA: true,
           // https://github.com/yeoman/grunt-usemin/issues/44
           //collapseWhitespace: true,
           collapseBooleanAttributes: true,
           removeAttributeQuotes: true,
           removeRedundantAttributes: true,
           useShortDoctype: true,
           removeEmptyAttributes: true,
           removeOptionalTags: true*/
        },
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.app %>',
            src: ['*.html', 'views/*.html'],
            dest: '<%= yeoman.dist %>'
          }
        ]
      }
    },
    // Put files not handled in other tasks here
    copy: {
      dist: {
        files: [
          {
            expand: true,
            dot: true,
            cwd: '<%= yeoman.app %>',
            dest: '<%= yeoman.dist %>',
            src: [
              '*.{ico,png,txt}',
              '.htaccess',
              'bower_components/**',

              // 'vendor/scripts/angularjs/*',
              'vendor/styles/genomeviewer/**/*',
              'styles/images/**/*.{gif,webp,svg,png,jpg}',
              'styles/fonts/*',
              'views/**/*',
              'scripts/**/*.html',
              'data/*'
            ]
          },
          {
            expand: true,
            cwd: '.tmp/images',
            dest: '<%= yeoman.dist %>/images',
            src: [
              'generated/*'
            ]
          },
          // Genome viewer
          {
            expand: true,
            flatten: true,
            dot: true,
            cwd: '<%= yeoman.app %>',
            dest: '<%= yeoman.dist %>/styles/fonts/',
            src: ['vendor/scripts/genome-viewer/vendor/font-awesome/fonts/*' ]
          }
        ]
      }
    },
    concurrent: {
      server: [
        'compass:server'
      ],
      test: [
        'compass'
      ],
      dist: [
        //'compass:dist',
        'imagemin',
        'htmlmin'
      ]
    },
    karma: {
      unit: {
        configFile: './karma.conf.js',
        singleRun: true
      }
    },
    cdnify: {
      dist: {
        html: ['<%= yeoman.dist %>/*.html']
      }
    },
    ngmin: {
      dist: {
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.dist %>/scripts',
            src: 'scripts.js',
            dest: '<%= yeoman.dist %>/scripts'
          }
        ]
      }
    },
    uglify: {
      dist: {
        files: {
          '<%= yeoman.dist %>/scripts/scripts.js': [
            '<%= yeoman.dist %>/scripts/scripts.js'
          ]
        }
      }
    },
    injector: {
      options: {},
      dev: {
        options: {
          template: '<%= yeoman.app %>/index.html',
          destFile: '<%= yeoman.app %>/<%= yeoman.developIndexFile %>',
          relative: false,
          ignorePath: 'app'
        },
        files: [{
          expand: true,
          cwd: '<%= yeoman.app %>/develop/scripts',
          src: ['*.js']
        }]
      }
    }
  });

  grunt.registerTask('bower-install', ['bower-install-simple']);
  
  grunt.registerTask('server', function (target) {
    if (target === 'dist') {
      return grunt.task.run(['build',
        'connect:dist:keepalive']);
    }

    grunt.task.run([
      'ICGC-setBuildEnv:development',
      'injector:dev',
      'clean:server',
      'concurrent:server',
      'connect:livereload',
      //'open',
      'watch'
    ]);
  });

  grunt.registerTask('test', [
    'clean:server',
    'concurrent:test',
    'connect:test',
    'karma'
  ]);

  grunt.registerTask('build', [
    'ICGC-setBuildEnv:production',
    'clean:dist',
    'compass:dist', // run in case files were changed outside of grunt server (dev environment)
    'bower-install',
    'jshint',
    'peg',
    'karma',
    'useminPrepare',
    'concurrent:dist',
    'concat',
    'copy',
//    'cdnify',
    'ngmin',
    'cssmin',
    'uglify',
    'rev',
    'usemin'
  ]);

  grunt.registerTask('default', [
    //'jshint',
    'test',
    'build'
  ]);
};
