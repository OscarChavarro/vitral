package vsdk.toolkit.render.jogl;

// Basic Java classes

// JOGL classes
import com.jogamp.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.Camera;

public class Jogl2FunctionalExplicitSurfaceRenderer extends Jogl2Renderer {

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    draw(GL2 gl, FunctionalExplicitSurface surface, Camera camera, RendererConfiguration quality) {
        TriangleMesh mesh;
        mesh = surface.getInternalTriangleMesh();
        Jogl2TriangleMeshRenderer.draw(gl, mesh, quality, false);
    }
}
