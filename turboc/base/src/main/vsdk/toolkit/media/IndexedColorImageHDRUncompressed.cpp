#include <stddef.h>
#include "vsdk/toolkit/media/IndexedColorImageHDRUncompressed.h"
IndexedColorImageHDRUncompressed::IndexedColorImageHDRUncompressed() : data(NULL), xSize(0), ySize(0), colorMapSize(0), colorTable(NULL) {
}

IndexedColorImageHDRUncompressed::~IndexedColorImageHDRUncompressed() {
    delete[] data;
    delete[] colorTable;
}

int
IndexedColorImageHDRUncompressed::getXSize() const {
    return xSize;
}

int
IndexedColorImageHDRUncompressed::getYSize() const {
    return ySize;
}

void
IndexedColorImageHDRUncompressed::setXSize(int w) {
    xSize = w;
}

void
IndexedColorImageHDRUncompressed::setYSize(int h) {
    ySize = h;
}

int
IndexedColorImageHDRUncompressed::getColorMapSize() const {
    return colorMapSize;
}

void
IndexedColorImageHDRUncompressed::setColorMapSize(int n) {
    colorMapSize = n;
}

RGBAPixelHDR *
IndexedColorImageHDRUncompressed::getColorTable() const {
    return colorTable;
}

void
IndexedColorImageHDRUncompressed::setColorTable(RGBAPixelHDR *ct) {
    colorTable = ct;
}

void
IndexedColorImageHDRUncompressed::allocate(int w, int h) {
    xSize = w;
    ySize = h;
    delete[] data;
    data = new unsigned char[w * h];
    int i;
    for ( i = 0; i < w * h; i++ ) data[i] = 0;
}
