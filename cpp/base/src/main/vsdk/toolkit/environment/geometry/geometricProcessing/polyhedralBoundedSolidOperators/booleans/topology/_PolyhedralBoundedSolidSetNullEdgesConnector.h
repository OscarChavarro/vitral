//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___POLYHEDRAL_BOUNDED_SOLID_SET_NULL_EDGES_CONNECTOR__
#define ___POLYHEDRAL_BOUNDED_SOLID_SET_NULL_EDGES_CONNECTOR__

#include "java/util/ArrayList.h"
#include "java/util/TreeMap.h"

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetOperatorNullEdge.h"

class InfinitePlane;
class _PolyhedralBoundedSolidEdge;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;

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
    typedef java::ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> NullEdgeList;
    typedef java::ArrayList<_PolyhedralBoundedSolidFace*> FaceList;
    typedef java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> HalfEdgeList;

    /**
    C++ port note: the Java result keeps a reference to its connector to
    answer the pair indexes; here it keeps a copy of the connector's final
    pair index maps, since the connector is usually a local of the caller.
    The default constructor builds the Java null result.
    */
    class ConnectResult {
    public:
        ConnectResult();
        ConnectResult(const FaceList& sonfa, const FaceList& sonfb,
            const _PolyhedralBoundedSolidSetNullEdgesConnector& connector);
        /** Pair index of a null face of A, or -1 (used by finish). */
        int pairIndexA(_PolyhedralBoundedSolidFace* face) const;
        /** Pair index of a null face of B, or -1 (used by finish). */
        int pairIndexB(_PolyhedralBoundedSolidFace* face) const;
        const FaceList& sonfa() const;
        const FaceList& sonfb() const;
    private:
        FaceList sonfaValue;
        FaceList sonfbValue;
        java::TreeMap<int, int> sonfaPairIndexByFaceId;
        java::TreeMap<int, int> sonfbPairIndexByFaceId;
        bool hasPairIndexMaps;
    };

    /**
    Curve report of the most recent connect on the calling thread.
    @return null when there is none
    */
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

    static const int DEBUG_01_STRUCTURE = 0x01;
    static const int DEBUG_05_CONNECT = 0x10;
    static const int DEBUG_99_SHOWOPERATIONS = 0x40;
    static const int ENDPOINT_SOLID_A = 0;
    static const int ENDPOINT_SOLID_B = 1;

    static int debugFlags;
    int operation;
    int nextNullEdgeIndex;
    NullEdgeList* sonea;
    NullEdgeList* soneb;
    HalfEdgeList endsa;
    HalfEdgeList endsb;
    FaceList sonfa;
    FaceList sonfb;
    java::TreeMap<int, int> sonfaPairIndexByFaceId;
    java::TreeMap<int, int> sonfbPairIndexByFaceId;
    bool hasPairIndexMaps;

    /**
    Counters of the most recent `connect` on the calling thread, for tests
    and diagnostics. The connector itself keeps all its working state in
    the instance (Fable5.2Analysis.md pending item 9).
    */
    class Diagnostics {
    public:
        int looseA;
        int looseB;
        int sonfaCount;
        int sonfbCount;
        int pairCount;
        /** False for the Java null report. */
        bool hasCurveReport;
        _PolyhedralBoundedSolidSetIntersectionCurveBuilder::Report curveReport;
        Diagnostics();
    };

    static thread_local Diagnostics LAST;
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
    bool curveOrientationApplied;

    /**
    Junction adjacency in processing-position space when the curve order is
    active; empty (Java null) otherwise. `scanjoin` joins a null edge only
    with a loose end left by one of its neighbours along the curve.
    */
    java::ArrayList<java::ArrayList<int> > curveNeighborPositions;

    /**
    Processing pair index that pushed each loose entry, parallel to
    `endsa`/`endsb`.
    */
    java::ArrayList<int> endsPairIndex;

    static bool isPipelineSummaryTraceEnabled();
    static bool isKeepInsertionOrderEnabled();
    static void tracePipelineSummary(const java::String& message);
    static java::String summarizeHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static java::String summarizeNullEdge(
        const _PolyhedralBoundedSolidSetOperatorNullEdge& edge);
    void setCurrentConnectContext(int pairIndex);
    int allocateSyntheticPairIndex();
    static java::String summarizeLooseEnds(const HalfEdgeList& endsA,
        const HalfEdgeList& endsB);
    static bool isSamePoint(_PolyhedralBoundedSolidHalfEdge* first,
        _PolyhedralBoundedSolidHalfEdge* second);
    static bool canCutCoincidentHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static bool hasReusableCoincidentCutFace(_PolyhedralBoundedSolidHalfEdge* he);
    static bool canCutCoincidentFinishFace(_PolyhedralBoundedSolidHalfEdge* he);
    static _PolyhedralBoundedSolidFace* registerCoincidentCutFace(
        _PolyhedralBoundedSolidHalfEdge* he, FaceList& target,
        const char* label);
    static bool canFinalizeCoincidentLooseA(_PolyhedralBoundedSolidHalfEdge* he);
    static bool canFinalizeCoincidentLooseB(_PolyhedralBoundedSolidHalfEdge* he);
    static bool isPointLikeHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static bool isDegenerateCoincidentLooseClosure(
        _PolyhedralBoundedSolidHalfEdge* firstA,
        _PolyhedralBoundedSolidHalfEdge* secondA,
        _PolyhedralBoundedSolidHalfEdge* firstB,
        _PolyhedralBoundedSolidHalfEdge* secondB);
    void finalizeCoincidentLooseA(_PolyhedralBoundedSolidHalfEdge* he);
    void finalizeCoincidentLooseB(_PolyhedralBoundedSolidHalfEdge* he);
    void removeLoosePair(int index);
    void updateLastSnapshot();
    static bool isLiveHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static bool sharesParentFace(_PolyhedralBoundedSolidHalfEdge* first,
        _PolyhedralBoundedSolidHalfEdge* second);
    static bool isOppositeHalfEdgeSide(_PolyhedralBoundedSolidHalfEdge* first,
        _PolyhedralBoundedSolidHalfEdge* second);
    static _PolyhedralBoundedSolidFace* findUniqueClassicRebindTargetFace(
        _PolyhedralBoundedSolidHalfEdge* currentTargetFirst,
        _PolyhedralBoundedSolidHalfEdge* currentTargetSecond,
        _PolyhedralBoundedSolidHalfEdge* currentReferenceFirst,
        _PolyhedralBoundedSolidHalfEdge* currentReferenceSecond,
        const HalfEdgeList& targetEnds,
        const HalfEdgeList& referenceEnds);
    static bool isStrictRebindTargetFace(_PolyhedralBoundedSolidFace* targetFace,
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
    void applyOrderPermutation(const java::ArrayList<int>& order);
    static int curveComponentFind(java::ArrayList<int>& parent, int x);
    void groupNullEdgesByRing();
    bool sgetnextnulledge(NullEdgePair& out);
    bool scanjoin(_PolyhedralBoundedSolidHalfEdge* hea,
        _PolyhedralBoundedSolidHalfEdge* heb,
        _PolyhedralBoundedSolidHalfEdge** outA,
        _PolyhedralBoundedSolidHalfEdge** outB);
    bool isCurveNeighborOfCurrentPair(int i);
    bool isLooseA(_PolyhedralBoundedSolidHalfEdge* he);
    bool isLooseB(_PolyhedralBoundedSolidHalfEdge* he);
    _PolyhedralBoundedSolidFace* cutA(_PolyhedralBoundedSolidHalfEdge* he);
    _PolyhedralBoundedSolidFace* cutB(_PolyhedralBoundedSolidHalfEdge* he);
    static void removeLastCutFaceIfSame(FaceList& faces,
        _PolyhedralBoundedSolidFace* face);
    void setOpConnect();
    bool dropIsolatedNodes();
    bool liesInCoplanarFaces(int index);
    static bool isVertexInCoplanarFace(_PolyhedralBoundedSolidEdge* vertexEdge,
        _PolyhedralBoundedSolidEdge* strut);
    static bool hasFaceOnPlane(_PolyhedralBoundedSolidHalfEdge* start,
        InfinitePlane* plane,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& context);
    void dropIsolatedStruts();
};

#endif
