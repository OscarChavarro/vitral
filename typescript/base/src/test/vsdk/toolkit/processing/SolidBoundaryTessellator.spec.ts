// Port of cpp/base/src/test/solid_boundary_tessellator_tests.cpp
import { test } from "vitest";
import { expectEq, expectGt, expectLt, expectNe, expectNear, expectTrue } from "../../../GTestAssertions.js";
import { TessellationMeshCheck } from "../../../TessellationMeshCheck.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { BezierPatchSubdivision } from "vsdk/toolkit/environment/geometry/surface/BezierPatchSubdivision.js";
import { SubdividedBezierPatch } from "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.js";
import { BlobComponent } from "vsdk/toolkit/environment/geometry/volume/BlobComponent.js";
import { BlobSolid } from "vsdk/toolkit/environment/geometry/volume/BlobSolid.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { CsgBody } from "vsdk/toolkit/environment/scene/csg/CsgBody.js";
import { CsgBodyEvaluator } from "vsdk/toolkit/environment/scene/csg/CsgBodyEvaluator.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";
import { BezierPatchTessellator } from "vsdk/toolkit/processing/BezierPatchTessellator.js";
import { SolidBoundaryTessellationReport } from "vsdk/toolkit/processing/SolidBoundaryTessellationReport.js";
import { SolidBoundaryTessellator } from "vsdk/toolkit/processing/SolidBoundaryTessellator.js";

// A placed sphere: a closed, outward mesh of genus 0 whose vertices lie on the
// sphere, with the area and volume of the sphere within the resolution.
test("SolidBoundaryTessellatorContract_SphereMeshIsClosedOutwardAndOnTheSurface", () => {
    const ball = new SimpleBody();
    ball.setGeometry(new Sphere(1.0));
    ball.setPosition(new Vector3Dd(0.3, -0.2, 0.1));
    const region = new GeometryBounds();
    expectTrue(region.setFinite(new Vector3Dd(-0.8, -1.3, -1.0), new Vector3Dd(1.4, 0.9, 1.2)));
    const report = new SolidBoundaryTessellationReport();
    const mesh = SolidBoundaryTessellator.tessellate(ball, region, 32, report);
    expectNe(null, mesh);
    expectTrue(TessellationMeshCheck.isClosedAndOriented(mesh!));
    expectEq(2, TessellationMeshCheck.eulerCharacteristic(mesh!));
    expectNear((4.0 * Math.PI) / 3.0, TessellationMeshCheck.enclosedVolume(mesh!), (0.03 * 4.0 * Math.PI) / 3.0);
    expectNear(4.0 * Math.PI, TessellationMeshCheck.area(mesh!), 0.05 * 4.0 * Math.PI);
    expectEq(0, report.midpointCrossings);
    expectEq(0, report.regionCrossings);
    expectEq(0, report.pointClassifiedLines);
    expectEq(report.triangles, mesh!.getNumTriangles());
    expectEq(report.boundaryVertices + report.cellVertices, mesh!.getNumVertices());
    expectGt(report.boundaryVertices, 0.95 * mesh!.getNumVertices());
    const normals = mesh!.getVertexNormals()!;
    let onSurface = 0;
    for (let v = 0; v < mesh!.getNumVertices(); v++) {
        const p = TessellationMeshCheck.vertex(mesh!, v).subtract(ball.getPosition());
        expectNear(1.0, p.length(), report.cellSize);
        if (Math.abs(p.length() - 1.0) < 1e-9) onSurface++;
        // Vertex normals are the sphere's outward normals.
        const n = new Vector3Dd(normals[v * 3]!, normals[v * 3 + 1]!, normals[v * 3 + 2]!);
        expectGt(n.dotProduct(p.normalized()), 0.9);
    }
    expectEq(report.boundaryVertices, onSurface);
});

// A CSG difference: the cavity is a second shell facing into it.
test("SolidBoundaryTessellatorContract_CsgDifferenceKeepsTheCavity", () => {
    for (let e = 0; e < 2; e++) {
        const body = new CsgBody(
            CsgOperation.DIFFERENCE,
            e === 0 ? CsgBodyEvaluator.ROTH : CsgBodyEvaluator.CROSSING_MEMBERSHIP,
        );
        const outer = new SimpleBody();
        outer.setGeometry(new Box(2.0, 2.0, 2.0));
        const hole = new SimpleBody();
        hole.setGeometry(new Sphere(0.6));
        expectTrue(body.addLeaf(0, outer));
        expectTrue(body.addLeaf(0, hole));
        const region = new GeometryBounds();
        expectTrue(region.setFinite(new Vector3Dd(-1.23, -1.23, -1.23), new Vector3Dd(1.23, 1.23, 1.23)));
        const report = new SolidBoundaryTessellationReport();
        const mesh = SolidBoundaryTessellator.tessellate(body, region, 24, report);
        expectNe(null, mesh);
        expectTrue(TessellationMeshCheck.isClosedAndOriented(mesh!), String(e));
        // Two spheres' worth of Euler characteristic: the box shell and the cavity.
        expectEq(4, TessellationMeshCheck.eulerCharacteristic(mesh!), String(e));
        const expected = 8.0 - (4.0 * Math.PI * 0.216) / 3.0;
        expectNear(expected, TessellationMeshCheck.enclosedVolume(mesh!), 0.04 * expected, String(e));
        expectEq(0, report.regionCrossings, String(e));
        expectGt(report.exactCrossings, 0, String(e));
    }
}, 20_000);

// A blob of two merged components is one closed shell.
test("SolidBoundaryTessellatorContract_MergedBlobIsOneShell", () => {
    const blob = new BlobSolid(0.6, false);
    blob.addComponent(new BlobComponent(new Vector3Dd(-0.8, 0, 0), 1.5, 1.0));
    blob.addComponent(new BlobComponent(new Vector3Dd(0.8, 0, 0), 1.5, 1.0));
    const body = new SimpleBody();
    body.setGeometry(blob);
    const region = new GeometryBounds();
    body.getSolidWorldBounds(region);
    expectEq(GeometryBounds.Kind.Finite, region.getKind());
    const report = new SolidBoundaryTessellationReport();
    const mesh = SolidBoundaryTessellator.tessellate(body, region.inflated(0.1), 40, report);
    expectNe(null, mesh);
    expectTrue(TessellationMeshCheck.isClosedAndOriented(mesh!));
    expectEq(2, TessellationMeshCheck.eulerCharacteristic(mesh!));
    expectGt(TessellationMeshCheck.enclosedVolume(mesh!), 0);
    expectEq(0, report.regionCrossings);
    for (let v = 0; v < mesh!.getNumVertices(); v++) {
        const p = TessellationMeshCheck.vertex(mesh!, v);
        // Every vertex is within a cell of the threshold surface.
        expectLt(Math.abs(blob.evaluateField(p)), 1.0, String(v));
    }
});

// A solid larger than the region is closed by the region's sides.
test("SolidBoundaryTessellatorContract_RegionClosesASolidThatLeavesIt", () => {
    const ball = new SimpleBody();
    ball.setGeometry(new Sphere(3.0));
    const region = new GeometryBounds();
    expectTrue(region.setFinite(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1)));
    const report = new SolidBoundaryTessellationReport();
    const mesh = SolidBoundaryTessellator.tessellate(ball, region, 8, report);
    expectNe(null, mesh);
    expectTrue(TessellationMeshCheck.isClosedAndOriented(mesh!));
    expectNear(8.0, TessellationMeshCheck.enclosedVolume(mesh!), 1e-9);
    expectGt(report.regionCrossings, 0);
    expectEq(0, report.exactCrossings);
});

test("SolidBoundaryTessellatorContract_InvalidRequestsAndEmptyResults", () => {
    const ball = new SimpleBody();
    ball.setGeometry(new Sphere(1.0));
    const report = new SolidBoundaryTessellationReport();
    const region = new GeometryBounds();
    expectEq(null, SolidBoundaryTessellator.tessellate(ball, region, 8, report));
    region.setUnbounded();
    expectEq(null, SolidBoundaryTessellator.tessellate(ball, region, 8, report));
    expectTrue(region.setFinite(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1)));
    expectEq(null, SolidBoundaryTessellator.tessellate(ball, region, 0, report));
    expectEq(
        null,
        SolidBoundaryTessellator.tessellate(ball, region, SolidBoundaryTessellator.MAXIMUM_RESOLUTION + 1, report),
    );
    // A region away from the solid gives an empty mesh.
    expectTrue(region.setFinite(new Vector3Dd(5, 5, 5), new Vector3Dd(6, 6, 6)));
    const mesh = SolidBoundaryTessellator.tessellate(ball, region, 8, report);
    expectNe(null, mesh);
    expectEq(0, mesh!.getNumTriangles());
});

// The patch mesh samples the Bezier surface and faces its u x v side.
test("BezierPatchTessellatorContract_VerticesLieOnTheBezierSurface", () => {
    const net: Vector3Dd[][] = [];
    for (let i = 0; i < 4; i++) {
        net.push([]);
        for (let j = 0; j < 4; j++) {
            net[i]!.push(new Vector3Dd(i, j, (i === 1 || i === 2) && (j === 1 || j === 2) ? 0.6 : 0.2 * i));
        }
    }
    const patch = new SubdividedBezierPatch(BezierPatchSubdivision.UniformGrid, 4, 4, 0.0, net);
    const mesh = BezierPatchTessellator.tessellate(patch, 6, 9);
    expectNe(null, mesh);
    expectEq(7 * 10, mesh!.getNumVertices());
    expectEq(2 * 6 * 9, mesh!.getNumTriangles());
    const uvs = mesh!.getVertexUvs()!;
    const faceNormals = mesh!.getTriangleNormals()!;
    const indices = mesh!.getTriangleIndexes()!;
    for (let v = 0; v < mesh!.getNumVertices(); v++) {
        const expected = patch.evaluate(uvs[v * 2]!, uvs[v * 2 + 1]!);
        expectLt(TessellationMeshCheck.vertex(mesh!, v).subtract(expected).length(), 1e-12);
    }
    for (let t = 0; t < mesh!.getNumTriangles(); t++) {
        let u = 0;
        let v = 0;
        for (let k = 0; k < 3; k++) {
            u += uvs[indices[t * 3 + k]! * 2]! / 3.0;
            v += uvs[indices[t * 3 + k]! * 2 + 1]! / 3.0;
        }
        const n = new Vector3Dd(faceNormals[t * 3]!, faceNormals[t * 3 + 1]!, faceNormals[t * 3 + 2]!);
        expectGt(n.dotProduct(BezierPatchTessellator.surfaceNormal(patch, u, v)), 0.9, String(t));
    }
    // The vertex normals are S_u x S_v, here +z at the center of the flat-sided net.
    const center = BezierPatchTessellator.surfaceNormal(patch, 0.5, 0.5);
    expectGt(center.z(), 0.9);
    expectEq(null, BezierPatchTessellator.tessellate(patch, 0, 3));
    expectEq(null, BezierPatchTessellator.tessellate(patch, 3, BezierPatchTessellator.MAXIMUM_STEPS + 1));
});
