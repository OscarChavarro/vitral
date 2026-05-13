package vsdk.toolkit.media;
import java.io.Serial;

public abstract class ShapeDescriptor extends MediaEntity
{
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    @Serial private static final long serialVersionUID = 20150218L;
    protected String label;

    public ShapeDescriptor(String label)
    {
        setLabel(label);
    }

    public String getLabel()
    {
        return label;
    }

    public final void setLabel(String label)
    {
        this.label = label;
    }

    public double [] getFeatureVector() {
        return null;
    }

    public void setFeatureVector(double vector[]) {

    }
}
