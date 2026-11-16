//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "java/util/Collections.h"
#include "java/lang/Math.h"
#include "java/lang/String.h"
#include "java/util/TreeMap.txx"
#include "java/util/TreeSet.txx"
#include <cmath>
#include <cstdio>
#include <strings.h>

#include "java/lang/Boolean.h"
#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef _PolyhedralBoundedSolidSetNullEdgesConnector Connector;

namespace {

const char* const TRACE_PIPELINE_SUMMARY_PROPERTY =
    "vsdk.setop.tracePipelineSummary";
const char* const KEEP_INSERTION_ORDER_PROPERTY =
    "vsdk.setop.connect.keepInsertionOrder";

java::String str(int value)
{
    return java::String::valueOf(value);
}

java::String positionString(const Vector3Dd& p)
{
    java::String* text = p.toString();
    java::String result(text->c_str());
    delete text;
    return result;
}

}

int Connector::debugFlags = 0;
thread_local Connector::Diagnostics Connector::LAST;

Connector::Diagnostics::Diagnostics()
    : looseA(0), looseB(0), sonfaCount(0), sonfbCount(0), pairCount(0),
      hasCurveReport(false)
{
}

//= Results =========================================================

Connector::ConnectResult::ConnectResult()
    : hasPairIndexMaps(false)
{
}

Connector::ConnectResult::ConnectResult(const FaceList& sonfa,
    const FaceList& sonfb, const Connector& connector)
    : sonfaValue(sonfa), sonfbValue(sonfb),
      sonfaPairIndexByFaceId(connector.sonfaPairIndexByFaceId),
      sonfbPairIndexByFaceId(connector.sonfbPairIndexByFaceId),
      hasPairIndexMaps(connector.hasPairIndexMaps)
{
}

/** Pair index of a null face of A, or -1 (used by finish). */
int Connector::ConnectResult::pairIndexA(_PolyhedralBoundedSolidFace* face) const
{
    if ( face == 0 || !hasPairIndexMaps ) {
        return -1;
    }
    return sonfaPairIndexByFaceId.getOrDefault(face->id, -1);
}

/** Pair index of a null face of B, or -1 (used by finish). */
int Connector::ConnectResult::pairIndexB(_PolyhedralBoundedSolidFace* face) const
{
    if ( face == 0 || !hasPairIndexMaps ) {
        return -1;
    }
    return sonfbPairIndexByFaceId.getOrDefault(face->id, -1);
}

const Connector::FaceList& Connector::ConnectResult::sonfa() const
{
    return sonfaValue;
}

const Connector::FaceList& Connector::ConnectResult::sonfb() const
{
    return sonfbValue;
}

Connector::_PolyhedralBoundedSolidSetNullEdgesConnector()
    : operation(0), nextNullEdgeIndex(0), sonea(0), soneb(0),
      hasPairIndexMaps(false), currentConnectPairIndex(0),
      nextSyntheticPairIndex(0), curveOrientationApplied(false)
{
}

/** Curve report of the most recent connect on the calling thread. */
const _PolyhedralBoundedSolidSetIntersectionCurveBuilder::Report*
Connector::getLastCurveReport()
{
    return LAST.hasCurveReport ? &LAST.curveReport : nullptr;
}

void Connector::clearLastCurveReport()
{
    LAST.hasCurveReport = false;
    LAST.curveReport = _PolyhedralBoundedSolidSetIntersectionCurveBuilder::Report();
}

//= Traces and summaries ============================================

bool Connector::isPipelineSummaryTraceEnabled()
{
    return java::Boolean::getBoolean(TRACE_PIPELINE_SUMMARY_PROPERTY);
}

bool Connector::isKeepInsertionOrderEnabled()
{
    // As Java: an unset property means true, otherwise `Boolean.parseBoolean`
    java::String propertyValue = java::System::getProperty(
        KEEP_INSERTION_ORDER_PROPERTY, "true");
    return strcasecmp(propertyValue.c_str(), "true") == 0;
}

void Connector::tracePipelineSummary(const java::String& message)
{
    if ( !isPipelineSummaryTraceEnabled() ) {
        return;
    }
    printf("[SetOpPipelineTrace] %s\n", message.c_str());
}

java::String Connector::summarizeHalfEdge(_PolyhedralBoundedSolidHalfEdge* he)
{
    if ( he == 0 ) {
        return "null";
    }

    java::String from = "?";
    java::String to = "?";
    java::String face = "?";

    if ( he->startingVertex != 0 ) {
        from = str(he->startingVertex->id);
    }
    if ( he->parentLoop != 0 ) {
        _PolyhedralBoundedSolidHalfEdge* next = he->next();
        if ( next != 0 && next->startingVertex != 0 ) {
            to = str(next->startingVertex->id);
        }
        if ( he->parentLoop->parentFace != 0 ) {
            face = str(he->parentLoop->parentFace->id);
        }
    }
    java::String fromPoint = "?";
    java::String toPoint = "?";

    if ( he->startingVertex != 0 ) {
        fromPoint = positionString(he->startingVertex->position);
    }
    if ( he->parentLoop != 0 ) {
        _PolyhedralBoundedSolidHalfEdge* next = he->next();
        if ( next != 0 && next->startingVertex != 0 ) {
            toPoint = positionString(next->startingVertex->position);
        }
    }
    java::String text = "he(v=" + from + "->" + to + ",f=" + face +
        ",p=" + fromPoint + "->" + toPoint + ")";
    return java::String(text.c_str());
}

java::String Connector::summarizeNullEdge(
    const _PolyhedralBoundedSolidSetOperatorNullEdge& edge)
{
    if ( edge.e == 0 ) {
        return "null";
    }
    return summarizeHalfEdge(edge.e->rightHalf) + " | " +
        summarizeHalfEdge(edge.e->leftHalf);
}

void Connector::setCurrentConnectContext(int pairIndex)
{
    currentConnectPairIndex = pairIndex;
}

int Connector::allocateSyntheticPairIndex()
{
    int pairIndex;

    pairIndex = nextSyntheticPairIndex;
    nextSyntheticPairIndex++;
    return pairIndex;
}

int Connector::getSonfaPairIndex(_PolyhedralBoundedSolidFace* face)
{
    if ( face == 0 || !hasPairIndexMaps ) {
        return -1;
    }
    return sonfaPairIndexByFaceId.getOrDefault(face->id, -1);
}

int Connector::getSonfbPairIndex(_PolyhedralBoundedSolidFace* face)
{
    if ( face == 0 || !hasPairIndexMaps ) {
        return -1;
    }
    return sonfbPairIndexByFaceId.getOrDefault(face->id, -1);
}

java::String Connector::summarizeLooseEnds(const HalfEdgeList& endsA,
    const HalfEdgeList& endsB)
{
    java::String out;
    size_t i;

    out += ("pairs=" + str((int)endsA.size()) + " [").c_str();
    for ( i = 0; i < endsA.size() && i < endsB.size(); i++ ) {
        if ( i > 0 ) {
            out += " | ";
        }
        out += (str((int)i) + ":A=").c_str();
        out += summarizeHalfEdge(endsA[i]);
        out += ",B=";
        out += summarizeHalfEdge(endsB[i]);
    }
    out += "]";
    return out;
}

//= Coincidence helpers =============================================

bool Connector::isSamePoint(_PolyhedralBoundedSolidHalfEdge* first,
    _PolyhedralBoundedSolidHalfEdge* second)
{
    if ( first == 0 || second == 0 ||
         first->startingVertex == 0 || second->startingVertex == 0 ) {
        return false;
    }
    return PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
        first->startingVertex->position,
        second->startingVertex->position,
        numericContext);
}

bool Connector::canCutCoincidentHalfEdge(_PolyhedralBoundedSolidHalfEdge* he)
{
    _PolyhedralBoundedSolidEdge* edge;
    _PolyhedralBoundedSolidLoop* loop;

    if ( he == 0 ) {
        return false;
    }
    edge = he->parentEdge;
    loop = he->parentLoop;
    if ( edge == 0 || loop == 0 ) {
        return false;
    }
    if ( edge->rightHalf == 0 || edge->leftHalf == 0 ) {
        return false;
    }
    if ( edge->rightHalf->parentLoop != edge->leftHalf->parentLoop ) {
        return true;
    }
    return loop->halfEdgesList.size() > 2;
}

bool Connector::hasReusableCoincidentCutFace(_PolyhedralBoundedSolidHalfEdge* he)
{
    _PolyhedralBoundedSolidFace* face;

    if ( he == 0 || he->parentLoop == 0 ) {
        return false;
    }
    face = he->parentLoop->parentFace;
    return face != 0 && face->boundariesList.size() > 1;
}

bool Connector::canCutCoincidentFinishFace(_PolyhedralBoundedSolidHalfEdge* he)
{
    _PolyhedralBoundedSolidEdge* edge;
    _PolyhedralBoundedSolidLoop* loop;

    if ( he == 0 ) {
        return false;
    }
    edge = he->parentEdge;
    loop = he->parentLoop;
    if ( edge == 0 || loop == 0 ||
         edge->rightHalf == 0 || edge->leftHalf == 0 ||
         edge->rightHalf->parentLoop == 0 ||
         edge->leftHalf->parentLoop == 0 ) {
        return false;
    }
    // Classic case: both halves in the same loop and loop large enough for
    // an interior cut.
    if ( edge->rightHalf->parentLoop == edge->leftHalf->parentLoop ) {
        return loop->halfEdgesList.size() > 2;
    }
    // Cross-loop intersection edge whose halves still share the same face
    // (typical for null-edges produced by the intersect+classify pipeline
    // on tessellated curved surfaces).
    return edge->rightHalf->parentLoop->parentFace ==
           edge->leftHalf->parentLoop->parentFace;
}

_PolyhedralBoundedSolidFace* Connector::registerCoincidentCutFace(
    _PolyhedralBoundedSolidHalfEdge* he,
    FaceList& target,
    const char* label)
{
    _PolyhedralBoundedSolidFace* face;

    (void)label;
    if ( he == 0 || he->parentLoop == 0 ) {
        return 0;
    }
    face = he->parentLoop->parentFace;
    if ( face == 0 || face->boundariesList.size() <= 1 ) {
        return 0;
    }
    target.add(face);
    return face;
}

bool Connector::canFinalizeCoincidentLooseA(_PolyhedralBoundedSolidHalfEdge* he)
{
    return canCutCoincidentFinishFace(he) || hasReusableCoincidentCutFace(he);
}

bool Connector::canFinalizeCoincidentLooseB(_PolyhedralBoundedSolidHalfEdge* he)
{
    return canCutCoincidentFinishFace(he) || hasReusableCoincidentCutFace(he);
}

bool Connector::isPointLikeHalfEdge(_PolyhedralBoundedSolidHalfEdge* he)
{
    return isLiveHalfEdge(he) &&
        he->next() != 0 &&
        he->next()->startingVertex != 0 &&
        isSamePoint(he, he->next());
}

bool Connector::isDegenerateCoincidentLooseClosure(
    _PolyhedralBoundedSolidHalfEdge* firstA,
    _PolyhedralBoundedSolidHalfEdge* secondA,
    _PolyhedralBoundedSolidHalfEdge* firstB,
    _PolyhedralBoundedSolidHalfEdge* secondB)
{
    return isPointLikeHalfEdge(firstA) &&
        isPointLikeHalfEdge(secondA) &&
        isPointLikeHalfEdge(firstB) &&
        isPointLikeHalfEdge(secondB);
}

void Connector::finalizeCoincidentLooseA(_PolyhedralBoundedSolidHalfEdge* he)
{
    if ( canCutCoincidentFinishFace(he) ) {
        cutA(he);
    }
    else {
        registerCoincidentCutFace(he, sonfa, "reuse-sonfa");
    }
}

void Connector::finalizeCoincidentLooseB(_PolyhedralBoundedSolidHalfEdge* he)
{
    if ( canCutCoincidentFinishFace(he) ) {
        cutB(he);
    }
    else {
        registerCoincidentCutFace(he, sonfb, "reuse-sonfb");
    }
}

void Connector::removeLoosePair(int index)
{
    endsa.remove(index);
    endsb.remove(index);
}

int Connector::getLastLooseACount()
{
    return LAST.looseA;
}

int Connector::getLastLooseBCount()
{
    return LAST.looseB;
}

int Connector::getLastSonfaCount()
{
    return LAST.sonfaCount;
}

int Connector::getLastSonfbCount()
{
    return LAST.sonfbCount;
}

int Connector::getLastPairCount()
{
    return LAST.pairCount;
}

void Connector::updateLastSnapshot()
{
    Diagnostics& last = LAST;
    last.looseA = (int)endsa.size();
    last.looseB = (int)endsb.size();
    last.sonfaCount = (int)sonfa.size();
    last.sonfbCount = (int)sonfb.size();
}

bool Connector::isLiveHalfEdge(_PolyhedralBoundedSolidHalfEdge* he)
{
    return he != 0 &&
        he->parentEdge != 0 &&
        he->parentLoop != 0 &&
        he->parentLoop->parentFace != 0;
}

bool Connector::sharesParentFace(_PolyhedralBoundedSolidHalfEdge* first,
    _PolyhedralBoundedSolidHalfEdge* second)
{
    return isLiveHalfEdge(first) &&
        isLiveHalfEdge(second) &&
        first->parentLoop->parentFace == second->parentLoop->parentFace;
}

bool Connector::isOppositeHalfEdgeSide(_PolyhedralBoundedSolidHalfEdge* first,
    _PolyhedralBoundedSolidHalfEdge* second)
{
    if ( first == 0 || second == 0 ||
         first->parentEdge == 0 || second->parentEdge == 0 ) {
        return false;
    }

    return (first == first->parentEdge->rightHalf &&
            second == second->parentEdge->leftHalf) ||
           (first == first->parentEdge->leftHalf &&
            second == second->parentEdge->rightHalf);
}

//= Classic rebind ==================================================

_PolyhedralBoundedSolidFace* Connector::findUniqueClassicRebindTargetFace(
    _PolyhedralBoundedSolidHalfEdge* currentTargetFirst,
    _PolyhedralBoundedSolidHalfEdge* currentTargetSecond,
    _PolyhedralBoundedSolidHalfEdge* currentReferenceFirst,
    _PolyhedralBoundedSolidHalfEdge* currentReferenceSecond,
    const HalfEdgeList& targetEnds,
    const HalfEdgeList& referenceEnds)
{
    _PolyhedralBoundedSolidFace* candidateFace = 0;
    size_t i;
    size_t j;
    size_t pairCount;

    if ( !isLiveHalfEdge(currentTargetFirst) ||
         !isLiveHalfEdge(currentTargetSecond) ||
         !isLiveHalfEdge(currentReferenceFirst) ||
         !isLiveHalfEdge(currentReferenceSecond) ) {
        return 0;
    }

    pairCount = java::Math::min(targetEnds.size(), referenceEnds.size());
    for ( i = 0; i < pairCount; i++ ) {
        _PolyhedralBoundedSolidHalfEdge* looseTargetFirst;
        _PolyhedralBoundedSolidHalfEdge* looseReferenceFirst;

        looseTargetFirst = targetEnds[i];
        looseReferenceFirst = referenceEnds[i];
        if ( !isLiveHalfEdge(looseTargetFirst) ||
             !isLiveHalfEdge(looseReferenceFirst) ||
             !isOppositeHalfEdgeSide(currentTargetFirst, looseTargetFirst) ||
             !neighbor(currentReferenceFirst, looseReferenceFirst) ) {
            continue;
        }

        for ( j = 0; j < pairCount; j++ ) {
            _PolyhedralBoundedSolidHalfEdge* looseTargetSecond;
            _PolyhedralBoundedSolidHalfEdge* looseReferenceSecond;
            _PolyhedralBoundedSolidFace* looseFace;

            if ( i == j ) {
                continue;
            }

            looseTargetSecond = targetEnds[j];
            looseReferenceSecond = referenceEnds[j];
            if ( !isLiveHalfEdge(looseTargetSecond) ||
                 !isLiveHalfEdge(looseReferenceSecond) ||
                 !isOppositeHalfEdgeSide(currentTargetSecond, looseTargetSecond) ||
                 !neighbor(currentReferenceSecond, looseReferenceSecond) ) {
                continue;
            }

            looseFace = looseTargetFirst->parentLoop->parentFace;
            if ( looseFace != looseTargetSecond->parentLoop->parentFace ||
                 looseFace == currentTargetFirst->parentLoop->parentFace ) {
                continue;
            }

            if ( candidateFace == 0 ) {
                candidateFace = looseFace;
            }
            else if ( candidateFace != looseFace ) {
                return 0;
            }
        }
    }
    return candidateFace;
}

bool Connector::isStrictRebindTargetFace(_PolyhedralBoundedSolidFace* targetFace,
    _PolyhedralBoundedSolidHalfEdge* currentTarget)
{
    if ( targetFace == 0 ||
         !isLiveHalfEdge(currentTarget) ||
         currentTarget->startingVertex == 0 ) {
        return false;
    }
    InfinitePlane* plane = targetFace->getContainingPlane();
    if ( plane == 0 ) {
        return false;
    }

    PolyhedralBoundedSolidNumericPolicy::ToleranceContext faceContext =
        PolyhedralBoundedSolidNumericPolicy::forFace(targetFace);
    Vector3Dd point = currentTarget->startingVertex->position;
    double distance = std::fabs(plane->pointDistance(point));
    delete plane;
    if ( distance > faceContext.bigEpsilon() ) {
        return false;
    }

    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
        ::pointInFace(targetFace, point) == Geometry::INSIDE;
}

void Connector::rebindClassicCurrentNullEdgeIfNeeded(
    _PolyhedralBoundedSolidHalfEdge* currentTargetFirst,
    _PolyhedralBoundedSolidHalfEdge* currentTargetSecond,
    _PolyhedralBoundedSolidHalfEdge* currentReferenceFirst,
    _PolyhedralBoundedSolidHalfEdge* currentReferenceSecond,
    const HalfEdgeList& targetEnds,
    const HalfEdgeList& referenceEnds,
    const char* label)
{
    _PolyhedralBoundedSolidFace* targetFace;
    _PolyhedralBoundedSolidFace* sourceFace;
    _PolyhedralBoundedSolidLoop* sourceLoop;
    PolyhedralBoundedSolid* solid;

    if ( operation != SUBTRACT ||
         !isLiveHalfEdge(currentTargetFirst) ||
         !isLiveHalfEdge(currentTargetSecond) ||
         currentTargetFirst->parentLoop != currentTargetSecond->parentLoop ) {
        return;
    }

    sourceLoop = currentTargetFirst->parentLoop;
    sourceFace = sourceLoop->parentFace;
    if ( sourceLoop->halfEdgesList.size() != 2 ) {
        return;
    }

    targetFace = findUniqueClassicRebindTargetFace(
        currentTargetFirst, currentTargetSecond,
        currentReferenceFirst, currentReferenceSecond,
        targetEnds, referenceEnds);
    if ( targetFace == 0 ||
         targetFace == sourceFace ||
         !isStrictRebindTargetFace(targetFace, currentTargetFirst) ) {
        return;
    }

    solid = currentTargetFirst->parentLoop->parentFace->parentSolid;
    if ( !PolyhedralBoundedSolidEulerOperators::lringmv(
             solid, sourceLoop, targetFace, false) ) {
        tracePipelineSummary(java::String(("connect rebind" + java::String(label) +
            " failed sourceFace=" + str(sourceFace->id) + " targetFace=" +
            str(targetFace->id) + " edge=").c_str()) +
            summarizeHalfEdge(currentTargetFirst));
        return;
    }
    tracePipelineSummary(java::String(("connect rebind" + java::String(label) +
        " sourceFace=" + str(sourceFace->id) + " targetFace=" +
        str(targetFace->id) + " edge=").c_str()) +
        summarizeHalfEdge(currentTargetFirst));
}

void Connector::rebindClassicCurrentNullEdgesIfNeeded(
    _PolyhedralBoundedSolidHalfEdge* currentARight,
    _PolyhedralBoundedSolidHalfEdge* currentALeft,
    _PolyhedralBoundedSolidHalfEdge* currentBLeft,
    _PolyhedralBoundedSolidHalfEdge* currentBRight)
{
    rebindClassicCurrentNullEdgeIfNeeded(
        currentARight, currentALeft,
        currentBLeft, currentBRight,
        endsa, endsb, "A");
    rebindClassicCurrentNullEdgeIfNeeded(
        currentBLeft, currentBRight,
        currentARight, currentALeft,
        endsb, endsa, "B");
}

//= Entry point =====================================================

Connector::ConnectResult Connector::connect(int op, int flags,
    NullEdgeList& inSonea, NullEdgeList& inSoneb)
{
    operation = op;
    debugFlags = flags;
    sonea = &inSonea;
    soneb = &inSoneb;
    LAST = Diagnostics();
    Diagnostics& last = LAST;
    last.pairCount = (int)java::Math::min(sonea->size(), soneb->size());
    nextSyntheticPairIndex = last.pairCount;
    // [MANT1988] section 15.7 Program 15.14: single Connect implementation.
    // The "flexibleEndpointChains" alternative path was removed in section
    // 6.1 of plan-csg-boolean-fix-stage2 because it duplicated setOpConnect
    // with extra heuristics that the book does not require.
    setOpConnect();
    return ConnectResult(sonfa, sonfb, *this);
}

//= Null edges ordering =============================================

void Connector::sortNullEdges()
{
    curveOrientationApplied = false;
    curveNeighborPositions.clear();

    // Always group null-edges by topological ring before any further
    // processing. Without this, the connect loop pairs null-edges from
    // different intersection curves (e.g., the outer and inner boundary
    // circles of a spherical shell), producing non-coplanar faces.
    // Ring grouping is safe for the single-ring case (it is a no-op).
    groupNullEdgesByRing();

    if ( isKeepInsertionOrderEnabled() ) {
        tracePipelineSummary("connect sort skipped; using insertion order");
        return;
    }

    // Geometric sort within each ring, enabled only when keepInsertionOrder
    // is explicitly disabled via the system property.
    sonea->sort(nullptr);
    soneb->sort(nullptr);
}

void Connector::dbgDumpNullEdges(const char* label, const NullEdgeList& sone)
{
    size_t k;
    for ( k = 0; k < sone.size(); k++ ) {
        const _PolyhedralBoundedSolidSetOperatorNullEdge& ne = sone[k];
        _PolyhedralBoundedSolidHalfEdge* rh = ne.e->rightHalf;
        _PolyhedralBoundedSolidHalfEdge* lh = ne.e->leftHalf;
        printf("[DBG-ne] %s[%d] R{v=%d f=%d p=(%.4f,%.4f,%.4f)} "
            "L{v=%d f=%d p=(%.4f,%.4f,%.4f)}\n",
            label, (int)k,
            rh->startingVertex->id, rh->parentLoop->parentFace->id,
            rh->startingVertex->position.x(),
            rh->startingVertex->position.y(),
            rh->startingVertex->position.z(),
            lh->startingVertex->id, lh->parentLoop->parentFace->id,
            lh->startingVertex->position.x(),
            lh->startingVertex->position.y(),
            lh->startingVertex->position.z());
    }
}

/**
Reorders sonea/soneb in lockstep by the given permutation
(`position -> originalIndex`).
@param order permutation covering every index exactly once
*/
void Connector::applyOrderPermutation(const java::ArrayList<int>& order)
{
    NullEdgeList orderedA;
    NullEdgeList orderedB;
    size_t i;

    for ( i = 0; i < order.size(); i++ ) {
        orderedA.add((*sonea)[order[i]]);
        orderedB.add((*soneb)[order[i]]);
    }
    *sonea = orderedA;
    *soneb = orderedB;
}

int Connector::curveComponentFind(java::ArrayList<int>& parent, int x)
{
    while ( parent[x] != x ) {
        parent[x] = parent[parent[x]];
        x = parent[x];
    }
    return x;
}

namespace {

void nullEdgeFaceIds(const _PolyhedralBoundedSolidSetOperatorNullEdge& ne,
    long long ids[2])
{
    ids[0] = ne.e->rightHalf->parentLoop->parentFace->id;
    ids[1] = ne.e->leftHalf->parentLoop->parentFace->id;
}

bool nullEdgesShareFace(const long long* a, const long long* b)
{
    return a[0] == b[0] || a[0] == b[1] || a[1] == b[0] || a[1] == b[1];
}

}

/**
Reorders `sonea`/`soneb` so that null-edge pairs belonging to the same
intersection curve are contiguous and ordered for `scanjoin`, and distinct
curves are separated. Each intersection curve is recovered as a connected
component over the paired null-edge indices (face adjacency); within each
component the classifier's emission order is preserved.
*/
void Connector::groupNullEdgesByRing()
{
    if ( isPipelineSummaryTraceEnabled() ) {
        dbgDumpNullEdges("A", *sonea);
        dbgDumpNullEdges("B", *soneb);
    }

    // Reconstruct the intersection curves for diagnosis. The report is
    // captured before any reordering decision so it always describes the
    // classifier's raw emission.
    curveReport = _PolyhedralBoundedSolidSetIntersectionCurveBuilder
        ::build(*sonea, *soneb,
            PolyhedralBoundedSolidNumericPolicy::defaultContext()
                .unitVectorTolerance());
    LAST.curveReport = curveReport;
    LAST.hasCurveReport = true;
    tracePipelineSummary(java::String("connect ") + curveReport.summarize());
    if ( dropIsolatedNodes() ) {
        // The report kept for diagnostics stays the one above: what
        // the classifier emitted. This one describes what is left to
        // connect.
        curveReport = _PolyhedralBoundedSolidSetIntersectionCurveBuilder
            ::build(*sonea, *soneb,
                PolyhedralBoundedSolidNumericPolicy::defaultContext()
                    .unitVectorTolerance());
        tracePipelineSummary(java::String("connect ") + curveReport.summarize());
    }

    int n = (int)java::Math::min(sonea->size(), soneb->size());
    if ( n != (int)sonea->size() || n != (int)soneb->size() || n < 2 ) {
        return;
    }

    // When every null-edge pair lies on a cleanly closed intersection
    // curve, reorder pairs along each curve AND orient every strut
    // consistently with the traversal. On any anomaly fall through to the
    // legacy ordering below.
    if ( curveReport.isCleanlyClosed() ) {
        java::ArrayList<int> curveOrder;
        java::ArrayList<java::ArrayList<int> > neighbors;
        if ( _PolyhedralBoundedSolidSetIntersectionCurveBuilder
                 ::orderAndOrientAlongCurves(curveReport.cycles, n,
                     *sonea, *soneb, curveOrder, &neighbors) ) {
            applyOrderPermutation(curveOrder);
            curveOrientationApplied = true;
            curveNeighborPositions = neighbors;
            tracePipelineSummary(("connect curve-order applied: cycles=" +
                str((int)curveReport.cycles.size())).c_str());
            return;
        }
    }

    // When all null-edge rings are singletons (every pair is a zero-length
    // strut), there is no ring structure to separate, and a spatial
    // signature sort only scrambles the classifier's already-valid emission
    // order. Preserve insertion order in that case.
    java::ArrayList<long long> faceIdsA = java::Collections::nCopies<long long>((size_t)n * 2, 0LL);
    java::ArrayList<long long> faceIdsB = java::Collections::nCopies<long long>((size_t)n * 2, 0LL);
    int k;
    for ( k = 0; k < n; k++ ) {
        nullEdgeFaceIds((*sonea)[k], &faceIdsA[2*k]);
        nullEdgeFaceIds((*soneb)[k], &faceIdsB[2*k]);
    }

    java::ArrayList<int> parent = java::Collections::nCopies<int>((size_t)n, 0);
    for ( k = 0; k < n; k++ ) {
        parent[k] = k;
    }
    int i;
    int j;
    for ( i = 0; i < n; i++ ) {
        for ( j = i + 1; j < n; j++ ) {
            if ( nullEdgesShareFace(&faceIdsA[2*i], &faceIdsA[2*j]) ||
                 nullEdgesShareFace(&faceIdsB[2*i], &faceIdsB[2*j]) ) {
                parent[curveComponentFind(parent, i)] =
                    curveComponentFind(parent, j);
            }
        }
    }

    // Count distinct curve components.
    java::TreeSet<int> roots;
    for ( k = 0; k < n; k++ ) {
        roots.add(curveComponentFind(parent, k));
    }
    int componentCount = (int)roots.size();

    // Decide whether to reorder: only when at least one shared vertex id
    // exists, which signals a multi-curve intersection where distinct
    // curves must be kept contiguous. Vertex ids (not positions) are used
    // because after weldCoincidentVertices the coincident pairs are already
    // merged.
    java::TreeSet<int> vidsA;
    bool hasSharedA = false;
    int ki;
    for ( ki = 0; ki < n && !hasSharedA; ki++ ) {
        int v1 = (*sonea)[ki].e->rightHalf->startingVertex->id;
        int v2 = (*sonea)[ki].e->leftHalf->startingVertex->id;
        if ( !vidsA.add(v1) || (v1 != v2 && !vidsA.add(v2)) ) {
            hasSharedA = true;
        }
    }
    bool hasSharedB = false;
    if ( !hasSharedA ) {
        java::TreeSet<int> vidsB;
        for ( ki = 0; ki < n && !hasSharedB; ki++ ) {
            int v1 = (*soneb)[ki].e->rightHalf->startingVertex->id;
            int v2 = (*soneb)[ki].e->leftHalf->startingVertex->id;
            if ( !vidsB.add(v1) || (v1 != v2 && !vidsB.add(v2)) ) {
                hasSharedB = true;
            }
        }
    }
    if ( !hasSharedA && !hasSharedB ) {
        tracePipelineSummary("connect ring-group: all singletons; "
            "preserving insertion order");
        return;
    }

    // Multiple pairs share face-adjacency: reconstruct curve order, with
    // components in order of first appearance (as Java `LinkedHashMap`).
    java::ArrayList<int> componentRoots;
    java::ArrayList<java::ArrayList<int> > components;
    for ( k = 0; k < n; k++ ) {
        int root = curveComponentFind(parent, k);
        size_t c;
        for ( c = 0; c < componentRoots.size(); c++ ) {
            if ( componentRoots[c] == root ) {
                break;
            }
        }
        if ( c == componentRoots.size() ) {
            componentRoots.add(root);
            components.add(java::ArrayList<int>());
        }
        components[c].add(k);
    }

    NullEdgeList orderedA;
    NullEdgeList orderedB;
    for ( size_t c = 0; c < components.size(); c++ ) {
        for ( size_t m = 0; m < components[c].size(); m++ ) {
            int idx = components[c][m];
            orderedA.add((*sonea)[idx]);
            orderedB.add((*soneb)[idx]);
        }
    }
    *sonea = orderedA;
    *soneb = orderedB;

    tracePipelineSummary(("connect curve-components: count=" +
        str(componentCount)).c_str());
}

/**
Implements sgetnextnulledge per [MANT1988] section 15.7, Program 15.14:
advances the internal cursor over `sonea`/`soneb` and fills `out` with the
next (nea, neb) pair.
*/
bool Connector::sgetnextnulledge(NullEdgePair& out)
{
    if ( nextNullEdgeIndex >= (int)sonea->size() ||
         nextNullEdgeIndex >= (int)soneb->size() ) {
        return false;
    }
    out.nea = &(*sonea)[nextNullEdgeIndex];
    out.neb = &(*soneb)[nextNullEdgeIndex];
    out.pairIndex = nextNullEdgeIndex;
    nextNullEdgeIndex++;
    return true;
}

/**
Implements scanjoin per [MANT1988] section 15.7, Program 15.13: finds the
loose pair `i` such that `hea` is a neighbor of `endsa[i]` AND `heb` is a
neighbor of `endsb[i]`, removes and returns it. Otherwise `hea` and `heb`
are appended to the loose lists and false (Java null) is returned.
*/
bool Connector::scanjoin(_PolyhedralBoundedSolidHalfEdge* hea,
    _PolyhedralBoundedSolidHalfEdge* heb,
    _PolyhedralBoundedSolidHalfEdge** outA,
    _PolyhedralBoundedSolidHalfEdge** outB)
{
    size_t i;
    bool condition1;
    bool condition2;

    for ( i = 0; i < endsa.size(); i++ ) {
        // A face that is not convex can be crossed several times by
        // the same face of the other solid: the crossings lie on one
        // line and all of them pass the face and role test of
        // `neighbor`. Only the loose end left by a neighbour along the
        // intersection curve is the other end of this chord; joining
        // any other one draws a chord through the notch of the face
        // (plan-stage11 Phase F). Program 15.13 of [MANT1988] gets the
        // same effect from the order of its sorted null edges.
        if ( !isCurveNeighborOfCurrentPair((int)i) ) {
            continue;
        }
        condition1 = neighbor(hea, endsa[i]);
        condition2 = neighbor(heb, endsb[i]);

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            printf("    . Testing for neighborhood A[%d/%d] vs. A[%d/%d]: %s "
                "ParentFaces: %d / %d\n",
                hea->startingVertex->id, hea->next()->startingVertex->id,
                endsa[i]->startingVertex->id,
                endsa[i]->next()->startingVertex->id,
                condition1 ? "true" : "false",
                hea->parentLoop->parentFace->id,
                endsa[i]->parentLoop->parentFace->id);
            printf("    . Testing for neighborhood B[%d/%d] vs. B[%d/%d]: %s "
                "ParentFaces: %d / %d\n",
                heb->startingVertex->id, heb->next()->startingVertex->id,
                endsb[i]->startingVertex->id,
                endsb[i]->next()->startingVertex->id,
                condition2 ? "true" : "false",
                heb->parentLoop->parentFace->id,
                endsb[i]->parentLoop->parentFace->id);
        }

        if ( condition1 && condition2 ) {
            *outA = endsa[i];
            *outB = endsb[i];
            endsa.remove(i);
            endsb.remove(i);
            if ( i < endsPairIndex.size() ) {
                endsPairIndex.remove((long)i);
            }
            return true;
        }
    }

    endsa.add(hea);
    endsb.add(heb);
    endsPairIndex.add(currentConnectPairIndex);
    return false;
}

/**
True when the loose entry `i` was left by one of the two pairs next to the
current pair on its intersection curve, or when the curves are not known
(no curve order applied): then every entry is a candidate, as in the book.
*/
bool Connector::isCurveNeighborOfCurrentPair(int i)
{
    if ( !curveOrientationApplied || curveNeighborPositions.isEmpty() ||
         currentConnectPairIndex < 0 ||
         currentConnectPairIndex >= (int)curveNeighborPositions.size() ||
         curveNeighborPositions[currentConnectPairIndex].isEmpty() ||
         i >= (int)endsPairIndex.size() ) {
        return true;
    }
    int pusherPosition = endsPairIndex[i];

    return pusherPosition ==
            curveNeighborPositions[currentConnectPairIndex][0] ||
        pusherPosition ==
            curveNeighborPositions[currentConnectPairIndex][1];
}

bool Connector::isLooseA(_PolyhedralBoundedSolidHalfEdge* he)
{
    return endsa.contains(he);
}

bool Connector::isLooseB(_PolyhedralBoundedSolidHalfEdge* he)
{
    return endsb.contains(he);
}

//= Cuts ============================================================

_PolyhedralBoundedSolidFace* Connector::cutA(_PolyhedralBoundedSolidHalfEdge* he)
{
    PolyhedralBoundedSolid* s;
    _PolyhedralBoundedSolidFace* addedFace = 0;
    bool withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                     ((debugFlags & DEBUG_05_CONNECT) != 0x00);

    if ( withDebug ) {
        printf("       -> CUTA:\n");
        printf("          . He: %s\n", he->toString().c_str());
    }

    s = he->parentLoop->parentFace->parentSolid;
    if ( he->parentEdge->rightHalf->parentLoop ==
         he->parentEdge->leftHalf->parentLoop ) {
        addedFace = he->parentLoop->parentFace;
        PolyhedralBoundedSolidEulerOperators::lkemr(s,
            he->parentEdge->rightHalf, he->parentEdge->leftHalf);
        if ( addedFace->boundariesList.size() >= 2 ) {
            sonfa.add(addedFace);
            if ( hasPairIndexMaps ) {
                sonfaPairIndexByFaceId.put(addedFace->id, currentConnectPairIndex);
            }
        }
        else {
            addedFace = 0;
        }
    }
    else {
        PolyhedralBoundedSolidEulerOperators::lkef(s,
            he->parentEdge->rightHalf, he->parentEdge->leftHalf);
    }
    return addedFace;
}

_PolyhedralBoundedSolidFace* Connector::cutB(_PolyhedralBoundedSolidHalfEdge* he)
{
    PolyhedralBoundedSolid* s;
    _PolyhedralBoundedSolidFace* addedFace = 0;
    bool withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                     ((debugFlags & DEBUG_05_CONNECT) != 0x00);

    if ( withDebug ) {
        printf("       -> CUTB:\n");
        printf("          . He: %s\n", he->toString().c_str());
    }

    s = he->parentLoop->parentFace->parentSolid;
    if ( he->parentEdge->rightHalf->parentLoop ==
         he->parentEdge->leftHalf->parentLoop ) {
        addedFace = he->parentLoop->parentFace;
        PolyhedralBoundedSolidEulerOperators::lkemr(s,
            he->parentEdge->rightHalf, he->parentEdge->leftHalf);
        if ( addedFace->boundariesList.size() >= 2 ) {
            sonfb.add(addedFace);
            if ( hasPairIndexMaps ) {
                sonfbPairIndexByFaceId.put(addedFace->id, currentConnectPairIndex);
            }
        }
        else {
            addedFace = 0;
        }
    }
    else {
        PolyhedralBoundedSolidEulerOperators::lkef(s,
            he->parentEdge->rightHalf, he->parentEdge->leftHalf);
    }
    return addedFace;
}

void Connector::removeLastCutFaceIfSame(FaceList& faces,
    _PolyhedralBoundedSolidFace* face)
{
    if ( face == 0 || faces.isEmpty() ) {
        return;
    }
    if ( faces[faces.size() - 1] == face ) {
        faces.remove(faces.size() - 1);
    }
}

//= Connect loop ====================================================

/**
Neighbor null edges connector for the set operations algorithm
(big phase 3). Following section [MANT1988].15.7. and program
[MANT1988].15.14.
*/
void Connector::setOpConnect()
{
    if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
        printf("- 3. ------------------------------------------------------------------------------------------------------------------------------------------------------\n");
    }

    sortNullEdges();

    int i;

    if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
        printf("SORTED SET OF %d NULL EDGES PAIRS TO BE CONNECTED\n",
            (int)sonea->size());
    }
    tracePipelineSummary(("connect start pairsA=" + str((int)sonea->size()) +
        " pairsB=" + str((int)soneb->size())).c_str());
    if ( isPipelineSummaryTraceEnabled() ) {
        int sameLoopA = 0;
        int diffLoopA = 0;
        for ( size_t k = 0; k < sonea->size(); k++ ) {
            _PolyhedralBoundedSolidEdge* e = (*sonea)[k].e;
            if ( e->rightHalf != 0 && e->leftHalf != 0 &&
                 e->rightHalf->parentLoop == e->leftHalf->parentLoop ) {
                sameLoopA++;
            }
            else {
                diffLoopA++;
            }
        }
        int sameLoopB = 0;
        int diffLoopB = 0;
        for ( size_t k = 0; k < soneb->size(); k++ ) {
            _PolyhedralBoundedSolidEdge* e = (*soneb)[k].e;
            if ( e->rightHalf != 0 && e->leftHalf != 0 &&
                 e->rightHalf->parentLoop == e->leftHalf->parentLoop ) {
                sameLoopB++;
            }
            else {
                diffLoopB++;
            }
        }
        tracePipelineSummary(("connect null-edge-loops A:sameLoop=" +
            str(sameLoopA) + " diffLoop=" + str(diffLoopA) +
            " B:sameLoop=" + str(sameLoopB) +
            " diffLoop=" + str(diffLoopB)).c_str());
    }

    _PolyhedralBoundedSolidEdge* nextedgea;
    _PolyhedralBoundedSolidEdge* nextedgeb;
    _PolyhedralBoundedSolidHalfEdge* h1a = 0;
    _PolyhedralBoundedSolidHalfEdge* h2a = 0;
    _PolyhedralBoundedSolidHalfEdge* h1b = 0;
    _PolyhedralBoundedSolidHalfEdge* h2b = 0;
    _PolyhedralBoundedSolidHalfEdge* r0;
    _PolyhedralBoundedSolidHalfEdge* r1;
    bool withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                     ((debugFlags & DEBUG_05_CONNECT) != 0x00);

    endsa.clear();
    endsb.clear();
    endsPairIndex.clear();

    sonfa.clear();
    sonfb.clear();
    sonfaPairIndexByFaceId.clear();
    sonfbPairIndexByFaceId.clear();
    hasPairIndexMaps = true;
    setCurrentConnectContext(-1);
    size_t j;

    if ( sonea->size() != soneb->size() ) {
        PolyhedralBoundedSolidStatistics::recordOperationFailureCase();
        printf("**** Not paired null edges!\n");
    }

    // [MANT1988] Program 15.14:
    //   while (sgetnextnulledge(&nea, &neb)) { ... }
    // The cursor is set to 0 here so each call to setOpConnect()
    // restarts iteration from the first pair of the sorted set.
    nextNullEdgeIndex = 0;
    NullEdgePair pair;
    while ( sgetnextnulledge(pair) ) {
        _PolyhedralBoundedSolidSetOperatorNullEdge* nea = pair.nea;
        _PolyhedralBoundedSolidSetOperatorNullEdge* neb = pair.neb;
        i = pair.pairIndex;
        _PolyhedralBoundedSolidHalfEdge* ha;
        _PolyhedralBoundedSolidHalfEdge* ham;
        _PolyhedralBoundedSolidHalfEdge* hb;
        _PolyhedralBoundedSolidHalfEdge* hbm;
        _PolyhedralBoundedSolidHalfEdge* tmp;

        ha = nea->e->rightHalf;
        ham = nea->e->leftHalf;
        hb = neb->e->rightHalf;
        hbm = neb->e->leftHalf;
        if ( isPipelineSummaryTraceEnabled() ) {
            tracePipelineSummary(java::String(("connect pair[" + str(i) +
                "] A{").c_str()) + summarizeNullEdge(*nea) + "} B{" +
                summarizeNullEdge(*neb) + "}");
        }

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            printf("  - %d = %d+%d loose ends before processing pair [%d]:\n",
                (int)(endsa.size() + endsb.size()), (int)endsa.size(),
                (int)endsb.size(), i);

            for ( j = 0; j < endsa.size(); j++ ) {
                _PolyhedralBoundedSolidHalfEdge* hat = endsa[j];
                _PolyhedralBoundedSolidHalfEdge* hbt = endsb[j];
                printf("    . [%d]: He(A): %d/%d | He(B): %d/%d\n", (int)j,
                    hat->startingVertex->id, hat->next()->startingVertex->id,
                    hbt->startingVertex->id, hbt->next()->startingVertex->id);
            }

            if ( ha->startingVertex->id > ham->startingVertex->id ) {
                printf("********* FORCING ORDER!\n");
            }

            printf("  - Processing pair [%d]: He(A1): %d/%d He(A2): %d/%d "
                "He(B1): %d/%d He(B2): %d/%d\n", i,
                ha->startingVertex->id, ha->next()->startingVertex->id,
                ham->startingVertex->id, ham->next()->startingVertex->id,
                hb->startingVertex->id, hb->next()->startingVertex->id,
                hbm->startingVertex->id, hbm->next()->startingVertex->id);
        }

        nextedgea = nea->e;
        nextedgeb = neb->e;
        h1a = 0;
        h2a = 0;
        h1b = 0;
        h2b = 0;
        // Legacy strut orientation: vertex-id order encodes classifier
        // emission order. When sortNullEdges already oriented the struts
        // along the intersection curves, that orientation must be respected.
        if ( !curveOrientationApplied ) {
            if ( ha->startingVertex->id > ham->startingVertex->id ) {
                tmp = nextedgea->rightHalf;
                nextedgea->rightHalf = nextedgea->leftHalf;
                nextedgea->leftHalf = tmp;
            }
            if ( hb->startingVertex->id > hbm->startingVertex->id ) {
                tmp = nextedgeb->rightHalf;
                nextedgeb->rightHalf = nextedgeb->leftHalf;
                nextedgeb->leftHalf = tmp;
            }
        }

        rebindClassicCurrentNullEdgesIfNeeded(
            nextedgea->rightHalf,
            nextedgea->leftHalf,
            nextedgeb->leftHalf,
            nextedgeb->rightHalf);

        setCurrentConnectContext(i);
        if ( scanjoin(nextedgea->rightHalf, nextedgeb->leftHalf, &r0, &r1) ) {
            h1a = r0;
            h2b = r1;
            join(h1a, nextedgea->rightHalf, withDebug);
            if ( !isLooseA(h1a->mirrorHalfEdge()) ) {
                cutA(h1a);
            }
            join(h2b, nextedgeb->leftHalf, withDebug);
            if ( !isLooseB(h2b->mirrorHalfEdge()) ) {
                cutB(h2b);
            }
        }

        setCurrentConnectContext(i);
        if ( scanjoin(nextedgea->leftHalf, nextedgeb->rightHalf, &r0, &r1) ) {
            h2a = r0;
            h1b = r1;
            join(h2a, nextedgea->leftHalf, withDebug);
            if ( !isLooseA(h2a->mirrorHalfEdge()) ) {
                cutA(h2a);
            }
            join(h1b, nextedgeb->rightHalf, withDebug);
            if ( !isLooseB(h1b->mirrorHalfEdge()) ) {
                cutB(h1b);
            }
        }

        if ( h1a != 0 && h1b != 0 && h2a != 0 && h2b != 0 ) {
            cutA(nextedgea->rightHalf);
            cutB(nextedgeb->rightHalf);
            if ( isPipelineSummaryTraceEnabled() ) {
                tracePipelineSummary(java::String(("connect pair[" + str(i) +
                    "] produced cuts h1a=").c_str()) + summarizeHalfEdge(h1a) +
                    " h2a=" + summarizeHalfEdge(h2a) +
                    " h1b=" + summarizeHalfEdge(h1b) +
                    " h2b=" + summarizeHalfEdge(h2b));
            }
        }
        else {
            PolyhedralBoundedSolidStatistics::recordJoinIncompleteCase();
            PolyhedralBoundedSolidStatistics::recordOperationFailureCase();
            if ( isPipelineSummaryTraceEnabled() ) {
                tracePipelineSummary(java::String(("connect pair[" + str(i) +
                    "] incomplete h1a=").c_str()) + summarizeHalfEdge(h1a) +
                    " h2a=" + summarizeHalfEdge(h2a) +
                    " h1b=" + summarizeHalfEdge(h1b) +
                    " h2b=" + summarizeHalfEdge(h2b) +
                    (" looseA=" + str((int)endsa.size()) +
                     " looseB=" + str((int)endsb.size())).c_str());
            }
        }
    }

    if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
        printf("  . Pending null edges to connect:\n");
        for ( j = 0; j < endsa.size(); j++ ) {
            printf("    . A[%d]: %s\n", (int)(j+1), endsa[j]->toString().c_str());
        }
        for ( j = 0; j < endsb.size(); j++ ) {
            printf("    . B[%d]: %s\n", (int)(j+1), endsb[j]->toString().c_str());
        }
    }
    tracePipelineSummary(("connect end sonfa=" + str((int)sonfa.size()) +
        " sonfb=" + str((int)sonfb.size()) +
        " looseA=" + str((int)endsa.size()) +
        " looseB=" + str((int)endsb.size())).c_str());
    // Per [MANT1988] Program 15.14 the main loop must leave
    // looseA == looseB == 0. The only survivors left are isolated
    // struts: both halves of one null edge still loose on A and on B.
    // The vertex/vertex classifier emits them at a coplanar contact
    // along a boundary ray (sectoroverlap is closed, see its comment)
    // where the curve does not pass, e.g. MANT1988_15_1 ∩ and −. Such
    // a strut bounds no null face; killing it restores the vertex it
    // split. It never joins anything, so it cannot fuse shells (the
    // retired pairLatentLooseEnds closure did, on HOLLOW_BRICK).
    dropIsolatedStruts();
    tracePipelineSummary(("connect post-pass sonfa=" + str((int)sonfa.size()) +
        " sonfb=" + str((int)sonfb.size()) +
        " looseA=" + str((int)endsa.size()) +
        " looseB=" + str((int)endsb.size())).c_str());
    updateLastSnapshot();

    if ( isPipelineSummaryTraceEnabled() ) {
        for ( j = 0; j < endsa.size() && j < endsb.size(); j++ ) {
            tracePipelineSummary(java::String(("connect loose[" + str((int)j) +
                "] A=").c_str()) + summarizeHalfEdge(endsa[j]) + " B=" +
                summarizeHalfEdge(endsb[j]));
        }
    }
}

/**
A node without neighbours on the intersection curve is a point where the
two boundaries touch without crossing: nothing is to be cut there. When
such nodes are the only thing that keeps the curves from being closed
cycles, their null edges are removed before connect (each with lkev,
which merges its two coincident vertices back into one), so that the rest
is connected in curve order. Groups of nodes on a pair of coplanar faces
do not prevent it: on coplanar faces those groups are made of the
isolated nodes themselves (vertices of one solid lying in a face of the
other) and are gone once they are removed. They used to stay, and a
single one of them sent the whole operation to the order of emission,
where a face crossed twice along one line is joined across its hole
(plan-stage11, coplanar non-convex faces).
@return true when some null edge was removed
*/
bool Connector::dropIsolatedNodes()
{
    java::ArrayList<int> dropped;
    int covered = 0;
    int i;
    int j;

    if ( !curveReport.pinchNodes.isEmpty() ||
         curveReport.oddFacePairGroupCount > 0 ||
         sonea->size() != soneb->size() ) {
        return false;
    }
    for ( i = 0; i < (int)curveReport.openChains.size(); i++ ) {
        const java::ArrayList<int>& chain = curveReport.openChains[i];
        for ( j = 0; j < (int)chain.size(); j++ ) {
            if ( !liesInCoplanarFaces(chain[j]) ) {
                return false;
            }
            dropped.add(chain[j]);
        }
        covered += (int)chain.size();
    }
    for ( i = 0; i < (int)curveReport.isolatedNodes.size(); i++ ) {
        dropped.add(curveReport.isolatedNodes[i]);
    }
    if ( dropped.isEmpty() ) {
        return false;
    }
    for ( i = 0; i < (int)curveReport.cycles.size(); i++ ) {
        covered += (int)curveReport.cycles[i].size();
    }
    if ( covered + (int)curveReport.isolatedNodes.size() !=
         curveReport.nodeCount ) {
        return false;
    }
    dropped.sort(nullptr);
    for ( i = (int)dropped.size() - 1; i >= 0; i-- ) {
        int index = dropped[i];
        _PolyhedralBoundedSolidEdge* a = (*sonea)[index].e;
        _PolyhedralBoundedSolidEdge* b = (*soneb)[index].e;
        tracePipelineSummary(java::String(("connect drop contact node " +
            str(index) + " A=").c_str()) + summarizeHalfEdge(a->rightHalf) +
            " B=" + summarizeHalfEdge(b->rightHalf));
        PolyhedralBoundedSolidEulerOperators::lkev(
            a->rightHalf->parentLoop->parentFace->parentSolid, a->rightHalf,
            a->leftHalf);
        PolyhedralBoundedSolidEulerOperators::lkev(
            b->rightHalf->parentLoop->parentFace->parentSolid, b->rightHalf,
            b->leftHalf);
        sonea->remove(index);
        soneb->remove(index);
    }
    return true;
}

/**
True when node `index` is a vertex of one solid lying inside a face of the
other, with a face of its own on the plane of that face: on that solid the
null edge is a strut inside the face, and some face around the vertex of
the first solid is coplanar with it. A chain of such nodes is an edge
lying in a coplanar face; it crosses nothing.
*/
bool Connector::liesInCoplanarFaces(int index)
{
    _PolyhedralBoundedSolidEdge* a = (*sonea)[index].e;
    _PolyhedralBoundedSolidEdge* b = (*soneb)[index].e;

    if ( a == 0 || b == 0 || a->rightHalf == 0 ||
         a->leftHalf == 0 || b->rightHalf == 0 ||
         b->leftHalf == 0 ) {
        return false;
    }
    return isVertexInCoplanarFace(a, b) || isVertexInCoplanarFace(b, a);
}

/**
True when `strut` lies inside one face and some face around the ends of
`vertexEdge` is on the plane of that face.
*/
bool Connector::isVertexInCoplanarFace(_PolyhedralBoundedSolidEdge* vertexEdge,
    _PolyhedralBoundedSolidEdge* strut)
{
    _PolyhedralBoundedSolidFace* face =
        strut->rightHalf->parentLoop->parentFace;
    InfinitePlane* plane = face->getContainingPlane();

    if ( face != strut->leftHalf->parentLoop->parentFace || plane == 0 ) {
        delete plane;
        return false;
    }
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(face->parentSolid,
            vertexEdge->rightHalf->parentLoop->parentFace->parentSolid);
    bool answer = hasFaceOnPlane(vertexEdge->rightHalf, plane, context) ||
        hasFaceOnPlane(vertexEdge->leftHalf, plane, context);
    delete plane;
    return answer;
}

/** Walks the faces around the starting vertex of `start`. */
bool Connector::hasFaceOnPlane(_PolyhedralBoundedSolidHalfEdge* start,
    InfinitePlane* plane,
    const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& context)
{
    _PolyhedralBoundedSolidHalfEdge* he = start;
    Vector3Dd normal = plane->getNormal();
    int guard = 0;

    do {
        InfinitePlane* own = he->parentLoop->parentFace->getContainingPlane();
        if ( own != 0 ) {
            Vector3Dd ownNormal = own->getNormal();
            delete own;
            if ( normal.crossProduct(ownNormal).length() <=
                     context.unitVectorTolerance() * normal.length() *
                         ownNormal.length() &&
                 java::Math::abs(plane->pointDistance(
                     he->startingVertex->position)) <=
                     context.bigEpsilon() ) {
                return true;
            }
        }
        _PolyhedralBoundedSolidHalfEdge* mate = he->mirrorHalfEdge();
        if ( mate == 0 ) {
            return false;
        }
        he = mate->next();
        guard++;
    } while ( he != 0 && he != start && guard < 1000 );
    return false;
}

/**
Removes the null edges whose two halves are both loose, on A and on B at
the same loose positions (see the call site). Each is killed with lkev,
which merges its two coincident vertices back into one.
*/
void Connector::dropIsolatedStruts()
{
    int i;
    int j;

    for ( i = 0; i < (int)endsa.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* a = endsa[i];
        _PolyhedralBoundedSolidHalfEdge* b = endsb[i];
        for ( j = i + 1; j < (int)endsa.size(); j++ ) {
            if ( endsa[j] != a->mirrorHalfEdge() ||
                 endsb[j] != b->mirrorHalfEdge() ) {
                continue;
            }
            tracePipelineSummary(java::String("connect drop isolated strut A=")
                + summarizeHalfEdge(a) + " B=" + summarizeHalfEdge(b));
            PolyhedralBoundedSolidEulerOperators::lkev(
                a->parentLoop->parentFace->parentSolid, a,
                a->mirrorHalfEdge());
            PolyhedralBoundedSolidEulerOperators::lkev(
                b->parentLoop->parentFace->parentSolid, b,
                b->mirrorHalfEdge());
            removeLoosePair(j);
            removeLoosePair(i);
            if ( j < (int)endsPairIndex.size() ) {
                endsPairIndex.remove((long)j);
            }
            if ( i < (int)endsPairIndex.size() ) {
                endsPairIndex.remove((long)i);
            }
            i--;
            break;
        }
    }
}
