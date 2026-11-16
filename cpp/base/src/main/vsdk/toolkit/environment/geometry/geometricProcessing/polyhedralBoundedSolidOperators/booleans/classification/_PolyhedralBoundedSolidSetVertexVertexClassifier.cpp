#include "java/lang/String.h"
#include <cstdio>

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_SetOperationTrace.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetClassifier.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetVertexVertexClassifier.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef _PolyhedralBoundedSolidSetVertexVertexClassifier Classifier;
typedef _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector OnSector;

int Classifier::debugFlags = 0;

namespace {

const int DEBUG_01_STRUCTURE = 0x01;
const int DEBUG_04_VERTEX_VERTEX_CLASSIFIER = 0x08;

int unionChoice(int op, int ifUnion, int otherwise)
{
    return (op == _PolyhedralBoundedSolidOperator::UNION) ? ifUnion : otherwise;
}

}

int Classifier::coplanarSameOrientationForSectorPair(int secta, int sectb)
{
    _PolyhedralBoundedSolidHalfEdge* ha;
    _PolyhedralBoundedSolidHalfEdge* hb;
    Vector3Dd n1;
    Vector3Dd n2;

    if ( secta < 0 || sectb < 0 || secta >= (int)nba.size() ||
         sectb >= (int)nbb.size() ) {
        return -1;
    }

    ha = nba[(size_t)secta].he;
    hb = nbb[(size_t)sectb].he;
    if ( ha == nullptr || hb == nullptr ||
         ha->parentLoop == nullptr || hb->parentLoop == nullptr ||
         ha->parentLoop->parentFace == nullptr ||
         hb->parentLoop->parentFace == nullptr ||
         ha->parentLoop->parentFace->hasContainingPlane() == false ||
         hb->parentLoop->parentFace->hasContainingPlane() == false ) {
        return -1;
    }

    n1 = ha->parentLoop->parentFace->containingPlaneNormal();
    n2 = hb->parentLoop->parentFace->containingPlaneNormal();
    if ( !_PolyhedralBoundedSolidSetGeometricPredicateProcessor::colinearVectors(n1, n2) ) {
        return -1;
    }

    return n1.dotProduct(n2) >= 0.0 ? 1 : 0;
}

Classifier::VertexVertexClassificationData Classifier::classify(
    _PolyhedralBoundedSolidVertex* va,
    _PolyhedralBoundedSolidVertex* vb,
    int op,
    int flags)
{
    debugFlags = flags;

    if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) == 0x00 ) {
            printf("  - ");
        }
        else {
            printf("  * ");
        }
        printf("Vertex of {A} / Vertex of {B} pair: A[%d] / B[%d]", va->id, vb->id);
        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) == 0x00 ) {
            printf(".\n");
        }
        else {
            printf(" ->\n");
        }
    }

    vertexVertexGetNeighborhood(va, vb);

    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
        printf("   - Initial sector/sector intersection candidates:\n");
        for ( size_t i = 0; i < sectors.size(); i++ ) {
            printf("    . %s\n", sectors[i].toString().c_str());
        }
    }

    vertexVertexReclassifyOnSectors(op);

    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
        printf("   - On sector reclassified:\n");
        for ( size_t i = 0; i < sectors.size(); i++ ) {
            printf("    . %s\n", sectors[i].toString().c_str());
        }
    }

    vertexVertexReclassifyOnEdges(op);

    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
        printf("   - On edges reclassified:\n");
        for ( size_t i = 0; i < sectors.size(); i++ ) {
            printf("    . %s\n", sectors[i].toString().c_str());
        }
    }

    return VertexVertexClassificationData(nba, nbb, sectors);
}

/**
Following program [MANT1988].15.8.
*/
java::ArrayList<Classifier::SectorOnVertex> Classifier::nbrpreproc(
    _PolyhedralBoundedSolidVertex* v)
{
    Vector3Dd bisec;
    _PolyhedralBoundedSolidHalfEdge* he;
    java::ArrayList<SectorOnVertex> nb;

    he = v->emanatingHalfEdge;
    Vector3Dd oldref2;

    do {
        SectorOnVertex n;
        n.he = he;
        n.wide = false;

        n.ref1 = he->previous()->startingVertex->position.subtract(
            he->startingVertex->position);
        n.ref2 = he->next()->startingVertex->position.subtract(
            he->startingVertex->position);
        n.ref12 = n.ref1.crossProduct(n.ref2);

        if ( PolyhedralBoundedSolidNumericPolicy::vectorsColinear(
                 n.ref1, n.ref2, numericContext) ||
             (n.ref12.dotProduct(
                 he->parentLoop->parentFace->containingPlaneNormal()) > 0.0 ) ) {
            if ( PolyhedralBoundedSolidNumericPolicy::vectorsColinear(
                     n.ref1, n.ref2, numericContext) ) {
                bisec = _PolyhedralBoundedSolidSetClassifier::inside(he);
            }
            else {
                bisec = n.ref1.add(n.ref2);
                bisec = bisec.multiply(-1);
            }
            oldref2 = n.ref2;
            n.ref2 = bisec;
            n.ref12 = n.ref1.crossProduct(n.ref2);
            nb.add(n);

            n = SectorOnVertex();
            n.he = he;
            n.ref2 = oldref2;
            n.ref1 = bisec;
            n.ref12 = n.ref1.crossProduct(n.ref2);
            n.wide = true;
        }

        nb.add(n);

        he = (he->mirrorHalfEdge())->next();
    } while( he != v->emanatingHalfEdge );

    return nb;
}

bool Classifier::sectoroverlap(const SectorOnVertex& na, const SectorOnVertex& nb)
{
    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor::sectoroverlap(
        na, nb, (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0);
}

bool Classifier::sctrwitthin(const Vector3Dd& dir, const Vector3Dd& ref1,
                             const Vector3Dd& ref2, const Vector3Dd& ref12)
{
    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor::sctrwitthin(
        dir, ref1, ref2, ref12);
}

int Classifier::compareToZero(double value)
{
    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor::compareToZero(value);
}

int Classifier::resolveCoplanarVertexVertexClass(int op, bool sameOrientation,
                                                 bool sideA)
{
    return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
        ::resolveCoplanarVertexVertexClass(op, sameOrientation, sideA);
}

/**
Sector intersection test.
Following program [MANT1988].15.9 and section [MANT1988].15.6.2.
*/
bool Classifier::vertexVertexSectorIntersectionTest(int i, int j)
{
    _PolyhedralBoundedSolidHalfEdge* h1;
    _PolyhedralBoundedSolidHalfEdge* h2;
    bool c1;
    bool c2;

    const SectorOnVertex& na = nba[(size_t)i];
    const SectorOnVertex& nb = nbb[(size_t)j];
    h1 = na.he;
    h2 = nb.he;

    Vector3Dd n1;
    Vector3Dd n2;
    Vector3Dd intrs;

    n1 = h1->parentLoop->parentFace->containingPlaneNormal();
    n2 = h2->parentLoop->parentFace->containingPlaneNormal();
    intrs = n1.crossProduct(n2);

    if ( PolyhedralBoundedSolidNumericPolicy::unitVectorsParallel(
        n1, n2, numericContext) ) {
        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
            printf(" <coplanar>");
        }
        return sectoroverlap(na, nb);
    }

    c1 = sctrwitthin(intrs, na.ref1, na.ref2, na.ref12);
    c2 = sctrwitthin(intrs, nb.ref1, nb.ref2, nb.ref12);
    if ( c1 && c2 ) {
        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
            printf(" <TRUE>");
        }
        return true;
    }
    else {
        intrs = intrs.multiply(-1);
        c1 = sctrwitthin(intrs, na.ref1, na.ref2, na.ref12);
        c2 = sctrwitthin(intrs, nb.ref1, nb.ref2, nb.ref12);
        if ( c1 && c2 ) {
            if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
                printf(" <TRUE>");
            }
            return true;
        }
    }

    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
        printf(" <FALSE>");
    }

    return false;
}

/**
Given a pair of coincident vertices `va` and `vb`, this method creates the
lists `nba`, `nbb`, and `sectors` as explained in section
[MANT1988].15.6.2 and program [MANT1988].15.7.
*/
void Classifier::vertexVertexGetNeighborhood(_PolyhedralBoundedSolidVertex* va,
                                             _PolyhedralBoundedSolidVertex* vb)
{
    size_t i;

    nba = nbrpreproc(va);
    nbb = nbrpreproc(vb);
    sectors.clear();

    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
        printf("   - NBA list of neighbor sectors for vertex on {A}:\n");
        for ( i = 0; i < nba.size(); i++ ) {
            printf("    . A[%zu]: %s\n", i + 1, nba[i].toString().c_str());
        }
        printf("   - NBB list of neighbor sectors for vertex on {B}:\n");
        for ( i = 0; i < nbb.size(); i++ ) {
            printf("    . B[%zu]: %s\n", i + 1, nbb[i].toString().c_str());
        }
    }

    double d1;
    double d2;
    double d3;
    double d4;
    size_t j;
    Vector3Dd na;
    Vector3Dd nb;

    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
        printf("   - Initial intersection tests between sectors (false intersections are sectors touching on a single point):\n");
    }

    for ( i = 0; i < nba.size(); i++ ) {
        for ( j = 0; j < nbb.size(); j++ ) {
            if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
                printf("    . A[%zu] / B[%zu]:", i + 1, j + 1);
            }

            if ( vertexVertexSectorIntersectionTest((int)i, (int)j) ) {
                SectorOnSector s;
                s.secta = (int)i;
                s.sectb = (int)j;
                const SectorOnVertex& xa = nba[i];
                const SectorOnVertex& xb = nbb[j];
                s.hea = xa.he;
                s.heb = xb.he;
                s.wa = xa.wide;
                s.wb = xb.wide;

                na = xa.he->parentLoop->parentFace->containingPlaneNormal();
                nb = xb.he->parentLoop->parentFace->containingPlaneNormal();
                d1 = nb.dotProduct(xa.ref1);
                d2 = nb.dotProduct(xa.ref2);
                d3 = na.dotProduct(xb.ref1);
                d4 = na.dotProduct(xb.ref2);
                s.s1a = compareToZero(d1);
                s.s2a = compareToZero(d2);
                s.s1b = compareToZero(d3);
                s.s2b = compareToZero(d4);
                s.intersect = true;
                sectors.add(s);
            }

            if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
                printf("\n");
            }
        }
    }
}

/**
Following section [MANT1988].15.6.2 and program [MANT1988].15.10.
*/
void Classifier::vertexVertexReclassifyOnSectors(int op)
{
    _PolyhedralBoundedSolidHalfEdge* ha;
    _PolyhedralBoundedSolidHalfEdge* hb;
    size_t i;
    size_t j;
    int newsa;
    int newsb;
    bool nonopposite;
    int secta;
    int prevsecta;
    int nextsecta;
    int sectb;
    int prevsectb;
    int nextsectb;
    double d;
    Vector3Dd n1;
    Vector3Dd n2;
    int nbaSize = (int)nba.size();
    int nbbSize = (int)nbb.size();

    for ( i = 0; i < sectors.size(); i++ ) {
        if ( sectors[i].s1a == OnSector::ON &&
             sectors[i].s2a == OnSector::ON &&
             sectors[i].s1b == OnSector::ON &&
             sectors[i].s2b == OnSector::ON ) {
            secta = sectors[i].secta;
            sectb = sectors[i].sectb;
            prevsecta = (secta == 0) ? nbaSize - 1 : secta - 1;
            prevsectb = (sectb == 0) ? nbbSize - 1 : sectb - 1;
            nextsecta = (secta == nbaSize - 1) ? 0 : secta + 1;
            nextsectb = (sectb == nbbSize - 1) ? 0 : sectb + 1;
            ha = nba[(size_t)secta].he;
            hb = nbb[(size_t)sectb].he;
            n1 = ha->parentLoop->parentFace->containingPlaneNormal();
            n2 = hb->parentLoop->parentFace->containingPlaneNormal();
            d = n1.subtract(n2).length();
            nonopposite = ( d < VSDK::EPSILON );
            if ( nonopposite ) {
                newsa = unionChoice(op, OnSector::OUT, OnSector::IN);
                newsb = unionChoice(op, OnSector::IN, OnSector::OUT);
            }
            else {
                newsa = unionChoice(op, OnSector::IN, OnSector::OUT);
                newsb = unionChoice(op, OnSector::IN, OnSector::OUT);
            }
            SectorOnSector& si = sectors[i];

            for ( j = 0; j < sectors.size(); j++ ) {
                SectorOnSector& sj = sectors[j];
                if ( (sj.secta == prevsecta) && (sj.sectb == sectb) ) {
                    if ( sj.s1a != OnSector::ON ) {
                        sj.s2a = newsa;
                    }
                }
                if ( (sj.secta == nextsecta) && (sj.sectb == sectb) ) {
                    if ( sj.s2a != OnSector::ON ) {
                        sj.s1a = newsa;
                    }
                }
                if ( (sj.secta == secta) && (sj.sectb == prevsectb) ) {
                    if ( sj.s1b != OnSector::ON ) {
                        sj.s2b = newsb;
                    }
                }
                if ( (sj.secta == secta) && (sj.sectb == nextsectb) ) {
                    if ( sj.s2b != OnSector::ON ) {
                        sj.s1b = newsb;
                    }
                }
                if ( (sj.s1a == sj.s2a) &&
                     (sj.s1a == OnSector::IN || sj.s1a == OnSector::OUT) ) {
                    sj.intersect = false;
                }
                if ( (sj.s1b == sj.s2b) &&
                     (sj.s1b == OnSector::IN || sj.s1b == OnSector::OUT) ) {
                    sj.intersect = false;
                }
            }

            si.s1a = si.s2a = newsa;
            si.s1b = si.s2b = newsb;
            si.intersect = false;
            _SetOperationTrace::traceCoplanarTangential(
                java::String(("  deactivated vv sector pair sectA=" +
                java::String::valueOf(secta) + " sectB=" + java::String::valueOf(sectb)).c_str()));
        }
    }
}

/**
Resolves "on" edge classes from the local geometry of both vertex
neighborhoods before the operation-biased rules of figures
[MANT1988].15.10 to 15.12 are applied.

A sector edge is classified "on" when it lies in the plane of the other
sector. The biased rules assume that the surface crosses the other solid
along that edge. When the surface on both sides of the edge has the same
class with respect to the other solid (for instance a concave edge of one
solid lying on a convex edge of the other, with the faces interleaved),
there is no crossing and the edge takes that class. When the two sides
differ, the curve runs along the edge and it stays "on" for the biased
rules.
*/
void Classifier::resolveOnEdgesByLocalNeighborhood()
{
    int ON = OnSector::ON;
    int k;

    for ( k = 0; k < sectors.size(); k++ ) {
        SectorOnSector& s = sectors[k];
        if ( !s.intersect ) {
            continue;
        }
        if ( s.s1a == ON ) {
            s.s1a = classifyRayWithoutCrossing(nba, s.secta, true, nbb);
        }
        if ( s.s2a == ON ) {
            s.s2a = classifyRayWithoutCrossing(nba, s.secta, false, nbb);
        }
        if ( s.s1b == ON ) {
            s.s1b = classifyRayWithoutCrossing(nbb, s.sectb, true, nba);
        }
        if ( s.s2b == ON ) {
            s.s2b = classifyRayWithoutCrossing(nbb, s.sectb, false, nba);
        }
        if ( (s.s1a == s.s2a && s.s1a != ON) ||
             (s.s1b == s.s2b && s.s1b != ON) ) {
            s.intersect = false;
        }
    }
}

/**
Class of the surface next to a boundary ray of sector `index` in `own`,
measured against the `other` neighborhood on both sides of the ray (in
this sector and in the adjacent one that shares the ray). ON when the
sides differ or either side lies on the other solid's surface.
*/
int Classifier::classifyRayWithoutCrossing(
    const java::ArrayList<SectorOnVertex>& own,
    int index, bool firstRay,
    const java::ArrayList<SectorOnVertex>& other)
{
    int ON = OnSector::ON;
    Vector3Dd ray;
    Vector3Dd adjacentRay;
    Vector3Dd adjacentOther;
    int here;
    int there;

    const SectorOnVertex& n = own[index];
    if ( firstRay ) {
        const SectorOnVertex& adjacent =
            own[index == 0 ? own.size() - 1 : index - 1];
        ray = n.ref1;
        here = classifyDirectionInNeighborhood(
            directionNextToRay(n.ref1, n.ref2), other);
        adjacentRay = adjacent.ref2;
        adjacentOther = adjacent.ref1;
    }
    else {
        const SectorOnVertex& adjacent =
            own[index == own.size() - 1 ? 0 : index + 1];
        ray = n.ref2;
        here = classifyDirectionInNeighborhood(
            directionNextToRay(n.ref2, n.ref1), other);
        adjacentRay = adjacent.ref1;
        adjacentOther = adjacent.ref2;
    }
    if ( here == ON ||
         Vector3Dd(ray).normalized().dotProduct(
             Vector3Dd(adjacentRay).normalized()) <
             1.0 - VSDK::EPSILON ) {
        return ON;
    }
    there = classifyDirectionInNeighborhood(
        directionNextToRay(adjacentRay, adjacentOther), other);
    return there == here ? here : ON;
}

/**
A direction inside a (minor arc) sector, next to its boundary ray.
*/
Vector3Dd Classifier::directionNextToRay(const Vector3Dd& ray,
    const Vector3Dd& otherRay)
{
    Vector3Dd r = Vector3Dd(ray).normalized();
    Vector3Dd o = Vector3Dd(otherRay).normalized();
    return r.add(o.multiply(1.0e-3)).normalized();
}

const Vector3Dd Classifier::PROBE_RAYS[3] = {
    Vector3Dd(0.5773, 0.3341, 0.7452).normalized(),
    Vector3Dd(-0.2311, 0.8817, 0.4113).normalized(),
    Vector3Dd(0.7061, -0.6129, 0.3549).normalized()
};

/**
Classifies a direction leaving the common vertex against the cone that
the given neighborhood sectors bound: IN, OUT, or ON when the direction
lies on one of those sectors or no probe ray gives an unambiguous count.
Ray parity over the sectors, each a planar minor arc through the vertex.
*/
int Classifier::classifyDirectionInNeighborhood(const Vector3Dd& d,
    const java::ArrayList<SectorOnVertex>& nb)
{
    int ON = OnSector::ON;
    double planeTolerance = 1.0e-9;
    int k;
    int m;

    for ( k = 0; k < nb.size(); k++ ) {
        const SectorOnVertex& n = nb[k];
        InfinitePlane* plane = n.he->parentLoop->parentFace->getContainingPlane();
        Vector3Dd normal = plane->getNormal();
        delete plane;
        if ( java::Math::abs(d.dotProduct(normal)) < planeTolerance &&
             sctrwitthin(d, n.ref1, n.ref2, n.ref12) ) {
            return ON;
        }
    }

    for ( m = 0; m < 3; m++ ) {
        const Vector3Dd& w = PROBE_RAYS[m];
        int crossings = 0;
        bool ambiguous = false;

        for ( k = 0; k < nb.size(); k++ ) {
            const SectorOnVertex& n = nb[k];
            InfinitePlane* plane =
                n.he->parentLoop->parentFace->getContainingPlane();
            Vector3Dd normal = plane->getNormal();
            delete plane;
            double denominator = w.dotProduct(normal);
            double t;
            Vector3Dd q;

            if ( java::Math::abs(denominator) < 1.0e-9 ) {
                continue;
            }
            t = -d.dotProduct(normal) / denominator;
            if ( t <= 0.0 ) {
                continue;
            }
            q = d.add(w.multiply(t));
            if ( q.length() < 1.0e-9 ||
                 PolyhedralBoundedSolidNumericPolicy::vectorsColinear(
                     q, n.ref1, numericContext) ||
                 PolyhedralBoundedSolidNumericPolicy::vectorsColinear(
                     q, n.ref2, numericContext) ) {
                ambiguous = true;
                break;
            }
            if ( sctrwitthin(q, n.ref1, n.ref2, n.ref12) ) {
                crossings++;
            }
        }
        if ( !ambiguous ) {
            return (crossings % 2) == 1 ? OnSector::IN : OnSector::OUT;
        }
    }
    return ON;
}

/**
Reclassification procedure for "on"-edges on the vertex/vertex classifier,
as expected from the high-level description of section [MANT1988].15.6.2
and the case structures of figures [MANT1988].15.10, [MANT1988].15.11,
and [MANT1988].15.12.
*/
void Classifier::vertexVertexReclassifyOnEdges(int op)
{
    size_t i;
    size_t j;
    int newsa;
    int newsb;
    int secta;
    int prevsecta;
    int sectb;
    int prevsectb;
    int nbaSize = (int)nba.size();
    int nbbSize = (int)nbb.size();

    resolveOnEdgesByLocalNeighborhood();

    for ( i = 0; i < sectors.size(); i++ ) {
        if ( sectors[i].intersect &&
             sectors[i].s1a == OnSector::ON &&
             sectors[i].s1b == OnSector::ON ) {
            newsa = unionChoice(op, OnSector::OUT, OnSector::IN);
            newsb = unionChoice(op, OnSector::IN, OnSector::OUT);

            secta = sectors[i].secta;
            sectb = sectors[i].sectb;
            prevsecta = (secta == 0) ? nbaSize - 1 : secta - 1;
            prevsectb = (sectb == 0) ? nbbSize - 1 : sectb - 1;

            for ( j = 0; j < sectors.size(); j++ ) {
                SectorOnSector& sj = sectors[j];
                if ( sj.intersect ) {
                    if ( (sj.secta == secta) && (sj.sectb == sectb) ) {
                        sj.s1a = newsa;
                        sj.s1b = newsb;
                    }

                    if ( (sj.secta == prevsecta) && (sj.sectb == sectb) ) {
                        sj.s2a = newsa;
                        sj.s1b = newsb;
                    }

                    if ( (sj.secta == secta) && (sj.sectb == prevsectb) ) {
                        sj.s1a = newsa;
                        sj.s2b = newsb;
                    }

                    if ( (sj.secta == prevsecta) && (sj.sectb == prevsectb) ) {
                        sj.s2a = newsa;
                        sj.s2b = newsb;
                    }

                    if ( sj.s1a == sj.s2a &&
                         (sj.s1a == OnSector::IN || sj.s1a == OnSector::OUT) ) {
                        sj.intersect = false;
                    }
                    if ( sj.s1b == sj.s2b &&
                         (sj.s1b == OnSector::IN || sj.s1b == OnSector::OUT) ) {
                        sj.intersect = false;
                    }
                }
            }
        }
    }

    for ( i = 0; i < sectors.size(); i++ ) {
        if ( sectors[i].intersect && sectors[i].s1a == OnSector::ON ) {
            secta = sectors[i].secta;
            sectb = sectors[i].sectb;
            prevsecta = (secta == 0) ? nbaSize - 1 : secta - 1;
            prevsectb = (sectb == 0) ? nbbSize - 1 : sectb - 1;
            (void)prevsectb;
            newsa = unionChoice(op, OnSector::OUT, OnSector::IN);

            for ( j = 0; j < sectors.size(); j++ ) {
                SectorOnSector& sj = sectors[j];
                if ( sj.intersect ) {
                    if ( (sj.secta == secta) && (sj.sectb == sectb) ) {
                        sj.s1a = newsa;
                    }

                    if ( (sj.secta == prevsecta) && (sj.sectb == sectb) ) {
                        sj.s2a = newsa;
                    }

                    if ( sj.s1a == sj.s2a &&
                         (sj.s1a == OnSector::IN || sj.s1a == OnSector::OUT) ) {
                        sj.intersect = false;
                    }
                }
            }
        }
        else if ( sectors[i].intersect && sectors[i].s1b == OnSector::ON ) {
            secta = sectors[i].secta;
            sectb = sectors[i].sectb;
            prevsecta = (secta == 0) ? nbaSize - 1 : secta - 1;
            (void)prevsecta;
            prevsectb = (sectb == 0) ? nbbSize - 1 : sectb - 1;
            newsb = unionChoice(op, OnSector::OUT, OnSector::IN);

            for ( j = 0; j < sectors.size(); j++ ) {
                SectorOnSector& sj = sectors[j];
                if ( sj.intersect ) {
                    if ( (sj.secta == secta) && (sj.sectb == sectb) ) {
                        sj.s1b = newsb;
                    }

                    if ( (sj.secta == secta) && (sj.sectb == prevsectb) ) {
                        sj.s2b = newsb;
                    }

                    if ( sj.s1b == sj.s2b &&
                         (sj.s1b == OnSector::IN || sj.s1b == OnSector::OUT) ) {
                        sj.intersect = false;
                    }
                }
            }
        }
    }
}
