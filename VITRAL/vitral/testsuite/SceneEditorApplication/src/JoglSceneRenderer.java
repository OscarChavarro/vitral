import java.util.Iterator;

import javax.media.opengl.GL;

import vsdk.toolkit.environment.geometry.RayableObject;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;

public class JoglSceneRenderer
{
    public static void draw(GL gl, Scene s)
    {
        if ( s.showCorridor ) {
            s.corridor.drawGL(gl);
    }
        for ( Iterator i = s.things.iterator(); i.hasNext(); ) {
            JoglGeometryRenderer.draw(gl,
                ((RayableObject)i.next()).getGeometry());
    }
    }
}
