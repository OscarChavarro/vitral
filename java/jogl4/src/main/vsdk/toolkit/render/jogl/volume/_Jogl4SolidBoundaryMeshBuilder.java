package vsdk.toolkit.render.jogl.volume;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.volume.ImplicitSolid;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.processing.SolidBoundaryTessellationReport;
import vsdk.toolkit.processing.SolidBoundaryTessellator;
import vsdk.toolkit.render.jogl.Jogl4MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.surface.Jogl4TriangleMeshRenderer;

/**
Builds the GL4 mesh of a solid with no parametric tessellation (quadrics,
implicit solids and height fields) from the `SolidBoundaryTessellator` mesh
of its finite bounds. The unbounded ones are clipped by the cube used for
their boundary representation (see `ImplicitSolid.DEFAULT_CLIPPING_HALF_SIZE`),
where the mesh is closed.
*/
public final class _Jogl4SolidBoundaryMeshBuilder {
    /// Cells along the longest side of the bounds of a tessellated solid
    public static final int RESOLUTION = 64;

    private _Jogl4SolidBoundaryMeshBuilder() {
    }

    /**
    @param geometry solid to tessellate
    @return the mesh of the boundary of the solid, or null if its bounds can
    not be determined or it is empty
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(Geometry geometry)
    {
        GeometryBounds bounds = new GeometryBounds();
        geometry.getBounds(bounds);
        if ( bounds.getKind() == GeometryBounds.Kind.Unbounded ) {
            double c = ImplicitSolid.DEFAULT_CLIPPING_HALF_SIZE;
            bounds.setFinite(new Vector3Dd(-c, -c, -c), new Vector3Dd(c, c, c));
        }
        if ( bounds.getKind() != GeometryBounds.Kind.Finite ) {
            return null;
        }
        SimpleBody body = new SimpleBody();
        // cpp setGeometryReference (borrowed); the same under garbage collection.
        body.setGeometry(geometry);
        SolidBoundaryTessellationReport report = new SolidBoundaryTessellationReport();
        double size = Jogl4MeshBuilder.characteristicSize(new double[] {
            bounds.getMinimum().x(), bounds.getMinimum().y(),
            bounds.getMinimum().z(), bounds.getMaximum().x(),
            bounds.getMaximum().y(), bounds.getMaximum().z()});
        // The margin keeps the extreme points of the solid inside the region
        TriangleMesh mesh = SolidBoundaryTessellator.tessellate(body,
            bounds.inflated(1e-3 * size), RESOLUTION, report);
        if ( mesh == null ) {
            return null;
        }
        return Jogl4TriangleMeshRenderer.buildMesh(mesh);
    }
}
