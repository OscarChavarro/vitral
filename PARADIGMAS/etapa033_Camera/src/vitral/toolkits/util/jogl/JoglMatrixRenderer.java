//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 22 2005 - David Diaz: Original base version                    =
//===========================================================================

package vitral.toolkits.util.jogl;

import vitral.toolkits.common.Matrix4x4;
import net.java.games.jogl.GL;

/**
 *
 * @author Oscar Chavarro, David Diaz
 */
public class JoglMatrixRenderer {
    
    public static void activateGL(GL gl, Matrix4x4 mtr)
    {
        double Mgl[] = new double[16];
        int row, column, pos;

        for ( pos = 0, column = 0; column < 4; column++ ) {
            for ( row = 0; row < 4; row++, pos++ ) {
                Mgl[pos] = mtr.M[row][column];
            }
        }

        gl.glMultMatrixd(Mgl);
    }

    
}
