#include "fs/RootFs.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "fs/FileType.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"

RootFs::RootFs() {
    for (Node &n : nodes) {
        n.used = false;
    }
    nodes[0].used = true;
    nodes[0].parent = 0;
    nodes[0].name[0] = 0;
}

void
RootFs::describe(unsigned index, Inode &out) {
    out = Inode();
    out.fs = this;
    out.mode = FileType::DIRECTORY;
    out.ino = index + 1;
    out.aux0 = index;
}

bool
RootFs::root(Inode &out) {
    describe(0, out);
    return true;
}

int64_t
RootFs::lookup(const Inode &dir, const char *name, Inode &out) {
    if (dir.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    for (unsigned i = 1; i < MAX_NODES; i++) {
        if (nodes[i].used && nodes[i].parent == dir.aux0 && String::equals(nodes[i].name, name)) {
            describe(i, out);
            return 0;
        }
    }
    return -Err::NOENT;
}

int64_t
RootFs::readDir(const Inode &dir, uint64_t &cursor, DirEntry &out) {
    if (dir.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    while (cursor < MAX_NODES) {
        unsigned i = (unsigned)cursor++;
        if (i == 0 || !nodes[i].used || nodes[i].parent != dir.aux0) {
            continue;
        }
        out.ino = i + 1;
        out.mode = FileType::DIRECTORY;
        String::copy(out.name, nodes[i].name, sizeof(out.name));
        return 1;
    }
    return 0;
}

int64_t
RootFs::create(const Inode &dir, const char *name, uint32_t type, Inode &out) {
    if (type != FileType::DIRECTORY) {
        return -Err::PERM;
    }
    for (unsigned i = 1; i < MAX_NODES; i++) {
        if (!nodes[i].used) {
            nodes[i].used = true;
            nodes[i].parent = (unsigned)dir.aux0;
            String::copy(nodes[i].name, name, sizeof(nodes[i].name));
            describe(i, out);
            return 0;
        }
    }
    return -Err::NOSPC;
}

int64_t
RootFs::remove(const Inode &dir, const char *name, bool directory) {
    Inode node;
    int64_t r = lookup(dir, name, node);
    if (r < 0) {
        return r;
    }
    if (!directory) {
        return -Err::ISDIR;
    }
    for (unsigned i = 1; i < MAX_NODES; i++) {
        if (nodes[i].used && nodes[i].parent == node.aux0) {
            return -Err::NOTEMPTY;
        }
    }
    nodes[node.aux0].used = false;
    return 0;
}
