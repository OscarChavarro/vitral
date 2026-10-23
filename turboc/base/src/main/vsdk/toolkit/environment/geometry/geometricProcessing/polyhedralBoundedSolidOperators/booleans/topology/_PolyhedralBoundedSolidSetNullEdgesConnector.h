//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_SET_NULL_EDGES_CONNECTOR__
#define ___PBS_SET_NULL_EDGES_CONNECTOR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/TreeMap.h"
#include "java/util/ArrayList.h"

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetOperatorNullEdge.h"

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
    typedef ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> NullEdgeList;
    typedef ArrayList<_PolyhedralBoundedSolidFace*> FaceList;
    typedef ArrayList<_PolyhedralBoundedSolidHalfEdge*> HalfEdgeList;

    class ConnectResult {
    public:
        ConnectResult(const FaceList& sonfa, const FaceList& sonfb);
        const FaceList& sonfa() const;
        const FaceList& sonfb() const;
    private:
        FaceList sonfaValue;
        FaceList sonfbValue;
    };

    /**
    Structural report of the intersection curves reconstructed from the
    sonea/soneb pairs of the most recent `connect` call.
    */
    static _PolyhedralBoundedSolidSetIntersectionCurveBuilder::Report
        lastCurveReport;

    /**
    Test-only injection point: when `hasTestOnlyForcedConnectOrder` is set and
    the permutation matches the pair count, `sortNullEdges` reorders
    sonea/soneb by this permutation (entry `position -> originalIndex`)
    instead of the production ordering. Always unset in production.
    */
    static ArrayList<int> testOnlyForcedConnectOrder;
    static BOOLEAN hasTestOnlyForcedConnectOrder;

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

    static int getSonfaPairIndex(_PolyhedralBoundedSolidFace* face);
    static int getSonfbPairIndex(_PolyhedralBoundedSolidFace* face);
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
    static int operation;
    int nextNullEdgeIndex;
    NullEdgeList* sonea;
    NullEdgeList* soneb;
    static HalfEdgeList endsa;
    static HalfEdgeList endsb;
    FaceList sonfa;
    FaceList sonfb;
    static TreeMap<int, int> sonfaPairIndexByFaceId;
    static TreeMap<int, int> sonfbPairIndexByFaceId;
    static BOOLEAN hasPairIndexMaps;
    static int lastLooseACount;
    static int lastLooseBCount;
    static int lastSonfaCount;
    static int lastSonfbCount;
    static int lastPairCount;
    static int currentConnectPairIndex;
    static int nextSyntheticPairIndex;

    /**
    True when `sortNullEdges` already oriented every strut along the
    intersection curves, so the connect loop must respect that orientation
    instead of re-normalizing edge halves by vertex-id comparison.
    */
    static BOOLEAN curveOrientationApplied;

    /**
    Junction adjacency in processing-position space when the curve order is
    active; empty (Java null) otherwise.
    */
    static ArrayList<ArrayList<int> > curveNeighborPositions;

    /**
    Processing pair index that pushed each loose entry, parallel to
    `endsa`/`endsb`.
    */
    static ArrayList<int> endsPairIndex;

    static BOOLEAN isPipelineSummaryTraceEnabled();
    static BOOLEAN isKeepInsertionOrderEnabled();
    static void tracePipelineSummary(const String& message);
    static String summarizeHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static String summarizeNullEdge(
        const _PolyhedralBoundedSolidSetOperatorNullEdge& edge);
    static void setCurrentConnectContext(int pairIndex);
    static int allocateSyntheticPairIndex();
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
    static void removeLoosePair(int index);
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
    static void rebindClassicCurrentNullEdgeIfNeeded(
        _PolyhedralBoundedSolidHalfEdge* currentTargetFirst,
        _PolyhedralBoundedSolidHalfEdge* currentTargetSecond,
        _PolyhedralBoundedSolidHalfEdge* currentReferenceFirst,
        _PolyhedralBoundedSolidHalfEdge* currentReferenceSecond,
        const HalfEdgeList& targetEnds,
        const HalfEdgeList& referenceEnds,
        const char* label);
    static void rebindClassicCurrentNullEdgesIfNeeded(
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
    static BOOLEAN rolesOpposite(_PolyhedralBoundedSolidHalfEdge* h1,
        _PolyhedralBoundedSolidHalfEdge* h2);
    static BOOLEAN isPendingStrutRing(_PolyhedralBoundedSolidHalfEdge* he);
    BOOLEAN rescueRingFaceNearMiss(_PolyhedralBoundedSolidHalfEdge* hea,
        _PolyhedralBoundedSolidHalfEdge* heb,
        _PolyhedralBoundedSolidHalfEdge** outA,
        _PolyhedralBoundedSolidHalfEdge** outB);
    static BOOLEAN isLooseA(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN isLooseB(_PolyhedralBoundedSolidHalfEdge* he);
    _PolyhedralBoundedSolidFace* cutA(_PolyhedralBoundedSolidHalfEdge* he);
    _PolyhedralBoundedSolidFace* cutB(_PolyhedralBoundedSolidHalfEdge* he);
    static void removeLastCutFaceIfSame(FaceList& faces,
        _PolyhedralBoundedSolidFace* face);
    void setOpConnect();
};

#endif
