#ifndef __COMPRESS__
#define __COMPRESS__

#include <stdio.h>

extern void initLZW(long int sizeEstimate,
		    size_t (*readf)(void *, size_t, size_t, void *),
                    size_t (*writef)(void *, size_t, size_t, void *),
                    void (*flushf)(void *));
extern void compress(void *fdIn, void *fdOut);
extern void decompress(void *fdIn, void *fdOut);

#endif /* __COMPRESS__ */
