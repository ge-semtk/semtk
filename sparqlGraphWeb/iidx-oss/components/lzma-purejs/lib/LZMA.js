if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(['./freeze', './LZMA/Decoder', './LZMA/Encoder'],function(freeze, Decoder, Encoder){
  'use strict';
  return freeze({
    Decoder: Decoder,
    Encoder: Encoder
  });
});
