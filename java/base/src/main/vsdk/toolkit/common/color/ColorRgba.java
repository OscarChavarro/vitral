package vsdk.toolkit.common.color;

public final class ColorRgba
{
    private double r;
    private double g;
    private double b;
    private double a;

    public ColorRgba()
    {
        this(0.0, 0.0, 0.0, 0.0);
    }

    public ColorRgba(double r, double g, double b, double a)
    {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public ColorRgba(ColorRgba other)
    {
        this(other.r, other.g, other.b, other.a);
    }

    public double getR() { return r; }
    public double getG() { return g; }
    public double getB() { return b; }
    public double getA() { return a; }

    public void setR(double v) { r = v; }
    public void setG(double v) { g = v; }
    public void setB(double v) { b = v; }
    public void setA(double v) { a = v; }

    public void set(ColorRgba other)
    {
        r = other.r;
        g = other.g;
        b = other.b;
        a = other.a;
    }
}
