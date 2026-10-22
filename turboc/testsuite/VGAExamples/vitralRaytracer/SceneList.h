#ifndef __SCENE_LIST__
#define __SCENE_LIST__
#include <stddef.h>
#include "programs/vitralRaytracer/RaytracerConstant.h"

class SceneList {
public:
    enum { PATH_SIZE = MAX_PATH + 48 };

    static int endsWithRay(const char *name);
    void list(const char *path);
    void path(char *out) const;
    void step(int delta);

private:
    char dir[MAX_PATH];
    char names[MAX_SCENES][48];
    int count;
    int current;

    static int sameText(const char *a, const char *b);
};

#endif
