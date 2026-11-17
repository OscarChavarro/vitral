import {
    GeometryBounds,
    ImplicitSolid,
    SimpleBody,
    SolidBoundaryTessellationReport,
    SolidBoundaryTessellator,
    Vector3Dd,
    type Geometry,
    type TriangleMesh,
} from "@vitral/base";
import { WebGLMeshBuilder } from "../WebGLMeshBuilder.js";
import type { WebGLMesh } from "../WebGLMeshRenderer.js";
import { WebGLTriangleMeshRenderer } from "../surface/WebGLTriangleMeshRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.volume._Jogl4SolidBoundaryMeshBuilder`.

Builds the WebGL mesh of a solid with no parametric tessellation (quadrics,
implicit solids and height fields) from the `SolidBoundaryTessellator` mesh
of its finite bounds. The unbounded ones are clipped by the cube used for
their boundary representation (see `ImplicitSolid.DEFAULT_CLIPPING_HALF_SIZE`),
where the mesh is closed.
*/
export class _WebGLSolidBoundaryMeshBuilder {
    /// Cells along the longest side of the bounds of a tessellated solid
    public static readonly RESOLUTION: number = 64;

    private constructor() {}

    /**
    @param geometry solid to tessellate
    @return the mesh of the boundary of the solid, or null if its bounds can
    not be determined or it is empty
    */
    public static buildMesh(geometry: Geometry): WebGLMesh | null {
        const bounds: GeometryBounds = new GeometryBounds();
        geometry.getBounds(bounds);
        if (bounds.getKind() === GeometryBounds.Kind.Unbounded) {
            const c: number = ImplicitSolid.DEFAULT_CLIPPING_HALF_SIZE;
            bounds.setFinite(new Vector3Dd(-c, -c, -c), new Vector3Dd(c, c, c));
        }
        if (bounds.getKind() !== GeometryBounds.Kind.Finite) {
            return null;
        }
        const body: SimpleBody = new SimpleBody();
        // cpp setGeometryReference (borrowed); the same under garbage collection.
        body.setGeometry(geometry);
        const report: SolidBoundaryTessellationReport = new SolidBoundaryTessellationReport();
        const size: number = WebGLMeshBuilder.characteristicSize([
            bounds.getMinimum().x(),
            bounds.getMinimum().y(),
            bounds.getMinimum().z(),
            bounds.getMaximum().x(),
            bounds.getMaximum().y(),
            bounds.getMaximum().z(),
        ]);
        // The margin keeps the extreme points of the solid inside the region
        const mesh: TriangleMesh | null = SolidBoundaryTessellator.tessellate(
            body,
            bounds.inflated(1e-3 * size),
            _WebGLSolidBoundaryMeshBuilder.RESOLUTION,
            report,
        );
        if (mesh === null) {
            return null;
        }
        return WebGLTriangleMeshRenderer.buildMesh(mesh);
    }
}
