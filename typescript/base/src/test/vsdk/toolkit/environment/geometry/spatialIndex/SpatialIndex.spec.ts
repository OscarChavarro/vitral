// Port of cpp/base/src/test/spatial_index_tests.cpp
import { describe, it } from "vitest";
import { ArrayList } from "java/util/ArrayList.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";
import { RaytraceStatistics } from "vsdk/toolkit/common/statistics/RaytraceStatistics.js";
import { PlanarFace } from "vsdk/toolkit/environment/geometry/element/PlanarFace.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { BinnedSahSpatialIndex } from "vsdk/toolkit/environment/geometry/spatialIndex/BinnedSahSpatialIndex.js";
import { GridSpatialIndex } from "vsdk/toolkit/environment/geometry/spatialIndex/GridSpatialIndex.js";
import { LinearSpatialIndex } from "vsdk/toolkit/environment/geometry/spatialIndex/LinearSpatialIndex.js";
import type { PrimitiveRayTest } from "vsdk/toolkit/environment/geometry/spatialIndex/PrimitiveRayTest.js";
import { SpatialGroupTree } from "vsdk/toolkit/environment/geometry/spatialIndex/SpatialGroupTree.js";
import type { SpatialIndex } from "vsdk/toolkit/environment/geometry/spatialIndex/SpatialIndex.js";
import { SpatialIndexWorkspace } from "vsdk/toolkit/environment/geometry/spatialIndex/SpatialIndexWorkspace.js";
import { SpatialQueryMode } from "vsdk/toolkit/environment/geometry/spatialIndex/SpatialQueryMode.js";
import {
    expectDoubleEq,
    expectEq,
    expectFalse,
    expectGe,
    expectGt,
    expectLe,
    expectLt,
    expectTrue,
} from "../../../../../GTestAssertions.js";

const f = Math.fround;

// Deterministic generator, so that every run builds the same scene and rays
class Sequence {
    private state: number;

    public constructor(seed: number) {
        this.state = seed >>> 0;
    }

    public next(): number {
        this.state = (Math.imul(this.state, 1664525) + 1013904223) >>> 0;
        return (this.state >>> 8) / (1 << 24);
    }
}

// A scene of single precision triangles with padded boxes, as an RPK scene
// gives its faces to the index
class Scene {
    private readonly faces: PlanarFace[] = [];
    private readonly boxes: ArrayList<number> = new ArrayList<number>();

    public add(a: Vector3Df, b: Vector3Df, c: Vector3Df): void {
        this.faces.push(new PlanarFace(3, [a, b, c]));
        const padding = 1e-4;
        const v = [a, b, c];
        const lo = [1e30, 1e30, 1e30];
        const hi = [-1e30, -1e30, -1e30];
        for (let i = 0; i < 3; i++) {
            const q = [v[i]!.x(), v[i]!.y(), v[i]!.z()];
            for (let k = 0; k < 3; k++) {
                lo[k] = q[k]! < lo[k]! ? q[k]! : lo[k]!;
                hi[k] = q[k]! > hi[k]! ? q[k]! : hi[k]!;
            }
        }
        for (let k = 0; k < 3; k++) {
            this.boxes.add(lo[k]! - padding);
        }
        for (let k = 0; k < 3; k++) {
            this.boxes.add(hi[k]! + padding);
        }
    }

    public face(i: number): PlanarFace {
        return this.faces[i]!;
    }

    public size(): number {
        return this.faces.length;
    }

    public getBoxes(): ArrayList<number> {
        return this.boxes;
    }
}

function randomTriangles(count: number, seed: number): Scene {
    const scene = new Scene();
    const s = new Sequence(seed);
    for (let i = 0; i < count; i++) {
        const cx = f(10.0 * s.next());
        const cy = f(10.0 * s.next());
        const cz = f(10.0 * s.next());
        const a = new Vector3Df(cx, cy, cz);
        const b = new Vector3Df(f(cx + f(s.next())), f(cy + f(0.2 * s.next())), f(cz + f(0.5 * s.next())));
        const c = new Vector3Df(f(cx + f(0.3 * s.next())), f(cy + f(s.next())), f(cz + f(0.4 * s.next())));
        scene.add(a, b, c);
    }
    return scene;
}

// The exact test of the caller: PlanarFace.rayHit on the single precision
// ray, both sides accepted, with an optional excluded face and the lowest id
// winning ties, as brute force
class FaceRayTest implements PrimitiveRayTest {
    private readonly scene: Scene;
    private readonly origin: Vector3Df;
    private readonly direction: Vector3Df;
    private readonly excluded: number;
    private acceptedPrimitive: number;

    public constructor(inScene: Scene, inOrigin: Vector3Df, inDirection: Vector3Df, inExcluded = -1) {
        this.scene = inScene;
        this.origin = inOrigin;
        this.direction = inDirection;
        this.excluded = inExcluded;
        this.acceptedPrimitive = -1;
    }

    public test(primitive: number, minimumT: number, maximumT: number, hitT: number[]): boolean {
        if (primitive === this.excluded) {
            return false;
        }
        const hit = new PlanarFace.RayHit();
        if (
            !this.scene.face(primitive).rayHit(this.origin, this.direction, true, true, f(minimumT), f(maximumT), hit)
        ) {
            return false;
        }
        const t = hit.getDistance();
        if (t === maximumT && this.acceptedPrimitive >= 0 && primitive > this.acceptedPrimitive) {
            return false;
        }
        this.acceptedPrimitive = primitive;
        hitT[0] = t;
        return true;
    }
}

function affineRay(origin: Vector3Df, direction: Vector3Df): Ray {
    return Ray.fromAffine(
        new Vector3Dd(origin.x(), origin.y(), origin.z()),
        new Vector3Dd(direction.x(), direction.y(), direction.z()),
    );
}

interface Answer {
    hit: boolean;
    primitive: number;
    parameter: number;
}

function ask(
    index: SpatialIndex,
    scene: Scene,
    origin: Vector3Df,
    direction: Vector3Df,
    minimumT: number,
    maximumT: number,
    mode: SpatialQueryMode,
    workspace: SpatialIndexWorkspace,
    excluded = -1,
): Answer {
    const test = new FaceRayTest(scene, origin, direction, excluded);
    const result = index.query(affineRay(origin, direction), minimumT, maximumT, mode, test, workspace);
    return {
        hit: result.hasHit(),
        primitive: result.getPrimitive(),
        parameter: result.hasHit() ? result.getParameter() : 0.0,
    };
}

/** @return origin and direction (C++ output pointers) */
function randomRay(s: Sequence): [Vector3Df, Vector3Df] {
    const origin = new Vector3Df(f(-2.0 + 14.0 * s.next()), f(-2.0 + 14.0 * s.next()), f(-2.0 + 14.0 * s.next()));
    // Not normalized: the index works with the affine parameter of the test
    const direction = new Vector3Df(f(-1.0 + 2.0 * s.next()), f(-1.0 + 2.0 * s.next()), f(-1.0 + 2.0 * s.next()));
    return [origin, direction];
}

// Union of the boxes of primitives first .. last
function unionBox(scene: Scene, first: number, last: number, box: number[]): void {
    for (let k = 0; k < 3; k++) {
        box[k] = 1e30;
        box[3 + k] = -1e30;
    }
    for (let i = first; i <= last; i++) {
        for (let k = 0; k < 3; k++) {
            const lo = scene.getBoxes().get(6 * i + k);
            const hi = scene.getBoxes().get(6 * i + 3 + k);
            box[k] = lo < box[k]! ? lo : box[k]!;
            box[3 + k] = hi > box[3 + k]! ? hi : box[3 + k]!;
        }
    }
}

// A scene of small clusters of triangles (as the objects of an RPK scene),
// the cluster sizes cycling through 1 .. 8 and 15, plus a few large
// triangles; and the group tree a grid is built from: the root holds one
// group per cluster, then the large triangles as primitives of their own
class ClusteredScene {
    private readonly scene: Scene = new Scene();
    private readonly tree: SpatialGroupTree;

    public constructor(seed: number) {
        const scene = this.scene;
        const s = new Sequence(seed);
        const clusterFirst = new ArrayList<number>();
        for (let c = 0; c < 60; c++) {
            const count = c % 9 === 8 ? 15 : 1 + (c % 8);
            clusterFirst.add(scene.size());
            const cx = 10.0 * s.next();
            const cy = 10.0 * s.next();
            const cz = 10.0 * s.next();
            for (let i = 0; i < count; i++) {
                const x = f(cx + 0.4 * s.next());
                const y = f(cy + 0.4 * s.next());
                const z = f(cz + 0.4 * s.next());
                scene.add(
                    new Vector3Df(x, y, z),
                    new Vector3Df(f(x + f(0.2 * s.next())), f(y + f(0.05 * s.next())), f(z + f(0.1 * s.next()))),
                    new Vector3Df(f(x + f(0.05 * s.next())), f(y + f(0.2 * s.next())), f(z + f(0.1 * s.next()))),
                );
            }
        }
        clusterFirst.add(scene.size());
        const firstLarge = scene.size();
        for (let i = 0; i < 6; i++) {
            const z = f(10.0 * s.next());
            scene.add(
                new Vector3Df(-1.0, -1.0, z),
                new Vector3Df(11.0, -1.0, f(z + 0.5)),
                new Vector3Df(5.0, 11.0, f(z - 0.5)),
            );
        }

        const box: number[] = [0, 0, 0, 0, 0, 0];
        unionBox(scene, 0, scene.size() - 1, box);
        this.tree = new SpatialGroupTree(scene.getBoxes(), box);
        for (let c = 0; c + 1 < clusterFirst.size(); c++) {
            unionBox(scene, clusterFirst.get(c), clusterFirst.get(c + 1) - 1, box);
            const group = this.tree.addGroup(0, box);
            for (let i = clusterFirst.get(c); i < clusterFirst.get(c + 1); i++) {
                this.tree.addPrimitive(group, i);
            }
        }
        for (let i = firstLarge; i < scene.size(); i++) {
            this.tree.addPrimitive(0, i);
        }
    }

    public getScene(): Scene {
        return this.scene;
    }

    public getTree(): SpatialGroupTree {
        return this.tree;
    }
}

/**
The C++ tests run the workers on threads, one shared index, one workspace
and one test per worker; JavaScript has no shared memory threads for these
objects, so the workers run one after the other, each with its own
workspace, over the same interleaved ray assignment.
*/
function runWorkers(numberOfWorkers: number, body: (w: number) => void): void {
    for (let w = 0; w < numberOfWorkers; w++) {
        body(w);
    }
}

describe("SpatialIndex", () => {
    it("ClosestHitAgreesWithTheLinearReference", () => {
        const scene = randomTriangles(400, 7);
        const linear = new LinearSpatialIndex(scene.size());
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<number>());
        const linearWorkspace = new SpatialIndexWorkspace(linear.getWorkspaceSize());
        const binnedWorkspace = new SpatialIndexWorkspace(binned.getWorkspaceSize());
        const s = new Sequence(11);
        let hits = 0;
        for (let i = 0; i < 2000; i++) {
            const [origin, direction] = randomRay(s);
            const a = ask(linear, scene, origin, direction, 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, linearWorkspace);
            const b = ask(binned, scene, origin, direction, 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, binnedWorkspace);
            expectEq(a.hit, b.hit, "ray " + i);
            if (a.hit) {
                hits++;
                expectEq(a.primitive, b.primitive, "ray " + i);
                expectEq(a.parameter, b.parameter, "ray " + i);
            }
        }
        expectGt(hits, 100);
        // The hierarchy pruned: far fewer exact tests than brute force
        expectLt(binnedWorkspace.getPrimitiveTests(), Math.trunc(linearWorkspace.getPrimitiveTests() / 4));
        expectGt(binnedWorkspace.getNodeVisits(), 0);
        expectEq(0, linearWorkspace.getNodeVisits());
    });

    it("AnyHitAgreesOnVisibility", () => {
        const scene = randomTriangles(400, 3);
        const linear = new LinearSpatialIndex(scene.size());
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<number>());
        const workspace = new SpatialIndexWorkspace();
        const s = new Sequence(5);
        for (let i = 0; i < 2000; i++) {
            const [origin, direction] = randomRay(s);
            // A segment: visibility between two points, as a shadow ray
            const a = ask(linear, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode.ANY_HIT, workspace);
            const b = ask(binned, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode.ANY_HIT, workspace);
            expectEq(a.hit, b.hit, "ray " + i);
            if (b.hit) {
                // Any accepted blocker lies in the interval
                expectGe(b.parameter, 1e-3);
                expectLe(b.parameter, 1.0);
            }
        }
    });

    it("AnyHitStopsAtTheFirstAcceptedBlocker", () => {
        // A stack of parallel squares (two triangles each) along the ray
        const scene = new Scene();
        for (let k = 0; k < 10; k++) {
            const z = k + 1;
            scene.add(new Vector3Df(-1, -1, z), new Vector3Df(1, -1, z), new Vector3Df(1, 1, z));
            scene.add(new Vector3Df(-1, -1, z), new Vector3Df(1, 1, z), new Vector3Df(-1, 1, z));
        }
        const linear = new LinearSpatialIndex(scene.size());
        const workspace = new SpatialIndexWorkspace();
        const any = ask(
            linear,
            scene,
            new Vector3Df(0.1, 0.2, 0),
            new Vector3Df(0, 0, 1),
            0.0,
            100.0,
            SpatialQueryMode.ANY_HIT,
            workspace,
        );
        expectTrue(any.hit);
        const anyTests = workspace.getPrimitiveTests();
        workspace.clearCounters();
        const closest = ask(
            linear,
            scene,
            new Vector3Df(0.1, 0.2, 0),
            new Vector3Df(0, 0, 1),
            0.0,
            100.0,
            SpatialQueryMode.CLOSEST_HIT,
            workspace,
        );
        expectTrue(closest.hit);
        expectDoubleEq(1.0, closest.parameter);
        expectLt(anyTests, workspace.getPrimitiveTests());
    });

    it("MinimumAndMaximumParametersAreInclusiveAndApplied", () => {
        const scene = new Scene();
        scene.add(new Vector3Df(-1, -1, 1), new Vector3Df(1, -1, 1), new Vector3Df(0, 1, 1)); // t = 1
        scene.add(new Vector3Df(-1, -1, 3), new Vector3Df(1, -1, 3), new Vector3Df(0, 1, 3)); // t = 3
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<number>());
        const workspace = new SpatialIndexWorkspace();
        const origin = new Vector3Df(0, 0, 0);
        const up = new Vector3Df(0, 0, 1);
        // A blocker closer than the minimum is not accepted; the next one is
        let a = ask(binned, scene, origin, up, 1.5, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectTrue(a.hit);
        expectEq(1, a.primitive);
        // Inclusive ends
        a = ask(binned, scene, origin, up, 1.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectEq(0, a.primitive);
        a = ask(binned, scene, origin, up, 1.5, 3.0, SpatialQueryMode.ANY_HIT, workspace);
        expectEq(1, a.primitive);
        // A blocker just beyond the end
        a = ask(binned, scene, origin, up, 1.5, 2.999, SpatialQueryMode.ANY_HIT, workspace);
        expectFalse(a.hit);
    });

    it("ExcludedPrimitiveDoesNotHideTheOneBehind", () => {
        const scene = new Scene();
        scene.add(new Vector3Df(-1, -1, 1), new Vector3Df(1, -1, 1), new Vector3Df(0, 1, 1));
        scene.add(new Vector3Df(-1, -1, 2), new Vector3Df(1, -1, 2), new Vector3Df(0, 1, 2));
        const linear = new LinearSpatialIndex(scene.size());
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<number>());
        const workspace = new SpatialIndexWorkspace();
        for (let m = 0; m < 2; m++) {
            const mode = m === 0 ? SpatialQueryMode.CLOSEST_HIT : SpatialQueryMode.ANY_HIT;
            const a = ask(linear, scene, new Vector3Df(0, 0, 0), new Vector3Df(0, 0, 1), 0.0, 10.0, mode, workspace, 0);
            const b = ask(binned, scene, new Vector3Df(0, 0, 0), new Vector3Df(0, 0, 1), 0.0, 10.0, mode, workspace, 0);
            expectEq(1, a.primitive);
            expectEq(1, b.primitive);
        }
    });

    it("EqualParameterTiesFollowTheTestRule", () => {
        // Two triangles sharing an edge: a ray through the edge hits both at t = 1
        const scene = new Scene();
        scene.add(new Vector3Df(-1, -1, 1), new Vector3Df(1, -1, 1), new Vector3Df(0, 1, 1));
        scene.add(new Vector3Df(-1, -1, 1), new Vector3Df(0, 1, 1), new Vector3Df(-2, 1, 1));
        const linear = new LinearSpatialIndex(scene.size());
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<number>(), 1);
        const workspace = new SpatialIndexWorkspace();
        const origin = new Vector3Df(-0.5, 0.0, 0.0);
        const a = ask(
            linear,
            scene,
            origin,
            new Vector3Df(0, 0, 1),
            0.0,
            10.0,
            SpatialQueryMode.CLOSEST_HIT,
            workspace,
        );
        const b = ask(
            binned,
            scene,
            origin,
            new Vector3Df(0, 0, 1),
            0.0,
            10.0,
            SpatialQueryMode.CLOSEST_HIT,
            workspace,
        );
        expectTrue(a.hit);
        expectTrue(b.hit);
        expectEq(a.primitive, b.primitive);
        expectEq(a.parameter, b.parameter);
    });

    it("UnboundedPrimitivesAreAlwaysTested", () => {
        const scene = new Scene();
        scene.add(new Vector3Df(-1, -1, 5), new Vector3Df(1, -1, 5), new Vector3Df(0, 1, 5));
        scene.add(new Vector3Df(-100, -100, 2), new Vector3Df(100, -100, 2), new Vector3Df(0, 100, 2));
        const unboundedPrimitives = new ArrayList<number>();
        unboundedPrimitives.add(1);
        // Primitive 1 is unbounded: its box (which would contain it) is ignored
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), unboundedPrimitives);
        const workspace = new SpatialIndexWorkspace();
        const a = ask(
            binned,
            scene,
            new Vector3Df(0, 0, 0),
            new Vector3Df(0, 0, 1),
            0.0,
            10.0,
            SpatialQueryMode.CLOSEST_HIT,
            workspace,
        );
        expectTrue(a.hit);
        expectEq(1, a.primitive);
        expectDoubleEq(2.0, a.parameter);
    });

    it("EmptyIndex", () => {
        const scene = new Scene();
        const linear = new LinearSpatialIndex(0);
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<number>());
        const workspace = new SpatialIndexWorkspace();
        expectFalse(
            ask(
                linear,
                scene,
                new Vector3Df(0, 0, 0),
                new Vector3Df(0, 0, 1),
                0.0,
                10.0,
                SpatialQueryMode.CLOSEST_HIT,
                workspace,
            ).hit,
        );
        expectFalse(
            ask(
                binned,
                scene,
                new Vector3Df(0, 0, 0),
                new Vector3Df(0, 0, 1),
                0.0,
                10.0,
                SpatialQueryMode.ANY_HIT,
                workspace,
            ).hit,
        );
        expectEq(0, workspace.getPrimitiveTests());
    });

    it("QueriesWriteNoStaticStatistics", () => {
        const scene = randomTriangles(100, 13);
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<number>());
        const workspace = new SpatialIndexWorkspace();
        const before = RaytraceStatistics.getInstanceLevelTraversals();
        const s = new Sequence(17);
        for (let i = 0; i < 100; i++) {
            const [origin, direction] = randomRay(s);
            ask(binned, scene, origin, direction, 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, workspace);
        }
        expectEq(before, RaytraceStatistics.getInstanceLevelTraversals());
        expectGt(workspace.getNodeVisits(), 0);
    });

    it("WorkspaceMailboxReportsEachPrimitiveOncePerQuery", () => {
        const workspace = new SpatialIndexWorkspace(3);
        workspace.beginMailboxQuery();
        expectTrue(workspace.firstVisit(2));
        expectFalse(workspace.firstVisit(2));
        expectTrue(workspace.firstVisit(0));
        workspace.beginMailboxQuery();
        expectTrue(workspace.firstVisit(2));
    });

    it("ConcurrentQueriesEqualSequentialOnes", () => {
        const scene = randomTriangles(300, 23);
        const binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<number>());
        const numberOfRays = 4000;
        const origins: Vector3Df[] = [];
        const directions: Vector3Df[] = [];
        const s = new Sequence(29);
        for (let i = 0; i < numberOfRays; i++) {
            const [origin, direction] = randomRay(s);
            origins.push(origin);
            directions.push(direction);
        }

        const sequential: Answer[] = new Array<Answer>(numberOfRays);
        const sequentialWorkspace = new SpatialIndexWorkspace();
        for (let i = 0; i < numberOfRays; i++) {
            sequential[i] = ask(
                binned,
                scene,
                origins[i]!,
                directions[i]!,
                0.0,
                1e30,
                SpatialQueryMode.CLOSEST_HIT,
                sequentialWorkspace,
            );
        }

        // One shared index, one workspace and one test per worker
        const numberOfWorkers = 4;
        const concurrent: Answer[] = new Array<Answer>(numberOfRays);
        runWorkers(numberOfWorkers, (w) => {
            const workspace = new SpatialIndexWorkspace(binned.getWorkspaceSize());
            for (let i = w; i < numberOfRays; i += numberOfWorkers) {
                concurrent[i] = ask(
                    binned,
                    scene,
                    origins[i]!,
                    directions[i]!,
                    0.0,
                    1e30,
                    SpatialQueryMode.CLOSEST_HIT,
                    workspace,
                );
            }
        });
        for (let i = 0; i < numberOfRays; i++) {
            expectEq(sequential[i]!.hit, concurrent[i]!.hit, "ray " + i);
            expectEq(sequential[i]!.primitive, concurrent[i]!.primitive, "ray " + i);
            expectEq(sequential[i]!.parameter, concurrent[i]!.parameter, "ray " + i);
        }
    });
});

describe("GridSpatialIndex", () => {
    it("BuildsTheGridsAsRenderPark", () => {
        const clustered = new ClusteredScene(31);
        const grid = new GridSpatialIndex(clustered.getTree());
        const count = clustered.getScene().size();
        // floor(count^(1/3) + 1) cells along each axis at the top level
        expectEq(Math.trunc(Math.floor(Math.pow(count, 0.33333) + 1.0)), grid.getResolution(0));
        // The clusters of 15 triangles fit in a top level cell: sub grids
        expectGt(grid.getNumberOfGrids(), 1);
        for (let g = 1; g < grid.getNumberOfGrids(); g++) {
            expectEq(Math.trunc(Math.floor(Math.pow(15.0, 0.33333) + 1.0)), grid.getResolution(g));
        }
        // One mailbox entry per item, fewer items than primitives (group items)
        expectEq(grid.getNumberOfItems(), grid.getWorkspaceSize());
        expectEq(count, grid.getNumberOfPrimitives());
    });

    it("ClosestHitAgreesWithTheLinearReference", () => {
        const clustered = new ClusteredScene(37);
        const scene = clustered.getScene();
        const linear = new LinearSpatialIndex(scene.size());
        const grid = new GridSpatialIndex(clustered.getTree());
        const linearWorkspace = new SpatialIndexWorkspace(linear.getWorkspaceSize());
        const gridWorkspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        const s = new Sequence(41);
        let hits = 0;
        for (let i = 0; i < 3000; i++) {
            const [origin, direction] = randomRay(s);
            const a = ask(linear, scene, origin, direction, 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, linearWorkspace);
            const b = ask(grid, scene, origin, direction, 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, gridWorkspace);
            expectEq(a.hit, b.hit, "ray " + i);
            if (a.hit) {
                hits++;
                expectEq(a.primitive, b.primitive, "ray " + i);
                expectEq(a.parameter, b.parameter, "ray " + i);
            }
        }
        expectGt(hits, 300);
        expectLt(gridWorkspace.getPrimitiveTests(), Math.trunc(linearWorkspace.getPrimitiveTests() / 4));
        expectGt(gridWorkspace.getNodeVisits(), 0);
    });

    it("AnyHitAgreesOnVisibility", () => {
        const clustered = new ClusteredScene(43);
        const scene = clustered.getScene();
        const linear = new LinearSpatialIndex(scene.size());
        const grid = new GridSpatialIndex(clustered.getTree());
        const linearWorkspace = new SpatialIndexWorkspace();
        const gridWorkspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        const s = new Sequence(47);
        let blocked = 0;
        for (let i = 0; i < 3000; i++) {
            const [origin, direction] = randomRay(s);
            const a = ask(linear, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode.ANY_HIT, linearWorkspace);
            const b = ask(grid, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode.ANY_HIT, gridWorkspace);
            expectEq(a.hit, b.hit, "ray " + i);
            if (b.hit) {
                blocked++;
                expectGe(b.parameter, 1e-3);
                expectLe(b.parameter, 1.0);
            }
        }
        expectGt(blocked, 100);
    });

    it("IntervalEndsAndExclusionsAreTheTestsOnes", () => {
        // Two parallel triangles crossing the whole scene, in one group that does
        // not fit in a cell, so that they are items of their own
        const scene = new Scene();
        scene.add(new Vector3Df(-1, -1, 1), new Vector3Df(1, -1, 1), new Vector3Df(0, 1, 1)); // t = 1
        scene.add(new Vector3Df(-1, -1, 3), new Vector3Df(1, -1, 3), new Vector3Df(0, 1, 3)); // t = 3
        const box: number[] = [0, 0, 0, 0, 0, 0];
        unionBox(scene, 0, 1, box);
        const tree = new SpatialGroupTree(scene.getBoxes(), box);
        tree.addPrimitive(0, 0);
        tree.addPrimitive(0, 1);
        const grid = new GridSpatialIndex(tree);
        const workspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        const origin = new Vector3Df(0, 0, 0);
        const up = new Vector3Df(0, 0, 1);
        let a = ask(grid, scene, origin, up, 1.5, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectTrue(a.hit);
        expectEq(1, a.primitive);
        a = ask(grid, scene, origin, up, 1.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectEq(0, a.primitive);
        a = ask(grid, scene, origin, up, 1.5, 3.0, SpatialQueryMode.ANY_HIT, workspace);
        expectEq(1, a.primitive);
        a = ask(grid, scene, origin, up, 1.5, 2.999, SpatialQueryMode.ANY_HIT, workspace);
        expectFalse(a.hit);
        // An excluded face does not hide the one behind
        a = ask(grid, scene, origin, up, 0.0, 10.0, SpatialQueryMode.ANY_HIT, workspace, 0);
        expectEq(1, a.primitive);
        // A ray starting inside the scene box, along an axis
        a = ask(
            grid,
            scene,
            new Vector3Df(0.1, 0.0, 2.0),
            new Vector3Df(0, 0, -1),
            0.0,
            10.0,
            SpatialQueryMode.CLOSEST_HIT,
            workspace,
        );
        expectTrue(a.hit);
        expectEq(0, a.primitive);
    });

    it("EmptyTree", () => {
        const scene = new Scene();
        const box = [0, 0, 0, 1, 1, 1];
        const tree = new SpatialGroupTree(scene.getBoxes(), box);
        const grid = new GridSpatialIndex(tree);
        const workspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        expectFalse(
            ask(
                grid,
                scene,
                new Vector3Df(0, 0, -1),
                new Vector3Df(0, 0, 1),
                0.0,
                10.0,
                SpatialQueryMode.CLOSEST_HIT,
                workspace,
            ).hit,
        );
        expectEq(0, workspace.getPrimitiveTests());
    });

    it("ConcurrentQueriesEqualSequentialOnes", () => {
        const clustered = new ClusteredScene(53);
        const scene = clustered.getScene();
        const grid = new GridSpatialIndex(clustered.getTree());
        const numberOfRays = 4000;
        const origins: Vector3Df[] = [];
        const directions: Vector3Df[] = [];
        const s = new Sequence(59);
        for (let i = 0; i < numberOfRays; i++) {
            const [origin, direction] = randomRay(s);
            origins.push(origin);
            directions.push(direction);
        }
        const sequential: Answer[] = new Array<Answer>(numberOfRays);
        const sequentialWorkspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        for (let i = 0; i < numberOfRays; i++) {
            const mode = i % 2 === 0 ? SpatialQueryMode.CLOSEST_HIT : SpatialQueryMode.ANY_HIT;
            sequential[i] = ask(
                grid,
                scene,
                origins[i]!,
                directions[i]!,
                0.0,
                i % 2 === 0 ? 1e30 : 1.0,
                mode,
                sequentialWorkspace,
            );
        }
        const numberOfWorkers = 4;
        const concurrent: Answer[] = new Array<Answer>(numberOfRays);
        runWorkers(numberOfWorkers, (w) => {
            const workspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
            for (let i = w; i < numberOfRays; i += numberOfWorkers) {
                const mode = i % 2 === 0 ? SpatialQueryMode.CLOSEST_HIT : SpatialQueryMode.ANY_HIT;
                concurrent[i] = ask(
                    grid,
                    scene,
                    origins[i]!,
                    directions[i]!,
                    0.0,
                    i % 2 === 0 ? 1e30 : 1.0,
                    mode,
                    workspace,
                );
            }
        });
        for (let i = 0; i < numberOfRays; i++) {
            expectEq(sequential[i]!.hit, concurrent[i]!.hit, "ray " + i);
            expectEq(sequential[i]!.primitive, concurrent[i]!.primitive, "ray " + i);
            expectEq(sequential[i]!.parameter, concurrent[i]!.parameter, "ray " + i);
        }
    });
});
