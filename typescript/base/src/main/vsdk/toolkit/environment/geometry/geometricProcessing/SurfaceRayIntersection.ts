import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../Geometry.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { FunctionalExplicitSurface } from "../surface/FunctionalExplicitSurface.js";
import { TriangleMesh } from "../surface/TriangleMesh.js";
import { TriangleMeshGroup } from "../surface/TriangleMeshGroup.js";
import { Box } from "../volume/Box.js";

export class SurfaceRayIntersection {
    public static doIntersectionFirstHit(geometry: Geometry | null, ray: Ray, out: RayHit): boolean {
        if (geometry === null) return false;
        if (
            (geometry instanceof TriangleMesh ||
                geometry instanceof TriangleMeshGroup ||
                geometry instanceof FunctionalExplicitSurface) &&
            !this.rayIntersectsGeometryBounds(geometry, ray)
        )
            return false;
        return geometry.doIntersectionFirstHit(ray, out);
    }
    private static rayIntersectsGeometryBounds(geometry: Geometry, ray: Ray): boolean {
        const mm = geometry.getMinMax();
        const size = new Vector3Dd(mm[3]! - mm[0]!, mm[4]! - mm[1]!, mm[5]! - mm[2]!);
        const center = new Vector3Dd((mm[3]! + mm[0]!) / 2, (mm[4]! + mm[1]!) / 2, (mm[5]! + mm[2]!) / 2);
        return new Box(size).doIntersectionFirstHit(
            new Ray(ray.getOrigin().subtract(center), ray.getDirection(), ray.getT()),
            new RayHit(),
        );
    }
}
