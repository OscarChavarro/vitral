// Port of cpp/base/src/test/reader_mit_triangles_tests.cpp
import * as fs from "node:fs";
import { test } from "vitest";
import {
    expectDoubleEq,
    expectEq,
    expectFalse,
    expectGe,
    expectGt,
    expectNe,
    expectNear,
    expectThrow,
    expectTrue,
} from "../../../../../../../base/src/test/GTestAssertions.js";
import { Exception } from "@vitral/base/java/lang/Exception";
import { Vector3Dd } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd";
import { GeometryBounds } from "@vitral/base/vsdk/toolkit/environment/geometry/element/GeometryBounds";
import { Ray } from "@vitral/base/vsdk/toolkit/environment/geometry/element/Ray";
import { RayBoundary } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RayBoundary";
import { RayCrossingQuery } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RayCrossingQuery";
import { RayFirstHitStatus } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RayFirstHitStatus";
import { RayHit } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RayHit";
import { RaySolidClassification } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RaySolidClassification";
import { RaySolidQueryStatus } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus";
import { TriangleMesh } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/TriangleMesh";
import { Box } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Box";
import { PlanarHalfSpace } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace";
import { PolynomialSurface } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/PolynomialSurface";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { CsgBody } from "@vitral/base/vsdk/toolkit/environment/scene/csg/CsgBody";
import { CsgBodyEvaluator } from "@vitral/base/vsdk/toolkit/environment/scene/csg/CsgBodyEvaluator";
import { ReaderMitScene } from "../../../../../main/vsdk/toolkit/io/geometry/ReaderMitScene.js";
import { TEMP_DIR, VITRAL_REPOSITORY_ROOT } from "../../../../ReaderTestPaths.js";

test("ReaderMitTriangles_CowImportsFacesMaterialsAndHasRealHits", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/cow.ray", scene);
    const bodies = scene.getSimpleBodies();
    expectEq(6, bodies.size());
    let total = 0;
    for (let i = 0; i < bodies.size(); ++i) {
        const mesh = bodies.get(i).getGeometry();
        expectTrue(mesh instanceof TriangleMesh);
        expectNe(null, bodies.get(i).getMaterial());
        expectGt((mesh as TriangleMesh).getNumTriangles(), 0);
        total += (mesh as TriangleMesh).getNumTriangles();
    }
    expectEq(3640, total);
    const groundGeometry = bodies.get(0).getGeometry();
    expectTrue(groundGeometry instanceof TriangleMesh);
    const ground = groundGeometry as TriangleMesh;
    expectEq(2, ground.getNumTriangles());
    const bounds = new GeometryBounds();
    ground.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectDoubleEq(-10, bounds.getMinimum().x());
    expectDoubleEq(-4, bounds.getMinimum().y());
    expectDoubleEq(10, bounds.getMaximum().z());
    const groundHit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    expectTrue(
        ground.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(0, 0, 0), new Vector3Dd(0, -1, 0)), groundHit),
    );
    expectDoubleEq(-4, groundHit.point.y());

    const cowGeometry = bodies.get(1).getGeometry();
    expectTrue(cowGeometry instanceof TriangleMesh);
    const cow = cowGeometry as TriangleMesh;
    const indices = cow.getTriangleIndexes()!;
    const positions = cow.getVertexPositions()!;
    let found = false;
    for (let i = 0; i < cow.getNumTriangles() && !found; ++i) {
        const a = indices[i * 3]!;
        const b = indices[i * 3 + 1]!;
        const c = indices[i * 3 + 2]!;
        const p = new Vector3Dd(positions[a * 3]!, positions[a * 3 + 1]!, positions[a * 3 + 2]!);
        const q = new Vector3Dd(positions[b * 3]!, positions[b * 3 + 1]!, positions[b * 3 + 2]!);
        const r = new Vector3Dd(positions[c * 3]!, positions[c * 3 + 1]!, positions[c * 3 + 2]!);
        const cross = q.subtract(p).crossProduct(r.subtract(p));
        if (cross.dotProduct(cross) < 1e-8) continue;
        const normal = cross.normalized();
        const center = p
            .add(q)
            .add(r)
            .multiply(1.0 / 3.0);
        const sample = Ray.fromAffine(center.add(normal), normal.multiply(-1));
        const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        found = cow.doIntersectionFirstHit(sample, hit);
        if (found) {
            const typed = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
            const boundary = new RayBoundary();
            expectEq(RayFirstHitStatus.Hit, cow.queryFirstHit(sample, new RayCrossingQuery(0, 10), typed, boundary));
            expectEq(cow, boundary.geometry);
            expectGe(boundary.surfaceIndex, 0);
        }
    }
    expectTrue(found);
});

test("ReaderMitSolids_BoxAndHalfSpaceProduceActualSolidIntervals", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/box_offsets.ray", scene);
    const bodies = scene.getSimpleBodies();
    expectEq(3, bodies.size());
    expectTrue(bodies.get(0).getGeometry() instanceof PlanarHalfSpace);
    expectTrue(bodies.get(1).getGeometry() instanceof Box);
    const result = new RaySolidClassification();
    const boxRay = Ray.fromAffine(new Vector3Dd(-2, 0, 0.5), new Vector3Dd(1, 0, 0));
    expectEq(RaySolidQueryStatus.Supported, bodies.get(1).classifyRay(boxRay, new RayCrossingQuery(0, 10), result));
    expectEq(2, result.boundaries.size());
    expectNear(0.8, result.boundaries.get(0).t, 1e-12);
    expectNear(2.1, result.boundaries.get(1).t, 1e-12);
    const groundRay = Ray.fromAffine(new Vector3Dd(0, 0, 2), new Vector3Dd(0, 0, -1));
    expectEq(RaySolidQueryStatus.Supported, bodies.get(0).classifyRay(groundRay, new RayCrossingQuery(0, 10), result));
    expectEq(1, result.boundaries.size());
    expectEq(2, result.insideSegments.size());
    expectDoubleEq(2, result.boundaries.get(0).t);
    expectFalse(result.insideSegments.get(0));
    expectTrue(result.insideSegments.get(1));
});

test("ReaderMitCsg_SculptureBlocksBuildCsgBodiesWithTheirEvaluator", () => {
    const files = [
        VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/csg_sculpture.ray",
        VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/csg_sculpture_pov.ray",
    ];
    for (let i = 0; i < 2; ++i) {
        const scene = new SimpleScene();
        const reader = new ReaderMitScene();
        reader.importEnvironment(files[i]!, scene);
        const bodies = scene.getSimpleBodies();
        expectEq(2, bodies.size());
        const body = bodies.get(1);
        expectTrue(body instanceof CsgBody);
        const csg = body as CsgBody;
        expectEq(i === 0 ? CsgBodyEvaluator.ROTH : CsgBodyEvaluator.CROSSING_MEMBERSHIP, csg.getEvaluator());
        expectEq(3, csg.getNodeCount());
        expectEq(5, csg.getLeafCount());
        expectNe(csg.getLeaf(0)!.getMaterial(), csg.getLeaf(2)!.getMaterial());
        const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        // Down the vertical hole: Roth sees no solid; the POV heuristic keeps the
        // nested union's internal surface of the x-axis cylinder.
        const axisHit = csg.doIntersectionFirstHit(
            Ray.fromAffine(new Vector3Dd(0, 0, 3), new Vector3Dd(0, 0, -1)),
            hit,
        );
        expectEq(i === 1, axisHit);
        if (axisHit) expectNear(3 - (0.8 + Math.sqrt(0.24)), hit.getHitDistance(), 1e-12);
        hit.reset(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
        expectTrue(csg.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(0.6, 0, 3), new Vector3Dd(0, 0, -1)), hit));
        expectNear(1.45, hit.getHitDistance(), 1e-12);
        expectNear(1, hit.normal.z(), 1e-12);
        expectEq(csg.getLeaf(0)!.getMaterial(), hit.material);
    }
});

test("ReaderMitCsg_MalformedBlocksAreRejected", () => {
    const bodies = [
        "csg union\nsphere 0 0 0 1\n",
        "csg xor\nsphere 0 0 0 1\nend\n",
        "csg union roth\ncsg intersection pov\nend\nend\n",
        "csg union\ntriangles\nend\nend\n",
    ];
    const path = TEMP_DIR + "reader_mit_csg_malformed.ray";
    for (let i = 0; i < 4; ++i) {
        fs.writeFileSync(path, bodies[i]!);
        const scene = new SimpleScene();
        const reader = new ReaderMitScene();
        expectThrow(Exception, () => reader.importEnvironment(path, scene), String(i));
    }
    fs.rmSync(path);
});

test("ReaderMitPrimitives_PolySceneBuildsPlacedPolynomialBodies", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/poly_surfaces.ray", scene);
    const bodies = scene.getSimpleBodies();
    expectEq(3, bodies.size());
    const torusGeometry = bodies.get(1).getGeometry();
    expectTrue(torusGeometry instanceof PolynomialSurface);
    const torus = torusGeometry as PolynomialSurface;
    expectEq(4, torus.getOrder());
    expectTrue(torus.usesSturmSequences());
    expectDoubleEq(-3.2, torus.getCoefficients()[PolynomialSurface.getTermIndex(4, 2, 0, 0)]!);
    expectDoubleEq(-1.7, bodies.get(1).getAffinePlacement().get(0, 3));
    // The tilted torus is hit on its outer equator from -x.
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    expectTrue(
        bodies.get(1).doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-6, 0, 0.5), new Vector3Dd(1, 0, 0)), hit),
    );
    expectNear(6 - 1.7 - 1.6, hit.getHitDistance(), 1e-9);
    expectNear(-1, hit.normal.x(), 1e-9);
    const saddleBody = bodies.get(2);
    expectTrue(saddleBody instanceof CsgBody);
    const saddle = saddleBody as CsgBody;
    expectEq(2, saddle.getLeafCount());
    expectTrue(saddle.getLeaf(0)!.getGeometry() instanceof PolynomialSurface);
    // Below the saddle (z < 0 on its x = 0 line) the cut sphere is solid.
    hit.reset(RayHit.DETAIL_POINT);
    expectTrue(
        saddle.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(1.7, -5, -0.3), new Vector3Dd(0, 1, 0)), hit),
    );
    expectNear(5 - Math.sqrt(1.21 - 0.25), hit.getHitDistance(), 1e-9);
});

test("ReaderMitPrimitives_MalformedPolyAndTransformLinesAreRejected", () => {
    const bodies = [
        "poly 8\nend\n",
        "poly 3\nterm 1 2 2 0\nend\n",
        "poly 2\nterm 1 2 0 0\n",
        "poly 2 fast\nend\n",
        "poly 2\nsphere 0 0 0 1\nend\n",
        "transform 1 0 0 0 0 1 0 0 0 0 0 0\nsphere 0 0 0 1\n",
        "transform 1 0 0 0 0 1 0 0 0 0 1 0\n",
    ];
    const path = TEMP_DIR + "reader_mit_poly_malformed.ray";
    for (let i = 0; i < 7; ++i) {
        fs.writeFileSync(path, bodies[i]!);
        const scene = new SimpleScene();
        const reader = new ReaderMitScene();
        expectThrow(Exception, () => reader.importEnvironment(path, scene), String(i));
    }
    fs.rmSync(path);
});
