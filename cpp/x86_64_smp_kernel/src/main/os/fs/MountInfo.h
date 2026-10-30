#ifndef __MOUNT_INFO__
#define __MOUNT_INFO__
#include <stdint.h>

class MountInfo {
public:
    char source[32];
    char target[64];
    char type[16];
};

#endif
