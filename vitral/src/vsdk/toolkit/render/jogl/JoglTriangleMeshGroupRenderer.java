//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

public class JoglTriangleMeshGroupRenderer extends JoglRenderer {

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    draw(GL gl, TriangleMeshGroup meshGroup, QualitySelection quality) {
        TriangleMesh mesh = null;
        Iterator<TriangleMesh> i;

        for ( i = meshGroup.getMeshes().iterator(); i.hasNext(); ) {
            mesh = (TriangleMesh)i.next();
            JoglTriangleMeshRenderer.draw(gl, mesh, quality, false);
        }
    }

    /**
    @todo program this!
    */
    public static void drawWithSelection(GL gl, TriangleMeshGroup meshGroup,
                                         QualitySelection quality,
                                         int selectedMesh,
                                         ArrayList<int[]> selectedTriangles) {
    ;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
