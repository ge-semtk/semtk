# lzmajs

[![Build Status][1]][2] [![dependency status][3]][4] [![dev dependency status][5]][6]

`lzmajs` is a fast pure-JavaScript implementation of LZMA
compression/decompression.  It was originally written by Gary Linscott
based on decompression code by Juan Mellado and the 7-Zip SDK.
C. Scott Ananian started by cleaning up the source code and packaging
it for `node` and `volo`, then moved on to more extensive refactoring
and validation against the upstream Java implementation, adding
test cases, etc.

## How to install

```
npm install lzma-purejs
```
or
```
volo add cscott/lzma-purejs
```

This package uses
[Typed Arrays](https://developer.mozilla.org/en-US/docs/JavaScript/Typed_arrays)
and so requires node.js >= 0.5.5.  Full browser compatibility table
is available at [caniuse.com](http://caniuse.com/typedarrays); briefly:
IE 10, Firefox 4, Chrome 7, or Safari 5.1.

## Testing

```
npm install
npm test
```

## Usage

There is a binary available in bin:
```
$ bin/lzmajs --help
$ echo "Test me" | bin/lzmajs -z > test.lzma
$ bin/lzmajs -d test.lzma
Test me
```

From JavaScript:
```
var lzmajs = require('lzma-purejs');
var data = new Buffer('Example data', 'utf8');
var compressed = lzma.compressFile(data);
var uncompressed = lzma.uncompressFile(compressed);
// convert from array back to string
var data2 = new Buffer(uncompressed).toString('utf8');
console.log(data2);
```
There is a streaming interface as well.

See the tests in the `tests/` directory for further usage examples.

## Documentation

`require('lzma-purejs')` returns a `lzmajs` object.  It contains two main
methods.  The first is a function accepting one, two, three or four
parameters:

`lzmajs.compressFile = function(input, [output], [Number compressionLevel] or [props], [progress])`

The `input` argument can be a "stream" object (which must implement the
`readByte` method), or a `Uint8Array`, `Buffer`, or array.

If you omit the second argument, `compressFile` will return a JavaScript
array containing the byte values of the compressed data.  If you pass
a second argument, it must be a "stream" object (which must implement the
`writeByte` method).

The third argument may be omitted, or a number between 1 and 9 indicating
a compression level (1 being largest/fastest compression and 9 being
smallest/slowest compression), or else an object with fields specifying
specific lzma encoder parameters; see `lib/Util.js` for details.

The fourth argument, if present, is a callback which will be invoked
multiple times as `progress(inSize, outSize)`, where inSize is the number of
input stream bytes which have currently been processed, and outSize is the
corresponding number of output bytes which have been generated.

The second exported method is a function accepting one or two parameters:

`lzmajs.decompressFile = function(input, [output])`

The `input` parameter is as above.

If you omit the second argument, `decompressFile` will return a
`Uint8Array`, `Buffer` or JavaScript array with the decompressed
data, depending on what your platform supports.  For most modern
platforms (modern browsers, recent node.js releases) the returned
value will be a `Uint8Array`.

If you provide the second argument, it must be a "stream", implementing
the `writeByte` method.

## Asynchronous streaming

See the `test/stream.js` for sample code using the `fibers` package
to implement an asynchronous de/compression interface.

## Related projects

* http://code.google.com/p/js-lzma Decompression code by Juan Mellado
* http://code.google.com/p/gwt-lzma/ and https://github.com/nmrugg/LZMA-JS
  are ports of the original Java code in the 7-Zip SDK
  using the GWT Java-to-JavaScript compiler.

## License

> Copyright (c) 2011 Gary Linscott
>
> Copyright (c) 2011-2012 Juan Mellado
>
> Copyright (c) 2013 C. Scott Ananian
>
> All rights reserved.
>
> Permission is hereby granted, free of charge, to any person obtaining a copy
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in
> all copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
> THE SOFTWARE.

[1]: https://travis-ci.org/cscott/lzma-purejs.png
[2]: https://travis-ci.org/cscott/lzma-purejs
[3]: https://david-dm.org/cscott/lzma-purejs.png
[4]: https://david-dm.org/cscott/lzma-purejs
[5]: https://david-dm.org/cscott/lzma-purejs/dev-status.png
[6]: https://david-dm.org/cscott/lzma-purejs#info=devDependencies
