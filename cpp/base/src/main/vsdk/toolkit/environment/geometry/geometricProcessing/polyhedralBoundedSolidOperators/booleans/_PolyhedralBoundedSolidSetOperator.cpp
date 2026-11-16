//= References:                                                             =
//= [MANT1986] Mantyla Martti. "Boolean Operations of 2-Manifolds through   =
//=     Vertex Neighborhood Classification". ACM Transactions on Graphics,  =
//=     Vol. 5, No. 1, January 1986, pp. 1-29.                              =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

#include "java/lang/Exception.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/String.h"
#include "java/util/TreeMap.txx"
#include "java/util/TreeSet.txx"
#include <cstdio>

#include "java/lang/Double.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_SetOperationContext.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_SetOperationTrace.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetClassifier.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersector.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidIdNamespace.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/InvalidOperandException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/InvalidSolidResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SolidOperationFailedException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperandAligner.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef _PolyhedralBoundedSolidSetOperator SetOperator;

int SetOperator::debugFlags = 0;
SetOperator::DebugSolidExporter SetOperator::debugSolidExporter = 0;
_PolyhedralBoundedSolidIdNamespace* SetOperator::ownedNamespace = 0;
const char* const SetOperator::REJECTED_NON_MANIFOLD_PATH =
    "rejected-non-manifold-union";
thread_local java::String SetOperator::lastResultPath;
thread_local bool SetOperator::hasLastResultPath = false;
thread_local java::ArrayList<java::String> SetOperator::lastConnectInvariantViolations;
thread_local bool SetOperator::hasLastConnectInvariantViolations = false;

namespace {

const char* const CLASS_NAME = "_PolyhedralBoundedSolidSetOperator";

class HalfEdgePair {
public:
    _PolyhedralBoundedSolidHalfEdge* from;
    _PolyhedralBoundedSolidHalfEdge* to;

    HalfEdgePair() : from(nullptr), to(nullptr) {}

    HalfEdgePair(_PolyhedralBoundedSolidHalfEdge* from,
                 _PolyhedralBoundedSolidHalfEdge* to) : from(from), to(to) {}

    bool operator<(const HalfEdgePair& other) const
    {
        return java::naturalOrderLess(from, other.from) ||
            (from == other.from && java::naturalOrderLess(to, other.to));
    }

    bool operator==(const HalfEdgePair& other) const
    {
        return from == other.from && to == other.to;
    }
};

java::String str(long long value)
{
    return java::String::valueOf(value);
}

/** Java `"" + vector` (Vector3Dd.toString). */
java::String vectorText(const Vector3Dd& v)
{
    java::String* text = v.toString();
    java::String result(text->c_str());
    delete text;
    return result;
}

void tracePipelineSummary(const java::String& message)
{
    _SetOperationTrace::tracePipelineSummary(message.c_str());
}

bool isPipelineSummaryTraceEnabled()
{
    return _SetOperationTrace::isPipelineSummaryTraceEnabled();
}

/**
Emulates Java `Arrays.toString(double[])` for the six bounds values.
*/
java::String boundsText(const double* bounds)
{
    java::String text = "[";
    for ( int i = 0; i < 6; i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += java::Double::toString(bounds[i]).c_str();
    }
    return text + "]";
}

}

//= Configuration and helpers =======================================

void SetOperator::setDebugSolidExporter(DebugSolidExporter exporter)
{
    debugSolidExporter = exporter;
}

void SetOperator::updmaxnames(PolyhedralBoundedSolid* solidToUpdate,
    PolyhedralBoundedSolid* referenceSolid)
{
    _PolyhedralBoundedSolidIdNamespace::updmaxnames(solidToUpdate, referenceSolid);
}

int SetOperator::nextVertexId(PolyhedralBoundedSolid* current,
    PolyhedralBoundedSolid* other)
{
    return _PolyhedralBoundedSolidIdNamespace::nextVertexId(current, other,
        idNamespace);
}

/**
Initial vertex intersection detector for the set operations algorithm
(big phase 0). Following program [MANT1988].15.2. After generation,
coincident intersection vertices are welded in both solids and stale
entries are pruned from sonva/sonvb.
*/
void SetOperator::setOpGenerate(_SetOperationContext& ctx,
    PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB)
{
    _PolyhedralBoundedSolidSetIntersector::GenerationResult generation =
        _PolyhedralBoundedSolidSetIntersector::setOpGenerate(inSolidA, inSolidB);
    ctx.sonvv = generation.sonvv();
    ctx.sonva = generation.sonva();
    ctx.sonvb = generation.sonvb();

    weldIntersectionVertices(ctx, inSolidA, inSolidB);
}

/**
Post-Generate weld pass: collapses spatially coincident vertices introduced
during setOpGenerate in each solid, then removes from sonva/sonvb any entry
whose vertex was merged away by lkev.
*/
void SetOperator::weldIntersectionVertices(_SetOperationContext& ctx,
    PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB)
{
    int weldedA = PolyhedralBoundedSolidTopologyEditing::weldCoincidentVertices(
        inSolidA, numericContext);
    int weldedB = PolyhedralBoundedSolidTopologyEditing::weldCoincidentVertices(
        inSolidB, numericContext);

    if ( weldedA > 0 ) {
        Logger::reportMessage(java::String(), Logger::DEBUG,
            "weldIntersectionVertices",
            ("setOpGenerate weld: " + str(weldedA) +
             " vertex pair(s) collapsed in solidA").c_str());
        pruneStaleVertexFaceEntries(ctx.sonva, inSolidA);
    }
    if ( weldedB > 0 ) {
        Logger::reportMessage(java::String(), Logger::DEBUG,
            "weldIntersectionVertices",
            ("setOpGenerate weld: " + str(weldedB) +
             " vertex pair(s) collapsed in solidB").c_str());
        pruneStaleVertexFaceEntries(ctx.sonvb, inSolidB);
    }
}

/**
Removes entries from `list` whose vertex is no longer present in `solid`
(after lkev merges two vertices, the removed vertex is detached from the
solid's vertices list).
*/
void SetOperator::pruneStaleVertexFaceEntries(
    java::ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>& list,
    PolyhedralBoundedSolid* solid)
{
    java::TreeSet<_PolyhedralBoundedSolidVertex*> present;
    long int k;
    long int i;

    for ( k = 0; k < solid->getVerticesList().size(); k++ ) {
        present.add(solid->getVerticesList().get(k));
    }
    i = 0;
    while ( i < list.size() ) {
        if ( !present.contains(list[i].v) ) {
            list.remove(i);
        }
        else {
            i++;
        }
    }
}

Vector3Dd SetOperator::inside(_PolyhedralBoundedSolidHalfEdge* he)
{
    Vector3Dd middle;
    Vector3Dd a;
    Vector3Dd b;
    Vector3Dd n;

    a = (he->next())->startingVertex->position.subtract(he->startingVertex->position);
    b = (he->previous())->startingVertex->position.subtract(he->startingVertex->position);
    a = a.normalized();
    b = b.normalized();

    InfinitePlane* plane = he->parentLoop->parentFace->getContainingPlane();
    n = plane->getNormal();
    delete plane;

    middle = n.crossProduct(a);
    middle = middle.normalized();

    return middle;
}

/**
Checks if two coplanar sectors overlaps, following section [MANT1988].15.6.2.
*/
bool SetOperator::sectoroverlap(
    const _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex& na,
    const _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex& nb)
{
    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
        ::sectoroverlap(na, nb, (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0);
}

bool SetOperator::colinearVectorsWithDirection(const Vector3Dd& a,
    const Vector3Dd& b)
{
    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
        ::colinearVectorsWithDirection(a, b);
}

/**
Normalizes one endpoint for `separateEdgeSequence` when a previous null
strut edge was already inserted on the same vertex neighborhood.
*/
_PolyhedralBoundedSolidHalfEdge* SetOperator::recoverEdgeSequenceEndpointFromStrut(
    _PolyhedralBoundedSolidHalfEdge* endpoint, bool isFromEndpoint)
{
    _PolyhedralBoundedSolidHalfEdge* prev;

    if ( endpoint == 0 ) {
        return 0;
    }

    prev = endpoint->previous();
    if ( prev == 0 || prev->parentEdge == 0 ) {
        return endpoint;
    }

    if ( !nulledge(prev) || !strutnulledge(prev) ) {
        return endpoint;
    }

    if ( isFromEndpoint ) {
        if ( prev == prev->parentEdge->leftHalf ) {
            return prev->previous();
        }
    }
    else {
        if ( prev == prev->parentEdge->rightHalf ) {
            return prev->previous();
        }
    }
    return endpoint;
}

SetOperator::SeparateEdgeSequenceResult SetOperator::separateEdgeSequence(
    _PolyhedralBoundedSolidHalfEdge* from,
    _PolyhedralBoundedSolidHalfEdge* to,
    int type,
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB)
{
    bool trace = (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00;

    //-----------------------------------------------------------------
    if ( trace ) {
        printf("      SEPARATEEDGESEQUENCE %d\n", type);
        printf("        From: %s\n", from != 0 ? from->toString().c_str() : "null");
        printf("        To: %s\n", to != 0 ? to->toString().c_str() : "null");
    }

    if ( from == 0 || to == 0 ) {
        Logger::reportMessage(java::String(), Logger::WARNING,
            "separateEdgeSequence",
            "Unexpected case: null halfedges; skipping LMEV.");
        return FAILED_NULL_INPUT;
    }

    PolyhedralBoundedSolid* s = from->parentLoop->parentFace->parentSolid;

    if ( s != to->parentLoop->parentFace->parentSolid ) {
        Logger::reportMessage(java::String(), Logger::WARNING,
            "separateEdgeSequence",
            "Unexpected case: halfedges on different solids; skipping LMEV.");
        return FAILED_DIFFERENT_SOLIDS;
    }

    //-----------------------------------------------------------------
    // Recover from null edges already inserted. Cases A/B follow null-edge
    // struts inserted previously; cases C/D/E step backwards in the loop
    // until the two starts coincide. Each iteration must produce an unseen
    // (from, to) pair - repeating a pair proves divergence.
    java::TreeSet<HalfEdgePair> visitedConfigurations;
    bool changed;
    do {
        if ( !visitedConfigurations.add(HalfEdgePair(from, to)) ) {
            Logger::reportMessage(java::String(), Logger::WARNING,
                "separateEdgeSequence",
                "Cycle detected in endpoint recovery (cases A-E "
                "did not converge); skipping LMEV to keep B-rep valid.");
            return FAILED_CYCLE_DETECTED;
        }

        changed = false;

        _PolyhedralBoundedSolidHalfEdge* recoveredFrom =
            recoverEdgeSequenceEndpointFromStrut(from, true);
        if ( recoveredFrom != from ) {
            from = recoveredFrom;
            changed = true;
            if ( trace ) {
                printf("        Recovered edge sequence case A\n");
            }
        }

        _PolyhedralBoundedSolidHalfEdge* recoveredTo =
            recoverEdgeSequenceEndpointFromStrut(to, false);
        if ( recoveredTo != to ) {
            to = recoveredTo;
            changed = true;
            if ( trace ) {
                printf("        Recovered edge sequence case B\n");
            }
        }

        if ( from->startingVertex != to->startingVertex ) {
            _PolyhedralBoundedSolidHalfEdge* fromPrev = from->previous();
            _PolyhedralBoundedSolidHalfEdge* toPrev = to->previous();

            if ( fromPrev != 0 && toPrev != 0 &&
                 fromPrev->parentEdge != 0 && toPrev->parentEdge != 0 &&
                 fromPrev == toPrev->mirrorHalfEdge() ) {
                from = fromPrev;
                changed = true;
                if ( trace ) {
                    printf("        Recovered edge sequence case C\n");
                }
            }
            else if ( fromPrev != 0 &&
                      fromPrev->startingVertex == to->startingVertex ) {
                from = fromPrev;
                changed = true;
                if ( trace ) {
                    printf("        Recovered edge sequence case D\n");
                }
            }
            else if ( toPrev != 0 &&
                      toPrev->startingVertex == from->startingVertex ) {
                to = toPrev;
                changed = true;
                if ( trace ) {
                    printf("        Recovered edge sequence case E\n");
                }
            }
        }
    } while ( changed );

    if ( from->startingVertex != to->startingVertex ) {
        Logger::reportMessage(java::String(), Logger::WARNING,
            "separateEdgeSequence",
            "Unable to recover endpoint pairing after A-E normalization; "
            "skipping LMEV.");
        return FAILED_NO_PAIRING_REACHED;
    }

    //-----------------------------------------------------------------
    if ( trace && (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
        printf("       -> LMEV (Separate edge sequence):\n");
        printf("          . H1: %s\n", to->toString().c_str());
        printf("          . H2: %s\n", from->toString().c_str());
    }

    int id = nextVertexId(inSolidA, inSolidB);

    PolyhedralBoundedSolidEulerOperators::lmev(s, to, from, id,
        to->startingVertex->position);

    if ( trace && (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
        printf("          . New vertex: %d\n", id);
    }

    // The live pipeline records the created null edge from within
    // _PolyhedralBoundedSolidSetClassifier; this overload is retained only to
    // validate the cycle-detection failure modes (as in Java).
    return OK;
}

bool SetOperator::nulledge(_PolyhedralBoundedSolidHalfEdge* he)
{
    return PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
        he->startingVertex->position, he->next()->startingVertex->position,
        numericContext);
}

/**
Borrowed from [.wMANT2008].
*/
bool SetOperator::strutnulledge(_PolyhedralBoundedSolidHalfEdge* he)
{
    return he == he->mirrorHalfEdge()->next() ||
           he == he->mirrorHalfEdge()->previous();
}

/**
Main control algorithm for the big phases 1 and 2, following section
[MANT1988].16.6.1. and program [MANT1988].15.5.
*/
void SetOperator::setOpClassify(_SetOperationContext& ctx, int op,
    PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB)
{
    _PolyhedralBoundedSolidSetClassifier::runSetOpClassify(
        op, inSolidA, inSolidB, debugFlags, ctx);
}

/**
Diagnostic: scans a solid for boundary loops whose vertices are
coincident-but-distinct (self-touching / figure-8). Gated behind the
pipeline-summary trace property.
*/
void SetOperator::traceSelfTouchingLoops(PolyhedralBoundedSolid* solid,
    const char* label)
{
    if ( !isPipelineSummaryTraceEnabled() || solid == 0 ) {
        return;
    }
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext tol =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    long int fi;
    for ( fi = 0; fi < solid->getPolygonsList().size(); fi++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(fi);
        long int li;
        for ( li = 0; li < face->boundariesList.size(); li++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(li);
            long int sz = loop->halfEdgesList.size();
            long int a;
            for ( a = 0; a < sz; a++ ) {
                long int b;
                for ( b = a + 1; b < sz; b++ ) {
                    _PolyhedralBoundedSolidHalfEdge* ha = loop->halfEdgesList.get(a);
                    _PolyhedralBoundedSolidHalfEdge* hb = loop->halfEdgesList.get(b);
                    if ( ha->startingVertex->id != hb->startingVertex->id &&
                         PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                             ha->startingVertex->position,
                             hb->startingVertex->position, tol) ) {
                        long int gap = b - a;
                        bool adjacent = (gap == 1) || (gap == sz - 1);
                        printf("[SelfTouch] %s face=%d loop=%d size=%d "
                            "idx[%d]v%d==idx[%d]v%d%s at (%.4f,%.4f,%.4f)\n",
                            label, face->id, (int)li, (int)sz,
                            (int)a, ha->startingVertex->id,
                            (int)b, hb->startingVertex->id,
                            adjacent ? " ADJACENT(zero-len-edge)" :
                                " NON-ADJACENT(pinch)",
                            ha->startingVertex->position.x(),
                            ha->startingVertex->position.y(),
                            ha->startingVertex->position.z());
                    }
                }
            }
        }
    }
}

/**
Splits every boundary loop that is self-touching (pinched) into simple loops
via `lmef`. Run on each operand right after setOpGenerate and before
setOpClassify; size-2 strut loops are excluded.
*/
void SetOperator::splitSelfTouchingLoops(PolyhedralBoundedSolid* solid)
{
    if ( solid == 0 ) {
        return;
    }
    bool tracing = isPipelineSummaryTraceEnabled();
    int splitsFired = 0;
    long int fi = 0;
    while ( fi < solid->getPolygonsList().size() ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(fi);
        bool splitDone = false;
        long int li = 0;
        while ( li < face->boundariesList.size() && !splitDone ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(li);
            long int sz = loop->halfEdgesList.size();
            if ( sz <= 2 ) {
                li++;
                continue;
            }
            long int a;
            for ( a = 0; a < sz && !splitDone; a++ ) {
                long int b;
                for ( b = a + 2; b < sz; b++ ) {
                    if ( a == 0 && b == sz - 1 ) {
                        continue; // adjacent via wrap-around
                    }
                    _PolyhedralBoundedSolidHalfEdge* ha = loop->halfEdgesList.get(a);
                    _PolyhedralBoundedSolidHalfEdge* hb = loop->halfEdgesList.get(b);
                    if ( ha->startingVertex->id != hb->startingVertex->id &&
                         PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                             ha->startingVertex->position,
                             hb->startingVertex->position,
                             numericContext) ) {
                        int newId = (idNamespace != 0) ?
                            idNamespace->nextFaceId(solid) :
                            solid->getMaxFaceId() + 1;
                        _PolyhedralBoundedSolidFace* newFace =
                            PolyhedralBoundedSolidEulerOperators::lmef(
                                solid, ha, hb, newId);
                        if ( newFace != 0 ) {
                            splitsFired++;
                            if ( tracing ) {
                                tracePipelineSummary(
                                    "splitSelfTouchingLoops #" + str(splitsFired) +
                                    " face=" + str(face->id) + " loop=" + str(li) +
                                    " sz=" + str(sz) +
                                    " idx[" + str(a) + "]v" + str(ha->startingVertex->id) +
                                    "==idx[" + str(b) + "]v" + str(hb->startingVertex->id) +
                                    " -> newFace=" + str(newFace->id));
                            }
                            splitDone = true;
                        }
                        break;
                    }
                }
            }
            if ( !splitDone ) {
                li++;
            }
        }
        if ( !splitDone ) {
            fi++;
        }
        // If splitDone: stay at the same fi so the modified face is
        // re-examined for any remaining pinches.
    }
    if ( tracing && splitsFired > 0 ) {
        tracePipelineSummary("splitSelfTouchingLoops total=" + str(splitsFired) +
            " faces-after=" + str(solid->getPolygonsList().size()));
    }
}

void SetOperator::setOpConnect(_SetOperationContext& ctx, int op)
{
    _PolyhedralBoundedSolidSetNullEdgesConnector connector;
    _PolyhedralBoundedSolidSetNullEdgesConnector::ConnectResult result =
        connector.connect(op, debugFlags, ctx.sonea, ctx.soneb);
    ctx.sonfa = result.sonfa();
    ctx.sonfb = result.sonfb();
    ctx.connected = result;
}

/**
Answer integrator for the set operations algorithm (big phase 4).
Following program [MANT1988].15.15.
*/
void SetOperator::setOpFinish(_SetOperationContext& ctx,
    PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB,
    PolyhedralBoundedSolid* outRes, int op)
{
    _PolyhedralBoundedSolidSetFinisher::finish(
        inSolidA, inSolidB, outRes, op, debugFlags, ctx.sonfa, ctx.sonfb,
        ctx.connected);
}

void SetOperator::weldCoincidentVerticesOfOperand(
    PolyhedralBoundedSolid* solid, const char* name)
{
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    java::String ignored;

    if ( PolyhedralBoundedSolidGeometricValidator
            ::validateNoCoincidentVertices(solid, context, &ignored) ) {
        return;
    }
    int welded = PolyhedralBoundedSolidTopologyEditing
        ::weldCoincidentVertices(solid, context);
    tracePipelineSummary(java::String("setOp operand ") + name +
        " had coincident vertices; welded " + str(welded) + " pair(s)");
}

PolyhedralBoundedSolid* SetOperator::setOpNoIntersectionCase(
    PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB,
    PolyhedralBoundedSolid* outRes, int op)
{
    try {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            ::runSetOpNoIntersectionCase(inSolidA, inSolidB, outRes, op);
    }
    catch ( const NonManifoldBooleanResultException& e ) {
        setLastResultPath(REJECTED_NON_MANIFOLD_PATH);
        throw;
    }
}

PolyhedralBoundedSolid* SetOperator::setOpNoIntersectionCase(
    PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB,
    PolyhedralBoundedSolid* outRes, int op, PreflightCache& cache)
{
    try {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            ::runSetOpNoIntersectionCase(inSolidA, inSolidB, outRes, op,
                cache);
    }
    catch ( const NonManifoldBooleanResultException& e ) {
        setLastResultPath(REJECTED_NON_MANIFOLD_PATH);
        throw;
    }
}

void SetOperator::debugSolid(PolyhedralBoundedSolid* solid, const char* pattern)
{
    printf("**** DEBUGGING SOLID INFORMATION WRITEN TO FILES %s ****\n", pattern);
    java::String fileName = java::String(pattern) + ".txt";
    FILE* fd = fopen(fileName.c_str(), "w");
    if ( fd == 0 ) {
        return;
    }
    if ( debugSolidExporter != 0 ) {
        debugSolidExporter(solid, pattern);
    }
    fprintf(fd, "%s\n", solid->toString().c_str());
    fclose(fd);
}

/**
Following program [MANT1988].15.1.
*/
void SetOperator::postProcessResult(PolyhedralBoundedSolid* res,
    bool maximizeResultFaces)
{
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(res);
    PolyhedralBoundedSolidTopologyEditing::compactIds(res);
    if ( maximizeResultFaces ) {
        PolyhedralBoundedSolidTopologyEditing::maximizeFaces(res);
        _PolyhedralBoundedSolidSetFinisher::triangulateNonPlanarFaces(res);
        PolyhedralBoundedSolidTopologyEditing::compactIds(res);
    }
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(res);
}

void SetOperator::setLastResultPath(const char* path)
{
    if ( path == nullptr ) {
        hasLastResultPath = false;
        lastResultPath = java::String();
        return;
    }
    hasLastResultPath = true;
    lastResultPath = path;
}

/**
Name of the path that produced the last setOp result on the calling
thread: "normal-pipeline" for the [MANT1988] Ch. 15 pipeline, or the name
of the preflight that answered instead. Recorded before the strict
postcondition runs, so it is also available after a strict validation
failure. Fable5.2Analysis.md Stage A step 4.
@return null when no path was recorded
*/
const java::String* SetOperator::getLastResultPath()
{
    return hasLastResultPath ? &lastResultPath : nullptr;
}

/**
Violations of the post-connect invariant found on the last setOp of the
calling thread that reached connect (empty list: none; null: connect was
not reached). See `checkConnectInvariant`.
*/
const java::ArrayList<java::String>* SetOperator::getLastConnectInvariantViolations()
{
    return hasLastConnectInvariantViolations ?
        &lastConnectInvariantViolations : nullptr;
}

/**
Post-connect invariant ([MANT1988] Sec. 14.7 and 15.7; Fable5.2Analysis.md
Stage C step 1), checked when connect left no loose ends:
- every null face recorded for finish (sonfa, sonfb) has exactly two loops,
  the two copies of its section curve;
- no other face carries two loops that coincide vertex by vertex. A section
  ring strictly inside a face must leave that face with the ring as a hole
  and the disk as a separate face; two coincident loops in one face mean
  the hole was moved into the disk (B-01).
*/
java::ArrayList<java::String> SetOperator::checkConnectInvariant(
    PolyhedralBoundedSolid* inSolidA, PolyhedralBoundedSolid* inSolidB,
    _SetOperationContext& ctx)
{
    java::ArrayList<java::String> violations;

    if ( hasIncompleteConnectState() ) {
        return violations;
    }
    collectNullFaceViolations("A", ctx.sonfa, violations);
    collectNullFaceViolations("B", ctx.sonfb, violations);
    collectCoincidentLoopViolations("A", inSolidA, ctx.sonfa, violations);
    collectCoincidentLoopViolations("B", inSolidB, ctx.sonfb, violations);
    return violations;
}

void SetOperator::collectNullFaceViolations(const char* label,
    const java::ArrayList<_PolyhedralBoundedSolidFace*>& nullFaces,
    java::ArrayList<java::String>& violations)
{
    int i;

    for ( i = 0; i < nullFaces.size(); i++ ) {
        _PolyhedralBoundedSolidFace* f = nullFaces.get(i);
        // A null face collapsed to a single point (only zero-length
        // strut rings, left where the section curve runs straight
        // through a vertex of one face) bounds no section and is
        // discarded by finish; it is not a section-curve face.
        if ( f->boundariesList.size() != 2 && !isPointLikeFace(f) ) {
            violations.add(java::String(label) + ": null face " +
                str(f->id) + " has " + str(f->boundariesList.size()) +
                " loops instead of 2");
        }
    }
}

bool SetOperator::isPointLikeFace(_PolyhedralBoundedSolidFace* f)
{
    Vector3Dd first;
    bool hasFirst = false;
    int i;

    for ( i = 0; i < f->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* start =
            f->boundariesList.get(i)->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        if ( start == nullptr ) {
            continue;
        }
        do {
            Vector3Dd p = he->startingVertex->position;
            if ( !hasFirst ) {
                first = p;
                hasFirst = true;
            }
            else if ( Vector3Dd::distance(first, p) >
                      numericContext.bigEpsilon() ) {
                return false;
            }
            he = he->next();
        } while ( he != start );
    }
    return true;
}

void SetOperator::collectCoincidentLoopViolations(const char* label,
    PolyhedralBoundedSolid* solid,
    const java::ArrayList<_PolyhedralBoundedSolidFace*>& nullFaces,
    java::ArrayList<java::String>& violations)
{
    int i;
    int j;
    int k;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* f = solid->getPolygonsList().get(i);
        if ( f->boundariesList.size() < 2 || nullFaces.contains(f) ) {
            continue;
        }
        for ( j = 0; j < f->boundariesList.size(); j++ ) {
            for ( k = j + 1; k < f->boundariesList.size(); k++ ) {
                if ( loopsCoincide(f->boundariesList.get(j),
                         f->boundariesList.get(k)) ) {
                    violations.add(java::String(label) + ": face " +
                        str(f->id) + " has coincident loops " + str(j) +
                        " and " + str(k));
                }
            }
        }
    }
}

bool SetOperator::loopsCoincide(_PolyhedralBoundedSolidLoop* a,
    _PolyhedralBoundedSolidLoop* b)
{
    int i;
    int j;

    if ( a->halfEdgesList.size() != b->halfEdgesList.size() ||
         a->halfEdgesList.size() < 3 ) {
        return false;
    }
    for ( i = 0; i < a->halfEdgesList.size(); i++ ) {
        Vector3Dd p = a->halfEdgesList.get(i)->startingVertex->position;
        bool found = false;
        for ( j = 0; j < b->halfEdgesList.size() && !found; j++ ) {
            found = PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                p, b->halfEdgesList.get(j)->startingVertex->position,
                numericContext);
        }
        if ( !found ) {
            return false;
        }
    }
    return true;
}

/**
Detects result pieces that touch only along an edge or at a vertex: two
distinct vertices at the same point, or two distinct edges joining the
same two points. A manifold boundary has neither.
@return false (Java null) when there is no contact; otherwise true, with a
short description of the first contact found in `description`
*/
bool SetOperator::findNonManifoldContact(PolyhedralBoundedSolid* result,
    java::String* description)
{
    double tolerance = numericContext.bigEpsilon();
    int nv = result->getVerticesList().size();
    int ne = result->getEdgesList().size();
    int i;
    int j;

    for ( i = 0; i < nv; i++ ) {
        Vector3Dd p = result->getVerticesList().get(i)->position;
        for ( j = i + 1; j < nv; j++ ) {
            if ( Vector3Dd::distance(p,
                     result->getVerticesList().get(j)->position) <=
                 tolerance ) {
                *description = java::String("vertices ") +
                    str(result->getVerticesList().get(i)->id) + " and " +
                    str(result->getVerticesList().get(j)->id) +
                    " coincide at " + vectorText(p);
                return true;
            }
        }
    }
    for ( i = 0; i < ne; i++ ) {
        _PolyhedralBoundedSolidEdge* e = result->getEdgesList().get(i);
        for ( j = i + 1; j < ne; j++ ) {
            _PolyhedralBoundedSolidEdge* f = result->getEdgesList().get(j);
            if ( sameEndpoints(e, f) ) {
                *description = java::String("edges ") + str(e->id) +
                    " and " + str(f->id) + " join the same points " +
                    vectorText(e->rightHalf->startingVertex->position) +
                    " - " +
                    vectorText(e->leftHalf->startingVertex->position);
                return true;
            }
        }
    }
    return false;
}

bool SetOperator::sameEndpoints(_PolyhedralBoundedSolidEdge* e,
    _PolyhedralBoundedSolidEdge* f)
{
    _PolyhedralBoundedSolidVertex* e1 = e->rightHalf->startingVertex;
    _PolyhedralBoundedSolidVertex* e2 = e->leftHalf->startingVertex;
    _PolyhedralBoundedSolidVertex* f1 = f->rightHalf->startingVertex;
    _PolyhedralBoundedSolidVertex* f2 = f->leftHalf->startingVertex;

    return (e1 == f1 && e2 == f2) || (e1 == f2 && e2 == f1);
}

/**
C++ port note: on every exception thrown here the result is deleted (Java
leaves it to the garbage collector); the caller has already unlisted its
nodes from the operands.
*/
PolyhedralBoundedSolid* SetOperator::completeSetOpResult(
    PolyhedralBoundedSolid* result,
    const java::String& operandADiagnostics,
    const java::String& operandBDiagnostics,
    int op,
    const char* resultPath,
    bool maximizeResultFaces,
    bool doStrictValidation)
{
    setLastResultPath(resultPath);
    if ( result == 0 ) {
        throw SolidOperationFailedException(java::String("Boolean operation returned null: op=") +
            operationName(op).c_str() + ", path=" + resultPath);
    }
    if ( result->getPolygonsList().size() > 0 ) {
        postProcessResult(result, maximizeResultFaces);
    }
    java::String contact;
    if ( findNonManifoldContact(result, &contact) ) {
        delete result;
        // Same policy as B-04: pieces of the result that touch only
        // along an edge or at a vertex form a non-manifold solid.
        throw NonManifoldBooleanResultException(
            operationName(op) + " result is non-manifold (" + contact +
            "); rejected (path=" + resultPath + ")");
    }
    if ( doStrictValidation ) {
        java::String strictMessage;
        if ( !PolyhedralBoundedSolidValidationEngine::validateStrict(
                 result, &strictMessage) ) {
            PolyhedralBoundedSolidTopologySummary topology =
                PolyhedralBoundedSolidTopologySummary::from(result);
            java::String message =
                java::String("Strict boolean result validation failed: op=") +
                operationName(op).c_str() + ", path=" + resultPath +
                ", operandA=" + operandADiagnostics.c_str() +
                ", operandB=" + operandBDiagnostics.c_str() +
                ", result=" + cardinalitiesAndBounds(result).c_str() +
                ", " + topology.toString().c_str() + "\n" +
                strictMessage.c_str();
            delete result;
            throw InvalidSolidResultException(message);
        }
    }
    return result;
}

java::String SetOperator::operationName(int op)
{
    if ( op == UNION ) {
        return "UNION";
    }
    if ( op == INTERSECTION ) {
        return "INTERSECTION";
    }
    if ( op == SUBTRACT ) {
        return "SUBTRACT";
    }
    return ("UNKNOWN(" + str(op) + ")").c_str();
}

java::String SetOperator::cardinalitiesAndBounds(PolyhedralBoundedSolid* solid)
{
    if ( solid == 0 ) {
        return "null";
    }
    double* bounds = solid->getMinMax();
    java::String text = "{faces=" + str(solid->getPolygonsList().size()) +
        ", edges=" + str(solid->getEdgesList().size()) +
        ", vertices=" + str(solid->getVerticesList().size()) +
        ", bounds=" + boundsText(bounds) + "}";
    delete[] bounds;
    return text.c_str();
}

/**
Independent copy of `solid` for the recovery paths. It used to be a Java
serialization round trip, which recursed over the linked structure and
could overflow the stack on large solids; it is now the linear structural
copy of [MANT1988] Sec. 15.8 (`scopy`, Stage F). The label is kept for the
callers' diagnostics.
@return a new solid owned by the caller
*/
PolyhedralBoundedSolid* SetOperator::deepCloneSolid(PolyhedralBoundedSolid* solid,
    const char* solidLabel)
{
    (void)solidLabel;
    return PolyhedralBoundedSolidStructuralCopy::copy(solid);
}

bool SetOperator::hasIncompleteConnectState()
{
    return _PolyhedralBoundedSolidSetNullEdgesConnector::getLastLooseACount() > 0 ||
        _PolyhedralBoundedSolidSetNullEdgesConnector::getLastLooseBCount() > 0;
}

void SetOperator::unlistSharedNodes(PolyhedralBoundedSolid* from,
    PolyhedralBoundedSolid* reference)
{
    if ( from == 0 || reference == 0 || from == reference ) {
        return;
    }
    from->unlistNodesOwnedBy(reference);
}

void SetOperator::releaseCopy(PolyhedralBoundedSolid* copy,
    PolyhedralBoundedSolid* result)
{
    unlistSharedNodes(copy, result);
    delete copy;
}

//= Main algorithm ==================================================

/**
Runs a boolean operation following program [MANT1988].15.1, with
configurable strict B-Rep validation as a final result postcondition.
*/
PolyhedralBoundedSolid* SetOperator::setOp(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    int op,
    bool withDebug,
    bool maximizeResultFaces,
    bool doStrictValidation)
{
    setLastResultPath(nullptr);
    hasLastConnectInvariantViolations = false;
    lastConnectInvariantViolations.clear();
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(inSolidA, inSolidB);
    setNumericContext(&context);
    _PolyhedralBoundedSolidSetOperatorNullEdge::setNumericContext(&numericContext);

    if ( withDebug ) {
        debugFlags = 0
          | DEBUG_01_STRUCTURE
          | DEBUG_02_GENERATOR
          | DEBUG_03_VERTEXFACECLASIFFIER
          | DEBUG_04_VERTEXVERTEXCLASIFFIER
          | DEBUG_05_CONNECT
          | DEBUG_06_FINISH
          | DEBUG_99_SHOWOPERATIONS
          ;
    }
    else {
        debugFlags = 0;
    }

    if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
        printf("= [START OF SETOP REPORT] =================================================================================================================================\n");
        printf("Dumping debug log for _PolyhedralBoundedSolidSetOperator.setOp.\n");
        printf("The algorithm structure is:\n");
        printf("  0. Calculate vertex/face and vertex/vertex crossings.\n");
        printf("  1. Classify and split for vertex/face cases.\n");
        printf("  2. Classify and split for vertex/vertex cases.\n");
        printf("  3. Connect.\n");
        printf("  4. Finish.\n");
    }

    //-----------------------------------------------------------------
    PolyhedralBoundedSolid* res = new PolyhedralBoundedSolid();
    java::String resultPath;
    java::String operandADiagnostics;
    java::String operandBDiagnostics;

    _SetOperationContext ctx;
    resultPath = "normal-pipeline";
    operandADiagnostics = cardinalitiesAndBounds(inSolidA);
    operandBDiagnostics = cardinalitiesAndBounds(inSolidB);

    //-----------------------------------------------------------------
    if ( withDebug ) {
        debugSolid(inSolidA, "outputA_stage00");
        debugSolid(inSolidB, "outputB_stage00");
    }

    PolyhedralBoundedSolidTopologyEditing::compactIds(inSolidA);
    PolyhedralBoundedSolidTopologyEditing::compactIds(inSolidB);
    PolyhedralBoundedSolidTopologyEditing::maximizeFaces(inSolidA);
    PolyhedralBoundedSolidTopologyEditing::maximizeFaces(inSolidB);
    PolyhedralBoundedSolidTopologyEditing::compactIds(inSolidA);
    PolyhedralBoundedSolidTopologyEditing::compactIds(inSolidB);
    // Operand preparation, on the copies this operation owns: edges
    // shorter than the tolerance are collapsed, so that generate does
    // not meet two vertices at one point. Validation, below, only
    // reads (plan-stage11 Phase C).
    weldCoincidentVerticesOfOperand(inSolidA, "A");
    weldCoincidentVerticesOfOperand(inSolidB, "B");
    // Faces of B inside the tolerance band of a parallel face of A are
    // put on its plane, so that every stage below takes the same
    // decision about them (see the aligner).
    Vector3Dd* alignment =
        _PolyhedralBoundedSolidSetOperandAligner::align(inSolidA, inSolidB);
    if ( alignment != nullptr ) {
        tracePipelineSummary(java::String("setOp operand B moved by ") +
            vectorText(*alignment) +
            " to align faces inside the tolerance band");
        delete alignment;
    }
    java::String booleanInputMsg;
    bool validInputs = PolyhedralBoundedSolidValidationEngine
        ::validateBooleanInputs(inSolidA, inSolidB, &booleanInputMsg);
    if ( !validInputs ) {
        delete res;
        throw InvalidOperandException(
            java::String("Boolean operands are not valid solids:\n") +
            booleanInputMsg);
    }
    if ( booleanInputMsg.length() > 0 ) {
        Logger::reportMessage(java::String(), Logger::DEBUG, "setOp",
            java::String("Boolean input pre-processing:\n") + booleanInputMsg);
    }
    PolyhedralBoundedSolidTopologyEditing::compactIds(inSolidA);
    PolyhedralBoundedSolidTopologyEditing::compactIds(inSolidB);
    updmaxnames(inSolidB, inSolidA);
    // As Java, the namespace stays active after this operation (until the
    // next setOp replaces it); the operator owns it
    _PolyhedralBoundedSolidIdNamespace* previousNamespace = ownedNamespace;
    ownedNamespace = new _PolyhedralBoundedSolidIdNamespace(inSolidA, inSolidB);
    setIdNamespace(ownedNamespace);
    delete previousNamespace;
    context = PolyhedralBoundedSolidNumericPolicy::forSolids(inSolidA, inSolidB);
    setNumericContext(&context);
    _PolyhedralBoundedSolidSetOperatorNullEdge::setNumericContext(&numericContext);

    if ( withDebug ) {
        debugSolid(inSolidA, "outputA_stage01");
        debugSolid(inSolidB, "outputB_stage01");
    }

    // Per-setOp memo shared across all "no real intersection" preflights:
    // operands are not mutated until setOpGenerate runs (below), so the
    // heavy classification/overlap/edge-face predicates are computed once
    // and reused.
    PreflightCache preflightCache =
        _PolyhedralBoundedSolidSetNonIntersectingClassifier
            ::newPreflightCache(inSolidA, inSolidB);
    bool preflightResolved = false;

    // A union of solids that touch along a face is one solid, glued
    // along the AonB- faces ([MANT1988] Eq. 15.2): leave it to the
    // pipeline. Edge or vertex contact would leave two shells sharing
    // a boundary point: rejected as non-manifold (B-04).
    bool unionGluesAlongAFace = op == UNION &&
        _PolyhedralBoundedSolidSetNonIntersectingClassifier
            ::hasOppositeCoplanarFaceAreaContact(inSolidA, inSolidB);
    if ( !unionGluesAlongAFace &&
         _PolyhedralBoundedSolidSetNonIntersectingClassifier
             ::runTouchingOnlyPreflightCase(inSolidA, inSolidB, preflightCache) ) {
        try {
            res = setOpNoIntersectionCase(inSolidA, inSolidB, res, op,
                preflightCache);
        }
        catch ( ... ) {
            // C++ ownership: the operands keep their nodes, the partial
            // result is released (Java: garbage collector)
            inSolidA->unlistNodesOwnedBy(res);
            inSolidB->unlistNodesOwnedBy(res);
            delete res;
            throw;
        }
        resultPath = "touching-only-preflight";
        preflightResolved = true;
    }

    // Section 7.3.1.A - Degenerate identity preflight: when A and B are
    // geometrically identical, the classifier marks every face of both as
    // "inside the other" and the regular pipeline collapses to empty,
    // breaking A U A and A ^ A. Dispatch directly per set-theoretic
    // identity. MUST run before the containment preflight.
    if ( !preflightResolved &&
         PolyhedralBoundedSolidValidationEngine::areGeometricallyIdentical(
             inSolidA, inSolidB, numericContext.bigEpsilon()) ) {
        tracePipelineSummary("setOp identity-preflight A=B op=" + str(op));
        if ( op == UNION || op == INTERSECTION ) {
            PolyhedralBoundedSolid* clone =
                deepCloneSolid(inSolidA, "identity-preflight clone");
            if ( clone != 0 ) {
                delete res;
                res = clone;
            }
        }
        // SUBTRACT case: A - A = empty; res remains the empty solid
        // already initialised at function entry.
        resultPath = "geometric-identity-preflight";
        preflightResolved = true;
    }

    // Section 7.3.1.D - Containment-only preflight: when one solid is
    // contained inside the other without real edge/face intersections,
    // the regular pipeline produces empty. Dispatch to a dedicated
    // containment table per [MANT1988] chapter 15.1.
    // A solid inside the other and touching its boundary is not a plain
    // containment for the difference: A - B is then A with a pocket open
    // to the outside, not A with a cavity, and only the pipeline builds it.
    if ( !preflightResolved &&
         _PolyhedralBoundedSolidSetNonIntersectingClassifier
             ::runContainmentOnlyPreflightCase(inSolidA, inSolidB,
                 preflightCache) &&
         !(op == SUBTRACT &&
           _PolyhedralBoundedSolidSetNonIntersectingClassifier
               ::touchesBoundaryFromInside(inSolidB, inSolidA)) ) {
        try {
            res = setOpNoIntersectionCase(inSolidA, inSolidB, res, op,
                preflightCache);
        }
        catch ( ... ) {
            // C++ ownership: the operands keep their nodes, the partial
            // result is released (Java: garbage collector)
            inSolidA->unlistNodesOwnedBy(res);
            inSolidB->unlistNodesOwnedBy(res);
            delete res;
            throw;
        }
        resultPath = "containment-only-preflight";
        preflightResolved = true;
    }

    if ( !preflightResolved ) {
        // Generate and classify edit the operands (vertices on edges and
        // faces, null edges). When they find no intersection the answer is
        // one of the operands, or their contact: it is taken from these
        // copies, made before any edit (plan-stage11 Phase E). Taken from
        // the edited operands it carried their extra vertices, and failed
        // strict validation for contacts inside the tolerance band.
        PolyhedralBoundedSolid* untouchedA =
            PolyhedralBoundedSolidStructuralCopy::copy(inSolidA);
        PolyhedralBoundedSolid* untouchedB =
            PolyhedralBoundedSolidStructuralCopy::copy(inSolidB);

        setOpGenerate(ctx, inSolidA, inSolidB);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage02");
            debugSolid(inSolidB, "outputB_stage02");
        }
        traceSelfTouchingLoops(inSolidA, "A-after-generate");
        traceSelfTouchingLoops(inSolidB, "B-after-generate");
        splitSelfTouchingLoops(inSolidA);
        splitSelfTouchingLoops(inSolidB);

        setOpClassify(ctx, op, inSolidA, inSolidB);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage03");
            debugSolid(inSolidB, "outputB_stage03");
        }
        // NOTE: after Classify the algorithm has (by design) inserted
        // null-edge struts (size-2 loops with coincident endpoints);
        // genuine self-touch is only meaningful after Generate.

        // [MANT1988] Sec. 15.4: what matters is whether classification
        // produced null edges. Vertex/vertex contacts that yield none (a
        // solid tangent inside the other along whole faces, for instance)
        // leave nothing to connect, so the result follows from containment
        // as in the no-intersection case; running connect and finish on
        // them returned an empty solid.
        if ( ctx.sonea.isEmpty() && ctx.soneb.isEmpty() ) {
            // No intersections found
            try {
                res = setOpNoIntersectionCase(untouchedA, untouchedB, res,
                    op);
            }
            catch ( ... ) {
                // C++ ownership: as below, then the partial result is
                // released (Java: garbage collector)
                releaseCopy(untouchedA, res);
                releaseCopy(untouchedB, res);
                inSolidA->unlistNodesOwnedBy(res);
                inSolidB->unlistNodesOwnedBy(res);
                delete res;
                throw;
            }
            resultPath = "no-intersection-after-classify";
            preflightResolved = true;
        }
        // C++ ownership: the copies give the result the nodes it took
        // from them and are released (Java leaves them to the collector)
        releaseCopy(untouchedA, res);
        releaseCopy(untouchedB, res);
    }

    if ( !preflightResolved ) {
        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage04");
            debugSolid(inSolidB, "outputB_stage04");
        }

        setOpConnect(ctx, op);
        lastConnectInvariantViolations =
            checkConnectInvariant(inSolidA, inSolidB, ctx);
        hasLastConnectInvariantViolations = true;
        // [MANT1988] Program 15.14: connect ends with every null edge
        // joined. Loose ends mean that part of the intersection curve was
        // not cut: a crossing was missed or misclassified, or two ends that
        // belong together sat in different faces. The null faces found so
        // far then describe only part of the result. Finishing with them
        // returned a solid with a piece missing and no error (plan-stage11
        // Phase B, near-degenerate fuzz), or a solid with coincident,
        // unjoined vertices that the non-manifold check rejected for the
        // wrong reason.
        int looseA = _PolyhedralBoundedSolidSetNullEdgesConnector
            ::getLastLooseACount();
        int looseB = _PolyhedralBoundedSolidSetNullEdgesConnector
            ::getLastLooseBCount();
        const _PolyhedralBoundedSolidSetIntersectionCurveBuilder::Report*
            curveReport = _PolyhedralBoundedSolidSetNullEdgesConnector
                ::getLastCurveReport();
        if ( looseA > 0 || looseB > 0 ) {
            setLastResultPath("normal-pipeline");
            delete res;
            throw SolidOperationFailedException(
                java::String("Boolean operation could not close the intersection curve: ") +
                "op=" + operationName(op) + ", " + str(looseA) +
                " loose null edge end(s) on A and " + str(looseB) + " on B " +
                "after connect (" +
                (curveReport != nullptr ? curveReport->summarize() :
                 java::String("null")) + ")");
        }

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage05");
            debugSolid(inSolidB, "outputB_stage05");
        }

        // [MANT1988] Sec. 15.8: finish builds the result from the null
        // faces. Every answer comes from this pipeline; there is no
        // shape-specific fallback behind it.
        setOpFinish(ctx, inSolidA, inSolidB, res, op);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage06");
            debugSolid(inSolidB, "outputB_stage06");
            debugSolid(res, "outputR_stage06");
        }
    }

    // C++ ownership: leave the result as the only owner of its nodes
    unlistSharedNodes(inSolidA, res);
    unlistSharedNodes(inSolidB, res);

    res = completeSetOpResult(res, operandADiagnostics, operandBDiagnostics,
        op, resultPath.c_str(), maximizeResultFaces, doStrictValidation);

    if ( withDebug ) {
        debugSolid(res, "outputR_stage07");
    }

    if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
        printf("= [END OF SETOP REPORT] ===================================================================================================================================\n");
    }

    return res;
}
