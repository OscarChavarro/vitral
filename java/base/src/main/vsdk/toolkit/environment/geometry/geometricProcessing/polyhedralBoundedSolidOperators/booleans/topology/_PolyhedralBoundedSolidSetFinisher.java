//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerConstructions;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidFaceGeometry;
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

    /** §9.2 instrumentation: faces triangulated in the most recent finish(). */
    private static final ThreadLocal<int[]> lastTriangulatedFaceCount =
        ThreadLocal.withInitial(() -> new int[1]);


    static int getLastTriangulatedFaceCount()
    {
        return lastTriangulatedFaceCount.get()[0];
    }

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
        _PolyhedralBoundedSolidFace face,
        _PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult pairs)
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
            pairs.pairIndexA(face) +
            "/" +
            pairs.pairIndexB(face);
    }

    private static int sanitizePairedFaces(
        ArrayList<_PolyhedralBoundedSolidFace> sonfa,
        ArrayList<_PolyhedralBoundedSolidFace> sonfb,
        _PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult pairs)
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
                    integrationRingSummary(faceA, pairs));
                continue;
            }

            pairIndexA = pairs.pairIndexA(faceA);
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
                        integrationRingSummary(faceB, pairs));
                    continue;
                }
                pairIndexB = pairs.pairIndexB(faceB);
                if ( pairIndexA != -1 && pairIndexA == pairIndexB ) {
                    tracePipelineSummary(
                        "finish sanitize match A " +
                        integrationRingSummary(faceA, pairs) +
                        " B " +
                        integrationRingSummary(faceB, pairs));
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
            // [MANT1988] Program 15.15 pairs sonfa[i] with sonfb[i]. Connect
            // tags every pair with its index; when none of them matches, the
            // null faces cannot be paired and finish has nothing to glue.
            // This used to fall back to list order; it is now an error.
            throw new SolidOperationFailedException(
                "finish: no null face pair matched by pair index (sonfa=" +
                sonfa.size() + " sonfb=" + sonfb.size() + ")");
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
    `PolyhedralBoundedSolidEulerConstructions.translationalSweepExtrudeFacePlanar`. Each
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
        _PolyhedralBoundedSolidHalfEdge bestCandidate;
        Vector3Dd p0;
        Vector3Dd p1;
        Vector3Dd p2;
        Vector3Dd a;
        Vector3Dd b;
        double bestSinTheta;
        int safety;

        if ( start == null || context == null || loopSize <= 0 ) {
            return null;
        }
        bestCandidate = null;
        bestSinTheta = 0.0;
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
                    // Use the same normalized-vector collinearity test as
                    // validateFacePointsAreCoplanar to guarantee the ear
                    // triangle will pass planarity after the split.
                    if ( a.length() > context.epsilon() &&
                         b.length() > context.epsilon() ) {
                        Vector3Dd an = a.normalized();
                        Vector3Dd bn = b.normalized();
                        double aDotB = Math.abs(an.dotProduct(bn));
                        double sinTheta = an.crossProduct(bn).length();
                        if ( aDotB < 1.0 - context.unitVectorTolerance() ) {
                            return candidate;
                        }
                        if ( sinTheta > bestSinTheta ) {
                            bestSinTheta = sinTheta;
                            bestCandidate = candidate;
                        }
                    }
                }
            }
            candidate = candidate.next();
            safety++;
        } while ( candidate != null && candidate != start &&
                  safety <= loopSize + 1 );
        // No ear with sufficient angle found — fall back to the widest ear
        // above epsilon for cases where the polygon is nearly degenerate.
        if ( bestCandidate != null &&
             bestSinTheta > context.unitVectorTolerance() / 10.0 ) {
            return bestCandidate;
        }
        return null;
    }

    /**
    Returns true when the loop has at least one vertex whose position
    coincides with another vertex earlier in the loop (i.e., the boundary
    is self-touching / figure-8). Uses scaled bigEpsilon from tol.

    A self-touching inner ring cannot be extracted as a valid face via lmfkrh
    because the resulting face would be a degenerate inverted membrane.
    */
    private static boolean hasSelfTouchingVertex(
        _PolyhedralBoundedSolidLoop loop,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext tol)
    {
        if ( loop == null ) {
            return false;
        }
        int size = loop.halfEdgesList.size();
        int i;
        int j;
        for ( i = 1; i < size; i++ ) {
            for ( j = 0; j < i; j++ ) {
                if ( PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                        loop.halfEdgesList.get(i).startingVertex.position,
                        loop.halfEdgesList.get(j).startingVertex.position,
                        tol) ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void extractInnerLoopsOfNonPlanarFace(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face)
    {
        int safety;
        int maxLoops;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext tol;

        if ( face == null || face.boundariesList == null ) {
            return;
        }
        maxLoops = face.boundariesList.size();
        safety = 0;
        tol = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        while ( face.boundariesList.size() > 1 && safety <= maxLoops ) {
            _PolyhedralBoundedSolidLoop innerLoop;

            safety++;
            innerLoop = face.boundariesList.get(1);
            if ( innerLoop == null ) {
                break;
            }
            // §3 guard: a self-touching inner loop would produce an inverted
            // membrane face when extracted via lmfkrh. Retain it as-is so the
            // containing face keeps its inner ring but no invalid face is created.
            if ( hasSelfTouchingVertex(innerLoop, tol) ) {
                Logger.reportMessage(solid, VSDK.WARNING,
                    "extractInnerLoopsOfNonPlanarFace",
                    "finish: skipped self-touching inner loop in face "
                    + face.id + "; retaining loop to avoid membrane artifact");
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

        lastTriangulatedFaceCount.get()[0] = 0;
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
            // §3: For multi-loop faces, check whether any inner loop is self-touching
            // BEFORE the planarity check. A self-touching inner ring can be coplanar
            // with the outer boundary, causing validateFaceIsPlanar to return true and
            // skip the face — leaving the guard in extractInnerLoopsOfNonPlanarFace
            // unreachable. We only bypass the planarity gate when at least one inner
            // loop is self-touching; valid inner rings preserve the baseline behaviour.
            if ( face.boundariesList.size() > 1 ) {
                boolean hasSelfTouching = false;
                PolyhedralBoundedSolidNumericPolicy.ToleranceContext tol =
                    PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
                int li;
                for ( li = 1; li < face.boundariesList.size(); li++ ) {
                    if ( hasSelfTouchingVertex(
                             face.boundariesList.get(li), tol) ) {
                        hasSelfTouching = true;
                        break;
                    }
                }
                if ( hasSelfTouching ) {
                    extractInnerLoopsOfNonPlanarFace(solid, face);
                    if ( face.boundariesList.size() != 1 ) {
                        i++;
                        continue;
                    }
                    // All inner loops were extracted or guarded; face now has
                    // 1 loop — fall through to planarity check.
                }
            }
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
                // Degenerate or collinear small face.
                _PolyhedralBoundedSolidLoop loop0 =
                    face.boundariesList.get(0);
                // §9.5-degenerate: size-1 self-referential face with orphaned
                // mirror cannot be killed via lkef. Prune it directly — the
                // edge is dangling (mirror has no parentLoop) so removing the
                // face and edge is safe and leaves the solid consistent.
                if ( loopSize == 1 &&
                     loop0.halfEdgesList.get(0) != null &&
                     loop0.halfEdgesList.get(0).next() ==
                         loop0.halfEdgesList.get(0) ) {
                    _PolyhedralBoundedSolidHalfEdge h0 =
                        loop0.halfEdgesList.get(0);
                    if ( h0.mirrorHalfEdge() == null ||
                         h0.mirrorHalfEdge().parentLoop == null ) {
                        solid.getPolygonsList().remove(i);
                        if ( h0.parentEdge != null ) {
                            int edgeIdx;
                            for ( edgeIdx = 0;
                                  edgeIdx < solid.getEdgesList().size();
                                  edgeIdx++ ) {
                                if ( solid.getEdgesList().get(edgeIdx) ==
                                     h0.parentEdge ) {
                                    solid.getEdgesList().remove(edgeIdx);
                                    break;
                                }
                            }
                        }
                        continue; // don't increment i: next face slides to i
                    }
                }
                // Try to absorb into adjacent face via lkef. When lkef
                // succeeds, the absorbed vertices may make the adjacent face
                // non-planar; restart the scan from index 0 so it is re-checked.
                boolean killed = false;
                int k;
                for ( k = 0; k < loop0.halfEdgesList.size(); k++ ) {
                    _PolyhedralBoundedSolidHalfEdge h =
                        loop0.halfEdgesList.get(k);
                    if ( h != null &&
                         h.mirrorHalfEdge() != null &&
                         h.mirrorHalfEdge().parentLoop != null &&
                         h.mirrorHalfEdge().parentLoop.parentFace != face ) {
                        PolyhedralBoundedSolidEulerOperators.lkef(
                            solid, h.mirrorHalfEdge(), h);
                        killed = true;
                        break;
                    }
                }
                if ( killed ) {
                    i = 0; // restart to re-check faces that absorbed the triangle
                } else {
                    i++;
                }
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
            } else {
                lastTriangulatedFaceCount.get()[0]++;
            }
        }
    }

    /**
    Answer integrator for the set-operations pipeline.
    Following program [MANT1988].15.15.
    */
    private static void exchange(ArrayList<_PolyhedralBoundedSolidFace> faces,
        int i, int j)
    {
        _PolyhedralBoundedSolidFace exchanged = faces.get(i);

        faces.set(i, faces.get(j));
        faces.set(j, exchanged);
    }

    /**
    Adds to `counts` the answer for one face: element 0 when a point of its
    interior is strictly inside `other`, element 1 when strictly outside.
    A face without such a point, or with it on the boundary of `other`,
    adds nothing.
    */
    private static void countSide(_PolyhedralBoundedSolidFace face,
        PolyhedralBoundedSolid other,
        PolyhedralBoundedSolidFaceGeometry[] otherFaces, int[] counts)
    {
        Vector3Dd witness =
            _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .faceInteriorWitness(face);

        if ( witness == null ) {
            return;
        }
        int status = _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .classifyPointAgainstSolid(other, otherFaces, witness);
        if ( status == Geometry.INSIDE ) {
            counts[0]++;
        }
        else if ( status == Geometry.OUTSIDE ) {
            counts[1]++;
        }
    }

    /** {@link #countSide} over the faces across the half-edges of `loop`. */
    private static int[] sidesAcross(_PolyhedralBoundedSolidLoop loop,
        PolyhedralBoundedSolid other,
        PolyhedralBoundedSolidFaceGeometry[] otherFaces)
    {
        int[] counts = new int[2];
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;
        int guard = loop.halfEdgesList.size();

        if ( start == null ) {
            return counts;
        }
        do {
            _PolyhedralBoundedSolidHalfEdge mate = he.mirrorHalfEdge();
            if ( mate != null && mate.parentLoop != null &&
                 mate.parentLoop.parentFace != loop.parentFace ) {
                countSide(mate.parentLoop.parentFace, other, otherFaces,
                    counts);
            }
            he = he.next();
            guard--;
        } while ( he != null && he != start && guard >= 0 );
        return counts;
    }

    /**
    True when the outer loop of `face` runs through the points of its ring
    and no others: the face is the null face between the two copies of a
    cut, without area of its own.
    */
    private static boolean isBand(_PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidLoop outer = face.boundariesList.get(0);
        _PolyhedralBoundedSolidLoop ring = face.boundariesList.get(1);
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context =
            PolyhedralBoundedSolidNumericPolicy.forFace(face);
        int i;
        int j;

        for ( i = 0; i < outer.halfEdgesList.size(); i++ ) {
            boolean found = false;
            for ( j = 0; j < ring.halfEdgesList.size() && !found; j++ ) {
                found = PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    outer.halfEdgesList.get(i).startingVertex.position,
                    ring.halfEdgesList.get(j).startingVertex.position,
                    context);
            }
            if ( !found ) {
                return false;
            }
        }
        return true;
    }

    /**
    `face` is a null face of connect: its ring is one copy of a cut. By
    convention the faces across the ring are the part of the solid inside
    `other`, and what lies on the side of the outer loop (the faces across
    it for a band, the face itself otherwise) is the part outside. True
    when both sides say the opposite and neither has a face that says
    otherwise.
    */
    private static boolean sidesAreSwapped(_PolyhedralBoundedSolidFace face,
        PolyhedralBoundedSolid other, int cut, String name)
    {
        setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolids(
            face.parentSolid, other));
        PolyhedralBoundedSolidFaceGeometry[] otherFaces =
            PolyhedralBoundedSolidFaceGeometry.of(other);
        int[] inside = sidesAcross(face.boundariesList.get(1), other,
            otherFaces);
        int[] outside;

        if ( isBand(face) ) {
            outside = sidesAcross(face.boundariesList.get(0), other,
                otherFaces);
        }
        else {
            outside = new int[2];
            countSide(face, other, otherFaces, outside);
        }
        boolean swapped = inside[0] == 0 && outside[1] == 0 &&
            inside[1] > 0 && outside[0] > 0;
        tracePipelineSummary("finish sides of cut " + cut + " on " + name +
            ": the part taken for inside has " + inside[0] +
            " faces inside and " + inside[1] +
            " outside; the part taken for outside " + outside[0] +
            " and " + outside[1] + (swapped ? "; exchanged" : ""));
        return swapped;
    }

    /** Adds to `reached` every face connected to `seed` through edges. */
    private static void flood(_PolyhedralBoundedSolidFace seed,
        java.util.IdentityHashMap<_PolyhedralBoundedSolidFace, Boolean>
            reached,
        ArrayList<_PolyhedralBoundedSolidFace> component)
    {
        ArrayList<_PolyhedralBoundedSolidFace> pending =
            new ArrayList<_PolyhedralBoundedSolidFace>();
        int i;

        if ( seed == null || reached.containsKey(seed) ) {
            return;
        }
        reached.put(seed, Boolean.TRUE);
        pending.add(seed);
        while ( !pending.isEmpty() ) {
            _PolyhedralBoundedSolidFace face =
                pending.remove(pending.size() - 1);
            if ( component != null ) {
                component.add(face);
            }
            for ( i = 0; i < face.boundariesList.size(); i++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
                _PolyhedralBoundedSolidHalfEdge start =
                    loop.boundaryStartHalfEdge;
                _PolyhedralBoundedSolidHalfEdge he = start;
                int guard = loop.halfEdgesList.size();
                if ( start == null ) {
                    continue;
                }
                do {
                    _PolyhedralBoundedSolidHalfEdge mate =
                        he.mirrorHalfEdge();
                    if ( mate != null && mate.parentLoop != null &&
                         mate.parentLoop.parentFace != null &&
                         !reached.containsKey(mate.parentLoop.parentFace) ) {
                        reached.put(mate.parentLoop.parentFace, Boolean.TRUE);
                        pending.add(mate.parentLoop.parentFace);
                    }
                    he = he.next();
                    guard--;
                } while ( he != null && he != start && guard >= 0 );
            }
        }
    }

    /**
    One face of every shell of `solid` that no cut reaches and that lies on
    the wanted side of `other`: inside when `keepInside`, outside
    otherwise. The side is the one of the first vertex of the shell that is
    not on the boundary of `other`; a shell with every vertex on that
    boundary is left out.
    @param cutFaces the null faces of `solid` and the faces made from their
           rings: every cut part of the solid is connected to one of them
    */
    private static ArrayList<_PolyhedralBoundedSolidFace>
    untouchedShellsToKeep(PolyhedralBoundedSolid solid,
        ArrayList<_PolyhedralBoundedSolidFace> cutFaces,
        PolyhedralBoundedSolid other, boolean keepInside)
    {
        ArrayList<_PolyhedralBoundedSolidFace> seeds =
            new ArrayList<_PolyhedralBoundedSolidFace>();
        java.util.IdentityHashMap<_PolyhedralBoundedSolidFace, Boolean>
            reached = new java.util.IdentityHashMap<>();
        PolyhedralBoundedSolidFaceGeometry[] otherFaces = null;
        int i;
        int j;
        int k;

        for ( i = 0; i < cutFaces.size(); i++ ) {
            flood(cutFaces.get(i), reached, null);
        }
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            if ( reached.containsKey(face) ) {
                continue;
            }
            ArrayList<_PolyhedralBoundedSolidFace> shell =
                new ArrayList<_PolyhedralBoundedSolidFace>();
            flood(face, reached, shell);
            if ( otherFaces == null ) {
                setNumericContext(PolyhedralBoundedSolidNumericPolicy
                    .forSolids(solid, other));
                otherFaces = PolyhedralBoundedSolidFaceGeometry.of(other);
            }
            int side = Geometry.LIMIT;
            for ( j = 0; j < shell.size() && side == Geometry.LIMIT; j++ ) {
                _PolyhedralBoundedSolidFace member = shell.get(j);
                for ( k = 0; k < member.boundariesList.size() &&
                      side == Geometry.LIMIT; k++ ) {
                    _PolyhedralBoundedSolidHalfEdge he = member
                        .boundariesList.get(k).boundaryStartHalfEdge;
                    if ( he == null || he.startingVertex == null ) {
                        continue;
                    }
                    side = _PolyhedralBoundedSolidSetNonIntersectingClassifier
                        .classifyPointAgainstSolid(other, otherFaces,
                            he.startingVertex.position);
                }
            }
            if ( (keepInside && side == Geometry.INSIDE) ||
                 (!keepInside && side == Geometry.OUTSIDE) ) {
                seeds.add(face);
                tracePipelineSummary("finish keeps a whole shell of " +
                    shell.size() + " faces, " +
                    (keepInside ? "inside" : "outside") +
                    " the other solid");
            }
        }
        return seeds;
    }

    static void finish(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op,
        int debugFlags,
        ArrayList<_PolyhedralBoundedSolidFace> sonfa,
        ArrayList<_PolyhedralBoundedSolidFace> sonfb,
        _PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult pairs)
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

        int oldsize = sanitizePairedFaces(sonfa, sonfb, pairs);
        inda = (op == INTERSECTION) ? sonfa.size() : 0;
        indb = (op == UNION) ? 0 : sonfb.size();

        // [MANT1988] Program 15.15 takes sonf[i] for the part of the solid
        // outside the other one and the face lmfkrh makes from its ring
        // for the part inside. That rests on the orientation every null
        // edge got on its way here. The parts themselves say which is
        // which: a point of the faces on each side of the cut is inside
        // the other solid or outside it. When they say the opposite of the
        // convention for a whole cut, the two faces are exchanged
        // (plan-stage11 Phase F).
        boolean[] swappedA = new boolean[oldsize];
        boolean[] swappedB = new boolean[oldsize];
        for ( i = 0; i < oldsize; i++ ) {
            swappedA[i] = sidesAreSwapped(sonfa.get(i), inSolidB, i, "A");
            swappedB[i] = sidesAreSwapped(sonfb.get(i), inSolidA, i, "B");
        }

        for ( i = 0; i < oldsize; i++ ) {
            f = PolyhedralBoundedSolidEulerOperators.lmfkrh(inSolidA, sonfa.get(i).boundariesList.get(1),
                                inSolidA.getMaxFaceId()+1);
            sonfa.add(f);

            f = PolyhedralBoundedSolidEulerOperators.lmfkrh(inSolidB, sonfb.get(i).boundariesList.get(1),
                                inSolidB.getMaxFaceId()+1);
            sonfb.add(f);
        }

        for ( i = 0; i < oldsize; i++ ) {
            if ( swappedA[i] ) {
                exchange(sonfa, i, i + oldsize);
            }
            if ( swappedB[i] ) {
                exchange(sonfb, i, i + oldsize);
            }
        }

        // Shells that no intersection curve reaches are not connected to
        // any null face, so the moves below never see them. Each one lies
        // wholly inside or wholly outside the other solid and belongs to
        // the result on the same terms as the cut parts: A outside B for
        // the union and the difference, A inside B for the intersection;
        // B outside A for the union, B inside A otherwise. They used to be
        // left behind without an error (plan-stage11 Phase F): the union
        // of a two-piece solid with a solid that crosses only one of the
        // pieces lost the other piece.
        ArrayList<_PolyhedralBoundedSolidFace> wholeShellsOfA =
            untouchedShellsToKeep(inSolidA, sonfa, inSolidB,
                op == INTERSECTION);
        ArrayList<_PolyhedralBoundedSolidFace> wholeShellsOfB =
            untouchedShellsToKeep(inSolidB, sonfb, inSolidA, op != UNION);

        if ( op == SUBTRACT) {
            inSolidB.revert();
        }

        for ( i = 0; i < oldsize; i++ ) {
            movefac(sonfa.get(i+inda), outRes);
            movefac(sonfb.get(i+indb), outRes);
        }
        for ( i = 0; i < wholeShellsOfA.size(); i++ ) {
            movefac(wholeShellsOfA.get(i), outRes);
        }
        for ( i = 0; i < wholeShellsOfB.size(); i++ ) {
            movefac(wholeShellsOfB.get(i), outRes);
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
