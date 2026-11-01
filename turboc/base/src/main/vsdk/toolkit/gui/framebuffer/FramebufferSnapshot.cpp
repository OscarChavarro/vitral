#include <stddef.h>
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferSnapshot.h"
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
#include "vsdk/toolkit/media/XmsMemory.h"
#endif

FramebufferSnapshot::FramebufferSnapshot()
    : surface(NULL), rows(NULL),
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
      extended(NULL),
#endif
      rowBuffer(NULL), left(0), top(0), width(0), height(0), bytesPerPixel(1), stride(0)
{
}

FramebufferSnapshot::~FramebufferSnapshot()
{
    discard();
}

// The storage of height rows of stride bytes: an XMS block and a row buffer in real mode DOS, otherwise (or when
// there is no XMS) one block per row.
BOOLEAN
FramebufferSnapshot::allocate()
{
    rowBuffer = new unsigned char[stride];
    if ( !rowBuffer ) {
        return FALSE;
    }
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
    extended = new XmsMemory();
    if ( extended && extended->allocate((unsigned long)stride * (unsigned long)height) &&
         extended->isXmsBacked() ) {
        return TRUE;
    }
    delete extended;
    extended = NULL;
#endif
    rows = new unsigned char *[height];
    if ( !rows ) {
        return FALSE;
    }
    int i;
    for ( i = 0; i < height; i++ ) {
        rows[i] = NULL;
    }
    for ( i = 0; i < height; i++ ) {
        rows[i] = new unsigned char[stride];
        if ( !rows[i] ) {
            return FALSE;
        }
    }
    return TRUE;
}

// The bytes of a row: its own block, or the row buffer (filled from XMS when reading).
unsigned char *
FramebufferSnapshot::row(int index, BOOLEAN reading)
{
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
    if ( extended ) {
        if ( reading ) {
            extended->copyFrom((unsigned long)index * stride, rowBuffer, stride);
        }
        return rowBuffer;
    }
#else
    (void)reading;
#endif
    return rows[index];
}

void
FramebufferSnapshot::store(int index)
{
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
    if ( extended ) {
        extended->copyTo((unsigned long)index * stride, rowBuffer, stride);
    }
#else
    (void)index;
#endif
}

BOOLEAN
FramebufferSnapshot::capture(Framebuffer &target, int x, int y, int w, int h)
{
    discard();
    if ( w <= 0 || h <= 0 || x >= target.width() || y >= target.height() ) {
        return FALSE;
    }
    long right = (long)x + w;
    long bottom = (long)y + h;
    if ( right <= 0 || bottom <= 0 ) {
        return FALSE;
    }
    left = x < 0 ? 0 : x;
    top = y < 0 ? 0 : y;
    right = right > target.width() ? target.width() : right;
    bottom = bottom > target.height() ? target.height() : bottom;
    width = (int)right - left;
    height = (int)bottom - top;
    bytesPerPixel = (target.bitsPerPixel() + 7) / 8;
    if ( bytesPerPixel < 1 ) {
        bytesPerPixel = 1;
    }
    // Even, as XMS moves whole words.
    stride = ((unsigned int)width * (unsigned int)bytesPerPixel + 1) & ~1U;
    if ( !allocate() ) {
        discard();
        return FALSE;
    }
    surface = &target;
    int r;
    for ( r = 0; r < height; r++ ) {
        unsigned char *bytes = row(r, FALSE);
        int column;
        for ( column = 0; column < width; column++ ) {
            unsigned long value = target.nativePixel(left + column, top + r);
            int b;
            for ( b = 0; b < bytesPerPixel; b++ ) {
                bytes[column * bytesPerPixel + b] = (unsigned char)(value >> (8 * b));
            }
        }
        store(r);
    }
    return TRUE;
}

void
FramebufferSnapshot::restore()
{
    if ( !surface ) {
        return;
    }
    int r;
    for ( r = 0; r < height; r++ ) {
        const unsigned char *bytes = row(r, TRUE);
        int column;
        for ( column = 0; column < width; column++ ) {
            unsigned long value = 0;
            int b;
            for ( b = bytesPerPixel - 1; b >= 0; b-- ) {
                value = value << 8 | bytes[column * bytesPerPixel + b];
            }
            surface->putNativePixel(left + column, top + r, value);
        }
    }
    discard();
}

void
FramebufferSnapshot::discard()
{
    if ( rows ) {
        int i;
        for ( i = 0; i < height; i++ ) {
            delete[] rows[i];
        }
        delete[] rows;
        rows = NULL;
    }
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
    delete extended;
    extended = NULL;
#endif
    delete[] rowBuffer;
    rowBuffer = NULL;
    surface = NULL;
    width = 0;
    height = 0;
}
