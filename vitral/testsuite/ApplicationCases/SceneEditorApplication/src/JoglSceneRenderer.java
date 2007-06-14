//===========================================================================
import javax.media.opengl.GL;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.jogl.JoglSimpleBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCubemapBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglFixedBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;

public class JoglSceneRenderer
{
    public static void drawBackground(GL gl, Scene s)
    {
        switch ( s.selectedBackground ) {
          case 2:
            if ( s.cubemapBackground == null ) {
                s.buildCubemap();
            }
            if ( s.cubemapBackground != null ) {
                JoglCubemapBackgroundRenderer.draw(gl, s.cubemapBackground);
            }
            else {
                JoglSimpleBackgroundRenderer.draw(gl, s.simpleBackground);
            }
            break;
          case 1:
            if ( s.fixedBackground == null ) {
                s.buildFixedmap();
            }
            if ( s.fixedBackground != null ) {
                JoglFixedBackgroundRenderer.draw(gl, s.fixedBackground);
            }
            else {
                JoglSimpleBackgroundRenderer.draw(gl, s.simpleBackground);
            }
            break;
          case 0: default:
            JoglSimpleBackgroundRenderer.draw(gl, s.simpleBackground);
            break;
        }
    }

    public static void draw(GL gl, Scene s)
    {
        int i;
        Image texture;

        drawBackground(gl, s);
        JoglCameraRenderer.activate(gl, s.activeCamera);

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glLoadIdentity();

        if ( s.showCorridor ) {
            s.corridor.drawGL(gl);
        }

        SimpleBody gi;
        Vector3D p;
        Vector3D scale;

        if ( s.lights.size() > 0 ) {
            gl.glEnable(gl.GL_LIGHTING);
        }
        else {
            gl.glDisable(gl.GL_LIGHTING);
        }

        for ( i = 0; i < s.lights.size(); i++ ) {
            Light l = s.lights.get(i);
            JoglLightRenderer.activate(gl, l);
        }

        for ( i = 0; i < s.lights.size(); i++ ) {
            JoglLightRenderer.draw(gl, s.lights.get(i));
        }

        // Draw reference grid plane
        if ( s.showGrid ) drawGridRectangle(gl);

        for ( i = 0; i < s.things.size(); i++ ) {
            RendererConfiguration quality;
            gi = s.things.get(i);
            p = gi.getPosition();
            scale = gi.getScale();

            quality = s.qualityTemplate;

            if ( s.selectedThings.isSelected(i) ) {
                quality.setSelectionCorners(true);
            }
            else {
                quality.setSelectionCorners(false);
            }

            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glTranslated(p.x, p.y, p.z);
            JoglMatrixRenderer.activate(gl, gi.getRotation());

            gl.glScaled(scale.x, scale.y, scale.z);

            gl.glColor3d(1, 1, 1);
            JoglMaterialRenderer.activate(gl, gi.getMaterial());

            texture = gi.getTexture();

        if ( quality.isTextureSet() && (texture != null) ) {
                gl.glEnable(gl.GL_TEXTURE_2D);
                JoglImageRenderer.activate(gl, texture);
        }
        else {
                gl.glDisable(gl.GL_TEXTURE_2D);
        }

            JoglGeometryRenderer.draw(gl, gi.getGeometry(),
                                      s.activeCamera, quality);

            gl.glPopMatrix();

        }

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
        gl.glDisable(gl.GL_TEXTURE_2D);
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

//===========================================================================
//= EOF                                                                     =
//===========================================================================
