import {
    Camera,
    Matrix4x4d,
    PolyhedralBoundedSolidNumericPolicy,
    Ray,
    VSDK,
    Vector3Dd,
    Vector4Dd,
    type PolyhedralBoundedSolid,
    type ToleranceContext,
    type _PolyhedralBoundedSolidFace,
    type _PolyhedralBoundedSolidHalfEdge,
    type _PolyhedralBoundedSolidLoop,
    type _PolyhedralBoundedSolidVertex,
} from "@vitral/base";

/** Java's private nested `VertexLabelGroup`. */
class VertexLabelGroup {
    public readonly vertices: _PolyhedralBoundedSolidVertex[] = [];
    private readonly projectedPositions: Vector3Dd[] = [];

    public constructor(
        vertex: _PolyhedralBoundedSolidVertex,
        public readonly projectedPosition: Vector3Dd,
    ) {
        this.add(vertex, projectedPosition);
    }

    public add(vertex: _PolyhedralBoundedSolidVertex, projectedPosition: Vector3Dd): void {
        this.vertices.push(vertex);
        this.projectedPositions.push(projectedPosition);
    }

    public containsCloseVertex(
        vertex: _PolyhedralBoundedSolidVertex,
        projectedPosition: Vector3Dd,
        spatialTolerance: number,
    ): boolean {
        const spatialToleranceSquared: number = spatialTolerance * spatialTolerance;
        const viewportToleranceSquared: number =
            WebGLPolyhedralBoundedSolidDebugHUDRenderer.VERTEX_LABEL_GROUPING_PIXELS *
            WebGLPolyhedralBoundedSolidDebugHUDRenderer.VERTEX_LABEL_GROUPING_PIXELS;

        for (let i = 0; i < this.vertices.length; i++) {
            if (
                WebGLPolyhedralBoundedSolidDebugHUDRenderer.distanceSquared3D(
                    this.vertices[i]!.position,
                    vertex.position,
                ) <= spatialToleranceSquared
            ) {
                return true;
            }
            if (
                WebGLPolyhedralBoundedSolidDebugHUDRenderer.distanceSquared2D(
                    this.projectedPositions[i]!,
                    projectedPosition,
                ) <= viewportToleranceSquared
            ) {
                return true;
            }
        }
        return false;
    }
}

/**
Port of
`vsdk.toolkit.render.jogl.polyhedralBoundedSolid.Jogl4PolyhedralBoundedSolidDebugHUDRenderer`.

The two label layers of the debugger HUD, computed as Java computes them: the
id of the selected face in cyan at the mean of its projected vertices, and the
ids of the solid's vertices in white, grouped when they lie within the numeric
policy's big epsilon in space or within eighteen pixels on screen, and shown
only for the vertices a ray from the eye reaches before it reaches the solid.

The one boundary is the drawing surface. Java draws into the `Graphics2D` of
the HUD's `BufferedImage`; here the HUD draws into the 2D context of an
offscreen `<canvas>`, so that is what these methods take. `setColor` becomes
`fillStyle` and `drawString(s, x, y)` becomes `fillText(s, x, y)`, both of which
place the text by its baseline; the font is whatever the caller set, as in
Java.
*/
export class WebGLPolyhedralBoundedSolidDebugHUDRenderer {
    public static readonly VERTEX_LABEL_GROUPING_PIXELS = 18.0;
    private static readonly SCREEN_DISTANCE_DELTA = 1.0;

    private constructor() {}

    public static drawSelectedFaceLabel(
        g: CanvasRenderingContext2D | null,
        solid: PolyhedralBoundedSolid | null,
        faceIndex: number,
        camera: Camera | null,
        viewportWidth: number,
        viewportHeight: number,
    ): void {
        if (
            g === null ||
            solid === null ||
            solid.getPolygonsList() === null ||
            faceIndex < 0 ||
            faceIndex >= solid.getPolygonsList().size()
        ) {
            return;
        }

        const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(faceIndex)!;
        const projectedVertices: Vector3Dd[] = WebGLPolyhedralBoundedSolidDebugHUDRenderer.collectProjectedFaceVertices(
            face,
            camera,
            viewportWidth,
            viewportHeight,
        );
        if (projectedVertices.length === 0) {
            return;
        }

        const projectedMidpoint: Vector3Dd =
            WebGLPolyhedralBoundedSolidDebugHUDRenderer.averageProjectedPosition(projectedVertices);
        g.fillStyle = "rgb(0, 255, 255)";
        g.fillText(
            String(face.id),
            WebGLPolyhedralBoundedSolidDebugHUDRenderer.javaRound(projectedMidpoint.x()),
            WebGLPolyhedralBoundedSolidDebugHUDRenderer.javaRound(projectedMidpoint.y()),
        );
    }

    public static drawDebugVertexLabels(
        g: CanvasRenderingContext2D | null,
        solid: PolyhedralBoundedSolid | null,
        camera: Camera | null,
        viewportWidth: number,
        viewportHeight: number,
    ): void {
        if (g === null || solid === null || solid.getVerticesList() === null) {
            return;
        }

        const vertexGroups: VertexLabelGroup[] = WebGLPolyhedralBoundedSolidDebugHUDRenderer.buildVertexGroups(
            solid,
            camera,
            viewportWidth,
            viewportHeight,
        );

        for (let i = 0; i < vertexGroups.length; i++) {
            const group: VertexLabelGroup = vertexGroups[i]!;
            const visibleVertices: _PolyhedralBoundedSolidVertex[] =
                WebGLPolyhedralBoundedSolidDebugHUDRenderer.filterVisibleVertices(group.vertices, solid, camera);

            if (visibleVertices.length !== 0) {
                g.fillStyle = "rgb(255, 255, 255)";
                g.fillText(
                    WebGLPolyhedralBoundedSolidDebugHUDRenderer.buildVertexIdsLabel(visibleVertices),
                    WebGLPolyhedralBoundedSolidDebugHUDRenderer.javaRound(group.projectedPosition.x()) + 4,
                    WebGLPolyhedralBoundedSolidDebugHUDRenderer.javaRound(group.projectedPosition.y()) + 4,
                );
            }
        }
    }

    private static buildVertexGroups(
        solid: PolyhedralBoundedSolid,
        camera: Camera | null,
        viewportWidth: number,
        viewportHeight: number,
    ): VertexLabelGroup[] {
        const vertexGroups: VertexLabelGroup[] = [];
        const numericContext: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        const spatialTolerance: number =
            numericContext.bigEpsilon() * WebGLPolyhedralBoundedSolidDebugHUDRenderer.SCREEN_DISTANCE_DELTA;

        for (let i = 0; i < solid.getVerticesList().size(); i++) {
            const vertex: _PolyhedralBoundedSolidVertex = solid.getVerticesList().get(i)!;
            const projectedPosition: Vector3Dd | null =
                WebGLPolyhedralBoundedSolidDebugHUDRenderer.projectVertexToViewport(
                    vertex.position,
                    camera,
                    viewportWidth,
                    viewportHeight,
                );

            if (projectedPosition === null) {
                continue;
            }
            const group: VertexLabelGroup | null = WebGLPolyhedralBoundedSolidDebugHUDRenderer.findVertexGroup(
                vertexGroups,
                vertex,
                projectedPosition,
                spatialTolerance,
            );
            if (group === null) {
                vertexGroups.push(new VertexLabelGroup(vertex, projectedPosition));
            } else {
                group.add(vertex, projectedPosition);
            }
        }
        return vertexGroups;
    }

    private static findVertexGroup(
        vertexGroups: VertexLabelGroup[],
        vertex: _PolyhedralBoundedSolidVertex,
        projectedPosition: Vector3Dd,
        spatialTolerance: number,
    ): VertexLabelGroup | null {
        for (let i = 0; i < vertexGroups.length; i++) {
            const group: VertexLabelGroup = vertexGroups[i]!;
            if (group.containsCloseVertex(vertex, projectedPosition, spatialTolerance)) {
                return group;
            }
        }
        return null;
    }

    private static collectProjectedFaceVertices(
        face: _PolyhedralBoundedSolidFace,
        camera: Camera | null,
        viewportWidth: number,
        viewportHeight: number,
    ): Vector3Dd[] {
        const projected: Vector3Dd[] = [];
        // Java's LinkedHashSet<Integer>: only membership is observed.
        const visitedVertexIds = new Set<number>();

        for (let i = 0; i < face.boundariesList.size(); i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
            if (loop === null || loop.boundaryStartHalfEdge === null) {
                continue;
            }

            const start: _PolyhedralBoundedSolidHalfEdge = loop.boundaryStartHalfEdge;
            let he: _PolyhedralBoundedSolidHalfEdge | null = start;
            do {
                const vertex: _PolyhedralBoundedSolidVertex | null = he!.startingVertex;
                if (vertex !== null && !visitedVertexIds.has(vertex.id)) {
                    visitedVertexIds.add(vertex.id);
                    if (vertex.position !== null) {
                        const projectedVertex: Vector3Dd | null =
                            WebGLPolyhedralBoundedSolidDebugHUDRenderer.projectVertexToViewport(
                                vertex.position,
                                camera,
                                viewportWidth,
                                viewportHeight,
                            );
                        if (projectedVertex !== null) {
                            projected.push(projectedVertex);
                        }
                    }
                }
                he = he!.next();
            } while (he !== start);
        }
        return projected;
    }

    private static projectVertexToViewport(
        worldPosition: Vector3Dd | null,
        camera: Camera | null,
        viewportWidth: number,
        viewportHeight: number,
    ): Vector3Dd | null {
        if (worldPosition === null || camera === null) {
            return null;
        }

        const projection: Matrix4x4d = camera.calculateProjectionMatrix();
        const clip: Vector4Dd = projection.multiply(
            new Vector4Dd(worldPosition.x(), worldPosition.y(), worldPosition.z(), 1.0),
        );
        if (Math.abs(clip.w()) <= VSDK.EPSILON) {
            return null;
        }

        const ndcX: number = clip.x() / clip.w();
        const ndcY: number = clip.y() / clip.w();
        const ndcZ: number = clip.z() / clip.w();
        if (ndcX < -1.0 || ndcX > 1.0 || ndcY < -1.0 || ndcY > 1.0 || ndcZ < -1.0 || ndcZ > 1.0) {
            return null;
        }

        const width: number = Math.max(1, viewportWidth);
        const height: number = Math.max(1, viewportHeight);
        const x: number = (ndcX + 1.0) * 0.5 * width;
        const y: number = height - (ndcY + 1.0) * 0.5 * height;
        const z: number = (ndcZ + 1.0) * 0.5;
        return new Vector3Dd(x, y, z);
    }

    private static filterVisibleVertices(
        vertices: _PolyhedralBoundedSolidVertex[],
        solid: PolyhedralBoundedSolid,
        camera: Camera | null,
    ): _PolyhedralBoundedSolidVertex[] {
        const visibleVertices: _PolyhedralBoundedSolidVertex[] = [];

        for (let i = 0; i < vertices.length; i++) {
            const vertex: _PolyhedralBoundedSolidVertex = vertices[i]!;
            const isVisible: boolean = WebGLPolyhedralBoundedSolidDebugHUDRenderer.isVertexLabelVisible(
                vertex,
                solid,
                camera,
            );
            if (isVisible) {
                visibleVertices.push(vertex);
            }
        }
        return visibleVertices;
    }

    private static isVertexLabelVisible(
        vertex: _PolyhedralBoundedSolidVertex | null,
        solid: PolyhedralBoundedSolid,
        camera: Camera | null,
    ): boolean {
        if (vertex === null || vertex.position === null || camera === null) {
            return true;
        }

        const visibilityRay = new Ray(camera.getPosition(), vertex.position.subtract(camera.getPosition()));
        const vertexRayT: number = vertex.position
            .subtract(visibilityRay.getOrigin())
            .dotProduct(visibilityRay.getDirection());
        if (vertexRayT <= VSDK.EPSILON) {
            return true;
        }

        const numericContext: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        const closestPointOnRay: Vector3Dd = visibilityRay
            .getOrigin()
            .add(visibilityRay.getDirection().multiply(vertexRayT));
        if (closestPointOnRay.subtract(vertex.position).length() >= numericContext.bigEpsilon()) {
            return true;
        }

        const hit: Ray | null = solid.doIntersectionFirstHit(visibilityRay);
        if (hit === null) {
            return true;
        }

        return !(vertexRayT - hit.getT() >= numericContext.bigEpsilon());
    }

    private static averageProjectedPosition(projectedVertices: Vector3Dd[]): Vector3Dd {
        let sx = 0.0;
        let sy = 0.0;
        let sz = 0.0;

        for (let i = 0; i < projectedVertices.length; i++) {
            const p: Vector3Dd = projectedVertices[i]!;
            sx += p.x();
            sy += p.y();
            sz += p.z();
        }

        const n: number = projectedVertices.length;
        return new Vector3Dd(sx / n, sy / n, sz / n);
    }

    public static distanceSquared3D(a: Vector3Dd, b: Vector3Dd): number {
        const dx: number = a.x() - b.x();
        const dy: number = a.y() - b.y();
        const dz: number = a.z() - b.z();

        return dx * dx + dy * dy + dz * dz;
    }

    public static distanceSquared2D(a: Vector3Dd, b: Vector3Dd): number {
        const dx: number = a.x() - b.x();
        const dy: number = a.y() - b.y();

        return dx * dx + dy * dy;
    }

    private static buildVertexIdsLabel(vertices: _PolyhedralBoundedSolidVertex[]): string {
        let label = "";

        for (let i = 0; i < vertices.length; i++) {
            if (i > 0) {
                label += ", ";
            }
            label += vertices[i]!.id;
        }
        return label;
    }

    /** `(int)Math.round(double)`: rounds half up, as `Math.round` does. */
    private static javaRound(value: number): number {
        return Math.floor(value + 0.5) | 0;
    }
}
