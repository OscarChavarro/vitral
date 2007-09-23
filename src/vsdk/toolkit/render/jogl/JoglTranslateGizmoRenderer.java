//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 16 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

import javax.media.opengl.GL;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.gui.TranslateGizmo;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Arrow;

public class JoglTranslateGizmoRenderer extends JoglRenderer 
{
    public static Light light1 = null;
    public static Light light2 = null;

    private static void draw3dsmax(GL gl, TranslateGizmo gizmo)
    {
        RendererConfiguration q = new RendererConfiguration();
        ArrayList<SimpleBody> things = gizmo.getElements3dsmax();

        q.setSurfaces(true);
        q.setWires(false);
        q.setBoundingVolume(false);
        q.setTexture(false);
        q.setPoints(false);
        q.setBumpMap(false);
        q.setNormals(false);
        q.setShadingType(q.SHADING_TYPE_FLAT);

        gl.glShadeModel(gl.GL_SMOOTH);

        ColorRgb c;

        int i;

        //-----------------------------------------------------------------
        gl.glDisable(gl.GL_LIGHTING);
        for ( i = 0; i < things.size(); i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3D position;

            if ( g != null && (i < 9 || i > 11) ) {
                gl.glPushMatrix();

                gl.glLoadIdentity();
                position = r.getPosition();
                gl.glTranslated(position.x, position.y, position.z);
                JoglMatrixRenderer.activate(gl, r.getRotation());
                gl.glDisable(gl.GL_LIGHTING);
                c = r.getMaterial().getDiffuse();
                gl.glColor3d(c.r, c.g, c.b);
                q.setWireColor(c);
                JoglGeometryRenderer.draw(gl, g, gizmo.getCamera(), q);
                gl.glPopMatrix();
            }
        }

        //-----------------------------------------------------------------
        gl.glEnable(gl.GL_LIGHTING);
        for ( i = 9; i < things.size() && i < 12; i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3D position;

            if ( g != null ) {
                gl.glPushMatrix();

                gl.glLoadIdentity();
                position = r.getPosition();
                gl.glTranslated(position.x, position.y, position.z);
                JoglMatrixRenderer.activate(gl, r.getRotation());
                JoglMaterialRenderer.activate(gl, r.getMaterial());
                JoglGeometryRenderer.draw(gl, g, gizmo.getCamera(), q);
                gl.glPopMatrix();
            }
        }

    }

    private static void drawAquynza(GL gl, TranslateGizmo gizmo)
    {
        RendererConfiguration q = new RendererConfiguration();
        ArrayList<SimpleBody> things = gizmo.getElements();

        q.setSurfaces(true);
        q.setWires(false);
        q.setBoundingVolume(false);
        q.setTexture(false);
        q.setPoints(false);
        q.setBumpMap(false);
        q.setNormals(false);
        q.setShadingType(q.SHADING_TYPE_GOURAUD);

        gl.glShadeModel(gl.GL_SMOOTH);

        int i;
        for ( i = 0; i < things.size(); i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3D position;

            if ( g != null ) {
                gl.glPushMatrix();

                gl.glLoadIdentity();
                position = r.getPosition();
                gl.glTranslated(position.x, position.y, position.z);
                JoglMatrixRenderer.activate(gl, r.getRotation());
                JoglMaterialRenderer.activate(gl, r.getMaterial());
                JoglGeometryRenderer.draw(gl, g, gizmo.getCamera(), q);
                gl.glPopMatrix();
            }
        }
    }

    public static void draw(GL gl, TranslateGizmo gizmo)
    {
/*
        JoglLightRenderer.turnOffAllLights(gl);

        Vector3D lp1;
        Vector3D lp2;

        lp1 = new Vector3D(gizmo.getPosition());
        lp1.x += 20;
        lp1.y += 20;
        lp1.z += 20;
        lp2 = new Vector3D(gizmo.getPosition());
        lp2.x -= 20;
        lp2.y -= 20;
        if ( light1 == null ) {
            light1 = new Light(Light.DIRECTIONAL, lp1, new ColorRgb(1, 1, 1));
        }
        if ( light2 == null ) {
            light2 = new Light(Light.DIRECTIONAL, lp2, new ColorRgb(1, 1, 1));
        }
        light1.setPosition(lp1);
        light2.setPosition(lp2);
        JoglLightRenderer.activate(gl, light1);
        JoglLightRenderer.activate(gl, light2);
*/

        //-----------------------------------------------------------------
        //drawAquynza(gl, gizmo);
        draw3dsmax(gl, gizmo);
        //-----------------------------------------------------------------
        JoglLightRenderer.turnOffAllLights(gl);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
