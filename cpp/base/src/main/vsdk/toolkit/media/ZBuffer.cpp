#include "vsdk/toolkit/media/ZBuffer.h"
ZBuffer::ZBuffer(int width, int height) : xSize(width), ySize(height) {
    depth = new float[xSize * ySize];
    for (int i = 0; i < xSize * ySize; i++) {
        depth[i] = 0.0f;
    }
}

ZBuffer::ZBuffer(float* dep, int width, int height) : xSize(width), ySize(height) {
    depth = new float[xSize * ySize];

    int pos = 0;
    for (int y = ySize - 1; y >= 0; y--) {
        for (int x = 0; x < xSize; x++) {
            depth[xSize * y + x] = dep[pos];
            pos++;
        }
    }
}

ZBuffer::~ZBuffer() {
    if (depth != nullptr) {
        delete[] depth;
        depth = nullptr;
    }
}

int ZBuffer::getXSize() const {
    return xSize;
}

int ZBuffer::getYSize() const {
    return ySize;
}

float* ZBuffer::getZBuffer() const {
    if (depth == nullptr) {
        return nullptr;
    }
    float* copy = new float[xSize * ySize];
    for (int i = 0; i < xSize * ySize; i++) {
        copy[i] = depth[i];
    }
    return copy;
}

float ZBuffer::getDepth(int x, int y) const {
    if (x < 0 || x >= xSize || y < 0 || y >= ySize) {
        return 0.0f;
    }
    return depth[y * xSize + x];
}

void ZBuffer::setDepth(int x, int y, float value) {
    if (x >= 0 && x < xSize && y >= 0 && y < ySize) {
        depth[y * xSize + x] = value;
    }
}

ZBuffer* ZBuffer::clone() const {
    ZBuffer* copy = new ZBuffer(xSize, ySize);
    for (int i = 0; i < xSize * ySize; i++) {
        copy->depth[i] = depth[i];
    }
    return copy;
}
