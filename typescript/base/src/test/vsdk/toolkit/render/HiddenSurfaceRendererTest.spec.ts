// Port of java/base/src/test/vsdk/toolkit/render/HiddenSurfaceRendererTest.java
import { describe, expect, it } from "vitest";

import { ArrayList } from "java/util/ArrayList.js";
import { Random } from "java/util/Random.js";
import { Double } from "java/lang/Double.js";
import { Math as JavaMath } from "java/lang/Math.js";

import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "vsdk/toolkit/common/linealAlgebra/Vector4Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import type { Vertex2D } from "vsdk/toolkit/environment/geometry/element/Vertex2D.js";
import type { Polygon2D } from "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { AmbientLight } from "vsdk/toolkit/environment/light/AmbientLight.js";
import { DirectionalLight } from "vsdk/toolkit/environment/light/DirectionalLight.js";
import type { Light } from "vsdk/toolkit/environment/light/Light.js";
import { PointLight } from "vsdk/toolkit/environment/light/PointLight.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { HiddenSurfaceRenderer } from "vsdk/toolkit/render/hiddenSurface/HiddenSurfaceRenderer.js";

type VisiblePolygon = HiddenSurfaceRenderer.VisiblePolygon;

/**
Checks the hidden surface removal of [WEIL1977] against ray casting: at
sample points of the view, the visible polygon covering the point must
belong to the body first hit by the ray of that point, and the visible
polygons must not overlap. With shadows ([WEIL1977].218), the lights given
for that polygon must be the ones whose shadow rays from the hit point reach
them unblocked.
*/
describe("HiddenSurfaceRendererTest", () => {
    const VIEW_SIZE = 64;
    /// Samples closer than this (in normalized device coordinates) to an
    /// edge of the output are ambiguous and are not checked
    const EDGE_TOLERANCE = 1.0e-3;

    it("given_boxPartiallyInFrontOfAnother_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting", () => {
        const bodies = new ArrayList<SimpleBody>();
        bodies.add(createBox(new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(1.0, 1.0, 1.0)));
        bodies.add(createBox(new Vector3Dd(0.4, 0.5, 0.3), new Vector3Dd(1.0, 1.0, 1.0)));

        assertVisibilityMatchesRayCasting(bodies, createCamera(), 0);
    });

    it("given_penetratingBoxes_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting", () => {
        const bodies = new ArrayList<SimpleBody>();
        bodies.add(createBox(new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(1.6, 0.4, 0.4)));
        bodies.add(createBox(new Vector3Dd(0.1, 0.1, 0.0), new Vector3Dd(0.4, 1.6, 0.6)));

        assertVisibilityMatchesRayCasting(bodies, createCamera(), 0);
    });

    it("given_cyclicallyOverlappingBars_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting", () => {
        // Three bars, each one over the next one at one end and under it at
        // the other ([WEIL1977].216, figure 4b)
        const bodies = new ArrayList<SimpleBody>();
        bodies.add(createBox(new Vector3Dd(0.0, -0.6, 0.0), new Vector3Dd(1.8, 0.3, 0.1)));
        bodies.add(createRotatedBox(new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(1.8, 0.3, 0.1), 120.0, 0.12));
        bodies.add(createRotatedBox(new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(1.8, 0.3, 0.1), 240.0, -0.12));

        const camera = new Camera();
        camera.setPosition(new Vector3Dd(0.0, 0.0, 5.0));
        camera.setRotation(new Matrix4x4d().eulerAnglesRotation(0.0, JavaMath.toRadians(-90.0), 0.0));
        camera.updateVectors();
        camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);

        assertVisibilityMatchesRayCasting(bodies, camera, 0);
    });

    it("given_randomPenetratingBoxesAndSpheres_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting", () => {
        // Many faces sharing edges and vertices, and pieces inscribed in
        // their clip polygons: the degenerate cases of the polygon clipper
        const bodies: ArrayList<SimpleBody> = createRandomScene(new Random(1), 8);

        assertVisibilityMatchesRayCasting(bodies, createCamera(), 0, 2);
    }, 120_000);

    it("given_randomSceneSeenByOrthogonalCamera_when_removingHiddenSurfaces_then_visibleBodiesMatchRayCasting", () => {
        const bodies: ArrayList<SimpleBody> = createRandomScene(new Random(2), 8);
        const camera: Camera = createCamera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);

        assertVisibilityMatchesRayCasting(bodies, camera, 0, 2);
    }, 120_000);

    it("given_boxOverFloorAndPointLight_when_castingShadows_then_litAreasMatchShadowRays", () => {
        const bodies = new ArrayList<SimpleBody>();
        bodies.add(createBox(new Vector3Dd(0.0, 0.0, -0.6), new Vector3Dd(3.0, 3.0, 0.2)));
        bodies.add(createBox(new Vector3Dd(0.2, 0.1, 0.1), new Vector3Dd(0.6, 0.6, 0.6)));
        const lights = new ArrayList<Light>();
        lights.add(new AmbientLight(new ColorRgb(0.1, 0.1, 0.1)));
        lights.add(new PointLight(new Vector3Dd(-1.5, -1.0, 3.0), new ColorRgb(1, 1, 1)));

        assertShadowsMatchRayCasting(bodies, createCamera(), lights, 1);
    });

    it("given_lightAmongSolidsAndDirectionalLight_when_castingShadows_then_litAreasMatchShadowRays", () => {
        // The first light is inside the bounding sphere of the scene, so it
        // is seen with the six views of a cube
        const bodies: ArrayList<SimpleBody> = createRandomScene(new Random(3), 6);
        bodies.add(createBox(new Vector3Dd(0.0, 0.0, -1.6), new Vector3Dd(4.0, 4.0, 0.2)));
        const lights = new ArrayList<Light>();
        lights.add(new PointLight(new Vector3Dd(0.3, -0.2, 1.4), new ColorRgb(1, 1, 1)));
        lights.add(new DirectionalLight(new Vector3Dd(0.4, 0.3, -1.0), new ColorRgb(1, 1, 1)));

        assertShadowsMatchRayCasting(bodies, createCamera(), lights, 3);
    }, 120_000);

    function createRandomScene(random: Random, count: number): ArrayList<SimpleBody> {
        const bodies = new ArrayList<SimpleBody>();
        for (let i = 0; i < count; i++) {
            const body = new SimpleBody();
            if (i % 2 === 0) {
                body.setGeometry(new Sphere(0.3 + 0.4 * random.nextDouble()).exportToPolyhedralBoundedSolid());
            } else {
                body.setGeometry(
                    new Box(
                        new Vector3Dd(0.2 + random.nextDouble(), 0.2 + random.nextDouble(), 0.2 + random.nextDouble()),
                    ).exportToPolyhedralBoundedSolid(),
                );
            }
            const rotation: Matrix4x4d = new Matrix4x4d().eulerAnglesRotation(
                random.nextDouble() * 6,
                random.nextDouble() * 6,
                random.nextDouble() * 6,
            );
            body.setPosition(
                new Vector3Dd(random.nextGaussian() * 0.7, random.nextGaussian() * 0.7, random.nextGaussian() * 0.7),
            );
            body.setRotation(rotation);
            body.setRotationInverse(rotation.inverse());
            bodies.add(body);
        }
        return bodies;
    }

    /**
    @param stride distance, in pixels, between the sampled pixels (the ray
    casting of spheres is slow)
    */
    function assertVisibilityMatchesRayCasting(
        bodies: ArrayList<SimpleBody>,
        camera: Camera,
        allowedMismatches: number,
        stride = 1,
    ): void {
        const visible: ArrayList<VisiblePolygon> = HiddenSurfaceRenderer.executeWeilerAthertonAlgorithm(bodies, camera);
        const projection: Matrix4x4d = camera.calculateProjectionMatrix();
        let checked = 0;
        let mismatches = 0;
        let overlaps = 0;

        expect(visible.isEmpty()).toBe(false);
        for (let y = 0; y < VIEW_SIZE; y += stride) {
            for (let x = 0; x < VIEW_SIZE; x += stride) {
                const ray: Ray = camera.generateRay(x, y);
                let nearest: SimpleBody | null = null;
                let nearestPoint: Vector3Dd | null = null;
                let nearestT: number = Double.MAX_VALUE;
                for (const body of bodies) {
                    const hit = new RayHit(RayHit.DETAIL_POINT, true);
                    if (
                        body.doIntersectionFirstHit(new Ray(ray), hit) &&
                        hit.getRay() !== null &&
                        hit.getRay()!.getT() < nearestT
                    ) {
                        nearestT = hit.getRay()!.getT();
                        nearest = body;
                        nearestPoint = ray.getOrigin().add(ray.getDirection().multiply(nearestT));
                    }
                }
                if (nearest === null) {
                    continue;
                }
                const ndc: Vector4Dd = projection.multiply(new Vector4Dd(nearestPoint!)).dividedByW();
                if (isNearAnEdge(visible, ndc.x(), ndc.y())) {
                    continue;
                }
                const covering = new ArrayList<VisiblePolygon>();
                for (const polygon of visible) {
                    if (contains(polygon.area, ndc.x(), ndc.y())) {
                        covering.add(polygon);
                    }
                }
                checked++;
                if (covering.size() > 1) {
                    overlaps++;
                }
                if (covering.size() !== 1 || covering.get(0).body !== nearest) {
                    mismatches++;
                }
            }
        }
        expect(checked).toBeGreaterThan(100);
        expect(overlaps).toBe(0);
        expect(mismatches).toBeLessThanOrEqual(allowedMismatches);
    }

    /**
    @param stride distance, in pixels, between the sampled pixels
    */
    function assertShadowsMatchRayCasting(
        bodies: ArrayList<SimpleBody>,
        camera: Camera,
        lights: ArrayList<Light>,
        stride: number,
    ): void {
        const visible: ArrayList<VisiblePolygon> = HiddenSurfaceRenderer.executeWeilerAthertonAlgorithm(
            bodies,
            camera,
            lights,
        );
        const projection: Matrix4x4d = camera.calculateProjectionMatrix();
        let checked = 0;
        let lit = 0;
        let shadowed = 0;
        let mismatches = 0;

        expect(visible.isEmpty()).toBe(false);
        for (let y = 0; y < VIEW_SIZE; y += stride) {
            for (let x = 0; x < VIEW_SIZE; x += stride) {
                const ray: Ray = camera.generateRay(x, y);
                const point: Vector3Dd | null = firstHit(bodies, ray);
                if (point === null) {
                    continue;
                }
                const ndc: Vector4Dd = projection.multiply(new Vector4Dd(point)).dividedByW();
                if (isNearAnEdge(visible, ndc.x(), ndc.y())) {
                    continue;
                }
                let covering: VisiblePolygon | null = null;
                for (const polygon of visible) {
                    if (contains(polygon.area, ndc.x(), ndc.y())) {
                        covering = polygon;
                    }
                }
                expect(covering).not.toBeNull();
                checked++;
                for (const light of lights) {
                    const expected: boolean = light.isAmbient() || !isInShadow(bodies, point, light);
                    if (expected) {
                        lit++;
                    } else {
                        shadowed++;
                    }
                    if (covering!.lights!.contains(light) !== expected) {
                        mismatches++;
                    }
                }
            }
        }
        expect(checked).toBeGreaterThan(100);
        expect(lit).toBeGreaterThan(20);
        expect(shadowed).toBeGreaterThan(20);
        expect(mismatches).toBe(0);
    }

    function firstHit(bodies: ArrayList<SimpleBody>, ray: Ray): Vector3Dd | null {
        let nearestPoint: Vector3Dd | null = null;
        let nearestT: number = Double.MAX_VALUE;
        for (const body of bodies) {
            const hit = new RayHit(RayHit.DETAIL_POINT, true);
            if (
                body.doIntersectionFirstHit(new Ray(ray), hit) &&
                hit.getRay() !== null &&
                hit.getRay()!.getT() < nearestT
            ) {
                nearestT = hit.getRay()!.getT();
                nearestPoint = ray.getOrigin().add(ray.getDirection().multiply(nearestT));
            }
        }
        return nearestPoint;
    }

    /**
    @return true if the shadow ray from a surface point toward a light is
    blocked by a body (the body of the point included: a point facing away
    from the light is in its own shadow)
    */
    function isInShadow(bodies: ArrayList<SimpleBody>, point: Vector3Dd, light: Light): boolean {
        const toLight: Light.LightDirection = light.getDirectionAndDistance(point);
        const offset = 1.0e-6;
        const shadowRay = new Ray(point.add(toLight.direction().multiply(offset)), toLight.direction());
        for (const body of bodies) {
            const hit = new RayHit(RayHit.DETAIL_POINT, true);
            if (
                body.doIntersectionFirstHit(new Ray(shadowRay), hit) &&
                hit.getRay() !== null &&
                hit.getRay()!.getT() > 0.0 &&
                hit.getRay()!.getT() < toLight.maxShadowDistance() - offset
            ) {
                return true;
            }
        }
        return false;
    }

    function createCamera(): Camera {
        const camera = new Camera();
        camera.setPosition(new Vector3Dd(3.0, -2.0, 2.5));
        camera.setRotation(
            new Matrix4x4d().eulerAnglesRotation(JavaMath.toRadians(146.0), JavaMath.toRadians(-35.0), 0.0),
        );
        camera.updateVectors();
        camera.updateViewportResize(VIEW_SIZE, VIEW_SIZE);
        return camera;
    }

    function createBox(position: Vector3Dd, size: Vector3Dd): SimpleBody {
        return createRotatedBox(position, size, 0.0, 0.0);
    }

    /**
    @param yawDegrees rotation around the z axis
    @param tilt rotation (radians) around the x axis, tilting the bar
    */
    function createRotatedBox(position: Vector3Dd, size: Vector3Dd, yawDegrees: number, tilt: number): SimpleBody {
        const body = new SimpleBody();
        const box = new Box(size);
        const rotation: Matrix4x4d = new Matrix4x4d()
            .axisRotation(JavaMath.toRadians(yawDegrees), 0.0, 0.0, 1.0)
            .multiply(new Matrix4x4d().axisRotation(tilt, 1.0, 0.0, 0.0));
        body.setGeometry(box.exportToPolyhedralBoundedSolid());
        body.setPosition(position);
        body.setRotation(rotation);
        body.setRotationInverse(rotation.inverse());
        return body;
    }

    function contains(polygon: Polygon2D, x: number, y: number): boolean {
        let inside = false;
        for (const loop of polygon.loops) {
            const n: number = loop.vertices.length;
            for (let i = 0, j = n - 1; i < n; j = i++) {
                const a: Vertex2D = loop.vertices[i]!;
                const b: Vertex2D = loop.vertices[j]!;
                if (a.y > y !== b.y > y && x < ((b.x - a.x) * (y - a.y)) / (b.y - a.y) + a.x) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    function isNearAnEdge(visible: ArrayList<VisiblePolygon>, x: number, y: number): boolean {
        for (const polygon of visible) {
            for (const loop of polygon.area.loops) {
                const n: number = loop.vertices.length;
                for (let i = 0; i < n; i++) {
                    const a: Vertex2D = loop.vertices[i]!;
                    const b: Vertex2D = loop.vertices[(i + 1) % n]!;
                    if (distanceToSegment(x, y, a, b) < EDGE_TOLERANCE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    function distanceToSegment(x: number, y: number, a: Vertex2D, b: Vertex2D): number {
        const dx: number = b.x - a.x;
        const dy: number = b.y - a.y;
        const length2: number = dx * dx + dy * dy;
        const t: number =
            length2 > 0.0 ? Math.max(0.0, Math.min(1.0, ((x - a.x) * dx + (y - a.y) * dy) / length2)) : 0.0;
        const px: number = a.x + t * dx - x;
        const py: number = a.y + t * dy - y;
        return Math.sqrt(px * px + py * py);
    }
});
