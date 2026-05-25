#ifndef __VSDK_TOOLKIT_RENDER_WIREFRAMERENDERER_H__
#define __VSDK_TOOLKIT_RENDER_WIREFRAMERENDERER_H__

#include "vsdk/toolkit/render/RenderingElement.h"

#include <vector>

class Calligraphic2DBuffer;
class SimpleBody;
class Camera;
class Matrix4x4d;
class Vector3Dd;

class WireframeRenderer : public RenderingElement {
public:
    static void execute(Calligraphic2DBuffer* outLineSet,
                        const std::vector<SimpleBody*>& simpleBodies,
                        const Camera* camera);
};

#endif
