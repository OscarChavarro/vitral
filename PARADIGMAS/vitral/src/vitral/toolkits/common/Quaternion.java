package vitral.toolkits.common;

public class Quaternion
{
    public Vector3D direction;
    public double magnitude;

    public Quaternion()
    {
        direction = new Vector3D(0, 0, 0);
        magnitude = 0;
    }

    public double length()
    {
        return magnitude * magnitude + direction.dotProduct(direction);
    }

    public void normalize()
    {
        double l;

        l = length();
        magnitude *= 1/l;
        direction = direction.multiply(1/l);
    }
}
