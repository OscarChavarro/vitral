// Port of java/base/src/test/vsdk/toolkit/environment/geometry/volume/BrepExportVariantsTest.java
import { describe, expect, it } from "vitest";
import { expectNear } from "../../../../../GTestAssertions.js";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { Tube } from "vsdk/toolkit/environment/geometry/volume/Tube.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";

/**
Variants of the boundary representations exported by the primitives: a
sphere made of quadrilaterals (besides the default triangles) and the open
lamina of the lateral surface of a cylinder, a frustum and a cone (besides
the default closed solid), and a washer (or a disk) split into
quadrilaterals (or triangles) instead of two faces with a hole.
*/
describe("BrepExportVariantsTest", () => {
    function loopSize(loop: _PolyhedralBoundedSolidLoop): number {
        const start = loop.boundaryStartHalfEdge!;
        let he: _PolyhedralBoundedSolidHalfEdge = start;
        let count = 0;
        do {
            count++;
            he = he.next()!;
        } while (he !== start);
        return count;
    }

    function faceCentroid(face: _PolyhedralBoundedSolidFace): Vector3Dd {
        const start = face.boundariesList.get(0)!.boundaryStartHalfEdge!;
        let he: _PolyhedralBoundedSolidHalfEdge = start;
        let sum = new Vector3Dd(0, 0, 0);
        let count = 0;
        do {
            sum = sum.add(he.startingVertex.position);
            count++;
            he = he.next()!;
        } while (he !== start);
        return sum.multiply(1.0 / count);
    }

    /** C++ `_PolyhedralBoundedSolidFace::containingPlaneNormal()`. */
    function containingPlaneNormal(face: _PolyhedralBoundedSolidFace): Vector3Dd {
        return face.getContainingPlane()!.getNormal();
    }

    function expectTopology(solid: PolyhedralBoundedSolid, vertices: number, edges: number, faces: number): void {
        const topology = PolyhedralBoundedSolidTopologySummary.from(solid);
        expect(topology.getVertexCount()).toBe(vertices);
        expect(topology.getEdgeCount()).toBe(edges);
        expect(topology.getFaceCount()).toBe(faces);
        expect(topology.getShellCount()).toBe(1);
    }

    /**
    Checks that the faces with ids in [1, 1] U [3, n+1] point away from the
    axis and the other ones towards it, that every face is planar and has the
    expected number of vertices.
    */
    function expectOpenLaminaSides(solid: PolyhedralBoundedSolid, n: number, verticesPerFace: number): void {
        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            expect(face.boundariesList.size()).toBe(1);
            expect(loopSize(face.boundariesList.get(0)!)).toBe(verticesPerFace);
            expect(PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(face)).toBe(true);
            const normal = containingPlaneNormal(face);
            const centroid = faceCentroid(face);
            const radial = normal.x() * centroid.x() + normal.y() * centroid.y();
            const outer = face.id === 1 || (face.id >= 3 && face.id <= n + 1);
            expect(radial > 0, "face " + face.id).toBe(outer);
        }
    }

    /**
    Checks that the faces with ids in [1, 1] U [3, n+1] face +Z and the other
    ones -Z, that every face is planar, lies on the plane z = h and has the
    expected number of vertices.
    */
    function expectFlatLaminaSides(solid: PolyhedralBoundedSolid, n: number, verticesPerFace: number, h: number): void {
        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            expect(face.boundariesList.size()).toBe(1);
            expect(loopSize(face.boundariesList.get(0)!)).toBe(verticesPerFace);
            expect(PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(face)).toBe(true);
            expectNear(faceCentroid(face).z(), h, 1e-12);
            const up = face.id === 1 || (face.id >= 3 && face.id <= n + 1);
            expectNear(containingPlaneNormal(face).z(), up ? 1.0 : -1.0, 1e-9, "face " + face.id);
        }
    }

    it("given_sphere_when_exportedByDefault_then_bandsAreTriangles", () => {
        const sphere = new Sphere(2.0);
        const solid = sphere.exportToPolyhedralBoundedSolid(8, 4);

        // 2 poles + 3 parallels of 8 vertices; 2 fans of 8 triangles and 2 bands
        // of 8 quadrilaterals split into 16 triangles
        expectTopology(solid, 26, 72, 48);
        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            expect(loopSize(solid.getPolygonsList().get(i)!.boundariesList.get(0)!)).toBe(3);
        }
    });

    it("given_sphere_when_exportedWithQuadrilaterals_then_bandsArePlanarQuads", () => {
        const meridians = 16;
        const parallels = 8;
        const sphere = new Sphere(2.0);
        const solid = sphere.exportToPolyhedralBoundedSolid(meridians, parallels, true);

        const vertices = 2 + (parallels - 1) * meridians;
        const faces = 2 * meridians + (parallels - 2) * meridians;
        expectTopology(solid, vertices, vertices + faces - 2, faces);
        let triangles = 0;
        let quadrilaterals = 0;
        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            const size = loopSize(face.boundariesList.get(0)!);
            if (size === 3) {
                triangles++;
            } else if (size === 4) {
                quadrilaterals++;
            }
            expect(PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(face)).toBe(true);
            // Outward normals
            expect(containingPlaneNormal(face).dotProduct(faceCentroid(face))).toBeGreaterThan(0.0);
        }
        expect(triangles).toBe(2 * meridians);
        expect(quadrilaterals).toBe((parallels - 2) * meridians);
        const msg = new StringBuilder();
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(solid, msg), msg.toString()).toBe(true);
    });

    it("given_cylinder_when_exportedByDefault_then_isClosedSolid", () => {
        const n = 12;
        const cylinder = new Cone(1.5, 1.5, 3.0);
        const solid = cylinder.exportToPolyhedralBoundedSolid(n, 1);

        expectTopology(solid, 2 * n, 3 * n, n + 2);
    });

    it("given_cylinder_when_exportedWithoutCaps_then_isOpenLaminaOfQuads", () => {
        const n = 12;
        const cylinder = new Cone(1.5, 1.5, 3.0);
        const solid = cylinder.exportToPolyhedralBoundedSolid(n, 1, false);

        // Two coincident sides of n quadrilaterals; each vertical edge appears
        // twice (once per side): V - E + F = 0, genus 1
        expectTopology(solid, 2 * n, 4 * n, 2 * n);
        expectOpenLaminaSides(solid, n, 4);
        for (let i = 0; i < solid.getVerticesList().size(); i++) {
            const p = solid.getVerticesList().get(i)!.position;
            expectNear(Math.hypot(p.x(), p.y()), 1.5, 1e-9);
            expect(p.z() === 0.0 || p.z() === 3.0).toBe(true);
        }
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_frustum_when_exportedWithoutCaps_then_isOpenLaminaOfPlanarQuads", () => {
        const n = 10;
        const frustum = new Cone(2.0, 0.5, 1.0);
        const solid = frustum.exportToPolyhedralBoundedSolid(n, 3, false);

        expectTopology(solid, 2 * n, 4 * n, 2 * n);
        expectOpenLaminaSides(solid, n, 4);
        for (let i = 0; i < solid.getVerticesList().size(); i++) {
            const p = solid.getVerticesList().get(i)!.position;
            const expectedRadius = p.z() === 0.0 ? 2.0 : 0.5;
            expectNear(Math.hypot(p.x(), p.y()), expectedRadius, 1e-9);
        }
    });

    it("given_cone_when_exportedWithoutCaps_then_isOpenLaminaOfTriangleFans", () => {
        const n = 8;
        const cone = new Cone(1.0, 0.0, 2.0);
        const solid = cone.exportToPolyhedralBoundedSolid(n, 1, false);

        // Base ring plus one apex per side; n edges on the ring and n to each
        // apex: V - E + F = 2, a lamina with the topology of a sphere
        expectTopology(solid, n + 2, 3 * n, 2 * n);
        expectOpenLaminaSides(solid, n, 3);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_washer_when_builtWithQuadrilaterals_then_isLaminaOfRadialQuadrilaterals", () => {
        const n = 12;
        const solid = Tube.createWasherLamina(0.5, 2.0, 0.25, n, true);

        // Same vertices as the washer, n radial edges per side: V - E + F = 0
        expectTopology(solid, 2 * n, 4 * n, 2 * n);
        expectFlatLaminaSides(solid, n, 4, 0.25);
        for (let k = 0; k < n; k++) {
            const external = solid.findVertex(k + 1)!.position;
            const internal = solid.findVertex(n + 1 + ((n - k) % n))!.position;
            expectNear(internal.x(), external.x() * 0.25, 1e-12);
            expectNear(internal.y(), external.y() * 0.25, 1e-12);
        }
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_disk_when_builtWithQuadrilaterals_then_isLaminaOfTriangleFans", () => {
        const n = 10;
        const solid = Tube.createWasherLamina(0.0, 1.5, -1.0, n, true);

        expectTopology(solid, n + 2, 3 * n, 2 * n);
        expectFlatLaminaSides(solid, n, 3, -1.0);
        expect(solid.findVertex(n + 1)!.position.x()).toBe(0.0);
        expect(solid.findVertex(n + 2)!.position.z()).toBe(-1.0);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_washer_when_builtByDefault_then_hasTwoFacesWithHoles", () => {
        const solid = Tube.createWasherLamina(0.5, 2.0, 0.0, 8);

        expectTopology(solid, 16, 16, 2);
        expect(solid.findFace(1)!.boundariesList.size()).toBe(2);
        expect(solid.findFace(2)!.boundariesList.size()).toBe(2);
    });
});
