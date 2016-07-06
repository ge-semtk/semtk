#include <stdio.h>

typedef unsigned int  uint;
typedef unsigned char uc;
#define assert(a)

#define  DO(n)     for (int _=0; _<n; _++)
#define  TOP       (1<<24)
#define  BOT       (1<<16)

class RangeCoder
{
 uint  low, code, range, passed;
 FILE  *f;

 void OutByte (uc c)           { passed++; fputc(c,f); }
 uc   InByte ()                { passed++; return fgetc(f); }

public:

 uint GetPassed ()             { return passed; }
 void StartEncode (FILE *F)    { f=F; passed=low=0;  range= (uint) -1; }
 void FinishEncode ()          { DO(4)  OutByte(low>>24), low<<=8; }
 void StartDecode (FILE *F)    { passed=low=code=0;  range= (uint) -1;
                                 f=F; DO(4) code= code<<8 | InByte();
                               }

 void Encode (uint cumFreq, uint freq, uint totFreq) {
    assert(cumFreq+freq<totFreq && freq && totFreq<=BOT);
    low  += cumFreq * (range/= totFreq);
    range*= freq;
		printf("%d,%d\n",low,range);
    while ((low ^ low+range)<TOP || range<BOT && ((range= -low & BOT-1),1))
       OutByte(low>>24), range<<=8, low<<=8;
 }

 uint GetFreq (uint totFreq) {
   uint tmp= (code-low) / (range/= totFreq);
   if (tmp >= totFreq)  throw ("Input data corrupt"); // or force it to return
   return tmp;                                         // a valid value :)
 }

 void Decode (uint cumFreq, uint freq, uint totFreq) {
    assert(cumFreq+freq<totFreq && freq && totFreq<=BOT);
    low  += cumFreq*range;
    range*= freq;
    while ((low ^ low+range)<TOP || range<BOT && ((range= -low & BOT-1),1))
       code= code<<8 | InByte(), range<<=8, low<<=8;
 }
};

struct RangeCoder2 {
	unsigned long long low;
	uint range;
	FILE *file;
	unsigned char _cache;
	int _cacheSize;
	
	void StartEncode(FILE *f) { file = f; _cacheSize = 1; _cache = 0; low = 0; range = 0xFFFFFFFF; };
	
	void Encode(uint start, uint size, uint total) {
		low += start * (range /= total);
		range *= size;
		printf("%lld %d\n", low, range);
		while (range < (1<<24)) {
			range <<= 8;
			shiftlow();
		}
	}
	
	void shiftlow() {
    if ((uint)low < (uint)0xFF000000 || (int)(low >> 32) != 0)
    {
      unsigned char temp = _cache;
      do
      {
				fputc((unsigned char)(temp + (unsigned char)(low >> 32)), file);
        temp = 0xFF;
      }
      while(--_cacheSize != 0);
      _cache = (unsigned char)((uint)low >> 24);
    }
    _cacheSize++;
    low = (uint)low << 8;	
	}
	
	void FinishEncode() { for (int i = 0; i < 5; i++) shiftlow(); }
};

int main() {
	RangeCoder2 rc;
	FILE *f = fopen("test","wb");
	rc.StartEncode(f);
	for (int i = 0; i < 3; i++) {
		rc.Encode(0,6,20);
		rc.Encode(0,6,20);
		rc.Encode(6,2,20);
		rc.Encode(0,6,20);
	}
	rc.Encode(8,2,20);
	rc.FinishEncode();
	fclose(f);
	f = fopen("test","rb");
	for(;;)
	{
		int c = getc(f);
		if (c == -1) break;
		printf("%d,", c);
	}
	return 0;
}
