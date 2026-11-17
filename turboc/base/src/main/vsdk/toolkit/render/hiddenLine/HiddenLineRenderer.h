#ifndef __HIDDEN_LINE_RENDERER__
#define __HIDDEN_LINE_RENDERER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/render/RenderingElement.h"
class Camera;
class Calligraphic2DBuffer;
class Calligraphic3DBuffer;
class SimpleBody;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;

class HiddenLineQuerySolid {
public:
    SimpleBody* body;
    PolyhedralBoundedSolid* solid;
    BOOLEAN ownsSolid;

    HiddenLineQuerySolid()
        : body(0), solid(0), ownsSolid(FALSE)
    {
    }
};

class HiddenLineRenderer : public RenderingElement {
private:
    static void runAppelAlgorithm(
        ArrayList<SimpleBody*>& inSimpleBodyArray,
        const Camera* inCamera,
        Calligraphic2DBuffer* outVisibleContourLineSet,
        Calligraphic2DBuffer* outVisibleNonContourLineSet,
        Calligraphic2DBuffer* outHiddenLineSet,
        ArrayList<Calligraphic3DBuffer*>* inFreeLineSets,
        ArrayList<Calligraphic2DBuffer*>* outVisibleFreeLineSets,
        ArrayList<Calligraphic2DBuffer*>* outHiddenFreeLineSets);

public:
    static int isFaceVisibleFromCamera(
        _PolyhedralBoundedSolidFace* face,
        const Camera* camera);

    static void executeAppelAlgorithm(
        ArrayList<SimpleBody*>& inSimpleBodyArray,
        const Camera* inCamera,
        Calligraphic2DBuffer* outVisibleContourLineSet,
        Calligraphic2DBuffer* outVisibleNonContourLineSet,
        Calligraphic2DBuffer* outHiddenLineSet);

    /**
    As `executeAppelAlgorithm`, also presenting sets of free lines: lines
    that are not edges of the solids (i.e. curves, or a reference grid), given
    in world coordinates, which are hidden by the solids but do not hide
    anything. The visible and hidden parts of the lines of
    `inFreeLineSets[i]` are reported in `outVisibleFreeLineSets[i]` and
    `outHiddenFreeLineSets[i]`, so each set can be drawn with its own
    style.
    */
    static void executeAppelAlgorithm(
        ArrayList<SimpleBody*>& inSimpleBodyArray,
        const Camera* inCamera,
        Calligraphic2DBuffer* outVisibleContourLineSet,
        Calligraphic2DBuffer* outVisibleNonContourLineSet,
        Calligraphic2DBuffer* outHiddenLineSet,
        ArrayList<Calligraphic3DBuffer*>& inFreeLineSets,
        ArrayList<Calligraphic2DBuffer*>& outVisibleFreeLineSets,
        ArrayList<Calligraphic2DBuffer*>& outHiddenFreeLineSets);
};

#endif
