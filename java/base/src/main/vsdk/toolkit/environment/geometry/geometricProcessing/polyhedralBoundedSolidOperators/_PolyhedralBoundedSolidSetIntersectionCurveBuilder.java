//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

/**
Reconstructs the intersection curves of a boolean set operation from the
paired null-edge lists {@code sonea}/{@code soneb} produced by the classify
stage ([MANT1988].15.6), before the connect stage ([MANT1988].15.7) consumes
them.

<p>The intersection of the boundaries of two closed 2-manifolds is a set of
closed space polylines. Between two consecutive intersection points the curve
runs along the intersection line of one specific pair (faceA, faceB). Each
intersection point is generated where the curve crosses an edge of A or an
edge of B, so every point is a chord endpoint of the face pairs it lies on.
Therefore two paired null edges are candidate neighbors on a curve if and
only if they share at least one face of A <b>and</b> at least one face of B.
When more than two points lie on the same face pair, the chord structure is
recovered by sorting along the planes' intersection direction
{@code dir = nA x nB} and pairing entry/exit points by parity.</p>

<p>The resulting report orders each closed curve (cycle) as the connect
stage requires, and exposes structural anomalies that the legacy insertion
order silently turned into loose ends: open chains (a curve that does not
close: missing intersection point or unwelded coincidence), isolated nodes
(tangential grazing contacts that can never be paired by scanjoin), and
pinch nodes (figure-8 cusps where the curve touches itself).</p>
*/
final class _PolyhedralBoundedSolidSetIntersectionCurveBuilder
{
    /**
    Result of {@link #build}. Node indexes refer to positions in the
    index-aligned {@code sonea}/{@code soneb} lists given to {@code build}.
    */
    static final class Report
    {
        /** Closed curves; each array holds node indexes in traversal order. */
        final ArrayList<int[]> cycles;
        /** Open curve fragments (defect: the curve should close). */
        final ArrayList<int[]> openChains;
        /** Nodes with no curve neighbor (tangential strut candidates). */
        final ArrayList<Integer> isolatedNodes;
        /** Nodes with more than two curve neighbors (cusp / figure-8). */
        final ArrayList<Integer> pinchNodes;
        /** Face-pair groups with an odd point count (tangency on the pair). */
        final int oddFacePairGroupCount;
        /** Face-pair groups whose planes were parallel or degenerate. */
        final int degenerateDirectionGroupCount;
        /** Total number of null-edge pairs examined. */
        final int nodeCount;

        private Report(ArrayList<int[]> cycles,
            ArrayList<int[]> openChains,
            ArrayList<Integer> isolatedNodes,
            ArrayList<Integer> pinchNodes,
            int oddFacePairGroupCount,
            int degenerateDirectionGroupCount,
            int nodeCount)
        {
            this.cycles = cycles;
            this.openChains = openChains;
            this.isolatedNodes = isolatedNodes;
            this.pinchNodes = pinchNodes;
            this.oddFacePairGroupCount = oddFacePairGroupCount;
            this.degenerateDirectionGroupCount = degenerateDirectionGroupCount;
            this.nodeCount = nodeCount;
        }

        /**
        True when every node lies on a closed cycle and no structural
        anomaly was detected. Only in this state is the cycle order a
        complete, trustworthy traversal order for the connect stage.
        @return true when the curve set is structurally perfect
        */
        boolean isCleanlyClosed()
        {
            int coveredByCycles;
            int i;

            if ( !openChains.isEmpty() || !isolatedNodes.isEmpty() ||
                 !pinchNodes.isEmpty() || oddFacePairGroupCount > 0 ||
                 degenerateDirectionGroupCount > 0 ) {
                return false;
            }
            coveredByCycles = 0;
            for ( i = 0; i < cycles.size(); i++ ) {
                coveredByCycles += cycles.get(i).length;
            }
            return coveredByCycles == nodeCount;
        }

        /**
        One-line structural summary for pipeline traces.
        @return human-readable summary of cycles, chains and anomalies
        */
        String summarize()
        {
            StringBuilder sb;
            int i;

            sb = new StringBuilder();
            sb.append("curves: nodes=").append(nodeCount);
            sb.append(" cycles=").append(cycles.size()).append("[");
            for ( i = 0; i < cycles.size(); i++ ) {
                if ( i > 0 ) {
                    sb.append(",");
                }
                sb.append(cycles.get(i).length);
            }
            sb.append("] openChains=").append(openChains.size()).append("[");
            for ( i = 0; i < openChains.size(); i++ ) {
                if ( i > 0 ) {
                    sb.append(",");
                }
                sb.append(openChains.get(i).length);
            }
            sb.append("] isolated=").append(isolatedNodes.size());
            sb.append(" pinch=").append(pinchNodes.size());
            sb.append(" oddGroups=").append(oddFacePairGroupCount);
            sb.append(" degenerateGroups=").append(
                degenerateDirectionGroupCount);
            sb.append(" cleanlyClosed=").append(isCleanlyClosed());
            return sb.toString();
        }
    }

    /** Internal carrier for one face-pair point group. */
    private static final class FacePairGroup
    {
        _PolyhedralBoundedSolidFace faceA;
        _PolyhedralBoundedSolidFace faceB;
        final ArrayList<Integer> nodes = new ArrayList<Integer>();
    }

    private _PolyhedralBoundedSolidSetIntersectionCurveBuilder()
    {
    }

    private static _PolyhedralBoundedSolidFace halfEdgeFace(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( he == null || he.parentLoop == null ) {
            return null;
        }
        return he.parentLoop.parentFace;
    }

    private static void collectFaces(
        _PolyhedralBoundedSolidSetOperatorNullEdge ne,
        ArrayList<_PolyhedralBoundedSolidFace> outFaces)
    {
        _PolyhedralBoundedSolidFace rightFace;
        _PolyhedralBoundedSolidFace leftFace;

        outFaces.clear();
        if ( ne == null || ne.e == null ) {
            return;
        }
        rightFace = halfEdgeFace(ne.e.rightHalf);
        leftFace = halfEdgeFace(ne.e.leftHalf);
        if ( rightFace != null ) {
            outFaces.add(rightFace);
        }
        if ( leftFace != null && leftFace != rightFace ) {
            outFaces.add(leftFace);
        }
    }

    private static Vector3Dd nodePosition(
        _PolyhedralBoundedSolidSetOperatorNullEdge ne)
    {
        if ( ne == null || ne.e == null || ne.e.rightHalf == null ||
             ne.e.rightHalf.startingVertex == null ) {
            return null;
        }
        return ne.e.rightHalf.startingVertex.position;
    }

    private static Vector3Dd faceNormal(_PolyhedralBoundedSolidFace face)
    {
        InfinitePlane plane;

        if ( face == null ) {
            return null;
        }
        plane = face.getContainingPlane();
        if ( plane == null ) {
            return null;
        }
        return plane.getNormal();
    }

    private static void link(ArrayList<LinkedHashSet<Integer>> neighbors,
        int i,
        int j)
    {
        if ( i == j ) {
            return;
        }
        neighbors.get(i).add(Integer.valueOf(j));
        neighbors.get(j).add(Integer.valueOf(i));
    }

    /**
    Builds the intersection-curve report for the given index-aligned
    null-edge lists. The lists are not modified.
    @param sonea null edges on solid A, index-aligned with soneb
    @param soneb null edges on solid B, index-aligned with sonea
    @param unitVectorTolerance tolerance below which a cross product is
           considered degenerate (parallel face planes)
    @return structural report; never null
    */
    static Report build(
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb,
        double unitVectorTolerance)
    {
        int n;
        int k;
        int i;

        n = 0;
        if ( sonea != null && soneb != null ) {
            n = Math.min(sonea.size(), soneb.size());
        }

        ArrayList<int[]> cycles = new ArrayList<int[]>();
        ArrayList<int[]> openChains = new ArrayList<int[]>();
        ArrayList<Integer> isolatedNodes = new ArrayList<Integer>();
        ArrayList<Integer> pinchNodes = new ArrayList<Integer>();
        int oddGroups = 0;
        int degenerateGroups = 0;

        if ( n == 0 ) {
            return new Report(cycles, openChains, isolatedNodes, pinchNodes,
                0, 0, 0);
        }

        //-----------------------------------------------------------------
        // 1. Group nodes by (faceA, faceB) pair.
        //-----------------------------------------------------------------
        LinkedHashMap<Long, FacePairGroup> groups =
            new LinkedHashMap<Long, FacePairGroup>();
        ArrayList<_PolyhedralBoundedSolidFace> facesA =
            new ArrayList<_PolyhedralBoundedSolidFace>();
        ArrayList<_PolyhedralBoundedSolidFace> facesB =
            new ArrayList<_PolyhedralBoundedSolidFace>();

        for ( k = 0; k < n; k++ ) {
            collectFaces(sonea.get(k), facesA);
            ArrayList<_PolyhedralBoundedSolidFace> facesACopy =
                new ArrayList<_PolyhedralBoundedSolidFace>(facesA);
            collectFaces(soneb.get(k), facesB);
            for ( _PolyhedralBoundedSolidFace fa : facesACopy ) {
                for ( _PolyhedralBoundedSolidFace fb : facesB ) {
                    Long key = Long.valueOf(
                        (((long)fa.id) << 32) ^ (fb.id & 0xffffffffL));
                    FacePairGroup group = groups.get(key);
                    if ( group == null ) {
                        group = new FacePairGroup();
                        group.faceA = fa;
                        group.faceB = fb;
                        groups.put(key, group);
                    }
                    group.nodes.add(Integer.valueOf(k));
                }
            }
        }

        //-----------------------------------------------------------------
        // 2. Derive curve adjacency from each group's chord structure.
        //-----------------------------------------------------------------
        ArrayList<LinkedHashSet<Integer>> neighbors =
            new ArrayList<LinkedHashSet<Integer>>();
        for ( k = 0; k < n; k++ ) {
            neighbors.add(new LinkedHashSet<Integer>());
        }

        for ( FacePairGroup group : groups.values() ) {
            int m = group.nodes.size();
            if ( m < 2 ) {
                continue;
            }
            if ( m == 2 ) {
                link(neighbors, group.nodes.get(0).intValue(),
                    group.nodes.get(1).intValue());
                continue;
            }

            Vector3Dd normalA = faceNormal(group.faceA);
            Vector3Dd normalB = faceNormal(group.faceB);
            Vector3Dd direction = null;
            if ( normalA != null && normalB != null ) {
                direction = normalA.crossProduct(normalB);
                if ( direction.length() <= unitVectorTolerance ) {
                    direction = null;
                }
            }
            if ( direction == null ) {
                // Parallel or degenerate planes: chord order along the
                // intersection line is undefined; report instead of guessing.
                degenerateGroups++;
                continue;
            }

            final Vector3Dd dir = direction;
            ArrayList<Integer> sorted =
                new ArrayList<Integer>(group.nodes);
            sorted.sort((a, b) -> {
                Vector3Dd pa = nodePosition(sonea.get(a.intValue()));
                Vector3Dd pb = nodePosition(sonea.get(b.intValue()));
                if ( pa == null || pb == null ) {
                    return Integer.compare(a.intValue(), b.intValue());
                }
                int cmp = Double.compare(dir.dotProduct(pa),
                                         dir.dotProduct(pb));
                if ( cmp != 0 ) {
                    return cmp;
                }
                return Integer.compare(a.intValue(), b.intValue());
            });

            if ( (m % 2) != 0 ) {
                oddGroups++;
            }
            // Entry/exit parity along the intersection line: chord
            // endpoints pair as (0,1), (2,3), ... — linking consecutive
            // sorted points across chords would bridge separate curve
            // passes over the same face pair.
            for ( i = 0; i + 1 < m; i += 2 ) {
                link(neighbors, sorted.get(i).intValue(),
                    sorted.get(i + 1).intValue());
            }
        }

        //-----------------------------------------------------------------
        // 3. Classify nodes and extract chains (from terminals) and cycles.
        //-----------------------------------------------------------------
        boolean[] visited = new boolean[n];

        for ( k = 0; k < n; k++ ) {
            int degree = neighbors.get(k).size();
            if ( degree == 0 ) {
                isolatedNodes.add(Integer.valueOf(k));
                visited[k] = true;
            }
            else if ( degree > 2 ) {
                pinchNodes.add(Integer.valueOf(k));
            }
        }

        // Chains: corridors of degree-2 nodes hanging off terminal nodes
        // (degree 1 or degree > 2). Terminal-terminal direct links are
        // deduplicated with an edge-visited set.
        LinkedHashSet<Long> walkedTerminalLinks = new LinkedHashSet<Long>();
        for ( k = 0; k < n; k++ ) {
            int degree = neighbors.get(k).size();
            if ( degree == 2 || degree == 0 ) {
                continue;
            }
            for ( Integer nbBoxed : neighbors.get(k) ) {
                int nb = nbBoxed.intValue();
                int nbDegree = neighbors.get(nb).size();
                if ( nbDegree != 2 ) {
                    long a = Math.min(k, nb);
                    long b = Math.max(k, nb);
                    Long linkKey = Long.valueOf((a << 32) | b);
                    if ( walkedTerminalLinks.add(linkKey) ) {
                        openChains.add(new int[] { k, nb });
                    }
                    continue;
                }
                if ( visited[nb] ) {
                    continue;
                }
                ArrayList<Integer> path = new ArrayList<Integer>();
                path.add(Integer.valueOf(k));
                int prev = k;
                int cur = nb;
                while ( neighbors.get(cur).size() == 2 && !visited[cur] ) {
                    visited[cur] = true;
                    path.add(Integer.valueOf(cur));
                    int next = -1;
                    for ( Integer candidate : neighbors.get(cur) ) {
                        if ( candidate.intValue() != prev ) {
                            next = candidate.intValue();
                            break;
                        }
                    }
                    if ( next < 0 ) {
                        break;
                    }
                    prev = cur;
                    cur = next;
                }
                if ( cur != prev && !path.contains(Integer.valueOf(cur)) ) {
                    path.add(Integer.valueOf(cur));
                }
                openChains.add(toIntArray(path));
            }
        }

        // Cycles: remaining unvisited degree-2 components are pure cycles
        // (every corridor touching a terminal was consumed above).
        for ( k = 0; k < n; k++ ) {
            if ( visited[k] || neighbors.get(k).size() != 2 ) {
                continue;
            }
            ArrayList<Integer> path = new ArrayList<Integer>();
            path.add(Integer.valueOf(k));
            visited[k] = true;
            int prev = k;
            int cur = neighbors.get(k).iterator().next().intValue();
            boolean closed = true;
            while ( cur != k ) {
                if ( visited[cur] || neighbors.get(cur).size() != 2 ) {
                    closed = false;
                    break;
                }
                visited[cur] = true;
                path.add(Integer.valueOf(cur));
                int next = -1;
                for ( Integer candidate : neighbors.get(cur) ) {
                    if ( candidate.intValue() != prev ) {
                        next = candidate.intValue();
                        break;
                    }
                }
                if ( next < 0 ) {
                    closed = false;
                    break;
                }
                prev = cur;
                cur = next;
            }
            if ( closed ) {
                cycles.add(toIntArray(path));
            }
            else {
                openChains.add(toIntArray(path));
            }
        }

        return new Report(cycles, openChains, isolatedNodes, pinchNodes,
            oddGroups, degenerateGroups, n);
    }

    /**
    Computes the curve-traversal processing order for the connect stage from
    a set of closed cycles ([MANT1988] §15.7: null edges must be processed
    along each intersection curve so scanjoin finds its loose ends).

    <p>Cycles are emitted by ascending minimum member index and each cycle is
    rotated to start at its minimum member index, preserving the stored
    traversal direction. The direction is intentionally NOT canonicalized:
    {@link #applyCurveOrientation} orients every strut relative to the stored
    direction, and order and orientation must agree for scanjoin to close
    consecutive pairs (mythosPlan §5.3, probe evidence 2026-06-10).</p>

    @param cycles disjoint closed cycles in traversal order, expected to
           cover every index in {@code [0, nodeCount)} exactly once
    @param nodeCount total number of null-edge pairs
    @return permutation where {@code result[position] = originalIndex}, or
            null when the cycles do not cover the index range exactly
    */
    static int[] computeTraversalOrder(ArrayList<int[]> cycles, int nodeCount)
    {
        boolean[] seen;
        int covered;
        int i;
        int j;

        if ( cycles == null || nodeCount <= 0 ) {
            return null;
        }
        seen = new boolean[nodeCount];
        covered = 0;
        for ( i = 0; i < cycles.size(); i++ ) {
            int[] cycle = cycles.get(i);
            for ( j = 0; j < cycle.length; j++ ) {
                if ( cycle[j] < 0 || cycle[j] >= nodeCount ||
                     seen[cycle[j]] ) {
                    return null;
                }
                seen[cycle[j]] = true;
                covered++;
            }
        }
        if ( covered != nodeCount ) {
            return null;
        }

        ArrayList<int[]> ordered = new ArrayList<int[]>(cycles);
        ordered.sort((a, b) -> Integer.compare(minOf(a), minOf(b)));

        int[] permutation = new int[nodeCount];
        int position = 0;
        for ( i = 0; i < ordered.size(); i++ ) {
            int[] cycle = ordered.get(i);
            int len = cycle.length;
            int startPos = 0;
            for ( j = 1; j < len; j++ ) {
                if ( cycle[j] < cycle[startPos] ) {
                    startPos = j;
                }
            }
            for ( j = 0; j < len; j++ ) {
                permutation[position] = cycle[(startPos + j) % len];
                position++;
            }
        }
        return permutation;
    }

    /**
    Computes the connect-stage processing order along the intersection
    curves AND orients every strut consistently with that traversal, in one
    coherent operation (mythosPlan §5.3).

    <p>Derivation from the {@code neighbor} predicate ([MANT1988] §14.7.1)
    and the loose-end bookkeeping of Program 15.13: a failed pair P pushes
    the two diagonal tuples (P.A.rightHalf, P.B.leftHalf) and
    (P.A.leftHalf, P.B.rightHalf); its curve successor Q matches one of them
    only when the half of P lying in the A-face shared with Q is a
    <b>left</b> half and the half of P lying in the shared B-face is a
    <b>right</b> half (the mirrored configuration closes the ring at the
    cycle seam). Struts whose two halves lie in the same face on one solid
    satisfy the condition on that side for free; struts spanning two faces
    must have the half facing the successor in the required role.</p>

    <p>The legacy connect loop oriented struts by vertex-id comparison
    (smaller starting vertex id becomes the right half), which encodes
    classifier emission order. That orientation also feeds the
    {@code lkemr}/{@code lkef} cut semantics, so it must be preserved
    wherever it is already curve-consistent. Therefore each cycle's
    traversal direction is chosen by <b>majority vote of the existing
    vertex-id orientations</b> of its two-face struts: in the agreeing
    direction the surgery is the identity for already-consistent inputs
    (e.g. all star motifs keep their legacy orientation exactly), and only
    the disagreeing minority (the moon seam struts) is flipped.</p>

    <p>Cycles are ordered by ascending minimum member index; the first is
    rotated to start at its minimum member index and every other cycle is
    rotated to start at its node geometrically closest to the first cycle's
    start. Emission then <b>interleaves the cycles by spatial proximity</b>:
    at each step the cycle whose next pending node is closest to the last
    emitted node advances (ties broken by cycle then node index — fully
    deterministic, no tolerances). Parallel intersection curves (e.g. the
    outer and inner loops where a motif crosses both surfaces of a shell)
    must advance together region by region: completing one ring while the
    other is pending ends with {@code lkemr}/{@code lkef} cuts whose face
    re-parenting strands the pending ring's struts in different face
    fragments, making their junctions unmatchable for {@code neighbor}
    (probe evidence: moon 21 cycle 2 under concatenated emission, and moons
    20/22 under naive round-robin — mythosPlan §9). The classifier's
    emission order interleaves parallel curves regionally for the same
    reason, which is why the legacy order worked whenever it happened to be
    region-coherent.</p>

    @param cycles disjoint closed cycles (from {@link #build}), expected to
           cover every index in {@code [0, nodeCount)} exactly once
    @param nodeCount total number of null-edge pairs
    @param sonea null edges on solid A, index-aligned with soneb; struts may
           be flipped in place
    @param soneb null edges on solid B, index-aligned with sonea; struts may
           be flipped in place
    @return permutation where {@code result[position] = originalIndex}, or
            null when the cycles do not cover the index range exactly (in
            which case nothing is mutated)
    */
    static int[] orderAndOrientAlongCurves(ArrayList<int[]> cycles,
        int nodeCount,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb)
    {
        int c;
        int j;

        if ( computeTraversalOrder(cycles, nodeCount) == null ||
             sonea == null || soneb == null ) {
            return null;
        }

        ArrayList<int[]> ordered = new ArrayList<int[]>(cycles);
        ordered.sort((a, b) -> Integer.compare(minOf(a), minOf(b)));

        ArrayList<int[]> directedCycles = new ArrayList<int[]>();
        for ( c = 0; c < ordered.size(); c++ ) {
            int[] storedCycle = ordered.get(c);
            int len = storedCycle.length;

            // Direction vote: count two-face struts whose current vertex-id
            // orientation already satisfies the scanjoin role rule in the
            // stored direction. Reversing the cycle inverts every
            // unambiguous vote, so a single count decides the direction.
            int agree = 0;
            int disagree = 0;
            for ( j = 0; j < len; j++ ) {
                int current = storedCycle[j];
                int successor = storedCycle[(j + 1) % len];
                Boolean sharedIsRightA = successorSharedHalfIsRight(
                    sonea.get(current), sonea.get(successor));
                if ( sharedIsRightA != null ) {
                    // A-side rule: half toward successor must be LEFT.
                    if ( !sharedIsRightA.booleanValue() ) {
                        agree++;
                    }
                    else {
                        disagree++;
                    }
                }
                Boolean sharedIsRightB = successorSharedHalfIsRight(
                    soneb.get(current), soneb.get(successor));
                if ( sharedIsRightB != null ) {
                    // B-side rule: half toward successor must be RIGHT.
                    if ( sharedIsRightB.booleanValue() ) {
                        agree++;
                    }
                    else {
                        disagree++;
                    }
                }
            }
            int[] directedCycle;
            if ( disagree > agree ) {
                directedCycle = new int[len];
                for ( j = 0; j < len; j++ ) {
                    directedCycle[j] = storedCycle[(len - j) % len];
                }
            }
            else {
                directedCycle = storedCycle;
            }

            // Orient the disagreeing minority along the chosen direction.
            for ( j = 0; j < len; j++ ) {
                int current = directedCycle[j];
                int successor = directedCycle[(j + 1) % len];
                orientTowardSuccessor(sonea.get(current),
                    sonea.get(successor), false);
                orientTowardSuccessor(soneb.get(current),
                    soneb.get(successor), true);
            }

            // Rotation: the first cycle starts at its minimum member index;
            // later cycles start at the node geometrically closest to the
            // first cycle's start, so the proximity merge below begins in
            // phase. Emission happens after all cycles are prepared.
            int startPos = 0;
            if ( directedCycles.isEmpty() ) {
                for ( j = 1; j < len; j++ ) {
                    if ( directedCycle[j] < directedCycle[startPos] ) {
                        startPos = j;
                    }
                }
            }
            else {
                Vector3Dd anchor = nodePosition(
                    sonea.get(directedCycles.get(0)[0]));
                double bestDistance = Double.MAX_VALUE;
                for ( j = 0; j < len; j++ ) {
                    Vector3Dd p = nodePosition(sonea.get(directedCycle[j]));
                    if ( anchor == null || p == null ) {
                        continue;
                    }
                    double d = p.subtract(anchor).length();
                    if ( d < bestDistance ) {
                        bestDistance = d;
                        startPos = j;
                    }
                }
            }
            int[] rotated = new int[len];
            for ( j = 0; j < len; j++ ) {
                rotated[j] = directedCycle[(startPos + j) % len];
            }
            directedCycles.add(rotated);
        }

        // Interleave the cycles round-robin so parallel curves advance
        // together (see method javadoc). A geometric-proximity merge was
        // tried here and regressed four star motifs (mythosPlan §9):
        // round-robin with phase-aligned starts is the deterministic pacing
        // that preserves every case the legacy emission order handled.
        int[] permutation = new int[nodeCount];
        int position = 0;
        int round = 0;
        while ( position < nodeCount ) {
            for ( c = 0; c < directedCycles.size(); c++ ) {
                int[] cycle = directedCycles.get(c);
                if ( round < cycle.length ) {
                    permutation[position] = cycle[round];
                    position++;
                }
            }
            round++;
        }
        return permutation;
    }

    /**
    Determines whether the half of {@code current} lying in the face shared
    with {@code successor} is currently the right half.
    @param current strut to inspect
    @param successor next strut along the curve on the same solid
    @return true/false for an unambiguous two-face strut; null for
            same-face struts, missing faces, or ambiguous sharing
    */
    private static Boolean successorSharedHalfIsRight(
        _PolyhedralBoundedSolidSetOperatorNullEdge current,
        _PolyhedralBoundedSolidSetOperatorNullEdge successor)
    {
        _PolyhedralBoundedSolidFace currentRightFace;
        _PolyhedralBoundedSolidFace currentLeftFace;

        if ( current == null || successor == null ||
             current.e == null || successor.e == null ) {
            return null;
        }
        currentRightFace = halfEdgeFace(current.e.rightHalf);
        currentLeftFace = halfEdgeFace(current.e.leftHalf);
        if ( currentRightFace == null || currentLeftFace == null ||
             currentRightFace == currentLeftFace ) {
            return null;
        }
        _PolyhedralBoundedSolidFace successorRightFace =
            halfEdgeFace(successor.e.rightHalf);
        _PolyhedralBoundedSolidFace successorLeftFace =
            halfEdgeFace(successor.e.leftHalf);
        boolean rightShared = currentRightFace == successorRightFace ||
            currentRightFace == successorLeftFace;
        boolean leftShared = currentLeftFace == successorRightFace ||
            currentLeftFace == successorLeftFace;
        if ( rightShared == leftShared ) {
            return null;
        }
        return Boolean.valueOf(rightShared);
    }

    /**
    Flips the given strut (swaps its edge's right/left halves) when the half
    lying in the face shared with the successor strut does not have the
    required role. No-op for same-face struts (both halves already qualify)
    and for ambiguous or missing face sharing.
    @param current strut to orient
    @param successor next strut along the curve on the same solid
    @param successorSideIsRight true when the half facing the successor must
           be the right half (B side); false for the left half (A side)
    */
    private static void orientTowardSuccessor(
        _PolyhedralBoundedSolidSetOperatorNullEdge current,
        _PolyhedralBoundedSolidSetOperatorNullEdge successor,
        boolean successorSideIsRight)
    {
        Boolean sharedIsRight = successorSharedHalfIsRight(current,
            successor);

        if ( sharedIsRight == null ) {
            return;
        }
        if ( sharedIsRight.booleanValue() != successorSideIsRight ) {
            _PolyhedralBoundedSolidHalfEdge tmp = current.e.rightHalf;
            current.e.rightHalf = current.e.leftHalf;
            current.e.leftHalf = tmp;
        }
    }

    private static int minOf(int[] values)
    {
        int best;
        int i;

        best = values[0];
        for ( i = 1; i < values.length; i++ ) {
            if ( values[i] < best ) {
                best = values[i];
            }
        }
        return best;
    }

    private static int[] toIntArray(ArrayList<Integer> values)
    {
        int[] result;
        int i;

        result = new int[values.size()];
        for ( i = 0; i < values.size(); i++ ) {
            result[i] = values.get(i).intValue();
        }
        return result;
    }
}
