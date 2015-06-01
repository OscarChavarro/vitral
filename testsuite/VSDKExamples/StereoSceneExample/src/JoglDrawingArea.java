import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLAutoDrawable;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.render.jogl.JoglStereoStrategyRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyCyclopeanZBufferRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyAutostereogramRenderer;

public class JoglDrawingArea implements GLEventListener
{
    private JoglStereoStrategyRenderer stereoStrategy;
    private StereoSceneExample parent;

    public JoglDrawingArea(JoglStereoStrategyRenderer stereoStrategy, StereoSceneExample parent)
    {
        this.stereoStrategy = stereoStrategy;
        this.parent = parent;
    }

    public JoglDrawingArea(StereoSceneExample parent)
    {
        stereoStrategy = null;
        this.parent = parent;
    }

    public void setStereoStrategy(JoglStereoStrategyRenderer stereoStrategy)
    {
        this.stereoStrategy = stereoStrategy;
    }

    public void drawCenterModel(GL2 gl)
    {
        parent.scene.activeCamera = parent.scene.camera;
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glRotated(parent.angle, 0, 0, 1);
        JoglSceneRenderer.draw(gl, parent.scene);
    }

    public void drawLeftModel(GL2 gl)
    {
        //-----------------------------------------------------------------
        parent.scene.camera.updateVectors();

        Vector3D l = new Vector3D(parent.scene.camera.getLeft());
        l.normalize();
        l = l.multiply(parent.scene.eyeDistance/2.0);
        Vector3D p = new Vector3D(parent.scene.camera.getPosition());

        parent.scene.activeCamera = new Camera(parent.scene.camera);
        parent.scene.activeCamera.setPosition(p.add(l));
        
        //-----------------------------------------------------------------
        Matrix4x4 R1;
        Matrix4x4 R2;
        Vector3D u;

        u = new Vector3D(parent.scene.camera.getUp());

        R1 = parent.scene.camera.getRotation();
        R2 = new Matrix4x4();
        R2.axisRotation(-Math.toRadians(parent.scene.eyeTorsionAngle), u);

        parent.scene.activeCamera.setRotation(R2.multiply(R1));

        //-----------------------------------------------------------------
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glRotated(parent.angle, 0, 0, 1);
        JoglSceneRenderer.draw(gl, parent.scene);
    }

    public void drawRightModel(GL2 gl)
    {
        //-----------------------------------------------------------------
        parent.scene.camera.updateVectors();

        Vector3D l = new Vector3D(parent.scene.camera.getLeft());
        l.normalize();
        l = l.multiply(parent.scene.eyeDistance/2.0);
        Vector3D p = new Vector3D(parent.scene.camera.getPosition());

        parent.scene.activeCamera = new Camera(parent.scene.camera);
        parent.scene.activeCamera.setPosition(p.substract(l));
        
        //-----------------------------------------------------------------
        Matrix4x4 R1;
        Matrix4x4 R2;
        Vector3D u;

        u = new Vector3D(parent.scene.camera.getUp());

        R1 = parent.scene.camera.getRotation();
        R2 = new Matrix4x4();
        R2.axisRotation(Math.toRadians(parent.scene.eyeTorsionAngle), u);

        parent.scene.activeCamera.setRotation(R2.multiply(R1));

        //-----------------------------------------------------------------
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glRotated(parent.angle, 0, 0, 1);
        JoglSceneRenderer.draw(gl, parent.scene);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        //-----------------------------------------------------------------
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);

        //-----------------------------------------------------------------
        if ( parent.animator != null && parent.animator.isAnimating() &&
             parent.isRotating ) {
            parent.angle += 1.0;
        }
        //-----------------------------------------------------------------
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
                gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
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
