package vsdk.toolkit.render.androidgles20;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

public class AndroidGLES20TriangleMeshGroupRenderer 
    extends AndroidGLES20Renderer {
    
    public static void draw(TriangleMeshGroup g, 
        Camera c, RendererConfiguration q)
    {
        int i;
        
        for ( i = 0; i < g.getMeshes().size(); i++ ) {
            AndroidGLES20TriangleMeshRenderer.draw(g.getMeshAt(i), c, q);
        }
    }
    
    public static void drawWithDisplayList(TriangleMeshGroup g, 
        Camera c, RendererConfiguration q)
    {
        int i;

        AndroidGLES20DisplayList displayList;

        if ( isObjectRegisteredWithADisplayList(g) ) {
            executeCompiledDisplayList(g);
            return;
        }

        displayList = new AndroidGLES20DisplayList(q);
        for ( i = 0; i < g.getMeshes().size(); i++ ) {
            AndroidGLES20TriangleMeshRenderer.drawWithDisplayListCompiling(
                g.getMeshAt(i), c, q, displayList);

            //if ( !displayList.isEmpty() ) {
            registerObjectWithADisplayList(g, displayList);
            //}
        }
        
    }

}
