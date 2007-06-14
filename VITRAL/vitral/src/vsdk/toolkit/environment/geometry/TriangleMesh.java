//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Gabriel Sarmiento / Lina Rojas: Original base version =
//= - April 28 2006 - Oscar Chavarro: quality group                         =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.environment.Material;

public class TriangleMesh extends Geometry {
    private String name = "default";
    private Vertex[] vertexes;
    private Triangle[] triangles;
    private double[] MinMax;

    private RGBAImage[] textures;
    private int[][][] texTriRel;

    private Vector3D[] verTex;

    private Material material;

    private Vector3D _static_delta;

    private GeometryIntersectionInformation lastInfo;

    private int selectedTriangle;

    public TriangleMesh() {
        _static_delta = new Vector3D();
        lastInfo = new GeometryIntersectionInformation();
    }

    public TriangleMesh(Vertex[] vertexes, Triangle[] triangles) {
        this.vertexes = vertexes;
        this.triangles = triangles;
        MinMax = null;
        _static_delta = new Vector3D();
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

    public double[] getMinMax() {
        return this.MinMax;
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

    public void setVerTexure(Vector3D[] verTex) {

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

    public void setVerTexureSize(int size) {
        this.verTex = new Vector3D[size];
    }

    public void setTexTriRelSize(int size) {
        this.texTriRel = new int[size][][];
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
        vecinos = new ArrayList<ArrayList<vsdk.toolkit.common.Triangle>> (vertexes.
                                                                          length);

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

/**
 *
 *    0 - min (x)
 *    1 - min (y)
 *    2 - min (z)
 *    3 - max (x)
 *    4 - max (y)
 *    5 - max (z)
 * @return double[]
 */
    public void calculateMinMaxPositions() {
        if (MinMax == null) {
            MinMax = new double[6];

            boolean first = true;

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE,
                minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE,
                maxZ = Double.MIN_VALUE;

            for (int i = 0; i < vertexes.length; i++) {
                double x = vertexes[i].getPosition().x;
                double y = vertexes[i].getPosition().y;
                double z = vertexes[i].getPosition().z;

                if (first) {
                    minX = x;
                    maxX = x;
                    minY = y;
                    maxY = y;
                    minZ = z;
                    maxZ = z;
                    first = false;
                }

                if (x < minX) {
                    minX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (z < minZ) {
                    minZ = z;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y > maxY) {
                    maxY = y;
                }
                if (z > maxZ) {
                    maxZ = z;
                }
            }
            MinMax[0] = minX;
            MinMax[1] = minY;
            MinMax[2] = minZ;
            MinMax[3] = maxX;
            MinMax[4] = maxY;
            MinMax[5] = maxZ;
        }
    }

    public String toString() {
        return ("TriangleMesh < #T:" + triangles.length + " - #V" +
                vertexes.length + " - #N:" + " >");
    }

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
   Dado un Ray `inout_rayo`, esta operaci&oacute;n determina si el rayo se
   intersecta con la malla de triangulos. Si el rayo no intersecta
   al objeto se retorna 0, y de lo contrario se retorna la distancia desde
   el origen del rayo hasta el punto de interseccion.

   Busca la interseccion del rayo con un plano generado a partir de cada
   triangulo y existe la interseccion se verifica si el punto esta contenido
   dentro del triangulo mediante el producto punto de los vectores unitarios
   del triangulo
   Referencia: http://www.inmensia.com/articulos/raytracing/planotrianguloycubo.html
*/
    public boolean
    doIntersection(Ray inOut_Ray) {

        boolean intersection = false;

        Vector3D v0, v1, v2, u, v, w, n, I;
        double t, a, b, d;

        this._static_delta.x = Double.MAX_VALUE;
        this._static_delta.y = Double.MAX_VALUE;
        this._static_delta.z = Double.MAX_VALUE;
//recorre todos los triangulos de la malla
        for (int i = 0; i < triangles.length; i++) {
//vertices del triangulo
            v0 = vertexes[triangles[i].getPoint0()].getPosition();
            v1 = vertexes[triangles[i].getPoint1()].getPosition();
            v2 = vertexes[triangles[i].getPoint2()].getPosition();
//vectores que forman el triangulo
            u = v1.substract(v0);
            v = v2.substract(v0);

//vector normal a la superficie del plano que contiene al trinangulo
            n = u.crossProduct(v);
            n.normalize();

            d = -n.dotProduct(v0);

            a = n.dotProduct(inOut_Ray.origin) + d;
            b = n.dotProduct(inOut_Ray.direction);

// si el rayo no es paralelo al plano del triangulo
            if (b != 0) {
                t = - (a / b);
//  if ( t < 0 ) return false;
// I = R(t)
//punto de interseccion entre el rayo y el plano que contiene el triangulo

                I = inOut_Ray.origin.add(inOut_Ray.direction.multiply(t));

//verifica si el punto esta contenido dentro del triangulo

                double s1, s2, s3;
                s1 = (v1.substract(v0)).crossProduct(I.substract(v0)).dotProduct(n);
                s2 = (v2.substract(v1)).crossProduct(I.substract(v1)).dotProduct(n);
                s3 = (v0.substract(v2)).crossProduct(I.substract(v2)).dotProduct(n);

                if ( (s1 >= 0 && s2 >= 0 && s3 >= 0) || (s1 <= 0 && s2 <= 0 && s3 <= 0)) {
                    if (I.substract(inOut_Ray.origin).length() <
                        _static_delta.substract(inOut_Ray.origin).length()) {
                        this._static_delta.x = I.x;
                        this._static_delta.y = I.y;
                        this._static_delta.z = I.z;
                        lastInfo.p = I;
                        lastInfo.n = n;
                        inOut_Ray.t = t;

                        selectedTriangle = i;
                        intersection = true;
                    }
                }
            }
        }
        return intersection;
    }

    public void doExtraInformation(Ray inRay, double inT,
                                   GeometryIntersectionInformation outData) 
    {
        outData.p = lastInfo.p;
        outData.n = lastInfo.n;
    }

    public int doIntersectionInformation()
    {
            return selectedTriangle;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
