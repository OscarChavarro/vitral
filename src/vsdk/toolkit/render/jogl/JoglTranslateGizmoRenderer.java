//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 16 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Java basic classes
import java.util.ArrayList;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

// VSDK classes
import vsdk.toolkit.common.VSDK;
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
import vsdk.toolkit.environment.geometry.Cone;

public class JoglTranslateGizmoRenderer extends JoglRenderer 
{
    public static Light light1 = null;
    public static Light light2 = null;

    private static GLU glu = null;
    private static GLUquadric gluQuadric = null;

    private static void drawConeWithShadow(GL gl, Cone cone, ColorRgb color)
    {
        if ( glu == null ) {
            glu = new GLU();
            gluQuadric = glu.gluNewQuadric();
        }

        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);

        double r1, r2, h;
        int slices = 8;

        r1 = cone.getBaseRadius();
        r2 = cone.getTopRadius();
        h = cone.getHeight();

        VSDK.acumulatePrimitiveCount(VSDK.TRIANGLE, 4*slices);

        glu.gluCylinder(gluQuadric, r1, r2, h, slices, 1);
        gl.glPushMatrix();
        gl.glRotated(180, 1, 0, 0);
	double p = 0.5;
        gl.glColor3d(color.r*p, color.g*p, color.b*p);
        glu.gluDisk(gluQuadric, 0, r1, slices, 1);
        gl.glPopMatrix();
    }

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
                if ( g instanceof Cone ) {
                    drawConeWithShadow(gl, (Cone)g, c);
                }
                else {
                    JoglGeometryRenderer.draw(gl, g, gizmo.getCamera(), q);
                }
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
