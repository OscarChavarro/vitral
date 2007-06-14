package vsdk.toolkit.common;

import java.util.ArrayList;

public class Vertex extends Entity
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    ///
    public Vector3D position;

    ///
    public Vector3D normal;

    ///
    public Vector3D binormal;

    ///
    public Vector3D tangent;

    /// Texture coordinates
    public double u;
    public double v;

    ///
    public ArrayList<Triangle> incidentTriangles;

    public Vertex() {
        position = null;
        normal = null;
        binormal = null;
        tangent = null;
        this.incidentTriangles = null;
        u = 0.0;
        v = 0.0;
    }

    public Vertex(double x, double y, double z) {
        this.position = new Vector3D(x, y, z);
        normal = new Vector3D(1, 0, 0);
        binormal = new Vector3D(0, 1, 0);
        tangent = new Vector3D(0, 0, 1);
        incidentTriangles = null;
        u = 0.0;
        v = 0.0;
    }

    public Vertex(Vector3D position) {
        this.position = position;
        normal = new Vector3D(1, 0, 0);
        binormal = new Vector3D(0, 1, 0);
        tangent = new Vector3D(0, 0, 1);
        incidentTriangles = null;
        u = 0.0;
        v = 0.0;
    }

    public Vertex(Vector3D position, Vector3D normal) {
        this.position = position;
        this.normal = normal;
        this.normal.normalize();

        binormal = new Vector3D(0, 1, 0);
        tangent = new Vector3D(0, 0, 1);

        binormal = null;
        tangent = null;
        incidentTriangles = null;
        u = 0.0;
        v = 0.0;
    }

    public Vertex(Vector3D position, Vector3D normal, Vector3D binormal,
                  Vector3D tangent) {
        this.position = position;
        this.normal = normal;
        this.binormal = binormal;
        this.tangent = tangent;
        incidentTriangles = null;
        u = 0.0;
        v = 0.0;
    }

    public Vertex(Vertex vertex) {
        this.position = vertex.position;
        this.normal = vertex.normal;
        this.binormal = vertex.binormal;
        this.tangent = vertex.tangent;
        this.incidentTriangles=vertex.getIncidentTriangles();
        this.u = vertex.u;
        this.v = vertex.v;
    }

    public Vector3D getPosition() {
        return this.position;
    }

    public Vector3D getNormal() {
        return normal;
    }

    public Vector3D getBinormal() {
        return this.binormal;
    }

    public Vector3D getTangent() {
        return this.tangent;
    }

    public double getU() {
        return this.u;
    }

    public double getV() {
        return this.v;
    }

    public ArrayList<Triangle> getIncidentTriangles() {
        return this.incidentTriangles;
    }

    public void setPosition(Vector3D position) {
        this.position = new Vector3D(position);
    }

    public void setNormal(Vector3D normal) {
        this.normal = new Vector3D(normal);
        this.normal.normalize();
    }

    public void setBinormal(Vector3D binormal) {
        this.binormal = binormal;
    }

    public void setTangent(Vector3D tangent) {
        this.tangent = tangent;
    }

    public void setU(double u) {
        this.u = u;
    }

    public void setV(double v) {
        this.v = v;
    }

    public void setIncidentTriangles(ArrayList<Triangle> incidentTriangles) {
        this.incidentTriangles = incidentTriangles;
    }

    public Triangle getIncidentTriangleAt(int index) {
        return (Triangle) incidentTriangles.get(index);
    }

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    */
    public String toString() {
        return "v < " + position.x + ", " + position.y + ", " +
            position.z + " > <"+u+", "+v+">";
    }
}
