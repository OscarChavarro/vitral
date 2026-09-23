import {
    Camera,
    ColorRgb,
    Light,
    Matrix4x4d,
    MonotoneDecompositionTriangulator,
    Polygon2D,
    RendererConfiguration,
    SimpleMaterial,
    VSDK,
    Vector3Dd,
    Vector4Dd,
    type InfinitePlane,
    type PolyhedralBoundedSolid,
    _PolyhedralBoundedSolidFaceValidator,
    type _PolyhedralBoundedSolidFace,
    type _PolyhedralBoundedSolidHalfEdge,
    type _PolyhedralBoundedSolidLoop,
} from "@vitral/base";
import { WebGLLightRenderer } from "../WebGLLightRenderer.js";
import { WebGLRendererConfigurationShaderSelector } from "../WebGLRendererConfigurationShaderSelector.js";
import { WebGLShaderProgramUtil } from "../WebGLShaderProgramUtil.js";
import { WebGLSimpleMaterialRenderer } from "../WebGLSimpleMaterialRenderer.js";
import { WebGLPolyhedralBoundedSolidDebugRenderer } from "./WebGLPolyhedralBoundedSolidDebugRenderer.js";

interface PolyhedralBoundedSolidRendererResources {
    meshVaoId: WebGLVertexArrayObject;
    meshPositionVboId: WebGLBuffer;
    meshNormalVboId: WebGLBuffer;
    meshUvVboId: WebGLBuffer;
    colorVaoId: WebGLVertexArrayObject;
    colorPositionVboId: WebGLBuffer;
    colorDataVboId: WebGLBuffer;
    colorProgramId: WebGLProgram;
}

/**
Port of
`vsdk.toolkit.render.jogl.polyhedralBoundedSolid.Jogl4PolyhedralBoundedSolidRenderer`.

The surface pass is the Java one: a mesh built every call from the faces of the
solid, skipping the degenerate ones, with vertex normals smoothed across the
faces that meet at a quantized vertex position within the configured angle when
the shading type asks for it; drawn with polygon offset and back-face culling
unless the material is double sided; either through the shader the
configuration selects, with the material and the active lights as uniforms, or,
under flat shading, as per-triangle colours lit on the host by the same
ambient-plus-Phong sum. The debug overlays that follow are
`WebGLPolyhedralBoundedSolidDebugRenderer`'s.

The runtime boundaries are this package's recurring ones — the buffers and the
colour program Java keeps in static fields are kept per context in a `WeakMap`;
a shader source arrives over `fetch`, so every method that can reach a program
is asynchronous; `glGetUniformLocation`'s negative sentinel is `null`; WebGL has
no `glPointSize`, so a point size travels as the `pointSizeLocal` uniform
`WebGLShaderPreprocessor` installs; and the active material and lights are read
for the context being drawn into — plus one that is specific to this renderer:

  - Java triangulates each face with the GLU tessellator, `GLU.gluNewTess()`,
    which a browser cannot reach, as `WebGLPolygon2DRenderer` records. What this
    renderer asks of it is different from what that one asks, though: here the
    triangles carry smoothed vertex normals looked up by the exact position of
    each emitted vertex, so they have to be made of the face's own vertices, as
    GLU's are — GLU hands its `GLU_TESS_VERTEX` callback the very coordinate
    arrays it was given, and a face of a valid solid has no crossing contours
    that would make it synthesize others. `tessellateFace` therefore projects
    the loops onto an orthonormal basis of the face plane, the one
    `_StlFaceTriangulator` builds, triangulates them with the ported
    `MonotoneDecompositionTriangulator`, whose contour-aware pass fills the
    contours of odd nesting depth — the region `GLU_TESS_WINDING_ODD`, the
    tessellator default Java leaves in place, fills — and lifts every triangle
    back to the original 3D vertices. GLU emits triangles wound like the
    contours it was given, which for a B-rep face is counter-clockwise about
    its outward normal; each triangle is wound that way here, against the
    face's containing plane. And where GLU reports `GLU_TESS_ERROR` and the
    collector drops the face, a triangulation that throws drops the face here.
    The triangles themselves are not GLU's; the covered region, the vertices
    and the winding are.
*/
export class WebGLPolyhedralBoundedSolidRenderer {
    private static readonly SURFACE_POLYGON_OFFSET_FACTOR = 2.0;
    private static readonly SURFACE_POLYGON_OFFSET_UNITS = 2.0;
    private static readonly VERTEX_KEY_QUANTIZATION = 1.0e6;

    private static readonly resources = new WeakMap<WebGL2RenderingContext, PolyhedralBoundedSolidRendererResources>();

    private constructor() {}

    /**
    Compiles, ahead of the first frame, the colour program and the surface
    program of every shading type the renderer configuration can select, which
    Java compiles lazily on first use; see the drawing-buffer rule recorded for
    the WebGL example programs.
    */
    public static async prepare(gl: WebGL2RenderingContext): Promise<void> {
        await WebGLPolyhedralBoundedSolidRenderer.ensureInitialized(gl);
        const shadingTypes: readonly number[] = [
            RendererConfiguration.SHADING_TYPE_NOLIGHT,
            RendererConfiguration.SHADING_TYPE_GOURAUD,
            RendererConfiguration.SHADING_TYPE_PHONG,
            RendererConfiguration.SHADING_TYPE_COOK_TERRANCE,
        ];
        for (const shadingType of shadingTypes) {
            const quality = new RendererConfiguration();
            quality.setShadingType(shadingType);
            await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(gl, quality, false, false);
        }
    }

    public static async draw(
        gl: WebGL2RenderingContext | null,
        solid: PolyhedralBoundedSolid | null,
        camera: Camera,
        quality: RendererConfiguration | null,
        modelMatrix: Matrix4x4d | null,
    ): Promise<void> {
        if (gl === null) {
            return;
        }
        const local: Matrix4x4d = modelMatrix !== null ? modelMatrix : Matrix4x4d.identityMatrix();
        const mvp: Matrix4x4d = camera.calculateProjectionMatrix().multiply(local);
        await WebGLPolyhedralBoundedSolidRenderer.drawWithModelViewProjection(gl, solid, camera, quality, local, mvp);
    }

    public static async drawDebugFaceBoundary(
        gl: WebGL2RenderingContext | null,
        solid: PolyhedralBoundedSolid | null,
        faceIndex: number,
        modelViewProjection: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLPolyhedralBoundedSolidDebugRenderer.drawDebugFaceBoundary(gl, solid, faceIndex, modelViewProjection);
    }

    public static async drawDebugFace(
        gl: WebGL2RenderingContext | null,
        solid: PolyhedralBoundedSolid | null,
        faceIndex: number,
        modelMatrix: Matrix4x4d | null,
        modelViewProjection: Matrix4x4d | null,
        camera: Camera | null,
    ): Promise<void> {
        await WebGLPolyhedralBoundedSolidDebugRenderer.drawDebugFace(
            gl,
            solid,
            faceIndex,
            modelMatrix,
            modelViewProjection,
            camera,
        );
    }

    public static async drawDebugEdges(
        gl: WebGL2RenderingContext | null,
        solid: PolyhedralBoundedSolid | null,
        camera: Camera | null,
        edgeIndex: number,
        modelViewProjection: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLPolyhedralBoundedSolidDebugRenderer.drawDebugEdges(
            gl,
            solid,
            camera,
            edgeIndex,
            modelViewProjection,
        );
    }

    /**
    Java's private six-argument `draw` overload, which the public five-argument
    one calls after computing the model-view-projection matrix. TypeScript has
    no overloading on arity with distinct bodies, so it carries its own name.
    */
    private static async drawWithModelViewProjection(
        gl: WebGL2RenderingContext,
        solid: PolyhedralBoundedSolid | null,
        camera: Camera | null,
        quality: RendererConfiguration | null,
        modelMatrix: Matrix4x4d | null,
        modelViewProjection: Matrix4x4d | null,
    ): Promise<void> {
        if (
            solid === null ||
            quality === null ||
            camera === null ||
            modelMatrix === null ||
            modelViewProjection === null
        ) {
            return;
        }

        await WebGLPolyhedralBoundedSolidRenderer.ensureInitialized(gl);

        const modelViewITLocal: Matrix4x4d = modelMatrix.invert().transpose();
        const smoothNormals: boolean =
            quality.getShadingType() !== RendererConfiguration.SHADING_TYPE_FLAT &&
            quality.getShadingType() !== RendererConfiguration.SHADING_TYPE_NOLIGHT;
        const mesh: WebGLPolyhedralBoundedSolidRenderer.MeshData = WebGLPolyhedralBoundedSolidRenderer.buildMesh(
            solid,
            smoothNormals,
            quality,
        );
        const material: SimpleMaterial = WebGLSimpleMaterialRenderer.getActiveMaterial(gl);
        const activeLights: Light[] = WebGLLightRenderer.getActiveLights(gl);
        const cameraPosition: Vector3Dd = camera.getPosition();

        if (quality.isSurfacesSet() && mesh.vertexCount > 0) {
            gl.enable(gl.DEPTH_TEST);
            gl.depthMask(true);
            gl.depthFunc(gl.LESS);
            gl.enable(gl.POLYGON_OFFSET_FILL);
            gl.polygonOffset(
                WebGLPolyhedralBoundedSolidRenderer.SURFACE_POLYGON_OFFSET_FACTOR,
                WebGLPolyhedralBoundedSolidRenderer.SURFACE_POLYGON_OFFSET_UNITS,
            );
            if (material.isDoubleSided()) {
                gl.disable(gl.CULL_FACE);
            } else {
                gl.enable(gl.CULL_FACE);
                gl.cullFace(gl.BACK);
            }

            if (quality.getShadingType() === RendererConfiguration.SHADING_TYPE_FLAT) {
                const flatSurfaces = WebGLPolyhedralBoundedSolidRenderer.buildFlatShadedSurfaceTriangles(
                    mesh,
                    modelMatrix,
                    modelViewITLocal,
                    material,
                    activeLights,
                    cameraPosition,
                );
                if (flatSurfaces.positions.length > 0) {
                    await WebGLPolyhedralBoundedSolidRenderer.drawColoredPrimitives(
                        gl,
                        modelViewProjection,
                        flatSurfaces.positions,
                        flatSurfaces.colors,
                        gl.TRIANGLES,
                        1.0,
                        0.0,
                    );
                }
            } else {
                const programId: WebGLProgram =
                    await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                        gl,
                        quality,
                        false,
                        false,
                    );
                WebGLPolyhedralBoundedSolidRenderer.configureSurfaceProgram(
                    gl,
                    programId,
                    modelViewProjection,
                    modelMatrix,
                    modelViewITLocal,
                    material,
                    activeLights,
                    quality,
                    cameraPosition,
                );
                await WebGLPolyhedralBoundedSolidRenderer.renderMesh(gl, mesh, gl.TRIANGLES);
                WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
            }

            gl.disable(gl.POLYGON_OFFSET_FILL);
        }

        await WebGLPolyhedralBoundedSolidDebugRenderer.drawDebugOverlays(
            gl,
            solid,
            camera,
            quality,
            modelViewProjection,
        );
    }

    public static async ensureInitialized(
        gl: WebGL2RenderingContext,
    ): Promise<PolyhedralBoundedSolidRendererResources> {
        const existing = WebGLPolyhedralBoundedSolidRenderer.resources.get(gl);
        if (existing !== undefined) {
            return existing;
        }

        const colorProgramId: WebGLProgram = await WebGLShaderProgramUtil.createProgramFromFiles(
            gl,
            "lineVertexShader.glsl",
            "linePixelShader.glsl",
        );

        // A concurrent caller may have finished initialization while this one
        // was awaiting its shader sources; keep the first set installed.
        const installed = WebGLPolyhedralBoundedSolidRenderer.resources.get(gl);
        if (installed !== undefined) {
            gl.deleteProgram(colorProgramId);
            return installed;
        }

        const meshVaoId = gl.createVertexArray();
        const meshPositionVboId = gl.createBuffer();
        const meshNormalVboId = gl.createBuffer();
        const meshUvVboId = gl.createBuffer();

        const colorVaoId = gl.createVertexArray();
        const colorPositionVboId = gl.createBuffer();
        const colorDataVboId = gl.createBuffer();

        if (
            meshVaoId === null ||
            meshPositionVboId === null ||
            meshNormalVboId === null ||
            meshUvVboId === null ||
            colorVaoId === null ||
            colorPositionVboId === null ||
            colorDataVboId === null
        ) {
            throw new Error("Failed to create polyhedral bounded solid renderer buffers");
        }

        const resources: PolyhedralBoundedSolidRendererResources = {
            meshVaoId,
            meshPositionVboId,
            meshNormalVboId,
            meshUvVboId,
            colorVaoId,
            colorPositionVboId,
            colorDataVboId,
            colorProgramId,
        };
        WebGLPolyhedralBoundedSolidRenderer.resources.set(gl, resources);
        return resources;
    }

    public static release(gl: WebGL2RenderingContext): void {
        const resources = WebGLPolyhedralBoundedSolidRenderer.resources.get(gl);
        if (resources === undefined) {
            return;
        }

        gl.deleteBuffer(resources.meshPositionVboId);
        gl.deleteBuffer(resources.meshNormalVboId);
        gl.deleteBuffer(resources.meshUvVboId);
        gl.deleteBuffer(resources.colorPositionVboId);
        gl.deleteBuffer(resources.colorDataVboId);
        gl.deleteVertexArray(resources.meshVaoId);
        gl.deleteVertexArray(resources.colorVaoId);
        gl.deleteProgram(resources.colorProgramId);

        WebGLPolyhedralBoundedSolidRenderer.resources.delete(gl);
    }

    /** Package-private in Java; shared with the debug renderer. */
    public static configureSurfaceProgram(
        gl: WebGL2RenderingContext,
        programId: WebGLProgram,
        modelViewProjection: Matrix4x4d,
        modelViewLocal: Matrix4x4d,
        modelViewITLocal: Matrix4x4d,
        material: SimpleMaterial,
        lights: Light[] | null,
        quality: RendererConfiguration,
        cameraPosition: Vector3Dd,
    ): void {
        const kd: ColorRgb = material.getDiffuse();
        WebGLRendererConfigurationShaderSelector.activateShader(
            gl,
            programId,
            modelViewProjection,
            quality,
            kd.r(),
            kd.g(),
            kd.b(),
        );

        WebGLPolyhedralBoundedSolidRenderer.setMatrix(gl, programId, "modelViewLocal", modelViewLocal);
        WebGLPolyhedralBoundedSolidRenderer.setMatrix(gl, programId, "modelViewITLocal", modelViewITLocal);
        WebGLPolyhedralBoundedSolidRenderer.setVector3(gl, programId, "cameraPositionGlobal", cameraPosition);
        WebGLPolyhedralBoundedSolidRenderer.setColor3(gl, programId, "ambientColor", material.getAmbient());
        WebGLPolyhedralBoundedSolidRenderer.setColor3(gl, programId, "diffuseColor", material.getDiffuse());
        WebGLPolyhedralBoundedSolidRenderer.setColor3(gl, programId, "specularColor", material.getSpecular());
        WebGLPolyhedralBoundedSolidRenderer.setFloat(
            gl,
            programId,
            "phongExponent",
            Math.fround(material.getPhongExponent()),
        );
        WebGLPolyhedralBoundedSolidRenderer.setInt(gl, programId, "withTexture", 0);
        WebGLPolyhedralBoundedSolidRenderer.setInt(gl, programId, "withBumpMap", 0);

        let lightCount = 0;
        if (lights !== null) {
            lightCount = Math.min(lights.length, 8);
            for (let i = 0; i < lightCount; i++) {
                const light: Light = lights[i]!;
                WebGLPolyhedralBoundedSolidRenderer.setVector3(
                    gl,
                    programId,
                    "lightPositionsGlobal[" + i + "]",
                    light.getPosition(),
                );
                WebGLPolyhedralBoundedSolidRenderer.setColor3(
                    gl,
                    programId,
                    "lightColorsGlobal[" + i + "]",
                    light.getEmission(),
                );
            }
        }
        WebGLPolyhedralBoundedSolidRenderer.setInt(gl, programId, "numberOfLights", lightCount);
    }

    /** Package-private in Java; shared with the debug renderer. */
    public static async renderMesh(
        gl: WebGL2RenderingContext,
        mesh: WebGLPolyhedralBoundedSolidRenderer.MeshData,
        mode: number,
    ): Promise<void> {
        const resources = await WebGLPolyhedralBoundedSolidRenderer.ensureInitialized(gl);
        gl.bindVertexArray(resources.meshVaoId);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.meshPositionVboId);
        WebGLPolyhedralBoundedSolidRenderer.upload(gl, mesh.positions);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 4, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.meshNormalVboId);
        WebGLPolyhedralBoundedSolidRenderer.upload(gl, mesh.normals);
        gl.enableVertexAttribArray(1);
        gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.meshUvVboId);
        WebGLPolyhedralBoundedSolidRenderer.upload(gl, mesh.uvs);
        gl.enableVertexAttribArray(2);
        gl.vertexAttribPointer(2, 2, gl.FLOAT, false, 0, 0);

        gl.drawArrays(mode, 0, mesh.vertexCount);

        gl.disableVertexAttribArray(0);
        gl.disableVertexAttribArray(1);
        gl.disableVertexAttribArray(2);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
    }

    /** Package-private in Java; shared with the debug renderer. */
    public static async drawColoredPrimitives(
        gl: WebGL2RenderingContext,
        mvp: Matrix4x4d,
        positions: Float32Array,
        colors: Float32Array,
        mode: number,
        size: number,
        depthBiasNdc: number,
    ): Promise<void> {
        if (positions.length === 0 || colors.length === 0) {
            return;
        }

        const resources = await WebGLPolyhedralBoundedSolidRenderer.ensureInitialized(gl);
        const colorProgramId: WebGLProgram = resources.colorProgramId;

        gl.useProgram(colorProgramId);
        const mvpLoc = gl.getUniformLocation(colorProgramId, "modelViewProjectionLocal");
        if (mvpLoc !== null) {
            gl.uniformMatrix4fv(mvpLoc, false, mvp.exportToFloatArrayColumnOrder());
        }
        const depthBiasLoc = gl.getUniformLocation(colorProgramId, "depthBiasNdc");
        if (depthBiasLoc !== null) {
            gl.uniform1f(depthBiasLoc, depthBiasNdc);
        }

        gl.bindVertexArray(resources.colorVaoId);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.colorPositionVboId);
        WebGLPolyhedralBoundedSolidRenderer.upload(gl, positions);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.colorDataVboId);
        WebGLPolyhedralBoundedSolidRenderer.upload(gl, colors);
        gl.enableVertexAttribArray(1);
        gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);

        if (mode === gl.POINTS) {
            WebGLPolyhedralBoundedSolidRenderer.setFloat(gl, colorProgramId, "pointSizeLocal", size);
        } else if (mode === gl.LINES) {
            gl.lineWidth(size);
        }
        gl.drawArrays(mode, 0, positions.length / 3);

        gl.disableVertexAttribArray(0);
        gl.disableVertexAttribArray(1);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
        gl.useProgram(null);
    }

    private static upload(gl: WebGL2RenderingContext, data: Float32Array): void {
        gl.bufferData(gl.ARRAY_BUFFER, data, gl.STREAM_DRAW);
    }

    private static buildMesh(
        solid: PolyhedralBoundedSolid,
        smoothNormals: boolean,
        quality: RendererConfiguration,
    ): WebGLPolyhedralBoundedSolidRenderer.MeshData {
        const positions: number[] = [];
        const normals: number[] = [];
        const uvs: number[] = [];
        const vertexNormals: Map<string, Vector3Dd[]> | null = smoothNormals
            ? WebGLPolyhedralBoundedSolidRenderer.buildSmoothedVertexNormals(solid)
            : null;

        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face: _PolyhedralBoundedSolidFace | null = solid.getPolygonsList().get(i);
            WebGLPolyhedralBoundedSolidRenderer.appendFaceMesh(
                face,
                positions,
                normals,
                uvs,
                vertexNormals,
                quality.getVertexNormalSmoothingThresholdDegrees(),
            );
        }
        return new WebGLPolyhedralBoundedSolidRenderer.MeshData(
            new Float32Array(positions),
            new Float32Array(normals),
            new Float32Array(uvs),
        );
    }

    /** Package-private in Java; shared with the debug renderer. */
    public static buildFaceMesh(
        solid: PolyhedralBoundedSolid,
        faceIndex: number,
    ): WebGLPolyhedralBoundedSolidRenderer.MeshData {
        const positions: number[] = [];
        const normals: number[] = [];
        const uvs: number[] = [];

        if (faceIndex >= 0 && faceIndex < solid.getPolygonsList().size()) {
            WebGLPolyhedralBoundedSolidRenderer.appendFaceMesh(
                solid.getPolygonsList().get(faceIndex),
                positions,
                normals,
                uvs,
                null,
                0.0,
            );
        }
        return new WebGLPolyhedralBoundedSolidRenderer.MeshData(
            new Float32Array(positions),
            new Float32Array(normals),
            new Float32Array(uvs),
        );
    }

    private static appendFaceMesh(
        face: _PolyhedralBoundedSolidFace | null,
        positions: number[],
        normals: number[],
        uvs: number[],
        vertexNormals: Map<string, Vector3Dd[]> | null,
        smoothingThresholdDegrees: number,
    ): void {
        if (face === null || _PolyhedralBoundedSolidFaceValidator.isSurfaceDegenerate(face)) {
            return;
        }

        const plane: InfinitePlane | null = face.getContainingPlane();
        if (plane === null) {
            return;
        }
        const normal: Vector3Dd = plane.getNormal().normalized();
        const faceTriangles: number[] = WebGLPolyhedralBoundedSolidRenderer.tessellateFace(face);
        for (let i = 0; i + 3 < faceTriangles.length; i += 4) {
            const px: number = faceTriangles[i]!;
            const py: number = faceTriangles[i + 1]!;
            const pz: number = faceTriangles[i + 2]!;
            const vertexNormal: Vector3Dd = WebGLPolyhedralBoundedSolidRenderer.resolveVertexNormal(
                vertexNormals,
                px,
                py,
                pz,
                normal,
                smoothingThresholdDegrees,
            );
            positions.push(faceTriangles[i]!);
            positions.push(faceTriangles[i + 1]!);
            positions.push(faceTriangles[i + 2]!);
            positions.push(1.0);
            normals.push(Math.fround(vertexNormal.x()));
            normals.push(Math.fround(vertexNormal.y()));
            normals.push(Math.fround(vertexNormal.z()));
            uvs.push(0.0);
            uvs.push(0.0);
        }
    }

    /**
    Java keys a `HashMap` by `VertexCoordinateKey`, the three quantized
    coordinates with `equals` and `hashCode` over them. The map is only ever
    looked up, never iterated, so a native `Map` keyed by the same three
    integers joined into a string answers every lookup the Java one does.
    */
    private static buildSmoothedVertexNormals(solid: PolyhedralBoundedSolid | null): Map<string, Vector3Dd[]> {
        const incidentNormals = new Map<string, Vector3Dd[]>();

        if (solid === null || solid.getPolygonsList() === null) {
            return incidentNormals;
        }

        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face: _PolyhedralBoundedSolidFace | null = solid.getPolygonsList().get(i);
            if (
                face === null ||
                face.getContainingPlane() === null ||
                _PolyhedralBoundedSolidFaceValidator.isSurfaceDegenerate(face)
            ) {
                continue;
            }

            let faceNormal: Vector3Dd | null = face.getContainingPlane()!.getNormal();
            if (faceNormal === null || faceNormal.length() <= VSDK.EPSILON) {
                continue;
            }
            faceNormal = faceNormal.normalized();

            for (let j = 0; j < face.boundariesList.size(); j++) {
                const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(j);
                if (loop === null || loop.boundaryStartHalfEdge === null) {
                    continue;
                }
                const start: _PolyhedralBoundedSolidHalfEdge = loop.boundaryStartHalfEdge;
                let he: _PolyhedralBoundedSolidHalfEdge | null = start;
                do {
                    if (he.startingVertex !== null && he.startingVertex.position !== null) {
                        const key: string = WebGLPolyhedralBoundedSolidRenderer.vertexCoordinateKeyFrom(
                            he.startingVertex.position,
                        );
                        let current: Vector3Dd[] | undefined = incidentNormals.get(key);
                        if (current === undefined) {
                            current = [];
                            incidentNormals.set(key, current);
                        }
                        current.push(faceNormal);
                    }
                    he = he.next();
                } while (he !== null && he !== start);
            }
        }
        return incidentNormals;
    }

    private static resolveVertexNormal(
        vertexNormals: Map<string, Vector3Dd[]> | null,
        px: number,
        py: number,
        pz: number,
        fallback: Vector3Dd,
        smoothingThresholdDegrees: number,
    ): Vector3Dd {
        if (vertexNormals === null || vertexNormals.size === 0) {
            return fallback;
        }
        const incidentNormals: Vector3Dd[] | undefined = vertexNormals.get(
            WebGLPolyhedralBoundedSolidRenderer.vertexCoordinateKey(
                WebGLPolyhedralBoundedSolidRenderer.quantizeCoordinate(px),
                WebGLPolyhedralBoundedSolidRenderer.quantizeCoordinate(py),
                WebGLPolyhedralBoundedSolidRenderer.quantizeCoordinate(pz),
            ),
        );
        if (incidentNormals === undefined || incidentNormals.length === 0) {
            return fallback;
        }

        const clampedThreshold: number = Math.max(0.0, Math.min(180.0, smoothingThresholdDegrees));
        const cosineThreshold: number = Math.cos((clampedThreshold * Math.PI) / 180.0);
        let sum: Vector3Dd = new Vector3Dd(0, 0, 0);
        let count = 0;

        for (let i = 0; i < incidentNormals.length; i++) {
            const candidate: Vector3Dd | undefined = incidentNormals[i];
            if (candidate === undefined || candidate.length() <= VSDK.EPSILON) {
                continue;
            }
            const normalizedCandidate: Vector3Dd = candidate.normalized();
            if (fallback.dotProduct(normalizedCandidate) >= cosineThreshold) {
                sum = sum.add(normalizedCandidate);
                count++;
            }
        }

        if (count === 0 || sum.length() <= VSDK.EPSILON) {
            return fallback;
        }
        return sum.normalized();
    }

    /** `Math.round(value * VERTEX_KEY_QUANTIZATION)`, Java's `long` rounding. */
    private static quantizeCoordinate(value: number): number {
        return Math.round(value * WebGLPolyhedralBoundedSolidRenderer.VERTEX_KEY_QUANTIZATION);
    }

    /** Java's `VertexCoordinateKey.from(Vector3Dd)`. */
    private static vertexCoordinateKeyFrom(point: Vector3Dd): string {
        return WebGLPolyhedralBoundedSolidRenderer.vertexCoordinateKey(
            WebGLPolyhedralBoundedSolidRenderer.quantizeCoordinate(point.x()),
            WebGLPolyhedralBoundedSolidRenderer.quantizeCoordinate(point.y()),
            WebGLPolyhedralBoundedSolidRenderer.quantizeCoordinate(point.z()),
        );
    }

    private static vertexCoordinateKey(xBits: number, yBits: number, zBits: number): string {
        return xBits + "," + yBits + "," + zBits;
    }

    /**
    Java's `tessellateFace` with its `FaceTessellationCollector`: four floats a
    vertex, `(x, y, z, 1)`, three vertices a triangle. See the class comment
    for why the triangulation runs through `MonotoneDecompositionTriangulator`
    rather than GLU, and for what is kept of GLU's output. The loops are fed
    starting at the half-edge after `boundaryStartHalfEdge`, as Java feeds GLU.
    */
    private static tessellateFace(face: _PolyhedralBoundedSolidFace | null): number[] {
        const out: number[] = [];
        if (face === null) {
            return out;
        }

        const plane: InfinitePlane | null = face.getContainingPlane();
        if (plane === null) {
            return out;
        }
        const normal: Vector3Dd = plane.getNormal().normalized();
        const referenceAxis: Vector3Dd = WebGLPolyhedralBoundedSolidRenderer.chooseReferenceAxis(normal);
        const u: Vector3Dd = referenceAxis.crossProduct(normal).normalized();
        const v: Vector3Dd = normal.crossProduct(u).normalized();

        const polygon = new Polygon2D();
        polygon.loops.length = 0;
        const originalVertices: Vector3Dd[] = [];
        let origin: Vector3Dd | null = null;

        for (let i = 0; i < face.boundariesList.size(); i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
            if (loop === null || loop.boundaryStartHalfEdge === null) {
                continue;
            }
            polygon.nextLoop();
            let he: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
            const start: _PolyhedralBoundedSolidHalfEdge = he;
            do {
                he = he!.next();
                if (he === null) {
                    break;
                }
                const p: Vector3Dd = he.startingVertex.position;
                if (origin === null) {
                    origin = p;
                }
                const delta: Vector3Dd = p.subtract(origin);
                polygon.addVertex(delta.dotProduct(u), delta.dotProduct(v));
                originalVertices.push(p);
            } while (he !== start);
        }
        if (originalVertices.length < 3) {
            return out;
        }

        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        try {
            new MonotoneDecompositionTriangulator().triangulate(polygon, triangles);
        } catch {
            // GLU_TESS_ERROR: the collector discards what it was gathering.
            return out;
        }

        for (const triangle of triangles) {
            const a: Vector3Dd | undefined = originalVertices[triangle.point0];
            let b: Vector3Dd | undefined = originalVertices[triangle.point1];
            let c: Vector3Dd | undefined = originalVertices[triangle.point2];
            if (a === undefined || b === undefined || c === undefined) {
                continue;
            }
            if (b.subtract(a).crossProduct(c.subtract(a)).dotProduct(normal) < 0.0) {
                const temp: Vector3Dd = b;
                b = c;
                c = temp;
            }
            WebGLPolyhedralBoundedSolidRenderer.emitVertex(out, a);
            WebGLPolyhedralBoundedSolidRenderer.emitVertex(out, b);
            WebGLPolyhedralBoundedSolidRenderer.emitVertex(out, c);
        }
        return out;
    }

    private static emitVertex(out: number[], p: Vector3Dd): void {
        out.push(Math.fround(p.x()));
        out.push(Math.fround(p.y()));
        out.push(Math.fround(p.z()));
        out.push(1.0);
    }

    /** The reference-axis choice of `_StlFaceTriangulator.chooseReferenceAxis`. */
    private static chooseReferenceAxis(normal: Vector3Dd): Vector3Dd {
        const ax: number = Math.abs(normal.x());
        const ay: number = Math.abs(normal.y());
        const az: number = Math.abs(normal.z());
        if (ax <= ay && ax <= az) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        if (ay <= az) {
            return new Vector3Dd(0.0, 1.0, 0.0);
        }
        return new Vector3Dd(0.0, 0.0, 1.0);
    }

    private static buildFlatShadedSurfaceTriangles(
        mesh: WebGLPolyhedralBoundedSolidRenderer.MeshData | null,
        modelMatrix: Matrix4x4d | null,
        modelViewITLocal: Matrix4x4d | null,
        material: SimpleMaterial,
        activeLights: Light[] | null,
        cameraPosition: Vector3Dd | null,
    ): { positions: Float32Array; colors: Float32Array } {
        const positions: number[] = [];
        const colors: number[] = [];

        if (
            mesh === null ||
            modelMatrix === null ||
            modelViewITLocal === null ||
            mesh.positions.length < 12 ||
            mesh.normals.length < 9
        ) {
            return { positions: new Float32Array(positions), colors: new Float32Array(colors) };
        }

        for (
            let pos = 0, normal = 0;
            pos + 11 < mesh.positions.length && normal + 8 < mesh.normals.length;
            pos += 12, normal += 9
        ) {
            const p0 = new Vector3Dd(mesh.positions[pos]!, mesh.positions[pos + 1]!, mesh.positions[pos + 2]!);
            const p1 = new Vector3Dd(mesh.positions[pos + 4]!, mesh.positions[pos + 5]!, mesh.positions[pos + 6]!);
            const p2 = new Vector3Dd(mesh.positions[pos + 8]!, mesh.positions[pos + 9]!, mesh.positions[pos + 10]!);

            const centroidLocal: Vector3Dd = p0
                .add(p1)
                .add(p2)
                .multiply(1.0 / 3.0);
            const centroidGlobal: Vector3Dd = modelMatrix.multiply(centroidLocal);
            const normalLocal: Vector3Dd = new Vector3Dd(
                mesh.normals[normal]!,
                mesh.normals[normal + 1]!,
                mesh.normals[normal + 2]!,
            ).normalized();
            const normalGlobal4: Vector4Dd = modelViewITLocal.multiply(
                new Vector4Dd(normalLocal.x(), normalLocal.y(), normalLocal.z(), 0.0),
            );
            const normalGlobal: Vector3Dd = new Vector3Dd(
                normalGlobal4.x(),
                normalGlobal4.y(),
                normalGlobal4.z(),
            ).normalized();

            const color: ColorRgb = WebGLPolyhedralBoundedSolidRenderer.evaluateFlatColor(
                centroidGlobal,
                normalGlobal,
                material,
                activeLights,
                cameraPosition,
            );

            WebGLPolyhedralBoundedSolidRenderer.appendTriangleVertex(positions, colors, p0, color);
            WebGLPolyhedralBoundedSolidRenderer.appendTriangleVertex(positions, colors, p1, color);
            WebGLPolyhedralBoundedSolidRenderer.appendTriangleVertex(positions, colors, p2, color);
        }

        return { positions: new Float32Array(positions), colors: new Float32Array(colors) };
    }

    private static appendTriangleVertex(
        positions: number[],
        colors: number[],
        point: Vector3Dd,
        color: ColorRgb,
    ): void {
        positions.push(point.x());
        positions.push(point.y());
        positions.push(point.z());
        WebGLPolyhedralBoundedSolidRenderer.appendColor(colors, color);
    }

    private static appendColor(colors: number[], color: ColorRgb): void {
        colors.push(color.r());
        colors.push(color.g());
        colors.push(color.b());
    }

    private static evaluateFlatColor(
        pointGlobal: Vector3Dd,
        normalGlobal: Vector3Dd,
        material: SimpleMaterial,
        activeLights: Light[] | null,
        cameraPosition: Vector3Dd | null,
    ): ColorRgb {
        let normal: Vector3Dd = normalGlobal;
        let viewDir: Vector3Dd =
            cameraPosition !== null ? cameraPosition.subtract(pointGlobal) : new Vector3Dd(0, 0, 1);
        if (viewDir.length() > VSDK.EPSILON) {
            viewDir = viewDir.normalized();
            if (normal.dotProduct(viewDir) < 0.0) {
                normal = normal.multiply(-1.0);
            }
        }

        const ambient: ColorRgb = material.getAmbient();
        const diffuse: ColorRgb = material.getDiffuse();
        const specular: ColorRgb = material.getSpecular();
        let r: number = ambient.r();
        let g: number = ambient.g();
        let b: number = ambient.b();

        if (activeLights !== null) {
            for (let i = 0; i < activeLights.length; i++) {
                const light: Light = activeLights[i]!;
                let lightDir: Vector3Dd = light.getPosition().subtract(pointGlobal);
                if (lightDir.length() <= VSDK.EPSILON) {
                    continue;
                }
                lightDir = lightDir.normalized();
                const ndotl: number = Math.max(normal.dotProduct(lightDir), 0.0);
                const reflection: Vector3Dd = normal.multiply(2.0 * ndotl).subtract(lightDir);
                let spec = 0.0;
                if (ndotl > 0.0 && viewDir.length() > VSDK.EPSILON && reflection.length() > VSDK.EPSILON) {
                    spec = Math.pow(
                        Math.max(reflection.normalized().dotProduct(viewDir), 0.0),
                        material.getPhongExponent(),
                    );
                }

                const lightColor: ColorRgb = light.getEmission();
                r += lightColor.r() * diffuse.r() * ndotl + lightColor.r() * specular.r() * spec;
                g += lightColor.g() * diffuse.g() * ndotl + lightColor.g() * specular.g() * spec;
                b += lightColor.b() * diffuse.b() * ndotl + lightColor.b() * specular.b() * spec;
            }
        }

        return new ColorRgb(
            WebGLPolyhedralBoundedSolidRenderer.clamp01(r),
            WebGLPolyhedralBoundedSolidRenderer.clamp01(g),
            WebGLPolyhedralBoundedSolidRenderer.clamp01(b),
        );
    }

    private static clamp01(value: number): number {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private static setMatrix(
        gl: WebGL2RenderingContext,
        programId: WebGLProgram,
        name: string,
        matrix: Matrix4x4d,
    ): void {
        const loc = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniformMatrix4fv(loc, false, matrix.exportToFloatArrayColumnOrder());
        }
    }

    private static setVector3(
        gl: WebGL2RenderingContext,
        programId: WebGLProgram,
        name: string,
        value: Vector3Dd,
    ): void {
        const loc = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniform3f(loc, value.x(), value.y(), value.z());
        }
    }

    /** Java's `setVector3(GL4, int, String, ColorRgb)` overload. */
    private static setColor3(gl: WebGL2RenderingContext, programId: WebGLProgram, name: string, value: ColorRgb): void {
        const loc = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniform3f(loc, value.r(), value.g(), value.b());
        }
    }

    private static setInt(gl: WebGL2RenderingContext, programId: WebGLProgram, name: string, value: number): void {
        const loc = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniform1i(loc, value);
        }
    }

    private static setFloat(gl: WebGL2RenderingContext, programId: WebGLProgram, name: string, value: number): void {
        const loc = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniform1f(loc, value);
        }
    }
}

export namespace WebGLPolyhedralBoundedSolidRenderer {
    /** Java's package-private `MeshData`: four position, three normal and two texture floats a vertex. */
    export class MeshData {
        public readonly vertexCount: number;

        public constructor(
            public readonly positions: Float32Array,
            public readonly normals: Float32Array,
            public readonly uvs: Float32Array,
        ) {
            this.vertexCount = positions.length / 4;
        }
    }
}
