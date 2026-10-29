#ifndef __XMS_MEMORY__
#define __XMS_MEMORY__

#if defined(TURBO_C_286_ENABLED)
#error XMS storage is forbidden in protected 286 mode
#endif
#if defined(__TURBOC__) && !defined(TURBO_C_8086_REAL_MODE_ENABLED)
#error XMS storage requires TURBO_C_8086_REAL_MODE_ENABLED
#endif

#include "vsdk/toolkit/common/VSDK.h"

/**
Storage backed by the DOS XMS manager when compiled with Turbo C++.

XMS blocks cannot be addressed by the CPU in real mode.  They are accessed
through XMS move operations, so this class intentionally exposes copies rather
than a raw pointer.  On non-Turbo-C hosts it uses ordinary memory solely to
allow the DOS module's API to be built and checked there.
*/
class XmsMemory {
private:
    unsigned long allocatedBytes;
#ifdef __TURBOC__
    unsigned short handle;
#else
    char* data;
#endif

    XmsMemory(const XmsMemory& other);
    XmsMemory& operator=(const XmsMemory& other);

public:
    XmsMemory();
    ~XmsMemory();

    BOOLEAN allocate(unsigned long requestedBytes);
    void release();
    BOOLEAN clear();
    BOOLEAN copyTo(unsigned long offset, const void* source,
                   unsigned int byteCount);
    BOOLEAN copyFrom(unsigned long offset, void* destination,
                     unsigned int byteCount) const;

    unsigned long size() const;
    BOOLEAN isXmsBacked() const;
};

#endif
