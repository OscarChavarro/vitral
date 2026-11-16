// Port of cpp/base/src/test/triangle_acceleration_tests.cpp
import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { BinnedSahHierarchy } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahHierarchy.js";
import { BinnedSahTriangleHierarchy } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahTriangleHierarchy.js";
import { TriangleAccelerationStructure } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/TriangleAccelerationStructure.js";
import { TriangleMeshAccelerationMethod } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/TriangleMeshAccelerationMethod.js";
import { TriangleMeshAccelerationMethodUtil } from "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/TriangleMeshAccelerationMethodUtil.js";
import { TriangleMesh } from "vsdk/toolkit/environment/geometry/surface/TriangleMesh.js";
import { TriangleMeshGroup } from "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.js";

// Deterministic pseudo random numbers in [0, 1) (no platform rand()).
class TriangleAccelerationTestRandom {
    private state: number;
    public constructor(seed: number) {
        this.state = seed;
    }
    public next(): number {
        // Low 31 bits of the C++ unsigned long arithmetic.
        this.state = (Math.imul(this.state, 1103515245) + 12345) & 0x7fffffff;
        return this.state / 2147483648.0;
    }
}

const addVertex = (positions: number[], x: number, y: number, z: number): void => {
    positions.push(x);
    positions.push(y);
    positions.push(z);
};

const addTriangle = (indexes: number[], a: number, b: number, c: number): void => {
    indexes.push(a);
    indexes.push(b);
    indexes.push(c);
};

// Triangle soup: overlapping triangles of mixed sizes in [-1, 1]^3.
const buildSoup = (triangleCount: number, positions: number[], indexes: number[]): void => {
    const random = new TriangleAccelerationTestRandom(7);
    for (let i = 0; i < triangleCount; i++) {
        const cx = random.next() * 2 - 1;
        const cy = random.next() * 2 - 1;
        const cz = random.next() * 2 - 1;
        const size = random.next() < 0.1 ? 0.8 : 0.08;
        for (let k = 0; k < 3; k++) {
            addVertex(
                positions,
                cx + (random.next() - 0.5) * size,
                cy + (random.next() - 0.5) * size,
                cz + (random.next() - 0.5) * size,
            );
        }
        addTriangle(indexes, 3 * i, 3 * i + 1, 3 * i + 2);
    }
};

// Closed height field with shared vertices and edges, and an axis aligned
// flat part (zero thickness boxes) to exercise ties and degenerate slabs.
const buildGrid = (n: number, positions: number[], indexes: number[]): void => {
    for (let j = 0; j <= n; j++) {
        for (let i = 0; i <= n; i++) {
            const x = (i / n) * 2 - 1;
            const y = (j / n) * 2 - 1;
            const z = x < 0 ? 0.0 : 0.3 * x * y;
            addVertex(positions, x, y, z);
        }
    }
    for (let j = 0; j < n; j++) {
        for (let i = 0; i < n; i++) {
            const a = j * (n + 1) + i;
            addTriangle(indexes, a, a + 1, a + n + 2);
            addTriangle(indexes, a, a + n + 2, a + n + 1);
        }
    }
};

const randomRay = (random: TriangleAccelerationTestRandom, k: number): Ray => {
    const origin = new Vector3Dd(random.next() * 6 - 3, random.next() * 6 - 3, random.next() * 6 - 3);
    const target = new Vector3Dd(random.next() * 2 - 1, random.next() * 2 - 1, random.next() * 2 - 1);
    let direction = target.subtract(origin).normalized();
    // Some rays exactly parallel to the axes, through vertices and edges.
    if (k % 7 === 0) direction = new Vector3Dd(0, 0, k % 2 === 0 ? -1 : 1);
    if (k % 11 === 0) return new Ray(new Vector3Dd(0, 0, 2), new Vector3Dd(0, 0, -1));
    if (k % 13 === 0) return new Ray(new Vector3Dd(-0.5, 0.25, 2), new Vector3Dd(0, 0, -1));
    return new Ray(origin, direction);
};

/**
C++ assigns the arrays (`mesh.getVertexPositions() = positions`) and leaves
the triangle normals empty, so they are computed on the first intersection;
here the arrays are filled in place and the normals computed right away.
*/
const assignMeshArrays = (mesh: TriangleMesh, positions: number[], indexes: number[]): void => {
    mesh.initVertexPositionsArray(positions.length / 3);
    mesh.getVertexPositions()!.set(positions);
    mesh.initTriangleArrays(indexes.length / 3);
    mesh.getTriangleIndexes()!.set(indexes);
    mesh.calculateNormals();
};

const expectVectorEq = (expected: Vector3Dd, actual: Vector3Dd, message: string): void => {
    expect([actual.x(), actual.y(), actual.z()], message).toEqual([expected.x(), expected.y(), expected.z()]);
};

const expectSameAsBruteForce = (positions: number[], indexes: number[], rayCount: number): void => {
    const hierarchy = new BinnedSahTriangleHierarchy();
    hierarchy.build(positions, indexes);
    const random = new TriangleAccelerationTestRandom(11);
    let hits = 0;
    for (let k = 0; k < rayCount; k++) {
        const ray = randomRay(random, k);
        const t0 = [0],
            u0 = [0],
            v0 = [0],
            t1 = [0],
            u1 = [0],
            v1 = [0];
        const tri0 = [-1],
            tri1 = [-1];
        const hit0 = TriangleAccelerationStructure.firstHitBruteForce(ray, positions, indexes, t0, u0, v0, tri0);
        const hit1 = hierarchy.firstHit(ray, positions, indexes, t1, u1, v1, tri1);
        expect(hit1, "ray " + k).toBe(hit0);
        if (hit0) {
            hits++;
            expect(tri1[0], "ray " + k).toBe(tri0[0]);
            expect(t1[0], "ray " + k).toBe(t0[0]);
            expect(u1[0], "ray " + k).toBe(u0[0]);
            expect(v1[0], "ray " + k).toBe(v0[0]);
        }
    }
    expect(hits).toBeGreaterThan(Math.trunc(rayCount / 10));
};

describe("BinnedSahTriangleHierarchy", () => {
    it("SoupHitsMatchBruteForceExactly", () => {
        const positions: number[] = [];
        const indexes: number[] = [];
        buildSoup(3000, positions, indexes);
        expectSameAsBruteForce(positions, indexes, 4000);
    }, 120_000);

    it("SharedEdgeGridHitsMatchBruteForceExactly", () => {
        const positions: number[] = [];
        const indexes: number[] = [];
        buildGrid(40, positions, indexes);
        expectSameAsBruteForce(positions, indexes, 4000);
    }, 120_000);

    it("BuildIsDeterministicAndBounded", () => {
        const positions: number[] = [];
        const indexes: number[] = [];
        buildSoup(2000, positions, indexes);
        const a = new BinnedSahTriangleHierarchy();
        const b = new BinnedSahTriangleHierarchy();
        a.build(positions, indexes);
        b.build(positions, indexes);
        expect(b.getNodeCount()).toBe(a.getNodeCount());
        const orderA = a.getHierarchy().getPrimitiveOrder();
        const orderB = b.getHierarchy().getPrimitiveOrder();
        for (let i = 0; i < orderA.size(); i++) {
            expect(orderB.get(i)).toBe(orderA.get(i));
        }
        expect(a.getDepth()).toBeLessThanOrEqual(BinnedSahHierarchy.MAXIMUM_DEPTH);
        expect(a.getNodeCount()).toBeLessThanOrEqual(2 * 2000 - 1);
        expect(a.getDepth()).toBeGreaterThan(5);
        // Every triangle appears exactly once in the leaves.
        const seen: number[] = [];
        for (let i = 0; i < 2000; i++) seen.push(0);
        const nodes = a.getHierarchy().getNodes();
        for (let i = 0; i < nodes.size(); i++) {
            for (let k = 0; k < nodes.get(i).count; k++) {
                seen[orderA.get(nodes.get(i).leftOrFirst + k)]!++;
            }
        }
        for (let i = 0; i < 2000; i++) expect(seen[i], String(i)).toBe(1);
    });

    it("CoincidentCentroidsStillSplit", () => {
        const positions: number[] = [];
        const indexes: number[] = [];
        for (let i = 0; i < 100; i++) {
            addVertex(positions, -1, -1, 0);
            addVertex(positions, 1, -1, 0);
            addVertex(positions, 0, 2, 0);
            addTriangle(indexes, 3 * i, 3 * i + 1, 3 * i + 2);
        }
        const hierarchy = new BinnedSahTriangleHierarchy();
        hierarchy.build(positions, indexes);
        expect(hierarchy.getNodeCount()).toBeGreaterThan(1);
        const t = [0],
            u = [0],
            v = [0];
        const triangle = [-1];
        expect(
            hierarchy.firstHit(
                new Ray(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, -1)),
                positions,
                indexes,
                t,
                u,
                v,
                triangle,
            ),
        ).toBe(true);
        expect(triangle[0]).toBe(0); // ties go to the lowest index, as brute force
        expect(t[0]).toBeCloseTo(1.0, 12);
    });

    it("EmptyMeshMisses", () => {
        const positions: number[] = [];
        const indexes: number[] = [];
        const hierarchy = new BinnedSahTriangleHierarchy();
        hierarchy.build(positions, indexes);
        const t = [0],
            u = [0],
            v = [0];
        const triangle = [-1];
        expect(
            hierarchy.firstHit(
                new Ray(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, -1)),
                positions,
                indexes,
                t,
                u,
                v,
                triangle,
            ),
        ).toBe(false);
    });
});

describe("TriangleMeshAccelerationMethod", () => {
    it("NamesRoundTrip", () => {
        for (let code = 0; code < TriangleMeshAccelerationMethodUtil.COUNT; code++) {
            const method = TriangleMeshAccelerationMethodUtil.fromCode(code);
            const parsed = [TriangleMeshAccelerationMethod.BRUTE_FORCE];
            expect(
                TriangleMeshAccelerationMethodUtil.fromName(TriangleMeshAccelerationMethodUtil.name(method), parsed),
            ).toBe(true);
            expect(parsed[0] === method).toBe(true);
        }
        const unused = [TriangleMeshAccelerationMethod.BRUTE_FORCE];
        expect(TriangleMeshAccelerationMethodUtil.fromName("octree", unused)).toBe(false);
        expect(TriangleAccelerationStructure.create(TriangleMeshAccelerationMethod.BRUTE_FORCE) === null).toBe(true);
    });
});

describe("TriangleMesh", () => {
    it("AccelerationStructureIsPreparedCopiedAndReleased", () => {
        const mesh = new TriangleMesh();
        const positions: number[] = [];
        const indexes: number[] = [];
        buildGrid(10, positions, indexes);
        assignMeshArrays(mesh, positions, indexes);
        expect(mesh.getAccelerationStructure() === null).toBe(true);

        const ray = new Ray(new Vector3Dd(0.31, -0.27, 2), new Vector3Dd(0, 0, -1));
        const bruteHit = new RayHit();
        expect(mesh.doIntersectionFirstHit(ray, bruteHit)).toBe(true);

        mesh.prepareAccelerationStructure(TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH);
        const built = mesh.getAccelerationStructure();
        expect(built !== null).toBe(true);
        mesh.prepareAccelerationStructure(TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH);
        expect(mesh.getAccelerationStructure() === built).toBe(true); // not rebuilt

        const prepared: number[] = [0, 0, 0, 0, 0, 0];
        expect(mesh.getPreparedMinMax(prepared)).toBe(true);
        const walked = mesh.getMinMax();
        for (let i = 0; i < 6; i++) expect(prepared[i], String(i)).toBe(walked[i]);

        const acceleratedHit = new RayHit();
        expect(mesh.doIntersectionFirstHit(ray, acceleratedHit)).toBe(true);
        expectVectorEq(bruteHit.point, acceleratedHit.point, "point");
        expectVectorEq(bruteHit.normal, acceleratedHit.normal, "normal");

        const copy = mesh.clone() as TriangleMesh;
        expect(copy.getAccelerationStructure() !== null).toBe(true);
        expect(copy.getAccelerationStructure() !== built).toBe(true); // deep copy

        mesh.initTriangleArrays(0); // a mutator releases it
        expect(mesh.getAccelerationStructure() === null).toBe(true);
        expect(mesh.getPreparedMinMax(prepared)).toBe(false);
        copy.prepareAccelerationStructure(TriangleMeshAccelerationMethod.BRUTE_FORCE);
        expect(copy.getAccelerationStructure() === null).toBe(true);
    });
});

// Many small meshes, as an OBJ file split in groups: the top level hierarchy
// must give the hit of the linear loop over the meshes (mesh, triangle,
// point, normal), including meshes sharing an edge.
describe("TriangleMeshGroup", () => {
    it("MeshHierarchyHitsMatchLinearLoop", () => {
        const linear = new TriangleMeshGroup();
        const accelerated = new TriangleMeshGroup();
        for (let m = 0; m < 60; m++) {
            const positions: number[] = [];
            const indexes: number[] = [];
            if (m % 3 === 0) {
                buildGrid(4, positions, indexes);
                for (let i = 0; i < positions.length; i += 3) {
                    positions[i] = positions[i]! * 0.2 + (m % 6) * 0.3 - 0.9;
                    positions[i + 1] = positions[i + 1]! * 0.2 + Math.trunc(m / 6) * 0.2 - 0.9;
                }
            } else {
                buildSoup(40, positions, indexes);
            }
            const mesh = new TriangleMesh();
            assignMeshArrays(mesh, positions, indexes);
            // cpp `addMesh` stores a copy; TypeScript stores the reference, so
            // each group gets its own mesh object.
            linear.addMesh(mesh);
            accelerated.addMesh(mesh.clone() as TriangleMesh);
        }
        accelerated.prepareAccelerationStructures(TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH);
        expect(accelerated.getMeshHierarchy() !== null).toBe(true);
        expect(linear.getMeshHierarchy() === null).toBe(true);
        const preparedBounds: number[] = [0, 0, 0, 0, 0, 0];
        expect(accelerated.getPreparedMinMax(preparedBounds)).toBe(true);
        const walked = accelerated.getMinMax();
        for (let i = 0; i < 6; i++) expect(preparedBounds[i], String(i)).toBe(walked[i]);

        const random = new TriangleAccelerationTestRandom(5);
        let hits = 0;
        for (let k = 0; k < 3000; k++) {
            const ray = randomRay(random, k);
            const hit0 = new RayHit();
            const hit1 = new RayHit();
            const found0 = linear.doIntersectionFirstHit(ray, hit0);
            const info0 = linear.doIntersectionInformation();
            const found1 = accelerated.doIntersectionFirstHit(ray, hit1);
            const info1 = accelerated.doIntersectionInformation();
            expect(found1, "ray " + k).toBe(found0);
            expect(info1[0], "ray " + k).toBe(info0[0]);
            expect(info1[1], "ray " + k).toBe(info0[1]);
            if (found0) {
                hits++;
                expectVectorEq(hit0.point, hit1.point, "ray " + k);
                expectVectorEq(hit0.normal, hit1.normal, "ray " + k);
            }
        }
        expect(hits).toBeGreaterThan(300);

        accelerated.addMesh(new TriangleMesh()); // drops the top level
        expect(accelerated.getMeshHierarchy() === null).toBe(true);
        accelerated.prepareAccelerationStructures(TriangleMeshAccelerationMethod.BRUTE_FORCE);
        expect(accelerated.getMeshHierarchy() === null).toBe(true);
    }, 120_000);
});
