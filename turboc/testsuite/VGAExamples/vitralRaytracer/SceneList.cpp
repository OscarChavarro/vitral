#include "vsdk/toolkit/common/VSDK.h"
#include "programs/vitralRaytracer/SceneList.h"
#include <ctype.h>
#include <dirent.h>
#include <stdio.h>
#include <string.h>

int
SceneList::sameText(const char *a, const char *b) {
    for (; *a && *b; a++, b++) {
        if (tolower((unsigned char)*a) != tolower((unsigned char)*b)) {
            return 0;
        }
    }
    return *a == *b;
}

int
SceneList::endsWithRay(const char *name) {
    size_t n = strlen(name);
    return n > 4 && sameText(name + n - 4, ".ray");
}

void
SceneList::list(const char *path) {
    const char *slash = strrchr(path, '/');
    size_t dirLength = slash ? (size_t)(slash - path) : 0;
    if (dirLength >= MAX_PATH) {
        dirLength = MAX_PATH - 1;
    }
    memcpy(dir, path, dirLength);
    dir[dirLength] = 0;
    const char *file = slash ? slash + 1 : path;
    count = 0;
    current = 0;
    DIR *d = opendir(dirLength ? dir : "/");
    if (d) {
        for (dirent *e = readdir(d); e && count < MAX_SCENES; e = readdir(d)) {
            if (!endsWithRay(e->d_name) || strlen(e->d_name) >= sizeof(names[0])) {
                continue;
            }
            int at = count++;
            while (at > 0 && strcmp(names[at - 1], e->d_name) > 0) {
                strcpy(names[at], names[at - 1]);
                at--;
            }
            strcpy(names[at], e->d_name);
        }
        closedir(d);
    }
    if (strlen(file) >= sizeof(names[0])) {
        return;
    }
    for (int i = 0; i < count; i++) {
        if (strcmp(names[i], file) == 0) {
            current = i;
        }
    }
    if (count == 0) {
        strcpy(names[0], file);
        count = 1;
    }
}

void
SceneList::path(char *out) const {
    snprintf(out, PATH_SIZE, "%s/%s", dir, names[current]);
}

void
SceneList::step(int delta) {
    current = (current + (delta > 0 ? 1 : count - 1)) % count;
}
