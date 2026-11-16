//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___POLYHEDRAL_BOUNDED_SOLID_SET_FINISHER__
#define ___POLYHEDRAL_BOUNDED_SOLID_SET_FINISHER__

#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.h"

class PolyhedralBoundedSolid;
class PolyhedralBoundedSolidFaceGeometry;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidLoop;

/**
Finish stage (big phase 4) for set operations, corresponding to the answer
integration step of program [MANT1988].15.15.

C++ counterpart of Java's `_PolyhedralBoundedSolidSetFinisher`.
*/
class _PolyhedralBoundedSolidSetFinisher : public _PolyhedralBoundedSolidOperator {
public:
    typedef java::ArrayList<_PolyhedralBoundedSolidFace*> FaceList;


    /**
    @return faces triangulated in the most recent finish on the calling
    thread (section 9.2 instrumentation)
    */
    static int getLastTriangulatedFaceCount();

    /**
    Restores the planar-face invariant of [MANT1988].10.2.1 after the answer
    integration step, fanning each non-planar face into triangles with the
    `lmef(scan.next, scan.previous, newId)` split.
    @param solid solid to fix
    */
    static void triangulateNonPlanarFaces(PolyhedralBoundedSolid* solid);

    /**
    Answer integrator for the set-operations pipeline.
    Following program [MANT1988].15.15.
    @param inSolidA first operand, its result faces are moved to `outRes`
    @param inSolidB second operand, its result faces are moved to `outRes`
    @param outRes result solid
    @param op UNION, INTERSECTION or SUBTRACT
    @param debugFlags debug flags
    @param sonfa faces cut from A by the connect stage (updated in place)
    @param sonfb faces cut from B by the connect stage (updated in place)
    @param pairs pair indexes of the null faces, as connect matched them
    @throws SolidOperationFailedException when no null face pair matches
    */
    static void finish(PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        PolyhedralBoundedSolid* outRes,
        int op,
        int debugFlags,
        FaceList& sonfa,
        FaceList& sonfb,
        const _PolyhedralBoundedSolidSetNullEdgesConnector::ConnectResult& pairs);

private:
    static const int DEBUG_01_STRUCTURE = 0x01;
    static const int DEBUG_06_FINISH = 0x20;

    typedef java::ArrayList<PolyhedralBoundedSolidFaceGeometry*> FaceGeometries;

    static thread_local int lastTriangulatedFaceCount;

    _PolyhedralBoundedSolidSetFinisher();

    static bool isPipelineSummaryTraceEnabled();
    static void tracePipelineSummary(const java::String& message);
    static bool hasUsableIntegrationRing(_PolyhedralBoundedSolidFace* face);
    static bool hasCompleteHalfEdgeConnectivity(_PolyhedralBoundedSolidFace* face);
    static java::String integrationRingSummary(_PolyhedralBoundedSolidFace* face,
        const _PolyhedralBoundedSolidSetNullEdgesConnector::ConnectResult& pairs);
    static int sanitizePairedFaces(FaceList& sonfa, FaceList& sonfb,
        const _PolyhedralBoundedSolidSetNullEdgesConnector::ConnectResult& pairs);
    static void exchange(FaceList& faces, int i, int j);
    static void countSide(_PolyhedralBoundedSolidFace* face,
        PolyhedralBoundedSolid* other, const FaceGeometries& otherFaces,
        int counts[2]);
    static void sidesAcross(_PolyhedralBoundedSolidLoop* loop,
        PolyhedralBoundedSolid* other, const FaceGeometries& otherFaces,
        int counts[2]);
    static bool isBand(_PolyhedralBoundedSolidFace* face);
    static bool sidesAreSwapped(_PolyhedralBoundedSolidFace* face,
        PolyhedralBoundedSolid* other, const FaceGeometries& otherFaces,
        int cut, const char* name);
    static void flood(_PolyhedralBoundedSolidFace* seed,
        java::HashMap<_PolyhedralBoundedSolidFace*, bool>& reached,
        FaceList* component);
    static FaceList untouchedShellsToKeep(PolyhedralBoundedSolid* solid,
        const FaceList& cutFaces, PolyhedralBoundedSolid* other,
        bool keepInside);
    static _PolyhedralBoundedSolidHalfEdge* findNonDegenerateEar(
        _PolyhedralBoundedSolidHalfEdge* start,
        int loopSize,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& context);
    static bool hasSelfTouchingVertex(_PolyhedralBoundedSolidLoop* loop,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& tol);
    static void extractInnerLoopsOfNonPlanarFace(PolyhedralBoundedSolid* solid,
        _PolyhedralBoundedSolidFace* face);
};

#endif
