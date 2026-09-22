//===========================================================================
//= NativeImageReader: a small native (C++ / libpng) image reading library  =
//=                                                                         =
//= This library is used from Java through the JNI wrapper contained in     =
//= src/NativeImageReaderWrapper.C, and it can also be used from plain C++  =
//= programs (see src/testImageReader.C).                                   =
//===========================================================================

#ifndef __NativeImageReader__
#define __NativeImageReader__

#include <stdio.h>
#include <png.h>

#define TRUE 1
#define FALSE 0
#define BOOLEAN int
#define BYTE unsigned char

#define NATIVE_IMAGE_READER_MESSAGE_SIZE 512

/**
Holds the state of an image reading operation in progress: the opened file,
the libpng engine associated with it, and the image geometry as reported by
the file header. Instances of this class are created by `readPngHeader` and
must be destroyed with `releasePngHeader`.
*/
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

    // Last error reported by this module for this reading operation
    char errorMessage[NATIVE_IMAGE_READER_MESSAGE_SIZE];

    _NativeImageReaderHeaderInfo();
};

/**
Opens the png stream `fd` and reads its header. Returns a new reading
context, or NULL if the image can not be read. Ownership of `fd` is
transferred to the returned context.
*/
extern _NativeImageReaderHeaderInfo *readPngHeader(FILE *fd);

/**
Decodes the pending image data of `HeaderInfo` in to `arr`, which must
provide space for xSize*ySize*3 bytes. Any input color type / bit depth is
converted to 8 bit RGB. If `flip` is TRUE, the first image row is written at
the end of `arr` (bottom-up order, as needed by OpenGL style textures).
Returns TRUE on success, FALSE otherwise (check HeaderInfo->errorMessage).
*/
extern BOOLEAN
readPngDataRGB(_NativeImageReaderHeaderInfo *HeaderInfo, BYTE *arr, BOOLEAN flip);

/**
Same as `readPngDataRGB`, but converting to 8 bit RGBA. `arr` must provide
space for xSize*ySize*4 bytes. Images without an alpha channel are completed
with an opaque alpha value.
*/
extern BOOLEAN
readPngDataRGBA(_NativeImageReaderHeaderInfo *HeaderInfo, BYTE *arr, BOOLEAN flip);

/**
Releases all the resources associated with a reading context, including its
file descriptor and any pending libpng structure.
*/
extern void releasePngHeader(_NativeImageReaderHeaderInfo *HeaderInfo);

#endif

//===========================================================================
//= EOF                                                                     =
//===========================================================================
