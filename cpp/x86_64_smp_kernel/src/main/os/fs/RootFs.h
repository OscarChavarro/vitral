#ifndef __ROOT_FS__
#define __ROOT_FS__
#include "fs/Filesystem.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"

class RootFs : public Filesystem {
public:
    RootFs();

    const char *type() const override { return "rootfs"; }
    bool root(Inode &out) override;
    int64_t lookup(const Inode &dir, const char *name, Inode &out) override;
    int64_t readDir(const Inode &dir, uint64_t &cursor, DirEntry &out) override;
    int64_t create(const Inode &dir, const char *name, uint32_t type, Inode &out) override;
    int64_t remove(const Inode &dir, const char *name, bool directory) override;

private:
    static constexpr unsigned MAX_NODES = 16;

    class Node {
    public:
        bool used;
        unsigned parent;
        char name[DirEntry::NAME_MAX + 1];
    };

    void describe(unsigned index, Inode &out);
    Node nodes[MAX_NODES];
};

#endif
