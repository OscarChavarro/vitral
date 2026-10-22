#ifndef __FRAME__
#define __FRAME__
#include "lib/dithering/Dithering.h"
#include "programs/vitralRaytracer/SceneList.h"

class Frame {
public:
    Frame();
    ~Frame();

    int render(const SceneList *scenes, int width, int height, int text, int parallel,
               const Dithering::Palette *palette, double *seconds, char *error, unsigned errorSize);
    const unsigned char *pixels() const { return rgb; }
    int frameWidth() const { return width; }
    int frameHeight() const { return height; }
    int isValid() const { return valid; }

private:
    Frame(const Frame &);
    Frame &operator=(const Frame &);

    unsigned char *rgb;
    int width;
    int height;
    int valid;
};

#endif
