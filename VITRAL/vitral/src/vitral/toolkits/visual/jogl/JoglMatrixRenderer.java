//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 22 2005 - David Diaz: Original base version                    =
//= - November 15 2005 - Oscar Chavarro: Migrated to JOGL Beta Version      =
//===========================================================================

package vitral.toolkits.visual.jogl;

import vitral.toolkits.common.Matrix4x4;
import javax.media.opengl.GL;

/**
This class is meant to support rendering operations in the JOGL API from
vitral internal representation of a matrix containing an homogeneous
coordinates geometrical transformation represented in the 
`vitral.toolkits.common.Matrix4x4` class.
 */
public class JoglMatrixRenderer {

    /**
    This method acumulates the matrix represented in `A` in the currently
    selected matrix stack inside the JOGL state machine.
    */
    public static void activateGL(GL gl, Matrix4x4 A)
    {
        double Mgl[] = new double[16];
        int row, column, pos;

        for ( pos = 0, column = 0; column < 4; column++ ) {
            for ( row = 0; row < 4; row++, pos++ ) {
                Mgl[pos] = A.M[row][column];
            }
        }

        gl.glMultMatrixd(Mgl, 0);
    }

    /**
    This method is designed to provide a 3D graphical representation of
    a transformation matrix, in terms of 3 vectors. The vectors are drawn
    in the order `i'`, `j'`, `k'`, with corresponding colors red, green
    and blue, and each vector is represented as an arrow. If the matrix
    `A` is an identity matrix, then the vectors correspond to the
    orthogonal i, j, k vectors. If `A` is any orthogonal rotation matrix,
    the vectors drawn will correspond to a reference frame of unit vectors.
    In a similar fashion, the transformation components of the matrix
    will determine the center of the represented reference frame, and
    the scale components will change its state. The 3d graphical 
    representation of the reference frame determined by the `A` matrix
    will reflect any inconsistent state, as null vectors or non-ortogonal
    vectors, by making different red marks.

    THE METHOD IS NOT IMPLEMENTED.
    */
    public static void drawGL(GL gl, Matrix4x4 A)
    {
        ;
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
