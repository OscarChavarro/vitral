//===========================================================================

package vitral.toolkits.environment;

public class Material {
    public float ir, ig, ib;        // Color intrinseco de la superficie
    public float ka, kd, ks, ns;    // Constantes para el modelo phong
    public float kt, kr, nt;

    public Material(float rval, float gval, float bval, float a, float d, 
                    float s, float n, float r, float t, float index) {
        ir = rval; ig = gval; ib = bval;
        ka = a; kd = d; ks = s; ns = n;
        kr = r; kt = t;
        nt = index;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
