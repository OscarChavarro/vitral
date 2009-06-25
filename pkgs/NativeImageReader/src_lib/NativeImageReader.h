#ifndef __NativeImageReader__
#define __NativeImageReader__

#include <stdio.h>
#include <png.h>
#ifndef png_jmpbuf
  #define png_jmpbuf(png_ptr) ((png_ptr)->jmpbuf)
#endif

#define TRUE 1
#define FALSE 0
#define BOOLEAN int
#define BYTE unsigned char

class _NativeImageReaderHeaderInfo
{
  public:
    unsigned long int xSize;
    unsigned long int ySize;
    unsigned long int channels;
    FILE *fd;

    // Use only on PngReader module, ignored (not used) on other modules
    png_structp libpngEngine;
    png_infop imageInformation;
};

extern _NativeImageReaderHeaderInfo *readPngHeader(FILE *fd);
extern void
readPngDataRGB(_NativeImageReaderHeaderInfo *HeaderInfo, FILE *fd, BYTE *arr, BOOLEAN flip);
extern void
readPngDataRGBA(_NativeImageReaderHeaderInfo *HeaderInfo, FILE *fd, BYTE *arr, BOOLEAN flip);

#endif
