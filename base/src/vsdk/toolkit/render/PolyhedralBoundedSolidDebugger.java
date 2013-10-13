//===========================================================================

package vsdk.toolkit.render;

import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;

// Comment out this line if JOGL is not available at compile time
import vsdk.toolkit.render.jogl.JoglPolyhedralBoundedSolidDebugger;

/**
This class follows the Singleton design pattern. Its sole function is to
create an offline renderer for debugging PolyhedralBoundedSolid objects.

As current implementation of the offline renderer is implemented on
JOGL, it is possible this causes trouble on some platforms. In the event
of a compiling error due to unavailable JOGL libraries, this class could
be reprogrammed to return null.
*/
public abstract class PolyhedralBoundedSolidDebugger
{
    public static PolyhedralBoundedSolidDebugger createOfflineRenderer()
    {
        // Comment out this line if JOGL is not available at compile time
	return new JoglPolyhedralBoundedSolidDebugger();

        // Return null if JOGL is not available at compile time
        //return null;
    }

    public abstract void execute(PolyhedralBoundedSolid solid, String filename);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
