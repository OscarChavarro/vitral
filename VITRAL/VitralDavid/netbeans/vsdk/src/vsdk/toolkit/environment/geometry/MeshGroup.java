package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.Vector3D;
import java.util.Iterator;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class MeshGroup extends Geometry {
  private ArrayList<Mesh> meshes;
  private double[] MinMax;

  private Vector3D _static_delta;
  private GeometryIntersectionInformation lastInfo;

  public MeshGroup() {
    meshes = new ArrayList<Mesh> ();
    MinMax = null;
  }

  public MeshGroup(ArrayList<Mesh> meshes) {
    this.meshes = meshes;
  }

  public MeshGroup(MeshGroup group) {
    this.meshes = group.getMeshes();
  }

  public ArrayList<Mesh> getMeshes() {
    return this.meshes;
  }

  public void setMeshes(ArrayList<Mesh> meshes) {
    this.meshes = meshes;
  }

  public void addMesh(Mesh mesh) {
    this.meshes.add(mesh);
  }

  public Mesh getMeshAt(int index) {
    return (Mesh)this.meshes.get(index);
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
   Dado un Ray `inout_rayo`, esta operaci&oacute;n determina si el rayo se
   intersecta con alguna de las mallas de triangulos. Si el rayo no intersecta
   al objeto se retorna 0, y de lo contrario se retorna la distancia desde
   el origen del rayo hasta el punto de interseccion mas cercano de todas las mallas.

   */
  public boolean
      doIntersection(Ray inOut_Ray) {

    boolean intersection = false;

    Vector3D I;
    GeometryIntersectionInformation Info;

    this._static_delta.x = Double.MAX_VALUE;
    this._static_delta.y = Double.MAX_VALUE;
    this._static_delta.z = Double.MAX_VALUE;

    Ray ray = new Ray(inOut_Ray);

    for (Iterator<Mesh> i=  meshes.iterator(); i.hasNext() ;) {
      Mesh mesh = i.next();
      if (mesh.doIntersection(ray) == true) {
        I = new Vector3D();
        Info = new GeometryIntersectionInformation();
        mesh.doExtraInformation(ray, 0.0, Info);
        I.x = Info.p.x;
        I.y = Info.p.y;
        I.z = Info.p.z;

        if (I.substract(ray.origin).length() <
            _static_delta.substract(ray.origin).length()) {
          this._static_delta.x = I.x;
          this._static_delta.y = I.y;
          this._static_delta.z = I.z;
          lastInfo.p = I;
          lastInfo.n = Info.n;

          inOut_Ray.t = ray.t;
          intersection = true;
        }
      }
    }
    return intersection;
  }

  public void doExtraInformation(Ray inRay, double inT,
                                 GeometryIntersectionInformation outData) {
    outData.p = lastInfo.p;
    outData.n = lastInfo.n;
  }

  public String toString() {
    return "MeshGroup < #Mesh: " + this.meshes.size() + " >";
  }

}
