package vsdk.toolkit.common;
import java.io.Serial;

public class Vertex2D extends FundamentalEntity
{
    @Serial private static final long serialVersionUID = 20090816L;

    public double x;
    public double y;
    public ColorRgb color;

    public Vertex2D(double x, double y)
    {
        this.x = x;
        this.y = y;
        this.color = new ColorRgb();
    }

    public Vertex2D(double x, double y, double r, double g, double b)
    {
        this.x = x;
        this.y = y;
        this.color = new ColorRgb(r, g, b);
    }

    @Override
    public String toString()
    {
        String msg = "<" + VSDK.formatDouble(x) + ", " +
            VSDK.formatDouble(y) + ">";
        return msg;
    }
}
