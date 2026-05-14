package vsdk.toolkit.environment.light;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;

// All the public variables here are ugly, but I
// wanted Lights and Surfaces to be "friends"
public class Light extends Entity
{
    public int tipo_de_luz;
    public Vector3Dd lvec;             // the position of a point light or
                                      // the direction to a directional light
    private ColorRgb ambient;
    private ColorRgb diffuse;
    private ColorRgb specular;         // Emission color of the light source
    private int id;

    /// This string should be used for specific application defined
    /// functionality. Can be null.
    private String name;

    public String getName()
    {
        return name;
    }

    public void setName(String n)
    {
        name = n;
    }

    public Light(int type, Vector3Dd pos, ColorRgb emission) 
    {
        tipo_de_luz = type;
        lvec = new Vector3Dd();
        if ( type != LightType.AMBIENT ) {
            lvec = Vector3Dd.copyOf(pos);
            if ( type == LightType.DIRECTIONAL )
            {
                lvec = lvec.normalized();
            }
        }
        ambient=new ColorRgb(0,0,0);
        diffuse=new ColorRgb(1,1,1);
        specular=emission;
        id = 0;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int i)
    {
        id = i;
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

    public Vector3Dd getPosition()
    {
        return lvec;
    }

    public void setPosition(Vector3Dd pos)
    {
        lvec = Vector3Dd.copyOf(pos);
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

    public ColorRgb getSpecularReference()
    {
        return specular;
    }

    public int getLightType()
    {
        return tipo_de_luz;
    }

}
