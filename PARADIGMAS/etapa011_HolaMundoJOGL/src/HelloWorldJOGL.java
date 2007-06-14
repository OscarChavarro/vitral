// VITRAL recomendation: Use explicit class imports (not .*) in hello world type programs
// so the user/programmer can be exposed to all the complexity involved. This will help him
// to dominate the involved libraries.
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import net.java.games.jogl.GL;
import net.java.games.jogl.GLU;
import net.java.games.jogl.GLCanvas;
import net.java.games.jogl.GLCapabilities;
import net.java.games.jogl.GLDrawable;
import net.java.games.jogl.GLDrawableFactory;
import net.java.games.jogl.GLEventListener;

public class HelloWorldJOGL extends Frame implements GLEventListener {

    public HelloWorldJOGL() {
        super("VITRAL concept test - JOGL Hello World");

        GLCapabilities capabilities = new GLCapabilities();
        GLCanvas canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);
        canvas.addGLEventListener(this);

        this.add(canvas, BorderLayout.CENTER);
    }
    
    public Dimension getPreferredSize() {
        return new Dimension (640, 480);
    }
    
    public static void main (String[] args) {
        Frame f = new HelloWorldJOGL();
        f.pack();
        f.setVisible(true);
    }
    
    /** Called by drawable to initiate drawing */
    public void display(GLDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glColor3d(1, 1, 1); 

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0.5, 0.5, 0);
        gl.glEnd();
    }
   
    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLDrawable drawable, boolean a, boolean b) {
        ;
    }
    
    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape (GLDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height); 
    }   
}
