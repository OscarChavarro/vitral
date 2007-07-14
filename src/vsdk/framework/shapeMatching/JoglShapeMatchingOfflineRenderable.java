//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//===========================================================================

package vsdk.framework.shapeMatching;

// JOGL classes
import javax.media.opengl.GL;

// Vitral classes
import vsdk.framework.Component;

public abstract class JoglShapeMatchingOfflineRenderable extends Component {
    public abstract void executeRendering(GL gl);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
