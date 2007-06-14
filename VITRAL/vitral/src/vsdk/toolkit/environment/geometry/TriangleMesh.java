//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Gabriel Sarmiento / Lina Rojas: Original base version =
//= - May 2 2006 - Oscar Chavarro: quality check, doIntersection doc/test   =
//= - May 3 2006 - Oscar Chavarro: fixed doIntersection error when testing  =
//=       back facing triangles                                             =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.environment.Material;

public class TriangleMesh extends Surface {

    // Mesh data model
    private String name = "default";
    private Vertex[] vertexes;
    private Triangle[] triangles;

    // Auxiliary components for data model, should be extracted from here?
    private RGBAImage[] textures;
    private int[][][] texTriRel;
    private Vector3D[] verTex;
    private Material material;

    // Auxiliary data structures for storage of parcial results and 
    // preprocessing
    private double[] MinMax;
    private GeometryIntersectionInformation lastInfo;
    private int selectedTriangle;

    public TriangleMesh() {
        lastInfo = new GeometryIntersectionInformation();
    }

    public TriangleMesh(Vertex[] vertexes, Triangle[] triangles) {
        this.vertexes = vertexes;
        this.triangles = triangles;
        MinMax = null;
        lastInfo = new GeometryIntersectionInformation();
    }

    public String getName() {
        return this.name;
    }

    public Vertex[] getVertexes() {
        return this.vertexes;
    }

    public Vertex getVertexAt(int index) {
        return this.vertexes[index];
    }

    public Triangle[] getTriangles() {
        return this.triangles;
    }

    public Triangle getTriangleAt(int index) {
        return this.triangles[index];
    }

    public double getMinMaxAt(int index) {
        return this.MinMax[index];
    }

    public RGBAImage[] getTextures() {
        return this.textures;
    }

    public RGBAImage getTextureAt(int index) {
        return this.textures[index];
    }

    public int[][][] getTextTriRel() {
        return this.texTriRel;
    }

    public int[][] getTexTriRelAt(int index) {
        return this.texTriRel[index];
    }

    public int getTextTriRelAt(int i, int j, int k) {
        return this.texTriRel[i][j][k];
    }

    public Vector3D[] getVerTexture() {
        return this.verTex;
    }

    public Vector3D getVerTextureAt(int index) {
        return this.verTex[index];
    }

    public Material getMaterial() {
        return this.material;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVertexes(Vertex[] vertexes) {
        this.vertexes = vertexes;
    }

    public void setTriangles(Triangle[] triangles) {
        this.triangles = triangles;
    }

    public void setTextures(RGBAImage[] textures) {
        this.textures = textures;
    }

    public void setVerTexture(Vector3D[] verTex) {
        this.verTex = verTex;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setTrianglesSize(int size) {
        this.triangles = new Triangle[size];
    }

    public void setVertexesSize(int size) {
        this.vertexes = new Vertex[size];
    }

    public void setTexturesSize(int size) {
        this.textures = new RGBAImage[size];
    }

    public void setVerTextureSize(int size) {
        this.verTex = new Vector3D[size];
    }

    public void setTexTriRelSize(int size) {
        this.texTriRel = new int[size][][];
    }

    public void setTexTriRel(int r[][][]) {
        this.texTriRel = r;
    }

    public void setTexTriRelSizeAt(int index, int size) {
        this.texTriRel[index] = new int[size][2];
    }

    public void setTextTriRelAt(int i, int j, int[] ttr) {
        this.texTriRel[i][j] = ttr;
    }

    public void setVertexAt(int index, Vertex vertex) {
        this.vertexes[index] = vertex;
    }

    public void setVerTextureAt(int index, Vector3D verTex) {
        this.verTex[index] = verTex;
    }

    public void setTriangleAt(int index, Triangle triangle) {
        this.triangles[index] = triangle;
    }

    public void setTextureAt(int index, RGBAImage rgbaImage) {
        this.textures[index] = rgbaImage;
    }

    public void calculateNormals() {
        for (int i = 0; i < this.triangles.length; i++) {
            Vertex v1 = vertexes[triangles[i].getPoint0()];
            Vertex v2 = vertexes[triangles[i].getPoint1()];
            Vertex v3 = vertexes[triangles[i].getPoint2()];

            double ax = v2.getPosition().x - v1.getPosition().x;
            double ay = v2.getPosition().y - v1.getPosition().y;
            double az = v2.getPosition().z - v1.getPosition().z;

            double bx = v3.getPosition().x - v2.getPosition().x;
            double by = v3.getPosition().y - v2.getPosition().y;
            double bz = v3.getPosition().z - v2.getPosition().z;

            Vector3D a = new Vector3D(ax, ay, az);
            Vector3D b = new Vector3D(bx, by, bz);

            a.normalize();
            b.normalize();

            triangles[i].normal = a.crossProduct(b);
            triangles[i].normal.normalize();
        }

        ArrayList<ArrayList<vsdk.toolkit.common.Triangle>> vecinos;
        vecinos = new ArrayList<ArrayList<vsdk.toolkit.common.Triangle>> (vertexes.length);

        for (int i = 0; i < vertexes.length; i++) {
            vecinos.add(new ArrayList<Triangle> ());
        }

        for (int i = 0; i < this.triangles.length; i++) {
            vecinos.get(triangles[i].getPoint0()).add(triangles[i]);
            vecinos.get(triangles[i].getPoint1()).add(triangles[i]);
            vecinos.get(triangles[i].getPoint2()).add(triangles[i]);
        }

        for (int i = 0; i < vertexes.length; i++) {
            vertexes[i].setNormal(new Vector3D(0, 0, 0));
            for (int j = 0; j < vecinos.get(i).size(); j++) {
                vertexes[i].getNormal().x += vecinos.get(i).get(j).normal.x;
                vertexes[i].getNormal().y += vecinos.get(i).get(j).normal.y;
                vertexes[i].getNormal().z += vecinos.get(i).get(j).normal.z;
            }
            vertexes[i].getNormal().normalize();
            vertexes[i].setIncidentTriangles(vecinos.get(i));
        }
    }

    /** Needed for supplying the Geometry.getMinMax operation */
    private void calculateMinMaxPositions() {
        if ( MinMax == null ) {
            MinMax = new double[6];

            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;
            double maxZ = Double.MIN_VALUE;
            int i;

            for ( i = 0; i < vertexes.length; i++ ) {
                double x = vertexes[i].getPosition().x;
                double y = vertexes[i].getPosition().y;
                double z = vertexes[i].getPosition().z;

                if ( x < minX ) minX = x;
                if ( y < minY ) minY = y;
                if ( z < minZ ) minZ = z;
                if ( x > maxX ) maxX = x;
                if ( y > maxY ) maxY = y;
                if ( z > maxZ ) maxZ = z;
            }
            MinMax[0] = minX;
            MinMax[1] = minY;
            MinMax[2] = minZ;
            MinMax[3] = maxX;
            MinMax[4] = maxY;
            MinMax[5] = maxZ;
        }
    }

    /**
    This method is supposed to be a friend of TriangleMesh related objects.
    The method is used to query the last intersected triangle, after a
    positive called to the doIntersection method.
    */
    public int doIntersectionInformation() {
        return selectedTriangle;
    }

    public String toString() {
        return ("TriangleMesh < #T:" + triangles.length + " - #V" +
                vertexes.length + " - #N:" + " >");
    }

    /**
    @deprecated
    */
    public void printTriangleMesh() {
        System.out.println(this.toString());
        for (int i = 0; i < vertexes.length; i++) {
            System.out.println(vertexes[i]);
        }
        for (int i = 0; i < triangles.length; i++) {
            System.out.println(triangles[i]);
        }
    }

    /**
     Check the general interface contract in superclass method
     Geometry.doIntersection.

     SPECIFIC IMPLEMENTATION: this method solve the intersection problem
     for the TriangleMesh in two stages:
     <UL>
     <LI>For each Triangle, calculates the plane containing the Triangle
     and checks if the Ray intersects with that plane.
     <LI>If the Ray intersects the plane, a check is done to determine
     if the intersection is inside the Triangle.
     </UL>
     That logic y repeated for all the Triangles in the TriangleMesh, and
     the shortest length intersected Triangle is reported.<P>

     Precondition:
\f[
    \mathbf{Q} := inOut\_Ray.direction.length() = 1 \;
\f]

     NOTES:
     <UL>
     <LI>The plane normal is determined for each triangle as the cross product
     of two Triangle edge Vector3D's, algorithm step (1).
     <LI>The canonic equation for a plane with normal n is
\f[
        nx*x + ny*y +nz*z + d = 0
\f]
     <LI>The parametric equation for the ray inOut_Ray (call it r) is
\f[
        \vec p = \vec{r.o} + t * \vec{r.d}
\f]
     <LI>Combining those two equations and solving for parameter t, algorithm
     step (2), gives
\f[
        t = \frac{ -(nx*ox+ny*oy+nz*oz+d) }{ nx*dx+ny*dy+nz*dz }
\f]
     and observing that the appearing vector components can be expressed as
     dot product, this equation can be rewritten in the condensed vectorial
     form
\f[
        t = \frac{ -(\vec n \cdot \vec{r.o} +d) }
                               { \vec n \cdot \vec{r.d} }
\f]
     <LI>Scalar value d in that equation can be solve replacing the coordinates
     of any of the Triangle points into the plane equation.
     <LI>To check if an intersected point lies inside the triangle, a left/right
     test is done with each one of the three directed edge vectors. If all three
     tests pass, then the point is inside the triangle.
     <LI>If the normal and the direction of the ray are in the same direction
     (more than 90 degrees of vector angle) then the normal is inverted to manage
      meshes with reversed triangles.
     </UL>
    */
    public boolean
    doIntersection(Ray inOut_Ray) {
        int i;                // Index for iterating triangles
        boolean intersection; // true if intersection founded
        double min_t;         // Shortest distance founded so far
        Vector3D v0, v1, v2;  // Positions of the three triangle points
        Vector3D u, v, n;     // Edge vectors and normal
        Vector3D p;           // Point of intersection between ray and plane
        double t, a, b, d;    // Coefficients for solving equation (2)
        double s1, s2, s3;    // Side test for each of triangle border

        // Initialization values for search algorithm
        min_t = Double.MAX_VALUE;
        intersection = false;
        selectedTriangle = 0;

        // For each triangle in the mesh ...
        for ( i = 0; i < triangles.length; i++ ) {
            // The Triangle i has vertices <v0, v1, v2>
            v0 = vertexes[triangles[i].getPoint0()].getPosition();
            v1 = vertexes[triangles[i].getPoint1()].getPosition();
            v2 = vertexes[triangles[i].getPoint2()].getPosition();

            // The vectors u & v are two triangle edges, and define the 
            // normal (1)
            u = v1.substract(v0);
            v = v2.substract(v1);
            n = v.crossProduct(u);
            n.normalize();

            // This is the result of replacing point v0 on plane equation, 
            // solving for d
            d = -n.dotProduct(v0);

            // Calculate numerator and denominator for equation (2)
            a = n.dotProduct(inOut_Ray.origin) + d;
            b = n.dotProduct(inOut_Ray.direction);

            // The denominator is big when the ray is not parallel to the plane
            if ( Math.abs(b) > VSDK.EPSILON ) {
                // Solution for equation (2), only if non-zero denominator
                t = (-a) / b;

                if ( t < 0.0 ) continue;

                // Calculate the intersection point between ray and plane
                p = inOut_Ray.origin.add(inOut_Ray.direction.multiply(t));

                // Check if the point is inside the triangle
                s1 = u.crossProduct(p.substract(v0)).dotProduct(n);
                s2 = (v2.substract(v1)).crossProduct(p.substract(v1)).dotProduct(n);
                s3 = (v0.substract(v2)).crossProduct(p.substract(v2)).dotProduct(n);

                if ( (s1 >= 0 && s2 >= 0 && s3 >= 0) || 
                     (s1 <= 0 && s2 <= 0 && s3 <= 0) ) {
                    if ( t < min_t ) {
                        // Normal is always pointed "outwards" with respect to 
                        // the triangle (this manages the issue of back-facing
                        // normals)
                        if ( n.dotProduct(inOut_Ray.direction) < 0 ) {
                            lastInfo.n = n;
                          }
                          else {
                            lastInfo.n = n.multiply(-1);
                        }

                        lastInfo.p = p;
                        inOut_Ray.t = t;
                        min_t = t;
                        selectedTriangle = i;
                        intersection = true;
                    }
                }
            }
        }

        return intersection;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    */
    public void doExtraInformation(Ray inRay, double inT,
                                   GeometryIntersectionInformation outData) {
        outData.p.x = lastInfo.p.x;
        outData.p.y = lastInfo.p.y;
        outData.p.z = lastInfo.p.z;
        outData.n.x = lastInfo.n.x;
        outData.n.y = lastInfo.n.y;
        outData.n.z = lastInfo.n.z;
    }

    /**
     Check the general interface contract in superclass method
     Geometry.getMinMax.
    */
    public double[] getMinMax() {
        if ( MinMax == null ) {
            calculateMinMaxPositions();
        }
        return MinMax;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
