package vitral.toolkits.geometry;

import java.util.Vector;
import net.java.games.jogl.GL;
import java.util.Enumeration;
import vitral.toolkits.common.QualitySelection;
import vitral.toolkits.visual.jogl.JoglMeshRenderer;

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
public class MeshGroup {
  private Vector<Mesh> meshes;
  private double[] MinMax;

  public MeshGroup() {
    meshes = new Vector<Mesh> ();
    MinMax = null;
  }

  public MeshGroup(Vector<Mesh> meshes) {
    this.meshes = meshes;
  }

  public MeshGroup(MeshGroup group) {
    this.meshes = group.getMeshes();
  }

  public Vector<Mesh> getMeshes() {
    return this.meshes;
  }

  public void setMeshes(Vector<Mesh> meshes) {
    this.meshes = meshes;
  }

  public void addMesh(Mesh mesh) {
    this.meshes.add(mesh);
  }

  public Mesh getMeshAt(int index) {
    return (Mesh)this.meshes.elementAt(index);
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
    public void calculateMinMaxPositions()
    {
        if (MinMax == null)
        {
            MinMax = new double[6];

            boolean first=true;

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

            for ( int i=0; i<meshes.size(); i++)
            {
                double [] minmax_mesh = meshes.elementAt(i).getMinMax();
                double x = minmax_mesh[0];
                double y = minmax_mesh[1];
                double z = minmax_mesh[2];
                double X = minmax_mesh[3];
                double Y = minmax_mesh[4];
                double Z = minmax_mesh[5];
        
                if(first)
                {
                    minX = x; maxX=X; minY=y; maxY=Y; minZ=z; maxZ=Z;
                    first=false;
                }

                if (x < minX)
                {
                    minX = x;
                }
                if (y < minY)
                {
                    minY = y;
                }
                if (z < minZ)
                {
                    minZ = z;
                }
                if (X > maxX)
                {
                    maxX = X;
                }
                if (Y > maxY)
                {
                    maxY = Y;
                }
                if (Z > maxZ)
                {
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

  public String toString()
  {
    return "MeshGroup < #Mesh: " + this.meshes.size() + " >";
  }



}
