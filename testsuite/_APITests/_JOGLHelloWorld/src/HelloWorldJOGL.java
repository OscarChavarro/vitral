//===========================================================================
// VITRAL recomendation: Use explicit class imports (not .*) in hello world 
// type programs so the user/programmer can be exposed to all the complexity 
// involved. This will help him to know better the involved libraries.
import java.awt.BorderLayout;
import javax.swing.JFrame;

// JOGL classes
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public class HelloWorldJOGL implements GLEventListener {

    public HelloWorldJOGL() {
        createElements();
    }

    /**
    A final method is one that does not accept to have override in subclasses
    */
    public final void createElements()
    {
        //-----------------------------------------------------------------
        GLProfile glp = GLProfile.get(GLProfile.GL2); 
        GLCapabilities caps = new GLCapabilities(glp);
        GLCanvas canvas = new GLCanvas(caps);
        canvas.addGLEventListener(this);

        //-----------------------------------------------------------------
        JFrame frame;

        frame = new JFrame("VITRAL concept test - JOGL Hello World");
        frame.add(canvas, BorderLayout.CENTER);
        frame.pack();
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    /** 
    Called by drawable to initiate drawing.
    @param drawable 
    */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glBegin(GL2.GL_LINES);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0.5, 0.5, 0);
        gl.glEnd();
    }

    /**
    Not used method, but need to instance GLEventListener.
    @param drawable
    */
    @Override
    public void init(GLAutoDrawable drawable) {

    }

    /** 
    Not used method, but need to instance GLEventListener 
    @param drawable
    @param a
    @param b
    */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {

    }

    /** 
    Not used method, but need to instance GLEventListener.
    @param drawable
    */
    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    /**
    Called to indicate the drawing surface has been moved and/or resized
    @param drawable
    @param x
    @param y
    @param width
    @param height
    */
    @Override
    public void reshape(GLAutoDrawable drawable,
                        int x,
                        int y,
                        int width,
                        int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }

    /**
    Program entry point for applications.
    @param args 
    */
    public static void main(String[] args) {
        HelloWorldJOGL instance = new HelloWorldJOGL();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
