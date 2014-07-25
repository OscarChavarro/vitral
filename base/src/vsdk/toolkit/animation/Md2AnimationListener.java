
package vsdk.toolkit.animation;

// Java basic classes
import javax.media.opengl.awt.GLJPanel;

// VitralSDK classes
import vsdk.toolkit.environment.geometry.Md2Mesh;

/**
 *
 * @author
 */
public class Md2AnimationListener  extends AnimationListener {
    private final GLJPanel canvas;
    private final Md2Mesh md2Mesh;

    public Md2AnimationListener(GLJPanel canvas, Md2Mesh md2Mesh)
    {
        this.canvas = canvas;
        this.md2Mesh = md2Mesh;
    }

    @Override
    public void tick(AnimationEvent e) {
        // Vector3D p = new Vector3D(e.getT()*10.0, 0.0, 1.0);
        
        if ( md2Mesh != null ) {
            //model.setAnimationTestCursorPosition(p);
            md2Mesh.setElapsedTimeSeg((float)e.getT());
        }
        if ( canvas != null ) {
            canvas.repaint();
        }

    }    
}
