#ifndef __FILESYSTEM__
#define __FILESYSTEM__
#include <stdint.h>
#include "fs/Err.h"
#include <stddef.h>
#include "fs/PollEvent.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"

class BlockDevice;
class CharDevice;
class Filesystem;

class Filesystem {
public:
    virtual ~Filesystem() {}
    Filesystem(const Filesystem &) = delete;
    Filesystem &operator=(const Filesystem &) = delete;

    virtual const char *type() const = 0;
    virtual bool root(Inode &out) = 0;
    virtual int64_t lookup(const Inode &dir, const char *name, Inode &out) = 0;
    virtual int64_t readDir(const Inode &dir, uint64_t &cursor, DirEntry &out) = 0;

    virtual int64_t read(Inode &, uint64_t, void *, size_t) { return -Err::INVAL; }
    virtual int64_t write(Inode &, uint64_t, const void *, size_t) { return -Err::INVAL; }
    virtual int64_t truncate(Inode &, uint64_t) { return -Err::INVAL; }
    virtual int64_t create(const Inode &, const char *, uint32_t, Inode &) { return -Err::PERM; }
    virtual int64_t remove(const Inode &, const char *, bool) { return -Err::PERM; }
    virtual int64_t refresh(Inode &) { return 0; }
    virtual int64_t sync() { return 0; }
    virtual int poll(Inode &, int events) { return events & (PollEvent::IN | PollEvent::OUT); }
    virtual uint32_t blockSize() const { return 512; }

    uint64_t id() const { return idValue; }
    unsigned users = 0;

protected:
    Filesystem() : idValue(__atomic_fetch_add(&nextId, 1, __ATOMIC_RELAXED)) {}

private:
    static inline uint64_t nextId = 1;
    uint64_t idValue;
};

#endif
