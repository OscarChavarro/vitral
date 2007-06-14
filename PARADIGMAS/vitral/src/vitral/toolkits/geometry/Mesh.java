package vitral.toolkits.geometry;

import java.util.ArrayList;
import vitral.toolkits.environment.Material;
import vitral.toolkits.common.Vertex;
import vitral.toolkits.common.Triangle;
import vitral.toolkits.common.Vector3D;
import vitral.toolkits.media.RGBAImage;

public class Mesh
{
    private String name="default";
    private Vertex[] vertexes;
    private Triangle[] triangles;
    private double[] MinMax;

    private RGBAImage[] textures;
    private int[][][] texTriRel;

    private Vector3D[] verTex;

    private Material material;

    public Mesh()
    {
    }

    public Mesh(Vertex[] vertexes, Triangle[] triangles)
    {
        this.vertexes = vertexes;
        this.triangles = triangles;
        MinMax = null;
    }

    public String getName()
    {
      return this.name;
    }

    public Vertex[] getVertexes()
    {
      return this.vertexes;
    }

    public Vertex getVertexAt(int index)
    {
      return this.vertexes[index];
    }

    public Triangle[] getTriangles()
    {
      return this.triangles;
    }

    public Triangle getTriangleAt(int index)
    {
      return this.triangles[index];
    }

    public double[] getMinMax()
    {
      return this.MinMax;
    }

    public double getMinMaxAt(int index)
    {
      return this.MinMax[index];
    }

    public RGBAImage[] getTextures()
    {
      return this.textures;
    }

    public RGBAImage getTextureAt(int index)
    {
      return this.textures[index];
    }

    public int[][][] getTextTriRel()
    {
      return  this.texTriRel;
    }

    public int[][] getTexTriRelAt(int index)
    {
      return this.texTriRel[index];
    }

    public int getTextTriRelAt(int i,int j, int k)
    {
      return this.texTriRel[i][j][k];
    }

    public Vector3D[] getVerTexture()
    {
      return this.verTex;
    }

    public Vector3D getVerTextureAt(int index)
    {
      return this.verTex[index];
    }

    public Material getMaterial()
    {
      return this.material;
    }

    public void setName(String name)
    {


      this.name = name;
    }

    public void setVertexes(Vertex[] vertexes)
    {
      this.vertexes = vertexes;
    }

    public void setTriangles(Triangle[] triangles)
    {
      this.triangles = triangles;
    }

    public void setTextures(RGBAImage[] textures)
    {
      this.textures = textures;
    }

    public void setVerTexure(Vector3D[] verTex)
    {

      this.verTex = verTex;
    }

    public void setMaterial(Material material)
    {
      this.material = material;
    }

    public void setTrianglesSize(int size)
    {
      this.triangles = new Triangle[size];
    }

    public void setVertexesSize(int size)
    {
      this.vertexes = new Vertex[size];
    }

    public void setTexturesSize(int size)
    {
      this.textures = new RGBAImage[size];
    }

    public void setVerTexureSize(int size)
    {
      this.verTex = new Vector3D[size];
    }

    public void setTexTriRelSize(int size)
    {
      this.texTriRel = new int[size][][];
    }

    public void setTexTriRelSizeAt(int index,int size)
    {
      this.texTriRel[index] = new int[size][2];
    }

    public void setTextTriRelAt(int i,int j, int[] ttr)
    {
      this.texTriRel[i][j] = ttr;
    }

    public void setVertexAt(int index,Vertex vertex)
    {
      this.vertexes[index] = vertex;
    }

    public void setVerTextureAt(int index,Vector3D verTex)
    {
      this.verTex[index] = verTex;
    }

    public void setTriangleAt(int index,Triangle triangle)
    {
      this.triangles[index] = triangle;
    }

    public void setTextureAt(int index,RGBAImage rgbaImage)
    {
      this.textures[index] = rgbaImage;
    }


    public void calculateNormals()
    {
        for (int i = 0; i < this.triangles.length; i++)
        {
            Vertex v1=vertexes[triangles[i].getPoint0()];
            Vertex v2=vertexes[triangles[i].getPoint1()];
            Vertex v3=vertexes[triangles[i].getPoint2()];

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

    // TODO: Fix this data structure
        ArrayList<vitral.toolkits.common.Triangle> [] vecinos;
        vecinos = new ArrayList[vertexes.length];

        for (int i = 0; i < vertexes.length; i++)
        {
            vecinos[i]=new ArrayList<Triangle>();
        }

        for (int i = 0; i < this.triangles.length; i++)
        {
            vecinos[triangles[i].getPoint0()].add(triangles[i]);
            vecinos[triangles[i].getPoint1()].add(triangles[i]);
            vecinos[triangles[i].getPoint2()].add(triangles[i]);
        }

        for (int i = 0; i < vertexes.length; i++)
        {
            vertexes[i].setNormal(new Vector3D(0,0,0));
            for(int j=0; j<vecinos[i].size(); j++)
            {
                vertexes[i].getNormal().x+=vecinos[i].get(j).normal.x;
                vertexes[i].getNormal().y+=vecinos[i].get(j).normal.y;
                vertexes[i].getNormal().z+=vecinos[i].get(j).normal.z;
            }
            vertexes[i].getNormal().normalize();
            vertexes[i].setIncidentTriangles(vecinos[i]);
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
    public void calculateMinMaxPositions()
    {
        if (MinMax == null)
        {
            MinMax = new double[6];

            boolean first=true;

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

            for ( int i=0; i<vertexes.length; i++)
            {
                double x = vertexes[i].getPosition().x;
                double y = vertexes[i].getPosition().y;
                double z = vertexes[i].getPosition().z;

                if(first)
                {
                    minX = x; maxX=x; minY=y; maxY=y; minZ=z; maxZ=z;
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
                if (x > maxX)
                {
                    maxX = x;
                }
                if (y > maxY)
                {
                    maxY = y;
                }
                if (z > maxZ)
                {
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

    public String toString()
    {
        return ("Mesh < #T:" + triangles.length + " - #V" +
                vertexes.length +" - #N:" +" >");
    }

    public void printMesh()
    {
        System.out.println(this.toString());
        for (int i = 0; i < vertexes.length; i++)
        {
            System.out.println(vertexes[i]);
        }
        for (int i = 0; i < triangles.length; i++)
        {
            System.out.println(triangles[i]);
        }
    }
}
