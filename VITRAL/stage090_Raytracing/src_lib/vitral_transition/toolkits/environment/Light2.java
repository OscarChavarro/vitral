//===========================================================================

package vitral_transition.toolkits.environment;

import vitral.toolkits.common.Vector3D;

// All the public variables here are ugly, but I
// wanted Lights and Surfaces to be "friends"
public class Light2 {
    public static final int AMBIENTE = 0;
    public static final int DIRECCIONAL = 1;
    public static final int PUNTUAL = 2;

    public int tipo_de_luz;
    public Vector3D lvec;             // the position of a point light or
                                    // the direction to a directional light
    public float ir, ig, ib;        // color of the light source

    public Light2(int type, Vector3D v, float r, float g, float b) {
        tipo_de_luz = type;
        ir = r;
        ig = g;
        ib = b;
        if ( type != AMBIENTE ) {
            lvec = v;
            if ( type == DIRECCIONAL ) {
                lvec.normalize();
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
