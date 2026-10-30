#ifndef __SELECT__
#define __SELECT__
#include <sys/time.h>
#define FD_SETSIZE 1024
struct fd_set {
    unsigned long bits[FD_SETSIZE / (8 * sizeof(unsigned long))];
};
#define FD_ZERO(set)                                                                          \
    do {                                                                                      \
        for (unsigned long i__ = 0; i__ < sizeof((set)->bits) / sizeof(unsigned long); i__++) \
            (set)->bits[i__] = 0;                                                             \
    } while (0)
#define FD_SET(descriptor, set) ((set)->bits[(descriptor) / (8 * sizeof(unsigned long))] |= 1ul << ((descriptor) % (8 * sizeof(unsigned long))))
#define FD_CLR(descriptor, set) ((set)->bits[(descriptor) / (8 * sizeof(unsigned long))] &= ~(1ul << ((descriptor) % (8 * sizeof(unsigned long)))))
#define FD_ISSET(descriptor, set) (((set)->bits[(descriptor) / (8 * sizeof(unsigned long))] >> ((descriptor) % (8 * sizeof(unsigned long)))) & 1)
int
select(int count, fd_set *readSet, fd_set *writeSet, fd_set *errorSet, timeval *timeout);

#endif
