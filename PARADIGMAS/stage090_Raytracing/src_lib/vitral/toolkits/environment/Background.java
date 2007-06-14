//===========================================================================
package vitral.toolkits.environment;

import vitral.toolkits.common.ColorRgb;
import vitral.toolkits.common.Vector3D;

public abstract class Background {
    public Background() {
        ;
    }
    public abstract ColorRgb color_en_direccion(Vector3D d);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
