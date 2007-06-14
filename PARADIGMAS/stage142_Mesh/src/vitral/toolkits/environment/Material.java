package vitral.toolkits.environment;

import vitral.toolkits.common.ColorRgb;

public class Material
{
    private ColorRgb ambient; 
    private ColorRgb diffuse; 
    private ColorRgb specular; 
    private ColorRgb emission; 
    
    private String name="default";
    
    private double alpha=1;
    private double phongExponent;
    
    /** Creates a new instance of MaterialGL */
    public Material() 
    {
        ambient=new ColorRgb(); 
        diffuse=new ColorRgb(); 
        specular=new ColorRgb(); 
        emission=new ColorRgb(); 
    }
    
    public Material(Material m) 
    {
        name=m.name;
        ambient=new ColorRgb(m.getAmbient()); 
        diffuse=new ColorRgb(m.getDiffuse()); 
        specular=new ColorRgb(m.getSpecular()); 
        emission=new ColorRgb(m.getEmission()); 
        alpha=m.getAlpha();
        phongExponent=m.phongExponent;
    }
    
    public void setName(String n)
    {
        name=n;
    }
    
    public String getName()
    {
        return name;
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
    
    public void setEmission(ColorRgb e)
    {
        emission=new ColorRgb(e);
    }
    
    public void setPhongExponenet(float p)
    {
        this.phongExponent=p;
    }
    
    public void setAlpha(double a)
    {
        this.alpha=a;
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

    public ColorRgb getEmission()
    {
        return new ColorRgb(emission);
    }

    public double getPhongExponent()
    {
        return phongExponent;
    }
    
    public double getAlpha()
    {
        return alpha;
    }
}
