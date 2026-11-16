//= References:                                                             =
//= [MANT1986] Mantyla Martti. "Boolean Operations of 2-Manifolds through   =
//=     Vertex Neighborhood Classification". ACM Transactions on Graphics,  =
//=     Vol. 5, No. 1, January 1986, pp. 1-29.                              =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

// Java classes
import java.util.ArrayList;
import java.util.HashSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidStructuralCopy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;

import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._SetOperationTrace.isCoplanarTangentialTraceEnabled;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._SetOperationTrace.isPipelineSummaryTraceEnabled;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._SetOperationTrace.traceCoplanarTangential;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._SetOperationTrace.tracePipelineSummary;

/**
This class encapsulates the set operations algorithms for boundary
representation solids in VitralSDK. Basically, this class implements the
original algorithm published in the paper [MANT1986] and in the second
part of the book [MANT1988].
The algorithm is structured in 5 big phases:
  0. Calculate vertex/face and vertex/vertex crossings.
  1. Classify and split for vertex/face cases.
  2. Classify and split for vertex/vertex cases.
  3. Connect.
  4. Finish.
Note that each big phase is controlled in a method (mark as "big phase" in
its documentation).
*/
public class _PolyhedralBoundedSolidSetOperator extends _PolyhedralBoundedSolidOperator
{
    /**
    Debug flags.
    */
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_02_GENERATOR = 0x02;
    private static final int DEBUG_03_VERTEXFACECLASIFFIER = 0x04;
    private static final int DEBUG_04_VERTEXVERTEXCLASIFFIER = 0x08;
    private static final int DEBUG_05_CONNECT = 0x10;
    private static final int DEBUG_06_FINISH = 0x20;
    private static final int DEBUG_99_SHOWOPERATIONS = 0x40;
    private static int debugFlags = 0;

    @FunctionalInterface
    public interface DebugSolidExporter {
        void export(PolyhedralBoundedSolid solid, String pattern) throws Exception;
    }

    /**
    Optional callback used to export internal state in graphical form.
    */
    private static DebugSolidExporter debugSolidExporter = null;

    public static void setDebugSolidExporter(DebugSolidExporter exporter)
    {
        debugSolidExporter = exporter;
    }


    // Retained as a god-class delegation surface for the reflection-based
    // PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest; the production
    // call sites use _PolyhedralBoundedSolidSetGeometricPredicateProcessor
    // directly. The sibling compareToZero/pointInFace/resolveCoplanarVertexVertexClass
    // wrappers were removed in Stage 7 R3 (no remaining callers).
    private static int classifyCoplanarSectorRelation(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace sectorInfo,
        _PolyhedralBoundedSolidFace referenceFace)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .classifyCoplanarSectorRelation(sectorInfo, referenceFace);
    }

    // The Mantyla son* globals ([MANT1988].15.1: sonvv, sonva, sonvb, sonea,
    // soneb, sonfa, sonfb) are no longer static fields; they live in a
    // per-invocation _SetOperationContext threaded through the pipeline
    // (Stage 7 R5).

    /**
    Procedure `updmaxnames` functionality is described on section
    [MANT1988].15.4. This method increments the face and vertex
    identifiers of `solidToUpdate` so that they do not overlap with
    `referenceSolid` identifiers.
    */
    public static void updmaxnames(PolyhedralBoundedSolid solidToUpdate,
                                   PolyhedralBoundedSolid referenceSolid)
    {
        _PolyhedralBoundedSolidIdNamespace.updmaxnames(
            solidToUpdate, referenceSolid);
    }

    private static int nextVertexId(PolyhedralBoundedSolid current,
                             PolyhedralBoundedSolid other)
    {
        return _PolyhedralBoundedSolidIdNamespace.nextVertexId(
            current, other, idNamespace);
    }

    /**
    Initial vertex intersection detector for the set operations algorithm
    (big phase 0).
    Following program [MANT1988].15.2.
    After generation, coincident intersection vertices are welded in both
    solids and stale entries are pruned from sonva/sonvb.
    */
    private static void setOpGenerate(_SetOperationContext ctx,
                                      PolyhedralBoundedSolid inSolidA,
                                      PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidSetIntersector.GenerationResult generation;

        generation = _PolyhedralBoundedSolidSetIntersector.setOpGenerate(
            inSolidA, inSolidB);
        ctx.sonvv = generation.sonvv();
        ctx.sonva = generation.sonva();
        ctx.sonvb = generation.sonvb();

        weldIntersectionVertices(ctx, inSolidA, inSolidB);
    }

    /**
    Post-Generate weld pass: collapses spatially coincident vertices introduced
    during setOpGenerate in each solid, then removes from sonva/sonvb any entry
    whose vertex was merged away by lkev.  This prevents duplicate-position
    vertices from propagating into the Classify and Connect phases.
    */
    private static void weldIntersectionVertices(
        _SetOperationContext ctx,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        int weldedA;
        int weldedB;

        weldedA = PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(
            inSolidA, numericContext);
        weldedB = PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(
            inSolidB, numericContext);

        if ( weldedA > 0 ) {
            Logger.reportMessage(null, VSDK.DEBUG, "weldIntersectionVertices",
                "setOpGenerate weld: " + weldedA + " vertex pair(s) collapsed in solidA");
            pruneStaleVertexFaceEntries(ctx.sonva, inSolidA);
        }
        if ( weldedB > 0 ) {
            Logger.reportMessage(null, VSDK.DEBUG, "weldIntersectionVertices",
                "setOpGenerate weld: " + weldedB + " vertex pair(s) collapsed in solidB");
            pruneStaleVertexFaceEntries(ctx.sonvb, inSolidB);
        }
    }

    /**
    Removes entries from {@code list} whose vertex is no longer present in
    {@code solid}.  After lkev merges two vertices the removed vertex object
    is detached from the solid's verticesList; any sonva/sonvb entry still
    pointing to it would reference a dangling node.
    */
    private static void pruneStaleVertexFaceEntries(
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> list,
        PolyhedralBoundedSolid solid)
    {
        int i;

        i = 0;
        while ( i < list.size() ) {
            _PolyhedralBoundedSolidSetOperatorVertexFace entry;
            entry = list.get(i);
            if ( !solid.getVerticesList().locateWindowAtElem(entry.v) ) {
                list.remove(i);
            }
            else {
                i++;
            }
        }
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    that points inward the he's containing face.
    */
    protected static Vector3Dd inside(_PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3Dd middle = null;
        Vector3Dd a;
        Vector3Dd b;
        Vector3Dd n;

        a = (he.next()).startingVertex.position.subtract(he.startingVertex.position);
        b = (he.previous()).startingVertex.position.subtract(he.startingVertex.position);
        a = a.normalized();
        b = b.normalized();

        n = he.parentLoop.parentFace.getContainingPlane().getNormal();

        middle = n.crossProduct(a);
        middle = middle.normalized();

        return middle;
    }

    /**
    Checks if two coplanar sectors overlaps, by doing a "sector within" test
    for coplanar sectors: If the two given sectors are coplanar and with
    overlaping faces:
      - If sectors only intersects in one point returns false.
      - If sectors intersects on a line or area returns true.

    Following section [MANT1988].15.6.2. Note that this operation is not
    elaborated on [MANT1988], but left as an excercise.

    PRE: Given sectors are "coplanar".
    */
    private static boolean sectoroverlap(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex nb)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .sectoroverlap(na, nb,
                (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0);
    }

    public static boolean colinearVectorsWithDirection(Vector3Dd a, Vector3Dd b)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .colinearVectorsWithDirection(a, b);
    }

    /**
    Normalizes one endpoint for `separateEdgeSequence` when a previous null
    strut edge was already inserted on the same vertex neighborhood.
    */
    private static _PolyhedralBoundedSolidHalfEdge
    recoverEdgeSequenceEndpointFromStrut(
        _PolyhedralBoundedSolidHalfEdge endpoint,
        boolean isFromEndpoint)
    {
        _PolyhedralBoundedSolidHalfEdge prev;

        if ( endpoint == null ) {
            return null;
        }

        prev = endpoint.previous();
        if ( prev == null || prev.parentEdge == null ) {
            return endpoint;
        }

        if ( !nulledge(prev) || !strutnulledge(prev) ) {
            return endpoint;
        }

        if ( isFromEndpoint ) {
            if ( prev == prev.parentEdge.leftHalf ) {
                return prev.previous();
            }
        }
        else {
            if ( prev == prev.parentEdge.rightHalf ) {
                return prev.previous();
            }
        }
        return endpoint;
    }

    /**
    Outcome of the endpoint-pairing recovery loop in {@link #separateEdgeSequence}.
    Distinguishes successful pairing from each failure mode so the caller can
    report or react specifically instead of relying on a generic fatal log.
    */
    enum SeparateEdgeSequenceResult
    {
        OK,
        FAILED_NULL_INPUT,
        FAILED_DIFFERENT_SOLIDS,
        FAILED_CYCLE_DETECTED,
        FAILED_NO_PAIRING_REACHED
    }

    /**
    Following program [MANT1988].15.12. Adapts the wMANT2008 recovery
    extensions for null-edge endpoints (cases A-E) using strict cycle
    detection — every iteration must produce an unseen (from, to)
    configuration, otherwise we abort and report
    {@link SeparateEdgeSequenceResult#FAILED_CYCLE_DETECTED}. This is the
    convergence proof requested in plan-csg-boolean-fix-stage2 §5.3:
    progress is measured as "new configurations visited", which is bounded
    by the (finite) product of half-edges in both loops, so the loop
    necessarily terminates.

    @return diagnostic result; {@link SeparateEdgeSequenceResult#OK} only
        when {@code from} and {@code to} share starting vertex and the LMEV
        split has been applied. Any other value indicates the LMEV was
        skipped to avoid corrupting the B-rep.
    */
    static SeparateEdgeSequenceResult separateEdgeSequence(
        _PolyhedralBoundedSolidHalfEdge from,
        _PolyhedralBoundedSolidHalfEdge to,
        int type,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        //-----------------------------------------------------------------
        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
            System.out.println("      SEPARATEEDGESEQUENCE " + type);
            System.out.println("        From: " + from);
            System.out.println("        To: " + to);
        }

        if ( from == null || to == null ) {
            Logger.reportMessage(null, VSDK.WARNING, "separateEdgeSequence",
                "Unexpected case: null halfedges; skipping LMEV.");
            return SeparateEdgeSequenceResult.FAILED_NULL_INPUT;
        }

        PolyhedralBoundedSolid s;
        s = from.parentLoop.parentFace.parentSolid;

        if ( s != to.parentLoop.parentFace.parentSolid ) {
            Logger.reportMessage(null, VSDK.WARNING, "separateEdgeSequence",
                "Unexpected case: halfedges on different solids; skipping LMEV.");
            return SeparateEdgeSequenceResult.FAILED_DIFFERENT_SOLIDS;
        }

        //-----------------------------------------------------------------
        // Recover from null edges already inserted.
        // Cases A/B follow null-edge struts inserted previously; cases C/D/E
        // step backwards in the loop until the two starts coincide. Each
        // iteration must produce an unseen (from, to) pair — repeating a pair
        // proves divergence and is reported as a bug instead of looping
        // forever or aborting silently after a magic count.
        HashSet<Long> visitedConfigurations = new HashSet<Long>();
        boolean changed;
        do {
            long configurationKey =
                ((long) System.identityHashCode(from) << 32) |
                ((long) System.identityHashCode(to) & 0xFFFFFFFFL);
            if ( !visitedConfigurations.add(configurationKey) ) {
                Logger.reportMessage(null, VSDK.WARNING,
                    "separateEdgeSequence",
                    "Cycle detected in endpoint recovery (cases A-E "
                    + "did not converge); skipping LMEV to keep B-rep valid.");
                return SeparateEdgeSequenceResult.FAILED_CYCLE_DETECTED;
            }

            changed = false;

            _PolyhedralBoundedSolidHalfEdge recoveredFrom;
            _PolyhedralBoundedSolidHalfEdge recoveredTo;

            recoveredFrom = recoverEdgeSequenceEndpointFromStrut(from, true);
            if ( recoveredFrom != from ) {
                from = recoveredFrom;
                changed = true;
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                    System.out.println("        Recovered edge sequence case A");
                }
            }

            recoveredTo = recoverEdgeSequenceEndpointFromStrut(to, false);
            if ( recoveredTo != to ) {
                to = recoveredTo;
                changed = true;
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                    System.out.println("        Recovered edge sequence case B");
                }
            }

            if ( from.startingVertex != to.startingVertex ) {
                _PolyhedralBoundedSolidHalfEdge fromPrev = from.previous();
                _PolyhedralBoundedSolidHalfEdge toPrev = to.previous();

                if ( fromPrev != null && toPrev != null &&
                     fromPrev.parentEdge != null && toPrev.parentEdge != null &&
                     fromPrev == toPrev.mirrorHalfEdge() ) {
                    from = fromPrev;
                    changed = true;
                    if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                        System.out.println("        Recovered edge sequence case C");
                    }
                }
                else if ( fromPrev != null &&
                          fromPrev.startingVertex == to.startingVertex ) {
                    from = fromPrev;
                    changed = true;
                    if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                        System.out.println("        Recovered edge sequence case D");
                    }
                }
                else if ( toPrev != null &&
                          toPrev.startingVertex == from.startingVertex ) {
                    to = toPrev;
                    changed = true;
                    if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                        System.out.println("        Recovered edge sequence case E");
                    }
                }
            }
        } while ( changed );

        if ( from.startingVertex != to.startingVertex ) {
            Logger.reportMessage(null, VSDK.WARNING, "separateEdgeSequence",
                "Unable to recover endpoint pairing after A-E normalization; "
                + "skipping LMEV.");
            return SeparateEdgeSequenceResult.FAILED_NO_PAIRING_REACHED;
        }

        //-----------------------------------------------------------------
        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOWOPERATIONS ) != 0x00 ) {
            System.out.println("       -> LMEV (Separate edge sequence):");
            System.out.println("          . H1: " + to);
            System.out.println("          . H2: " + from);
            //from.startingVertex.debugColor = new ColorRgb(1, 0, 1);
        }

        int id = nextVertexId(inSolidA, inSolidB);

        PolyhedralBoundedSolidEulerOperators.lmev(s, to, from, id, to.startingVertex.position);

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
            System.out.println("          . New vertex: " + id);
        }

        // The live pipeline records the created null edge into the per-call
        // sonea/soneb lists from within _PolyhedralBoundedSolidSetClassifier
        // (see its own separateEdgeSequence). This god-class overload is
        // retained only to validate the cycle-detection failure modes /
        // result enum via VertexVertexEndpointRecoveryTest and is not part of
        // the live path, so it no longer touches the (now per-call) son*
        // state. Stage 7 R5.

        return SeparateEdgeSequenceResult.OK;
    }

    private static boolean nulledge(_PolyhedralBoundedSolidHalfEdge he)
    {
        return PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
            he.startingVertex.position, he.next().startingVertex.position,
            numericContext);
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static boolean strutnulledge(_PolyhedralBoundedSolidHalfEdge he)
    {
        if( he == he.mirrorHalfEdge().next() ||
            he == he.mirrorHalfEdge().previous() ) {
            return true;
        }
        return false;
    }

    /**
    Main control algorithm for the big phases 1 and 2. This calls the
    classifiers for vertex/face and vertex/vertex coincidences found on
    `setOpGenerate`.
    Following section [MANT1988].16.6.1. and program [MANT1988].15.5.
    */
    private static void setOpClassify(_SetOperationContext ctx,
                                      int op,
                                      PolyhedralBoundedSolid inSolidA,
                                      PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidSetClassifier.runSetOpClassify(
            op, inSolidA, inSolidB, debugFlags, ctx);
    }

    // TEMP moon-diagnostic: scans a solid for boundary loops whose vertices are
    // coincident-but-distinct (self-touching / figure-8) — the signature of a
    // broken double/cut face that Generate produces on a concave cap. Gated
    // behind the pipeline-summary trace property; no effect in normal runs.
    private static void traceSelfTouchingLoops(
        PolyhedralBoundedSolid solid, String label)
    {
        if ( !_SetOperationTrace.isPipelineSummaryTraceEnabled() ) {
            return;
        }
        if ( solid == null || solid.getPolygonsList() == null ) {
            return;
        }
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext tol =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        int fi;
        for ( fi = 0; fi < solid.getPolygonsList().size(); fi++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(fi);
            int li;
            for ( li = 0; li < face.boundariesList.size(); li++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(li);
                int sz = loop.halfEdgesList.size();
                int a;
                for ( a = 0; a < sz; a++ ) {
                    int b;
                    for ( b = a + 1; b < sz; b++ ) {
                        _PolyhedralBoundedSolidHalfEdge ha =
                            loop.halfEdgesList.get(a);
                        _PolyhedralBoundedSolidHalfEdge hb =
                            loop.halfEdgesList.get(b);
                        if ( ha.startingVertex.id != hb.startingVertex.id &&
                             PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                                 ha.startingVertex.position,
                                 hb.startingVertex.position, tol) ) {
                            int gap = b - a;
                            boolean adjacent = (gap == 1) || (gap == sz - 1);
                            System.out.println("[SelfTouch] " + label
                                + " face=" + face.id + " loop=" + li
                                + " size=" + sz
                                + " idx[" + a + "]v" + ha.startingVertex.id
                                + "==idx[" + b + "]v" + hb.startingVertex.id
                                + (adjacent ? " ADJACENT(zero-len-edge)"
                                            : " NON-ADJACENT(pinch)")
                                + " at (" + String.format("%.4f,%.4f,%.4f",
                                    ha.startingVertex.position.x(),
                                    ha.startingVertex.position.y(),
                                    ha.startingVertex.position.z()) + ")");
                        }
                    }
                }
            }
        }
    }

    /**
    Splits every boundary loop that is self-touching (pinched) — i.e. has two
    non-adjacent half-edges whose start vertices are geometrically coincident —
    into simple loops via {@code lmef}.

    <p>This is run on each operand right after {@code setOpGenerate} and before
    {@code setOpClassify}. At that point, the only coincident-vertex pairs that
    exist in real boundary loops (size&gt;2) are genuine pinches introduced by
    the intersector on concave faces (e.g. a crescent-shaped cap). Size-2 strut
    loops (the normal null-edge form) are deliberately excluded.</p>

    <p>For a pinch at loop positions {@code a} and {@code b}, {@code lmef(he_a,
    he_b)} splits the loop into two simple loops. If the original loop had
    multiple pinches, the outer restartable scan re-examines the face until all
    pinches are resolved. This handles both nested and interleaved pinch pairs.</p>

    <p>Tolerance: uses the pipeline's {@code numericContext} (scaled
    {@code bigEpsilon}), consistent with other coincidence tests.</p>
    */
    private static void splitSelfTouchingLoops(PolyhedralBoundedSolid solid)
    {
        if ( solid == null || solid.getPolygonsList() == null ) {
            return;
        }
        boolean tracing = isPipelineSummaryTraceEnabled();
        int splitsFired = 0;
        int fi = 0;
        while ( fi < solid.getPolygonsList().size() ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(fi);
            boolean splitDone = false;
            int li = 0;
            while ( li < face.boundariesList.size() && !splitDone ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(li);
                int sz = loop.halfEdgesList.size();
                if ( sz <= 2 ) {
                    li++;
                    continue;
                }
                int a;
                for ( a = 0; a < sz && !splitDone; a++ ) {
                    int b;
                    for ( b = a + 2; b < sz; b++ ) {
                        if ( a == 0 && b == sz - 1 ) {
                            continue; // adjacent via wrap-around
                        }
                        _PolyhedralBoundedSolidHalfEdge ha =
                            loop.halfEdgesList.get(a);
                        _PolyhedralBoundedSolidHalfEdge hb =
                            loop.halfEdgesList.get(b);
                        if ( ha.startingVertex.id != hb.startingVertex.id &&
                             PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                                 ha.startingVertex.position,
                                 hb.startingVertex.position,
                                 numericContext) ) {
                            int newId = (idNamespace != null)
                                ? idNamespace.nextFaceId(solid)
                                : solid.getMaxFaceId() + 1;
                            _PolyhedralBoundedSolidFace newFace =
                                PolyhedralBoundedSolidEulerOperators.lmef(
                                    solid, ha, hb, newId);
                            if ( newFace != null ) {
                                splitsFired++;
                                if ( tracing ) {
                                    tracePipelineSummary(
                                        "splitSelfTouchingLoops #" + splitsFired
                                        + " face=" + face.id + " loop=" + li
                                        + " sz=" + sz
                                        + " idx[" + a + "]v" + ha.startingVertex.id
                                        + "==idx[" + b + "]v" + hb.startingVertex.id
                                        + " -> newFace=" + newFace.id);
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
            tracePipelineSummary(
                "splitSelfTouchingLoops total=" + splitsFired
                + " faces-after=" + solid.getPolygonsList().size());
        }
    }

    private static void setOpConnect(_SetOperationContext ctx, int op)
    {
        _PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult result;

        result = new _PolyhedralBoundedSolidSetNullEdgesConnector().connect(
            op, debugFlags, ctx.sonea, ctx.soneb);
        ctx.sonfa = result.sonfa();
        ctx.sonfb = result.sonfb();
    }

    /**
    Answer integrator for the set operations algorithm (big phase 4).
    Following program [MANT1988].15.15.
    */
    private static void setOpFinish(
        _SetOperationContext ctx,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op
    )
    {
        _PolyhedralBoundedSolidSetFinisher.finish(
            inSolidA, inSolidB, outRes, op, debugFlags, ctx.sonfa, ctx.sonfb);
    }

    private static boolean isTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .runTouchingOnlyPreflightCase(inSolidA, inSolidB);
    }

    private static boolean isTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        _PolyhedralBoundedSolidSetNonIntersectingClassifier._PreflightCache cache)
    {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .runTouchingOnlyPreflightCase(inSolidA, inSolidB, cache);
    }

    private static boolean isContainmentOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .runContainmentOnlyPreflightCase(inSolidA, inSolidB);
    }

    private static boolean isContainmentOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        _PolyhedralBoundedSolidSetNonIntersectingClassifier._PreflightCache cache)
    {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .runContainmentOnlyPreflightCase(inSolidA, inSolidB, cache);
    }

    /**
    Handles no-intersection cases (book problem [MANT1988].15.1). This overload
    builds a fresh classification: use it after the generate/classify stages,
    where the operands have already been mutated and the preflight memo is stale.
    */
    private static PolyhedralBoundedSolid setOpNoIntersectionCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op)
    {
        try {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .runSetOpNoIntersectionCase(inSolidA, inSolidB, outRes, op);
        }
        catch ( NonManifoldBooleanResultException e ) {
            lastResultPath.set(REJECTED_NON_MANIFOLD_PATH);
            throw e;
        }
    }

    /**
    Handles no-intersection cases (book problem [MANT1988].15.1), reusing the
    per-setOp preflight memo. Only valid while the operands are still unmutated
    (i.e. inside the preflight block).
    */
    private static PolyhedralBoundedSolid setOpNoIntersectionCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op,
        _PolyhedralBoundedSolidSetNonIntersectingClassifier._PreflightCache cache)
    {
        try {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .runSetOpNoIntersectionCase(inSolidA, inSolidB, outRes, op, cache);
        }
        catch ( NonManifoldBooleanResultException e ) {
            lastResultPath.set(REJECTED_NON_MANIFOLD_PATH);
            throw e;
        }
    }

    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        return setOp(inSolidA, inSolidB, op, false, true, true);
    }

    private static void debugSolid(PolyhedralBoundedSolid solid, String pattern)
    {
        System.out.println("**** DEBUGGING SOLID INFORMATION WRITEN TO FILES " +
            pattern + " ****");
        try {
            File fd = new File(pattern + ".txt");
            FileOutputStream fos = new FileOutputStream(fd);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            if ( debugSolidExporter != null ) {
                debugSolidExporter.export(solid, pattern);
            }

            PersistenceElement.writeAsciiLine(bos, solid.toString());
            bos.close();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
    Following program [MANT1988].15.1.
    */
    private static void postProcessResult(
        PolyhedralBoundedSolid res,
        boolean maximizeResultFaces)
    {
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
        PolyhedralBoundedSolidTopologyEditing.compactIds(res);
        if ( maximizeResultFaces ) {
            PolyhedralBoundedSolidTopologyEditing.maximizeFaces(res);
            _PolyhedralBoundedSolidSetFinisher.triangulateNonPlanarFaces(res);
            PolyhedralBoundedSolidTopologyEditing.compactIds(res);
        }
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
    }

    /**
    Name of the path that produced the last setOp result on the calling
    thread: "normal-pipeline" for the [MANT1988] Ch. 15 pipeline, or the name
    of the preflight that answered instead. Recorded before the
    strict postcondition runs, so it is also available after a strict
    validation failure. Fable5.2Analysis.md Stage A step 4.
    */
    /**
    Result path recorded when the operation is rejected with
    {@link NonManifoldBooleanResultException}.
    */
    public static final String REJECTED_NON_MANIFOLD_PATH =
        "rejected-non-manifold-union";

    private static final ThreadLocal<String> lastResultPath =
        new ThreadLocal<String>();

    public static String getLastResultPath()
    {
        return lastResultPath.get();
    }

    /**
    Violations of the post-connect invariant found on the last setOp of the
    calling thread that reached connect (empty list: none; null: connect was
    not reached). See {@link #checkConnectInvariant}.
    */
    private static final ThreadLocal<ArrayList<String>>
        lastConnectInvariantViolations = new ThreadLocal<ArrayList<String>>();

    static ArrayList<String> getLastConnectInvariantViolations()
    {
        return lastConnectInvariantViolations.get();
    }

    /**
    Post-connect invariant ([MANT1988] Sec. 14.7 and 15.7; Fable5.2Analysis.md
    Stage C step 1), checked when connect left no loose ends:
    <ul>
    <li>every null face recorded for finish (sonfa, sonfb) has exactly two
        loops, the two copies of its section curve;</li>
    <li>no other face carries two loops that coincide vertex by vertex. A
        section ring strictly inside a face must leave that face with the
        ring as a hole and the disk as a separate face; two coincident
        loops in one face mean the hole was moved into the disk (B-01).</li>
    </ul>
    */
    private static ArrayList<String> checkConnectInvariant(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        _SetOperationContext ctx)
    {
        ArrayList<String> violations = new ArrayList<String>();

        if ( hasIncompleteConnectState() ) {
            return violations;
        }
        collectNullFaceViolations("A", ctx.sonfa, violations);
        collectNullFaceViolations("B", ctx.sonfb, violations);
        collectCoincidentLoopViolations("A", inSolidA, ctx.sonfa, violations);
        collectCoincidentLoopViolations("B", inSolidB, ctx.sonfb, violations);
        return violations;
    }

    private static void collectNullFaceViolations(String label,
        ArrayList<_PolyhedralBoundedSolidFace> nullFaces,
        ArrayList<String> violations)
    {
        int i;

        if ( nullFaces == null ) {
            return;
        }
        for ( i = 0; i < nullFaces.size(); i++ ) {
            _PolyhedralBoundedSolidFace f = nullFaces.get(i);
            // A null face collapsed to a single point (only zero-length
            // strut rings, left where the section curve runs straight
            // through a vertex of one face) bounds no section and is
            // discarded by finish; it is not a section-curve face.
            if ( f.boundariesList.size() != 2 && !isPointLikeFace(f) ) {
                violations.add(label + ": null face " + f.id + " has " +
                    f.boundariesList.size() + " loops instead of 2");
            }
        }
    }

    private static boolean isPointLikeFace(_PolyhedralBoundedSolidFace f)
    {
        Vector3Dd first = null;
        int i;

        for ( i = 0; i < f.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge start =
                f.boundariesList.get(i).boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            if ( start == null ) {
                continue;
            }
            do {
                Vector3Dd p = he.startingVertex.position;
                if ( first == null ) {
                    first = p;
                }
                else if ( Vector3Dd.distance(first, p) >
                          numericContext.bigEpsilon() ) {
                    return false;
                }
                he = he.next();
            } while ( he != start );
        }
        return true;
    }

    private static void collectCoincidentLoopViolations(String label,
        PolyhedralBoundedSolid solid,
        ArrayList<_PolyhedralBoundedSolidFace> nullFaces,
        ArrayList<String> violations)
    {
        int i;
        int j;
        int k;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace f = solid.getPolygonsList().get(i);
            if ( f.boundariesList.size() < 2 ||
                 (nullFaces != null && nullFaces.contains(f)) ) {
                continue;
            }
            for ( j = 0; j < f.boundariesList.size(); j++ ) {
                for ( k = j + 1; k < f.boundariesList.size(); k++ ) {
                    if ( loopsCoincide(f.boundariesList.get(j),
                             f.boundariesList.get(k)) ) {
                        violations.add(label + ": face " + f.id +
                            " has coincident loops " + j + " and " + k);
                    }
                }
            }
        }
    }

    private static boolean loopsCoincide(_PolyhedralBoundedSolidLoop a,
        _PolyhedralBoundedSolidLoop b)
    {
        int i;
        int j;

        if ( a.halfEdgesList.size() != b.halfEdgesList.size() ||
             a.halfEdgesList.size() < 3 ) {
            return false;
        }
        for ( i = 0; i < a.halfEdgesList.size(); i++ ) {
            Vector3Dd p = a.halfEdgesList.get(i).startingVertex.position;
            boolean found = false;
            for ( j = 0; j < b.halfEdgesList.size() && !found; j++ ) {
                found = PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    p, b.halfEdgesList.get(j).startingVertex.position,
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
    same two points. A manifold boundary has neither. Returns a short
    description of the first contact found, or null.
    */
    static String findNonManifoldContact(PolyhedralBoundedSolid result)
    {
        double tolerance = numericContext.bigEpsilon();
        int nv = result.getVerticesList().size();
        int ne = result.getEdgesList().size();
        int i;
        int j;

        for ( i = 0; i < nv; i++ ) {
            Vector3Dd p = result.getVerticesList().get(i).position;
            for ( j = i + 1; j < nv; j++ ) {
                if ( Vector3Dd.distance(p,
                         result.getVerticesList().get(j).position) <=
                     tolerance ) {
                    return "vertices " +
                        result.getVerticesList().get(i).id + " and " +
                        result.getVerticesList().get(j).id +
                        " coincide at " + p;
                }
            }
        }
        for ( i = 0; i < ne; i++ ) {
            _PolyhedralBoundedSolidEdge e = result.getEdgesList().get(i);
            for ( j = i + 1; j < ne; j++ ) {
                _PolyhedralBoundedSolidEdge f = result.getEdgesList().get(j);
                if ( sameEndpoints(e, f) ) {
                    return "edges " + e.id + " and " + f.id +
                        " join the same points " +
                        e.rightHalf.startingVertex.position + " - " +
                        e.leftHalf.startingVertex.position;
                }
            }
        }
        return null;
    }

    private static boolean sameEndpoints(_PolyhedralBoundedSolidEdge e,
        _PolyhedralBoundedSolidEdge f)
    {
        _PolyhedralBoundedSolidVertex e1 = e.rightHalf.startingVertex;
        _PolyhedralBoundedSolidVertex e2 = e.leftHalf.startingVertex;
        _PolyhedralBoundedSolidVertex f1 = f.rightHalf.startingVertex;
        _PolyhedralBoundedSolidVertex f2 = f.leftHalf.startingVertex;

        return (e1 == f1 && e2 == f2) || (e1 == f2 && e2 == f1);
    }

    private static PolyhedralBoundedSolid completeSetOpResult(
        PolyhedralBoundedSolid result,
        String operandADiagnostics,
        String operandBDiagnostics,
        int op,
        String resultPath,
        boolean maximizeResultFaces,
        boolean doStrictValidation)
    {
        lastResultPath.set(resultPath);
        if ( result == null ) {
            throw new SolidOperationFailedException(
                "Boolean operation returned null: op=" +
                operationName(op) + ", path=" + resultPath);
        }
        if ( result.getPolygonsList().size() > 0 ) {
            postProcessResult(result, maximizeResultFaces);
        }
        String contact = findNonManifoldContact(result);
        if ( contact != null ) {
            // Same policy as B-04: pieces of the result that touch only
            // along an edge or at a vertex form a non-manifold solid.
            throw new NonManifoldBooleanResultException(
                operationName(op) + " result is non-manifold (" + contact +
                "); rejected (path=" + resultPath + ")");
        }
        if ( doStrictValidation ) {
            StringBuilder strictMessage = new StringBuilder();
            if ( !PolyhedralBoundedSolidValidationEngine.validateStrict(
                    result, strictMessage) ) {
                PolyhedralBoundedSolidTopologySummary topology =
                    PolyhedralBoundedSolidTopologySummary.from(result);
                throw new InvalidSolidResultException(
                    "Strict boolean result validation failed: op=" +
                    operationName(op) + ", path=" + resultPath +
                    ", operandA=" + operandADiagnostics +
                    ", operandB=" + operandBDiagnostics +
                    ", result=" + cardinalitiesAndBounds(result) +
                    ", " + topology + "\n" + strictMessage);
            }
        }
        return result;
    }

    private static String operationName(int op)
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
        return "UNKNOWN(" + op + ")";
    }

    private static String cardinalitiesAndBounds(
        PolyhedralBoundedSolid solid)
    {
        if ( solid == null ) {
            return "null";
        }
        return "{faces=" + solid.getPolygonsList().size() +
            ", edges=" + solid.getEdgesList().size() +
            ", vertices=" + solid.getVerticesList().size() +
            ", bounds=" + java.util.Arrays.toString(solid.getMinMax()) +
            "}";
    }

    /**
    Independent copy of `solid` for the recovery paths. It used to be a
    Java serialization round trip, which recursed over the linked structure
    and could overflow the stack on large solids; it is now the linear
    structural copy of [MANT1988] Sec. 15.8 (`scopy`, Stage F). The label
    is kept for the callers' diagnostics.
    */
    private static PolyhedralBoundedSolid deepCloneSolid(
        PolyhedralBoundedSolid solid,
        String solidLabel)
    {
        return PolyhedralBoundedSolidStructuralCopy.copy(solid);
    }

    private static boolean hasIncompleteConnectState()
    {
        return _PolyhedralBoundedSolidSetNullEdgesConnector
            .getLastLooseACount() > 0 ||
            _PolyhedralBoundedSolidSetNullEdgesConnector
                .getLastLooseBCount() > 0;
    }

    /**
    Following program [MANT1988].15.1.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op, boolean withDebug)
    {
        return setOp(inSolidA, inSolidB, op, withDebug, true, true);
    }

    /**
    Following program [MANT1988].15.1.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug,
        boolean maximizeResultFaces)
    {
        return setOp(inSolidA, inSolidB, op, withDebug,
            maximizeResultFaces, true);
    }

    /**
    Runs a boolean operation with configurable strict B-Rep validation as a
    final result postcondition. Shorter overloads enable it by default.
    *
    * @throws IllegalStateException when {@code doStrictValidation} is true
    *     and the completed result fails strict validation
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug,
        boolean maximizeResultFaces,
        boolean doStrictValidation)
    {
        lastResultPath.set(null);
        lastConnectInvariantViolations.set(null);
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(
            numericContext);

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
            System.out.println("= [START OF SETOP REPORT] =================================================================================================================================");
            System.out.println("Dumping debug log for _PolyhedralBoundedSolidSetOperator.setOp.");
            System.out.println("The algorithm structure is:");
            System.out.println("  0. Calculate vertex/face and vertex/vertex crossings.");
            System.out.println("  1. Classify and split for vertex/face cases.");
            System.out.println("  2. Classify and split for vertex/vertex cases.");
            System.out.println("  3. Connect.");
            System.out.println("  4. Finish.");
        }

        //-----------------------------------------------------------------
        PolyhedralBoundedSolid res = new PolyhedralBoundedSolid();
        String resultPath;
        String operandADiagnostics;
        String operandBDiagnostics;

        _SetOperationContext ctx = new _SetOperationContext();
        ctx.sonea = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        ctx.soneb = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        resultPath = "normal-pipeline";
        operandADiagnostics = cardinalitiesAndBounds(inSolidA);
        operandBDiagnostics = cardinalitiesAndBounds(inSolidB);

        //-----------------------------------------------------------------
        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage00");
            debugSolid(inSolidB, "outputB_stage00");
        }

        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidB);
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(inSolidB);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidB);
        StringBuilder booleanInputMsg = new StringBuilder();
        if ( !PolyhedralBoundedSolidValidationEngine.validateBooleanInputs(
                inSolidA, inSolidB, booleanInputMsg) ) {
            Logger.reportMessage(null, VSDK.WARNING, "setOp",
                "Boolean input validation failed:\n" + booleanInputMsg);
        } else if ( booleanInputMsg.length() > 0 ) {
            Logger.reportMessage(null, VSDK.DEBUG, "setOp",
                "Boolean input pre-processing:\n" + booleanInputMsg);
        }
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidB);
        updmaxnames(inSolidB, inSolidA);
        setIdNamespace(new _PolyhedralBoundedSolidIdNamespace(inSolidA, inSolidB));
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(
            numericContext);
        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage01");
            debugSolid(inSolidB, "outputB_stage01");
        }

        // Per-setOp memo shared across all "no real intersection" preflights:
        // operands are not mutated until setOpGenerate runs (below), so the
        // heavy classification/overlap/edge-face predicates are computed once
        // and reused. See _PreflightCache.
        _PolyhedralBoundedSolidSetNonIntersectingClassifier._PreflightCache
            preflightCache =
                _PolyhedralBoundedSolidSetNonIntersectingClassifier
                    .newPreflightCache(inSolidA, inSolidB);

        PolyhedralBoundedSolid coplanarAreaContactResult =
            _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .runPartialCoplanarFaceAreaCase(inSolidA, inSolidB, res, op,
                    preflightCache);
        if ( coplanarAreaContactResult != null ) {
            res = coplanarAreaContactResult;
            return completeSetOpResult(res, operandADiagnostics,
                operandBDiagnostics, op,
                "partial-coplanar-area-preflight", maximizeResultFaces,
                doStrictValidation);
        }

        // A union of solids that touch along a face is one solid, glued
        // along the AonB- faces ([MANT1988] Eq. 15.2): leave it to the
        // pipeline. Edge or vertex contact would leave two shells sharing
        // a boundary point: rejected as non-manifold (B-04).
        boolean unionGluesAlongAFace = op == UNION &&
            _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .hasOppositeCoplanarFaceAreaContact(inSolidA, inSolidB);
        if ( !unionGluesAlongAFace &&
             isTouchingOnlyPreflightCase(inSolidA, inSolidB, preflightCache) ) {
            res = setOpNoIntersectionCase(inSolidA, inSolidB, res, op,
                preflightCache);
            return completeSetOpResult(res, operandADiagnostics,
                operandBDiagnostics, op,
                "touching-only-preflight", maximizeResultFaces,
                doStrictValidation);
        }

        // §7.3.1.A — Degenerate identity preflight (algebraic identity
        // detector): when A ≡ B geometrically (e.g. tests of idempotence
        // A∪A = A on cloned operands), the classifier marks every face of
        // both as "inside the other" and the regular pipeline collapses
        // to ∅, breaking A∪A and A∩A. Detect and dispatch directly per
        // set-theoretic identity. MUST run before the containment
        // preflight (which would also accept A ≡ B but produce slightly
        // different topology via merge() instead of deepClone()).
        if ( PolyhedralBoundedSolidValidationEngine
                .areGeometricallyIdentical(
                    inSolidA, inSolidB, numericContext.bigEpsilon()) ) {
            tracePipelineSummary(
                "setOp identity-preflight A≡B op=" + op);
            if ( op == UNION || op == INTERSECTION ) {
                res = deepCloneSolid(inSolidA, "identity-preflight clone");
                if ( res == null ) {
                    res = new PolyhedralBoundedSolid();
                }
            }
            // SUBTRACT case: A − A = ∅; res remains the empty PolyhedralBoundedSolid
            // already initialised at function entry.
            return completeSetOpResult(res, operandADiagnostics,
                operandBDiagnostics, op,
                "geometric-identity-preflight", maximizeResultFaces,
                doStrictValidation);
        }

        // §7.3.1.D — Containment-only preflight: when one solid is
        // contained inside the other (strictly or with tangent
        // boundary) without real edge/face intersections (e.g., the
        // second step of absorption identities A ∪ (A ∩ B) where
        // A ∩ B ⊂ A), the regular pipeline produces ∅. Dispatch to a
        // dedicated containment table per Mäntylä Ch.15.1.
        if ( isContainmentOnlyPreflightCase(inSolidA, inSolidB,
                preflightCache) ) {
            res = setOpNoIntersectionCase(inSolidA, inSolidB, res, op,
                preflightCache);
            return completeSetOpResult(res, operandADiagnostics,
                operandBDiagnostics, op,
                "containment-only-preflight", maximizeResultFaces,
                doStrictValidation);
        }

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
        // NOTE: after Classify the algorithm has (by design) inserted null-edge
        // struts (size-2 loops with coincident endpoints); scanning here would
        // flag those normal struts. Genuine self-touch is only meaningful
        // after Generate (above), so we deliberately do not scan post-Classify.

        // [MANT1988] Sec. 15.4: what matters is whether classification
        // produced null edges. Vertex/vertex contacts that yield none (a
        // solid tangent inside the other along whole faces, for instance)
        // leave nothing to connect, so the result follows from containment
        // as in the no-intersection case; running connect and finish on
        // them returned an empty solid.
        if ( ctx.sonea.isEmpty() && ctx.soneb.isEmpty() ) {
            // No intersections found
            res = setOpNoIntersectionCase(inSolidA, inSolidB, res, op);
            return completeSetOpResult(res, operandADiagnostics,
                operandBDiagnostics, op,
                "no-intersection-after-classify", maximizeResultFaces,
                doStrictValidation);
        }

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage04");
            debugSolid(inSolidB, "outputB_stage04");
        }

        setOpConnect(ctx, op);
        lastConnectInvariantViolations.set(
            checkConnectInvariant(inSolidA, inSolidB, ctx));

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

        res = completeSetOpResult(res, operandADiagnostics,
            operandBDiagnostics, op, resultPath, maximizeResultFaces,
            doStrictValidation);

        if ( withDebug ) {
            debugSolid(res, "outputR_stage07");
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("= [END OF SETOP REPORT] ===================================================================================================================================");
        }

        return res;
    }
}
