module.exports = function (grunt) {
    'use strict';

    var pkg = grunt.file.readJSON('package.json');

    // Project configuration
    grunt.initConfig({
        // Metadata
        pkg: pkg,
        banner: '/*! <%= pkg.name %> - v<%= pkg.version %> - ' +
            '<%= grunt.template.today("yyyy-mm-dd") %>\n' +
            '<%= pkg.homepage ? "* " + pkg.homepage + "\\n" : "" %>' +
            '* Copyright (c) <%= grunt.template.today("yyyy") %> <%= pkg.author.name %>;' +
            ' Licensed <%= pkg.license %> */\n',
        // Task configuration
        clean: {
            build: {
                src: ['build','dist','mock/sniffy.js','mock/sniffy.min.js']
            }
        },
        copy: {
            mock: {
                expand: true,
                cwd: 'dist/',
                src: ['sniffy.js','sniffy.min.js','sniffy.map'],
                dest: 'mock/'
            }
        },
        includereplace: {
            dist: {
                options: {
                  prefix: '//@@',
                  suffix: '',
                  processIncludeContents: function(includeContents, localVars, filePath) {
                    return filePath.endsWith('.js') ? includeContents : 
                      includeContents.replace(/\\/g, "\\\\").replace(/'/g, "\\'").replace(/\n/g, " ");
                  },
                  globals: {
                    version: '<%= pkg.version %>'
                  }
                },
                src: 'src/sniffy.js',
                dest: 'dist/sniffy.js'
            },
            iframe: {
                options: {
                  globals: {
                    version: '<%= pkg.version %>'
                  }
                },
                src: 'src/sniffy.iframe.html',
                dest: 'build/sniffy.iframe.html'
            }
        },
        uglify: {
            options: {
                banner: '<%= banner %>',
                sourceMap: true,
                sourceMapName: 'dist/sniffy.map'
            },
            dist: {
                src: 'dist/sniffy.js',
                dest: 'dist/sniffy.min.js'
            }
        },
        jshint: {
            options: {
                node: true,
                curly: true,
                eqeqeq: true,
                immed: true,
                latedef: true,
                newcap: true,
                noarg: true,
                sub: true,
                undef: true,
                unused: true,
                eqnull: true,
                browser: true,
                globals: { jQuery: true },
                boss: true
            },
            gruntfile: {
                src: 'Gruntfile.js'
            },
            dist: [
                'src/*.js'
            ]
        },
        less: {
            options: {
              compress: true,
              yuicompress: true,
              optimization: 2
            },
            dist: {
                files: {
                  "build/sniffy.css": "src/less/sniffy.less",
                  "build/sniffy.iframe.css": "src/less/sniffy.iframe.less"
                }
            }
        },
        htmlmin: {
            dist: {
                options: {
                    removeComments: true,
                    collapseWhitespace: true,
                    minifyCSS: true,
                    minifyJS: true
                },
                files: {
                    'build/sniffy.iframe.html': 'build/sniffy.iframe.html'
                }
            }
        },
        imageEmbed: {
            dist: {
              src: [ "build/sniffy.iframe.css" ],
              dest: "build/sniffy.iframe.embedded.css",
              options: {
                deleteAfterEncoding : false,
                preEncodeCallback: function () { return true; }
              }
            }
        },
        watch: {
            gruntfile: {
                files: '<%= jshint.gruntfile.src %>',
                tasks: ['jshint:gruntfile', 'less', 'includereplace:iframe', 'htmlmin', 'jshint', 'includereplace:dist', 'uglify', 'copy']
            },
            htmlmin: {
                files: 'src/*.html',
                tasks: ['includereplace:iframe','htmlmin:dist','jshint:dist','includereplace:dist','uglify:dist','copy:mock']
            },
            src: {
                files: 'src/*.js',
                tasks: ['jshint:dist','includereplace:dist','uglify:dist','copy:mock']
            },
            styles: {
                files: ['src/less/*.less'],
                tasks: ['less:dist','imageEmbed:dist','includereplace:iframe','htmlmin:dist','jshint:dist','includereplace:dist','uglify:dist','copy:mock'],
                options: {
                    nospawn: true
                }
            }
        },
        notify_hooks: {
            options: {
                enabled: true,
                max_jshint_notifications: 5, // maximum number of notifications from jshint output
                title: "Sniffy", // defaults to the name in package.json, or will use project directory's name
                success: true, // whether successful grunt executions should be notified automatically
                duration: 3 // the duration of notification in seconds, for `notify-send only
            }
        }
    });

    // These plugins provide necessary tasks
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-include-replace');
    grunt.loadNpmTasks('grunt-contrib-htmlmin');
    grunt.loadNpmTasks('grunt-notify');
    grunt.loadNpmTasks("grunt-image-embed");
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-notify');

    // Default task
    grunt.registerTask('default', ['less', 'imageEmbed', 'includereplace:iframe', 'htmlmin', 'jshint', 'includereplace:dist', 'uglify', 'copy']);

    grunt.task.run('notify_hooks');
};

