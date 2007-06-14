import java.util.Iterator;

import javax.media.opengl.GL;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.scene.SimpleThing;
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

        JoglCameraRenderer.activate(gl, s.activeCamera);

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glLoadIdentity();

        if ( s.showCorridor ) {
            s.corridor.drawGL(gl);
        }

        SimpleThing gi;
        Vector3D p;
        Vector3D scale;

        if ( s.lights.size() > 0 ) {
            gl.glEnable(gl.GL_LIGHTING);
    }
    else {
            gl.glDisable(gl.GL_LIGHTING);
    }

        for ( Iterator i = s.lights.iterator(); i.hasNext(); ) {
            Light l = (Light)i.next();
            JoglLightRenderer.activate(gl, l);
        }

        int j = 0;
        for ( Iterator i = s.things.iterator(); i.hasNext(); j++ ) {
            QualitySelection quality;
            gi = ((SimpleThing)i.next());
            p = gi.getPosition();
            scale = gi.getScale();

            quality = new QualitySelection();
            quality.setSurfaces(true);
            quality.setWires(false);

            if ( j == s.selectedThingIndex ) {
                quality.setBoundingVolume(true);
            }
            else {
                quality.setBoundingVolume(false);
            }

            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glTranslated(p.x, p.y, p.z);
            JoglMatrixRenderer.activate(gl, gi.getRotation());

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

        // Draw reference grid plane
        if ( s.showGrid ) drawGridRectangle(gl);

    }

    private static void drawGridRectangle(GL gl)
    {
        int nx = 14; // Must be an even number
        int ny = 14; // Must be an even number
        double dx = 1.0;
        double dy = 1.0;
        int x, y;
        double minx = -(((double)nx)/2) * dx;
        double maxx = (((double)nx)/2) * dx;
        double miny = -(((double)ny)/2) * dy;
        double maxy = (((double)ny)/2) * dy;

        gl.glDisable(gl.GL_LIGHTING);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL.GL_LINES);
        gl.glColor3d(0.37, 0.37, 0.37);
        for ( x = 0; x <= nx; x++ ) {
            if ( x == nx/2 ) continue;
        gl.glVertex3d(minx + ((double)x)*dx, miny, 0);
        gl.glVertex3d(minx + ((double)x)*dx, maxy, 0);
    }
        for ( y = 0; y <= ny; y++ ) {
            if ( y == ny/2 ) continue;
        gl.glVertex3d(minx, minx + ((double)y)*dy, 0);
        gl.glVertex3d(maxx, minx + ((double)y)*dy, 0);
    }
        gl.glColor3d(0, 0, 0);
    gl.glVertex3d(minx + ((double)(nx/2))*dx, miny, 0);
    gl.glVertex3d(minx + ((double)(nx/2))*dx, maxy, 0);
    gl.glVertex3d(minx, minx + ((double)(ny/2))*dy, 0);
        gl.glVertex3d(maxx, minx + ((double)(ny/2))*dy, 0);

        gl.glEnd();
    }

}
