#ifndef __AUTOSTEREOGRAMGENERATOR__
#define __AUTOSTEREOGRAMGENERATOR__

#include "vsdk/toolkit/render/RenderingElement.h"
class RGBImageUncompressed;
class ZBuffer;

class AutoStereogramGenerator : public RenderingElement {
public:
    static void generate(RGBImageUncompressed* result, RGBImageUncompressed* tilePattern, ZBuffer* depthMap);
};

#endif
