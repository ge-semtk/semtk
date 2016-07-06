var assert = require('assert');
var lzmajs = require('../');
var fs = require('fs');

var LZ = lzmajs.LZ;
var LZMA = lzmajs.LZMA;
var RangeCoder = lzmajs.RangeCoder;
var Util = lzmajs.Util;

var min = function(a, b) {
        return a < b ? a : b;
};

var compareArray = function(a1, a2) {
        var i;
        if (a1.length !== a2.length) {
                throw 'lengths not equal';
        }
        for (i = 0; i < a1.length; i++) {
                if (a1[i] !== a2[i]) {
                        throw 'not equal at ' + i + ' a1[i]=' + a1[i] + ', a2[i]=' + a2[i];
                }
        }
};

var createInStream = function(data) {
        var inStream = {
          data: data,
          offset: 0,
          readByte: function(){
            return this.data[this.offset++];
          },
          read: function(buffer, bufOffset, length) {
            var bytesRead = 0;
            while (bytesRead < length && this.offset < this.data.length) {
              buffer[bufOffset++] = this.data[this.offset++];
              bytesRead++;
            }
            return bytesRead;
          }
        };
        return inStream;
};
var createOutStream = function() {
  return {
    pos: 0,
    data: [],
    writeByte: function(byte) { this.data[this.data.length] = byte; }
  };
};

describe('range coder', function() {
  var DEBUG = false;
  var testString = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  var makeStream = function() {
    return {
      pos: 0,
      data: [],
      writeByte: function(byte) { this.data[this.data.length] = byte; },
      readByte: function() { return this.data[this.pos++]; }
    };
  };
  it('encodeDirectBits->decodeDirectBits should be the identity', function() {
    var stream = makeStream();
    var enc = new RangeCoder.Encoder(stream);
    var i;
    for (i=0; i<testString.length; i++) {
      enc.encodeDirectBits(testString.charCodeAt(i), 16);
    }
    enc.flushData();
    // show output
    if (DEBUG)
      console.log(stream.data.length, new Buffer(stream.data).toString('hex'));
    // now decode this.
    var dec = new RangeCoder.Decoder(stream);
    for (i=0; i<testString.length; i++) {
      assert.equal(testString.charCodeAt(i), dec.decodeDirectBits(16));
    }
  });

  var testDecodeBit = function(probsLength, makeContext, name) {
    var stream = makeStream();
    var probs = RangeCoder.Encoder.initBitModels(null, probsLength);

    var enc = new RangeCoder.Encoder(stream);
    var i, j, c, bit, context;
    RangeCoder.Encoder.initBitModels(probs);
    context = 0;
    for (i=0; i<testString.length; i++) {
      c = testString.charCodeAt(i);
      for (j=15; j>=0; j--) {
        bit = (c >> j) & 1; // big-endian
        enc.encode(probs, context, bit);
        context = makeContext(context, bit, j);
      }
    }
    enc.flushData();

    // show output
    if (DEBUG)
      console.log(name||'', stream.data.length,
                  new Buffer(stream.data).toString('hex'));

    // now decode this.
    var dec = new RangeCoder.Decoder(stream);
    RangeCoder.Encoder.initBitModels(probs);
    context = 0;
    for (i=0; i<testString.length; i++) {
      c = 0;
      for (j=15; j>=0; j--) {
        bit = dec.decodeBit(probs, context);
        context = makeContext(context, bit, j);
        c = (c<<1) | bit;
      }
      assert.equal(testString.charCodeAt(i), c);
    }
  };

  it('encode->decodeBit (single context)', function() {
    testDecodeBit(1, function(){return 0;}, 'no context');
  });
  // running context
  var makeRunningContextTest = function(contextLength) {
    return function() {
      testDecodeBit(1<<contextLength, function(context, bit, _) {
        return ((context<<1) | bit) & ((1<<contextLength)-1);
      }, contextLength+'-bit context');
    };
  };
  var contextLength;
  for (contextLength=1; contextLength <= 16; contextLength++) {
    it('encode->decodeBit ('+contextLength+'-bit context)',
       makeRunningContextTest(contextLength));
  }
  // context based on bit position
  it('encode->decodeBit (bit-position-based context)', function() {
    testDecodeBit(16, function(_,__,pos) { return pos; }, 'bitpos');
  });
});

var testBitEncoder = function() {
        // Simple test for the range encoder
        var testSequence = [5, 1, 9, 8, 10, 15];
        var out = createOutStream();
        var i;

        var prob = RangeCoder.Encoder.initBitModels(null, 1);
        var rangeEncoder = new RangeCoder.Encoder(out);
        for (i = 0; i < testSequence.length; i++) {
          rangeEncoder.encode(prob, 0, testSequence[i]?1:0);
        }
        rangeEncoder.flushData();
        assert.deepEqual(out.data, [ 0, 249, 223, 15, 188 ]);
};
describe('range encoder', function() {
    it('should pass a simple test', testBitEncoder);
});

var testBitTreeEncoder = function(testSequence) {
        // Test the BitTreeEncoder, using LZMA.js decompression for verification
        var out = createOutStream();
        var i;

        var rangeEncoder = new RangeCoder.Encoder(out);
        var bitTreeEncoder = new RangeCoder.BitTreeEncoder(8);
        bitTreeEncoder.init();
        for (i = 0; i < testSequence.length; i++) {
                bitTreeEncoder.encode(rangeEncoder, testSequence[i]);
        }
        rangeEncoder.flushData();

        var bitTreeDecoder = new RangeCoder.BitTreeDecoder(8);
        bitTreeDecoder.init();
        var rangeDecoder = new RangeCoder.Decoder();
        rangeDecoder.setStream(createInStream(out.data));
        rangeDecoder.init();

        var result = [];
        for (i = 0; i < testSequence.length; i++) {
                result[result.length] = bitTreeDecoder.decode(rangeDecoder);
        }
        compareArray(result, testSequence);
};

var buildSequence = function(length, maxVal) {
        var sequence = [];
        var seed = 0xDEADBEEF;
        var i;
        for (i = 0; i < length; i++) {
                seed = ((seed * 73) + 0x1234567) % 0xFFFFFFFF;
                sequence[i] = seed % maxVal;
        }
        return sequence;
};

var testEncoder = function() {
        var out = createOutStream();
        var rangeEncoder = new RangeCoder.Encoder(out);
        var encoder = new LZMA.Encoder();
        encoder.create();
        encoder.init();

        var literalEncoder = new LZMA.Encoder.LiteralEncoder();
        literalEncoder.create(2, 3);
        literalEncoder.init();
        var subCoder = literalEncoder.getSubCoder(5, 11);
        assert.ok(subCoder !== null);

        var lenEncoder = new LZMA.Encoder.LenEncoder();
        lenEncoder.init(5);
        lenEncoder.encode(rangeEncoder, 1, 0);
        lenEncoder.encode(rangeEncoder, 20, 0);
        lenEncoder.encode(rangeEncoder, 199, 0);
        rangeEncoder.flushData();

        var lenPriceTableEncoder = new LZMA.Encoder.LenPriceTableEncoder();
        lenPriceTableEncoder.init();
};

var testBinTree = function(sequence) {
        var stream = createInStream(sequence);

        var blockSize = (1 << 12) + 0x20 + 275;
        var inWindow = new LZ.InWindow();
        inWindow.create(1 << 12, 0x20, 275);
        inWindow.setStream(stream);
        inWindow.init();

        // Test basics
        var remaining = min(sequence.length, blockSize);
        assert.equal(inWindow.getNumAvailableBytes(), remaining);
        assert.equal(inWindow.getIndexByte(0), sequence[0]);
        assert.equal(inWindow.getIndexByte(1), sequence[1]);
        inWindow.movePos();
        assert.equal(inWindow.getNumAvailableBytes(), remaining - 1);
        assert.equal(inWindow.getIndexByte(0), sequence[1]);

        // Test sequence matching
        var testSequenceRepeats = [0, 1, 2, 3, 5, 0, 1, 2, 3, 4];
        inWindow.setStream(createInStream(testSequenceRepeats));
        inWindow.init();
        assert.equal(inWindow.getMatchLen(5, 4, 8), 4);

        // Test BinTree
        stream = createInStream(sequence);
        var binTree = new LZ.BinTree();
        binTree.setType(4);
        binTree.create(1 << 22, 1 << 12, 0x20, 275);
        binTree.setStream(stream);
        binTree.init();

        assert.equal(binTree.getNumAvailableBytes(), sequence.length);
};

describe('small sequence', function() {
    var testSequenceSmall = [5, 112, 90, 8, 10, 153, 255, 0, 0, 15];
    it('should pass bit tree', function() {
        testBitTreeEncoder(testSequenceSmall);
    });
    it('should pass bin tree', function() {
        testBinTree(testSequenceSmall);
    });
});
describe('large sequence', function() {
    var testSequenceLarge = buildSequence(10000, 255);
    it('should pass bit tree', function() {
        testBitTreeEncoder(testSequenceLarge);
    });
    it('should pass bin tree', function() {
        testBinTree(testSequenceLarge);
    });
});
describe('encode', function() {
    it('should pass its tests', testEncoder);
});
