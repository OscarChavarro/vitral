#include "java/lang/Math.h"
#include "java/lang/String.h"
#include <cmath>
#include <cstdio>

#include "java/lang/Boolean.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef _PolyhedralBoundedSolidSetGeometricPredicateProcessor Processor;
typedef _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector OnSector;
typedef _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace OnFace;
typedef PolyhedralBoundedSolidNumericPolicy::ToleranceContext ToleranceContext;

thread_local java::ArrayList<Processor::SectoroverlapTraceEntry>*
    Processor::sectoroverlapTrace = nullptr;

namespace {

const double TWO_PI = 2.0 * M_PI;
const char* const TRACE_COPLANAR_TANGENTIAL_PROPERTY =
    "vsdk.setop.traceCoplanarTangential";

const int COPLANAR_OP_UNION = 0;
const int COPLANAR_OP_INTERSECTION = 1;
const int COPLANAR_OP_DIFFERENCE = 2;

const int COPLANAR_SIDE_A_VS_B = 0;
const int COPLANAR_SIDE_B_VS_A = 1;

const int COPLANAR_ORIENTATION_SAME = 0;
const int COPLANAR_ORIENTATION_OPPOSITE = 1;

/*
Coplanar overlap decision table for vertex/face classifier.
*/
const int COPLANAR_VERTEX_FACE_CLASS_TABLE[3][2][2] = {
    { { OnSector::OUT, OnSector::IN }, { OnSector::IN, OnSector::IN } },
    { { OnSector::IN, OnSector::OUT }, { OnSector::OUT, OnSector::OUT } },
    { { OnSector::IN, OnSector::OUT }, { OnSector::OUT, OnSector::OUT } }
};

/*
Coplanar overlap decision table for vertex/vertex classifier.
*/
const int COPLANAR_VERTEX_VERTEX_CLASS_TABLE[3][2][2] = {
    { { OnSector::OUT, OnSector::IN }, { OnSector::IN, OnSector::IN } },
    { { OnSector::IN, OnSector::OUT }, { OnSector::OUT, OnSector::OUT } },
    { { OnSector::IN, OnSector::OUT }, { OnSector::OUT, OnSector::OUT } }
};

/**
A `Vector3Dd` that may be missing (Java null).
*/
struct MaybeVector {
    bool valid;
    Vector3Dd v;

    MaybeVector() : valid(false) {}
    explicit MaybeVector(const Vector3Dd& v) : valid(true), v(v) {}
};

struct CoplanarAngleBasis {
    bool valid;
    Vector3Dd normal;
    Vector3Dd u;
    Vector3Dd v;

    CoplanarAngleBasis() : valid(false) {}
};

struct CoplanarAngularInterval {
    bool valid;
    double start;
    double end;
    double interior;

    CoplanarAngularInterval() : valid(false), start(0), end(0), interior(0) {}
};

bool isCoplanarTangentialTraceEnabled()
{
    return java::Boolean::getBoolean(TRACE_COPLANAR_TANGENTIAL_PROPERTY);
}

void traceCoplanarTangential(const java::String& message)
{
    if ( !isCoplanarTangentialTraceEnabled() ) {
        return;
    }
    printf("[SetOpCoplanarTrace] %s\n", message.c_str());
}

const char* booleanName(bool value)
{
    return value ? "true" : "false";
}

MaybeVector normalizedDirection(const MaybeVector& direction,
                                const ToleranceContext& context)
{
    if ( !direction.valid ) {
        return MaybeVector();
    }
    Vector3Dd out = direction.v;
    if ( out.length() <= context.unitVectorTolerance() ) {
        return MaybeVector();
    }
    return MaybeVector(out.normalized());
}

MaybeVector normalizedDirection(const Vector3Dd& direction,
                                const ToleranceContext& context)
{
    return normalizedDirection(MaybeVector(direction), context);
}

CoplanarAngleBasis buildCoplanarAngleBasis(
    const Vector3Dd& planeNormal,
    const MaybeVector& preferredDirection,
    const MaybeVector& fallbackDirection,
    const ToleranceContext& context)
{
    CoplanarAngleBasis basis;
    MaybeVector normal;
    MaybeVector u;
    MaybeVector v;

    normal = normalizedDirection(planeNormal, context);
    if ( !normal.valid ) {
        return basis;
    }

    u = normalizedDirection(preferredDirection, context);
    if ( !u.valid || normal.v.crossProduct(u.v).length() <=
         context.unitVectorTolerance() ) {
        u = normalizedDirection(fallbackDirection, context);
    }
    if ( !u.valid || normal.v.crossProduct(u.v).length() <=
         context.unitVectorTolerance() ) {
        if ( std::abs(normal.v.x()) < 0.9 ) {
            u = normalizedDirection(Vector3Dd(1.0, 0.0, 0.0)
                .subtract(normal.v.multiply(normal.v.x())), context);
        }
        else {
            u = normalizedDirection(Vector3Dd(0.0, 1.0, 0.0)
                .subtract(normal.v.multiply(normal.v.y())), context);
        }
    }
    if ( !u.valid ) {
        return basis;
    }

    v = normalizedDirection(normal.v.crossProduct(u.v), context);
    if ( !v.valid ) {
        return basis;
    }

    basis.valid = true;
    basis.normal = normal.v;
    basis.u = u.v;
    basis.v = v.v;
    return basis;
}

double angleOnBasis(const CoplanarAngleBasis& basis, const Vector3Dd& direction,
                    const ToleranceContext& context)
{
    MaybeVector d = normalizedDirection(direction, context);
    if ( !basis.valid || !d.valid ) {
        return 0.0;
    }
    return std::atan2(d.v.dotProduct(basis.v), d.v.dotProduct(basis.u));
}

double unwrapAngleNear(double angle, double reference)
{
    while ( angle - reference <= -M_PI ) {
        angle += TWO_PI;
    }
    while ( angle - reference > M_PI ) {
        angle -= TWO_PI;
    }
    return angle;
}

bool sectorContainsDirectionInclusive(const Vector3Dd& dir,
                                      const Vector3Dd& ref1,
                                      const Vector3Dd& ref2,
                                      const Vector3Dd& ref12)
{
    if ( Processor::colinearVectorsWithDirection(dir, ref1) ||
         Processor::colinearVectorsWithDirection(dir, ref2) ) {
        return true;
    }
    return Processor::sctrwitthin(dir, ref1, ref2, ref12);
}

MaybeVector acceptSectorInteriorProbe(const MaybeVector& candidate,
                                      const Vector3Dd& ref1,
                                      const Vector3Dd& ref2,
                                      const Vector3Dd& ref12,
                                      const ToleranceContext& context)
{
    MaybeVector normalized = normalizedDirection(candidate, context);
    if ( !normalized.valid ) {
        return MaybeVector();
    }
    if ( Processor::sctrwitthinProper(normalized.v, ref1, ref2, ref12) ) {
        return normalized;
    }
    if ( !Processor::colinearVectors(normalized.v, ref1) &&
         !Processor::colinearVectors(normalized.v, ref2) &&
         sectorContainsDirectionInclusive(normalized.v, ref1, ref2, ref12) ) {
        return normalized;
    }
    return MaybeVector();
}

MaybeVector selectSectorInteriorProbe(const Vector3Dd& ref1,
                                      const Vector3Dd& ref2,
                                      const Vector3Dd& ref12,
                                      const MaybeVector& fallbackProbe,
                                      const ToleranceContext& context)
{
    MaybeVector probe;
    Vector3Dd bisector;

    bisector = ref1.add(ref2);
    probe = acceptSectorInteriorProbe(MaybeVector(bisector), ref1, ref2, ref12,
        context);
    if ( probe.valid ) {
        return probe;
    }

    probe = acceptSectorInteriorProbe(MaybeVector(bisector.multiply(-1.0)),
        ref1, ref2, ref12, context);
    if ( probe.valid ) {
        return probe;
    }

    probe = acceptSectorInteriorProbe(MaybeVector(ref12.crossProduct(ref1)),
        ref1, ref2, ref12, context);
    if ( probe.valid ) {
        return probe;
    }

    probe = acceptSectorInteriorProbe(MaybeVector(ref2.crossProduct(ref12)),
        ref1, ref2, ref12, context);
    if ( probe.valid ) {
        return probe;
    }

    return acceptSectorInteriorProbe(fallbackProbe, ref1, ref2, ref12, context);
}

CoplanarAngularInterval buildCoplanarAngularInterval(
    const CoplanarAngleBasis& basis,
    const Vector3Dd& boundary1,
    const Vector3Dd& boundary2,
    const MaybeVector& interiorProbe,
    const ToleranceContext& context)
{
    CoplanarAngularInterval interval;
    MaybeVector probe;
    double t;

    if ( !basis.valid || !normalizedDirection(boundary1, context).valid ||
         !normalizedDirection(boundary2, context).valid ) {
        return interval;
    }

    probe = normalizedDirection(interiorProbe, context);
    if ( !probe.valid ) {
        return interval;
    }

    interval.valid = true;
    interval.interior = angleOnBasis(basis, probe.v, context);
    interval.start = unwrapAngleNear(angleOnBasis(basis, boundary1, context),
        interval.interior);
    interval.end = unwrapAngleNear(angleOnBasis(basis, boundary2, context),
        interval.interior);

    if ( interval.start > interval.end ) {
        t = interval.start;
        interval.start = interval.end;
        interval.end = t;
    }

    return interval;
}

CoplanarAngularInterval alignCoplanarInterval(
    const CoplanarAngularInterval& source, double referenceInterior)
{
    CoplanarAngularInterval aligned;
    double newInterior;
    double delta;

    if ( !source.valid ) {
        return aligned;
    }

    newInterior = unwrapAngleNear(source.interior, referenceInterior);
    delta = newInterior - source.interior;
    aligned.valid = true;
    aligned.start = source.start + delta;
    aligned.end = source.end + delta;
    aligned.interior = newInterior;
    return aligned;
}

int classifyCoplanarIntervalRelation(const CoplanarAngularInterval& a,
                                     const CoplanarAngularInterval& b,
                                     const ToleranceContext& context)
{
    CoplanarAngularInterval alignedB;
    double overlap;

    if ( !a.valid || !b.valid ) {
        return OnFace::COPLANAR_DISJOINT;
    }

    alignedB = alignCoplanarInterval(b, a.interior);
    overlap = java::Math::min(a.end, alignedB.end) -
        java::Math::max(a.start, alignedB.start);

    if ( overlap > context.angleTolerance() ) {
        return OnFace::COPLANAR_OVERLAP;
    }
    if ( overlap >= -context.angleTolerance() ) {
        return OnFace::COPLANAR_TOUCHING;
    }
    return OnFace::COPLANAR_DISJOINT;
}

CoplanarAngularInterval buildIntervalForHalfEdgeSector(
    const CoplanarAngleBasis& basis,
    _PolyhedralBoundedSolidHalfEdge* he,
    const ToleranceContext& context)
{
    Vector3Dd ref1;
    Vector3Dd ref2;
    MaybeVector probe;

    if ( he == nullptr || he->startingVertex == nullptr ||
         he->previous() == nullptr || he->next() == nullptr ||
         he->previous()->startingVertex == nullptr ||
         he->next()->startingVertex == nullptr ) {
        return CoplanarAngularInterval();
    }

    ref1 = he->previous()->startingVertex->position.subtract(
        he->startingVertex->position);
    ref2 = he->next()->startingVertex->position.subtract(
        he->startingVertex->position);
    probe = MaybeVector(_PolyhedralBoundedSolidSetOperator::inside(he));
    if ( probe.v.length() <= context.unitVectorTolerance() ) {
        probe = selectSectorInteriorProbe(ref1, ref2, ref1.crossProduct(ref2),
            MaybeVector(), context);
    }
    return buildCoplanarAngularInterval(basis, ref1, ref2, probe, context);
}

/**
Kept from Java (not used by the current classifiers).
*/
__attribute__((unused)) CoplanarAngularInterval buildIntervalForVertexSector(
    const CoplanarAngleBasis& basis,
    const Processor::SectorOnVertex* sector,
    const ToleranceContext& context)
{
    MaybeVector fallbackProbe;
    MaybeVector probe;

    if ( sector == nullptr ) {
        return CoplanarAngularInterval();
    }

    if ( sector->he != nullptr ) {
        fallbackProbe = MaybeVector(_PolyhedralBoundedSolidSetOperator::inside(sector->he));
    }
    probe = selectSectorInteriorProbe(sector->ref1, sector->ref2,
        sector->ref12, fallbackProbe, context);
    return buildCoplanarAngularInterval(basis, sector->ref1, sector->ref2,
        probe, context);
}

CoplanarAngularInterval buildIntervalForCoplanarEdge(
    const CoplanarAngleBasis& basis,
    _PolyhedralBoundedSolidHalfEdge* edge,
    const Vector3Dd& faceNormal,
    const ToleranceContext& context)
{
    Vector3Dd edgeDirection;
    Vector3Dd inward;

    if ( edge == nullptr || edge->startingVertex == nullptr ||
         edge->next() == nullptr || edge->next()->startingVertex == nullptr ) {
        return CoplanarAngularInterval();
    }

    edgeDirection = edge->next()->startingVertex->position.subtract(
        edge->startingVertex->position);
    inward = faceNormal.crossProduct(edgeDirection);
    return buildCoplanarAngularInterval(basis, edgeDirection,
        edgeDirection.multiply(-1.0), MaybeVector(inward), context);
}

int classifySectorAgainstReferenceVertex(
    const CoplanarAngularInterval& currentInterval,
    const CoplanarAngleBasis& basis,
    _PolyhedralBoundedSolidFace* referenceFace,
    _PolyhedralBoundedSolidVertex* referenceVertex,
    const ToleranceContext& context)
{
    long i;
    int j;
    int bestRelation;
    CoplanarAngularInterval referenceInterval;

    if ( !currentInterval.valid || referenceFace == nullptr ||
         referenceVertex == nullptr ) {
        return OnFace::COPLANAR_DISJOINT;
    }

    bestRelation = OnFace::COPLANAR_DISJOINT;

    for ( i = 0; i < referenceFace->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* he;
        _PolyhedralBoundedSolidHalfEdge* heStart;

        heStart = referenceFace->boundariesList.get(i)->boundaryStartHalfEdge;
        if ( heStart == nullptr ) {
            continue;
        }

        he = heStart;
        do {
            if ( he->startingVertex == referenceVertex ) {
                referenceInterval = buildIntervalForHalfEdgeSector(basis, he,
                    context);
                j = classifyCoplanarIntervalRelation(currentInterval,
                    referenceInterval, context);
                if ( j > bestRelation ) {
                    bestRelation = j;
                }
                if ( bestRelation == OnFace::COPLANAR_OVERLAP ) {
                    return bestRelation;
                }
            }
            he = he->next();
        } while ( he != heStart );
    }

    return bestRelation;
}

void registerCoplanarRelation(java::ArrayList<OnFace>& nbr, size_t index,
                              int relation)
{
    OnFace& n = nbr[index];
    if ( relation > n.coplanarRelation ) {
        n.coplanarRelation = relation;
    }
}

int coplanarOpIndex(int op)
{
    if ( op == _PolyhedralBoundedSolidOperator::UNION ) {
        return COPLANAR_OP_UNION;
    }
    if ( op == _PolyhedralBoundedSolidOperator::SUBTRACT ) {
        return COPLANAR_OP_DIFFERENCE;
    }
    return COPLANAR_OP_INTERSECTION;
}

int coplanarSideIndex(int BvsA)
{
    return (BvsA == 0) ? COPLANAR_SIDE_A_VS_B : COPLANAR_SIDE_B_VS_A;
}

int coplanarOrientationIndex(bool sameOrientation)
{
    return sameOrientation ? COPLANAR_ORIENTATION_SAME :
        COPLANAR_ORIENTATION_OPPOSITE;
}

}

const ToleranceContext& Processor::context()
{
    return numericContext;
}

void Processor::enableSectoroverlapTrace()
{
    delete sectoroverlapTrace;
    sectoroverlapTrace = new java::ArrayList<SectoroverlapTraceEntry>();
}

void Processor::disableSectoroverlapTrace()
{
    delete sectoroverlapTrace;
    sectoroverlapTrace = nullptr;
}

const java::ArrayList<Processor::SectoroverlapTraceEntry>*
Processor::getSectoroverlapTrace()
{
    return sectoroverlapTrace;
}

void Processor::recordSectoroverlapCall(const SectorOnVertex& na,
                                        const SectorOnVertex& nb,
                                        double a1, double a2, double b1,
                                        double b2, bool decision)
{
    java::ArrayList<SectoroverlapTraceEntry>* trace = sectoroverlapTrace;
    if ( trace == nullptr ) {
        return;
    }
    SectoroverlapTraceEntry entry;
    entry.callIndex = trace->size();
    entry.faceA = (na.he != nullptr && na.he->parentLoop != nullptr
                   && na.he->parentLoop->parentFace != nullptr)
        ? na.he->parentLoop->parentFace->id : -1;
    entry.faceB = (nb.he != nullptr && nb.he->parentLoop != nullptr
                   && nb.he->parentLoop->parentFace != nullptr)
        ? nb.he->parentLoop->parentFace->id : -1;
    entry.vertexAFrom = (na.he != nullptr && na.he->startingVertex != nullptr)
        ? na.he->startingVertex->id : -1;
    entry.vertexATo = -1;
    if ( na.he != nullptr && na.he->next() != nullptr
         && na.he->next()->startingVertex != nullptr ) {
        entry.vertexATo = na.he->next()->startingVertex->id;
    }
    entry.vertexBFrom = (nb.he != nullptr && nb.he->startingVertex != nullptr)
        ? nb.he->startingVertex->id : -1;
    entry.vertexBTo = -1;
    if ( nb.he != nullptr && nb.he->next() != nullptr
         && nb.he->next()->startingVertex != nullptr ) {
        entry.vertexBTo = nb.he->next()->startingVertex->id;
    }
    entry.a1 = a1; entry.a2 = a2;
    entry.b1 = b1; entry.b2 = b2;
    entry.diffA2B1 = a2 - b1;
    entry.diffB2A1 = b2 - a1;
    entry.boundaryRayContact = std::abs(entry.diffA2B1) < 1.0e-12
        || std::abs(entry.diffB2A1) < 1.0e-12;
    entry.decision = decision;
    trace->add(entry);
}

int Processor::compareToZero(double value)
{
    return PolyhedralBoundedSolidNumericPolicy::compareToZero(value,
        numericContext);
}

int Processor::pointInFace(_PolyhedralBoundedSolidFace* face,
                           const Vector3Dd& point)
{
    return face->testPointInside(point, numericContext.bigEpsilon());
}

_PolyhedralBoundedSolidFace::PointInsideResult Processor::pointInFaceDetailed(
    _PolyhedralBoundedSolidFace* face, const Vector3Dd& point)
{
    return face->testPointInsideDetailed(point, numericContext.bigEpsilon());
}

bool Processor::colinearVectors(const Vector3Dd& a, const Vector3Dd& b)
{
    return PolyhedralBoundedSolidNumericPolicy::vectorsColinear(a, b,
        numericContext);
}

bool Processor::colinearVectorsWithDirection(const Vector3Dd& a,
                                             const Vector3Dd& b)
{
    if ( PolyhedralBoundedSolidNumericPolicy::vectorsColinear(a, b,
             numericContext) ) {
        if ( a.dotProduct(b) >= 0 ) return true;
    }
    return false;
}

bool Processor::sctrwitthin(const Vector3Dd& dir, const Vector3Dd& ref1,
                            const Vector3Dd& ref2, const Vector3Dd& ref12)
{
    Vector3Dd c1;
    Vector3Dd c2;
    int t1;
    int t2;

    c1 = dir.crossProduct(ref1);
    if ( PolyhedralBoundedSolidNumericPolicy::vectorsColinear(
        dir, ref1, numericContext) ) {
        return (ref1.dotProduct(dir) > 0.0);
    }
    c2 = ref2.crossProduct(dir);
    if ( PolyhedralBoundedSolidNumericPolicy::vectorsColinear(
        ref2, dir, numericContext) ) {
        return (ref2.dotProduct(dir) > 0.0);
    }
    // Signs of unit quantities: the raw products grow with the cube of
    // the model size while the tolerance grows linearly, so at 10^-3
    // they were noise.
    t1 = PolyhedralBoundedSolidNumericPolicy::compare(
        unit(c1).dotProduct(unit(ref12)), 0.0,
        PolyhedralBoundedSolidNumericPolicy::BREP_EPSILON);
    t2 = PolyhedralBoundedSolidNumericPolicy::compare(
        unit(c2).dotProduct(unit(ref12)), 0.0,
        PolyhedralBoundedSolidNumericPolicy::BREP_EPSILON);
    return ( t1 < 0.0 && t2 < 0.0 );
}

Vector3Dd Processor::unit(const Vector3Dd& v)
{
    double length = v.length();
    return length > 0.0 ? v.multiply(1.0 / length) : v;
}

bool Processor::sctrwitthinProper(const Vector3Dd& dir, const Vector3Dd& ref1,
                                  const Vector3Dd& ref2, const Vector3Dd& ref12)
{
    if ( colinearVectors(dir, ref1) || colinearVectors(dir, ref2) ) {
        return false;
    }

    return sctrwitthin(dir, ref1, ref2, ref12);
}

bool Processor::sectoroverlap(const SectorOnVertex& na, const SectorOnVertex& nb,
                              bool withDebug)
{
    double a1;
    double a2;
    double b1;
    double b2;
    Vector3Dd u;
    Vector3Dd v;
    Vector3Dd a;
    Vector3Dd b;
    Vector3Dd c;
    Vector3Dd n;

    n = na.he->parentLoop->parentFace->containingPlaneNormal();
    u = na.ref1.normalized();
    v = n.crossProduct(u).normalized();

    a = na.ref2.normalized();
    b = nb.ref1.normalized();
    c = nb.ref2.normalized();

    a1 = angleFromVectors(u, v, u);
    a2 = angleFromVectors(u, v, a);
    b1 = angleFromVectors(u, v, b);
    b2 = angleFromVectors(u, v, c);

    if ( a1 > a2 ) {
        double t = a1;
        a1 = a2;
        a2 = t;
    }
    if ( b1 > b2 ) {
        double t = b1;
        b1 = b2;
        b2 = t;
    }

    // Closed-set (epsilon-tolerant) interval-overlap check on signed
    // angles. Intentionally returns true for touching sectors (a2 ≈ b1
    // or b2 ≈ a1) because the setop pipeline requires a null-edge strut
    // even at coplanar boundary-ray contact — a strict open-set check
    // breaks MANT1988 §15.1 reference geometry. Both ends are tested:
    // the check used to miss B entirely on the negative side of A.
    bool decision = (a2 + VSDK::EPSILON > b1 - VSDK::EPSILON) &&
                    (b2 + VSDK::EPSILON > a1 - VSDK::EPSILON);
    if ( decision && touchOnlyAlongConcaveEdge(na, nb) ) {
        decision = false;
    }
    recordSectoroverlapCall(na, nb, a1, a2, b1, b2, decision);
    if ( withDebug ) {
        printf(decision ? " <TRUE>" : " <FALSE>");
    }
    return decision;
}

/**
True when two coplanar sectors meet only along a boundary ray that is an
edge of both faces, and that edge is concave in at least one of the two
solids: a concave edge of one solid lying on an edge of the other, as at
the shared corner of two bars. The sectors lie on opposite sides of the
edge and share no area, so treating them as an "on" pair would propagate
a coplanar class across a face that does not touch the other solid.
Contact along a convex edge of both solids (blocks glued face to face)
and contact along the bisector that splits a wide sector keep the
closed-set behavior of [MANT1988].15.6.2.
*/
bool Processor::touchOnlyAlongConcaveEdge(const SectorOnVertex& na,
    const SectorOnVertex& nb)
{
    bool firstA[2] = { true, false };
    bool firstB[2] = { true, false };
    int i;
    int j;

    if ( sectorsShareInteriorDirection(na, nb) ) {
        return false;
    }
    for ( i = 0; i < 2; i++ ) {
        bool fa = firstA[i];
        for ( j = 0; j < 2; j++ ) {
            bool fb = firstB[j];
            Vector3Dd ra = fa ? na.ref1 : na.ref2;
            Vector3Dd rb = fb ? nb.ref1 : nb.ref2;
            if ( !sameDirection(ra, rb) ) {
                continue;
            }
            if ( !isFaceEdgeRay(na, ra) || !isFaceEdgeRay(nb, rb) ) {
                return false;
            }
            return isConcaveEdgeRay(na, fa) || isConcaveEdgeRay(nb, fb);
        }
    }
    return false;
}

bool Processor::sameDirection(const Vector3Dd& a, const Vector3Dd& b)
{
    Vector3Dd na = Vector3Dd(a).normalized();
    Vector3Dd nb = Vector3Dd(b).normalized();
    return na.dotProduct(nb) > 1.0 - VSDK::EPSILON;
}

bool Processor::isFaceEdgeRay(const SectorOnVertex& n, const Vector3Dd& ray)
{
    Vector3Dd origin = n.he->startingVertex->position;
    return sameDirection(ray,
               n.he->previous()->startingVertex->position.subtract(origin)) ||
           sameDirection(ray,
               n.he->next()->startingVertex->position.subtract(origin));
}

/**
Whether the edge along the sector's first (ref1, edge he.previous()) or
second (ref2, edge he) ray is concave: the sector's face, next to the
edge, lies in front of the plane of the face across the edge.
*/
bool Processor::isConcaveEdgeRay(const SectorOnVertex& n, bool firstRay)
{
    _PolyhedralBoundedSolidHalfEdge* edgeHalf;
    Vector3Dd ray;
    Vector3Dd other;
    Vector3Dd inside;
    Vector3Dd acrossNormal;

    edgeHalf = firstRay ? n.he->previous() : n.he;
    ray = firstRay ? n.ref1 : n.ref2;
    other = firstRay ? n.ref2 : n.ref1;
    if ( edgeHalf->mirrorHalfEdge() == nullptr ||
         edgeHalf->mirrorHalfEdge()->parentLoop == nullptr ) {
        return false;
    }
    InfinitePlane* acrossPlane = edgeHalf->mirrorHalfEdge()->parentLoop
        ->parentFace->getContainingPlane();
    acrossNormal = acrossPlane->getNormal();
    delete acrossPlane;
    inside = Vector3Dd(ray).normalized().add(
        Vector3Dd(other).normalized().multiply(1.0e-3));
    return inside.dotProduct(acrossNormal) > VSDK::EPSILON * 1.0e-3;
}

/**
Two coplanar sectors overlap when some direction lies strictly inside
both. Every sector is the minor arc from ref1 to ref2 (wide sectors are
split at their bisector by the neighborhood preprocessing), so it is
enough to test the middle direction of each gap between consecutive
boundary rays.
*/
bool Processor::sectorsShareInteriorDirection(const SectorOnVertex& na,
    const SectorOnVertex& nb)
{
    InfinitePlane* plane = na.he->parentLoop->parentFace->getContainingPlane();
    Vector3Dd n = plane->getNormal();
    delete plane;
    Vector3Dd u = Vector3Dd(na.ref1).normalized();
    Vector3Dd v = n.crossProduct(u).normalized();
    double angles[4];
    Vector3Dd rays[4] = { na.ref1, na.ref2, nb.ref1, nb.ref2 };
    int i;
    int j;

    for ( i = 0; i < 4; i++ ) {
        Vector3Dd r = Vector3Dd(rays[i]).normalized();
        angles[i] = std::atan2(r.dotProduct(v), r.dotProduct(u));
    }
    // java.util.Arrays.sort(double[]) (no NaN here): insertion sort.
    for ( i = 1; i < 4; i++ ) {
        double moving = angles[i];
        for ( j = i - 1; j >= 0 && angles[j] > moving; j-- ) {
            angles[j + 1] = angles[j];
        }
        angles[j + 1] = moving;
    }
    for ( i = 0; i < 4; i++ ) {
        double from = angles[i];
        double to = (i == 3) ? angles[0] + 2.0 * M_PI : angles[i + 1];
        double middle;
        Vector3Dd dir;

        if ( to - from < VSDK::EPSILON ) {
            continue;
        }
        middle = (from + to) / 2.0;
        dir = u.multiply(java::Math::cos(middle)).add(
            v.multiply(java::Math::sin(middle)));
        if ( sctrwitthinProper(dir, na.ref1, na.ref2, na.ref12) &&
             sctrwitthinProper(dir, nb.ref1, nb.ref2, nb.ref12) ) {
            return true;
        }
    }
    return false;
}

double Processor::angleFromVectors(const Vector3Dd& u, const Vector3Dd& v,
                                   const Vector3Dd& a)
{
    double x;
    double y;
    double angle;

    x = a.dotProduct(u);
    y = a.dotProduct(v);
    if ( x > 1.0 ) {
        x = 1.0;
    }
    else if ( x < -1.0 ) {
        x = -1.0;
    }

    angle = std::acos(x);
    if ( y < 0 ) {
        angle *= -1;
    }
    return angle;
}

int Processor::resolveCoplanarVertexVertexClass(int op, bool sameOrientation,
                                                bool sideA)
{
    int sideIndex = sideA ? 0 : 1;
    return COPLANAR_VERTEX_VERTEX_CLASS_TABLE[coplanarOpIndex(op)]
        [coplanarOrientationIndex(sameOrientation)]
        [sideIndex];
}

void Processor::applyCoplanarRulesToVertexFaceNeighborhood(
    java::ArrayList<SectorOnFace>& nbr,
    _PolyhedralBoundedSolidFace* referenceFace,
    InfinitePlane* referencePlane,
    int BvsA, int op,
    bool useMirrorFace)
{
    _PolyhedralBoundedSolidHalfEdge* he;
    _PolyhedralBoundedSolidFace* localFace;
    Vector3Dd c;
    double d;
    size_t i;
    size_t nnbr = nbr.size();
    int relation;
    int resolvedClass;
    bool sameOrientation;

    for ( i = 0; i < nnbr; i++ ) {
        SectorOnFace& current = nbr[i];
        he = current.sector;
        if ( he == nullptr || he->parentLoop == nullptr ||
             he->parentLoop->parentFace == nullptr ) {
            continue;
        }

        if ( useMirrorFace ) {
            _PolyhedralBoundedSolidHalfEdge* mirror = he->mirrorHalfEdge();
            if ( mirror == nullptr || mirror->parentLoop == nullptr ||
                 mirror->parentLoop->parentFace == nullptr ) {
                continue;
            }
            localFace = mirror->parentLoop->parentFace;
        }
        else {
            localFace = he->parentLoop->parentFace;
        }
        if ( localFace == nullptr || localFace->hasContainingPlane() == false ||
             referencePlane == nullptr ) {
            continue;
        }

        c = localFace->containingPlaneNormal().crossProduct(
            referencePlane->getNormal());
        d = c.dotProduct(c);
        if ( compareToZero(d) != 0 ) {
            continue;
        }

        relation = classifyCoplanarSectorRelation(&current, referenceFace);
        registerCoplanarRelation(nbr, i, relation);
        registerCoplanarRelation(nbr, (i+1)%nnbr, relation);
        traceCoplanarTangential(
            "vertexFace coplanar relation op=" + java::String::valueOf(op) +
            " side=" + java::String::valueOf(BvsA) +
            " face=" + java::String::valueOf(referenceFace->id) +
            " localFace=" + java::String::valueOf(localFace->id) +
            " sectorIndex=" + java::String::valueOf(i) +
            " relation=" + java::String::valueOf(relation));

        if ( relation == SectorOnFace::COPLANAR_OVERLAP ) {
            d = localFace->containingPlaneNormal().dotProduct(
                referencePlane->getNormal());
            sameOrientation = (compareToZero(d) == 1);
            resolvedClass = resolveCoplanarSectorClass(op, BvsA,
                sameOrientation);
            traceCoplanarTangential(
                java::String("  resolved vertexFace coplanar overlap sameOrientation=") +
                booleanName(sameOrientation) + " class=" +
                java::String::valueOf(resolvedClass));
            nbr[i].classification = resolvedClass;
            nbr[(i+1)%nnbr].classification = resolvedClass;
        }
    }
}

int Processor::classifyCoplanarSectorRelation(
    const SectorOnFace* sectorInfo,
    _PolyhedralBoundedSolidFace* referenceFace)
{
    _PolyhedralBoundedSolidHalfEdge* he;
    Vector3Dd start;
    CoplanarAngleBasis basis;
    CoplanarAngularInterval currentInterval;
    int status;
    _PolyhedralBoundedSolidHalfEdge* intersectedHalfedge;
    _PolyhedralBoundedSolidVertex* intersectedVertex;

    if ( sectorInfo == nullptr || referenceFace == nullptr ) {
        return SectorOnFace::COPLANAR_DISJOINT;
    }

    he = sectorInfo->sector;
    if ( he == nullptr || he->startingVertex == nullptr ) {
        return SectorOnFace::COPLANAR_DISJOINT;
    }

    start = he->startingVertex->position;
    _PolyhedralBoundedSolidFace::PointInsideResult containment =
        pointInFaceDetailed(referenceFace, start);
    status = containment.status();

    if ( status == Geometry::INSIDE ) {
        return SectorOnFace::COPLANAR_OVERLAP;
    }
    if ( status != Geometry::LIMIT ) {
        return SectorOnFace::COPLANAR_DISJOINT;
    }

    basis = buildCoplanarAngleBasis(
        he->parentLoop->parentFace->containingPlaneNormal(),
        MaybeVector(he->next()->startingVertex->position.subtract(start)),
        MaybeVector(he->previous()->startingVertex->position.subtract(start)),
        numericContext);
    currentInterval = buildIntervalForHalfEdgeSector(basis, he, numericContext);
    if ( !currentInterval.valid ) {
        return SectorOnFace::COPLANAR_TOUCHING;
    }

    intersectedHalfedge = containment.intersectedHalfedge();
    if ( intersectedHalfedge != nullptr ) {
        return classifyCoplanarIntervalRelation(currentInterval,
            buildIntervalForCoplanarEdge(basis,
                intersectedHalfedge,
                referenceFace->containingPlaneNormal(),
                numericContext),
            numericContext);
    }
    intersectedVertex = containment.intersectedVertex();
    if ( intersectedVertex != nullptr ) {
        status = classifySectorAgainstReferenceVertex(currentInterval, basis,
            referenceFace, intersectedVertex, numericContext);
        if ( status != SectorOnFace::COPLANAR_DISJOINT ) {
            return status;
        }
    }
    return SectorOnFace::COPLANAR_TOUCHING;
}

int Processor::resolveCoplanarSectorClass(int op, int BvsA,
                                          bool sameOrientation)
{
    return COPLANAR_VERTEX_FACE_CLASS_TABLE[coplanarOpIndex(op)]
        [coplanarSideIndex(BvsA)]
        [coplanarOrientationIndex(sameOrientation)];
}
