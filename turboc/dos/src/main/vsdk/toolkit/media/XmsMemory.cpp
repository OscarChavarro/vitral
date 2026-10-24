#include <string.h>

#include "vsdk/toolkit/media/XmsMemory.h"

#ifdef __TURBOC__
#include <dos.h>
#include <mem.h>

typedef void (far *XmsEntryPoint)(void);

struct XmsMoveDescriptor {
    unsigned long length;
    unsigned short sourceHandle;
    unsigned long sourceOffset;
    unsigned short destinationHandle;
    unsigned long destinationOffset;
};

static XmsEntryPoint xmsEntryPoint = 0;

static BOOLEAN xmsReady()
{
    union REGS registers;
    struct SREGS segments;

    if ( xmsEntryPoint != 0 ) {
        return TRUE;
    }
    registers.x.ax = 0x4300;
    int86(0x2F, &registers, &registers);
    if ( registers.h.al != 0x80 ) {
        return FALSE;
    }
    registers.x.ax = 0x4310;
    int86x(0x2F, &registers, &registers, &segments);
    xmsEntryPoint = (XmsEntryPoint)MK_FP(segments.es, registers.x.bx);
    return xmsEntryPoint != 0;
}

static unsigned long conventionalAddress(const void far* pointer)
{
    return (((unsigned long)FP_SEG(pointer)) << 16) | FP_OFF(pointer);
}

static BOOLEAN xmsMove(XmsMoveDescriptor far* descriptor)
{
    if ( xmsEntryPoint == 0 ) {
        return FALSE;
    }

    // XMS function 0Bh receives its descriptor in DS:SI.  ES:BX addresses
    // the far function pointer itself, allowing the indirect far call while
    // DS is temporarily used for the descriptor.
    asm push es;
    asm push ds;
    _ES = FP_SEG(&xmsEntryPoint);
    _BX = FP_OFF(&xmsEntryPoint);
    _DS = FP_SEG(descriptor);
    _SI = FP_OFF(descriptor);
    _AX = 0x0B00;
    asm call dword ptr es:[bx];
    asm pop ds;
    asm pop es;
    return _AX == 1;
}
#endif

XmsMemory::XmsMemory()
    : allocatedBytes(0)
#ifdef __TURBOC__
    , handle(0)
#else
    , data(0)
#endif
{
}

XmsMemory::~XmsMemory()
{
    release();
}

BOOLEAN XmsMemory::allocate(unsigned long requestedBytes)
{
    release();
    if ( requestedBytes == 0 ) {
        return FALSE;
    }

#ifdef __TURBOC__
    unsigned long kilobytes = (requestedBytes + 1023UL) / 1024UL;
    if ( kilobytes == 0 || kilobytes > 65535UL || !xmsReady() ) {
        return FALSE;
    }
    _AX = 0x0900;
    _DX = (unsigned short)kilobytes;
    (*xmsEntryPoint)();
    if ( _AX != 1 ) {
        return FALSE;
    }
    handle = _DX;
    allocatedBytes = kilobytes * 1024UL;
#else
    data = new char[requestedBytes];
    if ( data == 0 ) {
        return FALSE;
    }
    allocatedBytes = requestedBytes;
#endif
    return TRUE;
}

void XmsMemory::release()
{
#ifdef __TURBOC__
    if ( handle != 0 && xmsEntryPoint != 0 ) {
        _AX = 0x0A00;
        _DX = handle;
        (*xmsEntryPoint)();
    }
    handle = 0;
#else
    delete[] data;
    data = 0;
#endif
    allocatedBytes = 0;
}

BOOLEAN XmsMemory::copyTo(unsigned long offset, const void* source,
                          unsigned int byteCount)
{
    if ( source == 0 || byteCount == 0 || offset + byteCount > allocatedBytes ) {
        return FALSE;
    }
#ifdef __TURBOC__
    XmsMoveDescriptor descriptor;
    descriptor.length = byteCount;
    descriptor.sourceHandle = 0;
    descriptor.sourceOffset = conventionalAddress(source);
    descriptor.destinationHandle = handle;
    descriptor.destinationOffset = offset;
    return xmsMove(&descriptor);
#else
    memcpy(data + offset, source, byteCount);
    return TRUE;
#endif
}

BOOLEAN XmsMemory::copyFrom(unsigned long offset, void* destination,
                            unsigned int byteCount) const
{
    if ( destination == 0 || byteCount == 0 || offset + byteCount > allocatedBytes ) {
        return FALSE;
    }
#ifdef __TURBOC__
    XmsMoveDescriptor descriptor;
    descriptor.length = byteCount;
    descriptor.sourceHandle = handle;
    descriptor.sourceOffset = offset;
    descriptor.destinationHandle = 0;
    descriptor.destinationOffset = conventionalAddress(destination);
    return xmsMove(&descriptor);
#else
    memcpy(destination, data + offset, byteCount);
    return TRUE;
#endif
}

BOOLEAN XmsMemory::clear()
{
    static char zeroes[512];
    unsigned long offset = 0;
    memset(zeroes, 0, sizeof(zeroes));
    while ( offset < allocatedBytes ) {
        unsigned int count = (allocatedBytes - offset > sizeof(zeroes)) ?
            sizeof(zeroes) : (unsigned int)(allocatedBytes - offset);
        // XMS Move requires an even length. Allocations are KiB-sized, so the
        // final transfer is always even as well.
        if ( !copyTo(offset, zeroes, count) ) {
            return FALSE;
        }
        offset += count;
    }
    return TRUE;
}

unsigned long XmsMemory::size() const
{
    return allocatedBytes;
}

BOOLEAN XmsMemory::isXmsBacked() const
{
#ifdef __TURBOC__
    return handle != 0;
#else
    return FALSE;
#endif
}
