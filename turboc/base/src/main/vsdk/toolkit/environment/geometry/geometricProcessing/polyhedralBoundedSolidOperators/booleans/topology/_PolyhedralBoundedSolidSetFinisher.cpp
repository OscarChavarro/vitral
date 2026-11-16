//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include <math.h>
#include <stdio.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

#include "java/lang/Boolean.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidIdNamespace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "java/util/TreeSet.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SolidOperationFailedException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"

typedef _PolyhedralBoundedSolidSetFinisher Finisher;
typedef PolyhedralBoundedSolidNumericPolicy::ToleranceContext ToleranceContext;


static const char* const TRACE_PIPELINE_SUMMARY_PROPERTY =
    "vsdk.setop.tracePipelineSummary";

static String str(int value)
{
    return Cpp98Support::toString(value);
}


// C++ port note: Java keeps this counter per thread (ThreadLocal)
int Finisher::lastTriangulatedFaceCount = 0;

_PolyhedralBoundedSolidSetFinisher::_PolyhedralBoundedSolidSetFinisher()
{
}

int Finisher::getLastTriangulatedFaceCount()
{
    return lastTriangulatedFaceCount;
}

BOOLEAN Finisher::isPipelineSummaryTraceEnabled()
{
    return Boolean::getBoolean(TRACE_PIPELINE_SUMMARY_PROPERTY);
}

void Finisher::tracePipelineSummary(const String& message)
{
    if ( !isPipelineSummaryTraceEnabled() ) {
        return;
    }
    printf("[SetOpPipelineTrace] %s\n", message.c_str());
}

BOOLEAN Finisher::hasUsableIntegrationRing(_PolyhedralBoundedSolidFace* face)
{
    _PolyhedralBoundedSolidLoop* ring;
    _PolyhedralBoundedSolidHalfEdge* start;
    _PolyhedralBoundedSolidHalfEdge* current;
    Vector3Dd reference;
    long int guard;

    if ( face == 0 ||
         !hasCompleteHalfEdgeConnectivity(face) ||
         face->boundariesList.size() < 2 ||
         face->boundariesList.get(1) == 0 ) {
        return FALSE;
    }
    ring = face->boundariesList.get(1);
    if ( ring->halfEdgesList.size() < 2 ) {
        return FALSE;
    }
    start = ring->boundaryStartHalfEdge;
    if ( start == 0 || start->startingVertex == 0 ) {
        return FALSE;
    }
    reference = start->startingVertex->position;
    ToleranceContext context = PolyhedralBoundedSolidNumericPolicy::forFace(face);
    current = start->next();
    guard = 0;
    while ( current != 0 && current != start &&
            guard <= ring->halfEdgesList.size() ) {
        if ( current->startingVertex != 0 &&
             !PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                 reference, current->startingVertex->position, context) ) {
            return TRUE;
        }
        current = current->next();
        guard++;
    }
    return FALSE;
}

BOOLEAN Finisher::hasCompleteHalfEdgeConnectivity(_PolyhedralBoundedSolidFace* face)
{
    long int i;

    if ( face == 0 ) {
        return FALSE;
    }
    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start;
        _PolyhedralBoundedSolidHalfEdge* current;
        long int guard;

        if ( loop == 0 || loop->boundaryStartHalfEdge == 0 ) {
            return FALSE;
        }
        start = loop->boundaryStartHalfEdge;
        current = start;
        guard = 0;
        do {
            if ( current == 0 ||
                 current->parentEdge == 0 ||
                 current->parentLoop == 0 ||
                 current->startingVertex == 0 ||
                 current->mirrorHalfEdge() == 0 ||
                 current->next() == 0 ||
                 current->previous() == 0 ) {
                return FALSE;
            }
            current = current->next();
            guard++;
        } while ( current != start &&
                  guard <= loop->halfEdgesList.size() + 1 );
        if ( current != start ) {
            return FALSE;
        }
    }
    return TRUE;
}

String Finisher::integrationRingSummary(_PolyhedralBoundedSolidFace* face,
    const ConnectResult& pairs)
{
    String text;

    if ( face == 0 ) {
        return "null";
    }
    if ( face->boundariesList.size() < 2 ||
         face->boundariesList.get(1) == 0 ) {
        text = "face=" + str(face->id) + " boundaries=" +
            str((int)face->boundariesList.size());
        return String(text.c_str());
    }
    text = "face=" + str(face->id) + " ringSize=" +
        str((int)face->boundariesList.get(1)->halfEdgesList.size()) +
        " usable=" + (hasUsableIntegrationRing(face) ? "true" : "false") +
        " connected=" + (hasCompleteHalfEdgeConnectivity(face) ? "true" : "false") +
        " pair=" +
        str(pairs.pairIndexA(face)) +
        "/" +
        str(pairs.pairIndexB(face));
    return String(text.c_str());
}

int Finisher::sanitizePairedFaces(FaceList& sonfa, FaceList& sonfb,
    const ConnectResult& pairs)
{
    FaceList matchedA;
    FaceList matchedB;
    long i;
    long j;

    ArrayList<BOOLEAN> usedB(Collections<BOOLEAN>::nCopies(sonfb.size(), FALSE));

    for ( i = 0; i < sonfa.size(); i++ ) {
        _PolyhedralBoundedSolidFace* faceA = sonfa[i];
        int pairIndexA;
        BOOLEAN validA = hasUsableIntegrationRing(faceA);

        if ( !validA ) {
            if ( isPipelineSummaryTraceEnabled() ) {
                tracePipelineSummary(String("finish sanitize skip A ") +
                    integrationRingSummary(faceA, pairs));
            }
            continue;
        }

        pairIndexA = pairs.pairIndexA(faceA);
        for ( j = 0; j < sonfb.size(); j++ ) {
            _PolyhedralBoundedSolidFace* faceB = sonfb[j];
            int pairIndexB;
            BOOLEAN validB;

            if ( usedB[j] ) {
                continue;
            }
            validB = hasUsableIntegrationRing(faceB);
            if ( !validB ) {
                if ( isPipelineSummaryTraceEnabled() ) {
                    tracePipelineSummary(String("finish sanitize skip B ") +
                        integrationRingSummary(faceB, pairs));
                }
                continue;
            }
            pairIndexB = pairs.pairIndexB(faceB);
            if ( pairIndexA != -1 && pairIndexA == pairIndexB ) {
                if ( isPipelineSummaryTraceEnabled() ) {
                    tracePipelineSummary(String("finish sanitize match A ") +
                        integrationRingSummary(faceA, pairs) + " B " +
                        integrationRingSummary(faceB, pairs));
                }
                matchedA.add(faceA);
                matchedB.add(faceB);
                usedB[j] = TRUE;
                break;
            }
        }
    }

    if ( matchedA.isEmpty() && !sonfa.isEmpty() && sonfa.size() == sonfb.size() ) {
        // [MANT1988] Program 15.15 pairs sonfa[i] with sonfb[i]. Connect
        // tags every pair with its index; when none of them matches, the
        // null faces cannot be paired and finish has nothing to glue.
        // This used to fall back to list order; it is now an error.
        String message = "finish: no null face pair matched by pair index (sonfa=" +
            str((int)sonfa.size()) + " sonfb=" + str((int)sonfb.size()) + ")";
        throw SolidOperationFailedException(message);
    }

    sonfa = matchedA;
    sonfb = matchedB;
    tracePipelineSummary(("finish sanitize matched=" +
        str((int)matchedA.size())).c_str());
    return (int)matchedA.size();
}

_PolyhedralBoundedSolidHalfEdge* Finisher::findNonDegenerateEar(
    _PolyhedralBoundedSolidHalfEdge* start,
    int loopSize,
    const ToleranceContext& context)
{
    _PolyhedralBoundedSolidHalfEdge* candidate;
    _PolyhedralBoundedSolidHalfEdge* nextHe;
    _PolyhedralBoundedSolidHalfEdge* prevHe;
    _PolyhedralBoundedSolidHalfEdge* bestCandidate;
    Vector3Dd p0;
    Vector3Dd p1;
    Vector3Dd p2;
    Vector3Dd a;
    Vector3Dd b;
    double bestSinTheta;
    int safety;

    if ( start == 0 || loopSize <= 0 ) {
        return 0;
    }
    bestCandidate = 0;
    bestSinTheta = 0.0;
    candidate = start;
    safety = 0;
    do {
        nextHe = candidate->next();
        prevHe = candidate->previous();
        if ( nextHe != 0 && prevHe != 0 && nextHe != prevHe &&
             candidate->parentLoop != 0 &&
             nextHe->parentLoop == candidate->parentLoop &&
             prevHe->parentLoop == candidate->parentLoop &&
             candidate->startingVertex != 0 &&
             nextHe->startingVertex != 0 &&
             prevHe->startingVertex != 0 ) {
            p0 = prevHe->startingVertex->position;
            p1 = candidate->startingVertex->position;
            p2 = nextHe->startingVertex->position;
            a = p1.subtract(p0);
            b = p2.subtract(p0);
            // Use the same normalized-vector collinearity test as
            // validateFacePointsAreCoplanar to guarantee the ear
            // triangle will pass planarity after the split.
            if ( a.length() > context.epsilon() &&
                 b.length() > context.epsilon() ) {
                Vector3Dd an = a.normalized();
                Vector3Dd bn = b.normalized();
                double aDotB = fabs(an.dotProduct(bn));
                double sinTheta = an.crossProduct(bn).length();
                if ( aDotB < 1.0 - context.unitVectorTolerance() ) {
                    return candidate;
                }
                if ( sinTheta > bestSinTheta ) {
                    bestSinTheta = sinTheta;
                    bestCandidate = candidate;
                }
            }
        }
        candidate = candidate->next();
        safety++;
    } while ( candidate != 0 && candidate != start &&
              safety <= loopSize + 1 );
    // No ear with sufficient angle found - fall back to the widest ear
    // above epsilon for cases where the polygon is nearly degenerate.
    if ( bestCandidate != 0 &&
         bestSinTheta > context.unitVectorTolerance() / 10.0 ) {
        return bestCandidate;
    }
    return 0;
}

/**
Returns true when the loop has at least one vertex whose position coincides
with another vertex earlier in the loop (i.e., the boundary is
self-touching / figure-8). A self-touching inner ring cannot be extracted as
a valid face via lmfkrh because the resulting face would be a degenerate
inverted membrane.
*/
BOOLEAN Finisher::hasSelfTouchingVertex(_PolyhedralBoundedSolidLoop* loop,
    const ToleranceContext& tol)
{
    if ( loop == 0 ) {
        return FALSE;
    }
    long int size = loop->halfEdgesList.size();
    long int i;
    long int j;
    for ( i = 1; i < size; i++ ) {
        for ( j = 0; j < i; j++ ) {
            if ( PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                    loop->halfEdgesList.get(i)->startingVertex->position,
                    loop->halfEdgesList.get(j)->startingVertex->position,
                    tol) ) {
                return TRUE;
            }
        }
    }
    return FALSE;
}

void Finisher::extractInnerLoopsOfNonPlanarFace(PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidFace* face)
{
    long int safety;
    long int maxLoops;

    if ( face == 0 ) {
        return;
    }
    maxLoops = face->boundariesList.size();
    safety = 0;
    ToleranceContext tol = PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    while ( face->boundariesList.size() > 1 && safety <= maxLoops ) {
        _PolyhedralBoundedSolidLoop* innerLoop;

        safety++;
        innerLoop = face->boundariesList.get(1);
        if ( innerLoop == 0 ) {
            break;
        }
        // Section 3 guard: a self-touching inner loop would produce an
        // inverted membrane face when extracted via lmfkrh. Retain it as-is.
        if ( hasSelfTouchingVertex(innerLoop, tol) ) {
            Logger::reportMessage("PolyhedralBoundedSolid", Logger::WARNING,
                "extractInnerLoopsOfNonPlanarFace",
                ("finish: skipped self-touching inner loop in face " +
                 str(face->id) +
                 "; retaining loop to avoid membrane artifact").c_str());
            break;
        }
        if ( PolyhedralBoundedSolidEulerOperators::lmfkrh(solid,
                innerLoop, solid->getMaxFaceId() + 1) == 0 ) {
            break;
        }
    }
}

void Finisher::triangulateNonPlanarFaces(PolyhedralBoundedSolid* solid)
{
    long int i;
    long int safetyCount;
    long int maxIterations;
    long int initialCount;

    lastTriangulatedFaceCount = 0;
    i = 0;
    safetyCount = 0;
    initialCount = solid->getPolygonsList().size();
    maxIterations = 50 * (initialCount + 1);
    while ( i < solid->getPolygonsList().size() &&
            safetyCount < maxIterations ) {
        _PolyhedralBoundedSolidFace* face;
        _PolyhedralBoundedSolidHalfEdge* scan;
        _PolyhedralBoundedSolidHalfEdge* ear;
        _PolyhedralBoundedSolidHalfEdge* next;
        _PolyhedralBoundedSolidHalfEdge* prev;
        int loopSize;
        int newFaceId;

        safetyCount++;
        face = solid->getPolygonsList().get(i);
        // Section 3: for multi-loop faces, check whether any inner loop is
        // self-touching BEFORE the planarity check, as a self-touching inner
        // ring can be coplanar with the outer boundary.
        if ( face->boundariesList.size() > 1 ) {
            BOOLEAN hasSelfTouching = FALSE;
            ToleranceContext tol =
                PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
            long int li;
            for ( li = 1; li < face->boundariesList.size(); li++ ) {
                if ( hasSelfTouchingVertex(face->boundariesList.get(li), tol) ) {
                    hasSelfTouching = TRUE;
                    break;
                }
            }
            if ( hasSelfTouching ) {
                extractInnerLoopsOfNonPlanarFace(solid, face);
                if ( face->boundariesList.size() != 1 ) {
                    i++;
                    continue;
                }
                // All inner loops were extracted or guarded; face now has
                // 1 loop - fall through to planarity check.
            }
        }
        if ( PolyhedralBoundedSolidGeometricValidator::validateFaceIsPlanar(face) ) {
            i++;
            continue;
        }
        if ( face->boundariesList.size() > 1 ) {
            extractInnerLoopsOfNonPlanarFace(solid, face);
            if ( face->boundariesList.size() != 1 ) {
                i++;
                continue;
            }
        }
        loopSize = (int)face->boundariesList.get(0)->halfEdgesList.size();
        if ( loopSize <= 3 ) {
            // Degenerate or collinear small face.
            _PolyhedralBoundedSolidLoop* loop0 = face->boundariesList.get(0);
            // Section 9.5-degenerate: size-1 self-referential face with
            // orphaned mirror cannot be killed via lkef. Prune it directly.
            if ( loopSize == 1 &&
                 loop0->halfEdgesList.get(0) != 0 &&
                 loop0->halfEdgesList.get(0)->next() ==
                     loop0->halfEdgesList.get(0) ) {
                _PolyhedralBoundedSolidHalfEdge* h0 = loop0->halfEdgesList.get(0);
                if ( h0->mirrorHalfEdge() == 0 ||
                     h0->mirrorHalfEdge()->parentLoop == 0 ) {
                    solid->getPolygonsList().remove(i);
                    if ( h0->parentEdge != 0 ) {
                        long int edgeIdx;
                        for ( edgeIdx = 0;
                              edgeIdx < solid->getEdgesList().size();
                              edgeIdx++ ) {
                            if ( solid->getEdgesList().get(edgeIdx) ==
                                 h0->parentEdge ) {
                                solid->getEdgesList().remove(edgeIdx);
                                break;
                            }
                        }
                    }
                    continue; // don't increment i: next face slides to i
                }
            }
            // Try to absorb into adjacent face via lkef. When lkef
            // succeeds, restart the scan from index 0 so faces that absorbed
            // the triangle are re-checked.
            BOOLEAN killed = FALSE;
            long int k;
            for ( k = 0; k < loop0->halfEdgesList.size(); k++ ) {
                _PolyhedralBoundedSolidHalfEdge* h = loop0->halfEdgesList.get(k);
                if ( h != 0 &&
                     h->mirrorHalfEdge() != 0 &&
                     h->mirrorHalfEdge()->parentLoop != 0 &&
                     h->mirrorHalfEdge()->parentLoop->parentFace != face ) {
                    PolyhedralBoundedSolidEulerOperators::lkef(
                        solid, h->mirrorHalfEdge(), h);
                    killed = TRUE;
                    break;
                }
            }
            if ( killed ) {
                i = 0;
            }
            else {
                i++;
            }
            continue;
        }
        scan = face->boundariesList.get(0)->boundaryStartHalfEdge;
        if ( scan == 0 ) {
            i++;
            continue;
        }
        ToleranceContext context = PolyhedralBoundedSolidNumericPolicy::forFace(face);
        ear = findNonDegenerateEar(scan, loopSize, context);
        if ( ear == 0 ) {
            i++;
            continue;
        }
        next = ear->next();
        prev = ear->previous();
        if ( next == 0 || prev == 0 || next == prev ||
             next->parentLoop != ear->parentLoop ||
             prev->parentLoop != ear->parentLoop ) {
            i++;
            continue;
        }
        newFaceId = (idNamespace != 0) ?
            idNamespace->nextFaceId(solid) :
            solid->getMaxFaceId() + 1;
        if ( PolyhedralBoundedSolidEulerOperators::lmef(solid, next, prev,
                newFaceId) == 0 ) {
            i++;
        }
        else {
            lastTriangulatedFaceCount++;
        }
    }
}

void Finisher::exchange(FaceList& faces, int i, int j)
{
    _PolyhedralBoundedSolidFace* exchanged = faces[i];

    faces[i] = faces[j];
    faces[j] = exchanged;
}

/**
Adds to `counts` the answer for one face: element 0 when a point of its
interior is strictly inside `other`, element 1 when strictly outside.
A face without such a point, or with it on the boundary of `other`,
adds nothing.
*/
void Finisher::countSide(_PolyhedralBoundedSolidFace* face,
    PolyhedralBoundedSolid* other, const FaceGeometryList& otherFaces,
    int counts[2])
{
    Vector3Dd witness;

    if ( !_PolyhedralBoundedSolidSetNonIntersectingClassifier
              ::faceInteriorWitness(face, witness) ) {
        return;
    }
    int status = _PolyhedralBoundedSolidSetNonIntersectingClassifier
        ::classifyPointAgainstSolid(other, otherFaces, witness);
    if ( status == Geometry::INSIDE ) {
        counts[0]++;
    }
    else if ( status == Geometry::OUTSIDE ) {
        counts[1]++;
    }
}

/**
`countSide` over the faces across the half-edges of `loop`, each
face once.
*/
void Finisher::sidesAcross(_PolyhedralBoundedSolidLoop* loop,
    PolyhedralBoundedSolid* other, const FaceGeometryList& otherFaces,
    int counts[2])
{
    TreeSet<_PolyhedralBoundedSolidFace*> counted;
    _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* he = start;
    long guard = loop->halfEdgesList.size();

    counts[0] = 0;
    counts[1] = 0;
    if ( start == 0 ) {
        return;
    }
    do {
        _PolyhedralBoundedSolidHalfEdge* mate = he->mirrorHalfEdge();
        if ( mate != 0 && mate->parentLoop != 0 &&
             mate->parentLoop->parentFace != loop->parentFace &&
             !counted.contains(mate->parentLoop->parentFace) ) {
            counted.add(mate->parentLoop->parentFace);
            countSide(mate->parentLoop->parentFace, other, otherFaces,
                counts);
        }
        he = he->next();
        guard--;
    } while ( he != 0 && he != start && guard >= 0 );
}

/**
True when the outer loop of `face` runs through the points of its ring
and no others: the face is the null face between the two copies of a
cut, without area of its own.
*/
BOOLEAN Finisher::isBand(_PolyhedralBoundedSolidFace* face)
{
    _PolyhedralBoundedSolidLoop* outer = face->boundariesList.get(0);
    _PolyhedralBoundedSolidLoop* ring = face->boundariesList.get(1);
    ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forFace(face);
    long i;
    long j;

    for ( i = 0; i < outer->halfEdgesList.size(); i++ ) {
        BOOLEAN found = FALSE;
        for ( j = 0; j < ring->halfEdgesList.size() && !found; j++ ) {
            found = PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                outer->halfEdgesList.get(i)->startingVertex->position,
                ring->halfEdgesList.get(j)->startingVertex->position,
                context);
        }
        if ( !found ) {
            return FALSE;
        }
    }
    return TRUE;
}

/**
`face` is a null face of connect: its ring is one copy of a cut. By
convention the faces across the ring are the part of the solid inside
`other`, and what lies on the side of the outer loop (the faces across
it for a band, the face itself otherwise) is the part outside. True
when both sides say the opposite and neither has a face that says
otherwise.
*/
BOOLEAN Finisher::sidesAreSwapped(_PolyhedralBoundedSolidFace* face,
    PolyhedralBoundedSolid* other, const FaceGeometryList& otherFaces,
    int cut, const char* name)
{
    int inside[2];
    int outside[2];

    sidesAcross(face->boundariesList.get(1), other, otherFaces, inside);
    if ( isBand(face) ) {
        sidesAcross(face->boundariesList.get(0), other, otherFaces,
            outside);
    }
    else {
        outside[0] = 0;
        outside[1] = 0;
        countSide(face, other, otherFaces, outside);
    }
    BOOLEAN swapped = inside[0] == 0 && outside[1] == 0 &&
        inside[1] > 0 && outside[0] > 0;
    String text = "finish sides of cut " + str(cut) + " on " + name +
        ": the part taken for inside has " + str(inside[0]) +
        " faces inside and " + str(inside[1]) +
        " outside; the part taken for outside " + str(outside[0]) +
        " and " + str(outside[1]);
    if ( swapped ) {
        text = text + "; exchanged";
    }
    tracePipelineSummary(text);
    return swapped;
}

/**
Adds to `reached` every face connected to `seed` through edges.
C++ port note: Java takes a null `component` to skip the collection.
*/
void Finisher::flood(_PolyhedralBoundedSolidFace* seed,
    TreeSet<_PolyhedralBoundedSolidFace*>& reached,
    FaceList* component)
{
    FaceList pending;
    long i;

    if ( seed == 0 || reached.contains(seed) ) {
        return;
    }
    reached.add(seed);
    pending.add(seed);
    while ( !pending.isEmpty() ) {
        _PolyhedralBoundedSolidFace* face = pending[pending.size() - 1];
        pending.remove(pending.size() - 1);
        if ( component != 0 ) {
            component->add(face);
        }
        for ( i = 0; i < face->boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge* start =
                loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* he = start;
            long guard = loop->halfEdgesList.size();
            if ( start == 0 ) {
                continue;
            }
            do {
                _PolyhedralBoundedSolidHalfEdge* mate = he->mirrorHalfEdge();
                if ( mate != 0 && mate->parentLoop != 0 &&
                     mate->parentLoop->parentFace != 0 &&
                     !reached.contains(mate->parentLoop->parentFace) ) {
                    reached.add(mate->parentLoop->parentFace);
                    pending.add(mate->parentLoop->parentFace);
                }
                he = he->next();
                guard--;
            } while ( he != 0 && he != start && guard >= 0 );
        }
    }
}

/**
One face of every shell of `solid` that no cut reaches and that lies on
the wanted side of `other`: inside when `keepInside`, outside
otherwise. The side is the one of the first vertex of the shell that is
not on the boundary of `other`; a shell with every vertex on that
boundary is left out.
@param cutFaces the null faces of `solid` and the faces made from their
       rings: every cut part of the solid is connected to one of them
*/
void Finisher::untouchedShellsToKeep(PolyhedralBoundedSolid* solid,
    const FaceList& cutFaces, PolyhedralBoundedSolid* other,
    BOOLEAN keepInside, FaceList& seeds)
{
    TreeSet<_PolyhedralBoundedSolidFace*> reached;
    FaceGeometryList otherFaces;
    BOOLEAN haveOtherFaces = FALSE;
    long i;
    long j;
    long k;

    seeds.clear();
    for ( i = 0; i < cutFaces.size(); i++ ) {
        flood(cutFaces[i], reached, 0);
    }
    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        if ( reached.contains(face) ) {
            continue;
        }
        FaceList shell;
        flood(face, reached, &shell);
        if ( !haveOtherFaces ) {
            ToleranceContext context =
                PolyhedralBoundedSolidNumericPolicy::forSolids(solid, other);
            setNumericContext(&context);
            PolyhedralBoundedSolidFaceGeometry::of(other, otherFaces);
            haveOtherFaces = TRUE;
        }
        int side = Geometry::LIMIT;
        for ( j = 0; j < shell.size() && side == Geometry::LIMIT; j++ ) {
            _PolyhedralBoundedSolidFace* member = shell[j];
            for ( k = 0; k < member->boundariesList.size() &&
                  side == Geometry::LIMIT; k++ ) {
                _PolyhedralBoundedSolidHalfEdge* he = member
                    ->boundariesList.get(k)->boundaryStartHalfEdge;
                if ( he == 0 || he->startingVertex == 0 ) {
                    continue;
                }
                side = _PolyhedralBoundedSolidSetNonIntersectingClassifier
                    ::classifyPointAgainstSolid(other, otherFaces,
                        he->startingVertex->position);
            }
        }
        if ( (keepInside && side == Geometry::INSIDE) ||
             (!keepInside && side == Geometry::OUTSIDE) ) {
            seeds.add(face);
            String where;
            if ( keepInside ) {
                where = "inside";
            }
            else {
                where = "outside";
            }
            tracePipelineSummary("finish keeps a whole shell of " +
                str((int)shell.size()) + " faces, " + where +
                " the other solid");
        }
    }
    PolyhedralBoundedSolidFaceGeometry::dispose(otherFaces);
}

void Finisher::finish(PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    PolyhedralBoundedSolid* outRes,
    int op,
    int debugFlags,
    FaceList& sonfa,
    FaceList& sonfb,
    const ConnectResult& pairs)
{
    int i;
    int inda;
    int indb;
    _PolyhedralBoundedSolidFace* f;

    if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
        printf("- 4. ------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        printf("setOpFinish\n");
    }

    if ( (debugFlags & DEBUG_06_FINISH) != 0x00 ) {
        printf("TESTING FINISH: %d\n", (int)sonfa.size());
    }
    tracePipelineSummary(("finish start op=" + str(op) +
        " sonfa=" + str((int)sonfa.size()) +
        " sonfb=" + str((int)sonfb.size())).c_str());

    int oldsize = sanitizePairedFaces(sonfa, sonfb, pairs);
    inda = (op == INTERSECTION) ? (int)sonfa.size() : 0;
    indb = (op == UNION) ? 0 : (int)sonfb.size();

    // [MANT1988] Program 15.15 takes sonf[i] for the part of the solid
    // outside the other one and the face lmfkrh makes from its ring
    // for the part inside. That rests on the orientation every null
    // edge got on its way here. The parts themselves say which is
    // which: a point of the faces on each side of the cut is inside
    // the other solid or outside it. When they say the opposite of the
    // convention for a whole cut, the two faces are exchanged
    // (plan-stage11 Phase F).
    ArrayList<BOOLEAN> swappedA(Collections<BOOLEAN>::nCopies(oldsize, FALSE));
    ArrayList<BOOLEAN> swappedB(Collections<BOOLEAN>::nCopies(oldsize, FALSE));
    if ( oldsize > 0 ) {
        // Nothing edits the two solids while the sides are read, so
        // their face planes and boxes are computed once.
        ToleranceContext context =
            PolyhedralBoundedSolidNumericPolicy::forSolids(inSolidA, inSolidB);
        setNumericContext(&context);
        FaceGeometryList facesOfA;
        FaceGeometryList facesOfB;
        PolyhedralBoundedSolidFaceGeometry::of(inSolidA, facesOfA);
        PolyhedralBoundedSolidFaceGeometry::of(inSolidB, facesOfB);
        for ( i = 0; i < oldsize; i++ ) {
            swappedA[i] = sidesAreSwapped(sonfa[i], inSolidB,
                facesOfB, i, "A");
            swappedB[i] = sidesAreSwapped(sonfb[i], inSolidA,
                facesOfA, i, "B");
        }
        PolyhedralBoundedSolidFaceGeometry::dispose(facesOfA);
        PolyhedralBoundedSolidFaceGeometry::dispose(facesOfB);
    }

    for ( i = 0; i < oldsize; i++ ) {
        f = PolyhedralBoundedSolidEulerOperators::lmfkrh(inSolidA,
            sonfa[i]->boundariesList.get(1), inSolidA->getMaxFaceId()+1);
        sonfa.add(f);

        f = PolyhedralBoundedSolidEulerOperators::lmfkrh(inSolidB,
            sonfb[i]->boundariesList.get(1), inSolidB->getMaxFaceId()+1);
        sonfb.add(f);
    }

    for ( i = 0; i < oldsize; i++ ) {
        if ( swappedA[i] ) {
            exchange(sonfa, i, i + oldsize);
        }
        if ( swappedB[i] ) {
            exchange(sonfb, i, i + oldsize);
        }
    }

    // Shells that no intersection curve reaches are not connected to
    // any null face, so the moves below never see them. Each one lies
    // wholly inside or wholly outside the other solid and belongs to
    // the result on the same terms as the cut parts: A outside B for
    // the union and the difference, A inside B for the intersection;
    // B outside A for the union, B inside A otherwise. They used to be
    // left behind without an error (plan-stage11 Phase F): the union
    // of a two-piece solid with a solid that crosses only one of the
    // pieces lost the other piece.
    FaceList wholeShellsOfA;
    FaceList wholeShellsOfB;
    untouchedShellsToKeep(inSolidA, sonfa, inSolidB,
        op == INTERSECTION, wholeShellsOfA);
    untouchedShellsToKeep(inSolidB, sonfb, inSolidA, op != UNION,
        wholeShellsOfB);

    if ( op == SUBTRACT ) {
        inSolidB->revert();
    }

    for ( i = 0; i < oldsize; i++ ) {
        movefac(sonfa[i+inda], outRes);
        movefac(sonfb[i+indb], outRes);
    }
    for ( i = 0; i < (int)wholeShellsOfA.size(); i++ ) {
        movefac(wholeShellsOfA[i], outRes);
    }
    for ( i = 0; i < (int)wholeShellsOfB.size(); i++ ) {
        movefac(wholeShellsOfB[i], outRes);
    }

    cleanup(outRes);

    for ( i = 0; i < oldsize; i++ ) {
        PolyhedralBoundedSolidEulerOperators::lkfmrh(outRes, sonfa[i+inda],
            sonfb[i+indb]);
        PolyhedralBoundedSolidTopologyEditing::loopGlue(outRes, sonfa[i+inda]);
    }
    cleanup(outRes);
    triangulateNonPlanarFaces(outRes);
    PolyhedralBoundedSolidTopologyEditing::compactIds(outRes);
    tracePipelineSummary(("finish end outRes faces=" +
        str((int)outRes->getPolygonsList().size()) +
        " edges=" + str((int)outRes->getEdgesList().size()) +
        " vertices=" + str((int)outRes->getVerticesList().size())).c_str());
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
