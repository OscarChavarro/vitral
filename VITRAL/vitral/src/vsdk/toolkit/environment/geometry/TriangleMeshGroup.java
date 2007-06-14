//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Gabriel Sarmiento / Lina Rojas: Original base version =
//= - April 28 2006 - Oscar Chavarro: quality group                         =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.Vector3D;
import java.util.Iterator;

public class TriangleMeshGroup extends Surface {
    private ArrayList<TriangleMesh> meshes;
    private double[] MinMax;

    private GeometryIntersectionInformation lastInfo;
    private int[] intersectionInformation;

    public TriangleMeshGroup() {
        meshes = new ArrayList<TriangleMesh> ();
        MinMax = null;
        lastInfo = new GeometryIntersectionInformation();
    }

    public TriangleMeshGroup(ArrayList<TriangleMesh> meshes) {
        this.meshes = meshes;
        lastInfo = new GeometryIntersectionInformation();
    }

    public TriangleMeshGroup(TriangleMeshGroup group) {
        this.meshes = group.getMeshes();
        lastInfo = new GeometryIntersectionInformation();
    }

    public ArrayList<TriangleMesh> getMeshes() {
        return this.meshes;
    }

    public void setMeshes(ArrayList<TriangleMesh> meshes) {
        this.meshes = meshes;
    }

    public void addMesh(TriangleMesh mesh) {
        this.meshes.add(mesh);
    }

    public TriangleMesh getMeshAt(int index) {
        return (TriangleMesh)this.meshes.get(index);
    }

    public double[] getMinMax() {
        return this.MinMax;
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

            for (int i = 0; i < meshes.size(); i++) {
                double[] minmax_mesh = meshes.get(i).getMinMax();
                double x = minmax_mesh[0];
                double y = minmax_mesh[1];
                double z = minmax_mesh[2];
                double X = minmax_mesh[3];
                double Y = minmax_mesh[4];
                double Z = minmax_mesh[5];

                if (first) {
                    minX = x;
                    maxX = X;
                    minY = y;
                    maxY = Y;
                    minZ = z;
                    maxZ = Z;
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
                if (X > maxX) {
                    maxX = X;
                }
                if (Y > maxY) {
                    maxY = Y;
                }
                if (Z > maxZ) {
                    maxZ = Z;
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

    /**
     Check the general interface contract in superclass method
     Geometry.doIntersection.

       Dado un Ray `inout_rayo`, esta operaci&oacute;n determina si el rayo se
       intersecta con alguna de las mallas de triangulos. Si el rayo no intersecta
       al objeto se retorna 0, y de lo contrario se retorna la distancia desde
       el origen del rayo hasta el punto de interseccion mas cercano de todas las mallas.
     */
    public boolean doIntersection(Ray inOut_Ray) {
        int i;                // Index for iterating meshes
        boolean intersection; // true if intersection founded
        double min_t;         // Shortest distance founded so far
        GeometryIntersectionInformation Info;

        // Initialization values for search algorithm
        min_t = Double.MAX_VALUE;
        intersection = false;
        Info = new GeometryIntersectionInformation();

        for ( i= 0; i< meshes.size(); i++ ) {
            TriangleMesh mesh = meshes.get(i);
            Ray ray = new Ray(inOut_Ray);
            if ( mesh.doIntersection(ray) == true ) {
                mesh.doExtraInformation(ray, 0.0, Info);

                if ( ray.t < min_t ) {
                    min_t = ray.t;

                    // Stores standard doIntersection operation information
                    lastInfo.p.x = Info.p.x;
                    lastInfo.p.y = Info.p.y;
                    lastInfo.p.z = Info.p.z;
                    lastInfo.n.x = Info.n.x;
                    lastInfo.n.y = Info.n.y;
                    lastInfo.n.z = Info.n.z;
                    inOut_Ray.t = ray.t;

                    // Stores the intersected mesh and the triangle intersected inside that mesh
                    intersectionInformation = new int[2];
                    intersectionInformation[0] = i;
                    intersectionInformation[1] = mesh.doIntersectionInformation();

                    intersection = true;
                }
            }
        }
        return intersection;
    }

    public void doExtraInformation(Ray inRay, double inT,
                                   GeometryIntersectionInformation outData) {
        outData.p.x = lastInfo.p.x;
        outData.p.y = lastInfo.p.y;
        outData.p.z = lastInfo.p.z;
        outData.n.x = lastInfo.n.x;
        outData.n.y = lastInfo.n.y;
        outData.n.z = lastInfo.n.z;
    }

    public int[] doIntersectionInformation()
        {
            return intersectionInformation;
        }

    public String toString() {
        return "TriangleMeshGroup < #Mesh: " + this.meshes.size() + " >";
    }



}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
