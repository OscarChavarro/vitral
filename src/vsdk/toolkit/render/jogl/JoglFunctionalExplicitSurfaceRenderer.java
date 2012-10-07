//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 15 2007 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Basic Java classes

// JOGL classes
import javax.media.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.geometry.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.Camera;

public class JoglFunctionalExplicitSurfaceRenderer extends JoglRenderer {

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    draw(GL2 gl, FunctionalExplicitSurface surface, Camera camera, RendererConfiguration quality) {
        TriangleMesh mesh;
        mesh = surface.getInternalTriangleMesh();
        JoglTriangleMeshRenderer.draw(gl, mesh, quality, false);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
