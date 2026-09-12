import {
    Arrow,
    Camera,
    ColorRgb,
    Light,
    Matrix4x4d,
    RayGizmo,
    RendererConfiguration,
    SimpleMaterial,
    SimpleScene,
    Sphere,
    Vector3Dd,
    type Geometry,
} from "@vitral/base";
import { WebGLArrowRenderer } from "./WebGLArrowRenderer.js";
import { WebGLCameraRenderer } from "./WebGLCameraRenderer.js";
import { WebGLRendererConfigurationShaderSelector } from "./WebGLRendererConfigurationShaderSelector.js";
import { WebGLSphereRenderer } from "./WebGLSphereRenderer.js";

interface IndicatorResources {
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    normalBuffer: WebGLBuffer;
    uvBuffer: WebGLBuffer;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4RayGizmoRenderer`.

Renders a {@link RayGizmo} as a lit arrow (cylinder shaft + cone head) using
WebGL2 shaders.

The arrow mesh is tessellated procedurally around the +Z axis.  The
`SimpleBody` transform stored inside the gizmo maps +Z to the actual
ray direction and translates the origin to the ray position.

When the gizmo has a current `RaySnapshot`:
  - rays[i] and intersections[i] are always parallel lists of the same size.
  - If a bounce hit geometry (intersections[i] != null), the arrow is Z-scaled
    so its tip reaches the surface exactly.
  - If a bounce missed all geometry, the full-length arrow is drawn and three
    dot markers are placed beyond its tip as "ellipsis" indicators.
  - Reflection bounces (index >= 1) are drawn in a distinct blue material.

Arrow and sphere rendering are delegated to {@link WebGLArrowRenderer} and
{@link WebGLSphereRenderer} respectively.  This class only owns the indicator
fin mesh and the frame-level orchestration.

Usage (render loop, once per frame):
```
    gizmo.acquireSnapshot();                  // apply pending network update
    await WebGLRayGizmoRenderer.draw(gl, gizmo, camera, lights);
```

The runtime boundaries are this package's recurring ones: the indicator mesh is
held per `WebGL2RenderingContext` in a `WeakMap` instead of in static fields,
since a page can own several contexts; a GLSL source arrives over `fetch`, so
`draw` is asynchronous; and `glGetUniformLocation`'s negative sentinel becomes
`null`. Java's `glPolygonMode` call before drawing the fin has no counterpart,
because WebGL rasterizes filled polygons and nothing else.
*/
export class WebGLRayGizmoRenderer {
    private static readonly IND_OUTER_R = 0.65;
    private static readonly IND_INNER_R = 0.17;
    private static readonly IND_HALF_W = 0.12;
    private static readonly IND_TIP_Z = 0.3;

    private static readonly resources = new WeakMap<WebGL2RenderingContext, IndicatorResources>();

    private constructor() {}

    /**
    Draws the ray gizmo for the current frame.  Must be called after
    `RayGizmo.acquireSnapshot()` has been called for this frame.

    @param gl     WebGL2 context
    @param gizmo  gizmo to draw
    @param camera active camera
    @param lights scene lights (only the first is used for sphere shading)
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        gizmo: RayGizmo | null,
        camera: Camera | null,
        lights: readonly (Light | null)[] | null,
    ): Promise<void> {
        if (gizmo === null || camera === null) {
            return;
        }
        if (lights === null || lights.length === 0) {
            return;
        }
        if (!gizmo.isVisible()) {
            return;
        }

        const indicator: IndicatorResources = WebGLRayGizmoRenderer.ensureMesh(gl);

        const quality: RendererConfiguration = WebGLRayGizmoRenderer.buildSurfaceQuality();
        const primaryModelMatrix: Matrix4x4d = gizmo.getBody().getTransformationMatrix();
        const projection: Matrix4x4d = WebGLCameraRenderer.activate(gl, camera);

        const scene: SimpleScene = gizmo.buildScene();

        const bodies = scene.getSimpleBodies();
        for (let i = 0; i < bodies.size(); i++) {
            const body = bodies.get(i);
            const geom: Geometry | null = body.getGeometry();
            const modelMatrix: Matrix4x4d = body.getTransformationMatrix();
            const material: SimpleMaterial | null = body.getMaterial();

            if (geom instanceof Arrow) {
                await WebGLArrowRenderer.draw(gl, geom, modelMatrix, projection, camera, lights, material, quality);
            } else if (geom instanceof Sphere) {
                await WebGLSphereRenderer.draw(
                    gl,
                    geom,
                    camera,
                    lights[0]!,
                    material,
                    quality,
                    null,
                    null,
                    modelMatrix,
                    16,
                    12,
                );
            }
        }

        await WebGLRayGizmoRenderer.drawIndicator(
            gl,
            indicator,
            gizmo.getRotationAngleInRadians(),
            primaryModelMatrix,
            projection,
            camera,
            lights,
            quality,
        );

        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        WebGLArrowRenderer.dispose(gl);

        const resources = WebGLRayGizmoRenderer.resources.get(gl);
        if (resources === undefined) {
            return;
        }

        gl.deleteBuffer(resources.positionBuffer);
        gl.deleteBuffer(resources.normalBuffer);
        gl.deleteBuffer(resources.uvBuffer);
        gl.deleteVertexArray(resources.vertexArray);

        WebGLRayGizmoRenderer.resources.delete(gl);
    }

    private static buildSurfaceQuality(): RendererConfiguration {
        const quality = new RendererConfiguration();
        quality.setSurfaces(true);
        quality.setWires(false);
        quality.setPoints(false);
        quality.setTexture(false);
        quality.setBumpMap(false);
        return quality;
    }

    private static async drawIndicator(
        gl: WebGL2RenderingContext,
        indicator: IndicatorResources,
        rollAngleRadians: number,
        arrowModelMatrix: Matrix4x4d,
        projection: Matrix4x4d,
        camera: Camera,
        lights: readonly (Light | null)[],
        quality: RendererConfiguration,
    ): Promise<void> {
        const rollRotation: Matrix4x4d = new Matrix4x4d().axisRotation(rollAngleRadians, 0, 0, 1);
        const indicatorModelMatrix: Matrix4x4d = arrowModelMatrix.multiply(rollRotation);
        const indicatorMvp: Matrix4x4d = projection.multiply(indicatorModelMatrix);
        const indicatorModelIt: Matrix4x4d = indicatorModelMatrix.invert().transpose();

        const program: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
            gl,
            quality,
            false,
            false,
        );

        WebGLRayGizmoRenderer.configureProgram(
            gl,
            program,
            indicatorMvp,
            indicatorModelMatrix,
            indicatorModelIt,
            camera,
            lights,
            WebGLRayGizmoRenderer.indicatorMaterial(),
            quality,
        );

        gl.enable(gl.DEPTH_TEST);
        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
        gl.disable(gl.CULL_FACE);

        gl.bindVertexArray(indicator.vertexArray);
        gl.drawArrays(gl.TRIANGLES, 0, 3);
        gl.bindVertexArray(null);

        WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
    }

    private static ensureMesh(gl: WebGL2RenderingContext): IndicatorResources {
        const existing = WebGLRayGizmoRenderer.resources.get(gl);
        if (existing !== undefined) {
            return existing;
        }

        return WebGLRayGizmoRenderer.uploadIndicatorMesh(gl);
    }

    private static uploadIndicatorMesh(gl: WebGL2RenderingContext): IndicatorResources {
        // Fin triangle pointing in +X at the arrow base (z=0).
        // P0=tip, P1=base-left, P2=base-right.
        const positions = new Float32Array([
            WebGLRayGizmoRenderer.IND_OUTER_R,
            0.0,
            WebGLRayGizmoRenderer.IND_TIP_Z,
            WebGLRayGizmoRenderer.IND_INNER_R,
            -WebGLRayGizmoRenderer.IND_HALF_W,
            0.0,
            WebGLRayGizmoRenderer.IND_INNER_R,
            WebGLRayGizmoRenderer.IND_HALF_W,
            0.0,
        ]);

        const normals: Float32Array = WebGLRayGizmoRenderer.computeNormals();
        const uvs = new Float32Array([0.5, 1.0, 0.0, 0.0, 1.0, 0.0]);

        const vertexArray = gl.createVertexArray();
        const positionBuffer = gl.createBuffer();
        const normalBuffer = gl.createBuffer();
        const uvBuffer = gl.createBuffer();
        if (vertexArray === null || positionBuffer === null || normalBuffer === null || uvBuffer === null) {
            throw new Error("Failed to create ray gizmo indicator buffers");
        }

        gl.bindVertexArray(vertexArray);
        WebGLRayGizmoRenderer.uploadBuffer(gl, positionBuffer, 0, 3, positions);
        WebGLRayGizmoRenderer.uploadBuffer(gl, normalBuffer, 1, 3, normals);
        WebGLRayGizmoRenderer.uploadBuffer(gl, uvBuffer, 2, 2, uvs);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);

        const resources: IndicatorResources = { vertexArray, positionBuffer, normalBuffer, uvBuffer };
        WebGLRayGizmoRenderer.resources.set(gl, resources);
        return resources;
    }

    private static computeNormals(): Float32Array {
        const ax: number = WebGLRayGizmoRenderer.IND_INNER_R - WebGLRayGizmoRenderer.IND_OUTER_R;
        const ay: number = -WebGLRayGizmoRenderer.IND_HALF_W;
        const az: number = -WebGLRayGizmoRenderer.IND_TIP_Z;
        const bx: number = WebGLRayGizmoRenderer.IND_INNER_R - WebGLRayGizmoRenderer.IND_OUTER_R;
        const by: number = WebGLRayGizmoRenderer.IND_HALF_W;
        const bz: number = -WebGLRayGizmoRenderer.IND_TIP_Z;
        let nx: number = ay * bz - az * by;
        let ny: number = az * bx - ax * bz;
        let nz: number = ax * by - ay * bx;
        const normalLength: number = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= normalLength;
        ny /= normalLength;
        nz /= normalLength;

        return new Float32Array([nx, ny, nz, nx, ny, nz, nx, ny, nz]);
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

        WebGLRayGizmoRenderer.setMatrix(gl, program, "modelViewLocal", modelViewLocal);
        WebGLRayGizmoRenderer.setMatrix(gl, program, "modelViewITLocal", modelViewITLocal);
        WebGLRayGizmoRenderer.setVector3(gl, program, "cameraPositionGlobal", camera.getPosition());

        let lightCount = 0;
        for (const light of lights) {
            if (light === null) {
                continue;
            }
            WebGLRayGizmoRenderer.setVector3(
                gl,
                program,
                "lightPositionsGlobal[" + lightCount + "]",
                light.getPosition(),
            );
            WebGLRayGizmoRenderer.setColor(gl, program, "lightColorsGlobal[" + lightCount + "]", light.getEmission());
            lightCount++;
        }
        WebGLRayGizmoRenderer.setInt(gl, program, "numberOfLights", lightCount);
        WebGLRayGizmoRenderer.setColor(gl, program, "ambientColor", material.getAmbient());
        WebGLRayGizmoRenderer.setColor(gl, program, "diffuseColor", material.getDiffuse());
        WebGLRayGizmoRenderer.setColor(gl, program, "specularColor", material.getSpecular());
        WebGLRayGizmoRenderer.setFloat(gl, program, "phongExponent", material.getPhongExponent());
        WebGLRayGizmoRenderer.setInt(gl, program, "withTexture", 0);
        WebGLRayGizmoRenderer.setInt(gl, program, "withBumpMap", 0);
    }

    private static indicatorMaterial(): SimpleMaterial {
        let m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.3, 0.3, 0.0));
        m = m.withDiffuse(new ColorRgb(1.0, 0.9, 0.0));
        m = m.withSpecular(new ColorRgb(1.0, 1.0, 0.8));
        m = m.withPhongExponent(64.0);
        return m;
    }

    private static uploadBuffer(
        gl: WebGL2RenderingContext,
        buffer: WebGLBuffer,
        attrib: number,
        size: number,
        data: Float32Array,
    ): void {
        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(gl.ARRAY_BUFFER, data, gl.STATIC_DRAW);
        gl.enableVertexAttribArray(attrib);
        gl.vertexAttribPointer(attrib, size, gl.FLOAT, false, 0, 0);
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
}
