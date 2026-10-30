#ifndef __DIRENT__
#define __DIRENT__
#include <sys/types.h>
#define DT_UNKNOWN 0
#define DT_CHR 2
#define DT_DIR 4
#define DT_BLK 6
#define DT_REG 8
struct dirent {
    ino_t d_ino;
    unsigned char d_type;
    char d_name[256];
};
struct DIR {
    int descriptor;
    int used;
    int position;
    dirent entry;
    alignas(8) char buffer[1024];
};
typedef struct DIR DIR;
DIR *
opendir(const char *path);
dirent *
readdir(DIR *directory);
int
closedir(DIR *directory);
void
rewinddir(DIR *directory);

#endif
