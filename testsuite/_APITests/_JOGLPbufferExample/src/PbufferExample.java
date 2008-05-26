import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLDrawableFactory;

import vsdk.toolkit.io.image.ImagePersistence;

/*
import com.sun.opengl.utils.BufferUtils;
import java.nio.ByteBuffer;
*/

import java.io.File;

import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;

public class PbufferExample implements GLEventListener {
    private static int imageWidth = 320;
    private static int imageHeight = 240;
    private GLPbuffer  pbuffer;
    private RGBImage  image;

    public PbufferExample() {
        // Create a GLCapabilities object for the pbuffer
        GLCapabilities pbCaps = new GLCapabilities();
        pbCaps.setDoubleBuffered(false);
        try {
            pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(pbCaps, null, imageWidth, imageHeight, null);
          }
          catch ( Exception e ) {
              System.err.println("Error creating OpenGL Pbuffer. This program requires a 3D accelerator card.");
              System.exit(1);
        }
        pbuffer.addGLEventListener(this);
        pbuffer.display();
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

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
  
    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height ) 
    {
        GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height);
    }
  
    public static void main( String[] args ) {
        PbufferExample demo = new PbufferExample();
    }
}
