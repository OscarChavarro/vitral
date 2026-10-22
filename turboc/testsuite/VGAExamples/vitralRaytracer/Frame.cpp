#include <stddef.h>
#include "programs/vitralRaytracer/Frame.h"
#include "programs/vitralRaytracer/RaytracerRenderer.h"
#include "programs/vitralRaytracer/ScreenProgress.h"
#include <stdio.h>
#include <stdlib.h>
#include <vga.h>

Frame::Frame() : rgb(NULL), width(0), height(0), valid(0) {}

Frame::~Frame() { free(rgb); }

int
Frame::render(const SceneList *scenes, int width, int height, int text, int parallel,
              const Dithering::Palette *palette, double *seconds, char *error, unsigned errorSize) {
    free(rgb);
    rgb = (unsigned char *)malloc((unsigned)width * (unsigned)height * 3);
    this->width = width;
    this->height = height;
    valid = 0;
    if (!rgb) {
        snprintf(error, errorSize, "not enough memory for a %dx%d image", width, height);
        return 0;
    }
    if (palette->count == 256) {
        vga_setpalvec(0, 256, (int *)palette->rgb);
    }
    char path[SceneList::PATH_SIZE];
    scenes->path(path);
    ScreenProgress progress(width, height, (int)palette->count - 1, text);
    valid = RaytracerRenderer::render(path, width, height, parallel, rgb, &progress, seconds, error, errorSize);
    return valid;
}
