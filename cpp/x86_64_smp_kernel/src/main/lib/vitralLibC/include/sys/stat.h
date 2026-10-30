#ifndef __STAT__
#define __STAT__
#include <sys/types.h>
#include <time.h>
#define S_IFMT 0170000
#define S_IFDIR 0040000
#define S_IFREG 0100000
#define S_ISDIR(mode) (((mode) & S_IFMT) == S_IFDIR)
#define S_ISREG(mode) (((mode) & S_IFMT) == S_IFREG)
#define S_IRWXU 0700
#define S_IRUSR 0400
#define S_IWUSR 0200
#define S_IXUSR 0100
#define S_IRWXG 070
#define S_IRWXO 07
struct stat {
    dev_t st_dev;
    ino_t st_ino;
    mode_t st_mode;
    nlink_t st_nlink;
    uid_t st_uid;
    gid_t st_gid;
    off_t st_size;
    blksize_t st_blksize;
    blkcnt_t st_blocks;
    time_t st_atime;
    time_t st_mtime;
    time_t st_ctime;
};
int
stat(const char *path, struct stat *out);
int
fstat(int descriptor, struct stat *out);
int
mkdir(const char *path, mode_t mode);

#endif
