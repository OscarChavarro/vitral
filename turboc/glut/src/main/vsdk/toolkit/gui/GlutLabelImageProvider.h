#ifndef __GLUT_LABEL_IMAGE_PROVIDER__
#define __GLUT_LABEL_IMAGE_PROVIDER__

#include "vsdk/toolkit/gui/GlutSystem.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LabelImageProvider.h"

/**
Rasterizes OpenGL1 labels for GLUT applications with
`GlutSystem::calculateLabelImage`: the counterpart of the
`AwtSystem::calculateLabelImage` method reference that Java applications pass
as a label image provider.

Only the header is needed: clients that use it must also see the OpenGL1
module headers.
*/
class GlutLabelImageProvider : public OpenGL1LabelImageProvider {
public:
    virtual RGBAImageUncompressed* createLabelImage(
        const String& text, const ColorRgb& color, int fontSize)
    {
        return GlutSystem::calculateLabelImage(text.c_str(), color, fontSize);
    }
};

#endif
