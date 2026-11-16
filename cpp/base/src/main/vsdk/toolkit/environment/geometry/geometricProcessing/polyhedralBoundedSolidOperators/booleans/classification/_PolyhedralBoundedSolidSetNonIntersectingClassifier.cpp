//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "java/lang/Math.h"
#include "java/util/Collections.h"
#include <climits>
#include <cmath>

#include "java/lang/Double.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidInteriorOverlap.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef _PolyhedralBoundedSolidSetNonIntersectingClassifier Classifier;

//= Preflight cache =================================================

const int Classifier::_PreflightCache::UNSET = INT_MIN;

Classifier::_PreflightCache::_PreflightCache(
    PolyhedralBoundedSolid* inA, PolyhedralBoundedSolid* inB)
    : a(inA), b(inB), aInBValue(UNSET), bInAValue(UNSET),
      interiorOverlap(-1), edgeFaceAB(-1), edgeFaceBA(-1)
{
}

int Classifier::_PreflightCache::aInB()
{
    if ( aInBValue == UNSET ) {
        aInBValue = classifySolidAgainstSolid(a, b);
    }
    return aInBValue;
}

int Classifier::_PreflightCache::bInA()
{
    if ( bInAValue == UNSET ) {
        bInAValue = classifySolidAgainstSolid(b, a);
    }
    return bInAValue;
}

bool Classifier::_PreflightCache::hasInteriorOverlap()
{
    if ( interiorOverlap < 0 ) {
        interiorOverlap = (signed char)(hasConfirmedInteriorOverlap(a, b) ? 1 : 0);
    }
    return interiorOverlap == 1;
}

bool Classifier::_PreflightCache::hasEdgeFaceIntersectionAB()
{
    if ( edgeFaceAB < 0 ) {
        edgeFaceAB = (signed char)(hasProperEdgeFaceIntersection(a, b) ? 1 : 0);
    }
    return edgeFaceAB == 1;
}

bool Classifier::_PreflightCache::hasEdgeFaceIntersectionBA()
{
    if ( edgeFaceBA < 0 ) {
        edgeFaceBA = (signed char)(hasProperEdgeFaceIntersection(b, a) ? 1 : 0);
    }
    return edgeFaceBA == 1;
}

//= Face planes holder ==============================================

/**
Precomputes the containing plane and the bounding box of every face of
`solid`, indexed by face position. Point classification tests many points
against the same unmutated solid, so computing each plane once (instead of
once per point per face) removes the dominant Newell/tolerance-context
recompute from the hot path, and the box skips the point-in-face test for
points far from the face (Stage G). Only valid while `solid` is not
mutated.

C++ port note: this holder is the Java `precomputeFacePlanes` array; it
frees the snapshots when it goes out of scope.
*/
Classifier::FacePlanes::FacePlanes(PolyhedralBoundedSolid* solid)
{
    if ( solid == 0 ) {
        return;
    }
    planes = PolyhedralBoundedSolidFaceGeometry::of(solid);
}

Classifier::FacePlanes::~FacePlanes()
{
    PolyhedralBoundedSolidFaceGeometry::release(planes);
}

const java::ArrayList<PolyhedralBoundedSolidFaceGeometry*>&
Classifier::FacePlanes::list() const
{
    return planes;
}

//= Public preflight API ============================================

Classifier::_PolyhedralBoundedSolidSetNonIntersectingClassifier()
{
}

Classifier::_PreflightCache Classifier::newPreflightCache(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB)
{
    return _PreflightCache(inSolidA, inSolidB);
}

bool Classifier::runContainmentOnlyPreflightCase(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB)
{
    _PreflightCache cache = newPreflightCache(inSolidA, inSolidB);
    return runContainmentOnlyPreflightCase(inSolidA, inSolidB, cache);
}

bool Classifier::runContainmentOnlyPreflightCase(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    _PreflightCache& cache)
{
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(inSolidA, inSolidB);
    setNumericContext(&context);

    int relation = classifyNoIntersectionRelation(cache.aInB(), cache.bInA());

    // Restricted to strict containment only. An earlier attempt to
    // extend this preflight to "tangent containment" (one solid
    // sitting inside the other with all its boundary vertices on
    // the other's surface) regressed legitimate cases - those need
    // the regular pipeline because their result expects the inner
    // surface preserved as a hole/cut, not a plain merge. The two
    // absorption-step-2 fixtures (MANT1988_15_2_LIMIT,
    // MANT1988_6_13) that motivate this preflight are tracked in
    // plan section 7.3.1.D-cont as remaining drift.
    if ( relation != NO_INT_RELATION_A_IN_B &&
         relation != NO_INT_RELATION_B_IN_A ) {
        return false;
    }

    if ( cache.hasEdgeFaceIntersectionAB() ) {
        return false;
    }
    if ( cache.hasEdgeFaceIntersectionBA() ) {
        return false;
    }

    return true;
}

bool Classifier::runTouchingOnlyPreflightCase(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB)
{
    _PreflightCache cache = newPreflightCache(inSolidA, inSolidB);
    return runTouchingOnlyPreflightCase(inSolidA, inSolidB, cache);
}

bool Classifier::runTouchingOnlyPreflightCase(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    _PreflightCache& cache)
{
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(inSolidA, inSolidB);
    setNumericContext(&context);

    int relation = classifyNoIntersectionRelation(cache.aInB(), cache.bInA());

    if ( relation != NO_INT_RELATION_TOUCHING ) {
        return false;
    }

    if ( cache.hasEdgeFaceIntersectionAB() ) {
        return false;
    }
    if ( cache.hasEdgeFaceIntersectionBA() ) {
        return false;
    }

    // Solids that share part of a face and nothing else are a contact
    // too (STACKED_BLOCKS): the interiors do not overlap. A union along
    // such a face is glued by the pipeline; the caller keeps it out.
    if ( cache.hasInteriorOverlap() ) {
        return false;
    }

    return true;
}

PolyhedralBoundedSolid* Classifier::runSetOpNoIntersectionCase(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    PolyhedralBoundedSolid* outRes,
    int op)
{
    _PreflightCache cache = newPreflightCache(inSolidA, inSolidB);
    return runSetOpNoIntersectionCase(inSolidA, inSolidB, outRes, op, cache);
}

PolyhedralBoundedSolid* Classifier::runSetOpNoIntersectionCase(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    PolyhedralBoundedSolid* outRes,
    int op,
    _PreflightCache& cache)
{
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(inSolidA, inSolidB);
    setNumericContext(&context);

    int relation = classifyNoIntersectionRelation(cache.aInB(), cache.bInA());

    // Tangent containment (B-02 b): one solid inside the other with
    // faces on the other's boundary. The vertex test cannot see it
    // (every vertex is "on"), so it reports touching. Only the answers
    // that need no cavity use it; B \ A-style cavities need the
    // pipeline.
    if ( relation == NO_INT_RELATION_TOUCHING ) {
        if ( isTangentlyContained(inSolidA, inSolidB) ) {
            relation = NO_INT_RELATION_A_IN_B;
        }
        else if ( op != SUBTRACT &&
                  isTangentlyContained(inSolidB, inSolidA) ) {
            relation = NO_INT_RELATION_B_IN_A;
        }
    }

    if ( op == INTERSECTION ) {
        if ( relation == NO_INT_RELATION_A_IN_B ) {
            outRes->merge(inSolidA);
        }
        else if ( relation == NO_INT_RELATION_B_IN_A ) {
            outRes->merge(inSolidB);
        }
        else if ( !cache.hasInteriorOverlap() ) {
            appendContactLaminae(inSolidA, inSolidB, outRes);
        }
        return outRes;
    }

    if ( op == UNION ) {
        if ( relation == NO_INT_RELATION_A_IN_B ) {
            outRes->merge(inSolidB);
        }
        else if ( relation == NO_INT_RELATION_B_IN_A ) {
            outRes->merge(inSolidA);
        }
        else if ( relation == NO_INT_RELATION_TOUCHING ) {
            // Two shells sharing only an edge or a vertex: a
            // non-manifold result, rejected by design (B-04). Face
            // contact never gets here: the pipeline glues it.
            throw NonManifoldBooleanResultException(
                "UNION of solids touching only along edges or at "
                "vertices is non-manifold (two shells sharing a "
                "boundary point); rejected");
        }
        else {
            outRes->merge(inSolidA);
            outRes->merge(inSolidB);
        }
        return outRes;
    }

    if ( relation == NO_INT_RELATION_A_IN_B ) {
        return outRes;
    }
    if ( relation == NO_INT_RELATION_B_IN_A ) {
        outRes->merge(inSolidA);
        inSolidB->revert();
        outRes->merge(inSolidB);
        PolyhedralBoundedSolidTopologyEditing::compactIds(outRes);
        return outRes;
    }
    outRes->merge(inSolidA);
    return outRes;
}

/**
Lamina policy for the intersection of solids that share part of a face
and nothing else (STACKED_BLOCKS): the shared part is returned as
laminae, zero volume, valid and easy to display, not as the regularised
empty set. A contact whose area is below the collapsed face threshold
gives no lamina: a collapsed lamina is not a valid result (see
_GeometricCollapsedLaminaStrategy).

This is part of the "no intersections found" resolution of the pipeline
([MANT1988] 15.4). Until plan-stage11 Phase E a preflight answered these
pairs before generate and classify ran.
*/
void Classifier::appendContactLaminae(PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB, PolyhedralBoundedSolid* outRes)
{
    java::ArrayList<Polygon> contactPolygons =
        partialCoplanarFaceAreaOverlapPolygons(inSolidA, inSolidB);
    int i;

    for ( i = 0; i < contactPolygons.size(); i++ ) {
        if ( polygonArea(contactPolygons.get(i)) <
             PolyhedralBoundedSolidGeometricValidator
                 ::collapsedFaceAreaThreshold(numericContext) ) {
            continue;
        }
        PolyhedralBoundedSolid* lamina =
            createLaminaFromPolygon(contactPolygons.get(i));
        if ( lamina->getPolygonsList().size() > 0 ) {
            outRes->merge(lamina);
        }
        // After merge the lamina is an empty shell
        delete lamina;
    }
}

//= Point and solid classification ==================================

int Classifier::compareToZero(double value)
{
    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
        ::compareToZero(value);
}

int Classifier::pointInFace(_PolyhedralBoundedSolidFace* face,
    const Vector3Dd& point)
{
    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
        ::pointInFace(face, point);
}

int Classifier::classifyPointAgainstSolid(PolyhedralBoundedSolid* solid,
    const Vector3Dd& point)
{
    FacePlanes facePlanes(solid);
    return classifyPointAgainstSolid(solid, facePlanes.list(), point);
}

int Classifier::classifyPointAgainstSolid(PolyhedralBoundedSolid* solid,
    const java::ArrayList<PolyhedralBoundedSolidFaceGeometry*>& facePlanes,
    const Vector3Dd& point)
{
    long int i;
    int j;
    _PolyhedralBoundedSolidFace* face;
    double eps = numericContext.bigEpsilon();
    int insideVotes = 0;
    int outsideVotes = 0;

    if ( solid == 0 || solid->getPolygonsList().size() < 1 ) {
        return Geometry::OUTSIDE;
    }

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        face = solid->getPolygonsList().get(i);
        InfinitePlane* facePlane = facePlanes.get(i)->getPlane();
        if ( facePlane == 0 ) {
            continue;
        }
        if ( std::fabs(facePlane->pointDistance(point)) <= eps &&
             !facePlanes.get(i)->projectionExcludes(point, eps) ) {
            if ( face->testPointInside(point, eps, facePlane) !=
                 Geometry::OUTSIDE ) {
                return Geometry::LIMIT;
            }
        }
    }

    Vector3Dd dirs[3] = {
        Vector3Dd(1.0, 0.371, 0.137),
        Vector3Dd(0.193, 1.0, 0.417),
        Vector3Dd(0.217, 0.173, 1.0)
    };

    for ( j = 0; j < 3; j++ ) {
        int hits = 0;
        bool ambiguous = false;
        java::ArrayList<double> distances;
        Ray ray(point, dirs[j]);

        for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
            face = solid->getPolygonsList().get(i);
            InfinitePlane* facePlane = facePlanes.get(i)->getPlane();
            if ( facePlane == 0 ) {
                ambiguous = true;
                break;
            }
            Ray rayHit(ray);
            Ray* hit = facePlane->doIntersectionFirstHit(rayHit);
            if ( hit == 0 ) {
                continue;
            }
            double t = hit->getT();
            Vector3Dd pi = hit->getOrigin().add(hit->getDirection().multiply(t));
            delete hit;
            if ( t <= eps ) {
                continue;
            }
            if ( facePlanes.get(i)->projectionExcludes(pi, eps) ) {
                continue;
            }

            int status = face->testPointInside(pi, eps, facePlane);
            if ( status == Geometry::LIMIT ) {
                ambiguous = true;
                break;
            }
            if ( status == Geometry::INSIDE ) {
                bool duplicated = false;
                size_t k;
                for ( k = 0; k < distances.size(); k++ ) {
                    if ( std::fabs(distances[k] - t) <= eps ) {
                        duplicated = true;
                        break;
                    }
                }
                if ( !duplicated ) {
                    distances.add(t);
                    hits++;
                }
            }
        }

        if ( !ambiguous ) {
            if ( (hits % 2) == 1 ) {
                insideVotes++;
            }
            else {
                outsideVotes++;
            }
        }
    }

    if ( insideVotes > outsideVotes ) {
        return Geometry::INSIDE;
    }
    if ( outsideVotes > insideVotes ) {
        return Geometry::OUTSIDE;
    }
    return Geometry::LIMIT;
}

/**
Witness test used to tell a right empty result from a lost solid
(Fable5.2Analysis.md Stage F step 4): true when some point is interior
to both solids, so their intersection cannot be empty. The operands
must be intact (not consumed by a set operation).
*/
bool Classifier::interiorsOverlap(PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB)
{
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(solidA, solidB);
    setNumericContext(&context);
    return hasConfirmedInteriorOverlap(solidA, solidB);
}

/**
Witness test for A \ B: true when a vertex or a face interior witness
of `solidA` lies strictly outside `solidB`. A boundary point of A
outside B has interior points of A around it that are outside B, so
A \ B cannot be empty. (The converse does not hold, so a false answer
proves nothing.)
*/
bool Classifier::hasBoundaryPointOutside(PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB)
{
    int i;

    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(solidA, solidB);
    setNumericContext(&context);
    FacePlanes planesB(solidB);
    for ( i = 0; i < solidA->getVerticesList().size(); i++ ) {
        if ( classifyPointAgainstSolid(solidB, planesB.list(),
                 solidA->getVerticesList().get(i)->position) ==
             Geometry::OUTSIDE ) {
            return true;
        }
    }
    return someFaceWitnessIsOutside(solidA, solidB, planesB.list());
}

/**
True when some point is strictly inside both solids. Decided from the
boundaries by `_PolyhedralBoundedSolidInteriorOverlap`, without sampling.
*/
bool Classifier::hasConfirmedInteriorOverlap(
    PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB)
{
    return _PolyhedralBoundedSolidInteriorOverlap::interiorsOverlap(
        solidA, solidB);
}

/**
Classifies solid A against solid B for the "no proper intersection"
preflights (Fable5.2Analysis.md Stage D step 1, defect B-03).
- INSIDE: A is contained in B. No vertex of A is outside B, at least one
  is strictly inside, and a witness point interior to every face of A is
  not outside B. The witness test separates tangent containment from a
  solid whose edges lie in a face of B while part of it sticks out (B-03):
  there every vertex is in or on B, yet a face is not.
- LIMIT: A touches B, or straddles its boundary (vertices on both sides,
  or a face witness outside). Callers must not take LIMIT as containment;
  the touching preflight additionally requires that the interiors do not
  overlap.
- OUTSIDE: every vertex of A is outside B.
Before this rule the first vertex found inside made A "contained", even
with other vertices outside.
*/
int Classifier::classifySolidAgainstSolid(
    PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB)
{
    int i;
    int inside = 0;
    int limit = 0;
    int outside = 0;

    if ( solidA == 0 || solidA->getVerticesList().size() < 1 ) {
        return Geometry::OUTSIDE;
    }

    FacePlanes planesB(solidB);
    for ( i = 0; i < solidA->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* v = solidA->getVerticesList().get(i);
        int status = classifyPointAgainstSolid(solidB, planesB.list(),
            v->position);
        if ( status == Geometry::INSIDE ) {
            inside++;
        }
        else if ( status == Geometry::LIMIT ) {
            limit++;
        }
        else {
            outside++;
        }
    }

    if ( outside > 0 ) {
        return (inside > 0 || limit > 0) ? Geometry::LIMIT
                                         : Geometry::OUTSIDE;
    }
    if ( inside == 0 ) {
        return Geometry::LIMIT;
    }
    if ( someFaceWitnessIsOutside(solidA, solidB, planesB.list()) ) {
        return Geometry::LIMIT;
    }
    return Geometry::INSIDE;
}

/**
True when no vertex and no face witness of `inner` is outside `outer`,
and at least one face witness is strictly inside it: containment with
boundary contact.
*/
bool Classifier::isTangentlyContained(PolyhedralBoundedSolid* inner,
    PolyhedralBoundedSolid* outer)
{
    int i;
    bool someInside = false;
    FacePlanes planes(outer);

    for ( i = 0; i < inner->getVerticesList().size(); i++ ) {
        if ( classifyPointAgainstSolid(outer, planes.list(),
                 inner->getVerticesList().get(i)->position) ==
             Geometry::OUTSIDE ) {
            return false;
        }
    }
    for ( i = 0; i < inner->getPolygonsList().size(); i++ ) {
        Vector3Dd witness;
        if ( !faceInteriorWitness(inner->getPolygonsList().get(i),
                 &witness) ) {
            continue;
        }
        int status = classifyPointAgainstSolid(outer, planes.list(), witness);
        if ( status == Geometry::OUTSIDE ) {
            return false;
        }
        if ( status == Geometry::INSIDE ) {
            someInside = true;
        }
    }
    return someInside;
}

/**
True when some vertex of `inner`, or a point inside one of its faces,
lies on the boundary of `outer`. Meant for a solid already known to be
contained in `outer`.
*/
bool Classifier::touchesBoundaryFromInside(PolyhedralBoundedSolid* inner,
    PolyhedralBoundedSolid* outer)
{
    int i;

    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(inner, outer);
    setNumericContext(&context);
    FacePlanes planes(outer);
    for ( i = 0; i < inner->getVerticesList().size(); i++ ) {
        if ( classifyPointAgainstSolid(outer, planes.list(),
                 inner->getVerticesList().get(i)->position) ==
             Geometry::LIMIT ) {
            return true;
        }
    }
    for ( i = 0; i < inner->getPolygonsList().size(); i++ ) {
        Vector3Dd witness;
        if ( faceInteriorWitness(inner->getPolygonsList().get(i),
                 &witness) &&
             classifyPointAgainstSolid(outer, planes.list(), witness) ==
                 Geometry::LIMIT ) {
            return true;
        }
    }
    return false;
}

bool Classifier::someFaceWitnessIsOutside(
    PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB,
    const java::ArrayList<PolyhedralBoundedSolidFaceGeometry*>& planesB)
{
    int i;

    for ( i = 0; i < solidA->getPolygonsList().size(); i++ ) {
        Vector3Dd witness;
        if ( faceInteriorWitness(solidA->getPolygonsList().get(i),
                 &witness) &&
             classifyPointAgainstSolid(solidB, planesB, witness) ==
                 Geometry::OUTSIDE ) {
            return true;
        }
    }
    return false;
}

/**
A point strictly inside the face: the centroid of the first triangle of
consecutive outer-loop vertices whose centroid the face classifies as
INSIDE (holes and concave corners are rejected by that test).
C++ port note: the Java null answer is a false return value.
@return true and the witness in `out`, or false when no such triangle
exists
*/
bool Classifier::faceInteriorWitness(_PolyhedralBoundedSolidFace* face,
    Vector3Dd* out)
{
    _PolyhedralBoundedSolidLoop* loop;
    _PolyhedralBoundedSolidHalfEdge* he;
    InfinitePlane* plane;
    double eps = numericContext.bigEpsilon();

    if ( face == 0 || face->boundariesList.size() < 1 ) {
        return false;
    }
    plane = face->getContainingPlane();
    loop = face->boundariesList.get(0);
    if ( plane == 0 || loop == 0 ||
         loop->boundaryStartHalfEdge == 0 ) {
        delete plane;
        return false;
    }
    he = loop->boundaryStartHalfEdge;
    do {
        _PolyhedralBoundedSolidHalfEdge* n1 = he->next();
        if ( n1 == 0 || n1->next() == 0 ) {
            delete plane;
            return false;
        }
        Vector3Dd c = he->startingVertex->position
            .add(n1->startingVertex->position)
            .add(n1->next()->startingVertex->position)
            .multiply(1.0 / 3.0);
        if ( face->testPointInside(c, eps, plane) == Geometry::INSIDE ) {
            delete plane;
            *out = c;
            return true;
        }
        he = n1;
    } while ( he != loop->boundaryStartHalfEdge );
    delete plane;
    return false;
}

int Classifier::classifyNoIntersectionRelation(int aInB, int bInA)
{
    if ( aInB == Geometry::INSIDE ) {
        return NO_INT_RELATION_A_IN_B;
    }
    if ( bInA == Geometry::INSIDE ) {
        return NO_INT_RELATION_B_IN_A;
    }
    if ( aInB == Geometry::LIMIT || bInA == Geometry::LIMIT ) {
        return NO_INT_RELATION_TOUCHING;
    }
    return NO_INT_RELATION_DISJOINT;
}

bool Classifier::hasProperEdgeFaceIntersection(
    PolyhedralBoundedSolid* current,
    PolyhedralBoundedSolid* other)
{
    long int i;
    long int j;
    _PolyhedralBoundedSolidEdge* edge;
    _PolyhedralBoundedSolidFace* face;
    _PolyhedralBoundedSolidVertex* v1;
    _PolyhedralBoundedSolidVertex* v2;
    double d1;
    double d2;
    double d3;
    double t;
    int s1;
    int s2;
    Vector3Dd p;

    if ( current == 0 || other == 0 ) {
        return false;
    }

    FacePlanes otherFaces(other);

    for ( i = 0; i < current->getEdgesList().size(); i++ ) {
        edge = current->getEdgesList().get(i);
        if ( edge == 0 || edge->rightHalf == 0 || edge->leftHalf == 0 ) {
            continue;
        }
        v1 = edge->rightHalf->startingVertex;
        v2 = edge->leftHalf->startingVertex;
        if ( v1 == 0 || v2 == 0 ) {
            continue;
        }

        for ( j = 0; j < other->getPolygonsList().size(); j++ ) {
            face = other->getPolygonsList().get(j);
            if ( face == 0 ) {
                continue;
            }
            InfinitePlane* facePlane = otherFaces.list().get(j)->getPlane();
            if ( facePlane == 0 ) {
                continue;
            }

            d1 = facePlane->pointDistance(v1->position);
            d2 = facePlane->pointDistance(v2->position);
            s1 = compareToZero(d1);
            s2 = compareToZero(d2);

            if ( !((s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1)) ) {
                continue;
            }

            t = d1 / (d1 - d2);
            p = v1->position.add(
                v2->position.subtract(v1->position).multiply(t));
            d3 = facePlane->pointDistance(p);
            if ( compareToZero(d3) != 0 ) {
                continue;
            }

            if ( pointInFace(face, p) == Geometry::INSIDE ) {
                return true;
            }
        }
    }
    return false;
}

//= Coplanar faces overlap ==========================================

/**
True when a face of A and a face of B lie on the same plane with
opposite orientation and share a region of positive area: the solids
touch along a face (Fable5.2Analysis.md B-04). Two cubes sharing a whole
face are detected through the interior witness of one face, since none
of their vertices or edge midpoints is inside the other face.
*/
bool Classifier::hasOppositeCoplanarFaceAreaContact(
    PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB)
{
    int i;
    int j;

    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(solidA, solidB);
    setNumericContext(&context);
    for ( i = 0; i < solidA->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* faceA = solidA->getPolygonsList().get(i);
        for ( j = 0; j < solidB->getPolygonsList().size(); j++ ) {
            _PolyhedralBoundedSolidFace* faceB =
                solidB->getPolygonsList().get(j);
            if ( !coplanarFaces(faceA, faceB) ) {
                continue;
            }
            InfinitePlane* planeA = faceA->getContainingPlane();
            InfinitePlane* planeB = faceB->getContainingPlane();
            double dot = planeA->getNormal().dotProduct(planeB->getNormal());
            delete planeA;
            delete planeB;
            if ( dot >= 0.0 ) {
                continue;
            }
            if ( partialCoplanarFaceAreaOverlap(faceA, faceB) ||
                 witnessInsideFace(faceA, faceB) ||
                 witnessInsideFace(faceB, faceA) ) {
                return true;
            }
        }
    }
    return false;
}

bool Classifier::witnessInsideFace(
    _PolyhedralBoundedSolidFace* source,
    _PolyhedralBoundedSolidFace* target)
{
    Vector3Dd witness;

    return faceInteriorWitness(source, &witness) &&
        target->testPointInside(witness, numericContext.bigEpsilon()) ==
            Geometry::INSIDE;
}

java::ArrayList<Classifier::Polygon> Classifier::partialCoplanarFaceAreaOverlapPolygons(
    PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB)
{
    java::ArrayList<Polygon> polygons;
    long int i;
    long int j;

    if ( solidA == 0 || solidB == 0 ) {
        return polygons;
    }

    for ( i = 0; i < solidA->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* faceA = solidA->getPolygonsList().get(i);
        for ( j = 0; j < solidB->getPolygonsList().size(); j++ ) {
            _PolyhedralBoundedSolidFace* faceB = solidB->getPolygonsList().get(j);
            if ( coplanarFaces(faceA, faceB) &&
                 partialCoplanarFaceAreaOverlap(faceA, faceB) ) {
                Polygon polygon = coplanarFaceIntersectionPolygon(faceA, faceB);
                if ( polygon.size() >= 3 ) {
                    polygons.add(polygon);
                }
            }
        }
    }

    return polygons;
}

bool Classifier::coplanarFaces(_PolyhedralBoundedSolidFace* faceA,
    _PolyhedralBoundedSolidFace* faceB)
{
    if ( faceA == 0 || faceB == 0 ) {
        return false;
    }
    InfinitePlane* planeA = faceA->getContainingPlane();
    InfinitePlane* planeB = faceB->getContainingPlane();
    bool coplanar = false;

    if ( planeA != 0 && planeB != 0 &&
         PolyhedralBoundedSolidNumericPolicy::unitVectorsParallel(
             planeA->getNormal(), planeB->getNormal(), numericContext) &&
         faceB->boundariesList.size() >= 1 &&
         faceB->boundariesList.get(0)->boundaryStartHalfEdge != 0 ) {
        coplanar = std::fabs(planeA->pointDistance(
            faceB->boundariesList.get(0)->boundaryStartHalfEdge
                ->startingVertex->position)) <= numericContext.bigEpsilon();
    }
    delete planeA;
    delete planeB;
    return coplanar;
}

bool Classifier::partialCoplanarFaceAreaOverlap(
    _PolyhedralBoundedSolidFace* faceA,
    _PolyhedralBoundedSolidFace* faceB)
{
    if ( faceHasInteriorVertexOrEdgeMidpoint(faceA, faceB) ||
         faceHasInteriorVertexOrEdgeMidpoint(faceB, faceA) ) {
        return true;
    }

    return faceBoundariesCrossProperly(faceA, faceB);
}

Classifier::Polygon Classifier::coplanarFaceIntersectionPolygon(
    _PolyhedralBoundedSolidFace* faceA,
    _PolyhedralBoundedSolidFace* faceB)
{
    Polygon points;

    appendFaceVerticesInsideOther(points, faceA, faceB);
    appendFaceVerticesInsideOther(points, faceB, faceA);
    appendBoundaryIntersections(points, faceA, faceB);
    InfinitePlane* planeA = faceA->getContainingPlane();
    Vector3Dd planeNormalA = planeA->getNormal();
    delete planeA;
    sortCoplanarPolygon(points, planeNormalA);

    if ( coplanarPolygonAreaMagnitude(points, planeNormalA) <=
         numericContext.bigEpsilon() * numericContext.bigEpsilon() ) {
        points.clear();
    }

    return points;
}

void Classifier::appendFaceVerticesInsideOther(
    Polygon& points,
    _PolyhedralBoundedSolidFace* source,
    _PolyhedralBoundedSolidFace* target)
{
    long int i;

    for ( i = 0; i < source->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = source->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start;
        _PolyhedralBoundedSolidHalfEdge* he;

        if ( loop == 0 || loop->boundaryStartHalfEdge == 0 ) {
            continue;
        }
        start = loop->boundaryStartHalfEdge;
        he = start;
        do {
            if ( target->testPointInside(he->startingVertex->position,
                     numericContext.bigEpsilon()) != Geometry::OUTSIDE ) {
                appendUniquePoint(points, he->startingVertex->position);
            }
            he = he->next();
        } while ( he != 0 && he != start );
    }
}

void Classifier::appendBoundaryIntersections(
    Polygon& points,
    _PolyhedralBoundedSolidFace* faceA,
    _PolyhedralBoundedSolidFace* faceB)
{
    long int i;
    long int j;
    int dominantCoordinate = dominantCoordinateForFace(faceA);

    for ( i = 0; i < faceA->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loopA = faceA->boundariesList.get(i);
        if ( loopA == 0 ) {
            continue;
        }
        for ( j = 0; j < faceB->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loopB = faceB->boundariesList.get(j);
            if ( loopB != 0 ) {
                appendLoopIntersections(points, loopA, loopB,
                    dominantCoordinate);
            }
        }
    }
}

void Classifier::appendLoopIntersections(
    Polygon& points,
    _PolyhedralBoundedSolidLoop* loopA,
    _PolyhedralBoundedSolidLoop* loopB,
    int dominantCoordinate)
{
    long int i;
    long int j;

    for ( i = 0; i < loopA->halfEdgesList.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* heA = loopA->halfEdgesList.get(i);
        if ( heA == 0 || heA->next() == 0 ) {
            continue;
        }
        for ( j = 0; j < loopB->halfEdgesList.size(); j++ ) {
            _PolyhedralBoundedSolidHalfEdge* heB = loopB->halfEdgesList.get(j);
            if ( heB == 0 || heB->next() == 0 ) {
                continue;
            }
            appendSegmentIntersection(points, heA, heB, dominantCoordinate);
        }
    }
}

void Classifier::appendSegmentIntersection(
    Polygon& points,
    _PolyhedralBoundedSolidHalfEdge* heA,
    _PolyhedralBoundedSolidHalfEdge* heB,
    int dominantCoordinate)
{
    Vector2Dd a1 = projectPointTo2D(heA->startingVertex->position,
        dominantCoordinate);
    Vector2Dd a2 = projectPointTo2D(heA->next()->startingVertex->position,
        dominantCoordinate);
    Vector2Dd b1 = projectPointTo2D(heB->startingVertex->position,
        dominantCoordinate);
    Vector2Dd b2 = projectPointTo2D(heB->next()->startingVertex->position,
        dominantCoordinate);
    double den;
    double t;

    if ( !segmentsCrossProperly2D(a1, a2, b1, b2) ) {
        return;
    }

    Vector2Dd da(a2.x - a1.x, a2.y - a1.y);
    Vector2Dd db(b2.x - b1.x, b2.y - b1.y);
    Vector2Dd ba(b1.x - a1.x, b1.y - a1.y);
    den = cross2D(da, db);
    if ( std::fabs(den) <= numericContext.bigEpsilon() ) {
        return;
    }

    t = cross2D(ba, db) / den;
    appendUniquePoint(points, heA->startingVertex->position.add(
        heA->next()->startingVertex->position
            .subtract(heA->startingVertex->position).multiply(t)));
}

double Classifier::cross2D(const Vector2Dd& a, const Vector2Dd& b)
{
    return a.x*b.y - a.y*b.x;
}

void Classifier::appendUniquePoint(Polygon& points, const Vector3Dd& point)
{
    size_t i;

    for ( i = 0; i < points.size(); i++ ) {
        if ( PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                points[i], point, numericContext) ) {
            return;
        }
    }
    points.add(point);
}

void Classifier::sortCoplanarPolygon(Polygon& points, const Vector3Dd& normal)
{
    Vector3Dd center;
    Vector3Dd u;
    Vector3Dd v;
    Vector3Dd n;
    size_t i;

    if ( points.size() < 3 ) {
        return;
    }

    for ( i = 0; i < points.size(); i++ ) {
        center = center.add(points[i]);
    }
    center = center.multiply(1.0 / (double)points.size());

    n = normal.normalized();
    u = points[0].subtract(center);
    if ( u.length() <= numericContext.bigEpsilon() ) {
        return;
    }
    u = u.normalized();
    v = n.crossProduct(u).normalized();

    // As Java `Collections.sort`, a stable sort by angle around the center
    points.sort([&center, &u, &v](const Vector3Dd& p1, const Vector3Dd& p2) {
            Vector3Dd d1 = p1.subtract(center);
            Vector3Dd d2 = p2.subtract(center);
            double a1 = std::atan2(d1.dotProduct(v), d1.dotProduct(u));
            double a2 = std::atan2(d2.dotProduct(v), d2.dotProduct(u));
            return java::Double::compare(a1, a2) < 0;
        });
}

double Classifier::coplanarPolygonAreaMagnitude(const Polygon& points,
    const Vector3Dd& normal)
{
    Vector3Dd accumulator;
    size_t i;

    if ( points.size() < 3 ) {
        return 0.0;
    }

    for ( i = 0; i < points.size(); i++ ) {
        const Vector3Dd& p = points[i];
        const Vector3Dd& q = points[(i+1)%points.size()];
        accumulator = accumulator.add(p.crossProduct(q));
    }

    return std::fabs(accumulator.dotProduct(normal.normalized())) * 0.5;
}

double Classifier::polygonArea(const Polygon& points)
{
    double nx = 0.0;
    double ny = 0.0;
    double nz = 0.0;
    int i;

    for ( i = 0; i < points.size(); i++ ) {
        Vector3Dd p = points.get(i);
        Vector3Dd q = points.get((i + 1) % points.size());
        nx += (p.y() - q.y()) * (p.z() + q.z());
        ny += (p.z() - q.z()) * (p.x() + q.x());
        nz += (p.x() - q.x()) * (p.y() + q.y());
    }
    return 0.5 * java::Math::sqrt(nx * nx + ny * ny + nz * nz);
}

PolyhedralBoundedSolid* Classifier::createLaminaFromPolygon(
    const Polygon& points)
{
    PolyhedralBoundedSolid* solid;
    size_t i;

    solid = new PolyhedralBoundedSolid();
    if ( points.size() < 3 ) {
        return solid;
    }

    PolyhedralBoundedSolidEulerOperators::mvfs(solid, points[0], 1, 1);
    for ( i = 1; i < points.size(); i++ ) {
        PolyhedralBoundedSolidEulerOperators::smev(solid, 1, (int)i,
            (int)i + 1, points[i]);
    }
    PolyhedralBoundedSolidEulerOperators::smef(solid, 1, (int)points.size(),
        1, 2);
    return solid;
}

bool Classifier::faceHasInteriorVertexOrEdgeMidpoint(
    _PolyhedralBoundedSolidFace* source,
    _PolyhedralBoundedSolidFace* target)
{
    long int i;

    for ( i = 0; i < source->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = source->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start;
        _PolyhedralBoundedSolidHalfEdge* he;

        if ( loop == 0 || loop->boundaryStartHalfEdge == 0 ) {
            continue;
        }
        start = loop->boundaryStartHalfEdge;
        he = start;
        do {
            if ( target->testPointInside(he->startingVertex->position,
                     numericContext.bigEpsilon()) == Geometry::INSIDE ) {
                return true;
            }
            if ( he->next() != 0 ) {
                Vector3Dd midpoint = he->startingVertex->position.add(
                    he->next()->startingVertex->position
                        .subtract(he->startingVertex->position)
                        .multiply(0.5));
                if ( target->testPointInside(midpoint,
                         numericContext.bigEpsilon()) == Geometry::INSIDE ) {
                    return true;
                }
            }
            he = he->next();
        } while ( he != 0 && he != start );
    }

    return false;
}

bool Classifier::faceBoundariesCrossProperly(
    _PolyhedralBoundedSolidFace* faceA,
    _PolyhedralBoundedSolidFace* faceB)
{
    long int i;
    long int j;
    int dominantCoordinate = dominantCoordinateForFace(faceA);

    for ( i = 0; i < faceA->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loopA = faceA->boundariesList.get(i);
        if ( loopA == 0 ) {
            continue;
        }
        for ( j = 0; j < faceB->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loopB = faceB->boundariesList.get(j);
            if ( loopB != 0 &&
                 loopsCrossProperly(loopA, loopB, dominantCoordinate) ) {
                return true;
            }
        }
    }

    return false;
}

bool Classifier::loopsCrossProperly(_PolyhedralBoundedSolidLoop* loopA,
    _PolyhedralBoundedSolidLoop* loopB,
    int dominantCoordinate)
{
    long int i;
    long int j;

    for ( i = 0; i < loopA->halfEdgesList.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* heA = loopA->halfEdgesList.get(i);
        if ( heA == 0 || heA->next() == 0 ) {
            continue;
        }
        Vector2Dd a1 = projectPointTo2D(heA->startingVertex->position,
            dominantCoordinate);
        Vector2Dd a2 = projectPointTo2D(heA->next()->startingVertex->position,
            dominantCoordinate);

        for ( j = 0; j < loopB->halfEdgesList.size(); j++ ) {
            _PolyhedralBoundedSolidHalfEdge* heB = loopB->halfEdgesList.get(j);
            if ( heB == 0 || heB->next() == 0 ) {
                continue;
            }
            Vector2Dd b1 = projectPointTo2D(heB->startingVertex->position,
                dominantCoordinate);
            Vector2Dd b2 = projectPointTo2D(
                heB->next()->startingVertex->position, dominantCoordinate);
            if ( segmentsCrossProperly2D(a1, a2, b1, b2) ) {
                return true;
            }
        }
    }

    return false;
}

int Classifier::dominantCoordinateForFace(_PolyhedralBoundedSolidFace* face)
{
    InfinitePlane* plane = face->getContainingPlane();
    Vector3Dd n = plane->getNormal();
    delete plane;

    if ( std::fabs(n.x()) >= std::fabs(n.y()) &&
         std::fabs(n.x()) >= std::fabs(n.z()) ) {
        return 1;
    }
    if ( std::fabs(n.y()) >= std::fabs(n.x()) &&
         std::fabs(n.y()) >= std::fabs(n.z()) ) {
        return 2;
    }
    return 3;
}

Vector2Dd Classifier::projectPointTo2D(const Vector3Dd& in,
    int dominantCoordinate)
{
    if ( dominantCoordinate == 1 ) {
        return Vector2Dd(in.y(), in.z());
    }
    if ( dominantCoordinate == 2 ) {
        return Vector2Dd(in.x(), in.z());
    }
    return Vector2Dd(in.x(), in.y());
}

double Classifier::orientation2D(const Vector2Dd& a, const Vector2Dd& b,
    const Vector2Dd& c)
{
    return (b.x-a.x)*(c.y-a.y) - (b.y-a.y)*(c.x-a.x);
}

bool Classifier::segmentsCrossProperly2D(const Vector2Dd& a1,
    const Vector2Dd& a2, const Vector2Dd& b1, const Vector2Dd& b2)
{
    double o1 = orientation2D(a1, a2, b1);
    double o2 = orientation2D(a1, a2, b2);
    double o3 = orientation2D(b1, b2, a1);
    double o4 = orientation2D(b1, b2, a2);
    double tolerance = PolyhedralBoundedSolidNumericPolicy
        ::orientationTolerance2D(a1, a2, b1, numericContext);

    tolerance = java::Math::max(tolerance, PolyhedralBoundedSolidNumericPolicy
        ::orientationTolerance2D(a1, a2, b2, numericContext));
    tolerance = java::Math::max(tolerance, PolyhedralBoundedSolidNumericPolicy
        ::orientationTolerance2D(b1, b2, a1, numericContext));
    tolerance = java::Math::max(tolerance, PolyhedralBoundedSolidNumericPolicy
        ::orientationTolerance2D(b1, b2, a2, numericContext));

    return ((o1 > tolerance && o2 < -tolerance) ||
            (o1 < -tolerance && o2 > tolerance)) &&
           ((o3 > tolerance && o4 < -tolerance) ||
            (o3 < -tolerance && o4 > tolerance));
}
