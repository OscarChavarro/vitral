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

import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.render.jogl.JoglRGBAImageRenderer;

public class PbufferExample implements GLEventListener {
    private static int imageWidth = 320;
    private static int imageHeight = 240;
    private GLPbuffer  pbuffer;
    private RGBAImage  image;

    public PbufferExample() {
        // Create a GLCapabilities object for the pbuffer
        GLCapabilities pbCaps = new GLCapabilities();
        pbCaps.setDoubleBuffered(false);
        pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(pbCaps, null, imageWidth, imageHeight, null);
        pbuffer.addGLEventListener(this);
        pbuffer.display();
    }

    public void display(GLAutoDrawable drawable) {
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
        gl.glFlush();
        
        image=JoglRGBAImageRenderer.getImageJOGL(gl);
        ImagePersistence.exportPPM(new File("./output.ppm"), image);
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
