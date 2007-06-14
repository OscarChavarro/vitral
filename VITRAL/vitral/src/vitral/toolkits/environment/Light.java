//===========================================================================

package vitral.toolkits.environment;

import vitral.toolkits.common.Vector3D;
import vitral.toolkits.common.ColorRgb;

// All the public variables here are ugly, but I
// wanted Lights and Surfaces to be "friends"
public class Light
{
    public static final int AMBIENTE = 0;
    public static final int DIRECCIONAL = 1;
    public static final int PUNTUAL = 2;

    public int tipo_de_luz;
    public Vector3D lvec;             // the position of a point light or
                                      // the direction to a directional light
    private ColorRgb ambient;
    private ColorRgb diffuse;
    private ColorRgb specular;         // Emission color of the light source

    public Light(int type, Vector3D pos, ColorRgb emission) 
    {
        tipo_de_luz = type;
        if ( type != AMBIENTE ) {
            lvec = pos;
            if ( type == DIRECCIONAL ) 
            {
                lvec.normalize();
            }
        }
        ambient=new ColorRgb(0,0,0);
        diffuse=new ColorRgb(1,1,1);
        specular=emission;
    }

    public void setAmbient(ColorRgb a)
    {
        this.ambient=new ColorRgb(a);
    }
    
    public void setDiffuse(ColorRgb d)
    {
        this.diffuse=new ColorRgb(d);
    }
    
    public void setSpecular(ColorRgb s)
    {
        specular=new ColorRgb(s);
    }
    
    public ColorRgb getAmbient()
    {
        return new ColorRgb(ambient);
    }

    public ColorRgb getDiffuse()
    {
        return new ColorRgb(diffuse);
    }

    public ColorRgb getSpecular()
    {
        return new ColorRgb(specular);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
