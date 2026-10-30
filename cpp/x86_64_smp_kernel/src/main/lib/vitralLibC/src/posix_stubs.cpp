#include "kernel/Scheduler.h"
#include <unistd.h>
#include <fcntl.h>
#include <poll.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <errno.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#include <dirent.h>
#include "syscalls/Syscalls.h"
#include "drivers/tty/Tty.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "fs/OpenFlag.h"
#include "fs/Whence.h"
#include "fs/PollRecord.h"
#include "fs/FileStat.h"
#include "fs/DirRecord.h"

static int
unsupported() {
    errno = ENOSYS;
    return -1;
}

static int
failWith(int64_t result) {
    errno = (int)-result;
    return -1;
}

static bool
isFile(int descriptor) { return descriptor >= 3; }

ssize_t
read(int descriptor, void *buffer, size_t count) {
    if (descriptor == 0) {
        return (ssize_t)Tty::read((uint8_t *)buffer, count);
    }
    if (isFile(descriptor)) {
        int64_t n = Syscalls::read(descriptor, buffer, count);
        return n < 0 ? failWith(n) : (ssize_t)n;
    }
    errno = EBADF;
    return -1;
}

ssize_t
write(int descriptor, const void *buffer, size_t count) {
    if (descriptor == 1 || descriptor == 2) {
        const char *bytes = (const char *)buffer;
        unsigned console = Syscalls::console();
        for (size_t i = 0; i < count; i++) {
            VirtualConsole::putc(console, bytes[i]);
        }
        return (ssize_t)count;
    }
    if (isFile(descriptor)) {
        int64_t n = Syscalls::write(descriptor, buffer, count);
        return n < 0 ? failWith(n) : (ssize_t)n;
    }
    errno = EBADF;
    return -1;
}

int
close(int descriptor) {
    if (!isFile(descriptor)) {
        return 0;
    }
    int64_t r = Syscalls::close(descriptor);
    return r < 0 ? failWith(r) : 0;
}

off_t
lseek(int descriptor, off_t offset, int whence) {
    if (!isFile(descriptor)) {
        errno = ESPIPE;
        return -1;
    }
    int64_t r = Syscalls::lseek(descriptor, offset, whence);
    return r < 0 ? failWith(r) : (off_t)r;
}

int
unlink(const char *path) {
    int64_t r = Syscalls::unlink(path);
    return r < 0 ? failWith(r) : 0;
}

int
access(const char *path, int) {
    FileStat st;
    int64_t r = Syscalls::stat(path, &st);
    return r < 0 ? failWith(r) : 0;
}
int
isatty(int descriptor) { return descriptor >= 0 && descriptor <= 2; }
int
pipe(int[2]) { return unsupported(); }
int
dup(int) { return unsupported(); }
int
dup2(int, int) { return unsupported(); }

unsigned int
sleep(unsigned int seconds) {
    Syscalls::sleep(seconds * 1000);
    return 0;
}

int
usleep(useconds_t microseconds) {
    Syscalls::sleep((microseconds + 999) / 1000);
    return 0;
}

pid_t
getpid() { return (pid_t)Syscalls::getpid(); }
pid_t
getppid() { return (pid_t)Syscalls::getppid(); }
pid_t
fork() { return unsupported(); }

char *
getcwd(char *buffer, size_t size) {
    int64_t r = Syscalls::getcwd(buffer, size);
    if (r < 0) {
        failWith(r);
        return nullptr;
    }
    return buffer;
}

int
chdir(const char *path) {
    int64_t r = Syscalls::chdir(path);
    return r < 0 ? failWith(r) : 0;
}

long
sysconf(int name) {
    if (name == _SC_PAGESIZE) {
        return 4096;
    }
    if (name == _SC_NPROCESSORS_ONLN || name == _SC_NPROCESSORS_CONF) {
        return (long)Scheduler::onlineCount();
    }
    return 1;
}

int
open(const char *path, int flags, ...) {
    int64_t fd = Syscalls::open(path, flags);
    return fd < 0 ? failWith(fd) : (int)fd;
}

int
fcntl(int, int, ...) { return unsupported(); }

static void
fillStat(const FileStat &in, struct stat *out) {
    out->st_dev = in.device;
    out->st_ino = in.inode;
    out->st_mode = in.mode;
    out->st_nlink = 1;
    out->st_uid = out->st_gid = 0;
    out->st_size = (off_t)in.size;
    out->st_blksize = in.blockSize;
    out->st_blocks = (blkcnt_t)in.blocks;
    out->st_atime = out->st_mtime = out->st_ctime = 0;
}

int
stat(const char *path, struct stat *out) {
    FileStat st;
    int64_t r = Syscalls::stat(path, &st);
    if (r < 0) {
        return failWith(r);
    }
    fillStat(st, out);
    return 0;
}

int
fstat(int descriptor, struct stat *out) {
    FileStat st;
    if (!isFile(descriptor)) {
        errno = EBADF;
        return -1;
    }
    int64_t r = Syscalls::fstat(descriptor, &st);
    if (r < 0) {
        return failWith(r);
    }
    fillStat(st, out);
    return 0;
}

int
mkdir(const char *path, mode_t) {
    int64_t r = Syscalls::mkdir(path);
    return r < 0 ? failWith(r) : 0;
}

DIR *
opendir(const char *path) {
    int64_t fd = Syscalls::open(path, OpenFlag::RDONLY | OpenFlag::DIRECTORY);
    if (fd < 0) {
        errno = (int)-fd;
        return nullptr;
    }
    DIR *dir = (DIR *)malloc(sizeof(DIR));
    if (!dir) {
        Syscalls::close((int)fd);
        errno = ENOMEM;
        return nullptr;
    }
    dir->descriptor = (int)fd;
    dir->used = dir->position = 0;
    return dir;
}

dirent *
readdir(DIR *dir) {
    if (!dir) {
        errno = EBADF;
        return nullptr;
    }
    if (dir->position >= dir->used) {
        int64_t n = Syscalls::getdents(dir->descriptor, dir->buffer, sizeof(dir->buffer));
        if (n <= 0) {
            if (n < 0) {
                failWith(n);
            }
            return nullptr;
        }
        dir->used = (int)n;
        dir->position = 0;
    }
    const DirRecord *record = (const DirRecord *)(dir->buffer + dir->position);
    dir->position += record->length;
    dir->entry.d_ino = record->inode;
    dir->entry.d_type = record->type;
    strncpy(dir->entry.d_name, record->name, sizeof(dir->entry.d_name) - 1);
    dir->entry.d_name[sizeof(dir->entry.d_name) - 1] = 0;
    return &dir->entry;
}

int
closedir(DIR *dir) {
    if (!dir) {
        errno = EBADF;
        return -1;
    }
    Syscalls::close(dir->descriptor);
    free(dir);
    return 0;
}

void
rewinddir(DIR *dir) {
    if (!dir) {
        return;
    }
    Syscalls::lseek(dir->descriptor, 0, Whence::SET);
    dir->used = dir->position = 0;
}
static short
pollConsole(int descriptor, short events) {
    if (descriptor == 0) {
        return Tty::readable() ? (short)(events & POLLIN) : 0;
    }
    return (short)(events & POLLOUT);
}

int
poll(pollfd *descriptors, nfds_t count, int timeout) {
    uint64_t start = Syscalls::uptimeMs();
    for (;;) {
        int ready = 0;
        for (nfds_t i = 0; i < count; i++) {
            pollfd &p = descriptors[i];
            p.revents = 0;
            if (p.fd < 0) {
                continue;
            }
            if (isFile(p.fd)) {
                PollRecord record = {p.fd, p.events, 0};
                int64_t r = Syscalls::poll(&record, 1, 0);
                if (r < 0) {
                    return failWith(r);
                }
                p.revents = record.revents;
            } else {
                p.revents = pollConsole(p.fd, p.events);
            }
            if (p.revents) {
                ready++;
            }
        }
        if (ready || timeout == 0) {
            return ready;
        }
        if (timeout > 0 && Syscalls::uptimeMs() - start >= (uint64_t)timeout) {
            return 0;
        }
        Syscalls::sleep(1);
    }
}

int
select(int count, fd_set *readSet, fd_set *writeSet, fd_set *errorSet, timeval *timeout) {
    if (count < 0 || count > FD_SETSIZE) {
        errno = EINVAL;
        return -1;
    }
    pollfd descriptors[64];
    nfds_t used = 0;
    for (int fd = 0; fd < count; fd++) {
        short events = 0;
        if (readSet && FD_ISSET(fd, readSet)) {
            events |= POLLIN;
        }
        if (writeSet && FD_ISSET(fd, writeSet)) {
            events |= POLLOUT;
        }
        if (errorSet && FD_ISSET(fd, errorSet)) {
            events |= POLLPRI;
        }
        if (!events) {
            continue;
        }
        if (used == sizeof(descriptors) / sizeof(descriptors[0])) {
            errno = EINVAL;
            return -1;
        }
        descriptors[used].fd = fd;
        descriptors[used].events = events;
        used++;
    }
    int milliseconds = timeout ? (int)(timeout->tv_sec * 1000 + (timeout->tv_usec + 999) / 1000) : -1;
    int r = poll(descriptors, used, milliseconds);
    if (r < 0) {
        return r;
    }
    if (readSet) {
        FD_ZERO(readSet);
    }
    if (writeSet) {
        FD_ZERO(writeSet);
    }
    if (errorSet) {
        FD_ZERO(errorSet);
    }
    int bits = 0;
    for (nfds_t i = 0; i < used; i++) {
        const pollfd &p = descriptors[i];
        if (readSet && (p.revents & (POLLIN | POLLHUP | POLLERR))) { FD_SET(p.fd, readSet); bits++; }
        if (writeSet && (p.revents & (POLLOUT | POLLERR))) { FD_SET(p.fd, writeSet); bits++; }
        if (errorSet && (p.revents & POLLPRI)) { FD_SET(p.fd, errorSet); bits++; }
    }
    return bits;
}
int
socket(int, int, int) { return unsupported(); }
int
bind(int, const sockaddr *, socklen_t) { return unsupported(); }
int
listen(int, int) { return unsupported(); }
int
accept(int, sockaddr *, socklen_t *) { return unsupported(); }
int
connect(int, const sockaddr *, socklen_t) { return unsupported(); }
ssize_t
send(int, const void *, size_t, int) { return unsupported(); }
ssize_t
recv(int, void *, size_t, int) { return unsupported(); }
int
shutdown(int, int) { return unsupported(); }
int
setsockopt(int, int, int, const void *, socklen_t) { return unsupported(); }
int
getsockopt(int, int, int, void *, socklen_t *) { return unsupported(); }

uint16_t
htons(uint16_t value) { return (uint16_t)((value << 8) | (value >> 8)); }
uint16_t
ntohs(uint16_t value) { return htons(value); }
uint32_t
htonl(uint32_t value) { return __builtin_bswap32(value); }
uint32_t
ntohl(uint32_t value) { return __builtin_bswap32(value); }

in_addr_t
inet_addr(const char *) { return (in_addr_t)-1; }
int
inet_pton(int, const char *, void *) { return unsupported(); }
const char *
inet_ntop(int, const void *, char *, socklen_t) {
    errno = ENOSYS;
    return nullptr;
}
char *
inet_ntoa(in_addr) { return (char *)""; }

int
getaddrinfo(const char *, const char *, const addrinfo *, addrinfo **) { return -1; }
void
freeaddrinfo(addrinfo *) {}
const char *
gai_strerror(int) { return "not supported"; }
hostent *
gethostbyname(const char *) { return nullptr; }
