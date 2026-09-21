package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.render.RenderingElement;

public abstract class Jogl4Renderer extends RenderingElement {
    public static boolean verifyOpenGLAvailability()
    {
        return true;
    }

    /**
    Releases every OpenGL object (programs, buffers, textures) cached by the
    GL4 renderers, so a new context can create them again. It must be called
    with the context current, when it is about to be destroyed (i.e. from
    `GLEventListener.dispose`).

    @param gl OpenGL context
    */
    public static void disposeAll(GL4 gl)
    {
        Jogl4GeometryRenderer.dispose(gl);
        Jogl4MeshRenderer.dispose(gl);
        Jogl4ColoredPrimitiveRenderer.release(gl);
        Jogl4LineRenderer.release(gl);
        Jogl4MinMaxRenderer.dispose(gl);
        Jogl4CameraRenderer.dispose(gl);
        Jogl4ArrowRenderer.dispose(gl);
        Jogl4ImageRenderer.dispose(gl);
    }
}
