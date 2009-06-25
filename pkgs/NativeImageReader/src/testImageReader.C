//===========================================================================

#include "NativeImageReader.h"

static void
exportArrayAsPPM(_NativeImageReaderHeaderInfo *HeaderInfo, BYTE *arr, const char *output)
{
    unsigned long int y;
    unsigned long int x, idx;
    BYTE r, g, b;
    FILE *fdout = fopen(output, "wb");

    fprintf(fdout, "P6\n%d %d\n255\n", (int)HeaderInfo->xSize, (int)HeaderInfo->ySize);

    for ( y = 0, idx = 0; y < HeaderInfo->ySize; y++ ) {
        for ( x = 0; x < HeaderInfo->xSize; x++, idx += 3 ) {
            r = arr[idx];
            g = arr[idx+1];
            b = arr[idx+2];
            fwrite(&r, 1, 1, fdout);
            fwrite(&g, 1, 1, fdout);
            fwrite(&b, 1, 1, fdout);
        }
    }

    fclose(fdout);
}

static void
testPngReader(const char *input, const char *output)
{
    FILE *fd;
    _NativeImageReaderHeaderInfo *HeaderInfo;

    fd = fopen(input, "rb");

    HeaderInfo = readPngHeader(fd);

    BYTE *arr;
    arr = new BYTE[HeaderInfo->xSize * HeaderInfo->ySize * 3];

    printf("Processing image of %d x %d pixels... ", (int)HeaderInfo->xSize, (int)HeaderInfo->ySize);
    fflush(stdout);
    readPngDataRGB(HeaderInfo, fd, arr, FALSE);
    printf("Ok!\n");
    fflush(stdout);

    printf("Exporting test image on PPM format... ");
    fflush(stdout);
    exportArrayAsPPM(HeaderInfo, arr, output);
    printf("Ok!\n");
    fflush(stdout);

    delete arr;
    delete HeaderInfo;

    fclose(fd);
}

int
main(int argc, char *argv[])
{
    testPngReader("input.png", "output.ppm");
    return 0;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
