import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLAutoDrawable;

import vsdk.toolkit.render.jogl.JoglStereoStrategyRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyCyclopeanZBufferRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyAutostereogramRenderer;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class JoglDrawingArea implements GLEventListener
{
    private JoglStereoStrategyRenderer stereoStrategy;

    public JoglDrawingArea(JoglStereoStrategyRenderer stereoStrategy)
    {
        this.stereoStrategy = stereoStrategy;
    }

    public JoglDrawingArea()
    {
        stereoStrategy = null;
    }

    public void setStereoStrategy(JoglStereoStrategyRenderer stereoStrategy)
    {
        this.stereoStrategy = stereoStrategy;
    }

    public void drawCenterModel(GL2 gl)
    {
        // Default reference model
        //gl.glMatrixMode(gl.GL_PROJECTION);
        //gl.glLoadIdentity();
        gl.glEnable(gl.GL_DEPTH_TEST);
        Camera c = new Camera();
        c.setNearPlaneDistance(1);
        c.setFarPlaneDistance(6.0);
        Vector3D v = new Vector3D(0, 0, 4);
        Matrix4x4 R1 = new Matrix4x4();
        Matrix4x4 R2 = new Matrix4x4();
        R1.axisRotation(Math.PI/2, 0, 1, 0);
        R2.axisRotation(Math.PI/2, 0, 0, 1);
        c.setPosition(v);
        c.setRotation(R2.multiply(R1));
        JoglCameraRenderer.activate(gl, c);

        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        // White background quad
        gl.glColor4d(1, 1, 1, 1);
        gl.glBegin(gl.GL_QUADS);
            gl.glVertex3d(-0.7, -0.7, 0.0);
            gl.glVertex3d(0.7, -0.7, 0.0);
            gl.glVertex3d(0.7, 0.7, 0.0);
            gl.glVertex3d(-0.7, 0.7, 0.0);
        gl.glEnd();

        // Gray "C" letter
        double z2 = 0.5;

        gl.glColor4d(0.5, 0.5, 0.5, 1.0);
        gl.glBegin(gl.GL_QUADS);
            gl.glVertex3d(-0.4, 0.5, z2);
            gl.glVertex3d(-0.4, -0.5, z2);
            gl.glVertex3d(-0.3, -0.5, z2);
            gl.glVertex3d(-0.3, 0.5, z2);

            gl.glVertex3d(-0.4, -0.4, z2);
            gl.glVertex3d(-0.4, -0.5, z2);
            gl.glVertex3d(0.4, -0.5, z2);
            gl.glVertex3d(0.4, -0.4, z2);

            gl.glVertex3d(-0.4, 0.5, z2);
            gl.glVertex3d(-0.4, 0.4, z2);
            gl.glVertex3d(0.4, 0.4, z2);
            gl.glVertex3d(0.4, 0.5, z2);
        gl.glEnd();
    }

    public void drawLeftModel(GL2 gl)
    {
        // Default reference model
        gl.glDisable(gl.GL_DEPTH_TEST);
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        // White background quad
        gl.glColor4d(1, 1, 1, 1);
        gl.glBegin(gl.GL_QUADS);
            gl.glVertex2d(-0.8, -0.6);
            gl.glVertex2d(0.6, -0.6);
            gl.glVertex2d(0.6, 0.6);
            gl.glVertex2d(-0.8, 0.6);
        gl.glEnd();

        // Gray "L" letter
        gl.glColor4d(0.5, 0.5, 0.5, 1.0);
        gl.glBegin(gl.GL_QUADS);
            gl.glVertex2d(-0.5, 0.5);
            gl.glVertex2d(-0.5, -0.5);
            gl.glVertex2d(-0.4, -0.5);
            gl.glVertex2d(-0.4, 0.5);

            gl.glVertex2d(-0.5, -0.4);
            gl.glVertex2d(-0.5, -0.5);
            gl.glVertex2d(0.3, -0.5);
            gl.glVertex2d(0.3, -0.4);
        gl.glEnd();
    }

    public void drawRightModel(GL2 gl)
    {
        // Default reference model
        gl.glDisable(gl.GL_DEPTH_TEST);
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        // White background quad
        gl.glColor4f(1, 1, 1, 1);
        gl.glBegin(gl.GL_QUADS);
            gl.glVertex2d(-0.6, -0.8);
            gl.glVertex2d(0.8, -0.8);
            gl.glVertex2d(0.8, 0.8);
            gl.glVertex2d(-0.6, 0.8);
        gl.glEnd();

        // Gray "R" letter
        gl.glColor4d(0.5, 0.5, 0.5, 1.0);
        gl.glBegin(gl.GL_QUADS);
            gl.glVertex2d(-0.4, 0.5);
            gl.glVertex2d(-0.4, -0.5);
            gl.glVertex2d(-0.3, -0.5);
            gl.glVertex2d(-0.3, 0.5);

            gl.glVertex2d(-0.4, 0.5);
            gl.glVertex2d(-0.4, 0.4);
            gl.glVertex2d(0.6, 0.4);
            gl.glVertex2d(0.6, 0.5);

            gl.glVertex2d(0.6, 0.4);
            gl.glVertex2d(0.45, 0.4);
            gl.glVertex2d(-0.3, 0.0);
            gl.glVertex2d(-0.3, -0.1);

            gl.glVertex2d(-0.3, 0.0);
            gl.glVertex2d(-0.3, -0.1);
            gl.glVertex2d(0.35, -0.5);
            gl.glVertex2d(0.5, -0.5);

        gl.glEnd();
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);

        if ( stereoStrategy == null ) {
            drawCenterModel(gl);
        }
        else if ( 
        stereoStrategy instanceof JoglStereoStrategyCyclopeanZBufferRenderer ||
        stereoStrategy instanceof JoglStereoStrategyAutostereogramRenderer
        ) {
            stereoStrategy.activateStereoMode(gl);
 
            if ( stereoStrategy.configureDefaultRightChannel(gl) ) {
                drawCenterModel(gl);
            }

            stereoStrategy.deactivateStereoMode(gl);
        }
        else {
            stereoStrategy.activateStereoMode(gl);
 
            if ( stereoStrategy.configureDefaultRightChannel(gl) ) {
                drawRightModel(gl);
            }

            if ( stereoStrategy.configureDefaultLeftChannel(gl) ) {
                drawLeftModel(gl);
            }

            stereoStrategy.deactivateStereoMode(gl);
        }
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable)
    {
        ;
    }

    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape(GLAutoDrawable drawable,
                        int x,
                        int y,
                        int width,
                        int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }

}
