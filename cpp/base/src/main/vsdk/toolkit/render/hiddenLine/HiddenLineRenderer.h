#ifndef __VSDK_TOOLKIT_RENDER_HIDDENLINERENDERER_H__
#define __VSDK_TOOLKIT_RENDER_HIDDENLINERENDERER_H__

#include "vsdk/toolkit/render/RenderingElement.h"

#include "java/util/ArrayList.h"

class Camera;
class Calligraphic2DBuffer;
class SimpleBody;
class _PolyhedralBoundedSolidFace;

class HiddenLineRenderer : public RenderingElement {
public:
    static int isFaceVisibleFromCamera(
        _PolyhedralBoundedSolidFace* face,
        const Camera* camera);

    static void executeAppelAlgorithm(
        java::ArrayList<SimpleBody*>& inSimpleBodyArray,
        const Camera* inCamera,
        Calligraphic2DBuffer* outVisibleContourLineSet,
        Calligraphic2DBuffer* outVisibleNonContourLineSet,
        Calligraphic2DBuffer* outHiddenLineSet);
};

#endif
