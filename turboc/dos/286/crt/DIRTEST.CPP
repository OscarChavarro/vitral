/* Hosted ABI test for DIR layout; FAT sector traversal is tested separately. */
#include <dirent.h>
#include <dir.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <errno.h>
#include "../Protected286Crt.h"
extern "C" void crt286InitializeDirectories();
int Protected286Crt::nextRootEntry(Protected286Word* cursor, char far* name)
{
    if ((*cursor)++) return 0;
    strcpy(name, "FIRST.DAT");
    return 1;
}
int main()
{
    char root[] = "C:\\", sub[] = "C:\\SUB", buffer[4];
    DIR* d;
    struct dirent* entry;
    crt286InitializeDirectories();
    assert(getcwd(buffer, 4) == buffer && !strcmp(buffer, root));
    assert(getcwd(buffer, 3) == 0 && errno == ERANGE);
    d = opendir(root);
    assert(d);
    entry = readdir(d);
    assert(entry && !strcmp(entry->d_name, "FIRST.DAT"));
    assert(!readdir(d) && !readdir(d));
    assert(closedir(d) == 0);
    assert(closedir(d) == -1 && errno == EBADF);
    assert(!opendir(sub) && errno == ENOENT);
    assert(mkdir(sub) == -1 && errno == EACCES);
    assert(!getenv("VITRAL_TEST_UNSET"));
    return 0;
}
