import { Geometry } from "../Geometry.js";
import { FunctionalExplicitSurface } from "../surface/FunctionalExplicitSurface.js";
import { QuadMesh } from "../surface/QuadMesh.js";
import { TriangleMesh } from "../surface/TriangleMesh.js";
import { TriangleMeshGroup } from "../surface/TriangleMeshGroup.js";
import { TriangleStripMesh } from "../surface/TriangleStripMesh.js";

export class GeometryTriangulator {
    public static exportToTriangleMeshGroup(geometry: Geometry | null): TriangleMeshGroup | null {
        if (geometry === null) return null;
        if (geometry instanceof TriangleMeshGroup) return geometry;
        const group = new TriangleMeshGroup();
        if (geometry instanceof TriangleMesh) group.addMesh(geometry);
        else if (geometry instanceof QuadMesh) return geometry.exportToTriangleMeshGroup();
        else if (geometry instanceof TriangleStripMesh) {
            const mesh = geometry.exportToTriangleMesh();
            if (mesh === null) {
                return null;
            }
            group.addMesh(mesh);
        } else if (geometry instanceof FunctionalExplicitSurface) group.addMesh(geometry.getInternalTriangleMesh());
        else return null;
        return group;
    }
}
