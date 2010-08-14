import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLDrawableFactory;

import vsdk.toolkit.io.image.ImagePersistence;

import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;

import java.io.File;

import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;

public class PbufferExample implements GLEventListener {
    private static int imageWidth = 320;
    private static int imageHeight = 240;
    private GLPbuffer  pbuffer;
    private RGBImage  image;

    public PbufferExample() {
        GLProfile profile;

        profile = GLProfile.get(GLProfile.GL2);
        // Create a GLCapabilities object for the pbuffer
        GLCapabilities pbCaps = new GLCapabilities(profile);
        pbCaps.setDoubleBuffered(false);
        try {
            pbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(pbCaps, null, imageWidth, imageHeight, null);
          }
          catch ( Exception e ) {
              System.err.println("Error creating OpenGL Pbuffer. This program requires a 3D accelerator card.");
              System.exit(1);
        }
        pbuffer.addGLEventListener(this);
        pbuffer.display();
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glColor3d(1, 1, 1); 

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glBegin(gl.GL_LINES);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0.5, 0.5, 0);
        gl.glEnd();
        gl.glFlush();
        
        image=JoglRGBImageRenderer.getImageJOGL(gl);
        ImagePersistence.exportJPG(new File("./output.jpg"), image);
    }

    public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
    }

    public void init( GLAutoDrawable drawable ) {
    }
  
    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
        ;
    }

    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height ) 
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }
  
    public static void main( String[] args ) {
        PbufferExample demo = new PbufferExample();
    }
}
