//===========================================================================
import javax.media.opengl.GL;

import java.util.ArrayList;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.render.jogl.JoglSimpleBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCubemapBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglFixedBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyGroupRenderer;

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
        int i, j;
        Image texture;
        SimpleBodyGroup ggi;
        RendererConfiguration quality;

        //-----------------------------------------------------------------
        drawBackground(gl, s);
        JoglCameraRenderer.activate(gl, s.activeCamera);

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glLoadIdentity();

        if ( s.showCorridor ) {
            s.corridor.drawGL(gl);
        }

        //- Draw lights ---------------------------------------------------
        for ( i = 0; i < s.scene.getLights().size(); i++ ) {
            Light l = s.scene.getLights().get(i);
            JoglLightRenderer.activate(gl, l);
        }

        for ( i = 0; i < s.scene.getLights().size(); i++ ) {
            JoglLightRenderer.draw(gl, s.scene.getLights().get(i));
        }

        //- Draw scene bodies ---------------------------------------------
        SimpleBody gi;

        if ( s.scene.getLights().size() > 0 ) {
            gl.glEnable(gl.GL_LIGHTING);
        }
        else {
            gl.glDisable(gl.GL_LIGHTING);
        }

        for ( i = 0; i < s.scene.getSimpleBodies().size(); i++ ) {
            quality = s.qualityTemplate.clone();

            if ( s.selectedThings.isSelected(i) ) {
                quality.setSelectionCorners(true);
            }
            else {
                quality.setSelectionCorners(false);
            }
            gi = s.scene.getSimpleBodies().get(i);
            JoglSimpleBodyRenderer.draw(gl, gi, s.activeCamera, quality);
        }

        //- Draw reference grid plane -------------------------------------
        if ( s.showGrid ) drawGridRectangle(gl);

        //- Draw visual debug entities (usually transparent) --------------
        for ( i = 0; i < s.debugThingGroups.size(); i++ ) {
            quality = s.qualityTemplate.clone();
            quality.setShadingType(quality.SHADING_TYPE_NOLIGHT);
            if ( s.selectedDebugThingGroups.isSelected(i) ) {
                quality.setSelectionCorners(true);
            }
            else {
                quality.setSelectionCorners(false);
            }
            ggi = s.debugThingGroups.get(i);
            if ( ggi.getBodies().get(0).getGeometry() instanceof Sphere ) {
                gl.glDisable(gl.GL_DEPTH_TEST);
            }
            JoglSimpleBodyGroupRenderer.draw(gl, ggi, s.activeCamera, quality);
            gl.glEnable(gl.GL_DEPTH_TEST);
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
