/* global matchMedia */

define([
  'jquery'
], function($) {
  'use strict';

  var QueryHandler = function(query) {
    this.query = query;
    this.isMatched = false;
    this.listeners = [];

    this.match();
  };

  // Check if the query matches the page size and if
  // this is our first time matching.
  // When the query handle gets a match it should execute
  // all of its callbacks.
  QueryHandler.prototype.match = function() {
    var matchedBefore = this.isMatched;
    this.isMatched = matchMedia(this.query).matches;

    if (this.isMatched && !matchedBefore) {
      this.run();
    }

    return this.isMatched;
  };

  QueryHandler.prototype.addListener = function(callback) {
    this.listeners.push(callback);
    // If we've already matched for this size go ahead and run
    // the callback.
    if (this.isMatched) {
      callback();
    }
  };

  QueryHandler.prototype.removeListener = function(callback) {
    // TODO
  };

  QueryHandler.prototype.run = function() {
    $.each(this.listeners, function(i, listener) {
      listener();
    });
  };

  return QueryHandler;
});
