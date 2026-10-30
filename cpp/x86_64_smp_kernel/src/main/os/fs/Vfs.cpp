#include "fs/Vfs.h"
#include "drivers/block/BlockDevice.h"
#include "fs/DevFs.h"
#include "fs/Fat.h"
#include "fs/RootFs.h"
#include "kernel/Panic.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/SpinLock.h"
#include "lib/vitralKernel/String.h"
#include "lib/vitralKernel/LockGuard.h"
#include "drivers/block/BlockDevices.h"
#include "fs/OpenFlag.h"
#include "fs/Whence.h"
#include "fs/FileType.h"
#include "fs/BlockIoctl.h"
#include "fs/FileStat.h"
#include "fs/DirRecord.h"
#include "fs/MountInfo.h"
#include "fs/OpenFile.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"

static SpinLock vfsLock;

Vfs::Mount Vfs::mountTable[Vfs::MAX_MOUNTS];

static bool
startsWith(const char *s, const char *prefix) {
    while (*prefix) {
        if (*s++ != *prefix++) {
            return false;
        }
    }
    return true;
}

void
Vfs::init() {
    {
        LockGuard guard(vfsLock);
        Mount &r = mountTable[0];
        r.used = true;
        String::copy(r.path, "/", sizeof(r.path));
        String::copy(r.source, "rootfs", sizeof(r.source));
        r.fs = new RootFs();
        r.device = nullptr;
        Mount &d = mountTable[1];
        d.used = true;
        String::copy(d.path, "/dev", sizeof(d.path));
        String::copy(d.source, "devtmpfs", sizeof(d.source));
        d.fs = new DevFs();
        d.device = nullptr;
    }
    if (mkdir("/", "/dev") < 0 || mkdir("/", "/media") < 0 || mkdir("/", RAMDISK) < 0) {
        Panic::halt("cannot create the mount points of the root file system");
    }
    if (BlockDevices::find("loop0") && mount("/", "/dev/loop0", RAMDISK, "vfat") < 0) {
        Panic::halt("cannot mount /dev/loop0 on /media/ramdisk");
    }
}

Vfs::Mount *
Vfs::mountAt(const char *canonical) {
    for (Mount &m : mountTable) {
        if (m.used && String::equals(m.path, canonical)) {
            return &m;
        }
    }
    return nullptr;
}

Vfs::Mount *
Vfs::mountOf(const Filesystem *fs) {
    for (Mount &m : mountTable) {
        if (m.used && m.fs == fs) {
            return &m;
        }
    }
    return nullptr;
}

int64_t
Vfs::begin(Walk &walk) {
    if (!mountTable[0].used) {
        return -Err::NOENT;
    }
    walk.depth = 0;
    if (!mountTable[0].fs->root(walk.stack[0])) {
        return -Err::IO;
    }
    walk.depth = 1;
    walk.path[0] = '/';
    walk.path[1] = 0;
    walk.length = 1;
    walk.pathEnd[0] = 1;
    return 0;
}

int64_t
Vfs::step(Walk &walk, const char *name, unsigned length) {
    if (length == 1 && name[0] == '.') {
        return 0;
    }
    if (length == 2 && name[0] == '.' && name[1] == '.') {
        if (walk.depth > 1) {
            walk.depth--;
            walk.length = walk.pathEnd[walk.depth - 1];
            walk.path[walk.length] = 0;
        }
        return 0;
    }
    if (length > NAME_MAX) {
        return -Err::NAMETOOLONG;
    }
    if (walk.depth >= MAX_DEPTH) {
        return -Err::NAMETOOLONG;
    }
    unsigned separator = walk.length > 1 ? 1 : 0;
    if (walk.length + separator + length >= PATH_MAX) {
        return -Err::NAMETOOLONG;
    }

    char component[NAME_MAX + 1];
    Memory::copy(component, name, length);
    component[length] = 0;
    char candidate[PATH_MAX];
    Memory::copy(candidate, walk.path, walk.length);
    if (separator) {
        candidate[walk.length] = '/';
    }
    Memory::copy(candidate + walk.length + separator, component, length + 1);

    Inode child;
    if (Mount *m = mountAt(candidate)) {
        if (!m->fs->root(child)) {
            return -Err::IO;
        }
    } else {
        const Inode &dir = walk.top();
        if (dir.mode != FileType::DIRECTORY) {
            return -Err::NOTDIR;
        }
        int64_t r = dir.fs->lookup(dir, component, child);
        if (r < 0) {
            return r;
        }
    }
    walk.stack[walk.depth] = child;
    walk.depth++;
    walk.length += separator + length;
    Memory::copy(walk.path, candidate, walk.length + 1);
    walk.pathEnd[walk.depth - 1] = (uint16_t)walk.length;
    return 0;
}

int64_t
Vfs::feed(Walk &walk, const char *path) {
    while (*path) {
        while (*path == '/') {
            path++;
        }
        if (!*path) {
            break;
        }
        const char *end = path;
        while (*end && *end != '/') {
            end++;
        }
        int64_t r = step(walk, path, (unsigned)(end - path));
        if (r < 0) {
            return r;
        }
        path = end;
    }
    return 0;
}

int64_t
Vfs::resolve(const char *cwd, const char *path, Walk &walk) {
    if (!path || !path[0]) {
        return -Err::NOENT;
    }
    int64_t r = begin(walk);
    if (r < 0) {
        return r;
    }
    if (path[0] != '/') {
        r = feed(walk, cwd && cwd[0] ? cwd : "/");
        if (r < 0) {
            return r;
        }
    }
    return feed(walk, path);
}

int64_t
Vfs::resolveParent(const char *cwd, const char *path, Walk &walk, char *name) {
    if (!path || !path[0]) {
        return -Err::NOENT;
    }
    char copy[PATH_MAX];
    size_t length = String::length(path);
    if (length >= PATH_MAX) {
        return -Err::NAMETOOLONG;
    }
    Memory::copy(copy, path, length + 1);
    while (length > 1 && copy[length - 1] == '/') {
        copy[--length] = 0;
    }
    size_t slash = length;
    while (slash > 0 && copy[slash - 1] != '/') {
        slash--;
    }
    const char *last = copy + slash;
    size_t lastLength = length - slash;
    if (lastLength == 0 || String::equals(last, ".") || String::equals(last, "..")) {
        return -Err::INVAL;
    }
    if (lastLength > NAME_MAX) {
        return -Err::NAMETOOLONG;
    }
    Memory::copy(name, last, lastLength + 1);
    if (slash == 0) {
        copy[0] = 0;
    } else if (slash == 1) {
        copy[1] = 0;
    } else {
        copy[slash - 1] = 0;
    }
    int64_t r = slash == 0 ? resolve(cwd, ".", walk) : resolve(cwd, copy, walk);
    if (r < 0) {
        return r;
    }
    return walk.top().mode == FileType::DIRECTORY ? 0 : -Err::NOTDIR;
}

int64_t
Vfs::open(const char *cwd, const char *path, int flags, OpenFile *&out) {
    LockGuard guard(vfsLock);
    Walk *walk = new Walk;
    if (!walk) {
        return -Err::NOMEM;
    }
    Inode node;
    char canonical[PATH_MAX];
    int64_t r = resolve(cwd, path, *walk);
    if (r == 0) {
        if ((flags & OpenFlag::CREAT) && (flags & OpenFlag::EXCL)) {
            r = -Err::EXIST;
        }
        node = walk->top();
        Memory::copy(canonical, walk->path, walk->length + 1);
    } else if (r == -Err::NOENT && (flags & OpenFlag::CREAT)) {
        char name[NAME_MAX + 1];
        r = resolveParent(cwd, path, *walk, name);
        if (r == 0) {
            const Inode &parent = walk->top();
            r = parent.fs->create(parent, name, FileType::REGULAR, node);
            if (r == 0) {
                unsigned separator = walk->length > 1 ? 1 : 0;
                Memory::copy(canonical, walk->path, walk->length);
                if (separator) {
                    canonical[walk->length] = '/';
                }
                String::copy(canonical + walk->length + separator, name, PATH_MAX - walk->length - separator);
            }
        }
    }
    delete walk;
    if (r < 0) {
        return r;
    }

    bool directory = node.mode == FileType::DIRECTORY;
    int access = flags & OpenFlag::ACCESS_MASK;
    if (access == 3) {
        return -Err::INVAL;
    }
    if (directory && access != OpenFlag::RDONLY) {
        return -Err::ISDIR;
    }
    if ((flags & OpenFlag::DIRECTORY) && !directory) {
        return -Err::NOTDIR;
    }
    if ((flags & OpenFlag::TRUNC) && node.mode == FileType::REGULAR && access != OpenFlag::RDONLY) {
        r = node.fs->truncate(node, 0);
        if (r < 0) {
            return r;
        }
    }

    OpenFile *file = new OpenFile;
    if (!file) {
        return -Err::NOMEM;
    }
    file->inode = node;
    file->flags = flags;
    String::copy(file->path, canonical, sizeof(file->path));
    node.fs->users++;
    out = file;
    return 0;
}

void
Vfs::close(OpenFile *file) {
    if (!file) {
        return;
    }
    LockGuard guard(vfsLock);
    if (file->inode.fs && file->inode.fs->users) {
        file->inode.fs->users--;
    }
    delete file;
}

int64_t
Vfs::read(OpenFile *file, void *buffer, size_t count) {
    LockGuard guard(vfsLock);
    if ((file->flags & OpenFlag::ACCESS_MASK) == OpenFlag::WRONLY) {
        return -Err::BADF;
    }
    if (file->inode.mode == FileType::DIRECTORY) {
        return -Err::ISDIR;
    }
    int64_t n = file->inode.fs->read(file->inode, file->position, buffer, count);
    if (n > 0) {
        file->position += (uint64_t)n;
    }
    return n;
}

int64_t
Vfs::write(OpenFile *file, const void *buffer, size_t count) {
    LockGuard guard(vfsLock);
    if ((file->flags & OpenFlag::ACCESS_MASK) == OpenFlag::RDONLY) {
        return -Err::BADF;
    }
    if (file->inode.mode == FileType::DIRECTORY) {
        return -Err::ISDIR;
    }
    if (file->flags & OpenFlag::APPEND) {
        file->inode.fs->refresh(file->inode);
        file->position = file->inode.size;
    }
    int64_t n = file->inode.fs->write(file->inode, file->position, buffer, count);
    if (n > 0) {
        file->position += (uint64_t)n;
    }
    return n;
}

int64_t
Vfs::pread(OpenFile *file, void *buffer, size_t count, uint64_t offset) {
    LockGuard guard(vfsLock);
    if ((file->flags & OpenFlag::ACCESS_MASK) == OpenFlag::WRONLY) {
        return -Err::BADF;
    }
    if (file->inode.mode == FileType::DIRECTORY) {
        return -Err::ISDIR;
    }
    return file->inode.fs->read(file->inode, offset, buffer, count);
}

int64_t
Vfs::pwrite(OpenFile *file, const void *buffer, size_t count, uint64_t offset) {
    LockGuard guard(vfsLock);
    if ((file->flags & OpenFlag::ACCESS_MASK) == OpenFlag::RDONLY) {
        return -Err::BADF;
    }
    if (file->inode.mode == FileType::DIRECTORY) {
        return -Err::ISDIR;
    }
    return file->inode.fs->write(file->inode, offset, buffer, count);
}

int64_t
Vfs::lseekLocked(OpenFile *file, int64_t offset, int whence) {
    int64_t base;
    if (whence == Whence::SET) {
        base = 0;
    } else if (whence == Whence::CUR) {
        base = (int64_t)file->position;
    } else if (whence == Whence::END) {
        file->inode.fs->refresh(file->inode);
        base = (int64_t)file->inode.size;
    } else {
        return -Err::INVAL;
    }
    int64_t target = base + offset;
    if (target < 0) {
        return -Err::INVAL;
    }
    if (file->inode.mode == FileType::DIRECTORY) {
        if (target != 0 || whence != Whence::SET) {
            return -Err::INVAL;
        }
        file->phase = 0;
        file->cursor = 0;
        file->mountCursor = 0;
    }
    file->position = (uint64_t)target;
    return target;
}

int64_t
Vfs::lseek(OpenFile *file, int64_t offset, int whence) {
    LockGuard guard(vfsLock);
    return lseekLocked(file, offset, whence);
}

int64_t
Vfs::truncate(OpenFile *file, uint64_t size) {
    LockGuard guard(vfsLock);
    if ((file->flags & OpenFlag::ACCESS_MASK) == OpenFlag::RDONLY) {
        return -Err::BADF;
    }
    if (file->inode.mode != FileType::REGULAR) {
        return -Err::INVAL;
    }
    return file->inode.fs->truncate(file->inode, size);
}

int64_t
Vfs::sync(OpenFile *file) {
    LockGuard guard(vfsLock);
    return file->inode.fs->sync();
}

int
Vfs::poll(OpenFile *file, int events) {
    LockGuard guard(vfsLock);
    return file->inode.fs->poll(file->inode, events);
}

int64_t
Vfs::ioctl(OpenFile *file, uint64_t request, void *argument) {
    LockGuard guard(vfsLock);
    BlockDevice *device = file->inode.device;
    if (file->inode.mode != FileType::BLOCK_DEVICE || !device) {
        return -Err::NOTTY;
    }
    switch (request) {
    case BlockIoctl::GET_SIZE:
        if (!argument) {
            return -Err::FAULT;
        }
        *(uint64_t *)argument = device->sizeBytes() / 512;
        return 0;
    case BlockIoctl::GET_SIZE_BYTES:
        if (!argument) {
            return -Err::FAULT;
        }
        *(uint64_t *)argument = device->sizeBytes();
        return 0;
    case BlockIoctl::GET_SECTOR_SIZE:
        if (!argument) {
            return -Err::FAULT;
        }
        *(int32_t *)argument = (int32_t)device->sectorSize();
        return 0;
    case BlockIoctl::GET_READ_ONLY:
        if (!argument) {
            return -Err::FAULT;
        }
        *(int32_t *)argument = device->readOnly() ? 1 : 0;
        return 0;
    case BlockIoctl::SET_READ_ONLY:
        return -Err::PERM;
    case BlockIoctl::FLUSH:
        return device->flush();
    default:
        return -Err::NOTTY;
    }
}

void
Vfs::fill(const Inode &inode, FileStat &out) {
    out.device = inode.fs->id();
    out.inode = inode.ino;
    out.mode = inode.mode;
    out.blockSize = inode.device ? inode.device->sectorSize() : inode.fs->blockSize();
    out.size = inode.size;
    out.blocks = (inode.size + 511) / 512;
    out.rdev = inode.device ? inode.device->sizeBytes() : 0;
}

int64_t
Vfs::stat(const char *cwd, const char *path, FileStat &out) {
    LockGuard guard(vfsLock);
    Walk *walk = new Walk;
    if (!walk) {
        return -Err::NOMEM;
    }
    int64_t r = resolve(cwd, path, *walk);
    if (r == 0) {
        Inode node = walk->top();
        node.fs->refresh(node);
        fill(node, out);
    }
    delete walk;
    return r;
}

int64_t
Vfs::fstat(OpenFile *file, FileStat &out) {
    LockGuard guard(vfsLock);
    file->inode.fs->refresh(file->inode);
    fill(file->inode, out);
    return 0;
}

int64_t
Vfs::nextRecord(OpenFile *file, DirEntry &entry) {
    if (file->phase == 0 || file->phase == 1) {
        entry.ino = file->inode.ino;
        entry.mode = FileType::DIRECTORY;
        String::copy(entry.name, file->phase == 0 ? "." : "..", sizeof(entry.name));
        file->phase++;
        return 1;
    }
    if (file->phase == 2) {
        int64_t r = file->inode.fs->readDir(file->inode, file->cursor, entry);
        if (r != 0) {
            return r;
        }
        file->phase = 3;
    }
    if (file->phase == 3) {
        size_t base = String::length(file->path);
        bool isRoot = base == 1;
        while (file->mountCursor < MAX_MOUNTS) {
            Mount &m = mountTable[file->mountCursor++];
            if (!m.used || String::equals(m.path, "/")) {
                continue;
            }
            if (!startsWith(m.path, file->path) || String::length(m.path) <= base + (isRoot ? 0 : 1)) {
                continue;
            }
            const char *rest = m.path + base + (isRoot ? 0 : 1);
            if (!isRoot && m.path[base] != '/') {
                continue;
            }
            bool direct = true;
            for (const char *c = rest; *c; c++) {
                if (*c == '/') {
                    direct = false;
                }
            }
            if (!direct) {
                continue;
            }
            Inode below;
            if (file->inode.fs->lookup(file->inode, rest, below) == 0) {
                continue;
            }
            Inode rootInode;
            m.fs->root(rootInode);
            entry.ino = rootInode.ino;
            entry.mode = FileType::DIRECTORY;
            String::copy(entry.name, rest, sizeof(entry.name));
            return 1;
        }
        file->phase = 4;
    }
    return 0;
}

int64_t
Vfs::getdents(OpenFile *file, void *buffer, size_t size) {
    LockGuard guard(vfsLock);
    if (file->inode.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    uint8_t *out = (uint8_t *)buffer;
    size_t used = 0;
    for (;;) {
        OpenFile saved = *file;
        DirEntry entry;
        int64_t r = nextRecord(file, entry);
        if (r < 0) {
            return used ? (int64_t)used : r;
        }
        if (r == 0) {
            break;
        }
        size_t length = (DirRecord::HEADER + String::length(entry.name) + 1 + 7) & ~(size_t)7;
        if (used + length > size) {
            *file = saved;
            if (used == 0) {
                return -Err::INVAL;
            }
            break;
        }
        DirRecord *record = (DirRecord *)(out + used);
        record->inode = entry.ino;
        record->offset = (int64_t)++file->position;
        record->length = (uint16_t)length;
        record->type = entry.mode == FileType::DIRECTORY ? DirRecord::TYPE_DIRECTORY
                     : entry.mode == FileType::BLOCK_DEVICE ? DirRecord::TYPE_BLOCK
                     : entry.mode == FileType::CHAR_DEVICE ? DirRecord::TYPE_CHAR : DirRecord::TYPE_REGULAR;
        String::copy(record->name, entry.name, length - DirRecord::HEADER);
        used += length;
    }
    return (int64_t)used;
}

int64_t
Vfs::mkdir(const char *cwd, const char *path) {
    LockGuard guard(vfsLock);
    Walk *walk = new Walk;
    if (!walk) {
        return -Err::NOMEM;
    }
    char name[NAME_MAX + 1];
    int64_t r = resolveParent(cwd, path, *walk, name);
    if (r == 0) {
        const Inode &parent = walk->top();
        Inode existing;
        Inode created;
        if (parent.fs->lookup(parent, name, existing) == 0) {
            r = -Err::EXIST;
        } else {
            r = parent.fs->create(parent, name, FileType::DIRECTORY, created);
        }
    } else if (r == -Err::INVAL) {
        r = -Err::EXIST;
    }
    delete walk;
    return r;
}

int64_t
Vfs::unlink(const char *cwd, const char *path) {
    LockGuard guard(vfsLock);
    Walk *walk = new Walk;
    if (!walk) {
        return -Err::NOMEM;
    }
    char name[NAME_MAX + 1];
    int64_t r = resolveParent(cwd, path, *walk, name);
    if (r == 0) {
        char full[PATH_MAX];
        unsigned separator = walk->length > 1 ? 1 : 0;
        Memory::copy(full, walk->path, walk->length);
        if (separator) {
            full[walk->length] = '/';
        }
        String::copy(full + walk->length + separator, name, PATH_MAX - walk->length - separator);
        const Inode &parent = walk->top();
        r = mountAt(full) ? -Err::BUSY : parent.fs->remove(parent, name, false);
    }
    delete walk;
    return r;
}

int64_t
Vfs::rmdir(const char *cwd, const char *path) {
    LockGuard guard(vfsLock);
    Walk *walk = new Walk;
    if (!walk) {
        return -Err::NOMEM;
    }
    char name[NAME_MAX + 1];
    int64_t r = resolveParent(cwd, path, *walk, name);
    if (r == 0) {
        char full[PATH_MAX];
        unsigned separator = walk->length > 1 ? 1 : 0;
        Memory::copy(full, walk->path, walk->length);
        if (separator) {
            full[walk->length] = '/';
        }
        String::copy(full + walk->length + separator, name, PATH_MAX - walk->length - separator);
        const Inode &parent = walk->top();
        r = mountAt(full) ? -Err::BUSY : parent.fs->remove(parent, name, true);
    }
    delete walk;
    return r;
}

int64_t
Vfs::chdir(const char *cwd, const char *path, char *canonical) {
    LockGuard guard(vfsLock);
    Walk *walk = new Walk;
    if (!walk) {
        return -Err::NOMEM;
    }
    int64_t r = resolve(cwd, path, *walk);
    if (r == 0 && walk->top().mode != FileType::DIRECTORY) {
        r = -Err::NOTDIR;
    }
    if (r == 0) {
        Memory::copy(canonical, walk->path, walk->length + 1);
    }
    delete walk;
    return r;
}

int64_t
Vfs::mount(const char *cwd, const char *source, const char *target, const char *type) {
    LockGuard guard(vfsLock);
    if (!source || !target) {
        return -Err::FAULT;
    }
    if (type && !String::equals(type, "vfat") && !String::equals(type, "fat") && !String::equals(type, "msdos")) {
        return -Err::NODEV;
    }
    Walk *walk = new Walk;
    if (!walk) {
        return -Err::NOMEM;
    }
    int64_t r = resolve(cwd, source, *walk);
    Inode node;
    if (r == 0) {
        node = walk->top();
        if (node.mode != FileType::BLOCK_DEVICE || !node.device) {
            r = -Err::NOTBLK;
        }
    }
    if (r == 0) {
        r = resolve(cwd, target, *walk);
    }
    if (r == 0 && walk->top().mode != FileType::DIRECTORY) {
        r = -Err::NOTDIR;
    }
    if (r == 0 && mountAt(walk->path)) {
        r = -Err::BUSY;
    }
    for (Mount &m : mountTable) {
        if (r == 0 && m.used && m.device == node.device) {
            r = -Err::BUSY;
        }
    }
    Mount *slot = nullptr;
    for (Mount &m : mountTable) {
        if (!m.used && !slot) {
            slot = &m;
        }
    }
    if (r == 0 && !slot) {
        r = -Err::MFILE;
    }
    if (r == 0) {
        Filesystem *fs = Fat::mount(node.device);
        if (!fs) {
            r = -Err::INVAL;
        } else {
            slot->used = true;
            String::copy(slot->path, walk->path, sizeof(slot->path));
            Format::print(slot->source, sizeof(slot->source), "/dev/%s", node.device->name());
            slot->fs = fs;
            slot->device = node.device;
        }
    }
    delete walk;
    return r;
}

int64_t
Vfs::umount(const char *cwd, const char *target) {
    LockGuard guard(vfsLock);
    Walk *walk = new Walk;
    if (!walk) {
        return -Err::NOMEM;
    }
    int64_t r = resolve(cwd, target, *walk);
    if (r == 0) {
        Mount *m = mountAt(walk->path);
        if (!m) {
            r = -Err::INVAL;
        } else if (String::equals(m->path, "/") || String::equals(m->path, "/dev") || m->fs->users) {
            r = -Err::BUSY;
        } else {
            m->fs->sync();
            delete m->fs;
            m->used = false;
        }
    }
    delete walk;
    return r;
}

unsigned
Vfs::mounts(MountInfo *out, unsigned max) {
    LockGuard guard(vfsLock);
    unsigned n = 0;
    for (Mount &m : mountTable) {
        if (!m.used || n >= max) {
            continue;
        }
        String::copy(out[n].source, m.source, sizeof(out[n].source));
        String::copy(out[n].target, m.path, sizeof(out[n].target));
        String::copy(out[n].type, m.fs->type(), sizeof(out[n].type));
        n++;
    }
    return n;
}
