//===========================================================================
package vsdk.toolkit.render.androidgles10;

// Android GLES 1.0 classes
import javax.microedition.khronos.opengles.GL10;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;

/**
*/
public class AndroidGLES10MatrixRenderer extends AndroidGLES10Renderer {

    /**
    This method accumulates the matrix represented in `A` in the currently
    selected matrix stack inside the JOGL state machine.
     * @param gl
     * @param A
    */
    public static void activate(GL10 gl, Matrix4x4 A)
    {
        float Mgl[] = new float[16];
        int row, column, pos;

        for ( pos = 0, column = 0; column < 4; column++ ) {
            for ( row = 0; row < 4; row++, pos++ ) {
                Mgl[pos] = (float)A.M[row][column];
            }
        }

        gl.glMultMatrixf(Mgl, 0);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
