import {
    Camera,
    ColorRgb,
    Light,
    Matrix4x4d,
    MicroFacetedMaterial,
    RendererConfiguration,
    SimpleMaterial,
    Sphere,
    Vector3Dd,
    type RGBImageUncompressed,
} from "@vitral/base";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";
import { WebGLMinMaxRenderer } from "./WebGLMinMaxRenderer.js";
import { WebGLRendererConfigurationShaderSelector } from "./WebGLRendererConfigurationShaderSelector.js";

interface MeshData {
    positions: Float32Array;
    normals: Float32Array;
    uvs: Float32Array;
    tangents: Float32Array;
    biNormals: Float32Array;
    vertexCount: number;
}

interface SphereResources {
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    normalBuffer: WebGLBuffer;
    uvBuffer: WebGLBuffer;
    tangentBuffer: WebGLBuffer;
    biNormalBuffer: WebGLBuffer;
    vertexCount: number;

    meshRadius: number;
    meshSlices: number;
    meshStacks: number;
    meshPositionsHost: Float32Array | null;
    meshNormalsHost: Float32Array | null;
    vertexNormalLinePositions: Float32Array | null;
    vertexNormalLineColors: Float32Array | null;
    triangleNormalLinePositions: Float32Array | null;
    triangleNormalLineColors: Float32Array | null;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4SphereRenderer`.

The mesh — `(stacks - 1) * slices * 2` triangles built from the `Sphere`'s own
`spherePosition` / `sphereNormal` / `sphereTangent` / `sphereBinormal`, with
the `1 - s` flip in the texture coordinate — is the Java one, and so are the
four passes over it (surfaces, wires, points, normal overlays), their
materials, their depth state, the rebuild condition of `ensureMesh`, the
uniform block, and the microfacet uniforms with their non-microfacet defaults.

The runtime boundaries are this package's recurring ones: per-process static GL
state is held per `WebGL2RenderingContext` in a `WeakMap`, since a page can own
several contexts; a texture is an object rather than an `int`, so Java's
`textureId > 0` test is a `!== null` test; a GLSL source arrives over `fetch`,
so every method that can reach a program is asynchronous; and
`glGetUniformLocation`'s negative sentinel becomes `null`.

Two are specific to the passes here, and are the same two recorded for the
`MeshExample` debugger renderer: WebGL has no `glPolygonMode`, so the wires
pass draws the three edges of every triangle as `LINES` instead of asking the
rasterizer to outline a filled primitive (and `POLYGON_OFFSET_LINE` is equally
absent, leaving the pass its depth state); and WebGL has no `glPointSize`, so
the point size travels to the vertex shader as the `pointSizeLocal` uniform
that `WebGLShaderPreprocessor` installs.
*/
export class WebGLSphereRenderer {
    private static readonly DEFAULT_SLICES = 32;
    private static readonly DEFAULT_STACKS = 16;
    private static readonly SURFACE_POLYGON_OFFSET_FACTOR = 1.0;
    private static readonly SURFACE_POLYGON_OFFSET_UNITS = 1.0;
    private static readonly VERTEX_NORMAL_SCALE = 0.1;
    private static readonly TRIANGLE_NORMAL_SCALE = 0.12;
    private static readonly NORMAL_START_EPSILON = 0.002;
    private static readonly NORMAL_LINE_DEPTH_BIAS_NDC = -1.0e-4;

    private static readonly DEFAULT_BUMP_SCALE = new Vector3Dd(1.0, 1.0, 1.0);
    private static readonly VERTEX_NORMAL_COLOR: readonly number[] = [1.0, 1.0, 0.0];
    private static readonly TRIANGLE_NORMAL_COLOR: readonly number[] = [0.0, 1.0, 1.0];

    private static readonly resources = new WeakMap<WebGL2RenderingContext, SphereResources>();

    private constructor() {}

    public static async draw(
        gl: WebGL2RenderingContext,
        sphere: Sphere | null,
        camera: Camera | null,
        light: Light | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null = null,
        normalMap: RGBImageUncompressed | null = null,
        modelViewLocal: Matrix4x4d = Matrix4x4d.identityMatrix(),
        slices: number = WebGLSphereRenderer.DEFAULT_SLICES,
        stacks: number = WebGLSphereRenderer.DEFAULT_STACKS,
    ): Promise<void> {
        if (sphere === null || camera === null || light === null || material === null || quality === null) {
            return;
        }

        const resources: SphereResources = WebGLSphereRenderer.ensureMesh(gl, sphere, slices, stacks);

        const hasTexture: boolean = textureMap !== null;
        const hasNormalMap: boolean = normalMap !== null;
        const textureId: WebGLTexture | null = hasTexture ? WebGLImageRenderer.activate(gl, textureMap) : null;
        const normalMapId: WebGLTexture | null =
            hasNormalMap && quality.isBumpMapSet() ? WebGLImageRenderer.activate(gl, normalMap) : null;

        const localTransform: Matrix4x4d = modelViewLocal;
        const modelViewProjection: Matrix4x4d = camera.calculateProjectionMatrix().multiply(localTransform);
        const modelViewITLocal: Matrix4x4d = localTransform.invert().transpose();

        if (quality.isSurfacesSet()) {
            const program: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                quality,
                hasTexture,
                normalMapId !== null,
            );
            WebGLSphereRenderer.configureProgram(
                gl,
                program,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                light,
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
            gl.polygonOffset(
                WebGLSphereRenderer.SURFACE_POLYGON_OFFSET_FACTOR,
                WebGLSphereRenderer.SURFACE_POLYGON_OFFSET_UNITS,
            );
            gl.enable(gl.CULL_FACE);
            gl.cullFace(gl.BACK);
            WebGLSphereRenderer.renderMesh(gl, resources);
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

            WebGLSphereRenderer.configureProgram(
                gl,
                wireProgram,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                light,
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
            WebGLSphereRenderer.renderWireMesh(gl, resources);

            WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if (quality.isPointsSet()) {
            const pointQuality = new RendererConfiguration();
            pointQuality.setTexture(false);
            pointQuality.setUseVertexColors(false);
            pointQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);

            const pointsProgram: WebGLProgram =
                await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                    gl,
                    pointQuality,
                    false,
                    false,
                );

            let pointMaterial = new SimpleMaterial(material);
            pointMaterial = pointMaterial.withAmbient(new ColorRgb(0, 0, 0));
            pointMaterial = pointMaterial.withDiffuse(new ColorRgb(1, 0, 0)); // #ff0000
            pointMaterial = pointMaterial.withSpecular(new ColorRgb(0, 0, 0));

            WebGLSphereRenderer.configureProgram(
                gl,
                pointsProgram,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                light,
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
            WebGLSphereRenderer.setFloat(gl, pointsProgram, "pointSizeLocal", 4.0);
            gl.bindVertexArray(resources.vertexArray);
            gl.drawArrays(gl.POINTS, 0, resources.vertexCount);
            gl.bindVertexArray(null);

            WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if (quality.isNormalsSet() || quality.isTrianglesNormalsSet()) {
            await WebGLSphereRenderer.drawNormalOverlays(gl, resources, quality, modelViewProjection);
        }

        if (quality.isBoundingVolumeSet()) {
            await WebGLMinMaxRenderer.draw(gl, sphere, camera, localTransform);
        }

        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
        gl.bindTexture(gl.TEXTURE_2D, null);
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        const resources = WebGLSphereRenderer.resources.get(gl);
        if (resources !== undefined) {
            gl.deleteBuffer(resources.positionBuffer);
            gl.deleteBuffer(resources.normalBuffer);
            gl.deleteBuffer(resources.uvBuffer);
            gl.deleteBuffer(resources.tangentBuffer);
            gl.deleteBuffer(resources.biNormalBuffer);
            gl.deleteVertexArray(resources.vertexArray);
            WebGLSphereRenderer.resources.delete(gl);
        }

        WebGLMinMaxRenderer.dispose(gl);
    }

    private static configureProgram(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        modelViewProjection: Matrix4x4d,
        modelViewLocal: Matrix4x4d,
        modelViewITLocal: Matrix4x4d,
        camera: Camera,
        light: Light,
        material: SimpleMaterial,
        quality: RendererConfiguration,
        textureId: WebGLTexture | null,
        normalMapId: WebGLTexture | null,
    ): void {
        const kd: ColorRgb = material.getDiffuse();
        WebGLRendererConfigurationShaderSelector.activateShader(
            gl,
            program,
            modelViewProjection,
            quality,
            kd.r(),
            kd.g(),
            kd.b(),
        );

        WebGLSphereRenderer.setMatrix(gl, program, "modelViewLocal", modelViewLocal);
        WebGLSphereRenderer.setMatrix(gl, program, "modelViewITLocal", modelViewITLocal);

        WebGLSphereRenderer.setVector3(gl, program, "cameraPositionGlobal", camera.getPosition());
        WebGLSphereRenderer.setVector3(gl, program, "lightPositionsGlobal[0]", light.getPosition());

        const lightColor: ColorRgb = light.getEmission();
        WebGLSphereRenderer.setColor(gl, program, "lightColorsGlobal[0]", lightColor);
        WebGLSphereRenderer.setInt(gl, program, "numberOfLights", 1);

        WebGLSphereRenderer.setColor(gl, program, "ambientColor", material.getAmbient());
        WebGLSphereRenderer.setColor(gl, program, "diffuseColor", material.getDiffuse());
        WebGLSphereRenderer.setColor(gl, program, "specularColor", material.getSpecular());
        WebGLSphereRenderer.setVector3(gl, program, "bumpScale", WebGLSphereRenderer.DEFAULT_BUMP_SCALE);
        WebGLSphereRenderer.setFloat(gl, program, "phongExponent", material.getPhongExponent());
        WebGLSphereRenderer.configureMicroFacetUniforms(gl, program, material);
        WebGLSphereRenderer.setInt(gl, program, "withTexture", quality.isTextureSet() && textureId !== null ? 1 : 0);
        WebGLSphereRenderer.setInt(gl, program, "withBumpMap", quality.isBumpMapSet() && normalMapId !== null ? 1 : 0);

        if (textureId !== null) {
            gl.activeTexture(gl.TEXTURE0);
            gl.bindTexture(gl.TEXTURE_2D, textureId);
        }

        if (normalMapId !== null) {
            gl.activeTexture(gl.TEXTURE1);
            gl.bindTexture(gl.TEXTURE_2D, normalMapId);
            gl.activeTexture(gl.TEXTURE0);
        }
    }

    private static setMatrix(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        name: string,
        matrix: Matrix4x4d,
    ): void {
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

    private static configureMicroFacetUniforms(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        material: SimpleMaterial,
    ): void {
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

        WebGLSphereRenderer.setFloat(gl, program, "cookRoughness", roughness);
        WebGLSphereRenderer.setFloat(gl, program, "cookAlpha", alpha);
        WebGLSphereRenderer.setFloat(gl, program, "cookKd", kd);
        WebGLSphereRenderer.setFloat(gl, program, "cookKs", ks);
        WebGLSphereRenderer.setColor(gl, program, "cookF0", fresnelF0);
        WebGLSphereRenderer.setColor(gl, program, "cookEta", eta);
        WebGLSphereRenderer.setColor(gl, program, "cookKappa", kappa);
        WebGLSphereRenderer.setInt(gl, program, "cookFresnelModel", fresnelModel);
        WebGLSphereRenderer.setInt(gl, program, "cookNdfModel", ndfModel);
        WebGLSphereRenderer.setInt(gl, program, "cookGeometryModel", geometryModel);
    }

    private static renderMesh(gl: WebGL2RenderingContext, resources: SphereResources): void {
        gl.bindVertexArray(resources.vertexArray);
        gl.drawArrays(gl.TRIANGLES, 0, resources.vertexCount);
        gl.bindVertexArray(null);
    }

    /**
    The stand-in for Java's `glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)` over
    the same mesh: `LINES` are drawn from the triangle vertices already in the
    vertex array, three edges per triangle, through the index buffer WebGL does
    have.
    */
    private static renderWireMesh(gl: WebGL2RenderingContext, resources: SphereResources): void {
        const indices = new Uint32Array((resources.vertexCount / 3) * 6);
        let out = 0;
        for (let triangle = 0; triangle * 3 < resources.vertexCount; triangle++) {
            const base: number = triangle * 3;
            indices[out++] = base;
            indices[out++] = base + 1;
            indices[out++] = base + 1;
            indices[out++] = base + 2;
            indices[out++] = base + 2;
            indices[out++] = base;
        }

        const indexBuffer = gl.createBuffer();
        if (indexBuffer === null) {
            return;
        }
        gl.bindVertexArray(resources.vertexArray);
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, indexBuffer);
        gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, indices, gl.STREAM_DRAW);
        gl.drawElements(gl.LINES, indices.length, gl.UNSIGNED_INT, 0);
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
        gl.deleteBuffer(indexBuffer);
    }

    private static ensureMesh(
        gl: WebGL2RenderingContext,
        sphere: Sphere,
        requestedSlices: number,
        requestedStacks: number,
    ): SphereResources {
        const slices: number = Math.max(12, requestedSlices);
        const stacks: number = Math.max(8, requestedStacks);
        const radius: number = sphere.getRadius();

        const existing = WebGLSphereRenderer.resources.get(gl);
        const needsRebuild: boolean =
            existing === undefined ||
            Math.abs(existing.meshRadius - radius) > 1e-9 ||
            existing.meshSlices !== slices ||
            existing.meshStacks !== stacks;

        if (!needsRebuild) {
            return existing!;
        }

        const mesh: MeshData = WebGLSphereRenderer.buildMesh(sphere, slices, stacks);
        const resources: SphereResources = WebGLSphereRenderer.uploadMesh(gl, mesh, existing);
        resources.meshPositionsHost = mesh.positions;
        resources.meshNormalsHost = mesh.normals;
        resources.vertexNormalLinePositions = null;
        resources.vertexNormalLineColors = null;
        resources.triangleNormalLinePositions = null;
        resources.triangleNormalLineColors = null;

        resources.meshRadius = radius;
        resources.meshSlices = slices;
        resources.meshStacks = stacks;
        return resources;
    }

    private static buildMesh(sphere: Sphere, slices: number, stacks: number): MeshData {
        const triangles: number = (stacks - 1) * slices * 2;
        const vertices: number = triangles * 3;

        const positions = new Float32Array(vertices * 3);
        const normals = new Float32Array(vertices * 3);
        const uvs = new Float32Array(vertices * 2);
        const tangents = new Float32Array(vertices * 3);
        const biNormals = new Float32Array(vertices * 3);

        let posIndex = 0;
        let uvIndex = 0;

        for (let i = 0; i < stacks - 1; i++) {
            const t0: number = i / (stacks - 1);
            const t1: number = (i + 1) / (stacks - 1);
            const phi0: number = Math.PI * t0 - Math.PI / 2;
            const phi1: number = Math.PI * t1 - Math.PI / 2;

            for (let j = 0; j < slices; j++) {
                const s0: number = j / slices;
                const s1: number = (j + 1) / slices;
                const theta0: number = 2 * Math.PI * s0;
                const theta1: number = 2 * Math.PI * s1;

                posIndex = WebGLSphereRenderer.addVertex(
                    sphere,
                    theta0,
                    phi0,
                    positions,
                    normals,
                    tangents,
                    biNormals,
                    posIndex,
                );
                uvIndex = WebGLSphereRenderer.addUv(s0, t0, uvs, uvIndex);

                posIndex = WebGLSphereRenderer.addVertex(
                    sphere,
                    theta0,
                    phi1,
                    positions,
                    normals,
                    tangents,
                    biNormals,
                    posIndex,
                );
                uvIndex = WebGLSphereRenderer.addUv(s0, t1, uvs, uvIndex);

                posIndex = WebGLSphereRenderer.addVertex(
                    sphere,
                    theta1,
                    phi1,
                    positions,
                    normals,
                    tangents,
                    biNormals,
                    posIndex,
                );
                uvIndex = WebGLSphereRenderer.addUv(s1, t1, uvs, uvIndex);

                posIndex = WebGLSphereRenderer.addVertex(
                    sphere,
                    theta0,
                    phi0,
                    positions,
                    normals,
                    tangents,
                    biNormals,
                    posIndex,
                );
                uvIndex = WebGLSphereRenderer.addUv(s0, t0, uvs, uvIndex);

                posIndex = WebGLSphereRenderer.addVertex(
                    sphere,
                    theta1,
                    phi1,
                    positions,
                    normals,
                    tangents,
                    biNormals,
                    posIndex,
                );
                uvIndex = WebGLSphereRenderer.addUv(s1, t1, uvs, uvIndex);

                posIndex = WebGLSphereRenderer.addVertex(
                    sphere,
                    theta1,
                    phi0,
                    positions,
                    normals,
                    tangents,
                    biNormals,
                    posIndex,
                );
                uvIndex = WebGLSphereRenderer.addUv(s1, t0, uvs, uvIndex);
            }
        }

        return { positions, normals, uvs, tangents, biNormals, vertexCount: vertices };
    }

    private static addVertex(
        sphere: Sphere,
        theta: number,
        phi: number,
        positions: Float32Array,
        normals: Float32Array,
        tangents: Float32Array,
        biNormals: Float32Array,
        index: number,
    ): number {
        const p: Vector3Dd = sphere.spherePosition(theta, phi);
        const n: Vector3Dd = sphere.sphereNormal(theta, phi);
        const tangent: Vector3Dd = sphere.sphereTangent(theta, phi);
        const biNormal: Vector3Dd = sphere.sphereBinormal(theta, phi);

        positions[index] = p.x();
        normals[index] = n.x();
        tangents[index] = tangent.x();
        biNormals[index] = biNormal.x();
        index++;

        positions[index] = p.y();
        normals[index] = n.y();
        tangents[index] = tangent.y();
        biNormals[index] = biNormal.y();
        index++;

        positions[index] = p.z();
        normals[index] = n.z();
        tangents[index] = tangent.z();
        biNormals[index] = biNormal.z();
        index++;

        return index;
    }

    private static addUv(s: number, t: number, uvs: Float32Array, index: number): number {
        uvs[index++] = 1.0 - s;
        uvs[index++] = t;
        return index;
    }

    private static uploadMesh(
        gl: WebGL2RenderingContext,
        mesh: MeshData,
        existing: SphereResources | undefined,
    ): SphereResources {
        let resources: SphereResources;
        if (existing !== undefined) {
            resources = existing;
        } else {
            const vertexArray = gl.createVertexArray();
            const positionBuffer = gl.createBuffer();
            const normalBuffer = gl.createBuffer();
            const uvBuffer = gl.createBuffer();
            const tangentBuffer = gl.createBuffer();
            const biNormalBuffer = gl.createBuffer();
            if (
                vertexArray === null ||
                positionBuffer === null ||
                normalBuffer === null ||
                uvBuffer === null ||
                tangentBuffer === null ||
                biNormalBuffer === null
            ) {
                throw new Error("Failed to create sphere renderer buffers");
            }
            resources = {
                vertexArray,
                positionBuffer,
                normalBuffer,
                uvBuffer,
                tangentBuffer,
                biNormalBuffer,
                vertexCount: 0,
                meshRadius: 0,
                meshSlices: 0,
                meshStacks: 0,
                meshPositionsHost: null,
                meshNormalsHost: null,
                vertexNormalLinePositions: null,
                vertexNormalLineColors: null,
                triangleNormalLinePositions: null,
                triangleNormalLineColors: null,
            };
            WebGLSphereRenderer.resources.set(gl, resources);
        }

        gl.bindVertexArray(resources.vertexArray);

        WebGLSphereRenderer.uploadFloatBuffer(gl, resources.positionBuffer, 0, 3, mesh.positions);
        WebGLSphereRenderer.uploadFloatBuffer(gl, resources.normalBuffer, 1, 3, mesh.normals);
        WebGLSphereRenderer.uploadFloatBuffer(gl, resources.uvBuffer, 2, 2, mesh.uvs);
        WebGLSphereRenderer.uploadFloatBuffer(gl, resources.tangentBuffer, 3, 3, mesh.tangents);
        WebGLSphereRenderer.uploadFloatBuffer(gl, resources.biNormalBuffer, 4, 3, mesh.biNormals);

        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);

        resources.vertexCount = mesh.vertexCount;
        return resources;
    }

    private static uploadFloatBuffer(
        gl: WebGL2RenderingContext,
        buffer: WebGLBuffer,
        attribIndex: number,
        coordinatesSize: number,
        data: Float32Array,
    ): void {
        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(gl.ARRAY_BUFFER, data, gl.STATIC_DRAW);
        gl.enableVertexAttribArray(attribIndex);
        gl.vertexAttribPointer(attribIndex, coordinatesSize, gl.FLOAT, false, 0, 0);
    }

    private static async drawNormalOverlays(
        gl: WebGL2RenderingContext,
        resources: SphereResources,
        quality: RendererConfiguration,
        modelViewProjection: Matrix4x4d,
    ): Promise<void> {
        if (resources.meshPositionsHost === null || resources.meshNormalsHost === null) {
            return;
        }

        gl.enable(gl.DEPTH_TEST);
        gl.depthMask(false);
        gl.depthFunc(gl.LEQUAL);
        gl.disable(gl.CULL_FACE);

        if (quality.isNormalsSet()) {
            if (resources.vertexNormalLinePositions === null || resources.vertexNormalLineColors === null) {
                const length: number = Math.max(1e-6, resources.meshRadius) * WebGLSphereRenderer.VERTEX_NORMAL_SCALE;
                const epsilon: number = Math.max(1e-6, resources.meshRadius) * WebGLSphereRenderer.NORMAL_START_EPSILON;
                resources.vertexNormalLinePositions = WebGLSphereRenderer.buildVertexNormalLinePositions(
                    resources,
                    length,
                    epsilon,
                );
                resources.vertexNormalLineColors = WebGLSphereRenderer.buildUniformColors(
                    resources.vertexNormalLinePositions.length / 3,
                    WebGLSphereRenderer.VERTEX_NORMAL_COLOR,
                );
            }
            await WebGLLineRenderer.drawLines(
                gl,
                modelViewProjection,
                resources.vertexNormalLinePositions,
                resources.vertexNormalLineColors,
                1.0,
                WebGLSphereRenderer.NORMAL_LINE_DEPTH_BIAS_NDC,
            );
        }

        if (quality.isTrianglesNormalsSet()) {
            if (resources.triangleNormalLinePositions === null || resources.triangleNormalLineColors === null) {
                const length: number = Math.max(1e-6, resources.meshRadius) * WebGLSphereRenderer.TRIANGLE_NORMAL_SCALE;
                const epsilon: number = Math.max(1e-6, resources.meshRadius) * WebGLSphereRenderer.NORMAL_START_EPSILON;
                resources.triangleNormalLinePositions = WebGLSphereRenderer.buildTriangleNormalLinePositions(
                    resources,
                    length,
                    epsilon,
                );
                resources.triangleNormalLineColors = WebGLSphereRenderer.buildUniformColors(
                    resources.triangleNormalLinePositions.length / 3,
                    WebGLSphereRenderer.TRIANGLE_NORMAL_COLOR,
                );
            }
            await WebGLLineRenderer.drawLines(
                gl,
                modelViewProjection,
                resources.triangleNormalLinePositions,
                resources.triangleNormalLineColors,
                1.0,
                WebGLSphereRenderer.NORMAL_LINE_DEPTH_BIAS_NDC,
            );
        }
    }

    private static buildVertexNormalLinePositions(
        resources: SphereResources,
        length: number,
        epsilon: number,
    ): Float32Array {
        const meshPositionsHost: Float32Array = resources.meshPositionsHost!;
        const meshNormalsHost: Float32Array = resources.meshNormalsHost!;
        const vertexCountLocal: number = meshPositionsHost.length / 3;
        const lines = new Float32Array(vertexCountLocal * 2 * 3);
        let out = 0;
        for (let i = 0; i < vertexCountLocal; i++) {
            const base: number = i * 3;
            const px: number = meshPositionsHost[base]!;
            const py: number = meshPositionsHost[base + 1]!;
            const pz: number = meshPositionsHost[base + 2]!;
            const nx: number = meshNormalsHost[base]!;
            const ny: number = meshNormalsHost[base + 1]!;
            const nz: number = meshNormalsHost[base + 2]!;

            const sx: number = px + nx * epsilon;
            const sy: number = py + ny * epsilon;
            const sz: number = pz + nz * epsilon;
            const ex: number = sx + nx * length;
            const ey: number = sy + ny * length;
            const ez: number = sz + nz * length;

            lines[out++] = sx;
            lines[out++] = sy;
            lines[out++] = sz;
            lines[out++] = ex;
            lines[out++] = ey;
            lines[out++] = ez;
        }
        return lines;
    }

    private static buildTriangleNormalLinePositions(
        resources: SphereResources,
        length: number,
        epsilon: number,
    ): Float32Array {
        const meshPositionsHost: Float32Array = resources.meshPositionsHost!;
        const triangleCount: number = meshPositionsHost.length / 9;
        const lines = new Float32Array(triangleCount * 2 * 3);
        let out = 0;

        for (let tri = 0; tri < triangleCount; tri++) {
            const base: number = tri * 9;
            const p0x: number = meshPositionsHost[base]!;
            const p0y: number = meshPositionsHost[base + 1]!;
            const p0z: number = meshPositionsHost[base + 2]!;
            const p1x: number = meshPositionsHost[base + 3]!;
            const p1y: number = meshPositionsHost[base + 4]!;
            const p1z: number = meshPositionsHost[base + 5]!;
            const p2x: number = meshPositionsHost[base + 6]!;
            const p2y: number = meshPositionsHost[base + 7]!;
            const p2z: number = meshPositionsHost[base + 8]!;

            const cx: number = (p0x + p1x + p2x) / 3.0;
            const cy: number = (p0y + p1y + p2y) / 3.0;
            const cz: number = (p0z + p1z + p2z) / 3.0;

            const ux: number = p1x - p0x;
            const uy: number = p1y - p0y;
            const uz: number = p1z - p0z;
            const vx: number = p2x - p0x;
            const vy: number = p2y - p0y;
            const vz: number = p2z - p0z;

            let nx: number = uy * vz - uz * vy;
            let ny: number = uz * vx - ux * vz;
            let nz: number = ux * vy - uy * vx;

            const norm: number = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (norm <= 1e-12) {
                nx = 0.0;
                ny = 0.0;
                nz = 1.0;
            } else {
                nx /= norm;
                ny /= norm;
                nz /= norm;
            }

            const outward: number = nx * cx + ny * cy + nz * cz;
            if (outward < 0.0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }

            const sx: number = cx + nx * epsilon;
            const sy: number = cy + ny * epsilon;
            const sz: number = cz + nz * epsilon;
            const ex: number = sx + nx * length;
            const ey: number = sy + ny * length;
            const ez: number = sz + nz * length;

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
