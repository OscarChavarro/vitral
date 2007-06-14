//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

public class JoglTriangleMeshGroupRenderer extends JoglRenderer {

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    draw(GL gl, TriangleMeshGroup meshGroup, RendererConfiguration quality) {
        TriangleMesh mesh = null;
        Iterator<TriangleMesh> i;

        for ( i = meshGroup.getMeshes().iterator(); i.hasNext(); ) {
            mesh = i.next();
            boolean c = quality.isSelectionCornersSet()?true:false;
            quality.setSelectionCorners(false);
            JoglTriangleMeshRenderer.draw(gl, mesh, quality, false);
            quality.setSelectionCorners(c);
        }
        if ( quality.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, mesh, quality);
        }
        if ( quality.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, meshGroup, quality);
        }
    }

    /**
    @todo program this!
    */
    public static void drawWithSelection(GL gl, TriangleMeshGroup meshGroup,
                                         RendererConfiguration quality,
                                         int selectedMesh,
                                         ArrayList<int[]> selectedTriangles) {
        ;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
