#ifndef __PROTECTED_286_CRT__
#define __PROTECTED_286_CRT__

#include "kernel/Protected286Types.h"

/*
 * This is intentionally not a replacement for <stdio.h>.  The Borland CRT
 * owns real-mode DOS handles and its FILE objects must not be used after PE
 * is set.  Protected programs use this very small ABI instead.  A FAT
 * implementation will install the backend before the raytracer is entered.
 */
class Protected286FileSystem {
public:
    virtual ~Protected286FileSystem() {}
    virtual int open(const char* path, Protected286Byte writable) = 0;
    virtual long read(int handle, void far* destination, long count) = 0;
    virtual long write(int handle, const void far* source, long count) = 0;
    virtual void close(int handle) = 0;
    virtual long seek(int handle, long offset, int origin) = 0;
    virtual int nextRootEntry(Protected286Word* cursor, char far* name) = 0;
};

class Protected286Crt {
private:
    static Protected286FileSystem* fileSystem;

public:
    static void initialize();

    /* Console input/output are direct VGA and 8042 operations. */
    static void write(const char* text);
    static void writeLine(const char* text);
    static void writeUnsigned(Protected286Dword value);
    static int getCharacter();

    /* These are the only file entry points allowed after protected entry. */
    static void installFileSystem(Protected286FileSystem* backend);
    static int openRead(const char* path);
    static int openWrite(const char* path);
    static long read(int handle, void far* destination, long count);
    static long write(int handle, const void far* source, long count);
    static void close(int handle);
    static long seek(int handle, long offset, int origin);
    static int nextRootEntry(Protected286Word* cursor, char far* name);
};

#endif
