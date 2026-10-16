#ifndef __GLFW_LABEL_IMAGE_PROVIDER__
#define __GLFW_LABEL_IMAGE_PROVIDER__

#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LabelImageProvider.h"

/**
Rasterizes OpenGL4 labels for GLFW applications with
`GlfwSystem::calculateLabelImage`: the C++ counterpart of the
`AwtSystem::calculateLabelImage` method reference that Java applications pass
as a `Jogl4LabelImageProvider`.

Only the header is needed: clients that use it must also see the OpenGL4
module headers.
*/
class GlfwLabelImageProvider : public OpenGL4LabelImageProvider {
public:
    virtual RGBAImageUncompressed* createLabelImage(
        const java::String& text, const ColorRgb& color,
        int fontSize) override
    {
        return GlfwSystem::calculateLabelImage(text.c_str(), color, fontSize);
    }
};

#endif
