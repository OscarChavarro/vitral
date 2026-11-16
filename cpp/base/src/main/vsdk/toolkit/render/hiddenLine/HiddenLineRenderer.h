#ifndef __HIDDEN_LINE_RENDERER__
#define __HIDDEN_LINE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/render/RenderingElement.h"
class Camera;
class Calligraphic2DBuffer;
class SimpleBody;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;

class HiddenLineQuerySolid {
public:
    SimpleBody* body;
    PolyhedralBoundedSolid* solid;
    bool ownsSolid;

    HiddenLineQuerySolid()
        : body(0), solid(0), ownsSolid(false)
    {
    }
};

/**
This class implements the Appel's algorithm for hidden line rendering. :)
*/
class HiddenLineRenderer : public RenderingElement {
public:
    static const int HIDDEN_LINE = 0;
    static const int VISIBLE_LINE = 1;
    static const int CONTOUR_LINE = 2;

    /// Diagnostic: when >= 0, processLineToBeDrawn logs the contour-crossing /
    /// segment-splitting decisions for the cached edge with this edgeIndex.
    static int debugEdgeIndex;

    class AppelSegmentDump : public RenderingElement {
    public:
        double tStart;
        double tEnd;
        Vector3Dd start;
        Vector3Dd end;
        Vector3Dd midpoint;
        int midpointQuantitativeInvisibility;
        java::ArrayList<int> midpointContributorFaceIds;
        java::String classification;

        AppelSegmentDump()
            : tStart(0), tEnd(0), midpointQuantitativeInvisibility(0)
        {
        }
    };

    class AppelEventDump : public RenderingElement {
    public:
        double lineParameter;
        int deltaQI;
        int contourEdgeIndex;
        int visibleFaceId;

        AppelEventDump()
            : lineParameter(0), deltaQI(0), contourEdgeIndex(0),
              visibleFaceId(0)
        {
        }
    };

    class AppelEdgeDump : public RenderingElement {
    public:
        int edgeIndex;
        int edgeType;
        java::String edgeTypeName;
        int face1Id;
        int face2Id;
        Vector3Dd start;
        Vector3Dd end;
        int initialQuantitativeInvisibility;
        java::ArrayList<AppelSegmentDump> segments;
        java::ArrayList<AppelEventDump> events;

        AppelEdgeDump()
            : edgeIndex(0), edgeType(0), face1Id(0), face2Id(0),
              initialQuantitativeInvisibility(0)
        {
        }
    };

    class AppelAlgorithmDump : public RenderingElement {
    public:
        java::ArrayList<AppelEdgeDump> edges;
    };

    static int isFaceVisibleFromCamera(
        _PolyhedralBoundedSolidFace* face,
        const Camera* camera);

    static void executeAppelAlgorithm(
        java::ArrayList<SimpleBody*>& inSimpleBodyArray,
        const Camera* inCamera,
        Calligraphic2DBuffer* outVisibleContourLineSet,
        Calligraphic2DBuffer* outVisibleNonContourLineSet,
        Calligraphic2DBuffer* outHiddenLineSet);

    /**
    As `executeAppelAlgorithm`, also collecting for each edge its
    sub-segments and their classification.
    @param outDump diagnostics (C++ port: an output parameter instead of the
    return value)
    */
    static void executeAppelAlgorithmWithDiagnostics(
        java::ArrayList<SimpleBody*>& inSimpleBodyArray,
        const Camera* inCamera,
        Calligraphic2DBuffer* outVisibleContourLineSet,
        Calligraphic2DBuffer* outVisibleNonContourLineSet,
        Calligraphic2DBuffer* outHiddenLineSet,
        AppelAlgorithmDump& outDump);

    static java::String edgeTypeName(int edgeType);

private:
    static void runAppelAlgorithm(
        java::ArrayList<SimpleBody*>& inSimpleBodyArray,
        const Camera* inCamera,
        Calligraphic2DBuffer* outVisibleContourLineSet,
        Calligraphic2DBuffer* outVisibleNonContourLineSet,
        Calligraphic2DBuffer* outHiddenLineSet,
        bool collectDiagnostics,
        AppelAlgorithmDump& outDump);
};

#endif
