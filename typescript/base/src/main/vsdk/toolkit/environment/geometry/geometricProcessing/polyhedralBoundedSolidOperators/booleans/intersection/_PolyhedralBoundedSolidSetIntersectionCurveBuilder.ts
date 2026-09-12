//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Double } from "../../../../../../../../java/lang/Double.js";
import { StringBuilder } from "../../../../../../../../java/lang/StringBuilder.js";
import { ArrayList } from "../../../../../../../../java/util/ArrayList.js";
import { Collections } from "../../../../../../../../java/util/Collections.js";
import type { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidSetOperatorNullEdge } from "../topology/_PolyhedralBoundedSolidSetOperatorNullEdge.js";

/** Java `Integer.compare(int, int)`. */
function integerCompare(x: number, y: number): number {
    return x < y ? -1 : x === y ? 0 : 1;
}

/**
Result of {@link #build}. Node indexes refer to positions in the
index-aligned {@code sonea}/{@code soneb} lists given to {@code build}.
*/
class Report {
    /** Closed curves; each array holds node indexes in traversal order. */
    public readonly cycles: ArrayList<number[]>;
    /** Open curve fragments (defect: the curve should close). */
    public readonly openChains: ArrayList<number[]>;
    /** Nodes with no curve neighbor (tangential strut candidates). */
    public readonly isolatedNodes: ArrayList<number>;
    /** Nodes with more than two curve neighbors (cusp / figure-8). */
    public readonly pinchNodes: ArrayList<number>;
    /** Face-pair groups with an odd point count (tangency on the pair). */
    public readonly oddFacePairGroupCount: number;
    /** Face-pair groups whose planes were parallel or degenerate. */
    public readonly degenerateDirectionGroupCount: number;
    /** Total number of null-edge pairs examined. */
    public readonly nodeCount: number;

    /** Java private constructor, used only by the enclosing builder. */
    public constructor(
        cycles: ArrayList<number[]>,
        openChains: ArrayList<number[]>,
        isolatedNodes: ArrayList<number>,
        pinchNodes: ArrayList<number>,
        oddFacePairGroupCount: number,
        degenerateDirectionGroupCount: number,
        nodeCount: number,
    ) {
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
    public isCleanlyClosed(): boolean {
        let coveredByCycles: number;
        let i: number;

        if (
            !this.openChains.isEmpty() ||
            !this.isolatedNodes.isEmpty() ||
            !this.pinchNodes.isEmpty() ||
            this.oddFacePairGroupCount > 0 ||
            this.degenerateDirectionGroupCount > 0
        ) {
            return false;
        }
        coveredByCycles = 0;
        for (i = 0; i < this.cycles.size(); i++) {
            coveredByCycles += this.cycles.get(i).length;
        }
        return coveredByCycles === this.nodeCount;
    }

    /**
    One-line structural summary for pipeline traces.
    @return human-readable summary of cycles, chains and anomalies
    */
    public summarize(): string {
        let i: number;

        const sb = new StringBuilder();
        sb.append("curves: nodes=").append(String(this.nodeCount));
        sb.append(" cycles=").append(String(this.cycles.size())).append("[");
        for (i = 0; i < this.cycles.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(String(this.cycles.get(i).length));
        }
        sb.append("] openChains=").append(String(this.openChains.size())).append("[");
        for (i = 0; i < this.openChains.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(String(this.openChains.get(i).length));
        }
        sb.append("] isolated=").append(String(this.isolatedNodes.size()));
        sb.append(" pinch=").append(String(this.pinchNodes.size()));
        sb.append(" oddGroups=").append(String(this.oddFacePairGroupCount));
        sb.append(" degenerateGroups=").append(String(this.degenerateDirectionGroupCount));
        sb.append(" cleanlyClosed=").append(String(this.isCleanlyClosed()));
        return sb.toString();
    }
}

/** Internal carrier for one face-pair point group. */
class FacePairGroup {
    public faceA: _PolyhedralBoundedSolidFace | null = null;
    public faceB: _PolyhedralBoundedSolidFace | null = null;
    public readonly nodes = new ArrayList<number>();
}

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
export class _PolyhedralBoundedSolidSetIntersectionCurveBuilder {
    /**
    For each processing position of the most recent
    {@link #orderAndOrientAlongCurves} result, the two processing positions
    of its cycle-adjacent pairs (its junction partners along the curve).
    Lets the connect stage restrict junction-repair logic to true curve
    neighbors. Null until an order is computed.
    */
    public static lastTraversalNeighborPositions: number[][] | null = null;

    private constructor() {}

    private static halfEdgeFace(he: _PolyhedralBoundedSolidHalfEdge | null): _PolyhedralBoundedSolidFace | null {
        if (he === null || he.parentLoop === null) {
            return null;
        }
        return he.parentLoop.parentFace;
    }

    private static collectFaces(
        ne: _PolyhedralBoundedSolidSetOperatorNullEdge | null,
        outFaces: ArrayList<_PolyhedralBoundedSolidFace>,
    ): void {
        outFaces.clear();
        if (ne === null || ne.e === null) {
            return;
        }
        const rightFace = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.halfEdgeFace(ne.e.rightHalf);
        const leftFace = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.halfEdgeFace(ne.e.leftHalf);
        if (rightFace !== null) {
            outFaces.add(rightFace);
        }
        if (leftFace !== null && leftFace !== rightFace) {
            outFaces.add(leftFace);
        }
    }

    private static nodePosition(ne: _PolyhedralBoundedSolidSetOperatorNullEdge | null): Vector3Dd | null {
        if (ne === null || ne.e === null || ne.e.rightHalf === null || ne.e.rightHalf.startingVertex === null) {
            return null;
        }
        return ne.e.rightHalf.startingVertex.position;
    }

    private static faceNormal(face: _PolyhedralBoundedSolidFace | null): Vector3Dd | null {
        if (face === null) {
            return null;
        }
        const plane = face.getContainingPlane();
        if (plane === null) {
            return null;
        }
        return plane.getNormal();
    }

    private static link(neighbors: ArrayList<Set<number>>, i: number, j: number): void {
        if (i === j) {
            return;
        }
        neighbors.get(i).add(j);
        neighbors.get(j).add(i);
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
    public static build(
        sonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null,
        soneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null,
        unitVectorTolerance: number,
    ): Report {
        let n: number;
        let k: number;
        let i: number;

        n = 0;
        if (sonea !== null && soneb !== null) {
            n = Math.min(sonea.size(), soneb.size());
        }

        const cycles = new ArrayList<number[]>();
        const openChains = new ArrayList<number[]>();
        const isolatedNodes = new ArrayList<number>();
        const pinchNodes = new ArrayList<number>();
        let oddGroups = 0;
        let degenerateGroups = 0;

        if (n === 0) {
            return new Report(cycles, openChains, isolatedNodes, pinchNodes, 0, 0, 0);
        }
        const aList = sonea!;
        const bList = soneb!;

        //-----------------------------------------------------------------
        // 1. Group nodes by (faceA, faceB) pair.
        //-----------------------------------------------------------------
        // Java LinkedHashMap<Long, FacePairGroup>: insertion ordered, keyed
        // by the 64-bit value ((long)fa.id << 32) ^ (fb.id & 0xffffffffL).
        const groups = new Map<bigint, FacePairGroup>();
        const facesA = new ArrayList<_PolyhedralBoundedSolidFace>();
        const facesB = new ArrayList<_PolyhedralBoundedSolidFace>();

        for (k = 0; k < n; k++) {
            _PolyhedralBoundedSolidSetIntersectionCurveBuilder.collectFaces(aList.get(k), facesA);
            const facesACopy = new ArrayList<_PolyhedralBoundedSolidFace>(facesA);
            _PolyhedralBoundedSolidSetIntersectionCurveBuilder.collectFaces(bList.get(k), facesB);
            for (const fa of facesACopy) {
                for (const fb of facesB) {
                    const key = BigInt.asIntN(64, (BigInt(fa.id) << 32n) ^ (BigInt(fb.id) & 0xffffffffn));
                    let group = groups.get(key);
                    if (group === undefined) {
                        group = new FacePairGroup();
                        group.faceA = fa;
                        group.faceB = fb;
                        groups.set(key, group);
                    }
                    group.nodes.add(k);
                }
            }
        }

        //-----------------------------------------------------------------
        // 2. Derive curve adjacency from each group's chord structure.
        //-----------------------------------------------------------------
        const neighbors = new ArrayList<Set<number>>();
        for (k = 0; k < n; k++) {
            neighbors.add(new Set<number>());
        }

        for (const group of groups.values()) {
            const m = group.nodes.size();
            if (m < 2) {
                continue;
            }
            if (m === 2) {
                _PolyhedralBoundedSolidSetIntersectionCurveBuilder.link(
                    neighbors,
                    group.nodes.get(0),
                    group.nodes.get(1),
                );
                continue;
            }

            const normalA = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.faceNormal(group.faceA);
            const normalB = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.faceNormal(group.faceB);
            let direction: Vector3Dd | null = null;
            if (normalA !== null && normalB !== null) {
                direction = normalA.crossProduct(normalB);
                if (direction.length() <= unitVectorTolerance) {
                    direction = null;
                }
            }
            if (direction === null) {
                // Parallel or degenerate planes: chord order along the
                // intersection line is undefined; report instead of guessing.
                degenerateGroups++;
                continue;
            }

            const dir = direction;
            const sorted = new ArrayList<number>(group.nodes);
            Collections.sort(sorted, {
                compare: (a: number, b: number): number => {
                    const pa = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.nodePosition(aList.get(a));
                    const pb = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.nodePosition(aList.get(b));
                    if (pa === null || pb === null) {
                        return integerCompare(a, b);
                    }
                    const cmp = Double.compare(dir.dotProduct(pa), dir.dotProduct(pb));
                    if (cmp !== 0) {
                        return cmp;
                    }
                    return integerCompare(a, b);
                },
            });

            if (m % 2 !== 0) {
                oddGroups++;
            }
            // Entry/exit parity along the intersection line: chord
            // endpoints pair as (0,1), (2,3), ... — linking consecutive
            // sorted points across chords would bridge separate curve
            // passes over the same face pair.
            for (i = 0; i + 1 < m; i += 2) {
                _PolyhedralBoundedSolidSetIntersectionCurveBuilder.link(neighbors, sorted.get(i), sorted.get(i + 1));
            }
        }

        //-----------------------------------------------------------------
        // 3. Classify nodes and extract chains (from terminals) and cycles.
        //-----------------------------------------------------------------
        const visited: boolean[] = new Array<boolean>(n).fill(false);

        for (k = 0; k < n; k++) {
            const degree = neighbors.get(k).size;
            if (degree === 0) {
                isolatedNodes.add(k);
                visited[k] = true;
            } else if (degree > 2) {
                pinchNodes.add(k);
            }
        }

        // Chains: corridors of degree-2 nodes hanging off terminal nodes
        // (degree 1 or degree > 2). Terminal-terminal direct links are
        // deduplicated with an edge-visited set.
        const walkedTerminalLinks = new Set<bigint>();
        for (k = 0; k < n; k++) {
            const degree = neighbors.get(k).size;
            if (degree === 2 || degree === 0) {
                continue;
            }
            for (const nbBoxed of neighbors.get(k)) {
                const nb = nbBoxed;
                const nbDegree = neighbors.get(nb).size;
                if (nbDegree !== 2) {
                    const a = BigInt(Math.min(k, nb));
                    const b = BigInt(Math.max(k, nb));
                    const linkKey = BigInt.asIntN(64, (a << 32n) | b);
                    if (!walkedTerminalLinks.has(linkKey)) {
                        walkedTerminalLinks.add(linkKey);
                        openChains.add([k, nb]);
                    }
                    continue;
                }
                if (visited[nb]) {
                    continue;
                }
                const path = new ArrayList<number>();
                path.add(k);
                let prev = k;
                let cur = nb;
                while (neighbors.get(cur).size === 2 && !visited[cur]) {
                    visited[cur] = true;
                    path.add(cur);
                    let next = -1;
                    for (const candidate of neighbors.get(cur)) {
                        if (candidate !== prev) {
                            next = candidate;
                            break;
                        }
                    }
                    if (next < 0) {
                        break;
                    }
                    prev = cur;
                    cur = next;
                }
                if (cur !== prev && !path.contains(cur)) {
                    path.add(cur);
                }
                openChains.add(_PolyhedralBoundedSolidSetIntersectionCurveBuilder.toIntArray(path));
            }
        }

        // Cycles: remaining unvisited degree-2 components are pure cycles
        // (every corridor touching a terminal was consumed above).
        for (k = 0; k < n; k++) {
            if (visited[k] || neighbors.get(k).size !== 2) {
                continue;
            }
            const path = new ArrayList<number>();
            path.add(k);
            visited[k] = true;
            let prev = k;
            let cur = neighbors.get(k).values().next().value!;
            let closed = true;
            while (cur !== k) {
                if (visited[cur] || neighbors.get(cur).size !== 2) {
                    closed = false;
                    break;
                }
                visited[cur] = true;
                path.add(cur);
                let next = -1;
                for (const candidate of neighbors.get(cur)) {
                    if (candidate !== prev) {
                        next = candidate;
                        break;
                    }
                }
                if (next < 0) {
                    closed = false;
                    break;
                }
                prev = cur;
                cur = next;
            }
            if (closed) {
                cycles.add(_PolyhedralBoundedSolidSetIntersectionCurveBuilder.toIntArray(path));
            } else {
                openChains.add(_PolyhedralBoundedSolidSetIntersectionCurveBuilder.toIntArray(path));
            }
        }

        return new Report(cycles, openChains, isolatedNodes, pinchNodes, oddGroups, degenerateGroups, n);
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
    public static computeTraversalOrder(cycles: ArrayList<number[]> | null, nodeCount: number): number[] | null {
        let covered: number;
        let i: number;
        let j: number;

        if (cycles === null || nodeCount <= 0) {
            return null;
        }
        const seen: boolean[] = new Array<boolean>(nodeCount).fill(false);
        covered = 0;
        for (i = 0; i < cycles.size(); i++) {
            const cycle = cycles.get(i);
            for (j = 0; j < cycle.length; j++) {
                if (cycle[j]! < 0 || cycle[j]! >= nodeCount || seen[cycle[j]!]) {
                    return null;
                }
                seen[cycle[j]!] = true;
                covered++;
            }
        }
        if (covered !== nodeCount) {
            return null;
        }

        const ordered = new ArrayList<number[]>(cycles);
        Collections.sort(ordered, {
            compare: (a: number[], b: number[]): number =>
                integerCompare(
                    _PolyhedralBoundedSolidSetIntersectionCurveBuilder.minOf(a),
                    _PolyhedralBoundedSolidSetIntersectionCurveBuilder.minOf(b),
                ),
        });

        const permutation: number[] = new Array<number>(nodeCount).fill(0);
        let position = 0;
        for (i = 0; i < ordered.size(); i++) {
            const cycle = ordered.get(i);
            const len = cycle.length;
            let startPos = 0;
            for (j = 1; j < len; j++) {
                if (cycle[j]! < cycle[startPos]!) {
                    startPos = j;
                }
            }
            for (j = 0; j < len; j++) {
                permutation[position] = cycle[(startPos + j) % len]!;
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
    public static orderAndOrientAlongCurves(
        cycles: ArrayList<number[]> | null,
        nodeCount: number,
        sonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null,
        soneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null,
    ): number[] | null {
        let c: number;
        let j: number;

        _PolyhedralBoundedSolidSetIntersectionCurveBuilder.lastTraversalNeighborPositions = null;
        if (
            _PolyhedralBoundedSolidSetIntersectionCurveBuilder.computeTraversalOrder(cycles, nodeCount) === null ||
            sonea === null ||
            soneb === null
        ) {
            return null;
        }

        const ordered = new ArrayList<number[]>(cycles!);
        Collections.sort(ordered, {
            compare: (a: number[], b: number[]): number =>
                integerCompare(
                    _PolyhedralBoundedSolidSetIntersectionCurveBuilder.minOf(a),
                    _PolyhedralBoundedSolidSetIntersectionCurveBuilder.minOf(b),
                ),
        });

        const directedCycles = new ArrayList<number[]>();
        for (c = 0; c < ordered.size(); c++) {
            const storedCycle = ordered.get(c);
            const len = storedCycle.length;

            // Direction vote: count two-face struts whose current vertex-id
            // orientation already satisfies the scanjoin role rule in the
            // stored direction. Reversing the cycle inverts every
            // unambiguous vote, so a single count decides the direction.
            let agree = 0;
            let disagree = 0;
            for (j = 0; j < len; j++) {
                const current = storedCycle[j]!;
                const successor = storedCycle[(j + 1) % len]!;
                const sharedIsRightA = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.successorSharedHalfIsRight(
                    sonea.get(current),
                    sonea.get(successor),
                );
                if (sharedIsRightA !== null) {
                    // A-side rule: half toward successor must be LEFT.
                    if (!sharedIsRightA) {
                        agree++;
                    } else {
                        disagree++;
                    }
                }
                const sharedIsRightB = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.successorSharedHalfIsRight(
                    soneb.get(current),
                    soneb.get(successor),
                );
                if (sharedIsRightB !== null) {
                    // B-side rule: half toward successor must be RIGHT.
                    if (sharedIsRightB) {
                        agree++;
                    } else {
                        disagree++;
                    }
                }
            }
            let directedCycle: number[];
            if (disagree > agree) {
                directedCycle = new Array<number>(len).fill(0);
                for (j = 0; j < len; j++) {
                    directedCycle[j] = storedCycle[(len - j) % len]!;
                }
            } else {
                directedCycle = storedCycle;
            }

            // Orient the disagreeing minority along the chosen direction.
            for (j = 0; j < len; j++) {
                const current = directedCycle[j]!;
                const successor = directedCycle[(j + 1) % len]!;
                _PolyhedralBoundedSolidSetIntersectionCurveBuilder.orientTowardSuccessor(
                    sonea.get(current),
                    sonea.get(successor),
                    false,
                );
                _PolyhedralBoundedSolidSetIntersectionCurveBuilder.orientTowardSuccessor(
                    soneb.get(current),
                    soneb.get(successor),
                    true,
                );
            }

            // Rotation: the first cycle starts at its minimum member index;
            // later cycles start at the node geometrically closest to the
            // first cycle's start, so the proximity merge below begins in
            // phase. Emission happens after all cycles are prepared.
            let startPos = 0;
            if (directedCycles.isEmpty()) {
                for (j = 1; j < len; j++) {
                    if (directedCycle[j]! < directedCycle[startPos]!) {
                        startPos = j;
                    }
                }
            } else {
                const anchor = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.nodePosition(
                    sonea.get(directedCycles.get(0)[0]!),
                );
                let bestDistance = Number.MAX_VALUE;
                for (j = 0; j < len; j++) {
                    const p = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.nodePosition(
                        sonea.get(directedCycle[j]!),
                    );
                    if (anchor === null || p === null) {
                        continue;
                    }
                    const d = p.subtract(anchor).length();
                    if (d < bestDistance) {
                        bestDistance = d;
                        startPos = j;
                    }
                }
            }
            const rotated: number[] = new Array<number>(len).fill(0);
            for (j = 0; j < len; j++) {
                rotated[j] = directedCycle[(startPos + j) % len]!;
            }
            directedCycles.add(rotated);
        }

        // Interleave the cycles round-robin so parallel curves advance
        // together (see method javadoc). A geometric-proximity merge was
        // tried here and regressed four star motifs (mythosPlan §9):
        // round-robin with phase-aligned starts is the deterministic pacing
        // that preserves every case the legacy emission order handled.
        const permutation: number[] = new Array<number>(nodeCount).fill(0);
        let position = 0;
        let round = 0;
        while (position < nodeCount) {
            for (c = 0; c < directedCycles.size(); c++) {
                const cycle = directedCycles.get(c);
                if (round < cycle.length) {
                    permutation[position] = cycle[round]!;
                    position++;
                }
            }
            round++;
        }

        // Export curve-junction adjacency in processing-position space:
        // positionOf[originalIndex] inverts the permutation; each pair's
        // junction partners are its cycle-adjacent pairs.
        const positionOf: number[] = new Array<number>(nodeCount).fill(0);
        for (j = 0; j < nodeCount; j++) {
            positionOf[permutation[j]!] = j;
        }
        const neighborPositions: number[][] = new Array<number[]>(nodeCount);
        for (c = 0; c < directedCycles.size(); c++) {
            const cycle = directedCycles.get(c);
            const len = cycle.length;
            for (j = 0; j < len; j++) {
                const original = cycle[j]!;
                const previousOriginal = cycle[(j - 1 + len) % len]!;
                const nextOriginal = cycle[(j + 1) % len]!;
                neighborPositions[positionOf[original]!] = [positionOf[previousOriginal]!, positionOf[nextOriginal]!];
            }
        }
        _PolyhedralBoundedSolidSetIntersectionCurveBuilder.lastTraversalNeighborPositions = neighborPositions;
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
    private static successorSharedHalfIsRight(
        current: _PolyhedralBoundedSolidSetOperatorNullEdge | null,
        successor: _PolyhedralBoundedSolidSetOperatorNullEdge | null,
    ): boolean | null {
        if (current === null || successor === null || current.e === null || successor.e === null) {
            return null;
        }
        const currentRightFace = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.halfEdgeFace(current.e.rightHalf);
        const currentLeftFace = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.halfEdgeFace(current.e.leftHalf);
        if (currentRightFace === null || currentLeftFace === null || currentRightFace === currentLeftFace) {
            return null;
        }
        const successorRightFace = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.halfEdgeFace(
            successor.e.rightHalf,
        );
        const successorLeftFace = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.halfEdgeFace(successor.e.leftHalf);
        const rightShared = currentRightFace === successorRightFace || currentRightFace === successorLeftFace;
        const leftShared = currentLeftFace === successorRightFace || currentLeftFace === successorLeftFace;
        if (rightShared === leftShared) {
            return null;
        }
        return rightShared;
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
    private static orientTowardSuccessor(
        current: _PolyhedralBoundedSolidSetOperatorNullEdge,
        successor: _PolyhedralBoundedSolidSetOperatorNullEdge,
        successorSideIsRight: boolean,
    ): void {
        const sharedIsRight = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.successorSharedHalfIsRight(
            current,
            successor,
        );

        if (sharedIsRight === null) {
            return;
        }
        if (sharedIsRight !== successorSideIsRight) {
            const tmp = current.e.rightHalf;
            current.e.rightHalf = current.e.leftHalf;
            current.e.leftHalf = tmp;
        }
    }

    private static minOf(values: readonly number[]): number {
        let best: number;
        let i: number;

        best = values[0]!;
        for (i = 1; i < values.length; i++) {
            if (values[i]! < best) {
                best = values[i]!;
            }
        }
        return best;
    }

    private static toIntArray(values: ArrayList<number>): number[] {
        let i: number;

        const result: number[] = new Array<number>(values.size()).fill(0);
        for (i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }
}

type _Report = Report;
type _FacePairGroup = FacePairGroup;

export namespace _PolyhedralBoundedSolidSetIntersectionCurveBuilder {
    export type Report = _Report;
    /** Java private nested class; exported only as a type for inventory traceability. */
    export type FacePairGroup = _FacePairGroup;
}
