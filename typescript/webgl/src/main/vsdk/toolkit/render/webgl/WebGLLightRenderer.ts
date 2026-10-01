import {
    Camera,
    ColorRgb,
    LightGizmoOmniBillboard,
    LightGizmoStyle,
    Light,
    Matrix4x4d,
    PointLight,
    Vector3Dd,
    type Calligraphic2DBuffer,
} from "@vitral/base";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4LightRenderer`.

Both gizmo styles are the Java ones: the three-axis cross, and the omni
billboard whose line pattern comes from `LightGizmoOmniBillboard` and is mapped
onto the camera-facing plane. The screen-size calculation, including its
orthogonal and perspective branches, is `LightGizmoOmniBillboard`'s, shared
with the picking of lights.

Two runtime boundaries are crossed:

  - Java keeps the active-light table in a static `LinkedHashMap`, because a
    JOGL application owns a single `GL4` context for its whole life. A browser
    page can hold several independent contexts, so the table is held per
    context in a `WeakMap`, as the rest of `@vitral/webgl` already does. The
    insertion-ordered iteration a `LinkedHashMap` guarantees is what a JavaScript
    `Map` gives for free.
  - `Jogl4LineRenderer.drawLines` is asynchronous here, because a browser
    reaches a shader source over `fetch`, so `draw` is asynchronous too.

Java's overload that takes the loose `GL` interface and checks `gl.isGL4()`
before narrowing has no counterpart: WebGL2 is the only context this package
draws into, so `WebGL2RenderingContext` is already the narrowed type.
*/
export class WebGLLightRenderer {
    private static readonly activeLights = new WeakMap<WebGL2RenderingContext, Map<number, Light>>();
    private static readonly SELECTED_COLOR: ColorRgb = new ColorRgb(1, 1, 0);
    private static readonly LINE_WIDTH: number = 2.0;
    private static readonly SELECTED_LINE_WIDTH: number = 4.0;
    private static scale = 1.0;

    private constructor() {}

    public static activate(gl: WebGL2RenderingContext, light: Light | null): void {
        if (light === null) {
            return;
        }
        let table = WebGLLightRenderer.activeLights.get(gl);
        if (table === undefined) {
            table = new Map<number, Light>();
            WebGLLightRenderer.activeLights.set(gl, table);
        }
        table.set(light.getId(), light.copy());
    }

    /**
    Draws the gizmo of a light. A selected light is drawn in the selection
    color and with thicker lines, instead of with its own emission color
    (Java's overloads are one method with optional arguments).
    @param gl WebGL context
    @param light light to draw
    @param camera camera that views the light
    @param lightGizmoStyle shape of the gizmo
    @param selected true if the light is selected
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        light: Light | null,
        camera: Camera | null = null,
        lightGizmoStyle: LightGizmoStyle = LightGizmoStyle.CROSS,
        selected: boolean = false,
    ): Promise<void> {
        if (light === null) {
            return;
        }

        if (lightGizmoStyle === LightGizmoStyle.OMNI_BILLBOARD) {
            await WebGLLightRenderer.drawOmniBillboard(gl, light, camera, selected);
            return;
        }

        await WebGLLightRenderer.drawCross(gl, light, camera, selected);
    }

    public static getScale(): number {
        return WebGLLightRenderer.scale;
    }

    public static setScale(newScale: number): void {
        WebGLLightRenderer.scale = newScale;
    }

    public static getActiveLights(gl: WebGL2RenderingContext): Light[] {
        const table = WebGLLightRenderer.activeLights.get(gl);
        if (table === undefined || table.size === 0) {
            return [WebGLLightRenderer.defaultLight()];
        }

        const out: Light[] = [];
        for (const light of table.values()) {
            out.push(light.copy());
        }
        return out;
    }

    private static async drawCross(
        gl: WebGL2RenderingContext,
        light: Light,
        camera: Camera | null,
        selected: boolean,
    ): Promise<void> {
        const viewport = gl.getParameter(gl.VIEWPORT) as Int32Array;

        const viewportWidth: number = Math.max(viewport[2] ?? 1, 1);
        const viewportHeight: number = Math.max(viewport[3] ?? 1, 1);

        let modelViewProjection: Matrix4x4d = Matrix4x4d.identityMatrix();
        if (camera !== null) {
            camera.updateViewportResize(viewportWidth, viewportHeight);
            modelViewProjection = camera.calculateProjectionMatrix();
        }

        const halfAxisLength: number =
            WebGLLightRenderer.calculateHalfAxisLength(light, camera, viewportWidth, viewportHeight) *
            WebGLLightRenderer.scale;

        const p: Vector3Dd = light.getPosition();
        const c: ColorRgb = selected ? WebGLLightRenderer.SELECTED_COLOR : light.getEmission();

        const px: number = p.x();
        const py: number = p.y();
        const pz: number = p.z();
        const d: number = halfAxisLength;

        const positions = new Float32Array([
            px - d,
            py,
            pz,
            px + d,
            py,
            pz,

            px,
            py - d,
            pz,
            px,
            py + d,
            pz,

            px,
            py,
            pz - d,
            px,
            py,
            pz + d,
        ]);

        const colors: Float32Array = WebGLLightRenderer.buildUniformColorArray(c, positions.length / 3);

        await WebGLLineRenderer.drawLines(gl, modelViewProjection, positions, colors,
            selected ? WebGLLightRenderer.SELECTED_LINE_WIDTH : WebGLLightRenderer.LINE_WIDTH);
    }

    private static async drawOmniBillboard(
        gl: WebGL2RenderingContext,
        light: Light,
        camera: Camera | null,
        selected: boolean,
    ): Promise<void> {
        const viewport = gl.getParameter(gl.VIEWPORT) as Int32Array;

        const viewportWidth: number = Math.max(viewport[2] ?? 1, 1);
        const viewportHeight: number = Math.max(viewport[3] ?? 1, 1);

        if (camera === null) {
            await WebGLLightRenderer.drawCross(gl, light, null, selected);
            return;
        }

        camera.updateViewportResize(viewportWidth, viewportHeight);
        const modelViewProjection: Matrix4x4d = camera.calculateProjectionMatrix();

        const worldHalfSize: number =
            WebGLLightRenderer.calculateHalfAxisLength(light, camera, viewportWidth, viewportHeight) *
            WebGLLightRenderer.scale;
        const worldFullSize: number = 2.0 * worldHalfSize;

        const lightPosition: Vector3Dd = light.getPosition();
        const right: Vector3Dd = camera.getLeft().multiply(-1.0).normalized();
        const up: Vector3Dd = camera.getUp().normalized();

        const pattern: Calligraphic2DBuffer = LightGizmoOmniBillboard.createLinePattern();

        const lineCount: number = pattern.getNumLines();
        if (lineCount <= 0) {
            return;
        }

        const positions = new Float32Array(lineCount * 2 * 3);
        let write = 0;
        for (let i = 0; i < lineCount; i++) {
            const line: readonly Vector3Dd[] = pattern.get2DLine(i);

            const p0: Vector3Dd = WebGLLightRenderer.mapPatternPointToWorld(
                line[0]!,
                lightPosition,
                right,
                up,
                worldFullSize,
            );
            const p1: Vector3Dd = WebGLLightRenderer.mapPatternPointToWorld(
                line[1]!,
                lightPosition,
                right,
                up,
                worldFullSize,
            );

            positions[write++] = p0.x();
            positions[write++] = p0.y();
            positions[write++] = p0.z();
            positions[write++] = p1.x();
            positions[write++] = p1.y();
            positions[write++] = p1.z();
        }

        const colors: Float32Array = WebGLLightRenderer.buildUniformColorArray(
            selected ? WebGLLightRenderer.SELECTED_COLOR : light.getEmission(),
            positions.length / 3,
        );
        await WebGLLineRenderer.drawLines(gl, modelViewProjection, positions, colors,
            selected ? WebGLLightRenderer.SELECTED_LINE_WIDTH : WebGLLightRenderer.LINE_WIDTH);
    }

    private static mapPatternPointToWorld(
        point: Vector3Dd,
        center: Vector3Dd,
        right: Vector3Dd,
        up: Vector3Dd,
        worldSize: number,
    ): Vector3Dd {
        const localX: number = (point.x() - 0.5) * worldSize;
        const localY: number = (point.y() - 0.5) * worldSize;

        const rightContribution: Vector3Dd = right.multiply(localX);
        const upContribution: Vector3Dd = up.multiply(localY);

        return center.add(rightContribution).add(upContribution);
    }

    private static buildUniformColorArray(c: ColorRgb, vertexCount: number): Float32Array {
        const cr: number = c.r();
        const cg: number = c.g();
        const cb: number = c.b();

        const colors = new Float32Array(vertexCount * 3);
        for (let i = 0; i < vertexCount; i++) {
            const base: number = i * 3;
            colors[base] = cr;
            colors[base + 1] = cg;
            colors[base + 2] = cb;
        }
        return colors;
    }

    private static calculateHalfAxisLength(
        light: Light,
        camera: Camera | null,
        viewportWidth: number,
        viewportHeight: number,
    ): number {
        return LightGizmoOmniBillboard.calculateWorldHalfSize(camera,
            light.getPosition(), viewportWidth, viewportHeight);
    }

    private static defaultLight(): Light {
        const light: Light = new PointLight(new Vector3Dd(10, 10, 10), new ColorRgb(1, 1, 1));
        light.setId(0);
        return light;
    }
}
