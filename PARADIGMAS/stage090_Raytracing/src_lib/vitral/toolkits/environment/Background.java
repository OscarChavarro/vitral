//===========================================================================
package vitral.toolkits.environment;

import vitral.toolkits.common.Color;
import vitral.toolkits.common.Vector3D;

public abstract class Background {
    public Background() {
        ;
    }
    public abstract Color color_en_direccion(Vector3D d);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
