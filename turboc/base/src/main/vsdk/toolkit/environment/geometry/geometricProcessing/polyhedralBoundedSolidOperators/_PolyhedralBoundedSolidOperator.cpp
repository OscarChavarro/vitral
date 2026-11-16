#include <float.h>
#include <math.h>
#include <stdio.h>

#include "java/lang/Boolean.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidIdNamespace.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

PolyhedralBoundedSolidNumericPolicy::ToleranceContext
    _PolyhedralBoundedSolidOperator::numericContext =
    PolyhedralBoundedSolidNumericPolicy::defaultContext();

_PolyhedralBoundedSolidIdNamespace* _PolyhedralBoundedSolidOperator::idNamespace =
    NULL;


/**
The outer loop of a face as `_PolyhedralBoundedSolidOperator::clearRingSide`
needs it (Java private nested class `_OuterLoopShadow`).
*/
class _OuterLoopShadow {
private:
    /** False when the loop has no start half-edge or is not closed. */
    BOOLEAN walkable;
    /** False when the face has no plane. */
    BOOLEAN planar;
    int dropAxis;
    ArrayList<Vector3Dd> points;
    ArrayList<double> u;
    ArrayList<double> v;

public:
    explicit _OuterLoopShadow(_PolyhedralBoundedSolidFace* f);
    double clearance(const Vector3Dd& point) const;
    int contains(const Vector3Dd& point) const;
};

/**
`_PolyhedralBoundedSolidOperator::clearRingSide` for many rings of the same
division (Java private nested class `_RingSideTest`).
*/
class _RingSideTest {
private:
    _PolyhedralBoundedSolidFace* firstFace;
    _PolyhedralBoundedSolidFace* secondFace;
    _OuterLoopShadow* shadow1;
    _OuterLoopShadow* shadow2;
    double tolerance;

    _RingSideTest(const _RingSideTest& other);
    _RingSideTest& operator=(const _RingSideTest& other);

public:
    _RingSideTest(_PolyhedralBoundedSolidFace* f1,
        _PolyhedralBoundedSolidFace* f2);
    ~_RingSideTest();
    int side(_PolyhedralBoundedSolidLoop* l);
};

/// Name of the system property that enables the pipeline traces
static const char* const TRACE_PIPELINE_SUMMARY = "vsdk.setop.tracePipelineSummary";

static BOOLEAN searchForEdge(CircularArrayList<_PolyhedralBoundedSolidEdge*>& l,
                   _PolyhedralBoundedSolidEdge* e)
{
    long i;

    for ( i = 0; i < l.size(); i++ ) {
        if ( l.get(i) == e ) return TRUE;
    }
    return FALSE;
}

static BOOLEAN searchForVertex(CircularArrayList<_PolyhedralBoundedSolidVertex*>& l,
                     _PolyhedralBoundedSolidVertex* v)
{
    long i;

    for ( i = 0; i < l.size(); i++ ) {
        if ( l.get(i) == v ) return TRUE;
    }
    return FALSE;
}

static const char* booleanName(BOOLEAN value)
{
    return value ? "true" : "false";
}


void _PolyhedralBoundedSolidOperator::setNumericContext(
    const PolyhedralBoundedSolidNumericPolicy::ToleranceContext* context)
{
    if ( context == NULL ) {
        numericContext = PolyhedralBoundedSolidNumericPolicy::defaultContext();
    }
    else {
        numericContext = *context;
    }
}

void _PolyhedralBoundedSolidOperator::setIdNamespace(
    _PolyhedralBoundedSolidIdNamespace* ns)
{
    idNamespace = ns;
}

BOOLEAN _PolyhedralBoundedSolidOperator::neighbor(
    _PolyhedralBoundedSolidHalfEdge* h1, _PolyhedralBoundedSolidHalfEdge* h2)
{
    return (h1->parentLoop->parentFace == h2->parentLoop->parentFace) &&
        ( (
          h1 == h1->parentEdge->rightHalf && h2 == h2->parentEdge->leftHalf
          ) ||
          (
          h1 == h1->parentEdge->leftHalf && h2 == h2->parentEdge->rightHalf
          ) );
}

void _PolyhedralBoundedSolidOperator::cleanup(PolyhedralBoundedSolid* s)
{
    long i;
    long j;
    _PolyhedralBoundedSolidFace* f;
    _PolyhedralBoundedSolidLoop* l;
    _PolyhedralBoundedSolidHalfEdge* he;

    for ( i = 0; i < s->getPolygonsList().size(); i++ ) {
        f = s->getPolygonsList().get(i);
        for ( j = 0; j < f->boundariesList.size(); j++ ) {
            l = f->boundariesList.get(j);
            he = l->boundaryStartHalfEdge;
            do {
                //
                if ( !searchForEdge(s->getEdgesList(), he->parentEdge) ) {
                    s->getEdgesList().add(he->parentEdge);
                }
                if ( !searchForVertex(s->getVerticesList(), he->startingVertex) ) {
                    s->getVerticesList().add(he->startingVertex);
                    he->startingVertex->emanatingHalfEdge = he;
                }
                //
                he = he->next();
            } while( he != l->boundaryStartHalfEdge );
        }
    }
}

void _PolyhedralBoundedSolidOperator::movefac(_PolyhedralBoundedSolidFace* f,
                                              PolyhedralBoundedSolid* s)
{
    _PolyhedralBoundedSolidLoop* l;
    _PolyhedralBoundedSolidHalfEdge* he;
    _PolyhedralBoundedSolidFace* f2;
    long i;

    if ( !canMoveFace(f) ) {
        return;
    }

    f->parentSolid->getPolygonsList().locateWindowAtElem(f);
    f->parentSolid->getPolygonsList().removeElemAtWindow();
    s->getPolygonsList().add(f);
    f->parentSolid = s;

    for ( i = 0; i < f->boundariesList.size(); i++ ) {
        l = f->boundariesList.get(i);
        he = l->boundaryStartHalfEdge;
        do {
            _PolyhedralBoundedSolidHalfEdge* mirror = he->mirrorHalfEdge();
            if ( mirror == NULL || mirror->parentLoop == NULL ||
                 mirror->parentLoop->parentFace == NULL ) {
                he = he->next();
                continue;
            }
            f2 = mirror->parentLoop->parentFace;
            if ( f2->parentSolid != s && canMoveFace(f2) ) {
                movefac(f2, s);
            }
            he = he->next();
        } while( he != l->boundaryStartHalfEdge );
    }
}

BOOLEAN _PolyhedralBoundedSolidOperator::canMoveFace(_PolyhedralBoundedSolidFace* f)
{
    long i;

    if ( f == NULL ) {
        return FALSE;
    }
    for ( i = 0; i < f->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* l = f->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start;
        _PolyhedralBoundedSolidHalfEdge* he;
        long guard;

        if ( l == NULL || l->boundaryStartHalfEdge == NULL ) {
            return FALSE;
        }
        start = l->boundaryStartHalfEdge;
        he = start;
        guard = 0;
        do {
            if ( he == NULL ||
                 he->parentEdge == NULL ||
                 he->parentLoop == NULL ||
                 he->startingVertex == NULL ||
                 he->mirrorHalfEdge() == NULL ||
                 he->next() == NULL ) {
                return FALSE;
            }
            he = he->next();
            guard++;
        } while ( he != start && guard <= l->halfEdgesList.size() + 1 );
        if ( he != start ) {
            return FALSE;
        }
    }
    return TRUE;
}

Vector3Dd _PolyhedralBoundedSolidOperator::bisector(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    Vector3Dd middle;
    Vector3Dd a;
    Vector3Dd b;

    a = (he->next())->startingVertex->position.subtract(he->startingVertex->position);
    b = (he->previous())->startingVertex->position.subtract(he->startingVertex->position);
    a = a.normalized();
    b = b.normalized();

    middle = he->startingVertex->position.add((a.add(b)).multiply(0.5));

    return middle;
}

/**
Moves those rings of `f1` that do not lie within its outer loop to
`f2`.
This procedure is used on the splitter and set operator algorithms to
ensure that after a face has been divided by a MEF, all loops will end up
in the correct halves.
This is an answer to problem [MANT1988].13.5. Its use in the context of
the splitter algorithm is briefly described on section [MANT1988].14.7.2.
*/
void _PolyhedralBoundedSolidOperator::laringmv(_PolyhedralBoundedSolidFace* f1,
                                               _PolyhedralBoundedSolidFace* f2)
{
    laringmv(f1, f2, NULL);
}

/**
As `laringmv(f1, f2)` but never moves `keep`. `join` passes the remainder
of an inner ring it has just divided: that remainder is the hole the
section polygon leaves in `f1`, while the divided-off part became the
outer loop of `f2` (the disk). Both coincide geometrically, so no
containment test can tell them apart; topology does (Fable5.2Analysis.md
defect B-01).
*/
void _PolyhedralBoundedSolidOperator::laringmv(_PolyhedralBoundedSolidFace* f1,
                                               _PolyhedralBoundedSolidFace* f2,
                                               _PolyhedralBoundedSolidLoop* keep)
{
    _PolyhedralBoundedSolidLoop* l;
    long i;

    // [MANT1988].13.5 moves the rings that are not inside the outer loop
    // of `f1`. A ring clearly inside `f1` only (clearRingSide) stays;
    // every other ring follows the legacy rule and moves to `f2`. A
    // containment test for every ring was tried (mythosPlan Phase 3) and
    // regressed reference flows: mid-connect loops are not simple
    // regions and pending strut rings usually sit on the dividing chord,
    // so only unambiguous geometry overrides the legacy rule.
    _RingSideTest sides(f1, f2);
    for ( i = 1; i < f1->boundariesList.size(); i++ ) {
        l = f1->boundariesList.get(i);
        if ( l == keep ) {
            continue;
        }
        if ( Boolean::getBoolean(TRACE_PIPELINE_SUMMARY) ) {
            traceRingMoveShadowDecision(f1, f2, l,
                String("laringmv MOVE wouldMove=") +
                booleanName(ringBelongsToOtherHalf(f1, f2, l)));
        }
        if ( sides.side(l) == 1 ) {
            continue;
        }
        if ( PolyhedralBoundedSolidEulerOperators::lringmv(f1->parentSolid, l, f2, FALSE) ) {
            i--;
        }
    }
}

/**
Decides on which side of a face division a pending ring belongs
([MANT1988].13.5, completed): after a `lmef` divides a face into
`f1` (keeping the old outer loop side) and `f2`, a ring belongs to the
half whose region geometrically contains its representative point.
Containment is evaluated by 2D parity against each half's outer loop on
the dominant projection plane. The three observed regimes (mythosPlan
Phase 3 shadow study):
- Chord division (regions disjoint): exactly one parity test is true
  — the ring follows it. This keeps cusp-region strut rings with
  their junction partners instead of stranding them.
- Interior closed curve (one region nested in the other): both tests
  are true for rings in the nested region; the nested half wins
  (decided by testing one vertex of f2's outer loop against f1's).
- Ring on the dividing boundary (zero clearance: the common
  single-chord pending strut): both tests are unreliable/false —
  the legacy behavior (move to `f2`) is preserved.
@param f1 face that kept the original outer loop side
@param f2 face created by the division
@param l pending ring currently parented by `f1`
@return true when the ring must move to `f2`
*/
BOOLEAN _PolyhedralBoundedSolidOperator::ringBelongsToOtherHalf(
    _PolyhedralBoundedSolidFace* f1,
    _PolyhedralBoundedSolidFace* f2,
    _PolyhedralBoundedSolidLoop* l)
{
    if ( f1 == NULL || f2 == NULL || l == NULL ||
         l->boundaryStartHalfEdge == NULL ||
         l->boundaryStartHalfEdge->startingVertex == NULL ) {
        return TRUE;
    }
    Vector3Dd point = l->boundaryStartHalfEdge->startingVertex->position;
    int insideF1 = outerLoopParityContains(f1, point);
    int insideF2 = outerLoopParityContains(f2, point);
    if ( insideF1 < 0 || insideF2 < 0 ) {
        return TRUE;
    }
    if ( insideF1 != insideF2 ) {
        return insideF2 == 1;
    }
    if ( insideF1 == 1 ) {
        // Nested halves: the ring belongs to the inner region. Decide
        // which outer loop is nested by testing a representative vertex
        // of f2's outer loop against f1's outer loop.
        _PolyhedralBoundedSolidLoop* f2Outer = f2->boundariesList.size() > 0
            ? f2->boundariesList.get(0) : NULL;
        if ( f2Outer == NULL || f2Outer->boundaryStartHalfEdge == NULL ||
             f2Outer->boundaryStartHalfEdge->startingVertex == NULL ) {
            return TRUE;
        }
        int f2NestedInF1 = outerLoopParityContains(f1,
            f2Outer->boundaryStartHalfEdge->startingVertex->position);
        if ( f2NestedInF1 < 0 ) {
            return TRUE;
        }
        return f2NestedInF1 == 1;
    }
    // Outside both (on the dividing boundary or degenerate): legacy.
    return TRUE;
}

/**
Side of a face division on which a ring clearly lies: 1 when its
representative point is inside the outer loop of `f1` only, 2 when
inside that of `f2` only, both times farther than the tolerance from
either outer loop; 0 otherwise (ambiguous: the caller keeps its legacy
rule). A ring clearly inside one region cannot belong to the other,
whatever the shape of the loops in the middle of the connect stage.
*/
int _PolyhedralBoundedSolidOperator::clearRingSide(
    _PolyhedralBoundedSolidFace* f1,
    _PolyhedralBoundedSolidFace* f2,
    _PolyhedralBoundedSolidLoop* l)
{
    if ( f1 == NULL || f2 == NULL ) {
        return 0;
    }
    _RingSideTest sides(f1, f2);
    return sides.side(l);
}

/**
The outer loop of a face as `clearRingSide` needs it: its vertices in
order and the coordinate dropped to project it. Built once per face, so
that testing many rings against the same division walks the loop and
computes the plane once (performance.md P-04). The answers are those of
`outerLoopClearance` and `outerLoopParityContains`.
*/
_OuterLoopShadow::_OuterLoopShadow(_PolyhedralBoundedSolidFace* f)
{
    _PolyhedralBoundedSolidLoop* outer = f->boundariesList.get(0);
    _PolyhedralBoundedSolidHalfEdge* start = NULL;
    if ( outer != NULL ) {
        start = outer->boundaryStartHalfEdge;
    }
    _PolyhedralBoundedSolidHalfEdge* he = start;
    BOOLEAN closed = start != NULL;
    int guard = 0;

    while ( closed ) {
        if ( he->startingVertex == NULL ) {
            closed = FALSE;
            break;
        }
        points.add(he->startingVertex->position);
        he = he->next();
        guard++;
        if ( he == NULL ) {
            closed = FALSE;
        }
        else if ( he == start || guard >= 100000 ) {
            break;
        }
    }
    walkable = closed;
    InfinitePlane* plane = f->getContainingPlane();
    planar = plane != NULL;
    int axis = 2;
    if ( planar ) {
        Vector3Dd normal = plane->getNormal();
        double ax = fabs(normal.x());
        double ay = fabs(normal.y());
        double az = fabs(normal.z());
        if ( ax >= ay && ax >= az ) {
            axis = 0;
        }
        else if ( ay >= ax && ay >= az ) {
            axis = 1;
        }
    }
    delete plane;
    dropAxis = axis;
    for ( guard = 0; guard < points.size(); guard++ ) {
        u.add(_PolyhedralBoundedSolidOperator::shadowProjectedU(
            points[guard], dropAxis));
        v.add(_PolyhedralBoundedSolidOperator::shadowProjectedV(
            points[guard], dropAxis));
    }
}

/**
As `outerLoopClearance`, with the same arithmetic on plain numbers: no
vector is allocated per segment.
*/
double _OuterLoopShadow::clearance(const Vector3Dd& point) const
{
    double best = DBL_MAX;
    double px = point.x();
    double py = point.y();
    double pz = point.z();
    long i;

    if ( !walkable ) {
        return 0.0;
    }
    for ( i = 0; i < points.size(); i++ ) {
        const Vector3Dd& a = points[i];
        const Vector3Dd& b = points[(i + 1) % points.size()];
        double abx = b.x() - a.x();
        double aby = b.y() - a.y();
        double abz = b.z() - a.z();
        double len2 = abx * abx + aby * aby + abz * abz;
        double t = 0.0;
        if ( len2 > 0.0 ) {
            t = ((px - a.x()) * abx + (py - a.y()) * aby +
                 (pz - a.z()) * abz) / len2;
        }
        t = Math::max(0.0, Math::min(1.0, t));
        double dx = px - (a.x() + abx * t);
        double dy = py - (a.y() + aby * t);
        double dz = pz - (a.z() + abz * t);
        best = Math::min(best, sqrt(dx * dx + dy * dy + dz * dz));
    }
    return best;
}

/**
As `outerLoopParityContains`: 1 / 0 parity result, or -1 when not
computable (Java returns a nullable `Boolean`).
*/
int _OuterLoopShadow::contains(const Vector3Dd& point) const
{
    double pu;
    double pv;
    BOOLEAN inside = FALSE;
    long i;

    if ( !planar || !walkable ) {
        return -1;
    }
    pu = _PolyhedralBoundedSolidOperator::shadowProjectedU(point, dropAxis);
    pv = _PolyhedralBoundedSolidOperator::shadowProjectedV(point, dropAxis);
    for ( i = 0; i < points.size(); i++ ) {
        long next = (i + 1) % points.size();
        if ( (v[i] > pv) != (v[next] > pv) ) {
            double crossingU = (u[next] - u[i]) * (pv - v[i]) /
                (v[next] - v[i]) + u[i];
            if ( pu < crossingU ) {
                inside = !inside;
            }
        }
    }
    if ( inside ) {
        return 1;
    }
    return 0;
}

/**
`clearRingSide` for many rings of the same division: the two outer loops
and the tolerance are prepared once. Moving a ring from one face to the
other does not change their outer loops, so one instance serves a whole
pass over the rings.
*/
_RingSideTest::_RingSideTest(_PolyhedralBoundedSolidFace* f1,
    _PolyhedralBoundedSolidFace* f2)
    : firstFace(f1), secondFace(f2), shadow1(NULL), shadow2(NULL),
      tolerance(0.0)
{
}

_RingSideTest::~_RingSideTest()
{
    delete shadow1;
    delete shadow2;
}

/** See `clearRingSide`. */
int _RingSideTest::side(_PolyhedralBoundedSolidLoop* l)
{
    if ( l == NULL || l->boundaryStartHalfEdge == NULL ||
         l->boundaryStartHalfEdge->startingVertex == NULL ||
         firstFace->boundariesList.size() == 0 ||
         secondFace->boundariesList.size() == 0 ||
         firstFace->boundariesList.get(0) == l ||
         secondFace->boundariesList.get(0) == l ) {
        return 0;
    }
    if ( shadow1 == NULL ) {
        shadow1 = new _OuterLoopShadow(firstFace);
        shadow2 = new _OuterLoopShadow(secondFace);
        tolerance = PolyhedralBoundedSolidNumericPolicy
            ::forSolid(firstFace->parentSolid).bigEpsilon();
    }
    Vector3Dd point = l->boundaryStartHalfEdge->startingVertex->position;
    if ( shadow1->clearance(point) <= tolerance ||
         shadow2->clearance(point) <= tolerance ) {
        return 0;
    }
    int in1 = shadow1->contains(point);
    int in2 = shadow2->contains(point);
    if ( in1 < 0 || in2 < 0 || in1 == in2 ) {
        return 0;
    }
    if ( in1 == 1 ) {
        return 1;
    }
    return 2;
}

/** Distance from `point` to the outer loop of `f` (3D segments). */
double _PolyhedralBoundedSolidOperator::outerLoopClearance(
    _PolyhedralBoundedSolidFace* f, const Vector3Dd& point)
{
    _PolyhedralBoundedSolidLoop* outer = f->boundariesList.get(0);
    _PolyhedralBoundedSolidHalfEdge* start = outer->boundaryStartHalfEdge;
    if ( start == NULL ) {
        return 0.0;
    }
    double best = DBL_MAX;
    _PolyhedralBoundedSolidHalfEdge* he = start;
    int guard = 0;
    do {
        _PolyhedralBoundedSolidHalfEdge* next = he->next();
        if ( next == NULL ) {
            return 0.0;
        }
        Vector3Dd a = he->startingVertex->position;
        Vector3Dd ab = next->startingVertex->position.subtract(a);
        double len2 = ab.dotProduct(ab);
        double t = 0.0;
        if ( len2 > 0.0 ) {
            t = point.subtract(a).dotProduct(ab) / len2;
        }
        t = Math::max(0.0, Math::min(1.0, t));
        best = Math::min(best,
            point.subtract(a.add(ab.multiply(t))).length());
        he = next;
        guard++;
    } while ( he != start && guard < 100000 );
    return best;
}

/**
2D parity (ray crossing) containment of a point against the outer loop
of a face, on the dominant projection plane of the face normal.
@param f face whose outer loop is tested
@param point point to classify
@return 1 / 0 parity result, or -1 when not computable (Java returns a
nullable `Boolean`)
*/
int _PolyhedralBoundedSolidOperator::outerLoopParityContains(
    _PolyhedralBoundedSolidFace* f,
    const Vector3Dd& point)
{
    if ( f == NULL || f->boundariesList.size() == 0 ||
         f->hasContainingPlane() == FALSE ) {
        return -1;
    }
    _PolyhedralBoundedSolidLoop* outerLoop = f->boundariesList.get(0);
    if ( outerLoop == NULL || outerLoop->boundaryStartHalfEdge == NULL ) {
        return -1;
    }

    Vector3Dd normal = f->containingPlaneNormal();
    double ax = Cpp98Support::abs(normal.x());
    double ay = Cpp98Support::abs(normal.y());
    double az = Cpp98Support::abs(normal.z());
    int dropAxis;
    if ( ax >= ay && ax >= az ) {
        dropAxis = 0;
    }
    else if ( ay >= ax && ay >= az ) {
        dropAxis = 1;
    }
    else {
        dropAxis = 2;
    }

    double pu = shadowProjectedU(point, dropAxis);
    double pv = shadowProjectedV(point, dropAxis);
    BOOLEAN inside = FALSE;
    int guard = 0;
    _PolyhedralBoundedSolidHalfEdge* start = outerLoop->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* he = start;
    do {
        _PolyhedralBoundedSolidHalfEdge* next = he->next();
        if ( next == NULL || he->startingVertex == NULL ||
             next->startingVertex == NULL ) {
            return -1;
        }
        double au = shadowProjectedU(he->startingVertex->position, dropAxis);
        double av = shadowProjectedV(he->startingVertex->position, dropAxis);
        double bu = shadowProjectedU(next->startingVertex->position, dropAxis);
        double bv = shadowProjectedV(next->startingVertex->position, dropAxis);
        if ( (av > pv) != (bv > pv) ) {
            double crossingU = (bu - au) * (pv - av) / (bv - av) + au;
            if ( pu < crossingU ) {
                inside = !inside;
            }
        }
        he = next;
        guard++;
    } while ( he != start && guard < 100000 );
    return inside ? 1 : 0;
}

/**
Diagnostic shadow for the ring redistribution decision (mythosPlan
Phase 3): computes — without changing behavior — whether the ring's
representative point lies inside `f1`'s outer loop (2D parity on the
dominant projection plane) and its clearance from the outer-loop edges,
then logs one line when the pipeline trace property is set. The legacy
behavior (move every ring to `f2`) is preserved by the caller; this
trace exists to map where that behavior is load-bearing versus where it
mis-parents pending null-edge strut rings (crescent-cusp faces).
@param f1 face whose rings are being redistributed
@param f2 destination face of the legacy unconditional move
@param l ring about to be moved
*/
void _PolyhedralBoundedSolidOperator::traceRingMoveShadowDecision(
    _PolyhedralBoundedSolidFace* f1,
    _PolyhedralBoundedSolidFace* f2,
    _PolyhedralBoundedSolidLoop* l,
    const String& site)
{
    if ( !Boolean::getBoolean(TRACE_PIPELINE_SUMMARY) ) {
        return;
    }
    if ( f1 == NULL || f2 == NULL || l == NULL ||
         f1->boundariesList.size() == 0 ||
         l->boundaryStartHalfEdge == NULL ||
         l->boundaryStartHalfEdge->startingVertex == NULL ||
         f1->hasContainingPlane() == FALSE ) {
#ifdef __TURBOC__
        // TCC 3.0 cannot destroy a String temporary selected by ?: inside a
        // varargs call.  Materialize both alternatives before printf.
        String f1Text = "?";
        String f2Text = "?";
        if ( f1 != NULL ) f1Text = Cpp98Support::toString(f1->id);
        if ( f2 != NULL ) f2Text = Cpp98Support::toString(f2->id);
        printf("[LARINGMV] f1=%s f2=%s ring=untestable site=%s\n",
            f1Text.c_str(), f2Text.c_str(), site.c_str());
#else
        printf("[LARINGMV] f1=%s f2=%s ring=untestable site=%s\n",
            f1 == NULL ? "?" : Cpp98Support::toString(f1->id).c_str(),
            f2 == NULL ? "?" : Cpp98Support::toString(f2->id).c_str(),
            site.c_str());
#endif
        return;
    }
    _PolyhedralBoundedSolidLoop* outerLoop = f1->boundariesList.get(0);
    if ( outerLoop == l || outerLoop->boundaryStartHalfEdge == NULL ) {
        printf("[LARINGMV] f1=%d f2=%d ring=outer? site=%s\n", f1->id, f2->id,
            site.c_str());
        return;
    }

    Vector3Dd normal = f1->containingPlaneNormal();
    double ax = Cpp98Support::abs(normal.x());
    double ay = Cpp98Support::abs(normal.y());
    double az = Cpp98Support::abs(normal.z());
    int dropAxis;
    if ( ax >= ay && ax >= az ) {
        dropAxis = 0;
    }
    else if ( ay >= ax && ay >= az ) {
        dropAxis = 1;
    }
    else {
        dropAxis = 2;
    }

    Vector3Dd point = l->boundaryStartHalfEdge->startingVertex->position;
    double pu = shadowProjectedU(point, dropAxis);
    double pv = shadowProjectedV(point, dropAxis);

    BOOLEAN inside = FALSE;
    double minClearance = DBL_MAX;
    int outerSize = 0;
    _PolyhedralBoundedSolidHalfEdge* start = outerLoop->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* he = start;
    BOOLEAN walkable = TRUE;
    do {
        _PolyhedralBoundedSolidHalfEdge* next = he->next();
        if ( next == NULL || he->startingVertex == NULL ||
             next->startingVertex == NULL ) {
            walkable = FALSE;
            break;
        }
        double au = shadowProjectedU(he->startingVertex->position, dropAxis);
        double av = shadowProjectedV(he->startingVertex->position, dropAxis);
        double bu = shadowProjectedU(next->startingVertex->position, dropAxis);
        double bv = shadowProjectedV(next->startingVertex->position, dropAxis);

        double eu = bu - au;
        double ev = bv - av;
        double lenSq = eu * eu + ev * ev;
        double t = 0.0;
        if ( lenSq > 0.0 ) {
            t = ((pu - au) * eu + (pv - av) * ev) / lenSq;
            if ( t < 0.0 ) {
                t = 0.0;
            }
            else if ( t > 1.0 ) {
                t = 1.0;
            }
        }
        double du = pu - (au + eu * t);
        double dv = pv - (av + ev * t);
        double clearance = sqrt(du * du + dv * dv);
        if ( clearance < minClearance ) {
            minClearance = clearance;
        }

        if ( (av > pv) != (bv > pv) ) {
            double crossingU = (bu - au) * (pv - av) / (bv - av) + au;
            if ( pu < crossingU ) {
                inside = !inside;
            }
        }
        outerSize++;
        he = next;
    } while ( he != start && outerSize < 100000 );

    long ringSize = l->halfEdgesList.size();
    String* position = point.toString();
    printf("[LARINGMV] f1=%d f2=%d ringV=%d ringSize=%ld outerSize=%d "
        "walkable=%s inside=%s clearance=%.6f p=%s site=%s\n",
        f1->id, f2->id, l->boundaryStartHalfEdge->startingVertex->id,
        ringSize, outerSize, booleanName(walkable), booleanName(inside),
        minClearance, position->c_str(), site.c_str());
    delete position;
}

double _PolyhedralBoundedSolidOperator::shadowProjectedU(const Vector3Dd& p,
                                                         int dropAxis)
{
    if ( dropAxis == 0 ) {
        return p.y();
    }
    return p.x();
}

double _PolyhedralBoundedSolidOperator::shadowProjectedV(const Vector3Dd& p,
                                                         int dropAxis)
{
    if ( dropAxis == 1 || dropAxis == 0 ) {
        return p.z();
    }
    return p.y();
}

/**
After `join` divides an inner ring of `oldf` with `lmef`, one of the two
coincident copies of the section polygon must stay in `oldf` as its hole
and the other must be the outer loop of `newf` (the disk). `lmef` puts
the h1 side in `newf` whatever its winding, so when the remainder left in
`oldf` is wound as a disk and `newf`'s outer loop is wound as a hole, the
two loops are exchanged. Returns the loop that is now the hole of `oldf`
(and must not be moved by laringmv), or null when the division does not
have that shape (Fable5.2Analysis.md defect B-01).
*/
_PolyhedralBoundedSolidLoop* _PolyhedralBoundedSolidOperator::holeLeftByDividedRing(
    _PolyhedralBoundedSolidFace* oldf,
    _PolyhedralBoundedSolidFace* newf,
    _PolyhedralBoundedSolidLoop* dividedInnerRing)
{
    if ( dividedInnerRing == NULL || newf == NULL ||
         dividedInnerRing->parentFace != oldf ||
         newf->boundariesList.size() < 1 ) {
        return NULL;
    }
    _PolyhedralBoundedSolidLoop* newOuter = newf->boundariesList.get(0);
    if ( Boolean::getBoolean(TRACE_PIPELINE_SUMMARY) ) {
        String dot = Double::toString(windingDot(dividedInnerRing, oldf));
        String dotVsOldf = Double::toString(windingDot(newOuter, oldf));
        printf("[RINGDIV] oldf=%d newf=%d remainder=%ld dot=%s newfLoops=%ld "
            "newOuter=%ld dotVsOldf=%s\n", oldf->id, newf->id,
            dividedInnerRing->halfEdgesList.size(), dot.c_str(),
            newf->boundariesList.size(), newOuter->halfEdgesList.size(),
            dotVsOldf.c_str());
    }
    if ( isWoundAsHole(dividedInnerRing, oldf) ) {
        return dividedInnerRing;
    }
    // Both copies are measured against oldf's normal: newf's own plane
    // follows whatever winding its outer loop got, so it cannot tell.
    if ( newf->boundariesList.size() != 1 ||
         !isWoundAsHole(newOuter, oldf) ||
         !isWoundAsDisk(dividedInnerRing, oldf) ) {
        return NULL;
    }
    PolyhedralBoundedSolid* s = oldf->parentSolid;
    // newf keeps one loop only while the exchange happens: first move the
    // disk in as outer loop, then send the hole piece to oldf.
    if ( !PolyhedralBoundedSolidEulerOperators::lringmv(s,
             dividedInnerRing, newf, TRUE) ) {
        return NULL;
    }
    if ( !PolyhedralBoundedSolidEulerOperators::lringmv(s, newOuter, oldf,
             FALSE) ) {
        return NULL;
    }
    return newOuter;
}

BOOLEAN _PolyhedralBoundedSolidOperator::isWoundAsDisk(
    _PolyhedralBoundedSolidLoop* loop, _PolyhedralBoundedSolidFace* face)
{
    return windingDot(loop, face) > VSDK::EPSILON * VSDK::EPSILON;
}

/**
True when `loop` is a loop of `face` whose winding (Newell normal) is
opposite to the face normal, i.e. it is wound as a hole. A loop with no
measurable area answers false.
*/
BOOLEAN _PolyhedralBoundedSolidOperator::isWoundAsHole(
    _PolyhedralBoundedSolidLoop* loop, _PolyhedralBoundedSolidFace* face)
{
    return windingDot(loop, face) < -VSDK::EPSILON * VSDK::EPSILON;
}

/** Newell normal of `loop` dotted with `face`'s normal (0 if unknown). */
double _PolyhedralBoundedSolidOperator::windingDot(
    _PolyhedralBoundedSolidLoop* loop, _PolyhedralBoundedSolidFace* face)
{
    if ( loop == NULL || face == NULL ) {
        return 0.0;
    }
    InfinitePlane* plane = face->getContainingPlane();
    if ( plane == NULL || loop->halfEdgesList.size() < 3 ) {
        delete plane;
        return 0.0;
    }
    double nx = 0.0;
    double ny = 0.0;
    double nz = 0.0;
    long i;
    long n = loop->halfEdgesList.size();
    for ( i = 0; i < n; i++ ) {
        Vector3Dd a = loop->halfEdgesList.get(i)->startingVertex->position;
        Vector3Dd b = loop->halfEdgesList.get((i + 1) % n)
            ->startingVertex->position;
        nx += (a.y() - b.y()) * (a.z() + b.z());
        ny += (a.z() - b.z()) * (a.x() + b.x());
        nz += (a.x() - b.x()) * (a.y() + b.y());
    }
    // C++ port note: Java computes the face plane again here; the first
    // one is kept, the result is the same
    Vector3Dd normal = plane->getNormal();
    delete plane;
    return nx * normal.x() + ny * normal.y() + nz * normal.z();
}

/**
Following section [MANT1988].14.7.2. and program [MANT1988].14.10.
*/
void _PolyhedralBoundedSolidOperator::join(_PolyhedralBoundedSolidHalfEdge* h1,
                                           _PolyhedralBoundedSolidHalfEdge* h2,
                                           BOOLEAN withDebug)
{
    PolyhedralBoundedSolidStatistics::recordJoinCall();
    _PolyhedralBoundedSolidFace* oldf;
    _PolyhedralBoundedSolidFace* newf;
    PolyhedralBoundedSolid* s;

    if ( withDebug ) {
        printf("       -> JOIN:\n");
        printf("          . H1: %s\n", h1->toString().c_str());
        printf("          . H2: %s\n", h2->toString().c_str());
    }

    oldf = h1->parentLoop->parentFace;
    newf = NULL;
    s = oldf->parentSolid;
    _PolyhedralBoundedSolidLoop* dividedInnerRing = NULL;
    if ( h1->parentLoop == h2->parentLoop ) {
        if ( oldf->boundariesList.size() > 1 &&
             oldf->boundariesList.get(0) != h1->parentLoop ) {
            dividedInnerRing = h1->parentLoop;
        }
        if ( h1->previous()->previous() != h2 ) {
            int fid1 = (idNamespace != NULL)
                ? idNamespace->nextFaceId(s)
                : s->getMaxFaceId() + 1;
            newf = PolyhedralBoundedSolidEulerOperators::lmef(s, h1, h2->next(), fid1);
        }
    }
    else {
        PolyhedralBoundedSolidEulerOperators::lmekr(s, h1, h2->next());
    }

    if ( h1->next()->next() != h2 ) {
        int fid2 = (idNamespace != NULL)
            ? idNamespace->nextFaceId(s)
            : s->getMaxFaceId() + 1;
        // The second division used to leave every ring in the face it
        // splits. That stranded strut rings far from their junction
        // partners, which the connector then had to rescue (pending item
        // 3 of Fable5.2Analysis.md). Moving every ring regressed
        // MOON_BLOCK; moving only rings that clearly lie in the new half
        // (below) removed every rescue of the reference corpus and the
        // box fuzz.
        _PolyhedralBoundedSolidFace* splitFace2 =
            h2->parentLoop->parentFace;
        _PolyhedralBoundedSolidFace* newFace2 =
            PolyhedralBoundedSolidEulerOperators::lmef(s, h2, h1->next(), fid2);
        if ( Boolean::getBoolean(TRACE_PIPELINE_SUMMARY) &&
             splitFace2 != NULL &&
             splitFace2->boundariesList.size() >= 2 ) {
            long shadowI;
            for ( shadowI = 1;
                  shadowI < splitFace2->boundariesList.size();
                  shadowI++ ) {
                traceRingMoveShadowDecision(splitFace2, newFace2,
                    splitFace2->boundariesList.get(shadowI),
                    String("lmef2 KEEP wouldMove=") +
                    booleanName(ringBelongsToOtherHalf(splitFace2, newFace2,
                        splitFace2->boundariesList.get(shadowI))));
            }
        }
        // Rings of the face divided by the second lmef that clearly lie
        // in the new half follow it ([MANT1988].13.5).
        if ( splitFace2 != NULL && newFace2 != NULL ) {
            long k;
            _RingSideTest sides(splitFace2, newFace2);
            for ( k = 1; k < splitFace2->boundariesList.size(); k++ ) {
                _PolyhedralBoundedSolidLoop* ring =
                    splitFace2->boundariesList.get(k);
                if ( sides.side(ring) == 2 &&
                     PolyhedralBoundedSolidEulerOperators::lringmv(s,
                         ring, newFace2, FALSE) ) {
                    k--;
                }
            }
        }
        if ( newf != NULL && oldf->boundariesList.size() >= 2 ) {
            // Rings are redistributed on both solids and for every
            // operation. Connect used to skip this on solid A unless
            // the operation was an intersection; the rings that then
            // stayed in the wrong half were what the ring rescue of
            // connect moved back later, and a hole already cut by
            // another curve was never moved back (plan-stage11 Phase
            // F).
            laringmv(oldf, newf,
                holeLeftByDividedRing(oldf, newf, dividedInnerRing));
        }
    }
}

BOOLEAN _PolyhedralBoundedSolidOperator::checkWideness(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    if ( he == NULL || he->parentLoop == NULL ||
         he->parentLoop->parentFace == NULL ||
         he->parentLoop->parentFace->hasContainingPlane() == FALSE ||
         he->previous() == NULL || he->next() == NULL ) {
        return TRUE;
    }

    Vector3Dd ref1;
    Vector3Dd ref2;
    Vector3Dd ref12;

    ref1 = he->previous()->startingVertex->position.subtract(
        he->startingVertex->position);
    ref2 = he->next()->startingVertex->position.subtract(
        he->startingVertex->position);
    ref12 = ref1.crossProduct(ref2);
    // Collinear edges: |ref1 x ref2| is |ref1| |ref2| sin(angle), so the
    // test compares the sine, not an area that shrinks with the model.
    if ( ref12.length() < VSDK::EPSILON * ref1.length() * ref2.length() ) {
        return TRUE;
    }
    return ref12.dotProduct(
        he->parentLoop->parentFace->containingPlaneNormal()) <= 0.0;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
