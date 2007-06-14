import java.util.Iterator;

import javax.media.opengl.GL;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.geometry.RayableObject;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;

public class JoglSceneRenderer
{
    public static void draw(GL gl, Scene s)
    {
        if ( s.showCorridor ) {
            s.corridor.drawGL(gl);
    }

        int j = 0;
        RayableObject gi;
        Vector3D p;
        Vector3D scale;

        for ( Iterator i = s.things.iterator(); i.hasNext(); j++ ) {
            QualitySelection quality;
            gi = ((RayableObject)i.next());
            p = gi.getPosition();
            scale = gi.getScale();

            quality = new QualitySelection();
            if ( j == s.selectedThingIndex ) {
                quality.setSurfaces(true);
                quality.setWires(false);
        }
        else {
                quality.setSurfaces(false);
                quality.setWires(true);
        }

            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glTranslated(p.x, p.y, p.z);
            JoglMatrixRenderer.activateGL(gl, gi.getRotation());

            gl.glScaled(scale.x, scale.y, scale.z);

            JoglGeometryRenderer.draw(gl,
                gi.getGeometry(),
        s.activeCamera, quality);

            gl.glPopMatrix();

    }
    }
}
