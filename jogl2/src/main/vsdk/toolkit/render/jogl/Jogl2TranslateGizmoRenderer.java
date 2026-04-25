package vsdk.toolkit.render.jogl;

// Java basic classes
import java.util.ArrayList;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.gui.TranslateGizmo;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.LightType;

public class Jogl2TranslateGizmoRenderer extends Jogl2Renderer 
{
    public static Light light1 = null;
    public static Light light2 = null;

    private static GLU glu = null;
    private static GLUquadric gluQuadric = null;

    private static void drawConeWithShadow(GL2 gl, Cone cone, ColorRgb color)
    {
        if ( glu == null ) {
            glu = new GLU();
            gluQuadric = glu.gluNewQuadric();
        }

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);

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

    private static void draw3dsmax(GL2 gl, TranslateGizmo gizmo)
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
        q.setShadingType(RendererConfiguration.SHADING_TYPE_FLAT);

        gl.glShadeModel(GL2.GL_SMOOTH);

        ColorRgb c;

        int i;

        //-----------------------------------------------------------------
        gl.glDisable(GL2.GL_LIGHTING);
        for ( i = 0; i < things.size(); i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3D position;

            if ( g != null && (i < 9 || i > 11) ) {
                gl.glPushMatrix();

                gl.glLoadIdentity();
                position = r.getPosition();
                gl.glTranslated(position.x(), position.y(), position.z());
                Jogl2MatrixRenderer.activate(gl, r.getRotation());
                gl.glDisable(GL2.GL_LIGHTING);
                c = r.getMaterial().getDiffuse();
                gl.glColor3d(c.r, c.g, c.b);
                q.setWireColor(c);
                if ( g instanceof Cone ) {
                    drawConeWithShadow(gl, (Cone)g, c);
                }
                else {
                    Jogl2GeometryRenderer.draw(gl, g, gizmo.getCamera(), q);
                }
                gl.glPopMatrix();
            }
        }

        //-----------------------------------------------------------------
        gl.glEnable(GL2.GL_LIGHTING);
        for ( i = 9; i < things.size() && i < 12; i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3D position;

            if ( g != null ) {
                gl.glPushMatrix();

                gl.glLoadIdentity();
                position = r.getPosition();
                gl.glTranslated(position.x(), position.y(), position.z());
                Jogl2MatrixRenderer.activate(gl, r.getRotation());
                Jogl2MaterialRenderer.activate(gl, r.getMaterial());
                Jogl2GeometryRenderer.draw(gl, g, gizmo.getCamera(), q);
                gl.glPopMatrix();
            }
        }

    }

    private static void drawAquynza(GL2 gl, TranslateGizmo gizmo)
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
        q.setShadingType(RendererConfiguration.SHADING_TYPE_GOURAUD);

        gl.glShadeModel(GL2.GL_SMOOTH);

        int i;
        for ( i = 0; i < things.size(); i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3D position;

            if ( g != null ) {
                gl.glPushMatrix();

                gl.glLoadIdentity();
                position = r.getPosition();
                gl.glTranslated(position.x(), position.y(), position.z());
                Jogl2MatrixRenderer.activate(gl, r.getRotation());
                Jogl2MaterialRenderer.activate(gl, r.getMaterial());
                Jogl2GeometryRenderer.draw(gl, g, gizmo.getCamera(), q);
                gl.glPopMatrix();
            }
        }
    }

    public static void draw(GL2 gl, TranslateGizmo gizmo)
    {
/*
        Jogl2LightRenderer.turnOffAllLights(gl);

        Vector3D lp1;
        Vector3D lp2;

        lp1 = new Vector3D(gizmo.getPosition());
        lp1 = lp1.withX(lp1.x() + 20);
        lp1 = lp1.withY(lp1.y() + 20);
        lp1 = lp1.withZ(lp1.z() + 20);
        lp2 = new Vector3D(gizmo.getPosition());
        lp2 = lp2.withX(lp2.x() - 20);
        lp2 = lp2.withY(lp2.y() - 20);
        if ( light1 == null ) {
            light1 = new Light(LightType.DIRECTIONAL, lp1, new ColorRgb(1, 1, 1));
        }
        if ( light2 == null ) {
            light2 = new Light(LightType.DIRECTIONAL, lp2, new ColorRgb(1, 1, 1));
        }
        light1.setPosition(lp1);
        light2.setPosition(lp2);
        Jogl2LightRenderer.activate(gl, light1);
        Jogl2LightRenderer.activate(gl, light2);
*/

        //-----------------------------------------------------------------
        //drawAquynza(gl, gizmo);
        draw3dsmax(gl, gizmo);
        //-----------------------------------------------------------------
        Jogl2LightRenderer.turnOffAllLights(gl);
    }
}
