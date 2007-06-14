package vsdk.toolkit.common;

public class Triangle extends Entity 
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    public int p0;
    public int p1;
    public int p2;

    public int vt0;
    public int vt1;
    public int vt2;

    /**
     */
    public Vector3D normal;

    /**
     */
    public Triangle() {
        normal = new Vector3D(0, 0, 0);
    }

    /**
     */
    public Triangle(int p0, int p1, int p2) {
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        normal = new Vector3D(0, 0, 0);
    }

    public Triangle(int p0, int p1, int p2, int vt0, int vt1, int vt2) {
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        normal = new Vector3D(0, 0, 0);
        this.vt0 = vt0;
        this.vt1 = vt1;
        this.vt2 = vt2;
    }

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    */
    public String toString() {

        return "f < " + this.p0 + ", " + this.p1 + ", " +
            this.p2 + " >";
    }

    /**
     */
    public int getPoint0() {
        return this.p0;
    }

    /**
     */
    public int getPoint1() {
        return this.p1;
    }

    /**
     */
    public int getPoint2() {
        return this.p2;
    }

    public int getVt0()
    {
        return this.vt0;
    }

    public int getVt1()
    {
        return this.vt1;
    }

    public int getVt2()
    {
        return this.vt2;
    }


    /**
     */
    public void setPoint0(int p0) {
        this.p0 = p0;
    }

    /**
     */
    public void setPoint1(int p1) {
        this.p1 = p1;
    }

    /**
     */
    public void setPoint2(int p2) {
        this.p2 = p2;
    }

    public void setVt0(int vt0)
    {
        this.vt0 = vt0;
    }

    public void setVt1(int vt1)
    {
        this.vt1 = vt1;
    }

    public void setVt2(int vt2)
    {
        this.vt2 = vt2;
    }
}
