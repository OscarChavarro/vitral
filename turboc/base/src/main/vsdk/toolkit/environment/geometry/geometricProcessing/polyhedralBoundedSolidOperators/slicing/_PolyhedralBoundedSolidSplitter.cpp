#include <math.h>
#include <stdio.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/slicing/_PolyhedralBoundedSolidSplitter.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "java/util/Collections.h"
#include "java/util/Comparator.h"
#include "java/util/TreeMap.txx"
#include "java/util/TreeSet.txx"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidVertexNeighborhood.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"


/** Order of `_PolyhedralBoundedSolidSplitterNullEdge::compareTo` */
class SplitterNullEdgeOrder :
    public Comparator<_PolyhedralBoundedSolidSplitterNullEdge> {
public:
    int compare(const _PolyhedralBoundedSolidSplitterNullEdge& a,
        const _PolyhedralBoundedSolidSplitterNullEdge& b) const
    {
        return a.compareTo(b);
    }
};

/**
A vertex position labelled with its shell, as the `double[]` entries
{x, y, z, shell} of `hasTouchingShells` in Java.
*/
class _SplitterShellPoint {
public:
    double x;
    double y;
    double z;
    double shell;

    _SplitterShellPoint() : x(0.0), y(0.0), z(0.0), shell(0.0) {}
    _SplitterShellPoint(double x, double y, double z, double shell) :
        x(x), y(y), z(z), shell(shell) {}
};

/** As Java `(u, w) -> Double.compare(u[0], w[0])` */
class SplitterShellPointOrder : public Comparator<_SplitterShellPoint> {
public:
    int compare(const _SplitterShellPoint& u,
        const _SplitterShellPoint& w) const
    {
        return Double::compare(u.x, w.x);
    }
};

//= _PolyhedralBoundedSolidSplitterSectorClassification ===================

_PolyhedralBoundedSolidSplitterSectorClassification::
_PolyhedralBoundedSolidSplitterSectorClassification()
    : _PolyhedralBoundedSolidSectorClassification()
{
}

_PolyhedralBoundedSolidSplitterSectorClassification::
_PolyhedralBoundedSolidSplitterSectorClassification(
    const _PolyhedralBoundedSolidSectorClassification& other)
    : _PolyhedralBoundedSolidSectorClassification(other)
{
}

String _PolyhedralBoundedSolidSplitterSectorClassification::toString() const
{
    String msg = "{";
    msg += sector->toString().c_str();
    switch ( classification ) {
      case ABOVE: msg += " ABOVE"; break;
      case BELOW: msg += " BELOW"; break;
      case ON: msg += " ON"; break;
      default: msg += "<INVALID!>"; break;
    }
    if ( isWide ) {
        msg += " (W) ";
    }
    //msg = msg + ", pos: " + position;

    switch ( situation ) {
      case COPLANAR_FACE: msg += "<COPLANAR_FACE>"; break;
      case INPLANE_EDGE: msg += "<INPLANE_EDGE>"; break;
      case CROSSING_EDGE: msg += "<CROSSING_EDGE>"; break;
      default: msg += "<UNDEFINED>"; break;
    }

    msg += "}";
    return String(msg.c_str());
}

//= _PolyhedralBoundedSolidSplitterNullEdge ===============================

_PolyhedralBoundedSolidSplitterNullEdge::_PolyhedralBoundedSolidSplitterNullEdge()
    : numericContext(PolyhedralBoundedSolidNumericPolicy::defaultContext()),
      e(NULL)
{
}

_PolyhedralBoundedSolidSplitterNullEdge::_PolyhedralBoundedSolidSplitterNullEdge(
    _PolyhedralBoundedSolidEdge* e,
    const PolyhedralBoundedSolidNumericPolicy::ToleranceContext* context)
    : numericContext(PolyhedralBoundedSolidNumericPolicy::defaultContext()),
      e(e)
{
    if ( context != NULL ) {
        numericContext = *context;
    }
}

int _PolyhedralBoundedSolidSplitterNullEdge::compareTo(
    const _PolyhedralBoundedSolidSplitterNullEdge& other) const
{
    Vector3Dd a;
    Vector3Dd b;

    a = this->e->rightHalf->startingVertex->position;
    b = other.e->rightHalf->startingVertex->position;

    if ( PolyhedralBoundedSolidNumericPolicy
        ::compare(a.x(), b.x(), numericContext.bigEpsilon()) != 0 ) {
        if ( a.x() < b.x() ) {
            return -1;
        }
        return 1;
    }
    else {
        if ( PolyhedralBoundedSolidNumericPolicy
            ::compare(a.y(), b.y(), numericContext.bigEpsilon()) != 0 ) {
            if ( a.y() < b.y() ) {
                return -1;
            }
            return 1;
        }
        else {
            if ( a.z() < b.z() ) {
                return -1;
            }
            return 1;
        }
    }
}

//= _PolyhedralBoundedSolidSplitter =======================================

class _PolyhedralBoundedSolidSplitter::_SplitContext {
public:
    /**
    Variable `soov` ("set of ON-vertices") from program [MANT1988].14.1.
    */
    ArrayList<_PolyhedralBoundedSolidVertex*> soov;

    /**
    Variable `sone` ("set of null edges") from program [MANT1988].14.1.
    */
    ArrayList<_PolyhedralBoundedSolidSplitterNullEdge> sone;

    /**
    Variable `sonf` ("set of null faces") from program [MANT1988].14.1.
    */
    ArrayList<_PolyhedralBoundedSolidFace*> sonf;

    ArrayList<_PolyhedralBoundedSolidFace*> facesToFixAbove;
    ArrayList<_PolyhedralBoundedSolidFace*> facesToFixBelow;

    /**
    Variable `ends` from program [MANT1988].14.9.
    */
    ArrayList<_PolyhedralBoundedSolidHalfEdge*> ends;
    ArrayList<_PolyhedralBoundedSolidHalfEdge*> tieds;

    /**
    Null edges created by lmev(he, he) (struts): both halves lie in the
    same face. Their halves are paired by geometry in `splitConnect`.
    C++ port note: Java uses an identity set.
    */
    TreeSet<_PolyhedralBoundedSolidEdge*> struts;
};

int _PolyhedralBoundedSolidSplitter::compareToZero(double value)
{
    return PolyhedralBoundedSolidNumericPolicy::compareToZero(value,
        numericContext);
}

/**
Implements function `addsoov` from section [MANT1988].14.4. and program
[MANT1988].14.2.
*/
void _PolyhedralBoundedSolidSplitter::addsoov(_SplitContext& ctx,
    _PolyhedralBoundedSolidVertex* v)
{
    long i;

    for ( i = 0; i < ctx.soov.size(); i++ ) {
        if ( ctx.soov[i] == v ) {
            return;
        }
    }
    ctx.soov.add(v);
}

/**
Implements solid splitting reduction step as indicated on sections
[MANT1988].14.2.1 and [MANT1988].14.4 and program [MANT1988].14.2.

This method is responsible for generating the set of coplanar
vertices of `inSolid` (with respect to `inSplittingPlane`) and store
them on `soov` for later usage.

This method subdivides all edges of `inSolid` that intersects
`inSplittingPlane` at their intersection points.
*/
void _PolyhedralBoundedSolidSplitter::splitGenerate(_SplitContext& ctx,
    PolyhedralBoundedSolid* inSolid, const InfinitePlane& inSplittingPlane)
{
    _PolyhedralBoundedSolidEdge* e;
    _PolyhedralBoundedSolidHalfEdge* he;
    _PolyhedralBoundedSolidVertex* v1;
    _PolyhedralBoundedSolidVertex* v2;
    Vector3Dd p;
    double d1;
    double d2;
    double t;
    int s1;
    int s2;
    long i;

    for ( i = 0; i < inSolid->getEdgesList().size(); i++ ) {
        e = inSolid->getEdgesList().get(i);
        v1 = e->rightHalf->startingVertex;
        v2 = e->leftHalf->startingVertex;
        d1 = inSplittingPlane.pointDistance(v1->position);
        d2 = inSplittingPlane.pointDistance(v2->position);
        s1 = compareToZero(d1);
        s2 = compareToZero(d2);
        if ( (s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1) ) {
            t = d1 / (d1 - d2);
            p = v1->position.add((v2->position.subtract(v1->position)).multiply(t));
            he = e->leftHalf->next();
            PolyhedralBoundedSolidEulerOperators::lmev(inSolid, e->rightHalf, he,
                inSolid->getMaxVertexId()+1, p);
            addsoov(ctx, he->previous()->startingVertex);
        }
        else {
            if ( s1 == 0 ) {
                addsoov(ctx, v1);
            }
            if ( s2 == 0 ) {
                addsoov(ctx, v2);
            }
        }
    }
}

/**
Current method is the first step for the initial classification of vertex
neighborhood for `vtx`, as indicated on section [MANT1988].14.5.2. and
program [MANT1988].14.4.

Vitral SDK's implementation of this procedure extends the original from
[MANT1988] by adding extra information flags to sector classifications
`.isWide`, `.position` and `.situation`, as a debugging aid.

Sector convention. Entries follow the half-edges `he_k` leaving
`vtx`, with `he_(k+1) = mirror(he_k).next()`. Entry k classifies the
edge `e_k` of `he_k`. The face of `he_k` lies between `e_(k-1)` and
`e_k`, so a change of class between entries k-1 and k means the plane
crosses the face of `he_k`: the sector of entry k. A reflex face
corner gets an extra (wide) entry at its inward bisector, placed
BEFORE the entry of its half-edge, i.e. between `e_(k-1)` and `e_k`
where it geometrically belongs (problem [MANT1988].14.1). The same
convention is used by the set operator's vertex/face classifier.
*/
ArrayList<_PolyhedralBoundedSolidSplitterSectorClassification>
_PolyhedralBoundedSolidSplitter::getNeighborhood(
    _PolyhedralBoundedSolidVertex* vtx, const InfinitePlane& inSplittingPlane)
{
    ArrayList<_PolyhedralBoundedSolidSectorClassification> entries;
    ArrayList<SectorClassification> neighborSectorsInfo;
    long i;

    _PolyhedralBoundedSolidVertexNeighborhood::classify(vtx,
        inSplittingPlane, entries);
    for ( i = 0; i < entries.size(); i++ ) {
        neighborSectorsInfo.add(SectorClassification(entries[i]));
    }
    return neighborSectorsInfo;
}

/**
Current method applies the first reclassification rule presented at
sections [MANT1988].14.5.1 and [MANT1988].14.5.2. A face lying on the
plane spans the entry of its half-edge and the entry before it (see the
sector convention in `getNeighborhood`); both get the class that
leaves the face on the side opposite to its outward normal.
For the given vertex neigborhood, classify each edge according to whether
its final vertex lies above, on or below the `inSplittingPlane`. Tag
the edge with the corresponding label ABOVE, ON or BELOW.
Following program [MANT1988].14.5.
*/
void _PolyhedralBoundedSolidSplitter::reclassifyOnSectors(
    ArrayList<SectorClassification>& nbr,
    const InfinitePlane& inSplittingPlane)
{
    _PolyhedralBoundedSolidFace* f;
    Vector3Dd c;
    double d;
    long i;

    for ( i = 0; i < nbr.size(); i++ ) {
        SectorClassification& l = nbr[i];
        f = l.sector->parentLoop->parentFace;
        InfinitePlane* facePlane = f->getContainingPlane();
        c = facePlane->getNormal().crossProduct(inSplittingPlane.getNormal());
        d = c.dotProduct(c);
        if ( compareToZero(d) == 0 ) {
            // Entering this means "faces are coplanar"
            d = facePlane->getNormal().dotProduct(inSplittingPlane.getNormal());
            if ( compareToZero(d) == 1 ) {
                l.classification = SectorClassification::BELOW;
                l.situation = SectorClassification::COPLANAR_FACE;
                nbr[(i+nbr.size()-1)%nbr.size()].classification = SectorClassification::BELOW;
            }
            else {
                l.classification = SectorClassification::ABOVE;
                l.situation = SectorClassification::COPLANAR_FACE;
                nbr[(i+nbr.size()-1)%nbr.size()].classification = SectorClassification::ABOVE;
            }
        }
        delete facePlane;
    }
}

/**
Current method applies the second reclassification rule presented at
sections [MANT1988].14.5.1 and [MANT1988].14.5.2:
After applying the first rule on method `reclassifyOnSectors`, ON edges
may appear in only four kinds of consecutive arrangements. For each
of the following arrangements, ON edge is reclassified as ABOVE or BELOW:
  - Sequence ABOVE/ON/ABOVE -> reclassified as BELOW
  - Sequence ABOVE/ON/BELOW -> reclassified as BELOW
  - Sequence BELOW/ON/BELOW -> reclassified as ABOVE
  - Sequence BELOW/ON/ABOVE -> reclassified as BELOW
Those 4 rules are designed so that nonmanifold results will be represented
as disconnected models.
Following program [MANT1988].14.6.

The two rules with equal neighbours are only right for a reflex
(concave) edge, where the solid lies on both sides of the plane and the
section is pinched at the edge. A convex edge with both neighbours on
one side merely touches the plane: reclassifying it to the other side
would insert a pair of null edges around a sector of zero width and
produce a two-vertex null face (found with the split fuzz, Stage E). A
convex touching edge therefore keeps the class of its neighbours.
*/
void _PolyhedralBoundedSolidSplitter::reclassifyOnEdges(
    ArrayList<SectorClassification>& nbr)
{
    int prev;
    int next;
    long i;
    long n = nbr.size();

    for ( i = 0; i < n; i++ ) {
        SectorClassification& l = nbr[i];
        if ( l.classification != SectorClassification::ON ) {
            continue;
        }
        prev = nbr[(n+i-1) % n].classification;
        next = nbr[(i+1) % n].classification;
        if ( prev == next && !l.isWide &&
             isConvexEdge(l.sector) ) {
            l.classification = prev;
        }
        else if ( prev == SectorClassification::BELOW &&
                  next == SectorClassification::BELOW ) {
            l.classification = SectorClassification::ABOVE;
        }
        else {
            l.classification = SectorClassification::BELOW;
        }
    }
}

/**
True when the edge of `he` is convex: with outward face normals `nA`
(face of `he`) and `nB` (face of its mirror) and loops running
counterclockwise about them, the edge is convex when
(nA x nB) . t > 0, `t` being the direction of `he`. The test is local
to the edge, so it holds for non-convex faces. Flat or undecidable
edges report false, which keeps the [MANT1988] rule.
*/
BOOLEAN _PolyhedralBoundedSolidSplitter::isConvexEdge(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    InfinitePlane* pa;
    InfinitePlane* pb;
    Vector3Dd t;
    double s;

    if ( he == NULL || he->parentEdge == NULL ) {
        return FALSE;
    }
    pa = he->parentLoop->parentFace->getContainingPlane();
    pb = he->mirrorHalfEdge()->parentLoop->parentFace->getContainingPlane();
    if ( pa == NULL || pb == NULL ) {
        delete pa;
        delete pb;
        return FALSE;
    }
    t = he->next()->startingVertex->position.subtract(
        he->startingVertex->position);
    if ( t.length() <= numericContext.unitVectorTolerance() ) {
        delete pa;
        delete pb;
        return FALSE;
    }
    s = pa->getNormal().crossProduct(pb->getNormal())
        .dotProduct(t.normalized());
    delete pa;
    delete pb;
    return s > numericContext.unitVectorTolerance();
}

/**
Following section [MANT1988].14,6,2 and program [MANT1988].14.7.
Note, this code is horrible! YUCK! :P

With respect to the original algorithm from [MANT1988], current
implementation adds an extra check to ensure the orientation of the
edges from below to above.

The null edge is inserted in the sectors where the class changes:
the entry AFTER each BELOW/ABOVE and ABOVE/BELOW transition (the face
of that entry's half-edge is the one the plane crosses). Taking the
entry before the transition, as this code used to, puts the null edge
in the wrong face whenever the ON-vertex has more than two edges, so
no null edges could be joined and the whole solid was lost (defect
S-01, plane through a vertex).
*/
void _PolyhedralBoundedSolidSplitter::insertNullEdges(
    _SplitContext& ctx,
    ArrayList<SectorClassification>& nbr,
    PolyhedralBoundedSolid* inSolid, const InfinitePlane& /*inSplittingPlane*/)
{
    long start;
    long i;
    _PolyhedralBoundedSolidHalfEdge* head;
    _PolyhedralBoundedSolidHalfEdge* tail;
    long nnbr = nbr.size();

    if ( nnbr <= 0 ) return;

    //- Locate the head of an ABOVE-sequence --------------------------
    i = 0;
    while ( !( nbr[i].classification == SectorClassification::BELOW &&
               nbr[(i+1)%nnbr].classification == SectorClassification::ABOVE )  ) {
        i++;
        if ( i >= nnbr ) {
            return;
        }
    }
    start = i;
    head = nbr[(i+1) % nnbr].sector;

    //-----------------------------------------------------------------
    while ( TRUE ) {
        //- Locate the final sector of the sequence ------------------
        while ( !( nbr[i].classification == SectorClassification::ABOVE &&
                   nbr[(i+1)%nnbr].classification == SectorClassification::BELOW ) ) {
            i = (i+1) % nnbr;
        }
        tail = nbr[(i+1) % nnbr].sector;

        //- Insert null edge -----------------------------------------
        // Always the same orientation, so that the two null-edge halves
        // lying in a crossed face are a right and a left half and
        // `neighbor` can join them. Choosing it per vertex from the side
        // of head's far end (as this code used to) gave inconsistent
        // orientations at reflex ON-vertices.
        // A face crossed twice at this vertex (reflex corner) is the
        // sector of two runs. After the first null edge split the
        // vertex, the corner left for the second crossing starts at
        // the null half inserted just before the sector half-edge.
        if ( tail->startingVertex != head->startingVertex ) {
            if ( tail->previous()->startingVertex == head->startingVertex ) {
                tail = tail->previous();
            }
            else if ( head->previous()->startingVertex ==
                      tail->startingVertex ) {
                head = head->previous();
            }
        }
        PolyhedralBoundedSolidEulerOperators::lmev(inSolid, tail, head,
            inSolid->getMaxVertexId()+1, head->startingVertex->position);
        if ( head == tail ) {
            // A run made of one wide (bisector) entry only: the plane
            // enters and leaves the same reflex face corner at the
            // vertex. lmev(he, he) inserts a strut edge with both
            // halves in that face ([MANT1988] Sec. 14.6.2).
            ctx.struts.add(tail->previous()->parentEdge);
        }
        ctx.sone.add(_PolyhedralBoundedSolidSplitterNullEdge(
            tail->previous()->parentEdge, &numericContext));

        //- Locate the start of the next sequence --------------------
        while ( !( nbr[i].classification == SectorClassification::BELOW &&
                   nbr[(i+1) % nnbr].classification == SectorClassification::ABOVE ) ) {
            i = (i+1) % nnbr;
            if ( i == start ) {
                return;
            }
        }
        // Program 14.7 starts every ABOVE run at its own head. This
        // code used to keep the head of the first run, so a vertex
        // with two runs got its second null edge in the wrong sector.
        head = nbr[(i+1) % nnbr].sector;
    }
}

/**
Vertex neighborhood classifier, as presented in section [MANT1988].14.5,
and program 14.3.

Earlier versions reversed the neighborhood when it had in-plane
edges, to compensate for taking the null-edge sectors one entry too
early. With the sector convention documented on `getNeighborhood` no
reversal is needed.
*/
void _PolyhedralBoundedSolidSplitter::splitClassify(_SplitContext& ctx,
    PolyhedralBoundedSolid* inSolid, const InfinitePlane& inSplittingPlane)
{
    long i;

    /// Following variable `nbr` from program [MANT1988].14.3.
    ArrayList<SectorClassification> nbr;

    for ( i = 0; i < ctx.soov.size(); i++ ) {
        nbr = getNeighborhood(ctx.soov[i], inSplittingPlane);
        reclassifyOnSectors(nbr, inSplittingPlane);
        reclassifyOnEdges(nbr);
        insertNullEdges(ctx, nbr, inSolid, inSplittingPlane);
    }
}

/**
Following section [MANT1988].14.7.2. and program [MANT1988].14.9.
*/
_PolyhedralBoundedSolidHalfEdge* _PolyhedralBoundedSolidSplitter::canJoin(
    _SplitContext& ctx, _PolyhedralBoundedSolidHalfEdge* he)
{
    _PolyhedralBoundedSolidHalfEdge* ret;
    long i;

    for ( i = 0; i < ctx.ends.size(); i++ ) {
        if ( neighbor(he, ctx.ends[i]) ) {
            ret = ctx.ends[i];
            ctx.ends.remove((long)i);
            ctx.tieds.add(ret);
            return ret;
        }
    }
    ctx.ends.add(he);
    return NULL;
}

void _PolyhedralBoundedSolidSplitter::printNbr(
    const ArrayList<SectorClassification>& neighborSectorsInfo)
{
    long i;

    for ( i = 0; i < neighborSectorsInfo.size(); i++ ) {
        printf("  - %s\n", neighborSectorsInfo[i].toString().c_str());
    }
}

void _PolyhedralBoundedSolidSplitter::printEnds(_SplitContext& ctx)
{
    long i;

    for ( i = 0; i < ctx.ends.size(); i++ ) {
        printf("  - ends[%ld]: %s\n", (long)i,
            ctx.ends[i]->toString().c_str());
    }
}

/**
Following section [MANT1988].14.7.2. and program [MANT1988].14.10.
*/
void _PolyhedralBoundedSolidSplitter::cut(_SplitContext& ctx,
    _PolyhedralBoundedSolidHalfEdge* he)
{
    PolyhedralBoundedSolid* s;

    s = he->parentLoop->parentFace->parentSolid;

    if ( he->parentEdge->rightHalf->parentLoop ==
         he->parentEdge->leftHalf->parentLoop ) {
        ctx.sonf.add(he->parentLoop->parentFace);
        PolyhedralBoundedSolidEulerOperators::lkemr(s, he->parentEdge->rightHalf,
            he->parentEdge->leftHalf);
    }
    else {
        PolyhedralBoundedSolidEulerOperators::lkef(s, he->parentEdge->rightHalf,
            he->parentEdge->leftHalf);
    }
}

BOOLEAN _PolyhedralBoundedSolidSplitter::isLoose(_SplitContext& ctx,
    _PolyhedralBoundedSolidHalfEdge* he)
{
    long i;

    for ( i = 0; i < ctx.tieds.size(); i++ ) {
        if ( he == ctx.tieds[i] ) return FALSE;
    }

    return TRUE;
}

/**
Following section [MANT1988].14.7.2. and program [MANT1988].14.9.
*/
void _PolyhedralBoundedSolidSplitter::splitConnect(_SplitContext& ctx)
{
    long i;
    _PolyhedralBoundedSolidEdge* nextedge;
    _PolyhedralBoundedSolidHalfEdge* h1;
    _PolyhedralBoundedSolidHalfEdge* h2;

    // As Java `Collections.sort`: stable, with the order of `compareTo`
    ctx.sone.sort(SplitterNullEdgeOrder());

    for ( i = 0; i < ctx.sone.size(); i++ ) {
        nextedge = ctx.sone[i].e;
        if ( ctx.struts.contains(nextedge) ) {
            orientStrut(ctx, nextedge);
        }
        h1 = canJoin(ctx, nextedge->rightHalf);
        if ( h1 != NULL ) {
            join(h1, nextedge->rightHalf, FALSE);
            ctx.tieds.add(nextedge->rightHalf);
            if ( !isLoose(ctx, h1->mirrorHalfEdge()) ) {
                cut(ctx, h1);
            }
        }
        h2 = canJoin(ctx, nextedge->leftHalf);
        if ( h2 != NULL ) {
            join(h2, nextedge->leftHalf, FALSE);
            ctx.tieds.add(nextedge->leftHalf);
            if ( !isLoose(ctx, h2->mirrorHalfEdge()) ) {
                cut(ctx, h2);
            }
        }
        if ( h1 != NULL && h2 != NULL ) {
            cut(ctx, nextedge->rightHalf);
        }
    }
}

/**
A strut (null edge from lmev(he, he)) sits at a reflex corner of a face
that the section line crosses straight through the vertex. Its halves
are `s1` (from the original vertex to the new one) followed by `s2`
in the face loop, and `s2.next()` leaves the vertex along the face
boundary in direction `a`. Joining the earlier loose end `E` of that
face to `s1` leaves `s2` in the part of the face that holds the
boundary after the vertex; joining it to `s2` leaves `s1` in the part
before. The far end of the section line must end up with the half
that stays loose, so `s1` is joined when `E` lies on the second ray
counterclockwise from `a`, and `s2` otherwise. `canJoin` pairs a right
half with a left one, so the edge's right/left roles are swapped when
needed for the chosen half to be complementary to `E`.
*/
void _PolyhedralBoundedSolidSplitter::orientStrut(_SplitContext& ctx,
    _PolyhedralBoundedSolidEdge* strut)
{
    _PolyhedralBoundedSolidHalfEdge* s1;
    _PolyhedralBoundedSolidHalfEdge* s2;
    _PolyhedralBoundedSolidHalfEdge* e = NULL;
    _PolyhedralBoundedSolidHalfEdge* wanted;
    _PolyhedralBoundedSolidFace* face;
    InfinitePlane* plane;
    Vector3Dd v;
    Vector3Dd a;
    Vector3Dd d;
    Vector3Dd n;
    long i;

    if ( strut->rightHalf->next() == strut->leftHalf ) {
        s1 = strut->rightHalf;
    }
    else {
        s1 = strut->leftHalf;
    }
    s2 = s1->next();
    face = s1->parentLoop->parentFace;
    for ( i = 0; i < ctx.ends.size(); i++ ) {
        if ( ctx.ends[i]->parentLoop->parentFace == face ) {
            e = ctx.ends[i];
            break;
        }
    }
    if ( e == NULL ) {
        return;
    }
    plane = face->getContainingPlane();
    if ( plane == NULL ) {
        return;
    }
    v = s1->startingVertex->position;
    a = s2->next()->next()->startingVertex->position.subtract(v);
    d = e->startingVertex->position.subtract(v);
    n = plane->getNormal();
    delete plane;
    if ( d.length() <= numericContext.unitVectorTolerance() ) {
        return;
    }
    if ( ccwAngle(a, d, n) > ccwAngle(a, d.multiply(-1.0), n) ) {
        wanted = s1;
    }
    else {
        wanted = s2;
    }
    BOOLEAN eIsRight = e == e->parentEdge->rightHalf;
    BOOLEAN wantedIsRight = wanted == strut->rightHalf;
    if ( eIsRight == wantedIsRight ) {
        _PolyhedralBoundedSolidHalfEdge* tmp = strut->rightHalf;
        strut->rightHalf = strut->leftHalf;
        strut->leftHalf = tmp;
    }
}

/** Counterclockwise angle about `n` from `a` to `b`, in [0, 2 pi). */
double _PolyhedralBoundedSolidSplitter::ccwAngle(const Vector3Dd& a,
    const Vector3Dd& b, const Vector3Dd& n)
{
    double angle = atan2(n.dotProduct(a.crossProduct(b)),
        a.dotProduct(b));
    if ( angle < 0.0 ) {
        return angle + 2.0 * M_PI;
    }
    return angle;
}

/**
Following section [MANT1988].14.8. and program [MANT1988].14.12.
*/
void _PolyhedralBoundedSolidSplitter::classify(_SplitContext& ctx,
                                               PolyhedralBoundedSolid* above,
                                               PolyhedralBoundedSolid* below)
{
    long i;
    long half = ctx.sonf.size()/2;

    for ( i = 0; i < half; i++ ) {
        movefac(ctx.sonf[i], above);
        ctx.facesToFixAbove.add(ctx.sonf[i]);
        movefac(ctx.sonf[i+half], below);
        ctx.facesToFixBelow.add(ctx.sonf[i+half]);
    }
}

/**
A face `a` is "inside" other `b` (and should be an internal loop) if
all vertices from `b` are inside the polygon of `a`.

PRE: given faces are coplanar.
*/
BOOLEAN _PolyhedralBoundedSolidSplitter::faceInsideFace(
    _PolyhedralBoundedSolidFace* a, _PolyhedralBoundedSolidFace* b)
{
    long i;
    _PolyhedralBoundedSolidHalfEdge* he;
    _PolyhedralBoundedSolidHalfEdge* heStart;

    for ( i = 0; i < b->boundariesList.size(); i++ ) {
        heStart = b->boundariesList.get(i)->boundaryStartHalfEdge;
        he = heStart;
        do {
            if ( a->testPointInside(he->startingVertex->position,
                    numericContext.bigEpsilon()) == Geometry::OUTSIDE ) {
                return FALSE;
            }
            he = he->next();
        } while ( he != heStart );
    }
    return TRUE;
}

void _PolyhedralBoundedSolidSplitter::fixNullFaces(
    ArrayList<_PolyhedralBoundedSolidFace*>& l)
{
    if ( l.size() == 1 ) {
        l.remove(0L);
        return;
    }

    long i;
    long j;

    for ( i = 0; i < l.size(); i++ ) {
        for ( j = 0; j < l.size(); j++ ) {
            if ( i == j ) continue;
            if ( faceInsideFace(l[j], l[i]) ) {
                PolyhedralBoundedSolidEulerOperators::lkfmrh(
                    l[i]->parentSolid, l[i], l[j]);
                l.remove((long)j);
                // Repeat he process with remaining list
                fixNullFaces(l);
                return;
            }
        }
    }
}

/**
Following section [MANT1988].14.8. and program [MANT1988].14.11.

The two halves of `sonf` are not tied to a side of the plane by the
construction, so each piece is put in the list of the side its
vertices lie on (it used to be the opposite one for every plane).
*/
void _PolyhedralBoundedSolidSplitter::splitFinish(_SplitContext& ctx,
    PolyhedralBoundedSolid* inSolid,
    const InfinitePlane& inSplittingPlane,
    ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
    ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow)
{
    long i;
    long firstHalfSize;
    _PolyhedralBoundedSolidFace* newface;
    PolyhedralBoundedSolid* first;
    PolyhedralBoundedSolid* second;

    firstHalfSize = ctx.sonf.size();
    for ( i = 0; i < firstHalfSize; i++ ) {
        newface = PolyhedralBoundedSolidEulerOperators::lmfkrh(inSolid,
            ctx.sonf[i]->boundariesList.get(1),
            inSolid->getMaxFaceId()+1);
        ctx.sonf.add(newface);
    }
    first = new PolyhedralBoundedSolid();
    second = new PolyhedralBoundedSolid();
    classify(ctx, first, second);
    fixNullFaces(ctx.facesToFixAbove);
    fixNullFaces(ctx.facesToFixBelow);
    cleanup(first);
    resolvePinchedLoops(first);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(first);
    cleanup(second);
    resolvePinchedLoops(second);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(second);
    if ( sideOf(first, inSplittingPlane) < 0 ||
         sideOf(second, inSplittingPlane) > 0 ) {
        PolyhedralBoundedSolid* swap = first;
        first = second;
        second = swap;
    }
    separateShells(first, outSolidsAbove);
    separateShells(second, outSolidsBelow);
}

/**
A plane through a reflex vertex can cut a section made of two regions
that touch at that vertex. Connect then closes both regions in one
loop that passes twice through the contact point, and the section face
is self-touching. Two forms appear, and each is resolved with Euler
operators so that every face loop is simple:

  - The loop passes twice through the same vertex: the piece
    is two regions joined only at that vertex. `lmev` splits the
    vertex between the two half-edges (the new edge, of zero length, is a
    bridge of the loop), `lkemr` removes the bridge leaving two
    rings, and `lmfkrh` makes the second ring a face of its own. The
    two regions become shells that touch at a vertex position, which
    `separateShells` returns as separate solids ([MANT1988] Sec.
    14.5.1).
  - The loop passes through two distinct vertices at the same
    position: the piece is one manifold solid whose section has two faces
    sharing that point. `lmef` splits the face with a zero-length
    edge between them and `lkev` merges the two vertices.
*/
void _PolyhedralBoundedSolidSplitter::resolvePinchedLoops(
    PolyhedralBoundedSolid* piece)
{
    BOOLEAN changed = TRUE;
    long f;
    long l;

    while ( changed ) {
        changed = FALSE;
        for ( f = 0; f < piece->getPolygonsList().size() && !changed;
              f++ ) {
            _PolyhedralBoundedSolidFace* face =
                piece->getPolygonsList().get(f);
            for ( l = 0; l < face->boundariesList.size() && !changed;
                  l++ ) {
                changed = resolveOnePinch(piece,
                    face->boundariesList.get(l));
            }
        }
    }
}

BOOLEAN _PolyhedralBoundedSolidSplitter::resolveOnePinch(
    PolyhedralBoundedSolid* piece, _PolyhedralBoundedSolidLoop* loop)
{
    long n = loop->halfEdgesList.size();
    long a;
    long b;

    if ( n < 4 ) {
        return FALSE;
    }
    for ( a = 0; a < n; a++ ) {
        for ( b = a + 2; b < n; b++ ) {
            if ( a == 0 && b == n - 1 ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge* ha = loop->halfEdgesList.get(a);
            _PolyhedralBoundedSolidHalfEdge* hb = loop->halfEdgesList.get(b);
            _PolyhedralBoundedSolidVertex* va = ha->startingVertex;
            _PolyhedralBoundedSolidVertex* vb = hb->startingVertex;

            if ( va == vb ) {
                if ( !rotatesInto(ha, hb) ) {
                    continue;
                }
                PolyhedralBoundedSolidEulerOperators::lmev(piece, ha, hb,
                    piece->getMaxVertexId() + 1,
                    Vector3Dd(va->position));
                // lmev put the new edge before ha and before hb, both
                // in this loop: it is a bridge.
                _PolyhedralBoundedSolidHalfEdge* bridgeA = ha->previous();
                _PolyhedralBoundedSolidHalfEdge* bridgeB = hb->previous();
                PolyhedralBoundedSolidEulerOperators::lkemr(piece,
                    bridgeA, bridgeB);
                _PolyhedralBoundedSolidFace* face = loop->parentFace;
                PolyhedralBoundedSolidEulerOperators::lmfkrh(piece,
                    face->boundariesList.get(
                        face->boundariesList.size() - 1),
                    piece->getMaxFaceId() + 1);
                return TRUE;
            }
            if ( PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                     va->position, vb->position, numericContext) ) {
                if ( PolyhedralBoundedSolidEulerOperators::lmef(piece, ha,
                         hb, piece->getMaxFaceId() + 1) == NULL ) {
                    continue;
                }
                // lmef put the half-edge from vb before ha and the one
                // from va before hb; lkev keeps va.
                PolyhedralBoundedSolidEulerOperators::lkev(piece,
                    ha->previous(), hb->previous());
                return TRUE;
            }
        }
    }
    return FALSE;
}

/**
True when turning around the vertex of `from` (mirror, then next,
as lmev does) reaches `to`.
*/
BOOLEAN _PolyhedralBoundedSolidSplitter::rotatesInto(
    _PolyhedralBoundedSolidHalfEdge* from,
    _PolyhedralBoundedSolidHalfEdge* to)
{
    _PolyhedralBoundedSolidHalfEdge* he = from;
    int steps;

    for ( steps = 0; steps < 1000; steps++ ) {
        if ( he == to ) {
            return TRUE;
        }
        _PolyhedralBoundedSolidHalfEdge* mirror = he->mirrorHalfEdge();
        if ( mirror == NULL ) {
            return FALSE;
        }
        he = mirror->next();
        if ( he == from ) {
            return FALSE;
        }
    }
    return FALSE;
}

/**
The pieces on one side of the plane can be several shells. Disjoint
shells stay together in one solid, as program [MANT1988].14.12 leaves
them. Shells that touch at a vertex or along an edge (the non-manifold
situations [MANT1988] Sec. 14.5.1 resolves as disconnected models) are
returned as separate solids, so that every solid in the output lists is
a valid manifold B-Rep.

C++ port note: the shells are appended to `shells` (Java returns a new
list); when `piece` is split into shells, the emptied `piece` is deleted.
*/
void _PolyhedralBoundedSolidSplitter::separateShells(
    PolyhedralBoundedSolid* piece,
    ArrayList<PolyhedralBoundedSolid*>& shells)
{
    if ( !hasTouchingShells(piece) ) {
        shells.add(piece);
        return;
    }
    while ( piece->getPolygonsList().size() > 0 ) {
        PolyhedralBoundedSolid* shell = new PolyhedralBoundedSolid();
        movefac(piece->getPolygonsList().get(0), shell);
        cleanup(shell);
        updateMaximumIds(shell);
        PolyhedralBoundedSolidValidationEngine::validateIntermediate(shell);
        shells.add(shell);
    }
    discard(piece);
}

/**
Labels the shells (connected components of the face/edge adjacency
graph) and reports whether two different shells have a vertex at the
same position. A split creates a vertex at every contact point, so
shells that touch always share a vertex position.
*/
BOOLEAN _PolyhedralBoundedSolidSplitter::hasTouchingShells(
    PolyhedralBoundedSolid* solid)
{
    TreeMap<_PolyhedralBoundedSolidFace*, int> shellOf;
    ArrayList<_PolyhedralBoundedSolidFace*> stack;
    ArrayList<_SplitterShellPoint> points;
    int shells = 0;
    long i;
    long j;
    long k;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* start = solid->getPolygonsList().get(i);
        if ( shellOf.containsKey(start) ) {
            continue;
        }
        shellOf.put(start, shells);
        stack.add(start);
        while ( !stack.isEmpty() ) {
            _PolyhedralBoundedSolidFace* f = stack.get(stack.size() - 1);
            stack.remove((long)(stack.size() - 1));
            for ( j = 0; j < f->boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop* l = f->boundariesList.get(j);
                for ( k = 0; k < l->halfEdgesList.size(); k++ ) {
                    _PolyhedralBoundedSolidHalfEdge* he =
                        l->halfEdgesList.get(k);
                    Vector3Dd p = he->startingVertex->position;
                    points.add(_SplitterShellPoint(p.x(), p.y(), p.z(),
                        (double)shells));
                    _PolyhedralBoundedSolidHalfEdge* m = he->mirrorHalfEdge();
                    if ( m != NULL && m->parentLoop != NULL &&
                         m->parentLoop->parentFace != NULL &&
                         !shellOf.containsKey(m->parentLoop->parentFace) ) {
                        shellOf.put(m->parentLoop->parentFace, shells);
                        stack.add(m->parentLoop->parentFace);
                    }
                }
            }
        }
        shells++;
    }
    if ( shells <= 1 ) {
        return FALSE;
    }

    double tolerance = numericContext.bigEpsilon();
    points.sort(SplitterShellPointOrder());
    for ( i = 0; i < points.size(); i++ ) {
        const _SplitterShellPoint& u = points[i];
        for ( j = i + 1; j < points.size() &&
                  points[j].x - u.x <= tolerance; j++ ) {
            const _SplitterShellPoint& w = points[j];
            if ( u.shell != w.shell &&
                 fabs(u.y - w.y) <= tolerance &&
                 fabs(u.z - w.z) <= tolerance ) {
                return TRUE;
            }
        }
    }
    return FALSE;
}

void _PolyhedralBoundedSolidSplitter::updateMaximumIds(
    PolyhedralBoundedSolid* solid)
{
    int maxVertex = -1;
    int maxFace = -1;
    long i;

    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        maxVertex = Math::max(maxVertex,
            solid->getVerticesList().get(i)->id);
    }
    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        maxFace = Math::max(maxFace, solid->getPolygonsList().get(i)->id);
    }
    solid->setMaxVertexId(maxVertex);
    solid->setMaxFaceId(maxFace);
}

/**
True when some vertex lies strictly above the plane and some strictly
below. Otherwise the plane misses the solid or only touches it (at a
vertex, along an edge or on a face), there is nothing to cut, and
running the algorithm would only insert null edges along the contact
and return a degenerate piece.
*/
BOOLEAN _PolyhedralBoundedSolidSplitter::straddles(
    PolyhedralBoundedSolid* solid, const InfinitePlane& plane)
{
    BOOLEAN above = FALSE;
    BOOLEAN below = FALSE;
    long i;

    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        int side = compareToZero(plane.pointDistance(
            solid->getVerticesList().get(i)->position));
        if ( side > 0 ) {
            above = TRUE;
        }
        if ( side < 0 ) {
            below = TRUE;
        }
        if ( above && below ) {
            return TRUE;
        }
    }
    return FALSE;
}

/**
Side of the plane a piece lies on: the sign of the signed distance of
its farthest vertex (0 when every vertex is on the plane or the piece
is empty).
*/
int _PolyhedralBoundedSolidSplitter::sideOf(PolyhedralBoundedSolid* piece,
    const InfinitePlane& plane)
{
    double best = 0.0;
    long i;

    for ( i = 0; i < piece->getVerticesList().size(); i++ ) {
        double d = plane.pointDistance(
            piece->getVerticesList().get(i)->position);
        if ( fabs(d) > fabs(best) ) {
            best = d;
        }
    }
    return compareToZero(best);
}

/**
C++ port note, no Java counterpart (garbage collected there): deletes a
solid whose faces were moved to other solids. Its lists are emptied first
without deleting their nodes, which belong to those solids now.
*/
void _PolyhedralBoundedSolidSplitter::discard(PolyhedralBoundedSolid* emptied)
{
    emptied->getPolygonsList().clear();
    emptied->getEdgesList().clear();
    emptied->getVerticesList().clear();
    delete emptied;
}

void _PolyhedralBoundedSolidSplitter::split(
    PolyhedralBoundedSolid* inSolid,
    const InfinitePlane& inSplittingPlane,
    ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
    ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow)
{
    PolyhedralBoundedSolidStatistics::recordSplitCall();
    int aboveBefore = (int)outSolidsAbove.size();
    int belowBefore = (int)outSolidsBelow.size();
    _SplitContext ctx;
    PolyhedralBoundedSolid* work;

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolid(inSolid);
    setNumericContext(&context);

    PolyhedralBoundedSolidValidationEngine::validateIntermediate(inSolid);
    work = NULL;
    if ( straddles(inSolid, inSplittingPlane) ) {
        work = PolyhedralBoundedSolidStructuralCopy::copy(inSolid);
        splitGenerate(ctx, work, inSplittingPlane);
        splitClassify(ctx, work, inSplittingPlane);
    }

    if ( ctx.sone.size() <= 0 ) {
        PolyhedralBoundedSolidStatistics::recordSplitNoNullEdgesCase();
        if ( sideOf(inSolid, inSplittingPlane) < 0 ) {
            outSolidsAbove.add(new PolyhedralBoundedSolid());
            outSolidsBelow.add(
                PolyhedralBoundedSolidStructuralCopy::copy(inSolid));
        }
        else {
            outSolidsAbove.add(
                PolyhedralBoundedSolidStructuralCopy::copy(inSolid));
            outSolidsBelow.add(new PolyhedralBoundedSolid());
        }
        // C++ port note: the copy that was cut by nothing is not used
        delete work;
    }
    else {
        splitConnect(ctx);
        splitFinish(ctx, work, inSplittingPlane, outSolidsAbove,
            outSolidsBelow);
        // C++ port note: the faces of the copy were moved to the pieces
        discard(work);
    }
    PolyhedralBoundedSolidStatistics::recordSplitProducedSolids(
        (int)outSolidsAbove.size() - aboveBefore,
        (int)outSolidsBelow.size() - belowBefore);
}
