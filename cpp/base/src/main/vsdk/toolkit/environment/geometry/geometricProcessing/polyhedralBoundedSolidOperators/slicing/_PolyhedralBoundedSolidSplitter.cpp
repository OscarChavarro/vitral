//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "java/lang/String.h"
#include <cmath>
#include <cstdio>

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidVertexNeighborhood.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/slicing/_PolyhedralBoundedSolidSplitter.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

namespace {

/**
C++ port note: the Java `double[] {x, y, z, shell}` entries of
`hasTouchingShells`.
*/
struct _ShellPoint
{
    double x;
    double y;
    double z;
    double shell;
};

}

//= _PolyhedralBoundedSolidSplitterSectorClassification ===================

java::String _PolyhedralBoundedSolidSplitterSectorClassification::toString() const
{
    java::String msg = "{";
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
    return java::String(msg.c_str());
}

//= _PolyhedralBoundedSolidSplitterNullEdge ===============================

_PolyhedralBoundedSolidSplitterNullEdge::_PolyhedralBoundedSolidSplitterNullEdge(
    _PolyhedralBoundedSolidEdge* e,
    const PolyhedralBoundedSolidNumericPolicy::ToleranceContext* numericContext)
    : e(e)
{
    this->numericContext = numericContext == nullptr ?
        PolyhedralBoundedSolidNumericPolicy::defaultContext() :
        *numericContext;
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
    int i;

    for ( i = 0; i < ctx.soov.size(); i++ ) {
        if ( ctx.soov.get(i) == v ) {
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
    int i;

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

Sector convention. Entries follow the half-edges `he_k` leaving `vtx`,
with `he_(k+1) = mirror(he_k).next()`. Entry k classifies the edge `e_k`
of `he_k`. The face of `he_k` lies between `e_(k-1)` and `e_k`, so a
change of class between entries k-1 and k means the plane crosses the
face of `he_k`: the sector of entry k. A reflex face corner gets an extra
(wide) entry at its inward bisector, placed BEFORE the entry of its
half-edge, i.e. between `e_(k-1)` and `e_k` where it geometrically belongs
(problem [MANT1988].14.1). The same convention is used by the set
operator's vertex/face classifier.
*/
java::ArrayList<_PolyhedralBoundedSolidSplitterSectorClassification>
_PolyhedralBoundedSolidSplitter::getNeighborhood(
    _PolyhedralBoundedSolidVertex* vtx, const InfinitePlane& inSplittingPlane)
{
    return _PolyhedralBoundedSolidVertexNeighborhood::classify<
        _PolyhedralBoundedSolidSplitterSectorClassification>(vtx,
        inSplittingPlane);
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
    java::ArrayList<SectorClassification>& nbr,
    const InfinitePlane& inSplittingPlane)
{
    _PolyhedralBoundedSolidFace* f;
    Vector3Dd c;
    double d;
    int i;

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

The two rules with equal neighbours are only right for a reflex (concave)
edge, where the solid lies on both sides of the plane and the section is
pinched at the edge. A convex edge with both neighbours on one side merely
touches the plane: reclassifying it to the other side would insert a pair
of null edges around a sector of zero width and produce a two-vertex null
face (found with the split fuzz, Stage E). A convex touching edge
therefore keeps the class of its neighbours.
*/
void _PolyhedralBoundedSolidSplitter::reclassifyOnEdges(
    java::ArrayList<SectorClassification>& nbr)
{
    int prev;
    int next;
    int i;
    int n = nbr.size();

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
bool _PolyhedralBoundedSolidSplitter::isConvexEdge(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    InfinitePlane* pa;
    InfinitePlane* pb;
    Vector3Dd t;
    double s;

    if ( he == nullptr || he->parentEdge == nullptr ) {
        return false;
    }
    pa = he->parentLoop->parentFace->getContainingPlane();
    pb = he->mirrorHalfEdge()->parentLoop->parentFace->getContainingPlane();
    if ( pa == nullptr || pb == nullptr ) {
        delete pa;
        delete pb;
        return false;
    }
    t = he->next()->startingVertex->position.subtract(
        he->startingVertex->position);
    if ( t.length() <= numericContext.unitVectorTolerance() ) {
        delete pa;
        delete pb;
        return false;
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

The null edge is inserted in the sectors where the class changes: the
entry AFTER each BELOW/ABOVE and ABOVE/BELOW transition (the face of that
entry's half-edge is the one the plane crosses). Taking the entry before
the transition, as this code used to, puts the null edge in the wrong face
whenever the ON-vertex has more than two edges, so no null edges could be
joined and the whole solid was lost (defect S-01, plane through a vertex).
*/
void _PolyhedralBoundedSolidSplitter::insertNullEdges(
    _SplitContext& ctx,
    java::ArrayList<SectorClassification>& nbr,
    PolyhedralBoundedSolid* inSolid, const InfinitePlane& inSplittingPlane)
{
    int start;
    int i;
    _PolyhedralBoundedSolidHalfEdge* head;
    _PolyhedralBoundedSolidHalfEdge* tail;
    int nnbr = nbr.size();

    (void)inSplittingPlane;
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
    while ( true ) {
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
            ctx.struts.put(tail->previous()->parentEdge, true);
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

Earlier versions reversed the neighborhood when it had in-plane edges, to
compensate for taking the null-edge sectors one entry too early. With the
sector convention documented on `getNeighborhood` no reversal is needed.
*/
void _PolyhedralBoundedSolidSplitter::splitClassify(_SplitContext& ctx,
    PolyhedralBoundedSolid* inSolid, const InfinitePlane& inSplittingPlane)
{
    int i;

    /// Following variable `nbr` from program [MANT1988].14.3.
    java::ArrayList<SectorClassification> nbr;

    for ( i = 0; i < ctx.soov.size(); i++ ) {
        nbr = getNeighborhood(ctx.soov.get(i), inSplittingPlane);
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
    int i;

    for ( i = 0; i < ctx.ends.size(); i++ ) {
        if ( neighbor(he, ctx.ends.get(i)) ) {
            ret = ctx.ends.get(i);
            ctx.ends.remove((long int)i);
            ctx.tieds.add(ret);
            return ret;
        }
    }
    ctx.ends.add(he);
    return nullptr;
}

void _PolyhedralBoundedSolidSplitter::printNbr(
    const java::ArrayList<SectorClassification>& neighborSectorsInfo)
{
    int i;

    for ( i = 0; i < neighborSectorsInfo.size(); i++ ) {
        printf("  - %s\n", neighborSectorsInfo.get(i).toString().c_str());
    }
}

void _PolyhedralBoundedSolidSplitter::printEnds(_SplitContext& ctx)
{
    int i;

    for ( i = 0; i < ctx.ends.size(); i++ ) {
        printf("  - ends[%d]: %s\n", i, ctx.ends.get(i)->toString().c_str());
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
        PolyhedralBoundedSolidEulerOperators::lkemr(s, he->parentEdge->rightHalf, he->parentEdge->leftHalf);
    }
    else {
        PolyhedralBoundedSolidEulerOperators::lkef(s, he->parentEdge->rightHalf, he->parentEdge->leftHalf);
    }
}

bool _PolyhedralBoundedSolidSplitter::isLoose(_SplitContext& ctx,
    _PolyhedralBoundedSolidHalfEdge* he)
{
    int i;

    for ( i = 0; i < ctx.tieds.size(); i++ ) {
        if ( he == ctx.tieds.get(i) ) return false;
    }

    return true;
}

/**
Following section [MANT1988].14.7.2. and program [MANT1988].14.9.
*/
void _PolyhedralBoundedSolidSplitter::splitConnect(_SplitContext& ctx)
{
    int i;
    _PolyhedralBoundedSolidEdge* nextedge;
    _PolyhedralBoundedSolidHalfEdge* h1;
    _PolyhedralBoundedSolidHalfEdge* h2;

    // As Java `Collections.sort`: stable, with the order of `compareTo`
    ctx.sone.sort([](const _PolyhedralBoundedSolidSplitterNullEdge& a,
           const _PolyhedralBoundedSolidSplitterNullEdge& b) {
            return a.compareTo(b);
        });

    for ( i = 0; i < ctx.sone.size(); i++ ) {
        nextedge = ctx.sone.get(i).e;
        if ( ctx.struts.containsKey(nextedge) ) {
            orientStrut(ctx, nextedge);
        }
        h1 = canJoin(ctx, nextedge->rightHalf);
        if ( h1 != nullptr ) {
            join(h1, nextedge->rightHalf, false);
            ctx.tieds.add(nextedge->rightHalf);
            if ( !isLoose(ctx, h1->mirrorHalfEdge()) ) {
                cut(ctx, h1);
            }
        }
        h2 = canJoin(ctx, nextedge->leftHalf);
        if ( h2 != nullptr ) {
            join(h2, nextedge->leftHalf, false);
            ctx.tieds.add(nextedge->leftHalf);
            if ( !isLoose(ctx, h2->mirrorHalfEdge()) ) {
                cut(ctx, h2);
            }
        }
        if ( h1 != nullptr && h2 != nullptr ) {
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
    _PolyhedralBoundedSolidHalfEdge* e = nullptr;
    _PolyhedralBoundedSolidHalfEdge* wanted;
    _PolyhedralBoundedSolidFace* face;
    Vector3Dd v;
    Vector3Dd a;
    Vector3Dd d;
    Vector3Dd n;
    int i;

    s1 = strut->rightHalf->next() == strut->leftHalf ?
        strut->rightHalf : strut->leftHalf;
    s2 = s1->next();
    face = s1->parentLoop->parentFace;
    for ( i = 0; i < ctx.ends.size(); i++ ) {
        if ( ctx.ends.get(i)->parentLoop->parentFace == face ) {
            e = ctx.ends.get(i);
            break;
        }
    }
    InfinitePlane* facePlane = face->getContainingPlane();
    if ( e == nullptr || facePlane == nullptr ) {
        delete facePlane;
        return;
    }
    v = s1->startingVertex->position;
    a = s2->next()->next()->startingVertex->position.subtract(v);
    d = e->startingVertex->position.subtract(v);
    n = facePlane->getNormal();
    delete facePlane;
    if ( d.length() <= numericContext.unitVectorTolerance() ) {
        return;
    }
    wanted = ccwAngle(a, d, n) > ccwAngle(a, d.multiply(-1.0), n) ?
        s1 : s2;
    bool eIsRight = e == e->parentEdge->rightHalf;
    bool wantedIsRight = wanted == strut->rightHalf;
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
    double angle = std::atan2(n.dotProduct(a.crossProduct(b)),
        a.dotProduct(b));
    return angle < 0.0 ? angle + 2.0 * java::Math::PI : angle;
}

/**
Following section [MANT1988].14.8. and program [MANT1988].14.12.
*/
void _PolyhedralBoundedSolidSplitter::classify(_SplitContext& ctx,
                                               PolyhedralBoundedSolid* above,
                                               PolyhedralBoundedSolid* below)
{
    int i;
    int half = ctx.sonf.size()/2;

    for ( i = 0; i < half; i++ ) {
        movefac(ctx.sonf.get(i), above);
        ctx.facesToFixAbove.add(ctx.sonf.get(i));
        movefac(ctx.sonf.get(i+half), below);
        ctx.facesToFixBelow.add(ctx.sonf.get(i+half));
    }
}

/**
A face `a` is "inside" other `b` (and should be an internal loop) if
all vertices from `b` are inside the polygon of `a`.

PRE: given faces are coplanar.
*/
bool _PolyhedralBoundedSolidSplitter::faceInsideFace(
    _PolyhedralBoundedSolidFace* a, _PolyhedralBoundedSolidFace* b)
{
    int i;
    _PolyhedralBoundedSolidHalfEdge* he;
    _PolyhedralBoundedSolidHalfEdge* heStart;

    for ( i = 0; i < b->boundariesList.size(); i++ ) {
        heStart = b->boundariesList.get(i)->boundaryStartHalfEdge;
        he = heStart;
        do {
            if ( a->testPointInside(he->startingVertex->position,
                    numericContext.bigEpsilon()) == Geometry::OUTSIDE ) {
                return false;
            }
            he = he->next();
        } while ( he != heStart );
    }
    return true;
}

void _PolyhedralBoundedSolidSplitter::fixNullFaces(
    java::ArrayList<_PolyhedralBoundedSolidFace*>& l)
{
    if ( l.size() == 1 ) {
        l.remove(0L);
        return;
    }

    int i;
    int j;

    for ( i = 0; i < l.size(); i++ ) {
        for ( j = 0; j < l.size(); j++ ) {
            if ( i == j ) continue;
            if ( faceInsideFace(l.get(j), l.get(i)) ) {
                PolyhedralBoundedSolidEulerOperators::lkfmrh(
                    l.get(i)->parentSolid, l.get(i), l.get(j));
                l.remove((long int)j);
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
construction, so each piece is put in the list of the side its vertices
lie on (it used to be the opposite one for every plane).
*/
void _PolyhedralBoundedSolidSplitter::splitFinish(_SplitContext& ctx,
    PolyhedralBoundedSolid* inSolid,
    const InfinitePlane& inSplittingPlane,
    java::ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
    java::ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow)
{
    int i;
    int firstHalfSize;
    _PolyhedralBoundedSolidFace* newface;
    PolyhedralBoundedSolid* first;
    PolyhedralBoundedSolid* second;

    firstHalfSize = ctx.sonf.size();
    for ( i = 0; i < firstHalfSize; i++ ) {
        newface = PolyhedralBoundedSolidEulerOperators::lmfkrh(inSolid,
            ctx.sonf.get(i)->boundariesList.get(1),
            inSolid->getMaxFaceId()+1);
        ctx.sonf.add(newface);
    }
    first = new PolyhedralBoundedSolid();
    second = new PolyhedralBoundedSolid();
    classify(ctx, first, second);
    forgetMovedNodes(inSolid);
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
    java::ArrayList<PolyhedralBoundedSolid*> shells;
    int k;
    shells = separateShells(first);
    for ( k = 0; k < shells.size(); k++ ) {
        outSolidsAbove.add(shells.get(k));
    }
    if ( shells.size() != 1 || shells.get(0) != first ) {
        releaseIntermediate(first, shells);
    }
    shells = separateShells(second);
    for ( k = 0; k < shells.size(); k++ ) {
        outSolidsBelow.add(shells.get(k));
    }
    if ( shells.size() != 1 || shells.get(0) != second ) {
        releaseIntermediate(second, shells);
    }
}

/**
A plane through a reflex vertex can cut a section made of two regions
that touch at that vertex. Connect then closes both regions in one
loop that passes twice through the contact point, and the section face
is self-touching. Two forms appear, and each is resolved with Euler
operators so that every face loop is simple:

- The loop passes twice through the same vertex: the piece is two regions
  joined only at that vertex. `lmev` splits the vertex between the two
  half-edges (the new edge, of zero length, is a bridge of the loop),
  `lkemr` removes the bridge leaving two rings, and `lmfkrh` makes the
  second ring a face of its own. The two regions become shells that touch
  at a vertex position, which `separateShells` returns as separate solids
  ([MANT1988] Sec. 14.5.1).
- The loop passes through two distinct vertices at the same position: the
  piece is one manifold solid whose section has two faces sharing that
  point. `lmef` splits the face with a zero-length edge between them and
  `lkev` merges the two vertices.
*/
void _PolyhedralBoundedSolidSplitter::resolvePinchedLoops(
    PolyhedralBoundedSolid* piece)
{
    bool changed = true;

    while ( changed ) {
        changed = false;
        int f;
        for ( f = 0; f < piece->getPolygonsList().size() && !changed;
              f++ ) {
            _PolyhedralBoundedSolidFace* face =
                piece->getPolygonsList().get(f);
            int l;
            for ( l = 0; l < face->boundariesList.size() && !changed;
                  l++ ) {
                changed = resolveOnePinch(piece,
                    face->boundariesList.get(l));
            }
        }
    }
}

bool _PolyhedralBoundedSolidSplitter::resolveOnePinch(
    PolyhedralBoundedSolid* piece, _PolyhedralBoundedSolidLoop* loop)
{
    int n = loop->halfEdgesList.size();
    int a;
    int b;

    if ( n < 4 ) {
        return false;
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
                return true;
            }
            if ( PolyhedralBoundedSolidNumericPolicy::pointsCoincident(
                     va->position, vb->position, numericContext) ) {
                if ( PolyhedralBoundedSolidEulerOperators::lmef(piece, ha,
                         hb, piece->getMaxFaceId() + 1) == nullptr ) {
                    continue;
                }
                // lmef put the half-edge from vb before ha and the one
                // from va before hb; lkev keeps va.
                PolyhedralBoundedSolidEulerOperators::lkev(piece,
                    ha->previous(), hb->previous());
                return true;
            }
        }
    }
    return false;
}

/**
True when turning around the vertex of `from` (mirror, then next, as lmev
does) reaches `to`.
*/
bool _PolyhedralBoundedSolidSplitter::rotatesInto(
    _PolyhedralBoundedSolidHalfEdge* from, _PolyhedralBoundedSolidHalfEdge* to)
{
    _PolyhedralBoundedSolidHalfEdge* he = from;
    int steps;

    for ( steps = 0; steps < 1000; steps++ ) {
        if ( he == to ) {
            return true;
        }
        _PolyhedralBoundedSolidHalfEdge* mirror = he->mirrorHalfEdge();
        if ( mirror == nullptr ) {
            return false;
        }
        he = mirror->next();
        if ( he == from ) {
            return false;
        }
    }
    return false;
}

/**
The pieces on one side of the plane can be several shells. Disjoint
shells stay together in one solid, as program [MANT1988].14.12 leaves
them. Shells that touch at a vertex or along an edge (the non-manifold
situations [MANT1988] Sec. 14.5.1 resolves as disconnected models) are
returned as separate solids, so that every solid in the output lists is
a valid manifold B-Rep.
*/
java::ArrayList<PolyhedralBoundedSolid*>
_PolyhedralBoundedSolidSplitter::separateShells(PolyhedralBoundedSolid* piece)
{
    java::ArrayList<PolyhedralBoundedSolid*> shells;

    if ( !hasTouchingShells(piece) ) {
        shells.add(piece);
        return shells;
    }
    while ( piece->getPolygonsList().size() > 0 ) {
        PolyhedralBoundedSolid* shell = new PolyhedralBoundedSolid();
        movefac(piece->getPolygonsList().get(0), shell);
        cleanup(shell);
        updateMaximumIds(shell);
        PolyhedralBoundedSolidValidationEngine::validateIntermediate(shell);
        shells.add(shell);
    }
    return shells;
}

/**
Labels the shells (connected components of the face/edge adjacency
graph) and reports whether two different shells have a vertex at the
same position. A split creates a vertex at every contact point, so
shells that touch always share a vertex position.
*/
bool _PolyhedralBoundedSolidSplitter::hasTouchingShells(
    PolyhedralBoundedSolid* solid)
{
    java::HashMap<_PolyhedralBoundedSolidFace*, int> shellOf;
    java::ArrayList<_PolyhedralBoundedSolidFace*> stack;
    java::ArrayList<_ShellPoint> points;
    int shells = 0;
    int i;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* start = solid->getPolygonsList().get(i);
        if ( shellOf.containsKey(start) ) {
            continue;
        }
        shellOf.put(start, shells);
        stack.add(start);
        while ( !stack.isEmpty() ) {
            _PolyhedralBoundedSolidFace* f = stack.get(stack.size() - 1);
            stack.remove(stack.size() - 1);
            int j;
            for ( j = 0; j < f->boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop* l = f->boundariesList.get(j);
                int k;
                for ( k = 0; k < l->halfEdgesList.size(); k++ ) {
                    _PolyhedralBoundedSolidHalfEdge* he =
                        l->halfEdgesList.get(k);
                    Vector3Dd p = he->startingVertex->position;
                    _ShellPoint entry;
                    entry.x = p.x();
                    entry.y = p.y();
                    entry.z = p.z();
                    entry.shell = shells;
                    points.add(entry);
                    _PolyhedralBoundedSolidHalfEdge* m = he->mirrorHalfEdge();
                    if ( m != nullptr && m->parentLoop != nullptr &&
                         m->parentLoop->parentFace != nullptr &&
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
        return false;
    }

    double tolerance = numericContext.bigEpsilon();
    points.sort([](const _ShellPoint& u, const _ShellPoint& w) {
            return java::Double::compare(u.x, w.x);
        });
    for ( i = 0; i < points.size(); i++ ) {
        _ShellPoint u = points.get(i);
        int j;
        for ( j = i + 1; j < points.size() &&
                  points.get(j).x - u.x <= tolerance; j++ ) {
            _ShellPoint w = points.get(j);
            if ( u.shell != w.shell &&
                 java::Math::abs(u.y - w.y) <= tolerance &&
                 java::Math::abs(u.z - w.z) <= tolerance ) {
                return true;
            }
        }
    }
    return false;
}

void _PolyhedralBoundedSolidSplitter::updateMaximumIds(
    PolyhedralBoundedSolid* solid)
{
    int maxVertex = -1;
    int maxFace = -1;
    int i;

    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        maxVertex = java::Math::max(maxVertex,
            solid->getVerticesList().get(i)->id);
    }
    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        maxFace = java::Math::max(maxFace, solid->getPolygonsList().get(i)->id);
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
bool _PolyhedralBoundedSolidSplitter::straddles(PolyhedralBoundedSolid* solid,
    const InfinitePlane& plane)
{
    bool above = false;
    bool below = false;
    int i;

    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        int side = compareToZero(plane.pointDistance(
            solid->getVerticesList().get(i)->position));
        above |= side > 0;
        below |= side < 0;
        if ( above && below ) {
            return true;
        }
    }
    return false;
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
    int i;

    for ( i = 0; i < piece->getVerticesList().size(); i++ ) {
        double d = plane.pointDistance(
            piece->getVerticesList().get(i)->position);
        if ( java::Math::abs(d) > java::Math::abs(best) ) {
            best = d;
        }
    }
    return compareToZero(best);
}

/**
Removes from the lists of `s` the nodes another list holds now. `cleanup`
lists the edges and vertices of the moved faces in `first` and `second`
without unlisting them from the working copy; `resolvePinchedLoops` then
deletes some of them (lkev, lkemr), and `releaseIntermediate` of the
working copy would read them after that.
*/
template <class E>
static void keepOwnedOnly(CircularArrayList<E*>& list)
{
    java::ArrayList<E*> owned;
    int i;

    for ( i = 0; i < list.size(); i++ ) {
        if ( list.get(i)->getListOwner() == &list ) {
            owned.add(list.get(i));
        }
    }
    if ( owned.size() == list.size() ) {
        return;
    }
    list.clear();
    for ( i = 0; i < owned.size(); i++ ) {
        list.add(owned.get(i));
    }
}

void _PolyhedralBoundedSolidSplitter::forgetMovedNodes(
    PolyhedralBoundedSolid* s)
{
    keepOwnedOnly(s->getPolygonsList());
    keepOwnedOnly(s->getEdgesList());
    keepOwnedOnly(s->getVerticesList());
}

void _PolyhedralBoundedSolidSplitter::releaseIntermediate(
    PolyhedralBoundedSolid* s,
    const java::ArrayList<PolyhedralBoundedSolid*>& results)
{
    // The lists are cleared before any node is deleted: `clear` reads the
    // list owner of every element it detaches.
    java::ArrayList<_PolyhedralBoundedSolidFace*> faces;
    java::ArrayList<_PolyhedralBoundedSolidEdge*> edges;
    java::ArrayList<_PolyhedralBoundedSolidVertex*> vertices;
    int i;
    int k;

    // A node that a result lists (live or retired by an Euler operator) is
    // not released with `s`
    for ( k = 0; k < results.size(); k++ ) {
        s->unlistNodesOwnedBy(results.get(k));
    }
    for ( i = 0; i < s->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* f = s->getPolygonsList().get(i);
        bool kept = false;
        for ( k = 0; k < results.size(); k++ ) {
            if ( f->getListOwner() == &results.get(k)->getPolygonsList() ) {
                kept = true;
            }
        }
        if ( !kept ) {
            faces.add(f);
        }
    }
    for ( i = 0; i < s->getEdgesList().size(); i++ ) {
        _PolyhedralBoundedSolidEdge* e = s->getEdgesList().get(i);
        bool kept = false;
        for ( k = 0; k < results.size(); k++ ) {
            if ( e->getListOwner() == &results.get(k)->getEdgesList() ) {
                kept = true;
            }
        }
        if ( !kept ) {
            edges.add(e);
        }
    }
    for ( i = 0; i < s->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* v = s->getVerticesList().get(i);
        bool kept = false;
        for ( k = 0; k < results.size(); k++ ) {
            if ( v->getListOwner() == &results.get(k)->getVerticesList() ) {
                kept = true;
            }
        }
        if ( !kept ) {
            vertices.add(v);
        }
    }
    s->getPolygonsList().clear();
    s->getEdgesList().clear();
    s->getVerticesList().clear();
    for ( i = 0; i < faces.size(); i++ ) {
        delete faces.get(i);
    }
    for ( i = 0; i < edges.size(); i++ ) {
        delete edges.get(i);
    }
    for ( i = 0; i < vertices.size(); i++ ) {
        delete vertices.get(i);
    }
    delete s;
}

void _PolyhedralBoundedSolidSplitter::split(
    PolyhedralBoundedSolid* inSolid,
    const InfinitePlane& inSplittingPlane,
    java::ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
    java::ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow)
{
    PolyhedralBoundedSolidStatistics::recordSplitCall();
    int aboveBefore = outSolidsAbove.size();
    int belowBefore = outSolidsBelow.size();
    _SplitContext ctx;
    PolyhedralBoundedSolid* work;

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolid(inSolid);
    setNumericContext(&context);

    PolyhedralBoundedSolidValidationEngine::validateIntermediate(inSolid);
    work = nullptr;
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
    }
    else {
        splitConnect(ctx);
        splitFinish(ctx, work, inSplittingPlane, outSolidsAbove,
            outSolidsBelow);
    }
    PolyhedralBoundedSolidStatistics::recordSplitProducedSolids(
        outSolidsAbove.size() - aboveBefore,
        outSolidsBelow.size() - belowBefore);
    if ( work != nullptr ) {
        java::ArrayList<PolyhedralBoundedSolid*> results;
        int i;
        for ( i = aboveBefore; i < outSolidsAbove.size(); i++ ) {
            results.add(outSolidsAbove.get(i));
        }
        for ( i = belowBefore; i < outSolidsBelow.size(); i++ ) {
            results.add(outSolidsBelow.get(i));
        }
        releaseIntermediate(work, results);
    }
}
