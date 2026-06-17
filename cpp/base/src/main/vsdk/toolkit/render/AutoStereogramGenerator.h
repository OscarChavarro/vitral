#ifndef __VSDK_TOOLKIT_RENDER_AUTOSTEREOGRAMGENERATOR_H__
#define __VSDK_TOOLKIT_RENDER_AUTOSTEREOGRAMGENERATOR_H__

#include "vsdk/toolkit/render/RenderingElement.h"
class RGBImageUncompressed;
class ZBuffer;

class AutoStereogramGenerator : public RenderingElement {
public:
    static void generate(RGBImageUncompressed* result, RGBImageUncompressed* tilePattern, ZBuffer* depthMap);
};

#endif
