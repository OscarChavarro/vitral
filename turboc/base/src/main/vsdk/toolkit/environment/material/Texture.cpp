#include <stddef.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/material/Texture.h"

Texture::Texture():
    width(0),
    height(0),
    channels(0),
    data(NULL)
{
}

Texture::Texture(
    int inWidth,
    int inHeight,
    int inChannels,
    const unsigned char *inData):
    width(inWidth),
    height(inHeight),
    channels(inChannels),
    data(NULL)
{
    // C++ port note: a long (32 bits on every compiler of the port) instead
    // of a long long
    const long byteCount = (long)width * (long)height * (long)channels;
    if ( byteCount <= 0 || inData == NULL ) {
        return;
    }
    data = new unsigned char[byteCount];
    memcpy(data, inData, (size_t)byteCount);
}

Texture::~Texture() {
    delete[] data;
    data = NULL;
}

/**
C++ port note: the lambda of the cpp port: one channel of a pixel, in [0, 1]
*/
static float
channelValue(const unsigned char *data, int pixelIndex, int channel) {
    return (float)data[pixelIndex + channel] / 255.0F;
}

ColorRgb
Texture::evaluateColor(float u, float v) const {
    double u1 = u - Math::floor(u);
    double u0 = 1.0 - u1;
    double v1 = v - Math::floor(v);
    double v0 = 1.0 - v1;
    int i = (int)(u1 * width);
    int i1 = i + 1;
    int j = (int)(v1 * height);
    int j1 = j + 1;
    if ( i < 0 ) {
        i = 0;
    }
    if ( i >= width ) {
        i = width - 1;
    }
    if ( j < 0 ) {
        j = 0;
    }
    if ( j >= height ) {
        j = height - 1;
    }
    if ( i1 >= width ) {
        i1 -= width;
    }
    if ( j1 >= height ) {
        j1 -= height;
    }

    if ( !data ) {
        return ColorRgb(0.0, 0.0, 0.0);
    }

    const int pixelIndex00 = (j * width + i) * channels;
    const int pixelIndex01 = (j1 * width + i) * channels;
    const int pixelIndex10 = (j * width + i1) * channels;
    const int pixelIndex11 = (j1 * width + i1) * channels;

    double r00 = 0.0;
    double g00 = 0.0;
    double b00 = 0.0;
    double r10 = 0.0;
    double g10 = 0.0;
    double b10 = 0.0;
    double r01 = 0.0;
    double g01 = 0.0;
    double b01 = 0.0;
    double r11 = 0.0;
    double g11 = 0.0;
    double b11 = 0.0;

    switch ( channels ) {
        case 1:
            r00 = g00 = b00 = channelValue(data, pixelIndex00, 0);
            r10 = g10 = b10 = channelValue(data, pixelIndex10, 0);
            r01 = g01 = b01 = channelValue(data, pixelIndex01, 0);
            r11 = g11 = b11 = channelValue(data, pixelIndex11, 0);
            break;
        case 3:
        case 4: {
            r00 = channelValue(data, pixelIndex00, 0);
            g00 = channelValue(data, pixelIndex00, 1);
            b00 = channelValue(data, pixelIndex00, 2);
            r10 = channelValue(data, pixelIndex10, 0);
            g10 = channelValue(data, pixelIndex10, 1);
            b10 = channelValue(data, pixelIndex10, 2);
            r01 = channelValue(data, pixelIndex01, 0);
            g01 = channelValue(data, pixelIndex01, 1);
            b01 = channelValue(data, pixelIndex01, 2);
            r11 = channelValue(data, pixelIndex11, 0);
            g11 = channelValue(data, pixelIndex11, 1);
            b11 = channelValue(data, pixelIndex11, 2);
        }
            break;
        default:
            break;
    }

    const double r = 0.25 * (u0 * v0 * r00 + u1 * v0 * r10 + u0 * v1 * r01 + u1 * v1 * r11);
    const double g = 0.25 * (u0 * v0 * g00 + u1 * v0 * g10 + u0 * v1 * g01 + u1 * v1 * g11);
    const double b = 0.25 * (u0 * v0 * b00 + u1 * v0 * b10 + u0 * v1 * b01 + u1 * v1 * b11);
    return ColorRgb(r, g, b);
}
