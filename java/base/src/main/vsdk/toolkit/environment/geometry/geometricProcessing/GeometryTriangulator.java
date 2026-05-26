package vsdk.toolkit.environment.geometry.geometricProcessing;

import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;

public class GeometryTriangulator {
    public static TriangleMeshGroup exportToTriangleMeshGroup(Geometry geometry)
    {
        if ( geometry == null ) {
            return null;
        }

        if ( geometry instanceof TriangleMeshGroup ) {
            return (TriangleMeshGroup)geometry;
        }
        if ( geometry instanceof TriangleMesh ) {
            TriangleMeshGroup group = new TriangleMeshGroup();
            group.addMesh((TriangleMesh)geometry);
            return group;
        }
        if ( geometry instanceof QuadMesh ) {
            return ((QuadMesh)geometry).exportToTriangleMeshGroup();
        }
        if ( geometry instanceof FunctionalExplicitSurface ) {
            TriangleMeshGroup group = new TriangleMeshGroup();
            group.addMesh(((FunctionalExplicitSurface)geometry).getInternalTriangleMesh());
            return group;
        }

        return null;
    }
}
