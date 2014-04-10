//===========================================================================
package vsdk.toolkit.render.androidgles20;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

/**
*/
public class AndroidGLES20TriangleMeshGroupRenderer {
    public static void draw(TriangleMeshGroup g, 
        Camera c, RendererConfiguration q)
    {
        int i;
        
        for ( i = 0; i < g.getMeshes().size(); i++ ) {
            AndroidGLES20TriangleMeshRenderer.draw(g.getMeshAt(i), c, q);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
