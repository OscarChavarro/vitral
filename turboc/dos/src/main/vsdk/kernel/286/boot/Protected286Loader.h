#ifndef __PROTECTED_286_LOADER__
#define __PROTECTED_286_LOADER__

#include "kernel/Protected286Types.h"

#define PROTECTED_286_LOADER_MAX_DESCRIPTORS 248

/*
 * Real-mode half of the protected-image loader.  It is the sole code allowed
 * to use BIOS/DOS services; it completes before LMSW. The assembly gateway
 * consumes these values while DS still points at the conventional image.
 */
/* Turbo C++ 3 accepts C linkage on individual functions, but not a linkage
 * block containing data declarations.  Its global-data spelling is already
 * compatible with TASM /ml. */
extern Protected286Dword protected286ImageBase;
extern Protected286Dword protected286ImageBytes;
extern Protected286Dword protected286CodeBase;
extern Protected286Dword protected286DataBase;
extern Protected286Dword protected286StackBase;
extern Protected286Word protected286GatewaySegment;
extern Protected286Dword protected286PoolStart;
extern Protected286Dword protected286PoolLimit;
extern Protected286Dword protected286InitOffset;
extern Protected286Word protected286InitBytes;
extern Protected286Dword protected286ExitOffset;
extern Protected286Word protected286ExitBytes;
extern char protected286CommandLine[128];
extern char protected286CurrentDirectory[68];
extern Protected286Word protected286DescriptorCount;
extern Protected286Dword protected286DescriptorBases[PROTECTED_286_LOADER_MAX_DESCRIPTORS];
    extern Protected286Byte protected286DescriptorAccess[PROTECTED_286_LOADER_MAX_DESCRIPTORS];

extern "C" int protected286PrepareImage();
extern "C" void protected286DiscardImage();

#endif
