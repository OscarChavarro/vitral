#ifndef __DEV_FS__
#define __DEV_FS__
#include "fs/Filesystem.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"

class DevFs : public Filesystem {
public:
    DevFs() {}

    const char *type() const override { return "devtmpfs"; }
    bool root(Inode &out) override;
    int64_t lookup(const Inode &dir, const char *name, Inode &out) override;
    int64_t readDir(const Inode &dir, uint64_t &cursor, DirEntry &out) override;
    int64_t read(Inode &file, uint64_t offset, void *buffer, size_t count) override;
    int64_t write(Inode &file, uint64_t offset, const void *buffer, size_t count) override;
    int64_t refresh(Inode &node) override;
    int poll(Inode &file, int events) override;
    uint32_t blockSize() const override { return 512; }

private:
    static constexpr uint64_t ROOT_INO = 1;
    static constexpr uint64_t CHAR_INO = 0x100;
    static constexpr uint64_t DIR_INO = 0x200;
    void describe(BlockDevice *device, unsigned index, Inode &out);
    void describe(CharDevice *device, unsigned index, Inode &out);
    void describeDirectory(unsigned index, Inode &out);
    static int firstIn(const char *directory);
    static bool sameDirectory(const char *a, const char *b);
};

#endif
