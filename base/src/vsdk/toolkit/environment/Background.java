//===========================================================================
package vsdk.toolkit.environment;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;

public abstract class Background extends Entity
{
    public Background() {

    }
    public abstract ColorRgb colorInDireccion(Vector3D d);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
