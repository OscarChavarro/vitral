//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;

/**
Finish stage (big phase 4) for set operations, corresponding to the answer
integration step of program [MANT1988].15.15.
*/
final class _PolyhedralBoundedSolidSetFinisher
    extends _PolyhedralBoundedSolidOperator
{
    private static final String TRACE_PIPELINE_SUMMARY_PROPERTY =
        "vsdk.setop.tracePipelineSummary";
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_06_FINISH = 0x20;

    private static boolean isPipelineSummaryTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    private static void tracePipelineSummary(String message)
    {
        if ( !isPipelineSummaryTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpPipelineTrace] " + message);
    }

    private static boolean hasUsableIntegrationRing(
        _PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidLoop ring;
        _PolyhedralBoundedSolidHalfEdge start;
        _PolyhedralBoundedSolidHalfEdge current;
        Vector3Dd reference;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context;
        int guard;

        if ( face == null ||
             !hasCompleteHalfEdgeConnectivity(face) ||
             face.boundariesList.size() < 2 ||
             face.boundariesList.get(1) == null ||
             face.boundariesList.get(1).halfEdgesList == null ) {
            return false;
        }
        ring = face.boundariesList.get(1);
        if ( ring.halfEdgesList.size() < 2 ) {
            return false;
        }
        start = ring.boundaryStartHalfEdge;
        if ( start == null || start.startingVertex == null ||
             start.startingVertex.position == null ) {
            return false;
        }
        reference = start.startingVertex.position;
        context = PolyhedralBoundedSolidNumericPolicy.forFace(face);
        current = start.next();
        guard = 0;
        while ( current != null && current != start &&
                guard <= ring.halfEdgesList.size() ) {
            if ( current.startingVertex != null &&
                 current.startingVertex.position != null &&
                 !PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                     reference, current.startingVertex.position, context) ) {
                return true;
            }
            current = current.next();
            guard++;
        }
        return false;
    }

    private static boolean hasCompleteHalfEdgeConnectivity(
        _PolyhedralBoundedSolidFace face)
    {
        int i;

        if ( face == null || face.boundariesList == null ) {
            return false;
        }
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start;
            _PolyhedralBoundedSolidHalfEdge current;
            int guard;

            if ( loop == null ||
                 loop.halfEdgesList == null ||
                 loop.boundaryStartHalfEdge == null ) {
                return false;
            }
            start = loop.boundaryStartHalfEdge;
            current = start;
            guard = 0;
            do {
                if ( current == null ||
                     current.parentEdge == null ||
                     current.parentLoop == null ||
                     current.startingVertex == null ||
                     current.mirrorHalfEdge() == null ||
                     current.next() == null ||
                     current.previous() == null ) {
                    return false;
                }
                current = current.next();
                guard++;
            } while ( current != start &&
                      guard <= loop.halfEdgesList.size() + 1 );
            if ( current != start ) {
                return false;
            }
        }
        return true;
    }

    private static String integrationRingSummary(
        _PolyhedralBoundedSolidFace face)
    {
        if ( face == null ) {
            return "null";
        }
        if ( face.boundariesList.size() < 2 ||
             face.boundariesList.get(1) == null ||
             face.boundariesList.get(1).halfEdgesList == null ) {
            return "face=" + face.id + " boundaries=" +
                face.boundariesList.size();
        }
        return "face=" + face.id + " ringSize=" +
            face.boundariesList.get(1).halfEdgesList.size() +
            " usable=" + hasUsableIntegrationRing(face) +
            " connected=" + hasCompleteHalfEdgeConnectivity(face) +
            " pair=" +
            _PolyhedralBoundedSolidSetNullEdgesConnector
                .getSonfaPairIndex(face) +
            "/" +
            _PolyhedralBoundedSolidSetNullEdgesConnector
                .getSonfbPairIndex(face);
    }

    private static int sanitizePairedFaces(
        ArrayList<_PolyhedralBoundedSolidFace> sonfa,
        ArrayList<_PolyhedralBoundedSolidFace> sonfb)
    {
        ArrayList<_PolyhedralBoundedSolidFace> matchedA;
        ArrayList<_PolyhedralBoundedSolidFace> matchedB;
        boolean[] usedB;
        int i;
        int j;

        if ( sonfa == null || sonfb == null ) {
            return 0;
        }

        matchedA = new ArrayList<_PolyhedralBoundedSolidFace>();
        matchedB = new ArrayList<_PolyhedralBoundedSolidFace>();
        usedB = new boolean[sonfb.size()];

        for ( i = 0; i < sonfa.size(); i++ ) {
            _PolyhedralBoundedSolidFace faceA = sonfa.get(i);
            int pairIndexA;
            boolean foundMatch = false;
            boolean validA = hasUsableIntegrationRing(faceA);

            if ( !validA ) {
                tracePipelineSummary(
                    "finish sanitize skip A " +
                    integrationRingSummary(faceA));
                continue;
            }

            pairIndexA = _PolyhedralBoundedSolidSetNullEdgesConnector
                .getSonfaPairIndex(faceA);
            for ( j = 0; j < sonfb.size(); j++ ) {
                _PolyhedralBoundedSolidFace faceB = sonfb.get(j);
                int pairIndexB;
                boolean validB;

                if ( usedB[j] ) {
                    continue;
                }
                validB = hasUsableIntegrationRing(faceB);
                if ( !validB ) {
                    tracePipelineSummary(
                        "finish sanitize skip B " +
                        integrationRingSummary(faceB));
                    continue;
                }
                pairIndexB = _PolyhedralBoundedSolidSetNullEdgesConnector
                    .getSonfbPairIndex(faceB);
                if ( pairIndexA != -1 && pairIndexA == pairIndexB ) {
                    tracePipelineSummary(
                        "finish sanitize match A " +
                        integrationRingSummary(faceA) +
                        " B " +
                        integrationRingSummary(faceB));
                    matchedA.add(faceA);
                    matchedB.add(faceB);
                    usedB[j] = true;
                    foundMatch = true;
                    break;
                }
            }
        }

        if ( matchedA.isEmpty() && !sonfa.isEmpty() &&
             sonfa.size() == sonfb.size() ) {
            tracePipelineSummary(
                "finish sanitize kept legacy ordering");
            return sonfa.size();
        }

        sonfa.clear();
        sonfa.addAll(matchedA);
        sonfb.clear();
        sonfb.addAll(matchedB);
        tracePipelineSummary(
            "finish sanitize matched=" + matchedA.size());
        return matchedA.size();
    }

    /**
    Restores the planar-face invariant of [MANT1988].10.2.1 after the answer
    integration step.  The Connect phase merges adjacent operand faces via
    `lkef` whenever a null-edge crosses their shared boundary; for tessellated
    curved surfaces (spheres, cylinders) those neighbours have distinct face
    normals, so the merged face is non-planar.  Subsequent `lkfmrh` + `loopGlue`
    in Finish carry that non-planarity into single-loop result faces.

    This routine fans each offending face into triangles using the same
    `lmef(scan.next, scan.previous, newId)` split used by
    `PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar`. Each
    split peels off one triangle (always planar) from the remaining polygon
    until the polygon itself is a triangle, restoring the planarity invariant
    expected by `validateIntermediate`.
    */
    private static _PolyhedralBoundedSolidHalfEdge findNonDegenerateEar(
        _PolyhedralBoundedSolidHalfEdge start,
        int loopSize,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context)
    {
        _PolyhedralBoundedSolidHalfEdge candidate;
        _PolyhedralBoundedSolidHalfEdge nextHe;
        _PolyhedralBoundedSolidHalfEdge prevHe;
        Vector3Dd p0;
        Vector3Dd p1;
        Vector3Dd p2;
        Vector3Dd a;
        Vector3Dd b;
        double tolerance;
        int safety;

        if ( start == null || context == null || loopSize <= 0 ) {
            return null;
        }
        tolerance = context.bigEpsilon();
        candidate = start;
        safety = 0;
        do {
            nextHe = candidate.next();
            prevHe = candidate.previous();
            if ( nextHe != null && prevHe != null && nextHe != prevHe &&
                 candidate.parentLoop != null &&
                 nextHe.parentLoop == candidate.parentLoop &&
                 prevHe.parentLoop == candidate.parentLoop &&
                 candidate.startingVertex != null &&
                 nextHe.startingVertex != null &&
                 prevHe.startingVertex != null ) {
                p0 = prevHe.startingVertex.position;
                p1 = candidate.startingVertex.position;
                p2 = nextHe.startingVertex.position;
                if ( p0 != null && p1 != null && p2 != null ) {
                    a = p1.subtract(p0);
                    b = p2.subtract(p0);
                    if ( a.crossProduct(b).length() > tolerance ) {
                        return candidate;
                    }
                }
            }
            candidate = candidate.next();
            safety++;
        } while ( candidate != null && candidate != start &&
                  safety <= loopSize + 1 );
        return null;
    }

    private static void extractInnerLoopsOfNonPlanarFace(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face)
    {
        int safety;
        int maxLoops;

        if ( face == null || face.boundariesList == null ) {
            return;
        }
        maxLoops = face.boundariesList.size();
        safety = 0;
        while ( face.boundariesList.size() > 1 && safety <= maxLoops ) {
            _PolyhedralBoundedSolidLoop innerLoop;

            safety++;
            innerLoop = face.boundariesList.get(1);
            if ( innerLoop == null ) {
                break;
            }
            if ( PolyhedralBoundedSolidEulerOperators.lmfkrh(solid,
                    innerLoop, solid.getMaxFaceId() + 1) == null ) {
                break;
            }
        }
    }

    static void triangulateNonPlanarFaces(PolyhedralBoundedSolid solid)
    {
        int i;
        int safetyCount;
        int maxIterations;
        int initialCount;

        i = 0;
        safetyCount = 0;
        initialCount = solid.getPolygonsList().size();
        maxIterations = 50 * (initialCount + 1);
        while ( i < solid.getPolygonsList().size() &&
                safetyCount < maxIterations ) {
            _PolyhedralBoundedSolidFace face;
            _PolyhedralBoundedSolidHalfEdge scan;
            _PolyhedralBoundedSolidHalfEdge ear;
            _PolyhedralBoundedSolidHalfEdge next;
            _PolyhedralBoundedSolidHalfEdge prev;
            PolyhedralBoundedSolidNumericPolicy.ToleranceContext context;
            int loopSize;
            int newFaceId;

            safetyCount++;
            face = solid.getPolygonsList().get(i);
            if ( PolyhedralBoundedSolidGeometricValidator.
                    validateFaceIsPlanar(face) ) {
                i++;
                continue;
            }
            if ( face.boundariesList.size() > 1 ) {
                extractInnerLoopsOfNonPlanarFace(solid, face);
                if ( face.boundariesList.size() != 1 ) {
                    i++;
                    continue;
                }
            }
            loopSize = face.boundariesList.get(0).halfEdgesList.size();
            if ( loopSize <= 3 ) {
                i++;
                continue;
            }
            scan = face.boundariesList.get(0).boundaryStartHalfEdge;
            if ( scan == null ) {
                i++;
                continue;
            }
            context = PolyhedralBoundedSolidNumericPolicy.forFace(face);
            ear = findNonDegenerateEar(scan, loopSize, context);
            if ( ear == null ) {
                i++;
                continue;
            }
            next = ear.next();
            prev = ear.previous();
            if ( next == null || prev == null || next == prev ||
                 next.parentLoop != ear.parentLoop ||
                 prev.parentLoop != ear.parentLoop ) {
                i++;
                continue;
            }
            newFaceId = (idNamespace != null)
                ? idNamespace.nextFaceId(solid)
                : solid.getMaxFaceId() + 1;
            if ( PolyhedralBoundedSolidEulerOperators.lmef(solid, next, prev,
                    newFaceId) == null ) {
                i++;
            }
        }
    }

    /**
    Answer integrator for the set-operations pipeline.
    Following program [MANT1988].15.15.
    */
    static void finish(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op,
        int debugFlags,
        ArrayList<_PolyhedralBoundedSolidFace> sonfa,
        ArrayList<_PolyhedralBoundedSolidFace> sonfb)
    {
        int i;
        int inda;
        int indb;
        _PolyhedralBoundedSolidFace f;

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 4. ------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("setOpFinish");
        }

        if ( (debugFlags & DEBUG_06_FINISH) != 0x00 ) {
            System.out.println("TESTING FINISH: " + sonfa.size());
        }
        tracePipelineSummary(
            "finish start op=" + op +
            " sonfa=" + sonfa.size() +
            " sonfb=" + sonfb.size());

        int oldsize = sanitizePairedFaces(sonfa, sonfb);
        inda = (op == INTERSECTION) ? sonfa.size() : 0;
        indb = (op == UNION) ? 0 : sonfb.size();

        for ( i = 0; i < oldsize; i++ ) {
            f = PolyhedralBoundedSolidEulerOperators.lmfkrh(inSolidA, sonfa.get(i).boundariesList.get(1),
                                inSolidA.getMaxFaceId()+1);
            sonfa.add(f);

            f = PolyhedralBoundedSolidEulerOperators.lmfkrh(inSolidB, sonfb.get(i).boundariesList.get(1),
                                inSolidB.getMaxFaceId()+1);
            sonfb.add(f);
        }

        if ( op == SUBTRACT) {
            inSolidB.revert();
        }

        for ( i = 0; i < oldsize; i++ ) {
            movefac(sonfa.get(i+inda), outRes);
            movefac(sonfb.get(i+indb), outRes);
        }

        cleanup(outRes);

        for ( i = 0; i < oldsize; i++ ) {
            PolyhedralBoundedSolidEulerOperators.lkfmrh(outRes, sonfa.get(i+inda), sonfb.get(i+indb));
            PolyhedralBoundedSolidTopologyEditing.loopGlue(outRes, sonfa.get(i+inda));
        }
        cleanup(outRes);
        triangulateNonPlanarFaces(outRes);
        PolyhedralBoundedSolidTopologyEditing.compactIds(outRes);
        tracePipelineSummary(
            "finish end outRes faces=" + outRes.getPolygonsList().size() +
            " edges=" + outRes.getEdgesList().size() +
            " vertices=" + outRes.getVerticesList().size());
    }
}
