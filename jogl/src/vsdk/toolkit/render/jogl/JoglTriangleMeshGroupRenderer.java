//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

import javax.media.opengl.GL2;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

public class JoglTriangleMeshGroupRenderer extends JoglRenderer {

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    draw(GL2 gl, TriangleMeshGroup meshGroup, RendererConfiguration quality) {
        TriangleMesh mesh = null;
        int i;

        for ( i = 0; i < meshGroup.getMeshes().size(); i++ ) {
            mesh = meshGroup.getMeshes().get(i);
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

    public static void
    drawWithVertexArrays(GL2 gl, TriangleMeshGroup meshGroup, RendererConfiguration quality) {
        TriangleMesh mesh = null;
        int i;

        for ( i = 0; i < meshGroup.getMeshes().size(); i++ ) {
            mesh = meshGroup.getMeshes().get(i);
            boolean c = quality.isSelectionCornersSet()?true:false;
            quality.setSelectionCorners(false);
            JoglTriangleMeshRenderer.drawWithVertexArrays(gl, mesh, quality, false);
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
    public static void drawWithSelection(GL2 gl, TriangleMeshGroup meshGroup,
                                         RendererConfiguration quality,
                                         int selectedMesh,
                                         ArrayList<int[]> selectedTriangles) {
        ;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
