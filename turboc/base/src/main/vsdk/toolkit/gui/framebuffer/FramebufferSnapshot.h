#ifndef VITRAL_FRAMEBUFFER_SNAPSHOT_H
#define VITRAL_FRAMEBUFFER_SNAPSHOT_H

#include "vsdk/toolkit/common/VSDK.h"

class Framebuffer;
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
class XmsMemory;
#endif

// Saves exact native pixel values, including palette indexes, for one clipped rectangle.
// turboc port note: the values are kept in as many bytes as the depth of the surface needs (one up to 8 bits per
// pixel, two in 16, three in 24) and one row at a time, as a real mode program has 640 KB and blocks of 64 KB at most;
// in real mode DOS (TURBO_C_8086_REAL_MODE_ENABLED) the rows go to an XMS block when there is one.
class FramebufferSnapshot {
public:
    FramebufferSnapshot();
    ~FramebufferSnapshot();
    BOOLEAN capture(Framebuffer &surface, int x, int y, int width, int height);
    void restore();
    void discard();

private:
    Framebuffer *surface;
    unsigned char **rows;
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
    XmsMemory *extended;
#endif
    unsigned char *rowBuffer;
    int left;
    int top;
    int width;
    int height;
    int bytesPerPixel;
    unsigned int stride;

    FramebufferSnapshot(const FramebufferSnapshot &other);
    FramebufferSnapshot &operator=(const FramebufferSnapshot &other);

    BOOLEAN allocate();
    unsigned char *row(int index, BOOLEAN reading);
    void store(int index);
};

#endif
