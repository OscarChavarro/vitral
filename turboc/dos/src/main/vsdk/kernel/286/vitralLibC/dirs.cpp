/* Borland directory ABI over the protected FAT root, without DOS DTAs. */
#include <dirent.h>
#include <dir.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include "vitralLibC/Protected286Crt.h"
#include "boot/Protected286Loader.h"

struct Directory { DIR publicDir; Protected286Word cursor; int used; };
static Directory directories[4];
extern "C" void crt286InitializeDirectories()
{
    for (unsigned i = 0; i < 4; ++i) directories[i].used = 0;
}
static int root(const char* path)
{
    if (!path) return 0;
    if ((path[0] == 'C' || path[0] == 'c') && path[1] == ':') path += 2;
    while (*path == '/' || *path == '\\') ++path;
    return !*path || !strcmp(path, ".");
}
extern "C" DIR* opendir(char* path)
{
    if (!root(path)) { errno = ENOENT; return 0; }
    Protected286Word cursor = 0;
    char name[13];
    if (Protected286Crt::nextRootEntry(&cursor, name) < 0) { errno = EIO; return 0; }
    for (unsigned i = 0; i < 4; ++i) if (!directories[i].used) {
        directories[i].used = 1;
        directories[i].cursor = 0;
        return &directories[i].publicDir;
    }
    errno = EMFILE;
    return 0;
}
static Directory* directory(DIR* p)
{
    for (unsigned i = 0; i < 4; ++i)
        if (&directories[i].publicDir == p && directories[i].used) return &directories[i];
    return 0;
}
extern "C" struct dirent* readdir(DIR* p)
{
    Directory* d = directory(p);
    if (!d) { errno = EBADF; return 0; }
    int result = Protected286Crt::nextRootEntry(&d->cursor, d->publicDir._d_dirent.d_name);
    if (result < 0) errno = EIO;
    return result > 0 ? &d->publicDir._d_dirent : 0;
}
extern "C" int closedir(DIR* p)
{
    Directory* d = directory(p);
    if (!d) { errno = EBADF; return -1; }
    d->used = 0;
    return 0;
}
extern "C" char* getcwd(char* buffer, int size)
{
    unsigned needed = (unsigned)strlen(protected286CurrentDirectory) + 1;
    if (!buffer && !size) size = (int)needed;
    if (size < 0 || (unsigned)size < needed) { errno = ERANGE; return 0; }
    if (!buffer) { buffer = (char*)malloc((unsigned)size); if (!buffer) { errno = ENOMEM; return 0; } }
    memcpy(buffer, protected286CurrentDirectory, needed);
    return buffer;
}
extern "C" int mkdir(const char*)
{
    /* Root-only contract: explicitly fail directory creation. */
    errno = EACCES;
    return -1;
}
extern "C" char* getenv(const char*)
{
    /* The protected runtime starts with an empty process environment. */
    return 0;
}
