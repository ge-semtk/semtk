# Notes on the LZMA format

This package endeavors to maintain file-format compatibility with the
`lzma` command-line utility on Linux (which itself tries to provide
command-line compatibility with the `gzip`).  This utility is
found in the 7-Zip SDK distribution in `CPP/7zip/Bundles/LzmaCon/lzmp.cpp`.

## Running the Java reference implementation

The source code to this implementation is based on the Java source
in the 7-Zip SDK in `Java/SevenZip`.  This reference implementation can
be compiled and run as follows:

```
$ cd Java
$ mkdir out
$ find . -name "*.java" | xargs javac -d out
$ java -cp out SevenZip.LzmaAlone
LZMA (Java) 4.61  2008-11-23


Usage:  LZMA <e|d> [<switches>...] inputFile outputFile
  e: encode file
  d: decode file
  b: Benchmark
<Switches>
  -d{N}:  set dictionary - [0,28], default: 23 (8MB)
  -fb{N}: set number of fast bytes - [5, 273], default: 128
  -lc{N}: set number of literal context bits - [0, 8], default: 3
  -lp{N}: set number of literal pos bits - [0, 4], default: 0
  -pb{N}: set number of pos bits - [0, 4], default: 2
  -mf{MF_ID}: set Match Finder: [bt2, bt4], default: bt4
  -eos:   write End Of Stream marker
$ java -cp out SevenZip.LzmaAlone d sample0.lzma sample0.out
```

The following excerpt from `lzmp.cpp` gives the mapping between gzip-style
`-1` through `-9` options and the `LzmaAlone` options:

```
/* LZMA_Alone switches:
    -a{N}:  set compression mode - [0, 2], default: 2 (max)
    -d{N}:  set dictionary - [0,28], default: 23 (8MB)
    -fb{N}: set number of fast bytes - [5, 255], default: 128
    -lc{N}: set number of literal context bits - [0, 8], default: 3
    -lp{N}: set number of literal pos bits - [0, 4], default: 0
    -pb{N}: set number of pos bits - [0, 4], default: 2
    -mf{MF_ID}: set Match Finder: [bt2, bt3, bt4, bt4b, pat2r, pat2,
                pat2h, pat3h, pat4h, hc3, hc4], default: bt4
*/

struct lzma_option {
        short compression_mode;			// -a
        short dictionary;			// -d
        short fast_bytes;			// -fb
        const wchar_t *match_finder;		// -mf
        short literal_context_bits;		// -lc
        short literal_pos_bits;			// -lp
        short pos_bits;				// -pb
};

/* The following is a mapping from gzip/bzip2 style -1 .. -9 compression modes
 * to the corresponding LZMA compression modes. Thanks, Larhzu, for coining
 * these. */
const lzma_option option_mapping[] = {
        { 0,  0,  0,    NULL, 0, 0, 0},		// -0 (needed for indexing)
        { 0, 16, 64,  L"hc4", 3, 0, 2},		// -1
        { 0, 20, 64,  L"hc4", 3, 0, 2},		// -2
        { 1, 19, 64,  L"bt4", 3, 0, 2},		// -3
        { 2, 20, 64,  L"bt4", 3, 0, 2},		// -4
        { 2, 21, 128, L"bt4", 3, 0, 2},		// -5
        { 2, 22, 128, L"bt4", 3, 0, 2},		// -6
        { 2, 23, 128, L"bt4", 3, 0, 2},		// -7
        { 2, 24, 255, L"bt4", 3, 0, 2},		// -8
        { 2, 25, 255, L"bt4", 3, 0, 2},		// -9
};
```

## LZMA file format

The LZMA header is not well documented.  It's also a bit usual: it doesn't
appear to have a magic byte prefix, as one would expect from a modern file
format, and it not word-aligned.  Nevertheless:

```
  Offset Size  Description
    0     1    lc, lp and pb in encoded form
    1     4    dictSize (little endian)
    5     8    uncompressed size (little endian)
```
For the nine compression levels above, the five
[magic](http://en.wikipedia.org/wiki/List_of_file_signatures) bytes are:
```
   -1   5d 00 00 01 00
   -2   5d 00 00 10 00
   -3   5d 00 00 08 00
   -4   5d 00 00 10 00
   -5   5d 00 00 20 00
   -6   5d 00 00 40 00
   -7   5d 00 00 80 00
   -8   5d 00 00 00 01
   -9   5d 00 00 00 02
```

The `.lzma86` format adds an optional extra filter to better compress x86
executables.  It begins with a one-byte prefix, which is `0` for standard LZMA
and `1` to indicate that the x86 filter is applied.  The remainder of the
format is the same. (This package doesn't support lzma86 de/compression.)

## Range Coding notes

I recommend reading
[Lempel-Ziv-Markov chain algorithm](http://en.wikipedia.org/wiki/Lempel%E2%80%93Ziv%E2%80%93Markov_chain_algorithm)
and [Range encoding](http://en.wikipedia.org/wiki/Range_encoding) in
Wikipedia to understand `RangeCoder.Encoder` and `RangeCoder.Decoder`.
The
[source code to the RangeEncoder](http://git.tukaani.org/?p=xz-java.git;a=blob;f=src/org/tukaani/xz/rangecoder/RangeEncoder.java)
in [XZ](http://en.wikipedia.org/wiki/Xz) is also useful to read.
