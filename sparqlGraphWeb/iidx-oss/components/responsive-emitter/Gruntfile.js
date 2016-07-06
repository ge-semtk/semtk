/*global module:false*//*jshint unused:false*/

'use strict';

var path = require('path'),
    bower = require('bower');

module.exports = function(grunt) {

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    clean: {
      bower: ['components']
    },

    // Create requirejs config
    bower: {
      options: {
        baseUrl: './'
      },
      target: {
        rjsConfig: 'js/require.config.js'
      }
    },

    watch: {
      component: {
        files: ['js/**/*', 'test/**/*'],
        tasks: 'default',
        options: {
          interrupt: true
        }
      }
    }

  });

  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-bower-requirejs');

  // Default task.
  grunt.registerTask('default', 'Build the LESS for testing', [
    'clean',
    'bower-install',
    'bower'
  ]);

  // Execute Bower to install dependencies.
  grunt.registerTask('bower-install', function () {
    var done = this.async();
    try {
      bower.commands
        .install()
        .on('data', function (data) {
          grunt.log.writeln(data);
        })
        .on('end', function () {
          grunt.log.ok();
          done();
        })
        .on('error', function (error) {
          grunt.fail.fatal(error);
        });
    } catch(error) {
      grunt.fail.fatal(error);
    }
  });

};
