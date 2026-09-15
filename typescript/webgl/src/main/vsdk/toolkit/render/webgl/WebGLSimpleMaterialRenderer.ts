import { SimpleMaterial } from "@vitral/base";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4SimpleMaterialRenderer`.

Under the core profile there is no fixed-function material state, so Java
keeps the active material itself and the renderers that shade a surface read
it back: `activate` stores a copy, `null` restores the default material, and
`getActiveMaterial` answers another copy, so that no caller can reach the
stored one.

The one runtime boundary is the one `WebGLLightRenderer` records for the
active lights: Java keeps the material in a static field, because a JOGL
application owns a single `GL4` context for its whole life, while a page can
hold several contexts, so the active material is kept per context in a
`WeakMap`. A context that has activated nothing answers the default material,
as Java's static initializer does.
*/
export class WebGLSimpleMaterialRenderer {
    private static readonly DEFAULT_MATERIAL = new SimpleMaterial();
    private static readonly activeMaterials = new WeakMap<WebGL2RenderingContext, SimpleMaterial>();

    private constructor() {}

    public static activate(gl: WebGL2RenderingContext, material: SimpleMaterial | null): void {
        if (material === null) {
            WebGLSimpleMaterialRenderer.activeMaterials.set(gl, WebGLSimpleMaterialRenderer.DEFAULT_MATERIAL);
            return;
        }
        WebGLSimpleMaterialRenderer.activeMaterials.set(gl, new SimpleMaterial(material));
    }

    public static getActiveMaterial(gl: WebGL2RenderingContext): SimpleMaterial {
        const activeMaterial: SimpleMaterial =
            WebGLSimpleMaterialRenderer.activeMaterials.get(gl) ?? WebGLSimpleMaterialRenderer.DEFAULT_MATERIAL;
        return new SimpleMaterial(activeMaterial);
    }
}
