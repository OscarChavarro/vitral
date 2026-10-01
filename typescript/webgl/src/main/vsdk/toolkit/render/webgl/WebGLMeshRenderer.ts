import {
    ColorRgb,
    Logger,
    Matrix4x4d,
    MicroFacetedMaterial,
    PointLight,
    RendererConfiguration,
    SimpleMaterial,
    Vector3Dd,
    VSDK,
    type Camera,
    type Geometry,
    type Light,
    type RGBImageUncompressed,
} from "@vitral/base";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";
import { WebGLMinMaxRenderer } from "./WebGLMinMaxRenderer.js";
import { WebGLRendererConfigurationShaderSelector } from "./WebGLRendererConfigurationShaderSelector.js";
import { WebGLSelectionCornersRenderer } from "./WebGLSelectionCornersRenderer.js";

interface MeshGpuResources {
    vertexArray: WebGLVertexArrayObject;
    buffers: WebGLBuffer[];
    /// Index buffer of the edges of every triangle (see `renderWireMesh`)
    wireIndexBuffer: WebGLBuffer;
    wireIndexCount: number;
}

/**
A triangle mesh and the WebGL objects that hold it in the GPU (Java's
`Jogl4MeshRenderer.Mesh`). As a page can own several contexts, the GPU
objects are kept per context.
*/
export class WebGLMesh {
    public readonly positions: Float32Array;
    public readonly normals: Float32Array;
    public readonly uvs: Float32Array;
    public readonly tangents: Float32Array;
    public readonly biNormals: Float32Array;
    public readonly vertexCount: number;
    /// Vertices of the sides the geometry defines; the rest (if any) are
    /// back sides added to see open surfaces from both sides
    private frontVertexCount: number;
    public readonly characteristicSize: number;

    /** @internal */
    public readonly gpu = new WeakMap<WebGL2RenderingContext, MeshGpuResources>();

    /** @internal */
    public vertexNormalLinePositions: Float32Array | null = null;
    /** @internal */
    public vertexNormalLineColors: Float32Array | null = null;
    /** @internal */
    public triangleNormalLinePositions: Float32Array | null = null;
    /** @internal */
    public triangleNormalLineColors: Float32Array | null = null;

    /**
    @param positions x,y,z of each vertex; three consecutive vertices
    make a triangle
    @param normals x,y,z of the normal of each vertex
    @param uvs u,v of each vertex
    @param tangents x,y,z of the tangent of each vertex
    @param biNormals x,y,z of the binormal of each vertex
    @param characteristicSize approximate size of the mesh, used to scale
    the normal overlays
    */
    public constructor(
        positions: Float32Array,
        normals: Float32Array,
        uvs: Float32Array,
        tangents: Float32Array,
        biNormals: Float32Array,
        characteristicSize: number,
    ) {
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.tangents = tangents;
        this.biNormals = biNormals;
        this.vertexCount = Math.trunc(positions.length / 3);
        this.frontVertexCount = this.vertexCount;
        this.characteristicSize = characteristicSize;
    }

    /**
    Marks the vertices after the first `count` ones as back sides of open
    surfaces: they are drawn as surfaces and wires, but their points and
    normals are not shown again.
    @param count number of vertices of the front sides
    */
    public setFrontVertexCount(count: number): void {
        this.frontVertexCount = Math.max(0, Math.min(count, this.vertexCount));
    }

    public getFrontVertexCount(): number {
        return this.frontVertexCount;
    }
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4MeshRenderer`.

Renders any triangle mesh (given as non indexed arrays of positions, normals,
texture coordinates, tangents and binormals) honoring every bit of a
`RendererConfiguration` (surfaces, wires, points, normals, bounding volume,
texture, bump map and shading type), with the GLSL programs selected by
`WebGLRendererConfigurationShaderSelector`. It is the common flow shared by
the renderers of each geometry (see `WebGLSphereRenderer` and
`WebGLGeometryRenderer`).

The WebGL objects of a mesh are created the first time it is drawn in a
context, and must be released with `release` while that context is alive.

The runtime boundaries are the ones recorded in `WebGLSphereRenderer`: WebGL
has no `glPolygonMode`, so the wires pass draws the three edges of every
triangle as `LINES` (through an index buffer built once per mesh) instead of
asking the rasterizer to outline a filled primitive, and `POLYGON_OFFSET_LINE`
is equally absent, leaving the pass its depth state; WebGL has no
`glPointSize`, so the point size travels to the vertex shader as the
`pointSizeLocal` uniform; and a GLSL source arrives over `fetch`, so drawing is
asynchronous.
*/
export class WebGLMeshRenderer {
    private static readonly SURFACE_POLYGON_OFFSET_FACTOR = 1.0;
    private static readonly SURFACE_POLYGON_OFFSET_UNITS = 1.0;
    private static readonly VERTEX_NORMAL_SCALE = Math.fround(0.1);
    private static readonly TRIANGLE_NORMAL_SCALE = Math.fround(0.12);
    private static readonly NORMAL_START_EPSILON = Math.fround(0.002);
    private static readonly NORMAL_LINE_DEPTH_BIAS_NDC = Math.fround(-1.0e-4);
    // [BLIN1978b] bump scales used by both shader and raytracer examples.
    private static readonly DEFAULT_BUMP_SCALE = new Vector3Dd(1.0, 1.0, 1.0);
    private static readonly VERTEX_NORMAL_COLOR: readonly number[] = [1.0, 1.0, 0.0];
    private static readonly TRIANGLE_NORMAL_COLOR: readonly number[] = [0.0, 1.0, 1.0];

    /// Size of the light arrays of the GLSL programs
    public static readonly MAX_LIGHTS = 8;
    private static tooManyLightsReported = false;

    // Every Phong/Gouraud/Cook-Torrance shader declares a `sTexture` sampler
    // even when the material has no diffuse texture (`withTexture` is set to
    // 0, and the shader does not sample it then). A 1x1 white texture is
    // bound to unit 0 instead, whenever the material has none, so nothing is
    // ever unbound while such a shader is active.
    private static readonly dummyTextures = new WeakMap<WebGL2RenderingContext, WebGLTexture>();

    private constructor() {}

    private static reportTooManyLights(count: number): void {
        if (WebGLMeshRenderer.tooManyLightsReported) {
            return;
        }
        WebGLMeshRenderer.tooManyLightsReported = true;
        Logger.reportMessage(
            null,
            VSDK.WARNING,
            "WebGLMeshRenderer",
            "Scene has " + count + " lights, but shaders use only the first " + WebGLMeshRenderer.MAX_LIGHTS,
        );
    }

    /**
    Draws a mesh, with the passes selected by the configuration. Java's two
    overloads are taken by the kind of the `lights` argument: a single light
    (or null, for a light at the camera) or a list of them.

    @param gl WebGL context
    @param mesh mesh to draw
    @param geometry geometry the mesh comes from, used for the bounding volume
    @param camera camera that views the mesh
    @param lights lights of the scene (at most `MAX_LIGHTS` are used), a single
    light, or null or empty to use a light at the camera
    @param material material of the surfaces
    @param quality bits of rendering configuration
    @param textureMap texture of the surfaces, or null
    @param normalMap normal (bump) map of the surfaces, or null
    @param localTransform transformation from the mesh space to world space
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        mesh: WebGLMesh | null,
        geometry: Geometry | null,
        camera: Camera | null,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        if (mesh === null || camera === null || material === null || quality === null) {
            return;
        }
        let lightList: readonly (Light | null)[] = WebGLMeshRenderer.toLightList(lights);
        if (lightList.length === 0) {
            lightList = [new PointLight(camera.getPosition(), new ColorRgb(1, 1, 1))];
        }

        WebGLMeshRenderer.upload(gl, mesh);

        const hasTexture: boolean = textureMap !== null;
        const hasNormalMap: boolean = normalMap !== null;
        const textureId: WebGLTexture | null = hasTexture ? WebGLImageRenderer.activate(gl, textureMap) : null;
        const normalMapId: WebGLTexture | null =
            hasNormalMap && quality.isBumpMapSet() ? WebGLImageRenderer.activate(gl, normalMap) : null;

        const localTransformNotNull: Matrix4x4d = localTransform !== null ? localTransform : Matrix4x4d.identityMatrix();
        await WebGLMeshRenderer.drawPasses(
            gl,
            mesh,
            geometry,
            camera,
            lightList,
            material,
            quality,
            textureId,
            normalMapId,
            hasTexture,
            localTransformNotNull,
        );
    }

    /**
    @return the lights of Java's two overloads as one list: a single light,
    or a list of them (i.e. the `ArrayList` of a scene)
    */
    public static toLightList(lights: Light | Iterable<Light | null> | null): (Light | null)[] {
        if (lights === null) {
            return [];
        }
        if (typeof (lights as Partial<Iterable<Light | null>>)[Symbol.iterator] === "function") {
            return Array.from(lights as Iterable<Light | null>);
        }
        return [lights as Light];
    }

    private static async drawPasses(
        gl: WebGL2RenderingContext,
        mesh: WebGLMesh,
        geometry: Geometry | null,
        camera: Camera,
        lights: readonly (Light | null)[],
        material: SimpleMaterial,
        quality: RendererConfiguration,
        textureId: WebGLTexture | null,
        normalMapId: WebGLTexture | null,
        hasTexture: boolean,
        localTransform: Matrix4x4d,
    ): Promise<void> {
        const modelViewProjection: Matrix4x4d = camera.calculateProjectionMatrix().multiply(localTransform);
        const modelViewITLocal: Matrix4x4d = localTransform.invert().transpose();
        const resources: MeshGpuResources = mesh.gpu.get(gl)!;

        if (quality.isSurfacesSet()) {
            const program: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                quality,
                hasTexture,
                normalMapId !== null,
            );
            WebGLMeshRenderer.configureProgram(
                gl,
                program,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                lights,
                material,
                quality,
                textureId,
                normalMapId,
            );

            // Pass 1: Surfaces. Push slightly backwards to avoid z-fighting
            // with overlay passes (wireframe/points).
            gl.enable(gl.DEPTH_TEST);
            gl.depthMask(true);
            gl.depthFunc(gl.LESS);
            gl.enable(gl.POLYGON_OFFSET_FILL);
            gl.polygonOffset(WebGLMeshRenderer.SURFACE_POLYGON_OFFSET_FACTOR, WebGLMeshRenderer.SURFACE_POLYGON_OFFSET_UNITS);
            gl.enable(gl.CULL_FACE);
            gl.cullFace(gl.BACK);
            WebGLMeshRenderer.renderMesh(gl, mesh, resources);
            gl.disable(gl.POLYGON_OFFSET_FILL);

            WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if (quality.isWiresSet()) {
            const wireQuality = new RendererConfiguration();
            wireQuality.setTexture(false);
            wireQuality.setUseVertexColors(false);
            wireQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);

            const wireProgram: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                wireQuality,
                false,
                false,
            );

            let wireMaterial = new SimpleMaterial(material);
            wireMaterial = wireMaterial.withDiffuse(new ColorRgb(1, 1, 1));
            wireMaterial = wireMaterial.withSpecular(new ColorRgb(0, 0, 0));
            wireMaterial = wireMaterial.withAmbient(new ColorRgb(0, 0, 0));

            WebGLMeshRenderer.configureProgram(
                gl,
                wireProgram,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                lights,
                wireMaterial,
                wireQuality,
                null,
                null,
            );

            // Pass 2: Wires. Keep depth test but bias in front of surfaces.
            gl.enable(gl.DEPTH_TEST);
            gl.depthMask(false);
            gl.depthFunc(gl.LEQUAL);
            gl.disable(gl.CULL_FACE);
            gl.lineWidth(1.0);
            WebGLMeshRenderer.renderWireMesh(gl, resources);

            WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if (quality.isPointsSet()) {
            const pointQuality = new RendererConfiguration();
            pointQuality.setTexture(false);
            pointQuality.setUseVertexColors(false);
            pointQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);

            const pointsProgram: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                pointQuality,
                false,
                false,
            );

            let pointMaterial = new SimpleMaterial(material);
            pointMaterial = pointMaterial.withAmbient(new ColorRgb(0, 0, 0));
            pointMaterial = pointMaterial.withDiffuse(new ColorRgb(1, 0, 0)); // #ff0000
            pointMaterial = pointMaterial.withSpecular(new ColorRgb(0, 0, 0));

            WebGLMeshRenderer.configureProgram(
                gl,
                pointsProgram,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                lights,
                pointMaterial,
                pointQuality,
                null,
                null,
            );

            // Pass 3: Points. Draw last with depth test against surfaces
            // (and no depth writes), so points appear above wireframe.
            gl.enable(gl.DEPTH_TEST);
            gl.depthMask(false);
            gl.depthFunc(gl.LEQUAL);
            gl.disable(gl.CULL_FACE);
            WebGLMeshRenderer.setFloat(gl, pointsProgram, "pointSizeLocal", 4.0);
            gl.bindVertexArray(resources.vertexArray);
            gl.drawArrays(gl.POINTS, 0, mesh.getFrontVertexCount());
            gl.bindVertexArray(null);

            WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if (quality.isNormalsSet() || quality.isTrianglesNormalsSet()) {
            await WebGLMeshRenderer.drawNormalOverlays(gl, mesh, quality, modelViewProjection);
        }

        if (quality.isBoundingVolumeSet()) {
            await WebGLMinMaxRenderer.draw(gl, geometry, camera, localTransform);
        }

        if (quality.isSelectionCornersSet()) {
            await WebGLSelectionCornersRenderer.draw(gl, geometry, camera, localTransform);
        }

        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
        gl.bindTexture(gl.TEXTURE_2D, null);
    }

    /**
    Releases the WebGL objects of a mesh in a context. PRE: the context is
    alive.
    */
    public static release(gl: WebGL2RenderingContext, mesh: WebGLMesh | null): void {
        const resources: MeshGpuResources | undefined = mesh === null ? undefined : mesh.gpu.get(gl);

        if (mesh === null || resources === undefined) {
            return;
        }
        for (const buffer of resources.buffers) {
            gl.deleteBuffer(buffer);
        }
        gl.deleteBuffer(resources.wireIndexBuffer);
        gl.deleteVertexArray(resources.vertexArray);
        mesh.gpu.delete(gl);
    }

    /**
    Releases the resources shared by all the meshes.
    */
    public static dispose(gl: WebGL2RenderingContext): void {
        WebGLMinMaxRenderer.dispose(gl);
        const dummy: WebGLTexture | undefined = WebGLMeshRenderer.dummyTextures.get(gl);
        if (dummy !== undefined) {
            gl.deleteTexture(dummy);
            WebGLMeshRenderer.dummyTextures.delete(gl);
        }
    }

    /**
    @return the 1x1 white texture, created the first time it is needed
    */
    private static ensureDummyTexture(gl: WebGL2RenderingContext): WebGLTexture | null {
        const existing: WebGLTexture | undefined = WebGLMeshRenderer.dummyTextures.get(gl);

        if (existing !== undefined) {
            return existing;
        }

        const texture: WebGLTexture | null = gl.createTexture();

        if (texture === null) {
            return null;
        }
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.pixelStorei(gl.UNPACK_ALIGNMENT, 1);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGB8, 1, 1, 0, gl.RGB, gl.UNSIGNED_BYTE, new Uint8Array([255, 255, 255]));
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
        gl.bindTexture(gl.TEXTURE_2D, null);
        WebGLMeshRenderer.dummyTextures.set(gl, texture);

        return texture;
    }

    private static configureProgram(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        modelViewProjection: Matrix4x4d,
        modelViewLocal: Matrix4x4d,
        modelViewITLocal: Matrix4x4d,
        camera: Camera,
        lights: readonly (Light | null)[],
        material: SimpleMaterial,
        quality: RendererConfiguration,
        textureId: WebGLTexture | null,
        normalMapId: WebGLTexture | null,
    ): void {
        const kd: ColorRgb = material.getDiffuse();
        WebGLRendererConfigurationShaderSelector.activateShader(gl, program, modelViewProjection, quality, kd.r(), kd.g(), kd.b());

        WebGLMeshRenderer.setMatrix(gl, program, "modelViewLocal", modelViewLocal);
        WebGLMeshRenderer.setMatrix(gl, program, "modelViewITLocal", modelViewITLocal);

        WebGLMeshRenderer.setVector3(gl, program, "cameraPositionGlobal", camera.getPosition());
        let lightCount = 0;
        for (const light of lights) {
            if (light === null) {
                continue;
            }
            if (lightCount >= WebGLMeshRenderer.MAX_LIGHTS) {
                WebGLMeshRenderer.reportTooManyLights(lights.length);
                break;
            }
            WebGLMeshRenderer.setVector3(gl, program, "lightPositionsGlobal[" + lightCount + "]", light.getPosition());
            WebGLMeshRenderer.setColor(gl, program, "lightColorsGlobal[" + lightCount + "]", light.getEmission());
            lightCount++;
        }
        WebGLMeshRenderer.setInt(gl, program, "numberOfLights", lightCount);

        WebGLMeshRenderer.setColor(gl, program, "ambientColor", material.getAmbient());
        WebGLMeshRenderer.setColor(gl, program, "diffuseColor", material.getDiffuse());
        WebGLMeshRenderer.setColor(gl, program, "specularColor", material.getSpecular());
        WebGLMeshRenderer.setVector3(gl, program, "bumpScale", WebGLMeshRenderer.DEFAULT_BUMP_SCALE);
        WebGLMeshRenderer.setFloat(gl, program, "phongExponent", material.getPhongExponent());
        WebGLMeshRenderer.configureMicroFacetUniforms(gl, program, material);
        WebGLMeshRenderer.setInt(gl, program, "withTexture", quality.isTextureSet() && textureId !== null ? 1 : 0);
        WebGLMeshRenderer.setInt(gl, program, "withBumpMap", quality.isBumpMapSet() && normalMapId !== null ? 1 : 0);

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, textureId !== null ? textureId : WebGLMeshRenderer.ensureDummyTexture(gl));

        if (normalMapId !== null) {
            gl.activeTexture(gl.TEXTURE1);
            gl.bindTexture(gl.TEXTURE_2D, normalMapId);
            gl.activeTexture(gl.TEXTURE0);
        }
    }

    private static setMatrix(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, matrix: Matrix4x4d): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniformMatrix4fv(loc, false, matrix.exportToFloatArrayColumnOrder());
        }
    }

    private static setVector3(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: Vector3Dd): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform3f(loc, value.x(), value.y(), value.z());
        }
    }

    private static setColor(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: ColorRgb): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform3f(loc, value.r(), value.g(), value.b());
        }
    }

    private static setInt(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: number): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform1i(loc, value);
        }
    }

    private static setFloat(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: number): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform1f(loc, value);
        }
    }

    private static configureMicroFacetUniforms(gl: WebGL2RenderingContext, program: WebGLProgram, material: SimpleMaterial): void {
        let roughness = 0.35;
        let alpha: number = roughness * roughness;
        let fresnelF0: ColorRgb = material.getSpecular();
        let kd = 1.0;
        let ks = 1.0;
        let fresnelModel: number = MicroFacetedMaterial.FRESNEL_MODEL_SCHLICK;
        let ndfModel: number = MicroFacetedMaterial.NDF_MODEL_BECKMANN;
        let geometryModel: number = MicroFacetedMaterial.GEOMETRY_MODEL_SMITH;
        let eta = new ColorRgb(1.5, 1.5, 1.5);
        let kappa = new ColorRgb(0.0, 0.0, 0.0);

        if (material instanceof MicroFacetedMaterial) {
            roughness = material.getRoughness();
            alpha = material.getAlpha();
            fresnelF0 = material.getFresnelF0();
            kd = material.getKd();
            ks = material.getKs();
            fresnelModel = material.getFresnelModel();
            ndfModel = material.getNdfModel();
            geometryModel = material.getGeometryModel();
            eta = material.getEta();
            kappa = material.getKappa();
        }

        WebGLMeshRenderer.setFloat(gl, program, "cookRoughness", roughness);
        WebGLMeshRenderer.setFloat(gl, program, "cookAlpha", alpha);
        WebGLMeshRenderer.setFloat(gl, program, "cookKd", kd);
        WebGLMeshRenderer.setFloat(gl, program, "cookKs", ks);
        WebGLMeshRenderer.setColor(gl, program, "cookF0", fresnelF0);
        WebGLMeshRenderer.setColor(gl, program, "cookEta", eta);
        WebGLMeshRenderer.setColor(gl, program, "cookKappa", kappa);
        WebGLMeshRenderer.setInt(gl, program, "cookFresnelModel", fresnelModel);
        WebGLMeshRenderer.setInt(gl, program, "cookNdfModel", ndfModel);
        WebGLMeshRenderer.setInt(gl, program, "cookGeometryModel", geometryModel);
    }

    private static renderMesh(gl: WebGL2RenderingContext, mesh: WebGLMesh, resources: MeshGpuResources): void {
        gl.bindVertexArray(resources.vertexArray);
        gl.drawArrays(gl.TRIANGLES, 0, mesh.vertexCount);
        gl.bindVertexArray(null);
    }

    /**
    The stand-in for Java's `glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)` over
    the same mesh: `LINES` over the three edges of every triangle.
    */
    private static renderWireMesh(gl: WebGL2RenderingContext, resources: MeshGpuResources): void {
        gl.bindVertexArray(resources.vertexArray);
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, resources.wireIndexBuffer);
        gl.drawElements(gl.LINES, resources.wireIndexCount, gl.UNSIGNED_INT, 0);
        gl.bindVertexArray(null);
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, null);
    }

    private static upload(gl: WebGL2RenderingContext, mesh: WebGLMesh): void {
        if (mesh.gpu.has(gl)) {
            return;
        }
        const vertexArray: WebGLVertexArrayObject | null = gl.createVertexArray();
        const wireIndexBuffer: WebGLBuffer | null = gl.createBuffer();

        if (vertexArray === null || wireIndexBuffer === null) {
            throw new Error("WebGLMeshRenderer: cannot create the GL objects of a mesh");
        }
        const buffers: WebGLBuffer[] = [];

        gl.bindVertexArray(vertexArray);
        buffers.push(WebGLMeshRenderer.uploadFloatBuffer(gl, 0, 3, mesh.positions));
        buffers.push(WebGLMeshRenderer.uploadFloatBuffer(gl, 1, 3, mesh.normals));
        buffers.push(WebGLMeshRenderer.uploadFloatBuffer(gl, 2, 2, mesh.uvs));
        buffers.push(WebGLMeshRenderer.uploadFloatBuffer(gl, 3, 3, mesh.tangents));
        buffers.push(WebGLMeshRenderer.uploadFloatBuffer(gl, 4, 3, mesh.biNormals));
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);

        const triangleCount: number = Math.trunc(mesh.vertexCount / 3);
        const indices = new Uint32Array(triangleCount * 6);
        let out = 0;
        for (let triangle = 0; triangle < triangleCount; triangle++) {
            const base: number = triangle * 3;
            indices[out++] = base;
            indices[out++] = base + 1;
            indices[out++] = base + 1;
            indices[out++] = base + 2;
            indices[out++] = base + 2;
            indices[out++] = base;
        }
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, wireIndexBuffer);
        gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, indices, gl.STATIC_DRAW);
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, null);

        mesh.gpu.set(gl, { vertexArray, buffers, wireIndexBuffer, wireIndexCount: indices.length });
    }

    private static uploadFloatBuffer(
        gl: WebGL2RenderingContext,
        attribIndex: number,
        coordinatesSize: number,
        data: Float32Array,
    ): WebGLBuffer {
        const buffer: WebGLBuffer | null = gl.createBuffer();

        if (buffer === null) {
            throw new Error("WebGLMeshRenderer: cannot create a vertex buffer");
        }
        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(gl.ARRAY_BUFFER, data, gl.STATIC_DRAW);
        gl.enableVertexAttribArray(attribIndex);
        gl.vertexAttribPointer(attribIndex, coordinatesSize, gl.FLOAT, false, 0, 0);
        return buffer;
    }

    private static async drawNormalOverlays(
        gl: WebGL2RenderingContext,
        mesh: WebGLMesh,
        quality: RendererConfiguration,
        modelViewProjection: Matrix4x4d,
    ): Promise<void> {
        gl.enable(gl.DEPTH_TEST);
        gl.depthMask(false);
        gl.depthFunc(gl.LEQUAL);
        gl.disable(gl.CULL_FACE);

        const size: number = Math.fround(Math.max(1e-6, mesh.characteristicSize));

        if (quality.isNormalsSet()) {
            if (mesh.vertexNormalLinePositions === null || mesh.vertexNormalLineColors === null) {
                const length: number = Math.fround(size * WebGLMeshRenderer.VERTEX_NORMAL_SCALE);
                const epsilon: number = Math.fround(size * WebGLMeshRenderer.NORMAL_START_EPSILON);
                mesh.vertexNormalLinePositions = WebGLMeshRenderer.buildVertexNormalLinePositions(mesh, length, epsilon);
                mesh.vertexNormalLineColors = WebGLMeshRenderer.buildUniformColors(
                    mesh.vertexNormalLinePositions.length / 3,
                    WebGLMeshRenderer.VERTEX_NORMAL_COLOR,
                );
            }
            await WebGLLineRenderer.drawLines(
                gl,
                modelViewProjection,
                mesh.vertexNormalLinePositions,
                mesh.vertexNormalLineColors,
                1.0,
                WebGLMeshRenderer.NORMAL_LINE_DEPTH_BIAS_NDC,
            );
        }

        if (quality.isTrianglesNormalsSet()) {
            if (mesh.triangleNormalLinePositions === null || mesh.triangleNormalLineColors === null) {
                const length: number = Math.fround(size * WebGLMeshRenderer.TRIANGLE_NORMAL_SCALE);
                const epsilon: number = Math.fround(size * WebGLMeshRenderer.NORMAL_START_EPSILON);
                mesh.triangleNormalLinePositions = WebGLMeshRenderer.buildTriangleNormalLinePositions(mesh, length, epsilon);
                mesh.triangleNormalLineColors = WebGLMeshRenderer.buildUniformColors(
                    mesh.triangleNormalLinePositions.length / 3,
                    WebGLMeshRenderer.TRIANGLE_NORMAL_COLOR,
                );
            }
            await WebGLLineRenderer.drawLines(
                gl,
                modelViewProjection,
                mesh.triangleNormalLinePositions,
                mesh.triangleNormalLineColors,
                1.0,
                WebGLMeshRenderer.NORMAL_LINE_DEPTH_BIAS_NDC,
            );
        }
    }

    private static buildVertexNormalLinePositions(mesh: WebGLMesh, length: number, epsilon: number): Float32Array {
        const f = Math.fround;
        const vertexCountLocal: number = mesh.getFrontVertexCount();
        const lines = new Float32Array(vertexCountLocal * 2 * 3);
        let out = 0;
        for (let i = 0; i < vertexCountLocal; i++) {
            const base: number = i * 3;
            const px: number = mesh.positions[base]!;
            const py: number = mesh.positions[base + 1]!;
            const pz: number = mesh.positions[base + 2]!;
            const nx: number = mesh.normals[base]!;
            const ny: number = mesh.normals[base + 1]!;
            const nz: number = mesh.normals[base + 2]!;

            const sx: number = f(px + f(nx * epsilon));
            const sy: number = f(py + f(ny * epsilon));
            const sz: number = f(pz + f(nz * epsilon));
            const ex: number = f(sx + f(nx * length));
            const ey: number = f(sy + f(ny * length));
            const ez: number = f(sz + f(nz * length));

            lines[out++] = sx;
            lines[out++] = sy;
            lines[out++] = sz;
            lines[out++] = ex;
            lines[out++] = ey;
            lines[out++] = ez;
        }
        return lines;
    }

    private static buildTriangleNormalLinePositions(mesh: WebGLMesh, length: number, epsilon: number): Float32Array {
        const f = Math.fround;
        const triangleCount: number = Math.trunc(mesh.getFrontVertexCount() / 3);
        const lines = new Float32Array(triangleCount * 2 * 3);
        let out = 0;

        for (let tri = 0; tri < triangleCount; tri++) {
            const base: number = tri * 9;
            const p0x: number = mesh.positions[base]!;
            const p0y: number = mesh.positions[base + 1]!;
            const p0z: number = mesh.positions[base + 2]!;
            const p1x: number = mesh.positions[base + 3]!;
            const p1y: number = mesh.positions[base + 4]!;
            const p1z: number = mesh.positions[base + 5]!;
            const p2x: number = mesh.positions[base + 6]!;
            const p2y: number = mesh.positions[base + 7]!;
            const p2z: number = mesh.positions[base + 8]!;

            const cx: number = f(f(f(p0x + p1x) + p2x) / 3.0);
            const cy: number = f(f(f(p0y + p1y) + p2y) / 3.0);
            const cz: number = f(f(f(p0z + p1z) + p2z) / 3.0);

            const ux: number = f(p1x - p0x);
            const uy: number = f(p1y - p0y);
            const uz: number = f(p1z - p0z);
            const vx: number = f(p2x - p0x);
            const vy: number = f(p2y - p0y);
            const vz: number = f(p2z - p0z);

            let nx: number = f(f(uy * vz) - f(uz * vy));
            let ny: number = f(f(uz * vx) - f(ux * vz));
            let nz: number = f(f(ux * vy) - f(uy * vx));

            const norm: number = f(Math.sqrt(f(f(f(nx * nx) + f(ny * ny)) + f(nz * nz))));
            if (norm <= Math.fround(1e-12)) {
                nx = 0.0;
                ny = 0.0;
                nz = 1.0;
            } else {
                nx = f(nx / norm);
                ny = f(ny / norm);
                nz = f(nz / norm);
            }

            const outward: number = f(f(f(nx * cx) + f(ny * cy)) + f(nz * cz));
            if (outward < 0.0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }

            const sx: number = f(cx + f(nx * epsilon));
            const sy: number = f(cy + f(ny * epsilon));
            const sz: number = f(cz + f(nz * epsilon));
            const ex: number = f(sx + f(nx * length));
            const ey: number = f(sy + f(ny * length));
            const ez: number = f(sz + f(nz * length));

            lines[out++] = sx;
            lines[out++] = sy;
            lines[out++] = sz;
            lines[out++] = ex;
            lines[out++] = ey;
            lines[out++] = ez;
        }

        return lines;
    }

    private static buildUniformColors(vertexCountLocal: number, rgb: readonly number[]): Float32Array {
        const colors = new Float32Array(vertexCountLocal * 3);
        for (let i = 0; i < vertexCountLocal; i++) {
            const base: number = i * 3;
            colors[base] = rgb[0]!;
            colors[base + 1] = rgb[1]!;
            colors[base + 2] = rgb[2]!;
        }
        return colors;
    }
}
