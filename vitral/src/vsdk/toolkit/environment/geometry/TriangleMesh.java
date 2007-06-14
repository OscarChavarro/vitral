//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Gabriel Sarmiento / Lina Rojas: Original base version =
//= - May 2 2006 - Oscar Chavarro: quality check, doIntersection doc/test   =
//= - May 3 2006 - Oscar Chavarro: fixed doIntersection error when testing  =
//=       back facing triangles                                             =
//= - November 6 2006 - Oscar Chavarro: introduced bounding box and normal  =
//=       interpolation                                                     =
//= - November 13 2006 - Oscar Chavarro: re-structured and tested           =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.scene.SimpleBody;

/**
This class represents a "basic" triangle mesh. Its model is based in a set
of vertexes and triangles (the edges are not store explicitly, and there
can not be an edge not forming part of a triangle).

This basic triangle mesh model can be associated with one and only one
material (this could change in future), but can have multiple textures,
and each texture can be mapped to a different set of triangles.

As every model class or `Entity` in VSDK, this class only can represent
(store in memory) the mesh model. It doesn´t provide persistence or rendering
functionality, as this could be found at `io` and `render` packages.
Nevertheles, this class will be highly coupled with both of those, so
making any change here will impact highly that code.

This class does not ensure nor impose data integrity, and this will be the 
sole responsability of the cooperating utilities and applications.

@todo Document more this class (include samples and data structure diagrams)
@todo Generalize the material usage model, to conform similarly to current
      texture usage (i.e. allow multiple materials per mesh)
@todo Make sure this is always using good names with complete words on it
      (rename methods and attributes)
@todo Extend the model to allow dangling edges
*/
public class TriangleMesh extends Surface {

//= Class attributes ========================================================

    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060807L;

    // Basic mesh data model
    private String name = "default";
    private Vertex[] vertexes;
    private Triangle[] triangles;

    // Auxiliary components for data model
    private Material[] materials;
    private Image[] textures;

    private Vector3D[] verTex;

    /**
    textureRanges is a 2D array which contents mappings between the
    `triangles` and `textures` sets. Each pair 
    <textureRanges[i][0], textureRanges[i][1]> means that the triangles
    from textureRanges[i-1][0] to textureRanges[i][0] (from triangle 0 
    when i = 0), are associated with the texture textureRanges[i][1]
    (or no texure for unspecified range or when textureRanges[i][1]
    contains a value out of textures array bounds).
    */
    private int[][] textureRanges;

    /**
    materialRanges is a 2D array which contents mappings between the
    `triangles` and `materials` sets. Each pair 
    <materialRanges[i][0], materialRanges[i][1]> means that the triangles
    from materialRanges[i-1][0] to materialRanges[i][0] (from triangle 0 
    when i = 0), are associated with the material materialRanges[i][1]
    (or no texure for unspecified range or when materialRanges[i][1]
    contains a value out of materials array bounds).
    */
    private int[][] materialRanges;

    // Auxiliary data structures for storage of parcial results and 
    // preprocessing
    private double[] minMax;
    private int selectedTriangle;
    private SimpleBody boundingVolume;
    private GeometryIntersectionInformation lastInfo;

//= Basic class management methods ==========================================

    public TriangleMesh() {
        lastInfo = new GeometryIntersectionInformation();
        boundingVolume = null;
    }

    public TriangleMesh(Vertex[] vertexes, Triangle[] triangles) {
        this.vertexes = vertexes;
        this.triangles = triangles;
        minMax = null;
        lastInfo = new GeometryIntersectionInformation();
        boundingVolume = null;
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

    public Material[] getMaterials() {
        return this.materials;
    }

    public Image[] getTextures() {
        return this.textures;
    }

    public Image getTextureAt(int index) {
        return this.textures[index];
    }

    public Vector3D[] getVerTexture() {
        return this.verTex;
    }

    public Vector3D getVerTextureAt(int index) {
        return this.verTex[index];
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVertexes(Vertex[] vertexes) {
        this.vertexes = vertexes;
        boundingVolume = null;
    }

    public void setTriangles(Triangle[] triangles) {
        this.triangles = triangles;
        boundingVolume = null;
    }

    public void setTextures(Image[] textures) {
        this.textures = textures;
    }

    public void setMaterials(Material[] materials) {
        this.materials = materials;
    }

    public void setVerTexture(Vector3D[] verTex) {
        this.verTex = verTex;
    }

    public void setTrianglesSize(int size) {
        this.triangles = new Triangle[size];
        boundingVolume = null;
    }

    public void setVertexesSize(int size) {
        this.vertexes = new Vertex[size];
        boundingVolume = null;
    }

    public void setTexturesSize(int size) {
        this.textures = new Image[size];
        boundingVolume = null;
    }

    public void setVerTextureSize(int size) {
        this.verTex = new Vector3D[size];
        boundingVolume = null;
    }

    public void setVertexAt(int index, Vertex vertex) {
        this.vertexes[index] = vertex;
        boundingVolume = null;
    }

    public void setVerTextureAt(int index, Vector3D verTex) {
        this.verTex[index] = verTex;
        boundingVolume = null;
    }

    public void setTriangleAt(int index, Triangle triangle) {
        this.triangles[index] = triangle;
        boundingVolume = null;
    }

    public void setTextureAt(int index, Image image) {
        this.textures[index] = image;
        boundingVolume = null;
    }

//= Methods for managing textureRanges ======================================

    public int[][] getTextureRanges() {
        return textureRanges;
    }

    /**
    Note this always returns an array with two (2) integers: the first one
    is an index to `triangles` array, the second one is an index to the
    `textures` array.
    */
    public int[] getTextureRangeAt(int spanRange) {
        return textureRanges[spanRange];
    }

    public void setTextureRanges(int ranges[][]) {
        textureRanges = ranges;
    }

//= Methods for managing materialRanges =====================================

    public int[][] getMaterialRanges() {
        return materialRanges;
    }

    /**
    Note this always returns an array with two (2) integers: the first one
    is an index to `triangles` array, the second one is an index to the
    `materials` array.
    */
    public int[] getMaterialRangeAt(int spanRange) {
        return materialRanges[spanRange];
    }

    public void setMaterialRanges(int ranges[][]) {
        materialRanges = ranges;
    }

//= Fundamental geometry operations methods =================================

    public void calculateNormals() {
        boundingVolume = null;
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
        boundingVolume = null;
        if ( minMax == null ) {
            minMax = new double[6];

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
            minMax[0] = minX;
            minMax[1] = minY;
            minMax[2] = minZ;
            minMax[3] = maxX;
            minMax[4] = maxY;
            minMax[5] = maxZ;
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

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    */
    public String toString() {
        return ("TriangleMesh < #Triangles:" + triangles.length + 
                            " - #Vertexes:" + vertexes.length + " >");
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
        Vector3D n0, n1, n2;  // Normals of the three triangle points
        Vector3D u, v, n;     // Edge vectors and normal
        Vector3D p;           // Point of intersection between ray and plane
        double t, a, b, d;    // Coefficients for solving equation (2)
        double s1, s2, s3;    // Side test for each of triangle border

        // Bounding volume check
        if ( boundingVolume == null ) {
            double[] mm = getMinMax();
            Vector3D size, center;
            size = new Vector3D(mm[3]-mm[0], mm[4]-mm[1], mm[5]-mm[2]);
            center = new Vector3D((mm[3]+mm[0])/2,
                                  (mm[4]+mm[1])/2,
                                  (mm[5]+mm[2])/2);
            boundingVolume = new SimpleBody();
            boundingVolume.setPosition(center);
            boundingVolume.setGeometry(new Box(size));
        }
        if ( !boundingVolume.doIntersection(inOut_Ray) ) {
            return false;
        }

        // Initialization values for search algorithm
        min_t = Double.MAX_VALUE;
        intersection = false;
        selectedTriangle = 0;

        // For each triangle in the mesh ...
        for ( i = 0; i < triangles.length; i++ ) {
            // The Triangle i has vertices <v0, v1, v2>
            v0 = vertexes[triangles[i].p0].position;
            v1 = vertexes[triangles[i].p1].position;
            v2 = vertexes[triangles[i].p2].position;

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

                        //if ( withNormalInterpolation ) {
                        // Normal interpolation
                        n0 = vertexes[triangles[i].p0].normal;
                        n1 = vertexes[triangles[i].p1].normal;
                        n2 = vertexes[triangles[i].p2].normal;

                        // Obtain barycentric coordinates for point p
                        // Method taken from wikipedia
                        double A, B, C, D, E, F, G, H, I;
                        double lambda1, lambda2, lambda3;

                        A = v0.x - v2.x;
                        B = v1.x - v2.x;
                        C = v2.x - p.x;
                        D = v0.y - v2.y;
                        E = v1.y - v2.y;
                        F = v2.y - p.y;
                        G = v0.z - v2.z;
                        H = v1.z - v2.z;
                        I = v2.z - p.z;

                        // Recalculate n as the barycentric normal 
                        // interpolation of three vertex normals
                        lambda1 = (B*(F+I)-C*(E+H))/(A*(E+H)-B*(D+G));
                        lambda2 = (A*(F+I)-C*(D+G))/(B*(D+G)-A*(E+H));
                        lambda3 = 1-lambda1-lambda2;
                        n = n0.multiply(lambda1).
                            add(n1.multiply(lambda2).
                            add(n2.multiply(lambda3)));
                        n.normalize();
                        //}

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
        if ( minMax == null ) {
            calculateMinMaxPositions();
        }
        return minMax;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
