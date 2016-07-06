if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(['./freeze','./RangeCoder/BitTreeDecoder','./RangeCoder/BitTreeEncoder','./RangeCoder/Decoder','./RangeCoder/Encoder'],function(freeze, BitTreeDecoder,BitTreeEncoder,Decoder,Encoder){
  'use strict';
  return freeze({
    BitTreeDecoder: BitTreeDecoder,
    BitTreeEncoder: BitTreeEncoder,
    Decoder: Decoder,
    Encoder: Encoder
  });
});
