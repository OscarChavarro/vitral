//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.dataStructures.CircularDoubleLinkedList;
import vsdk.toolkit.common.statistics.PolyhedralBoundedSolidStatistics;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.processing.ProcessingElement;

/**
Shared low-level services for B-Rep operators. This class is not intended to
be used directly; it provides common behavior for slicing and boolean
operators.
*/
public class _PolyhedralBoundedSolidOperator extends ProcessingElement
{
    public static final int UNION = PolyhedralBoundedSolidModeler.UNION;
    public static final int INTERSECTION =
        PolyhedralBoundedSolidModeler.INTERSECTION;
    public static final int SUBTRACT = PolyhedralBoundedSolidModeler.SUBTRACT;
    public static final int DIFFERENCE = SUBTRACT;

    protected static PolyhedralBoundedSolidNumericPolicy.ToleranceContext
        numericContext = PolyhedralBoundedSolidNumericPolicy.defaultContext();

    protected static void setNumericContext(
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context)
    {
        if ( context == null ) {
            numericContext = PolyhedralBoundedSolidNumericPolicy.defaultContext();
        }
        else {
            numericContext = context;
        }
    }

    protected static _PolyhedralBoundedSolidIdNamespace idNamespace = null;

    protected static void setIdNamespace(
        _PolyhedralBoundedSolidIdNamespace ns)
    {
        idNamespace = ns;
    }

    private static boolean searchForEdge(
        CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge> l,
        _PolyhedralBoundedSolidEdge e)
    {
        int i;

        for ( i = 0; i < l.size(); i++ ) {
            if ( l.get(i) == e ) return true;
        }
        return false;
    }

    private static boolean searchForVertex(
        CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex> l,
        _PolyhedralBoundedSolidVertex v)
    {
        int i;

        for ( i = 0; i < l.size(); i++ ) {
            if ( l.get(i) == v ) return true;
        }
        return false;
    }

    /**
    Following section [MANT1988].14.7.1 and program [MANT1988].14.8.
    */
    protected static boolean neighbor(_PolyhedralBoundedSolidHalfEdge h1, _PolyhedralBoundedSolidHalfEdge h2)
    {
        return (h1.parentLoop.parentFace == h2.parentLoop.parentFace) &&
            ( (
              h1 == h1.parentEdge.rightHalf && h2 == h2.parentEdge.leftHalf
              ) || 
              (
              h1 == h1.parentEdge.leftHalf && h2 == h2.parentEdge.rightHalf
              ) );
    }

    /**
    This is the answer to problem [MANT1988].14.2.

    \todo  Check for consistency of `emanatingHalfEdge` pointers for vertices.
    */
    protected static void cleanup(PolyhedralBoundedSolid s)
    {
        int i;
        int j;
        _PolyhedralBoundedSolidFace f;
        _PolyhedralBoundedSolidLoop l;
        _PolyhedralBoundedSolidHalfEdge he;

        for ( i = 0; i < s.getPolygonsList().size(); i++ ) {
            f = s.getPolygonsList().get(i);
            for ( j = 0; j < f.boundariesList.size(); j++ ) {
                l = f.boundariesList.get(j);
                he = l.boundaryStartHalfEdge;
                do {
                    //
                    if ( !searchForEdge(s.getEdgesList(), he.parentEdge) ) {
                        s.getEdgesList().add(he.parentEdge);
                    }
                    if ( !searchForVertex(s.getVerticesList(), he.startingVertex) ) {
                        s.getVerticesList().add(he.startingVertex);
                        he.startingVertex.emanatingHalfEdge = he;
                    }
                    //
                    he = he.next();
                } while( he != l.boundaryStartHalfEdge );
            }
        }
    }

    /**
    Following section [MANT1988].14.8. and program [MANT1988].14.12.
    */
    protected static void movefac(_PolyhedralBoundedSolidFace f,
                                  PolyhedralBoundedSolid s)
    {
        _PolyhedralBoundedSolidLoop l;
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidFace f2;
        int i;

        if ( !canMoveFace(f) ) {
            return;
        }

        f.parentSolid.getPolygonsList().locateWindowAtElem(f);
        f.parentSolid.getPolygonsList().removeElemAtWindow();
        s.getPolygonsList().add(f);
        f.parentSolid = s;

        for ( i = 0; i < f.boundariesList.size(); i++ ) {
            l = f.boundariesList.get(i);
            he = l.boundaryStartHalfEdge;
            do {
                _PolyhedralBoundedSolidHalfEdge mirror = he.mirrorHalfEdge();
                if ( mirror == null || mirror.parentLoop == null ||
                     mirror.parentLoop.parentFace == null ) {
                    he = he.next();
                    continue;
                }
                f2 = mirror.parentLoop.parentFace;
                if ( f2.parentSolid != s && canMoveFace(f2) ) {
                    movefac(f2, s);
                }
                he = he.next();
            } while( he != l.boundaryStartHalfEdge );
        }                
    }

    private static boolean canMoveFace(_PolyhedralBoundedSolidFace f)
    {
        int i;

        if ( f == null || f.boundariesList == null ) {
            return false;
        }
        for ( i = 0; i < f.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop l = f.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start;
            _PolyhedralBoundedSolidHalfEdge he;
            int guard;

            if ( l == null || l.boundaryStartHalfEdge == null ||
                 l.halfEdgesList == null ) {
                return false;
            }
            start = l.boundaryStartHalfEdge;
            he = start;
            guard = 0;
            do {
                if ( he == null ||
                     he.parentEdge == null ||
                     he.parentLoop == null ||
                     he.startingVertex == null ||
                     he.mirrorHalfEdge() == null ||
                     he.next() == null ) {
                    return false;
                }
                he = he.next();
                guard++;
            } while ( he != start && guard <= l.halfEdgesList.size() + 1 );
            if ( he != start ) {
                return false;
            }
        }
        return true;
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    Answer to problem [MANT1988].14.1.

    Current implementation assumes the following interpretation:
    Given a vertex of interest `he.startingVertex`, one can measure the angle
    of incidence of loop `he.parentLoop` on vertex of interest by measuring the
    angle between the halfedges `he` (direction `a`) and `he.previous` 
    (direction `b`).  The bisector vector is the one having its tail on the
    vertex of interest position `he.startingVertex.position` and its end
    pointing in the middle of `a` and `b` directions.

    This is the answer to problem [MANT1988].14.1.

    \todo : check current assumptions!

    This protected method is here for exclusive use of subclasses
    `_PolyhedralBoundedSolidSplitter` and `_PolyhedralBoundedSolidSetOperator`.
    */
    protected static Vector3Dd bisector(_PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3Dd middle;
        Vector3Dd a;
        Vector3Dd b;

        a = (he.next()).startingVertex.position.subtract(he.startingVertex.position);
        b = (he.previous()).startingVertex.position.subtract(he.startingVertex.position);
        a = a.normalized();
        b = b.normalized();

        middle = he.startingVertex.position.add((a.add(b)).multiply(0.5));

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
    private static void laringmv(_PolyhedralBoundedSolidFace f1,
                                 _PolyhedralBoundedSolidFace f2)
    {
        _PolyhedralBoundedSolidLoop l;
        int i;

        // It is supposed to move all (internal) rings from `f1` to `f2`
        // using PolyhedralBoundedSolid.lringmv
        // Legacy rule: move every ring. A geometric two-face containment
        // rule was tried here (mythosPlan Phase 3) and regressed reference
        // flows: mid-connect loops are not simple regions (bridge edges,
        // spikes, half-built chains), so parity containment is unreliable
        // at this point of the pipeline. The shadow trace below records
        // what the geometric rule would have decided, for diagnosis only.
        for ( i = 1; i < f1.boundariesList.size(); i++ ) {
        l = f1.boundariesList.get(i);
            if ( Boolean.getBoolean("vsdk.setop.tracePipelineSummary") ) {
                traceRingMoveShadowDecision(f1, f2, l,
                    "laringmv MOVE wouldMove="
                    + ringBelongsToOtherHalf(f1, f2, l));
            }
            if ( PolyhedralBoundedSolidEulerOperators.lringmv(f1.parentSolid, l, f2, false) ) {
                i--;
            }
        }
    }

    /**
    Decides on which side of a face division a pending ring belongs
    ([MANT1988].13.5, completed): after a {@code lmef} divides a face into
    `f1` (keeping the old outer loop side) and `f2`, a ring belongs to the
    half whose region geometrically contains its representative point.
    Containment is evaluated by 2D parity against each half's outer loop on
    the dominant projection plane. The three observed regimes (mythosPlan
    Phase 3 shadow study):
    <ul>
    <li>Chord division (regions disjoint): exactly one parity test is true
        — the ring follows it. This keeps cusp-region strut rings with
        their junction partners instead of stranding them.</li>
    <li>Interior closed curve (one region nested in the other): both tests
        are true for rings in the nested region; the nested half wins
        (decided by testing one vertex of f2's outer loop against f1's).</li>
    <li>Ring on the dividing boundary (zero clearance: the common
        single-chord pending strut): both tests are unreliable/false —
        the legacy behavior (move to `f2`) is preserved.</li>
    </ul>
    @param f1 face that kept the original outer loop side
    @param f2 face created by the division
    @param l pending ring currently parented by {@code f1}
    @return true when the ring must move to {@code f2}
    */
    private static boolean ringBelongsToOtherHalf(
        _PolyhedralBoundedSolidFace f1,
        _PolyhedralBoundedSolidFace f2,
        _PolyhedralBoundedSolidLoop l)
    {
        if ( f1 == null || f2 == null || l == null ||
             l.boundaryStartHalfEdge == null ||
             l.boundaryStartHalfEdge.startingVertex == null ) {
            return true;
        }
        Vector3Dd point = l.boundaryStartHalfEdge.startingVertex.position;
        Boolean insideF1 = outerLoopParityContains(f1, point);
        Boolean insideF2 = outerLoopParityContains(f2, point);
        if ( insideF1 == null || insideF2 == null ) {
            return true;
        }
        if ( insideF1.booleanValue() != insideF2.booleanValue() ) {
            return insideF2.booleanValue();
        }
        if ( insideF1.booleanValue() ) {
            // Nested halves: the ring belongs to the inner region. Decide
            // which outer loop is nested by testing a representative vertex
            // of f2's outer loop against f1's outer loop.
            _PolyhedralBoundedSolidLoop f2Outer = f2.boundariesList.size() > 0
                ? f2.boundariesList.get(0) : null;
            if ( f2Outer == null || f2Outer.boundaryStartHalfEdge == null ||
                 f2Outer.boundaryStartHalfEdge.startingVertex == null ) {
                return true;
            }
            Boolean f2NestedInF1 = outerLoopParityContains(f1,
                f2Outer.boundaryStartHalfEdge.startingVertex.position);
            if ( f2NestedInF1 == null ) {
                return true;
            }
            return f2NestedInF1.booleanValue();
        }
        // Outside both (on the dividing boundary or degenerate): legacy.
        return true;
    }

    /**
    2D parity (ray crossing) containment of a point against the outer loop
    of a face, on the dominant projection plane of the face normal.
    @param f face whose outer loop is tested
    @param point point to classify
    @return TRUE/FALSE parity result, or null when not computable
    */
    private static Boolean outerLoopParityContains(
        _PolyhedralBoundedSolidFace f,
        Vector3Dd point)
    {
        if ( f == null || point == null || f.boundariesList.size() == 0 ||
             f.getContainingPlane() == null ) {
            return null;
        }
        _PolyhedralBoundedSolidLoop outerLoop = f.boundariesList.get(0);
        if ( outerLoop == null || outerLoop.boundaryStartHalfEdge == null ) {
            return null;
        }

        Vector3Dd normal = f.getContainingPlane().getNormal();
        double ax = Math.abs(normal.x());
        double ay = Math.abs(normal.y());
        double az = Math.abs(normal.z());
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
        boolean inside = false;
        int guard = 0;
        _PolyhedralBoundedSolidHalfEdge start = outerLoop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;
        do {
            _PolyhedralBoundedSolidHalfEdge next = he.next();
            if ( next == null || he.startingVertex == null ||
                 next.startingVertex == null ) {
                return null;
            }
            double au = shadowProjectedU(he.startingVertex.position, dropAxis);
            double av = shadowProjectedV(he.startingVertex.position, dropAxis);
            double bu = shadowProjectedU(next.startingVertex.position, dropAxis);
            double bv = shadowProjectedV(next.startingVertex.position, dropAxis);
            if ( (av > pv) != (bv > pv) ) {
                double crossingU = (bu - au) * (pv - av) / (bv - av) + au;
                if ( pu < crossingU ) {
                    inside = !inside;
                }
            }
            he = next;
            guard++;
        } while ( he != start && guard < 100000 );
        return Boolean.valueOf(inside);
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
    private static void traceRingMoveShadowDecision(
        _PolyhedralBoundedSolidFace f1,
        _PolyhedralBoundedSolidFace f2,
        _PolyhedralBoundedSolidLoop l,
        String site)
    {
        if ( !Boolean.getBoolean("vsdk.setop.tracePipelineSummary") ) {
            return;
        }
        if ( f1 == null || f2 == null || l == null ||
             f1.boundariesList.size() == 0 ||
             l.boundaryStartHalfEdge == null ||
             l.boundaryStartHalfEdge.startingVertex == null ||
             f1.getContainingPlane() == null ) {
            System.out.println("[LARINGMV] f1=" + (f1 == null ? "?" : f1.id)
                + " f2=" + (f2 == null ? "?" : f2.id)
                + " ring=untestable site=" + site);
            return;
        }
        _PolyhedralBoundedSolidLoop outerLoop = f1.boundariesList.get(0);
        if ( outerLoop == l || outerLoop.boundaryStartHalfEdge == null ) {
            System.out.println("[LARINGMV] f1=" + f1.id + " f2=" + f2.id
                + " ring=outer? site=" + site);
            return;
        }

        Vector3Dd normal = f1.getContainingPlane().getNormal();
        double ax = Math.abs(normal.x());
        double ay = Math.abs(normal.y());
        double az = Math.abs(normal.z());
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

        Vector3Dd point = l.boundaryStartHalfEdge.startingVertex.position;
        double pu = shadowProjectedU(point, dropAxis);
        double pv = shadowProjectedV(point, dropAxis);

        boolean inside = false;
        double minClearance = Double.MAX_VALUE;
        int outerSize = 0;
        _PolyhedralBoundedSolidHalfEdge start = outerLoop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;
        boolean walkable = true;
        do {
            _PolyhedralBoundedSolidHalfEdge next = he.next();
            if ( next == null || he.startingVertex == null ||
                 next.startingVertex == null ) {
                walkable = false;
                break;
            }
            double au = shadowProjectedU(he.startingVertex.position, dropAxis);
            double av = shadowProjectedV(he.startingVertex.position, dropAxis);
            double bu = shadowProjectedU(next.startingVertex.position, dropAxis);
            double bv = shadowProjectedV(next.startingVertex.position, dropAxis);

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
            double clearance = Math.sqrt(du * du + dv * dv);
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

        int ringSize = l.halfEdgesList == null ? -1 : l.halfEdgesList.size();
        System.out.println("[LARINGMV] f1=" + f1.id + " f2=" + f2.id
            + " ringV=" + (l.boundaryStartHalfEdge.startingVertex.id)
            + " ringSize=" + ringSize
            + " outerSize=" + outerSize
            + " walkable=" + walkable
            + " inside=" + inside
            + String.format(" clearance=%.6f", minClearance)
            + " p=" + point
            + " site=" + site);
    }

    private static double shadowProjectedU(Vector3Dd p, int dropAxis)
    {
        if ( dropAxis == 0 ) {
            return p.y();
        }
        return p.x();
    }

    private static double shadowProjectedV(Vector3Dd p, int dropAxis)
    {
        if ( dropAxis == 1 || dropAxis == 0 ) {
            return p.z();
        }
        return p.y();
    }

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.10.
    */
    protected static void
    join(_PolyhedralBoundedSolidHalfEdge h1, _PolyhedralBoundedSolidHalfEdge h2, boolean withDebug)
    {
        join(h1, h2, withDebug, true);
    }

    protected static void join(_PolyhedralBoundedSolidHalfEdge h1,
                               _PolyhedralBoundedSolidHalfEdge h2,
                               boolean withDebug,
                               boolean allowRingMove)
    {
        PolyhedralBoundedSolidStatistics.recordJoinCall();
        _PolyhedralBoundedSolidFace oldf;
        _PolyhedralBoundedSolidFace newf;
        PolyhedralBoundedSolid s;

        if ( withDebug ) {
            System.out.println("       -> JOIN:");
            System.out.println("          . H1: " + h1);
            System.out.println("          . H2: " + h2);
        }

        oldf = h1.parentLoop.parentFace;
        newf = null;
        s = oldf.parentSolid;
        if ( h1.parentLoop == h2.parentLoop ) {
            if ( h1.previous().previous() != h2 ) {
                int fid1 = (idNamespace != null)
                    ? idNamespace.nextFaceId(s)
                    : s.getMaxFaceId() + 1;
                newf = PolyhedralBoundedSolidEulerOperators.lmef(s, h1, h2.next(), fid1);
                if ( withDebug ) {
                    //h1.next().parentEdge.debugColor = new ColorRgb(1, 0, 0);
                }
            }
        }
        else {
            PolyhedralBoundedSolidEulerOperators.lmekr(s, h1, h2.next());
            if ( withDebug ) {
                //h1.next().parentEdge.debugColor = new ColorRgb(0, 1, 0);
            }
        }

        if ( h1.next().next() != h2 ) {
            int fid2 = (idNamespace != null)
                ? idNamespace.nextFaceId(s)
                : s.getMaxFaceId() + 1;
            // Shadow diagnostics (mythosPlan Phase 3): capture which face
            // the second division splits and the face it creates, so the
            // rings that are never redistributed across this division stay
            // observable. Applying laringmv here was tried and regressed
            // MOON_BLOCK reference cases — behavior stays legacy.
            _PolyhedralBoundedSolidFace splitFace2 =
                h2.parentLoop.parentFace;
            _PolyhedralBoundedSolidFace newFace2 =
                PolyhedralBoundedSolidEulerOperators.lmef(s, h2, h1.next(), fid2);
            if ( withDebug ) {
                //h2.next().parentEdge.debugColor = new ColorRgb(0, 0, 1);
            }
            if ( Boolean.getBoolean("vsdk.setop.tracePipelineSummary") &&
                 splitFace2 != null &&
                 splitFace2.boundariesList.size() >= 2 ) {
                int shadowI;
                for ( shadowI = 1;
                      shadowI < splitFace2.boundariesList.size();
                      shadowI++ ) {
                    traceRingMoveShadowDecision(splitFace2, newFace2,
                        splitFace2.boundariesList.get(shadowI),
                        "lmef2 KEEP wouldMove=" + ringBelongsToOtherHalf(
                            splitFace2, newFace2,
                            splitFace2.boundariesList.get(shadowI)));
                }
            }
            if ( newf != null && oldf.boundariesList.size() >= 2 ) {
                if ( allowRingMove ) {
                    laringmv(oldf, newf);
                }
            }
        }
    }

    /**
    This method checks whether the edges `he.previous().parentEdge` and
    `he.parentEdge` make a convex (less than 180 degrees) or concave
    (larger than 180 degrees) angle. In the first case the method returns
    `false` and `true` for the second case.
    This is an answer to problem [MANT1988].13.6.
    Current implementation intentionally follows the legacy boolean-kernel
    predicate: the sector is wide when the cross product of its two boundary
    vectors is degenerate or points opposite to the parent face normal.

    PRE: Parent solid should be previously validated to contain correct
    face equations.

    This protected method is here for exclusive use of subclasses
    `_PolyhedralBoundedSolidSplitter` and `_PolyhedralBoundedSolidSetOperator`.
    */
    protected static boolean checkWideness (_PolyhedralBoundedSolidHalfEdge he)
    {
        if ( he == null || he.parentLoop == null ||
             he.parentLoop.parentFace == null ||
             he.parentLoop.parentFace.getContainingPlane() == null ||
             he.previous() == null || he.next() == null ) {
            return true;
        }

        Vector3Dd ref1;
        Vector3Dd ref2;
        Vector3Dd ref12;

        ref1 = he.previous().startingVertex.position.subtract(
            he.startingVertex.position);
        ref2 = he.next().startingVertex.position.subtract(
            he.startingVertex.position);
        ref12 = ref1.crossProduct(ref2);
        if ( ref12.length() < VSDK.EPSILON ) {
            return true;
        }
        return ref12.dotProduct(
            he.parentLoop.parentFace.getContainingPlane().getNormal()) <= 0.0;
    }
}
