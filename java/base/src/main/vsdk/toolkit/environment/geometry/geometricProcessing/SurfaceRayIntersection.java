package vsdk.toolkit.environment.geometry.geometricProcessing;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.Box;

public class SurfaceRayIntersection {
    public static boolean doIntersection(Geometry geometry, Ray inRay, RayHit outHit)
    {
        if ( geometry == null ) {
            return false;
        }

        if ( geometry instanceof TriangleMesh ||
             geometry instanceof TriangleMeshGroup ||
             geometry instanceof FunctionalExplicitSurface ) {
            if ( !rayIntersectsGeometryBounds(geometry, inRay) ) {
                return false;
            }
        }

        return geometry.doIntersection(inRay, outHit);
    }

    private static boolean rayIntersectsGeometryBounds(Geometry geometry, Ray inRay)
    {
        double[] mm = geometry.getMinMax();
        Vector3Dd size = new Vector3Dd(mm[3]-mm[0], mm[4]-mm[1], mm[5]-mm[2]);
        Vector3Dd center = new Vector3Dd(
            (mm[3]+mm[0])/2,
            (mm[4]+mm[1])/2,
            (mm[5]+mm[2])/2
        );
        Box boundingVolume = new Box(size);
        Ray localRay = new Ray(
            inRay.origin().subtract(center),
            inRay.direction(),
            inRay.t()
        );
        return boundingVolume.doIntersection(localRay, null);
    }
}
