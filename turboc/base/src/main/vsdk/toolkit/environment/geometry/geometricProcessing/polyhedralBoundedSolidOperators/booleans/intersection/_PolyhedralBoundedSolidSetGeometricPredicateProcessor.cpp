#include <algorithm>
#include <math.h>
#include <stdio.h>
#include <string>

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
#include "vsdk/toolkit/common/Cpp98Support.h"

typedef _PolyhedralBoundedSolidSetGeometricPredicateProcessor Processor;
typedef _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector OnSector;
typedef _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace OnFace;
typedef PolyhedralBoundedSolidNumericPolicy::ToleranceContext ToleranceContext;

std::vector<Processor::SectoroverlapTraceEntry>* Processor::sectoroverlapTrace = NULL;
int Processor::sectoroverlapCallCounter = 0;


static const double TWO_PI = 2.0 * M_PI;
static const char* const TRACE_COPLANAR_TANGENTIAL_PROPERTY =
    "vsdk.setop.traceCoplanarTangential";

static const int COPLANAR_OP_UNION = 0;
static const int COPLANAR_OP_INTERSECTION = 1;
static const int COPLANAR_OP_DIFFERENCE = 2;

static const int COPLANAR_SIDE_A_VS_B = 0;
static const int COPLANAR_SIDE_B_VS_A = 1;

static const int COPLANAR_ORIENTATION_SAME = 0;
static const int COPLANAR_ORIENTATION_OPPOSITE = 1;

/*
Coplanar overlap decision table for vertex/face classifier.
*/
static const int COPLANAR_VERTEX_FACE_CLASS_TABLE[3][2][2] = {
    { { OnSector::OUT, OnSector::IN }, { OnSector::IN, OnSector::IN } },
    { { OnSector::IN, OnSector::OUT }, { OnSector::OUT, OnSector::OUT } },
    { { OnSector::IN, OnSector::OUT }, { OnSector::OUT, OnSector::OUT } }
};

/*
Coplanar overlap decision table for vertex/vertex classifier.
*/
static const int COPLANAR_VERTEX_VERTEX_CLASS_TABLE[3][2][2] = {
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

static bool isCoplanarTangentialTraceEnabled()
{
    return Boolean::getBoolean(TRACE_COPLANAR_TANGENTIAL_PROPERTY);
}

static void traceCoplanarTangential(const std::string& message)
{
    if ( !isCoplanarTangentialTraceEnabled() ) {
        return;
    }
    printf("[SetOpCoplanarTrace] %s\n", message.c_str());
}

static const char* booleanName(bool value)
{
    return value ? "true" : "false";
}

static MaybeVector normalizedDirection(const MaybeVector& direction,
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

static MaybeVector normalizedDirection(const Vector3Dd& direction,
                                const ToleranceContext& context)
{
    return normalizedDirection(MaybeVector(direction), context);
}

static CoplanarAngleBasis buildCoplanarAngleBasis(
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
        if ( Cpp98Support::abs(normal.v.x()) < 0.9 ) {
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

static double angleOnBasis(const CoplanarAngleBasis& basis, const Vector3Dd& direction,
                    const ToleranceContext& context)
{
    MaybeVector d = normalizedDirection(direction, context);
    if ( !basis.valid || !d.valid ) {
        return 0.0;
    }
    return atan2(d.v.dotProduct(basis.v), d.v.dotProduct(basis.u));
}

static double unwrapAngleNear(double angle, double reference)
{
    while ( angle - reference <= -M_PI ) {
        angle += TWO_PI;
    }
    while ( angle - reference > M_PI ) {
        angle -= TWO_PI;
    }
    return angle;
}

static bool sectorContainsDirectionInclusive(const Vector3Dd& dir,
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

static MaybeVector acceptSectorInteriorProbe(const MaybeVector& candidate,
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

static MaybeVector selectSectorInteriorProbe(const Vector3Dd& ref1,
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

static CoplanarAngularInterval buildCoplanarAngularInterval(
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

static CoplanarAngularInterval alignCoplanarInterval(
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

static int classifyCoplanarIntervalRelation(const CoplanarAngularInterval& a,
                                     const CoplanarAngularInterval& b,
                                     const ToleranceContext& context)
{
    CoplanarAngularInterval alignedB;
    double overlap;

    if ( !a.valid || !b.valid ) {
        return OnFace::COPLANAR_DISJOINT;
    }

    alignedB = alignCoplanarInterval(b, a.interior);
    overlap = std::min(a.end, alignedB.end) -
        std::max(a.start, alignedB.start);

    if ( overlap > context.angleTolerance() ) {
        return OnFace::COPLANAR_OVERLAP;
    }
    if ( overlap >= -context.angleTolerance() ) {
        return OnFace::COPLANAR_TOUCHING;
    }
    return OnFace::COPLANAR_DISJOINT;
}

static CoplanarAngularInterval buildIntervalForHalfEdgeSector(
    const CoplanarAngleBasis& basis,
    _PolyhedralBoundedSolidHalfEdge* he,
    const ToleranceContext& context)
{
    Vector3Dd ref1;
    Vector3Dd ref2;
    MaybeVector probe;

    if ( he == NULL || he->startingVertex == NULL ||
         he->previous() == NULL || he->next() == NULL ||
         he->previous()->startingVertex == NULL ||
         he->next()->startingVertex == NULL ) {
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
static __attribute__((unused)) CoplanarAngularInterval buildIntervalForVertexSector(
    const CoplanarAngleBasis& basis,
    const Processor::SectorOnVertex* sector,
    const ToleranceContext& context)
{
    MaybeVector fallbackProbe;
    MaybeVector probe;

    if ( sector == NULL ) {
        return CoplanarAngularInterval();
    }

    if ( sector->he != NULL ) {
        fallbackProbe = MaybeVector(_PolyhedralBoundedSolidSetOperator::inside(sector->he));
    }
    probe = selectSectorInteriorProbe(sector->ref1, sector->ref2,
        sector->ref12, fallbackProbe, context);
    return buildCoplanarAngularInterval(basis, sector->ref1, sector->ref2,
        probe, context);
}

static CoplanarAngularInterval buildIntervalForCoplanarEdge(
    const CoplanarAngleBasis& basis,
    _PolyhedralBoundedSolidHalfEdge* edge,
    const Vector3Dd& faceNormal,
    const ToleranceContext& context)
{
    Vector3Dd edgeDirection;
    Vector3Dd inward;

    if ( edge == NULL || edge->startingVertex == NULL ||
         edge->next() == NULL || edge->next()->startingVertex == NULL ) {
        return CoplanarAngularInterval();
    }

    edgeDirection = edge->next()->startingVertex->position.subtract(
        edge->startingVertex->position);
    inward = faceNormal.crossProduct(edgeDirection);
    return buildCoplanarAngularInterval(basis, edgeDirection,
        edgeDirection.multiply(-1.0), MaybeVector(inward), context);
}

static int classifySectorAgainstReferenceVertex(
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

    if ( !currentInterval.valid || referenceFace == NULL ||
         referenceVertex == NULL ) {
        return OnFace::COPLANAR_DISJOINT;
    }

    bestRelation = OnFace::COPLANAR_DISJOINT;

    for ( i = 0; i < referenceFace->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* he;
        _PolyhedralBoundedSolidHalfEdge* heStart;

        heStart = referenceFace->boundariesList.get(i)->boundaryStartHalfEdge;
        if ( heStart == NULL ) {
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

static void registerCoplanarRelation(std::vector<OnFace>& nbr, size_t index,
                              int relation)
{
    OnFace& n = nbr[index];
    if ( relation > n.coplanarRelation ) {
        n.coplanarRelation = relation;
    }
}

static int coplanarOpIndex(int op)
{
    if ( op == _PolyhedralBoundedSolidOperator::UNION ) {
        return COPLANAR_OP_UNION;
    }
    if ( op == _PolyhedralBoundedSolidOperator::SUBTRACT ) {
        return COPLANAR_OP_DIFFERENCE;
    }
    return COPLANAR_OP_INTERSECTION;
}

static int coplanarSideIndex(int BvsA)
{
    return (BvsA == 0) ? COPLANAR_SIDE_A_VS_B : COPLANAR_SIDE_B_VS_A;
}

static int coplanarOrientationIndex(bool sameOrientation)
{
    return sameOrientation ? COPLANAR_ORIENTATION_SAME :
        COPLANAR_ORIENTATION_OPPOSITE;
}


const ToleranceContext& Processor::context()
{
    return numericContext;
}

void Processor::enableSectoroverlapTrace()
{
    delete sectoroverlapTrace;
    sectoroverlapTrace = new std::vector<SectoroverlapTraceEntry>();
    sectoroverlapCallCounter = 0;
}

void Processor::disableSectoroverlapTrace()
{
    delete sectoroverlapTrace;
    sectoroverlapTrace = NULL;
}

const std::vector<Processor::SectoroverlapTraceEntry>*
Processor::getSectoroverlapTrace()
{
    return sectoroverlapTrace;
}

void Processor::recordSectoroverlapCall(const SectorOnVertex& na,
                                        const SectorOnVertex& nb,
                                        double a1, double a2, double b1,
                                        double b2, bool decision)
{
    if ( sectoroverlapTrace == NULL ) {
        return;
    }
    SectoroverlapTraceEntry entry;
    entry.callIndex = sectoroverlapCallCounter++;
    entry.faceA = (na.he != NULL && na.he->parentLoop != NULL
                   && na.he->parentLoop->parentFace != NULL)
        ? na.he->parentLoop->parentFace->id : -1;
    entry.faceB = (nb.he != NULL && nb.he->parentLoop != NULL
                   && nb.he->parentLoop->parentFace != NULL)
        ? nb.he->parentLoop->parentFace->id : -1;
    entry.vertexAFrom = (na.he != NULL && na.he->startingVertex != NULL)
        ? na.he->startingVertex->id : -1;
    entry.vertexATo = -1;
    if ( na.he != NULL && na.he->next() != NULL
         && na.he->next()->startingVertex != NULL ) {
        entry.vertexATo = na.he->next()->startingVertex->id;
    }
    entry.vertexBFrom = (nb.he != NULL && nb.he->startingVertex != NULL)
        ? nb.he->startingVertex->id : -1;
    entry.vertexBTo = -1;
    if ( nb.he != NULL && nb.he->next() != NULL
         && nb.he->next()->startingVertex != NULL ) {
        entry.vertexBTo = nb.he->next()->startingVertex->id;
    }
    entry.a1 = a1; entry.a2 = a2;
    entry.b1 = b1; entry.b2 = b2;
    entry.diffA2B1 = a2 - b1;
    entry.diffB2A1 = b2 - a1;
    entry.boundaryRayContact = Cpp98Support::abs(entry.diffA2B1) < 1.0e-12
        || Cpp98Support::abs(entry.diffB2A1) < 1.0e-12;
    entry.decision = decision;
    sectoroverlapTrace->push_back(entry);
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
    t1 = compareToZero(c1.dotProduct(ref12));
    t2 = compareToZero(c2.dotProduct(ref12));
    return ( t1 < 0.0 && t2 < 0.0 );
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

    n = na.he->parentLoop->parentFace->getContainingPlane()->getNormal();
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

    // Closed-set (epsilon-tolerant) interval-overlap check.
    // Intentionally returns true for touching sectors (a2 ≈ b1) because the
    // setop pipeline requires a null-edge strut even at coplanar boundary-ray
    // contact — a strict open-set check breaks MANT1988 §15.1 reference geometry.
    // The symmetric disjoint case (B entirely left of A) is not yet fixed here;
    // that requires a deeper restructuring of the coplanar V/V path.
    bool decision = (a2 + VSDK::EPSILON > b1 - VSDK::EPSILON);
    recordSectoroverlapCall(na, nb, a1, a2, b1, b2, decision);
    if ( withDebug ) {
        printf(decision ? " <TRUE>" : " <FALSE>");
    }
    return decision;
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

    angle = acos(x);
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
    std::vector<SectorOnFace>& nbr,
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
        if ( he == NULL || he->parentLoop == NULL ||
             he->parentLoop->parentFace == NULL ) {
            continue;
        }

        if ( useMirrorFace ) {
            _PolyhedralBoundedSolidHalfEdge* mirror = he->mirrorHalfEdge();
            if ( mirror == NULL || mirror->parentLoop == NULL ||
                 mirror->parentLoop->parentFace == NULL ) {
                continue;
            }
            localFace = mirror->parentLoop->parentFace;
        }
        else {
            localFace = he->parentLoop->parentFace;
        }
        if ( localFace == NULL || localFace->getContainingPlane() == NULL ||
             referencePlane == NULL ) {
            continue;
        }

        c = localFace->getContainingPlane()->getNormal().crossProduct(
            referencePlane->getNormal());
        d = c.dotProduct(c);
        if ( compareToZero(d) != 0 ) {
            continue;
        }

        relation = classifyCoplanarSectorRelation(&current, referenceFace);
        registerCoplanarRelation(nbr, i, relation);
        registerCoplanarRelation(nbr, (i+1)%nnbr, relation);
        traceCoplanarTangential(
            "vertexFace coplanar relation op=" + Cpp98Support::toString(op) +
            " side=" + Cpp98Support::toString(BvsA) +
            " face=" + Cpp98Support::toString(referenceFace->id) +
            " localFace=" + Cpp98Support::toString(localFace->id) +
            " sectorIndex=" + Cpp98Support::toString(i) +
            " relation=" + Cpp98Support::toString(relation));

        if ( relation == SectorOnFace::COPLANAR_OVERLAP ) {
            d = localFace->getContainingPlane()->getNormal().dotProduct(
                referencePlane->getNormal());
            sameOrientation = (compareToZero(d) == 1);
            resolvedClass = resolveCoplanarSectorClass(op, BvsA,
                sameOrientation);
            traceCoplanarTangential(
                std::string("  resolved vertexFace coplanar overlap sameOrientation=") +
                booleanName(sameOrientation) + " class=" +
                Cpp98Support::toString(resolvedClass));
            nbr[i].cl = resolvedClass;
            nbr[(i+1)%nnbr].cl = resolvedClass;
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

    if ( sectorInfo == NULL || referenceFace == NULL ) {
        return SectorOnFace::COPLANAR_DISJOINT;
    }

    he = sectorInfo->sector;
    if ( he == NULL || he->startingVertex == NULL ) {
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
        he->parentLoop->parentFace->getContainingPlane()->getNormal(),
        MaybeVector(he->next()->startingVertex->position.subtract(start)),
        MaybeVector(he->previous()->startingVertex->position.subtract(start)),
        numericContext);
    currentInterval = buildIntervalForHalfEdgeSector(basis, he, numericContext);
    if ( !currentInterval.valid ) {
        return SectorOnFace::COPLANAR_TOUCHING;
    }

    intersectedHalfedge = containment.intersectedHalfedge();
    if ( intersectedHalfedge != NULL ) {
        return classifyCoplanarIntervalRelation(currentInterval,
            buildIntervalForCoplanarEdge(basis,
                intersectedHalfedge,
                referenceFace->getContainingPlane()->getNormal(),
                numericContext),
            numericContext);
    }
    intersectedVertex = containment.intersectedVertex();
    if ( intersectedVertex != NULL ) {
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
