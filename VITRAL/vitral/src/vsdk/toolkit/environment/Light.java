//===========================================================================

package vsdk.toolkit.environment;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.ColorRgb;

// All the public variables here are ugly, but I
// wanted Lights and Surfaces to be "friends"
public class Light extends Entity
{
    public static final int AMBIENT = 0;
    public static final int DIRECTIONAL = 1;
    public static final int POINT = 2;

    private static int lightNumber = 0;

    public int tipo_de_luz;
    public Vector3D lvec;             // the position of a point light or
                                      // the direction to a directional light
    private ColorRgb ambient;
    private ColorRgb diffuse;
    private ColorRgb specular;         // Emission color of the light source
    private int id;

    public Light(int type, Vector3D pos, ColorRgb emission) 
    {
        tipo_de_luz = type;
        if ( type != AMBIENT ) {
            lvec = pos;
            if ( type == DIRECTIONAL ) 
            {
                lvec.normalize();
            }
        }
        ambient=new ColorRgb(0,0,0);
        diffuse=new ColorRgb(1,1,1);
        specular=emission;
        id = lightNumber;
        lightNumber++;
    }

    public int getId()
    {
        return lightNumber;
    }

    public void setId(int i)
    {
        id = i;
        lightNumber = i+1;
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

    public Vector3D getPosition()
    {
        return lvec;
    }

    public void setPosition(Vector3D pos)
    {
        lvec = new Vector3D(pos);
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

    public int getLightType()
    {
        return tipo_de_luz;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
