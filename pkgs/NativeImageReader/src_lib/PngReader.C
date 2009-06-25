//===========================================================================

#include "NativeImageReader.h"

//===========================================================================

/**
Return a new image information structure if everything goes fine. In any
error, NULL is returned.
*/
_NativeImageReaderHeaderInfo *
readPngHeader(FILE *fd)
{
    _NativeImageReaderHeaderInfo *HeaderInfo;

    HeaderInfo = new _NativeImageReaderHeaderInfo();

    //- 1. Acceso inicial a un archivo en formato PNG -----------------------
    png_uint_32 width, height;
    int bit_depth, color_type, interlace_type;

    // OJO: Notese que no se estan usando las funciones asincronicas de
    //      manejo de error, esto puede mejorarse.
    HeaderInfo->libpngEngine = png_create_read_struct(PNG_LIBPNG_VER_STRING,
                                           NULL, NULL, NULL);

    if ( !HeaderInfo->libpngEngine ) {
        delete HeaderInfo;
        return NULL;
    }

    /* Allocate/initialize the memory for image information.  REQUIRED. */
    HeaderInfo->imageInformation = png_create_info_struct(HeaderInfo->libpngEngine);
    if ( !HeaderInfo->imageInformation ) {
        png_destroy_read_struct(&HeaderInfo->libpngEngine, NULL, NULL);
        delete HeaderInfo;
        return NULL;
    }

    /* Set error handling if you are using the setjmp/longjmp method (this is
     * the normal method of doing things with libpng).  REQUIRED unless you
     * set up your own error handlers in the png_create_read_struct() earlier.
     */
    if ( setjmp(png_jmpbuf(HeaderInfo->libpngEngine)) ) {
        png_destroy_read_struct(&HeaderInfo->libpngEngine, &HeaderInfo->imageInformation, NULL);
        delete HeaderInfo;
        return NULL;
    }
    png_init_io(HeaderInfo->libpngEngine, fd);
    png_read_info(HeaderInfo->libpngEngine, HeaderInfo->imageInformation);
    png_get_IHDR(HeaderInfo->libpngEngine, HeaderInfo->imageInformation,
                 &width, &height, &bit_depth, 
                 &color_type, &interlace_type, NULL, NULL);
    HeaderInfo->xSize = width;
    HeaderInfo->ySize = height;

    //- 2. Configuracion de un formato de imagen compatible con AQUYNZA -----

    /* Extract multiple pixels with bit depths of 1, 2, and 4 from a single
     * byte into separate bytes (useful for paletted and grayscale images). */
    png_set_packing(HeaderInfo->libpngEngine);

    /* Expand paletted colors into true RGB triplets */
/*
    if (color_type == PNG_COLOR_TYPE_PALETTE) {
        png_set_palette_rgb(HeaderInfo->libpngEngine);
    }
*/

    if ( bit_depth != 8 ) {
        fprintf(stderr, "PngReader: ERROR -> only 8bpp depth images reading implemented!\n");
        fflush(stderr);
        delete HeaderInfo;
        return NULL;
    }

    HeaderInfo->channels = 3;

#ifdef NONONO
    /* Expand grayscale images to the full 8 bits from 1, 2, or 4 bits/pixel */
    if ( color_type == PNG_COLOR_TYPE_GRAY && bit_depth < 8 ) {
        png_set_gray_1_2_4_to_8(HeaderInfo->libpngEngine);
    }

    /* Expand paletted or RGB images with transparency to full alpha channels
     * so the data will be available as RGBA quartets.  */
    if ( png_get_valid(HeaderInfo->libpngEngine, HeaderInfo->imageInformation, PNG_INFO_tRNS)) {
        png_set_tRNS_to_alpha(HeaderInfo->libpngEngine);
    }
#endif

    /* Set the background color to draw transparent and alpha images over.
     * It is possible to set the red, green, and blue components directly
     * for paletted images instead of supplying a palette index.  Note that
     * even if the PNG file supplies a background, you are not required to
     * use it - you should use the (solid) application background if it has one
     */
    png_color_16 my_background, *image_background;

    if ( png_get_bKGD(HeaderInfo->libpngEngine, HeaderInfo->imageInformation, &image_background) ) {
        png_set_background(HeaderInfo->libpngEngine, image_background,
                           PNG_BACKGROUND_GAMMA_FILE, 1, 1.0);
      }
    else {
        png_set_background(HeaderInfo->libpngEngine, &my_background,
                           PNG_BACKGROUND_GAMMA_SCREEN, 0, 1.0);
    }

    /* Add filler (or alpha) byte (before/after each RGB triplet) */
//    png_set_filler(HeaderInfo->libpngEngine, 0xff, PNG_FILLER_AFTER);

    //- 4. Prueba: exportar la imagen a formato PPM -------------------------

    //- 3. Leida de la imagen, directamente a la memoria de usuaio ----------
    unsigned int number_passes;

    number_passes = png_set_interlace_handling(HeaderInfo->libpngEngine);

    if ( number_passes != 1 ) {
        fprintf(stderr, "PngReader: ERROR -> multiple passes image reading not implemented!\n");
        fflush(stderr);
        delete HeaderInfo;
        return NULL;
    }

    //-----------------------------------------------------------------------
    return HeaderInfo;
}

void
readPngDataRGB(_NativeImageReaderHeaderInfo *HeaderInfo, FILE *fd, BYTE *arr, BOOLEAN flip)
{
    //----------------------------------------------------------------------
    unsigned char **rowPointers;
    unsigned long int y;
    unsigned long int x, idx, base, size;
    BYTE r, g, b;

    rowPointers = new unsigned char *[1];
    rowPointers[0] = new unsigned char [3 * HeaderInfo->xSize];

    size = HeaderInfo->xSize*HeaderInfo->ySize*3;

    for ( y = 0; y < HeaderInfo->ySize; y++ ) {
        png_read_rows(HeaderInfo->libpngEngine, &rowPointers[0], NULL, 1);

        if ( flip ) {
            base = size - (1+y) * (HeaderInfo->xSize*3);
        }
        else {
            base = y * (3*HeaderInfo->xSize);
        }

        for ( x = 0, idx = 0; x < HeaderInfo->xSize; x++, idx += 3, base += 3 ) {
            r = rowPointers[0][idx];
            g = rowPointers[0][idx+1];
            b = rowPointers[0][idx+2];
            arr[base] = r;
            arr[base+1] = g;
            arr[base+2] = b;
        }

    }

    delete rowPointers[0];
    delete rowPointers;

    png_read_end(HeaderInfo->libpngEngine, HeaderInfo->imageInformation);
    png_destroy_read_struct(&HeaderInfo->libpngEngine, &HeaderInfo->imageInformation, NULL);
}

void
readPngDataRGBA(_NativeImageReaderHeaderInfo *HeaderInfo, FILE *fd, BYTE *arr, BOOLEAN flip)
{
    //----------------------------------------------------------------------
    unsigned char **rowPointers;
    unsigned long int y;
    unsigned long int x, idx, base, size;
    BYTE r, g, b;

    rowPointers = new unsigned char *[1];
    rowPointers[0] = new unsigned char [3 * HeaderInfo->xSize];

    size = HeaderInfo->xSize*HeaderInfo->ySize*4;

    for ( y = 0; y < HeaderInfo->ySize; y++ ) {
        png_read_rows(HeaderInfo->libpngEngine, &rowPointers[0], NULL, 1);

        if ( flip ) {
            base = size - (1+y) * (HeaderInfo->xSize*4);
        }
        else {
            base = y * (3*HeaderInfo->xSize);
        }
        for ( x = 0, idx = 0; x < HeaderInfo->xSize; x++, idx += 3, base += 4 ) {
            r = rowPointers[0][idx];
            g = rowPointers[0][idx+1];
            b = rowPointers[0][idx+2];
            arr[base] = r;
            arr[base+1] = g;
            arr[base+2] = b;
            arr[base+3] = 255;
        }
    }

    delete rowPointers[0];
    delete rowPointers;

    png_read_end(HeaderInfo->libpngEngine, HeaderInfo->imageInformation);
    png_destroy_read_struct(&HeaderInfo->libpngEngine, &HeaderInfo->imageInformation, NULL);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
