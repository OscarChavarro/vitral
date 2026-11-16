//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_ST_NLL_EDGS_CNNCTR__
#define ___PBS_ST_NLL_EDGS_CNNCTR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/TreeMap.h"
#include "java/util/ArrayList.h"

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetOperatorNullEdge.h"

class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidEdge;
class InfinitePlane;

/**
Connect stage (big phase 3) for set operations: null-edges pairing and joins,
following section [MANT1988].15.7 and programs [MANT1988].15.13 and
[MANT1988].15.14.

C++ counterpart of Java's `_PolyhedralBoundedSolidSetNullEdgesConnector`.
The connector reorders the given `sonea`/`soneb` lists in place, as the Java
version does.
*/
class _PolyhedralBoundedSolidSetNullEdgesConnector
    : public _PolyhedralBoundedSolidOperator {
public:
    typedef ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> NullEdgeList;
    typedef ArrayList<_PolyhedralBoundedSolidFace*> FaceList;
    typedef ArrayList<_PolyhedralBoundedSolidHalfEdge*> HalfEdgeList;

    /**
    C++ port note: Java keeps a reference to the connector to answer the
    pair indexes; here the result keeps a copy of the connector's pair index
    maps, so it does not depend on the connector lifetime.
    */
    class ConnectResult {
    public:
        ConnectResult();
        ConnectResult(const FaceList& sonfa, const FaceList& sonfb,
            const _PolyhedralBoundedSolidSetNullEdgesConnector& connector);
        int pairIndexA(_PolyhedralBoundedSolidFace* face) const;
        int pairIndexB(_PolyhedralBoundedSolidFace* face) const;
        const FaceList& sonfa() const;
        const FaceList& sonfb() const;
    private:
        FaceList sonfaValue;
        FaceList sonfbValue;
        TreeMap<int, int> sonfaPairIndexByFaceId;
        TreeMap<int, int> sonfbPairIndexByFaceId;
        BOOLEAN hasPairIndexMaps;
    };

    /** Curve report of the most recent connect (NULL: Java null). */
    static const _PolyhedralBoundedSolidSetIntersectionCurveBuilder::Report*
        getLastCurveReport();
    static void clearLastCurveReport();

    _PolyhedralBoundedSolidSetNullEdgesConnector();

    /**
    Neighbor null edges connector for the set operations algorithm
    (big phase 3), following section [MANT1988].15.7 and program
    [MANT1988].15.14.
    @param op UNION, INTERSECTION or SUBTRACT
    @param flags debug flags
    @param inSonea null edges of A, reordered in place
    @param inSoneb null edges of B, reordered in place
    @return the faces cut from each solid (sonfa, sonfb)
    */
    ConnectResult connect(int op, int flags, NullEdgeList& inSonea,
        NullEdgeList& inSoneb);

    int getSonfaPairIndex(_PolyhedralBoundedSolidFace* face);
    int getSonfbPairIndex(_PolyhedralBoundedSolidFace* face);
    static int getLastLooseACount();
    static int getLastLooseBCount();
    static int getLastSonfaCount();
    static int getLastSonfbCount();
    static int getLastPairCount();

private:
    /**
    Mutable carrier for the (nea, neb) pair returned by `sgetnextnulledge`,
    mirroring the out-param style used by [MANT1988] Program 15.14 in C.
    */
    struct NullEdgePair {
        _PolyhedralBoundedSolidSetOperatorNullEdge* nea;
        _PolyhedralBoundedSolidSetOperatorNullEdge* neb;
        int pairIndex;
    };

    enum {
        DEBUG_01_STRUCTURE = 0x01,
        DEBUG_05_CONNECT = 0x10,
        DEBUG_99_SHOWOPERATIONS = 0x40,
        ENDPOINT_SOLID_A = 0,
        ENDPOINT_SOLID_B = 1
    };

    static int debugFlags;
    int operation;
    int nextNullEdgeIndex;
    NullEdgeList* sonea;
    NullEdgeList* soneb;
    HalfEdgeList endsa;
    HalfEdgeList endsb;
    FaceList sonfa;
    FaceList sonfb;
    TreeMap<int, int> sonfaPairIndexByFaceId;
    TreeMap<int, int> sonfbPairIndexByFaceId;
    BOOLEAN hasPairIndexMaps;

    /**
    Counters of the most recent `connect`, for tests and diagnostics. The
    connector itself keeps all its working state in the instance.
    C++ port note: Java keeps them per thread (ThreadLocal); here they are
    a single static carrier.
    */
    struct Diagnostics {
        Diagnostics();
        int looseA;
        int looseB;
        int sonfaCount;
        int sonfbCount;
        int pairCount;
        _PolyhedralBoundedSolidSetIntersectionCurveBuilder::Report curveReport;
        BOOLEAN hasCurveReport;
    };
    static Diagnostics LAST;
    int currentConnectPairIndex;
    int nextSyntheticPairIndex;

    /**
    Structural report of the intersection curves reconstructed from the
    sonea/soneb pairs of this connect. Captured unconditionally (the
    reconstruction is cheap) so tests and diagnostics can audit curve
    closure without enabling pipeline traces; see `getLastCurveReport`.
    */
    _PolyhedralBoundedSolidSetIntersectionCurveBuilder::Report curveReport;

    /**
    True when `sortNullEdges` already oriented every strut along the
    intersection curves, so the connect loop must respect that orientation
    instead of re-normalizing edge halves by vertex-id comparison.
    */
    BOOLEAN curveOrientationApplied;

    /**
    Junction adjacency in processing-position space when the curve order is
    active; empty (Java null) otherwise. `scanjoin` joins a null edge only
    with a loose end left by one of its neighbours along the curve.
    */
    ArrayList<ArrayList<int> > curveNeighborPositions;

    /**
    Processing pair index that pushed each loose entry, parallel to
    `endsa`/`endsb`.
    */
    ArrayList<int> endsPairIndex;

    static BOOLEAN isPipelineSummaryTraceEnabled();
    static BOOLEAN isKeepInsertionOrderEnabled();
    static void tracePipelineSummary(const String& message);
    static String summarizeHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static String summarizeNullEdge(
        const _PolyhedralBoundedSolidSetOperatorNullEdge& edge);
    void setCurrentConnectContext(int pairIndex);
    int allocateSyntheticPairIndex();
    static String summarizeLooseEnds(const HalfEdgeList& endsA,
        const HalfEdgeList& endsB);
    static BOOLEAN isSamePoint(_PolyhedralBoundedSolidHalfEdge* first,
        _PolyhedralBoundedSolidHalfEdge* second);
    static BOOLEAN canCutCoincidentHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN hasReusableCoincidentCutFace(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN canCutCoincidentFinishFace(_PolyhedralBoundedSolidHalfEdge* he);
    static _PolyhedralBoundedSolidFace* registerCoincidentCutFace(
        _PolyhedralBoundedSolidHalfEdge* he, FaceList& target,
        const char* label);
    static BOOLEAN canFinalizeCoincidentLooseA(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN canFinalizeCoincidentLooseB(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN isPointLikeHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN isDegenerateCoincidentLooseClosure(
        _PolyhedralBoundedSolidHalfEdge* firstA,
        _PolyhedralBoundedSolidHalfEdge* secondA,
        _PolyhedralBoundedSolidHalfEdge* firstB,
        _PolyhedralBoundedSolidHalfEdge* secondB);
    void finalizeCoincidentLooseA(_PolyhedralBoundedSolidHalfEdge* he);
    void finalizeCoincidentLooseB(_PolyhedralBoundedSolidHalfEdge* he);
    void removeLoosePair(int index);
    void updateLastSnapshot();
    static BOOLEAN isLiveHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN sharesParentFace(_PolyhedralBoundedSolidHalfEdge* first,
        _PolyhedralBoundedSolidHalfEdge* second);
    static BOOLEAN isOppositeHalfEdgeSide(_PolyhedralBoundedSolidHalfEdge* first,
        _PolyhedralBoundedSolidHalfEdge* second);
    static _PolyhedralBoundedSolidFace* findUniqueClassicRebindTargetFace(
        _PolyhedralBoundedSolidHalfEdge* currentTargetFirst,
        _PolyhedralBoundedSolidHalfEdge* currentTargetSecond,
        _PolyhedralBoundedSolidHalfEdge* currentReferenceFirst,
        _PolyhedralBoundedSolidHalfEdge* currentReferenceSecond,
        const HalfEdgeList& targetEnds,
        const HalfEdgeList& referenceEnds);
    static BOOLEAN isStrictRebindTargetFace(_PolyhedralBoundedSolidFace* targetFace,
        _PolyhedralBoundedSolidHalfEdge* currentTarget);
    void rebindClassicCurrentNullEdgeIfNeeded(
        _PolyhedralBoundedSolidHalfEdge* currentTargetFirst,
        _PolyhedralBoundedSolidHalfEdge* currentTargetSecond,
        _PolyhedralBoundedSolidHalfEdge* currentReferenceFirst,
        _PolyhedralBoundedSolidHalfEdge* currentReferenceSecond,
        const HalfEdgeList& targetEnds,
        const HalfEdgeList& referenceEnds,
        const char* label);
    void rebindClassicCurrentNullEdgesIfNeeded(
        _PolyhedralBoundedSolidHalfEdge* currentARight,
        _PolyhedralBoundedSolidHalfEdge* currentALeft,
        _PolyhedralBoundedSolidHalfEdge* currentBLeft,
        _PolyhedralBoundedSolidHalfEdge* currentBRight);
    void sortNullEdges();
    static void dbgDumpNullEdges(const char* label, const NullEdgeList& sone);
    void applyOrderPermutation(const ArrayList<int>& order);
    static int curveComponentFind(ArrayList<int>& parent, int x);
    void groupNullEdgesByRing();
    BOOLEAN sgetnextnulledge(NullEdgePair& out);
    BOOLEAN scanjoin(_PolyhedralBoundedSolidHalfEdge* hea,
        _PolyhedralBoundedSolidHalfEdge* heb,
        _PolyhedralBoundedSolidHalfEdge** outA,
        _PolyhedralBoundedSolidHalfEdge** outB);
    BOOLEAN isCurveNeighborOfCurrentPair(int i);
    BOOLEAN isLooseA(_PolyhedralBoundedSolidHalfEdge* he);
    BOOLEAN isLooseB(_PolyhedralBoundedSolidHalfEdge* he);
    _PolyhedralBoundedSolidFace* cutA(_PolyhedralBoundedSolidHalfEdge* he);
    _PolyhedralBoundedSolidFace* cutB(_PolyhedralBoundedSolidHalfEdge* he);
    static void removeLastCutFaceIfSame(FaceList& faces,
        _PolyhedralBoundedSolidFace* face);
    void setOpConnect();
    BOOLEAN dropIsolatedNodes();
    BOOLEAN liesInCoplanarFaces(int index);
    static BOOLEAN isVertexInCoplanarFace(
        _PolyhedralBoundedSolidEdge* vertexEdge,
        _PolyhedralBoundedSolidEdge* strut);
    static BOOLEAN hasFaceOnPlane(
        _PolyhedralBoundedSolidHalfEdge* start, const InfinitePlane& plane,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& context);
    void dropIsolatedStruts();
};

#endif
