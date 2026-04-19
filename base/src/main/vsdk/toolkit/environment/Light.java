package vsdk.toolkit.environment;
import java.io.Serial;
import java.util.concurrent.atomic.AtomicInteger;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.ColorRgb;

// All the public variables here are ugly, but I
// wanted Lights and Surfaces to be "friends"
public class Light extends Entity
{
    @Serial private static final long serialVersionUID = 20060502L;

    public static final int AMBIENT = 0;
    public static final int DIRECTIONAL = 1;
    public static final int POINT = 2;

    private static final AtomicInteger lightNumber = new AtomicInteger(0);

    public int tipo_de_luz;
    public Vector3D lvec;             // the position of a point light or
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

    public Light(int type, Vector3D pos, ColorRgb emission) 
    {
        tipo_de_luz = type;
        lvec = new Vector3D();
        if ( type != AMBIENT ) {
            lvec = Vector3D.copyOf(pos);
            if ( type == DIRECTIONAL ) 
            {
                lvec = lvec.normalized();
            }
        }
        ambient=new ColorRgb(0,0,0);
        diffuse=new ColorRgb(1,1,1);
        specular=emission;
        id = lightNumber.getAndIncrement();
    }

    public int getId()
    {
        return id;
    }

    public void setId(int i)
    {
        id = i;
        //lightNumber = i+1;
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
        lvec = Vector3D.copyOf(pos);
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
