#include <stddef.h>
#include "vsdk/toolkit/media/RGBAImageHDRUncompressed.h"
RGBAImageHDRUncompressed::RGBAImageHDRUncompressed() : xSize(0), ySize(0), data(NULL) {
}

RGBAImageHDRUncompressed::~RGBAImageHDRUncompressed() {
    delete[] data;
}

void RGBAImageHDRUncompressed::allocate(int w, int h) {
    xSize = w;
    ySize = h;
    delete[] data;
    data = new RGBAPixelHDR[w * h];
    // TCC 3.0 does not support value-initialization after new[].
    int i;
    for ( i = 0; i < w * h; i++ ) {
        data[i].r = 0;
        data[i].g = 0;
        data[i].b = 0;
        data[i].a = 0;
    }
}
