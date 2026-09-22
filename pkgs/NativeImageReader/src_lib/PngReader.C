//===========================================================================

#include <string.h>
#include <setjmp.h>

#include "NativeImageReader.h"

//===========================================================================

_NativeImageReaderHeaderInfo::_NativeImageReaderHeaderInfo()
{
    xSize = 0;
    ySize = 0;
    channels = 0;
    fd = NULL;
    libpngEngine = NULL;
    imageInformation = NULL;
    errorMessage[0] = '\0';
}

static void
reportError(_NativeImageReaderHeaderInfo *HeaderInfo, const char *message)
{
    if ( HeaderInfo == NULL ) {
        return;
    }
    strncpy(HeaderInfo->errorMessage, message,
            NATIVE_IMAGE_READER_MESSAGE_SIZE - 1);
    HeaderInfo->errorMessage[NATIVE_IMAGE_READER_MESSAGE_SIZE - 1] = '\0';
}

/**
Releases the libpng structures of a reading context, but not the context
itself nor its file descriptor.
*/
static void
destroyLibpngEngine(_NativeImageReaderHeaderInfo *HeaderInfo)
{
    if ( HeaderInfo->libpngEngine != NULL ) {
        png_destroy_read_struct(&HeaderInfo->libpngEngine,
            HeaderInfo->imageInformation != NULL ?
                &HeaderInfo->imageInformation : NULL,
            NULL);
        HeaderInfo->libpngEngine = NULL;
        HeaderInfo->imageInformation = NULL;
    }
}

void
releasePngHeader(_NativeImageReaderHeaderInfo *HeaderInfo)
{
    if ( HeaderInfo == NULL ) {
        return;
    }
    destroyLibpngEngine(HeaderInfo);
    if ( HeaderInfo->fd != NULL ) {
        fclose(HeaderInfo->fd);
        HeaderInfo->fd = NULL;
    }
    delete HeaderInfo;
}

//===========================================================================

/**
Return a new image information structure if everything goes fine. In any
error, NULL is returned.
*/
_NativeImageReaderHeaderInfo *
readPngHeader(FILE *fd)
{
    if ( fd == NULL ) {
        return NULL;
    }

    _NativeImageReaderHeaderInfo *HeaderInfo;

    HeaderInfo = new _NativeImageReaderHeaderInfo();
    HeaderInfo->fd = fd;

    //- 1. Initial access to a file in PNG format ---------------------------
    HeaderInfo->libpngEngine = png_create_read_struct(PNG_LIBPNG_VER_STRING,
                                                      NULL, NULL, NULL);

    if ( !HeaderInfo->libpngEngine ) {
        HeaderInfo->fd = NULL;
        delete HeaderInfo;
        return NULL;
    }

    /* Allocate/initialize the memory for image information.  REQUIRED. */
    HeaderInfo->imageInformation =
        png_create_info_struct(HeaderInfo->libpngEngine);
    if ( !HeaderInfo->imageInformation ) {
        destroyLibpngEngine(HeaderInfo);
        HeaderInfo->fd = NULL;
        delete HeaderInfo;
        return NULL;
    }

    /* Error handling with the setjmp/longjmp method, as libpng requires
     * when no custom error handler is installed. */
    if ( setjmp(png_jmpbuf(HeaderInfo->libpngEngine)) ) {
        destroyLibpngEngine(HeaderInfo);
        HeaderInfo->fd = NULL;
        delete HeaderInfo;
        return NULL;
    }

    png_init_io(HeaderInfo->libpngEngine, fd);
    png_read_info(HeaderInfo->libpngEngine, HeaderInfo->imageInformation);

    png_uint_32 width, height;
    int bit_depth, color_type, interlace_type;

    png_get_IHDR(HeaderInfo->libpngEngine, HeaderInfo->imageInformation,
                 &width, &height, &bit_depth,
                 &color_type, &interlace_type, NULL, NULL);

    HeaderInfo->xSize = width;
    HeaderInfo->ySize = height;
    HeaderInfo->channels = png_get_channels(HeaderInfo->libpngEngine,
                                            HeaderInfo->imageInformation);

    //-----------------------------------------------------------------------
    return HeaderInfo;
}

//===========================================================================

/**
Configures the libpng transformations needed to obtain, from any input color
type and bit depth, an 8 bit per sample image with exactly
`requestedChannels` channels (3 for RGB, 4 for RGBA).
*/
static void
configureOutputFormat(_NativeImageReaderHeaderInfo *HeaderInfo,
                      int requestedChannels)
{
    png_structp engine = HeaderInfo->libpngEngine;
    png_infop information = HeaderInfo->imageInformation;

    int colorType = png_get_color_type(engine, information);
    int bitDepth = png_get_bit_depth(engine, information);

    /* Palette based images are expanded to RGB triplets */
    if ( colorType == PNG_COLOR_TYPE_PALETTE ) {
        png_set_palette_to_rgb(engine);
    }

    /* Low bit depth gray scale images are expanded to full bytes */
    if ( colorType == PNG_COLOR_TYPE_GRAY && bitDepth < 8 ) {
        png_set_expand_gray_1_2_4_to_8(engine);
    }

    /* Simple transparency is expanded to a real alpha channel */
    if ( png_get_valid(engine, information, PNG_INFO_tRNS) ) {
        png_set_tRNS_to_alpha(engine);
    }

    /* 16 bit samples are reduced to 8 bit samples */
    if ( bitDepth == 16 ) {
        png_set_strip_16(engine);
    }

    /* Gray scale images are expanded to RGB triplets */
    if ( colorType == PNG_COLOR_TYPE_GRAY ||
         colorType == PNG_COLOR_TYPE_GRAY_ALPHA ) {
        png_set_gray_to_rgb(engine);
    }

    if ( requestedChannels == 4 ) {
        /* Adds an opaque alpha channel if the image does not provide one */
        png_set_add_alpha(engine, 0xFF, PNG_FILLER_AFTER);
    }
    else {
        /* Removes the alpha channel if the image provides one */
        png_set_strip_alpha(engine);
    }

    /* Interlaced images are handled by png_read_image */
    png_set_interlace_handling(engine);

    png_read_update_info(engine, information);
}

/**
Common implementation for `readPngDataRGB` and `readPngDataRGBA`. Note that
the destination buffer is used as the libpng row storage, so no intermediate
image copy is needed.
*/
static BOOLEAN
readPngData(_NativeImageReaderHeaderInfo *HeaderInfo, BYTE *arr, BOOLEAN flip,
            int requestedChannels)
{
    if ( HeaderInfo == NULL ) {
        return FALSE;
    }

    if ( arr == NULL ) {
        reportError(HeaderInfo, "Null destination buffer");
        return FALSE;
    }

    if ( HeaderInfo->libpngEngine == NULL ) {
        reportError(HeaderInfo, "Image data already read or reader not "
                    "correctly initialized");
        return FALSE;
    }

    png_bytep * volatile rowPointers = NULL;

    if ( setjmp(png_jmpbuf(HeaderInfo->libpngEngine)) ) {
        reportError(HeaderInfo, "libpng reported an error while decoding "
                    "the image");
        delete [] rowPointers;
        destroyLibpngEngine(HeaderInfo);
        return FALSE;
    }

    configureOutputFormat(HeaderInfo, requestedChannels);

    size_t rowBytes = png_get_rowbytes(HeaderInfo->libpngEngine,
                                       HeaderInfo->imageInformation);
    size_t expectedRowBytes = (size_t)HeaderInfo->xSize * requestedChannels;

    if ( rowBytes != expectedRowBytes ) {
        char msg[NATIVE_IMAGE_READER_MESSAGE_SIZE];
        snprintf(msg, sizeof(msg),
                 "Unsupported png pixel format: %d bytes per row obtained, "
                 "%d expected", (int)rowBytes, (int)expectedRowBytes);
        reportError(HeaderInfo, msg);
        destroyLibpngEngine(HeaderInfo);
        return FALSE;
    }

    rowPointers = new png_bytep[HeaderInfo->ySize];

    unsigned long int y;

    for ( y = 0; y < HeaderInfo->ySize; y++ ) {
        unsigned long int destinationRow = flip ? (HeaderInfo->ySize - 1 - y) : y;
        rowPointers[y] = (png_bytep)(arr + destinationRow * rowBytes);
    }

    png_read_image(HeaderInfo->libpngEngine, rowPointers);
    png_read_end(HeaderInfo->libpngEngine, NULL);

    delete [] rowPointers;
    destroyLibpngEngine(HeaderInfo);

    return TRUE;
}

BOOLEAN
readPngDataRGB(_NativeImageReaderHeaderInfo *HeaderInfo, BYTE *arr, BOOLEAN flip)
{
    return readPngData(HeaderInfo, arr, flip, 3);
}

BOOLEAN
readPngDataRGBA(_NativeImageReaderHeaderInfo *HeaderInfo, BYTE *arr, BOOLEAN flip)
{
    return readPngData(HeaderInfo, arr, flip, 4);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
