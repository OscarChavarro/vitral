import java.util.Iterator;

import javax.media.opengl.GL;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.RayableObject;
import vsdk.toolkit.render.jogl.JoglSimpleBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCubemapBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;

public class JoglSceneRenderer
{
    public static void draw(GL gl, Scene s)
    {
        switch ( s.selectedBackground ) {
          case 1:
            JoglCubemapBackgroundRenderer.draw(gl, s.cubemapBackground);
            break;
          case 0: default:
            JoglSimpleBackgroundRenderer.draw(gl, s.simpleBackground);
            break;
        }

        JoglCameraRenderer.activateGL(gl, s.activeCamera);

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glLoadIdentity();

        if ( s.showCorridor ) {
            s.corridor.drawGL(gl);
        }

        int j = 0;
        RayableObject gi;
        Vector3D p;
        Vector3D scale;

        if ( s.lights.size() > 0 ) {
            gl.glEnable(gl.GL_LIGHTING);
    }
    else {
            gl.glDisable(gl.GL_LIGHTING);
    }

        for ( Iterator i = s.lights.iterator(); i.hasNext(); j++ ) {
            Light l = (Light)i.next();
            JoglLightRenderer.activate(gl, l);
        }

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

            gl.glColor3d(1, 1, 1);
            JoglMaterialRenderer.activate(gl, gi.getMaterial());
            JoglGeometryRenderer.draw(gl, gi.getGeometry(),
                                      s.activeCamera, quality);

            gl.glPopMatrix();

        }

        for ( Iterator i = s.lights.iterator(); i.hasNext(); j++ ) {
            Light l = (Light)i.next();
            JoglLightRenderer.draw(gl, l);
        }

    }
}
