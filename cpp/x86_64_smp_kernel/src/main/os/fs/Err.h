#ifndef __ERR__
#define __ERR__
#include <stdint.h>

class Err {
public:
    static constexpr int64_t PERM = 1;
    static constexpr int64_t NOENT = 2;
    static constexpr int64_t SRCH = 3;
    static constexpr int64_t IO = 5;
    static constexpr int64_t CHILD = 10;
    static constexpr int64_t BADF = 9;
    static constexpr int64_t AGAIN = 11;
    static constexpr int64_t NOMEM = 12;
    static constexpr int64_t FAULT = 14;
    static constexpr int64_t NOTBLK = 15;
    static constexpr int64_t BUSY = 16;
    static constexpr int64_t EXIST = 17;
    static constexpr int64_t NODEV = 19;
    static constexpr int64_t NOTDIR = 20;
    static constexpr int64_t ISDIR = 21;
    static constexpr int64_t INVAL = 22;
    static constexpr int64_t MFILE = 24;
    static constexpr int64_t NOTTY = 25;
    static constexpr int64_t FBIG = 27;
    static constexpr int64_t NOSPC = 28;
    static constexpr int64_t SPIPE = 29;
    static constexpr int64_t ROFS = 30;
    static constexpr int64_t NAMETOOLONG = 36;
    static constexpr int64_t NOSYS = 38;
    static constexpr int64_t NOTEMPTY = 39;
    static constexpr int64_t NETUNREACH = 101;
    static constexpr int64_t HOSTUNREACH = 113;
};

#endif
