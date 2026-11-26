import {
    ArrayList,
    Vector3Dd,
    type Prism,
    type Camera,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "../WebGLGeometryRenderer.js";
import { WebGLMeshBuilder } from "../WebGLMeshBuilder.js";
import type { WebGLMesh } from "../WebGLMeshRenderer.js";

/**
Port of `OpenGL4PrismRenderer` of the C++ port (`cpp/opengl4`), the
counterpart of `WebGLTubeRenderer` for a `Prism`.

Renders a `Prism` with the WebGL pipeline: it is tessellated by `buildMesh`
into one quadrilateral per wall (with the outward normal of its edge of the
base) and the two caps, triangulated by ear clipping
(`Prism.triangulateBase`); the mesh is cached by `WebGLGeometryRenderer`,
identified by the height and the vertices of the base.

Drawing is asynchronous because a GLSL source arrives over `fetch`.
*/
export class WebGLPrismRenderer {
    private constructor() {}

    /**
    Draws the prism, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        prism: Prism,
        camera: Camera | null,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLGeometryRenderer.draw(
            gl,
            prism,
            camera,
            lights,
            material,
            quality,
            textureMap,
            normalMap,
            localTransform,
        );
    }

    /**
    C++ formats the numbers with `%.17g`; the shortest round trip
    representation of JavaScript identifies the same values.
    @param prism a prism
    @return a string that identifies the mesh of the prism
    */
    public static meshKey(prism: Prism): string {
        const base = prism.getBase();
        let key = "prism/" + prism.getHeight() + "/" + base.size();
        for (let i = 0; i < base.size(); i++) {
            key = key + "/" + base.get(i).x + "/" + base.get(i).y;
        }
        return key;
    }

    /**
    @param prism prism to tessellate
    @return the mesh of the walls and the caps of the prism, or null if its
    parameters are not valid
    */
    public static buildMesh(prism: Prism): WebGLMesh | null {
        const triangles = new ArrayList<number>();
        if (!prism.hasValidParameters() || prism.triangulateBase(triangles) <= 0) {
            return null;
        }
        const base = prism.getBase();
        const n = base.size();
        const h = prism.getHeight();
        const r = prism.getCircumradius();
        const builder = new WebGLMeshBuilder(Math.max(2 * r, h));

        // Texture coordinates as `Prism.doExtraInformation`: the caps are
        // mapped from the box of the base, the walls around the perimeter
        let minX = base.get(0).x;
        let minY = base.get(0).y;
        let maxX = minX;
        let maxY = minY;
        for (let i = 1; i < n; i++) {
            minX = Math.min(minX, base.get(i).x);
            minY = Math.min(minY, base.get(i).y);
            maxX = Math.max(maxX, base.get(i).x);
            maxY = Math.max(maxY, base.get(i).y);
        }
        const width = maxX - minX;
        const depth = maxY - minY;

        //- Walls: the base is counterclockwise, the exterior on the right -
        for (let i = 0; i < n; i++) {
            const a = base.get(i);
            const b = base.get((i + 1) % n);
            const ex = b.x - a.x;
            const ey = b.y - a.y;
            const length = Math.sqrt(ex * ex + ey * ey);
            const normal = new Vector3Dd(ey / length, -ex / length, 0);
            const u0 = i / n;
            const u1 = (i + 1) / n;
            builder.addQuad(
                new Vector3Dd(a.x, a.y, 0),
                normal,
                u0,
                0,
                new Vector3Dd(b.x, b.y, 0),
                normal,
                u1,
                0,
                new Vector3Dd(b.x, b.y, h),
                normal,
                u1,
                1,
                new Vector3Dd(a.x, a.y, h),
                normal,
                u0,
                1,
            );
        }

        //- Caps -----------------------------------------------------------
        const up = new Vector3Dd(0, 0, 1);
        const down = new Vector3Dd(0, 0, -1);
        for (let i = 0; i < triangles.size(); i += 3) {
            const a = base.get(triangles.get(i));
            const b = base.get(triangles.get(i + 1));
            const c = base.get(triangles.get(i + 2));
            const ua = (a.x - minX) / width;
            const va = (a.y - minY) / depth;
            const ub = (b.x - minX) / width;
            const vb = (b.y - minY) / depth;
            const uc = (c.x - minX) / width;
            const vc = (c.y - minY) / depth;
            builder.addTriangle(
                new Vector3Dd(a.x, a.y, h),
                up,
                ua,
                va,
                new Vector3Dd(b.x, b.y, h),
                up,
                ub,
                vb,
                new Vector3Dd(c.x, c.y, h),
                up,
                uc,
                vc,
            );
            builder.addTriangle(
                new Vector3Dd(a.x, a.y, 0),
                down,
                ua,
                va,
                new Vector3Dd(c.x, c.y, 0),
                down,
                uc,
                vc,
                new Vector3Dd(b.x, b.y, 0),
                down,
                ub,
                vb,
            );
        }
        return builder.build();
    }
}
