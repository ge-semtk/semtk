if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(['./freeze','./LZ/BinTree','./LZ/InWindow','./LZ/OutWindow'],function(freeze,BinTree,InWindow,OutWindow){
  'use strict';
  return freeze({
    BinTree: BinTree,
    InWindow: InWindow,
    OutWindow: OutWindow
  });
});
