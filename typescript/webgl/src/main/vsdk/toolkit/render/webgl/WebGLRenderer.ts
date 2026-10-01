import { WebGLArrowRenderer } from "./WebGLArrowRenderer.js";
import { WebGLCameraRenderer } from "./WebGLCameraRenderer.js";
import { WebGLColorDepthImageRenderer } from "./WebGLColorDepthImageRenderer.js";
import { WebGLColoredPrimitiveRenderer } from "./WebGLColoredPrimitiveRenderer.js";
import { WebGLFrameBufferReader } from "./WebGLFrameBufferReader.js";
import { WebGLGeometryRenderer } from "./WebGLGeometryRenderer.js";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";
import { WebGLMeshRenderer } from "./WebGLMeshRenderer.js";
import { WebGLMinMaxRenderer } from "./WebGLMinMaxRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4Renderer`.

Java's renderers extend it to share its static services; the WebGL renderers
are classes of static methods that do not need the inheritance, so only the
services are kept.
*/
export class WebGLRenderer {
    protected constructor() {}

    /**
    @return true: a WebGL 2 context is required to create any renderer input
    */
    public static verifyOpenGLAvailability(): boolean {
        return true;
    }

    /**
    Releases every WebGL object (programs, buffers, textures) cached by the
    WebGL renderers for a context, so a new context can create them again. It
    must be called when the context is about to be discarded.

    @param gl WebGL context
    */
    public static disposeAll(gl: WebGL2RenderingContext): void {
        WebGLGeometryRenderer.dispose(gl);
        WebGLMeshRenderer.dispose(gl);
        WebGLColoredPrimitiveRenderer.release(gl);
        WebGLLineRenderer.release(gl);
        WebGLMinMaxRenderer.dispose(gl);
        WebGLCameraRenderer.dispose(gl);
        WebGLArrowRenderer.dispose(gl);
        WebGLImageRenderer.dispose(gl);
        void WebGLColorDepthImageRenderer.dispose(gl);
        WebGLFrameBufferReader.release(gl);
    }
}
