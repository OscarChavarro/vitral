#ifndef __VFS__
#define __VFS__
#include "fs/Filesystem.h"
#include "fs/FileStat.h"
#include "fs/MountInfo.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"
#include "fs/OpenFile.h"

class BlockDevice;

class Vfs {
public:
    static constexpr unsigned PATH_MAX = 256;
    static constexpr unsigned NAME_MAX = DirEntry::NAME_MAX;
    static constexpr unsigned MAX_MOUNTS = 8;
    static constexpr unsigned MAX_DEPTH = 32;

    static constexpr const char *RAMDISK = "/media/ramdisk";

    static void init();

    static int64_t open(const char *cwd, const char *path, int flags, OpenFile *&out);
    static void close(OpenFile *file);
    static int64_t read(OpenFile *file, void *buffer, size_t count);
    static int64_t write(OpenFile *file, const void *buffer, size_t count);
    static int64_t pread(OpenFile *file, void *buffer, size_t count, uint64_t offset);
    static int64_t pwrite(OpenFile *file, const void *buffer, size_t count, uint64_t offset);
    static int64_t lseek(OpenFile *file, int64_t offset, int whence);
    static int64_t truncate(OpenFile *file, uint64_t size);
    static int64_t sync(OpenFile *file);
    static int64_t ioctl(OpenFile *file, uint64_t request, void *argument);
    static int poll(OpenFile *file, int events);
    static int64_t stat(const char *cwd, const char *path, FileStat &out);
    static int64_t fstat(OpenFile *file, FileStat &out);
    static int64_t getdents(OpenFile *file, void *buffer, size_t size);
    static int64_t mkdir(const char *cwd, const char *path);
    static int64_t unlink(const char *cwd, const char *path);
    static int64_t rmdir(const char *cwd, const char *path);
    static int64_t chdir(const char *cwd, const char *path, char *canonical);
    static int64_t mount(const char *cwd, const char *source, const char *target, const char *type);
    static int64_t umount(const char *cwd, const char *target);
    static unsigned mounts(MountInfo *out, unsigned max);

private:
    class Mount {
    public:
        bool used;
        char path[PATH_MAX];
        char source[32];
        Filesystem *fs;
        BlockDevice *device;
    };

    class Walk {
    public:
        Inode stack[MAX_DEPTH];
        uint16_t pathEnd[MAX_DEPTH];
        unsigned depth;
        char path[PATH_MAX];
        unsigned length;
        const Inode &top() const { return stack[depth - 1]; }
    };

    static int64_t begin(Walk &walk);
    static int64_t step(Walk &walk, const char *name, unsigned length);
    static int64_t feed(Walk &walk, const char *path);
    static int64_t resolve(const char *cwd, const char *path, Walk &walk);
    static int64_t resolveParent(const char *cwd, const char *path, Walk &walk, char *name);
    static Mount *mountAt(const char *canonical);
    static Mount *mountOf(const Filesystem *fs);
    static void fill(const Inode &inode, FileStat &out);
    static int64_t nextRecord(OpenFile *file, DirEntry &entry);
    static int64_t lseekLocked(OpenFile *file, int64_t offset, int whence);

    static Mount mountTable[MAX_MOUNTS];
};

#endif
