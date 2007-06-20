//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 18 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.media;

public abstract class ShapeDescriptor extends MediaEntity
{
    protected String label;

    public ShapeDescriptor(String label)
    {
        setLabel(label);
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = new String(label);
    }

    public double [] getFeatureVector() {
        return null;
    }

    public void setFeatureVector(double vector[]) {
        ;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
