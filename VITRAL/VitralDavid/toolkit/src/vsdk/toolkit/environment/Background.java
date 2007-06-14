//===========================================================================
package vsdk.toolkit.environment;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;

public abstract class Background {
    public Background() {
        ;
    }
    public abstract ColorRgb colorInDireccion(Vector3D d);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
