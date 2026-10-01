import { describe, expect, it } from "vitest";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { SpherePolyhedralCache, type SpherePolyhedralCacheEntry } from "vsdk/toolkit/render/SpherePolyhedralCache.js";

/**
Exercises the conversion of a `Sphere` into the polyhedral bounded solid (and
the triangle attributes) shared by the sphere renderers.
*/
describe("SpherePolyhedralCache", () => {
    it("given a sphere when obtaining its tessellation then every face is an outward triangle on the sphere", () => {
        // Arrange
        const sphere: Sphere = new Sphere(2.0);

        // Act
        const entry: SpherePolyhedralCacheEntry = SpherePolyhedralCache.obtain(sphere, 32, 15);

        // Assert
        expect(entry.getVertexCount()).toBe(3 * entry.getSolid().getPolygonsList().size());
        const p: Float32Array = entry.getPositions();
        const n: Float32Array = entry.getNormals();
        for (let i: number = 0; i < entry.getVertexCount(); i++) {
            const length: number = Math.sqrt(p[3*i]!*p[3*i]! + p[3*i+1]!*p[3*i+1]! + p[3*i+2]!*p[3*i+2]!);

            expect(length).toBeCloseTo(2.0, 5);
            // The normal of the parametric surface is the radial direction
            expect(n[3*i]!).toBeCloseTo(p[3*i]! / length, 5);
            expect(n[3*i+1]!).toBeCloseTo(p[3*i+1]! / length, 5);
            expect(n[3*i+2]!).toBeCloseTo(p[3*i+2]! / length, 5);
        }
        for (let t: number = 0; t < entry.getVertexCount(); t += 3) {
            const ux: number = p[3*t+3]! - p[3*t]!;
            const uy: number = p[3*t+4]! - p[3*t+1]!;
            const uz: number = p[3*t+5]! - p[3*t+2]!;
            const vx: number = p[3*t+6]! - p[3*t]!;
            const vy: number = p[3*t+7]! - p[3*t+1]!;
            const vz: number = p[3*t+8]! - p[3*t+2]!;
            const outward: number = (uy*vz - uz*vy) * p[3*t]! + (uz*vx - ux*vz) * p[3*t+1]! +
                (ux*vy - uy*vx) * p[3*t+2]!;

            expect(outward).toBeGreaterThan(0.0);
        }
    });

    it("given a tessellation when reading the texture coordinates of a triangle then they do not jump across the seam", () => {
        // Arrange
        const entry: SpherePolyhedralCacheEntry = SpherePolyhedralCache.obtain(new Sphere(1.0), 16, 8);
        const uv: Float32Array = entry.getUvs();

        // Act / Assert
        for (let t: number = 0; t < entry.getVertexCount(); t += 3) {
            const min: number = Math.min(uv[2*t]!, uv[2*t+2]!, uv[2*t+4]!);
            const max: number = Math.max(uv[2*t]!, uv[2*t+2]!, uv[2*t+4]!);

            expect(max - min).toBeLessThanOrEqual(1.0 / 16 + 1e-5);
        }
    });

    it("given two spheres of the same radius when obtaining their tessellation then the entry is shared", () => {
        // Act
        const a: SpherePolyhedralCacheEntry = SpherePolyhedralCache.obtain(new Sphere(0.5), 20, 10);
        const b: SpherePolyhedralCacheEntry = SpherePolyhedralCache.obtain(new Sphere(0.5), 20, 10);

        // Assert
        expect(a).toBe(b);
        expect(a.getSolid()).toBe(b.getSolid());
    });
});
