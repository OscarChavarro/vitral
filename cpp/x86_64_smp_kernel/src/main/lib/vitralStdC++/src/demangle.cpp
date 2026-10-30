#include <cxxabi.h>
#include <ctype.h>
#include <stdlib.h>
#include <string.h>

namespace __cxxabiv1 {
static bool
appendName(const char *&p, char *out, size_t &used, size_t room) {
    if (!isdigit((unsigned char)*p)) {
        return false;
    }
    size_t n = 0;
    while (isdigit((unsigned char)*p)) {
        n = n * 10 + (size_t)(*p++ - '0');
    }
    if (strlen(p) < n || used + n + 3 > room) {
        return false;
    }
    if (used) {
        out[used++] = ':';
        out[used++] = ':';
    }
    memcpy(out + used, p, n);
    used += n;
    p += n;
    return true;
}

char *
__cxa_demangle(const char *mangled, char *buffer, size_t *length, int *status) {
    if (!mangled || (buffer && !length)) {
        if (status) {
            *status = -3;
        }
        return nullptr;
    }
    size_t room = strlen(mangled) * 2 + 8;
    char *out = (char *)malloc(room);
    if (!out) {
        if (status) {
            *status = -1;
        }
        return nullptr;
    }
    size_t used = 0;
    const char *p = mangled;
    bool ok;
    if (*p == 'N') {
        p++;
        if (p[0] == 'S' && p[1] == 't') {
            p += 2;
            memcpy(out, "std", 3);
            used = 3;
        }
        ok = appendName(p, out, used, room);
        while (ok && *p != 'E') {
            ok = appendName(p, out, used, room);
        }
        ok = ok && *p++ == 'E' && *p == 0;
    } else {
        if (p[0] == 'S' && p[1] == 't') {
            p += 2;
            memcpy(out, "std", 3);
            used = 3;
        }
        ok = appendName(p, out, used, room) && *p == 0;
    }
    if (!ok) {
        free(out);
        if (status) {
            *status = -2;
        }
        return nullptr;
    }
    out[used] = 0;
    if (buffer && *length > used) {
        memcpy(buffer, out, used + 1);
        free(out);
        out = buffer;
    } else {
        free(buffer);
    }
    if (length) {
        *length = used + 1;
    }
    if (status) {
        *status = 0;
    }
    return out;
}
}
