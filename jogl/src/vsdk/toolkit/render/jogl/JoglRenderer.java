//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - July 25 2006 - Oscar Chavarro: Original base version                  =
//= - December 28 2006 - Oscar Chavarro: Added Nvidia Cg support            =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Java base classes
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import com.jogamp.opengl.util.GLBuffers;

// VitralSDK classes
import vsdk.toolkit.render.RenderingElement;

/**
The JoglRenderer abstract class provides an interface for Jogl*Renderer
style classes. This serves two purposes:
  - To help in design level organization of Jogl renderers (this eases the
    study of the class hierarchy)
  - To provide a place to locate operations common to all Jogl renderers
    classes and Jogl renderers' private utility/supporting classes. In this
    moment, this operations include the global framework for managing
    general JOGL initialization, as such verifying correct availavility of
    native OpenGL libraries.
*/

public abstract class JoglRenderer extends RenderingElement {
    /**
    Note that this static block is automatically called for the first
    instanciation or the first static call to any of current subclasses.
    This is used to check correct JOGL environment availability, and
    report if not.
    Check why it is good to have this here disabled
    */
/*
    static {
        // What happens when this is executed from an applet?
        //verifyOpenGLAvailability();
    }
*/

    public static boolean verifyOpenGLAvailability()
    {
/*
        if ( !PersistenceElement.verifyLibrary("jogl_gl2es12") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL Library not found.  Check your installation.");
            return false;
        }
*/
/*
        if ( !PersistenceElement.verifyLibrary("jogl_awt") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL-AWT Library not found.  Check your installation.");
            return false;
        }
*/
/*
        if ( !PersistenceElement.verifyLibrary("jogl_cg") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL-CG Library not found.  Check your installation.");
            return false;
        }
*/
        return true;
    }

    protected static FloatBuffer
    cloneDoubleArrayToFloatBuffer(double v[])
    {
        FloatBuffer buffer;
        int i;

        //buffer = BufferUtil.newFloatBuffer(v.length);
        //buffer = ByteBuffer.allocateDirect(v.length*4).asFloatBuffer();
        buffer = (FloatBuffer)GLBuffers.newDirectGLBuffer(GL.GL_FLOAT, v.length);

        for ( i = 0; i < v.length; i++ ) {
            buffer.put((float)v[i]);
        }
        buffer.rewind();

        return buffer;
    }

    protected static FloatBuffer
    cloneDoubleArrayToInvertedFloatBuffer(double v[])
    {
        FloatBuffer buffer;
        int i;

        //buffer = BufferUtil.newFloatBuffer(v.length);
        //buffer = ByteBuffer.allocateDirect(v.length*4).asFloatBuffer();
        buffer = (FloatBuffer)GLBuffers.newDirectGLBuffer(GL.GL_FLOAT, v.length);

        for ( i = 0; i < v.length; i++ ) {
            buffer.put((float)(v[i]*-1));
        }
        buffer.rewind();

        return buffer;
    }

    protected static IntBuffer
    cloneIntArrayToIntBuffer(int v[])
    {
        IntBuffer buffer;
        int i;

        //buffer = BufferUtil.newIntBuffer(v.length);
        //buffer = ByteBuffer.allocateDirect(v.length*4).asIntBuffer();
        buffer = (IntBuffer)GLBuffers.newDirectGLBuffer(GL2.GL_INT, v.length);

        for ( i = 0; i < v.length; i++ ) {
            buffer.put(v[i]);
        }
        buffer.rewind();

        return buffer;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
