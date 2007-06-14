//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - David Diaz: Original base version                     =
//= - May 18 2006 - David Diaz: bug fixes                                   =
//= - May 22 2006 - David Diaz/Oscar Chavarro: documentation added          =
//= - November 13 2006 - Oscar Chavarro: re-structured and tested           =
//===========================================================================

package vsdk.toolkit.io.geometry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.io.image.ImageNotRecognizedException;
import vsdk.toolkit.io.image.ImagePersistence;

class _ReaderObjVertex
{
    public int vertexPositionIndex;
    public int vertexNormalIndex;
    public int vertexTextureCoordinateIndex;
    public boolean equals(Object alien)
    {
        System.out.println("Comparando...!");
        if ( !(alien instanceof _ReaderObjVertex) ) return false;
        _ReaderObjVertex other = (_ReaderObjVertex)alien;

        if ( other.vertexPositionIndex != this.vertexPositionIndex ||
             other.vertexNormalIndex != this.vertexNormalIndex ||
             other.vertexTextureCoordinateIndex != 
             this.vertexTextureCoordinateIndex ) {
            return false;
    }
    return true;
    }
}

/**
The class ReaderObj provides wavefront obj loading functionality. Wavefront
obj is a 3d object format used to describe polygon meshes; it is capable of
storing vertex, vertex normal, vetrex texture, faces, material, texture and
other maps information.
By the use of extensions, it has the potential to describe more information.
The original Wavefront format is not well standarized, so many variations
could exist. This code currently manages only triangle faces.
*/
public class ReaderObj
{
    /**
    This method reads an Alias/Wavefront .obj file in ASCII form from the 
    given filename. A wavefront obj file can have many objects within, so this 
    method returns a group of objects rather than a single one.

    Even though a wavefront obj file has many objects, all the objects in 
    the file share a common set of vertexes; this loader only stores for a 
    TriangleMesh the vertexes that it uses, not all the array of vertexes.

    For a mesh to have a material, the matrial file has to be in the same
    folder as the mesh file; the same statement can be given about the
    textures.

    @todo should not recieve a filename, but a previously opened stream, to
    make it independent of filesystems, and generalize it to URLs or whatever
    connection.
    */
    public static TriangleMeshGroup read(String fileName) throws IOException
    {
        //- Geometric data and geometric attributes extracted from file ---
        ArrayList<Vector3D> vertexPositionsArray;
        ArrayList<Vector3D> vertexNormalsArray;
        ArrayList<Vector3D> vertexTextureCoordinatesArray;

        vertexPositionsArray = new ArrayList<Vector3D>();
        vertexNormalsArray = new ArrayList<Vector3D>();
        vertexTextureCoordinatesArray = new ArrayList<Vector3D>();

        //- Topology data extracted from file -----------------------------
        ArrayList<int[][]> facesArray;
        ArrayList<_ReaderObjVertex> unifiedVertexesArray;

        facesArray = new ArrayList<int[][]>();
        unifiedVertexesArray = new ArrayList<_ReaderObjVertex>();

        //- Accumulated states for currently builded geometric object -----
        String nextGeometricObjectName;
        ArrayList<RGBAImage> nextTexturesArray;
        ArrayList<Material> nextMaterialsArray;

        nextGeometricObjectName = "OBJ_default_material";
        nextTexturesArray = new ArrayList<RGBAImage>();
        nextMaterialsArray = new ArrayList<Material>();        

        //- Aditional support data structures -----------------------------
        ArrayList<TriangleMesh> meshGroup;
        ArrayList<ArrayList<int[]>> texture_span_triangleRange_table;
        ArrayList<int[]> inicial;
        ArrayList<int[]> material_triangleRange_table;
        HashMap<String, RGBAImage> texturesHashMap;
        HashMap<String, Material> materialsHashMap;
        int textureIndex;

        meshGroup = new ArrayList<TriangleMesh>();
        texturesHashMap = new HashMap<String, RGBAImage>();
        materialsHashMap = new HashMap<String, Material>();        
        textureIndex = 0;

        texture_span_triangleRange_table = new ArrayList<ArrayList<int[]>>();
        inicial = new ArrayList<int[]>();
        inicial.add(new int[2]);
        texture_span_triangleRange_table.add(inicial);

        material_triangleRange_table = new ArrayList<int[]>();

        //- Geometry object processing from file / control -------------------
        BufferedReader br;
        String lineOfText;

        br = new BufferedReader(new FileReader(fileName));

        while ( (lineOfText = br.readLine()) != null ) {
            // Build material library
            if ( lineOfText.startsWith("mtllib ") ) {
                materialsHashMap = readMaterials(lineOfText, fileName);
            }
            // Change active material
            if ( lineOfText.startsWith("usemtl ") ) {
                //
                String auxMaterialName;
                StringTokenizer auxStringTokenizer;
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");
                auxStringTokenizer.nextToken();
                auxMaterialName = auxStringTokenizer.nextToken();
                nextMaterialsArray.add(materialsHashMap.get(auxMaterialName));
                //
                int auxMaterialRange[];
                auxMaterialRange = new int[2];
                auxMaterialRange[0] = facesArray.size();
                auxMaterialRange[1] = nextMaterialsArray.size()-1;
                material_triangleRange_table.add(auxMaterialRange);
            }
            // Add a vertex
            if ( lineOfText.startsWith("v ") ) {
                vertexPositionsArray.add(readVertex(lineOfText));
            }
            // Add a normal
            if ( lineOfText.startsWith("vn ") ) {
                vertexNormalsArray.add(readVertex(lineOfText));
            }
            // Add a texture coordinate
            if ( lineOfText.startsWith("vt ") ) {
                vertexTextureCoordinatesArray.add(
                    readVertexTexture(lineOfText));
            }
            // Read faces (only triangles)
            if ( lineOfText.startsWith("f ") ) {
                try {
                    // Note that only first 3 vertexes for each polygon are
                    // processed
                    ArrayList<int[][]> auxTriangle;
                    auxTriangle = readPolygonAsTriangleFan(lineOfText);
                    int auxVertexData[][];

                    System.out.println("Reading triangle: " + auxTriangle.size());

                    for( int i = 0; i < auxTriangle.size(); i++ ) {
                        auxVertexData = auxTriangle.get(i);
                        facesArray.add(auxVertexData);

                        // 
                        _ReaderObjVertex uv = new _ReaderObjVertex();
                        uv.vertexPositionIndex = 0;
                        uv.vertexNormalIndex = 1;
                        uv.vertexTextureCoordinateIndex = 2;
                        unifiedVertexesArray.add(uv);

                    }

                    //
                    ArrayList<int[]> actRanges;
                    actRanges =
                        texture_span_triangleRange_table.get(textureIndex);
                    int[] lastRange = actRanges.get(actRanges.size()-1);
                    lastRange[1] = facesArray.size();
                }
                catch( NoSuchElementException nsee ) {
                }
            }
            // File specified textures management
            if ( lineOfText.startsWith("usemap ") ) {
                // Put texture in hash map or select it from hash map
                String auxTextureName;
                StringTokenizer auxStringTokenizer;
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");
                auxStringTokenizer.nextToken();
                auxTextureName = auxStringTokenizer.nextToken();
                if ( !texturesHashMap.containsKey(auxTextureName) ) {
                    RGBAImage auxTexture;
                    auxTexture = obtainTextureFromFile(lineOfText, fileName);
                    if ( auxTexture == null ) {
                        textureIndex = 0;
                    }
                    else {
                        nextTexturesArray.add(auxTexture);
                        texture_span_triangleRange_table.add(
                            new ArrayList<int[]>());
                        textureIndex = nextTexturesArray.size();
                    }
                    texturesHashMap.put(auxTextureName, auxTexture);
                }
                else {
                    RGBAImage auxTexture = texturesHashMap.get(auxTextureName);
                    if( auxTexture == null ) {
                        textureIndex = 0;
                    }
                    else if( !nextTexturesArray.contains(auxTexture) ) {
                        nextTexturesArray.add(auxTexture);
                        texture_span_triangleRange_table.add(
                            new ArrayList<int[]>());
                        textureIndex = nextTexturesArray.size();
                    }
                    else {
                        textureIndex=nextTexturesArray.indexOf(auxTexture)+1;
                    }
                }
                // Add selected texture to current object texture definition
                ArrayList<int[]> actRanges;
                actRanges = texture_span_triangleRange_table.get(textureIndex);
                int[] newRange=new int[2];
                newRange[0] = facesArray.size();
                actRanges.add(newRange);
            }
            // Armar objeto
            if ( lineOfText.startsWith("o ") || lineOfText.startsWith("g ") ) {
                if ( vertexPositionsArray.size() > 0 ) {
                    addMeshToGroup(meshGroup,
                                   nextGeometricObjectName,
                                   vertexPositionsArray,
                                   vertexNormalsArray,
                                   vertexTextureCoordinatesArray,
                                   facesArray,
                                   nextTexturesArray,
                                   texture_span_triangleRange_table,
                                   nextMaterialsArray,
                                   material_triangleRange_table);
                }

                // Process next object name
                StringTokenizer auxStringTokenizer;
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");
                auxStringTokenizer.nextToken();
                nextGeometricObjectName = auxStringTokenizer.nextToken();

                // Clear accumulated states variables
                nextTexturesArray = new ArrayList<RGBAImage>();
                nextMaterialsArray = new ArrayList<Material>();
                facesArray = new ArrayList<int[][]>();
                material_triangleRange_table = new ArrayList<int[]>();
                texture_span_triangleRange_table =
                    new ArrayList<ArrayList<int[]>>();
                inicial = new ArrayList<int[]>();
                int[] auxInitialRange = new int[2];
                auxInitialRange[0] = auxInitialRange[1] = 0;
                inicial.add(auxInitialRange);
                texture_span_triangleRange_table.add(inicial);
                textureIndex = 0;
            }
        }

        // Build the last mesh from remaining vertexes, if any
        if ( vertexPositionsArray.size() > 0 ) {
            addMeshToGroup(meshGroup,
                           nextGeometricObjectName,
                           vertexPositionsArray,
                           vertexNormalsArray,
                           vertexTextureCoordinatesArray,
                           facesArray,
                           nextTexturesArray, texture_span_triangleRange_table,
                           nextMaterialsArray, material_triangleRange_table);
        }

        //-----------------------------------------------------------------
        TriangleMeshGroup finalTriangleMeshGroup = new TriangleMeshGroup();
        for( TriangleMesh auxTriangleMesh:meshGroup ) {
            Vertex[] auxTriangleMeshVertexArray;
            auxTriangleMeshVertexArray = auxTriangleMesh.getVertexes();
            if ( auxTriangleMeshVertexArray.length > 0 ) {
                finalTriangleMeshGroup.addMesh(auxTriangleMesh);
            }
        }
        return finalTriangleMeshGroup;
    }

    private static void
    addMeshToGroup(
        ArrayList<TriangleMesh> meshGroup,
        String nextGeometricObjectName,
        ArrayList<Vector3D> vertexPositionsArray,
        ArrayList<Vector3D> vertexNormalsArray,
        ArrayList<Vector3D> vertexTextureCoordinatesArray,
        ArrayList<int[][]> facesArray,
        ArrayList<RGBAImage> nextTexturesArray,
        ArrayList<ArrayList<int[]>> texture_span_triangleRange_table,
        ArrayList<Material> nextMaterialsArray,
        ArrayList<int[]> material_triangleRange_table
    )
    {
        TriangleMesh newTriangleMesh;
        if ( nextMaterialsArray.size() == 0 ) {
            nextMaterialsArray.add(new Material());
        }
        newTriangleMesh = buildGeometry(
                               vertexPositionsArray,
                               vertexNormalsArray,
                               vertexTextureCoordinatesArray,
                               facesArray,
                               nextTexturesArray,
                               texture_span_triangleRange_table,
                               nextMaterialsArray,
                               material_triangleRange_table);
        newTriangleMesh.setName(nextGeometricObjectName);
        meshGroup.add(newTriangleMesh);
    }
    
    private static void quickSortTriangleRange(int a[][], int izq, int der)
    {
        int i = izq;
        int j = der;
        int pivote = a[(izq+der)/2][0];
        int aux0;
        int aux1;
        do {
            while ( a[i][0] < pivote ) i++;
            while ( a[j][0] > pivote ) j--;
            if ( i <= j ) {
                aux0 = a[i][0];
                a[i][0] = a[j][0];
                a[j][0] = aux0;
                aux1 = a[i][1];
                a[i][1] = a[j][1];
                a[j][1] = aux1;
                i++;
                j--;
            }
        } while ( i <= j );
        if ( izq < j ) quickSortTriangleRange(a, izq, j);
        if ( i < der ) quickSortTriangleRange(a, i, der);
    }

    private static RGBAImage
    obtainTextureFromFile(String lineOfText, String fileName)
    {
        StringTokenizer st = new StringTokenizer(lineOfText, " ");
        st.nextToken(); //usemap
        String dirObj = new File(fileName).getParentFile().getAbsolutePath();
        String nomImage = st.nextToken();
        if ( nomImage.equals("(null)") ) {
            return null;
        }
        try
        {
            return ImagePersistence.importRGBA(
              new File(dirObj+System.getProperty("file.separator")+nomImage));
        }
        catch(ImageNotRecognizedException inre) {
            return null;
        }
    }

    private static TriangleMesh buildGeometry(
        ArrayList<Vector3D> vertexPositionsArray, 
        ArrayList<Vector3D> vertexNormalsArray,
        ArrayList<Vector3D> vertexTextureCoordinatesArray,
        ArrayList<int[][]> facesArray,
        ArrayList<RGBAImage> textures,
        ArrayList<ArrayList<int[]>> texture_span_triangleRange_table,
        ArrayList<Material> nextMaterialsArray,
        ArrayList<int[]> material_triangleRange_table)
    {
        TriangleMesh m = new TriangleMesh();

        m.setTriangles(new Triangle[facesArray.size()]);

        ArrayList<Integer> usedVertexes = new ArrayList<Integer>();
        ArrayList<Integer> usedTexVertexes = new ArrayList<Integer>();
        
        HashMap<Integer, Integer> cambiosVert;
        cambiosVert = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> cambiosVertTex;
        cambiosVertTex = new HashMap<Integer, Integer>();
        int indexVert = 0;
        int indexVertTex = 0;
        int i;

        for( i = 0; i < m.getTriangles().length; i++ ) {
            int[] p1 = facesArray.get(i)[0];
            int[] p2 = facesArray.get(i)[1];
            int[] p3 = facesArray.get(i)[2];

            if ( !cambiosVert.containsKey(p1[0]) ) {
                cambiosVert.put(p1[0], indexVert);
                indexVert++;
                usedVertexes.add(p1[0]);
            }
            p1[0] = cambiosVert.get(p1[0]);
            
            if ( !cambiosVertTex.containsKey(p1[1]) && p1[1] != -1 ) {
                cambiosVertTex.put(p1[1], indexVertTex);
                indexVertTex++;
                usedTexVertexes.add(p1[1]);
            }
            if ( p1[1]!=-1 ) {
                p1[1]=cambiosVertTex.get(p1[1]);
            }
            
            if ( !cambiosVert.containsKey(p2[0]) ) {
                cambiosVert.put(p2[0], indexVert);
                indexVert++;
                usedVertexes.add(p2[0]);
            }
            p2[0] = cambiosVert.get(p2[0]);
            
            if ( !cambiosVertTex.containsKey(p2[1]) && p2[1] != -1 ) {
                cambiosVertTex.put(p2[1], indexVertTex);
                indexVertTex++;
                usedTexVertexes.add(p2[1]);
            }
            if ( p2[1] != -1 ) {
                p2[1]=cambiosVertTex.get(p2[1]);
            }
            
            if ( !cambiosVert.containsKey(p3[0]) ) {
                cambiosVert.put(p3[0], indexVert);
                indexVert++;
                usedVertexes.add(p3[0]);
            }
            p3[0] = cambiosVert.get(p3[0]);
            
            if ( !cambiosVertTex.containsKey(p3[1]) && p3[1]!=-1 ) {
                cambiosVertTex.put(p3[1], indexVertTex);
                indexVertTex++;
                usedTexVertexes.add(p3[1]);
            }
            if ( p3[1] != -1 ) {
                p3[1] = cambiosVertTex.get(p3[1]);
            }
        }
 
        m.setVertexes(new Vertex[usedVertexes.size()]);
        for ( i = 0; i < usedVertexes.size(); i++ ) {
            m.getVertexes()[i] = new Vertex();
            m.getVertexes()[i].setPosition(
                vertexPositionsArray.get(usedVertexes.get(i)-1));
        }
        
        m.setVerTexture(new Vector3D[usedTexVertexes.size()]);
        for ( i = 0; i < m.getVerTexture().length; i++ ) {
            m.getVerTexture()[i]=vertexTextureCoordinatesArray.get(usedTexVertexes.get(i)-1);
        }
        
        for ( i = 0; i < m.getTriangles().length; i++ ) {
            m.getTriangles()[i]=new Triangle();
            m.getTriangles()[i].setPoint0(facesArray.get(i)[0][0]);
            m.getTriangles()[i].setPoint1(facesArray.get(i)[1][0]);
            m.getTriangles()[i].setPoint2(facesArray.get(i)[2][0]);

            m.getTriangles()[i].setVt0(facesArray.get(i)[0][1]);
            m.getTriangles()[i].setVt1(facesArray.get(i)[1][1]);
            m.getTriangles()[i].setVt2(facesArray.get(i)[2][1]);
        }
        
        //- Process textures ----------------------------------------------
        Material materials[];
        materials = new Material[nextMaterialsArray.size()];

        for ( i = 0; i < materials.length; i++ ) {
            materials[i] = nextMaterialsArray.get(i);
            if ( materials[i] == null ) {
                materials[i] = new Material();
            }
        }
        m.setMaterials(materials);

        //-----------------------------------------------------------------
        int auxMaterialRange[];
        auxMaterialRange = new int[2];
        auxMaterialRange[0] = facesArray.size();
        auxMaterialRange[1] = nextMaterialsArray.size()-1;
        material_triangleRange_table.add(auxMaterialRange);

        int materialRanges[][];

        materialRanges = new int[material_triangleRange_table.size()][2];
        for ( i = 1; i < material_triangleRange_table.size(); i++ ) {
            materialRanges[i][0] = material_triangleRange_table.get(i)[0];
            materialRanges[i][1] = material_triangleRange_table.get(i-1)[1];
        }
        m.setMaterialRanges(materialRanges);

        //- Process textures ----------------------------------------------
        m.setTextures(new RGBAImage[textures.size()]);
        for ( i = 0; i < m.getTextures().length; i++ ) {
            m.getTextures()[i]=textures.get(i);
        }

        //- Process texture ranges ----------------------------------------
        int numTextureSpans = 0;
        for ( int textureIndex = 0;
              textureIndex < texture_span_triangleRange_table.size();
              textureIndex++ ) {
            for( int j = 0;
                 j < texture_span_triangleRange_table.get(textureIndex).size();
                 j++ ) {
                numTextureSpans++;
            }
        }        

        int textureRanges[][] = new int[numTextureSpans][2];
        i = 0;
        for ( int textureIndex = 0;
              textureIndex < texture_span_triangleRange_table.size();
              textureIndex++ ) {
            for( int j = 0;
                 j < texture_span_triangleRange_table.get(textureIndex).size();
                 j++ ) {
                textureRanges[i][0] =
                  texture_span_triangleRange_table.get(textureIndex).get(j)[1];
                textureRanges[i][1] = textureIndex;
                i++;
            }
        }
        quickSortTriangleRange(textureRanges, 0, textureRanges.length-1);
        m.setTextureRanges(textureRanges);

        //- Process normals -----------------------------------------------
        m.calculateNormals();
        
        return m;
    }
    
    private static int[] copyLength3IntArray(int[] a)
    {
        int[] ret = new int[3];
        ret[0] = a[0];
        ret[1] = a[1];
        ret[2] = a[2];
        return ret;
    }
    
    /**
    This method reads a polygon from a face line. It returns a set of triangles
    as an ArrayList of matrices. For each matrix, there is the information of
    a single triangle, where there are 3 rows (one for each triangle vertex)
    and each row has three elements: vertex position index, texture coordinates
    and vertex normal.
    Note that the triangle set is builded as a triangle fan: the first vertex
    (p0) is a pivot which is fixed for all triangles, the second point 
    determines the first triangle edge, and for each following vertex,
    a new triangle is builded.
    */
    private static ArrayList<int[][]>
    readPolygonAsTriangleFan(String lineOfText) {
        ArrayList<int[][]> ret = new ArrayList<int[][]>();
        StringTokenizer st = new StringTokenizer(lineOfText, " \n\r\t");
        st.nextToken(); // The "f" token
        int numberOfTokens = st.countTokens();
        int[] p0 = null;
        int[] p1 = null;
        int[] p2 = null;
        int[][]aux = null;
        int i;

        for( i = 0; i < numberOfTokens; i++ ) {
            String token = st.nextToken();
            int[] indexes = readFaceVertex(token);

            if( i == 0 ) {
                p0 = indexes;
            }
            else if( i == 1 ) {
                p1 = indexes;
            }
            else {
                p2 = indexes;

                aux = new int[3][];
                aux[0] = copyLength3IntArray(p0);
                aux[1] = copyLength3IntArray(p1);
                aux[2] = copyLength3IntArray(p2);
                ret.add(aux);

                p1 = copyLength3IntArray(p2); // Why?
        }
        }
        return ret;
    }

    /**
    Returns three indices: vertex, texture coorinates, normal
    */    
    private static int[] readFaceVertex(String lineOfText)
    {
        int[] ret = new int[3];
        StringTokenizer st=new StringTokenizer(lineOfText, "/");
        if ( st.countTokens() == 2 ) {
            if( lineOfText.endsWith("/") ) {
                // Has vertex and texture
                try {
                    ret[0] = Integer.parseInt(st.nextToken());
                }
                catch ( NumberFormatException nfe ) {
                    ret[0] = -1;
                }
                try {
                    ret[1] = Integer.parseInt(st.nextToken());
                }
                catch ( NumberFormatException nfe ) {
                    ret[1] = -1;
                }
                ret[2] = -1;
              }
              else {
                // Has vertex and normal
                try {
                    ret[0] = Integer.parseInt(st.nextToken());
                }
                catch ( NumberFormatException nfe ) {
                    ret[0] = -1;
                }
                ret[1]=-1;
                try {
                    ret[2] = Integer.parseInt(st.nextToken());
                }
                catch ( NumberFormatException nfe ) {
                    ret[2] = -1;
                }
            }
          }
          else {
            // Has all
            try {
                ret[0] = Integer.parseInt(st.nextToken());
            }
            catch ( NumberFormatException nfe ) {
                ret[0] = -1;
            }
            try {
                ret[1] = Integer.parseInt(st.nextToken());
            }
            catch ( NumberFormatException nfe ) {
                ret[1] = -1;
            }
            try {
                ret[2] = Integer.parseInt(st.nextToken());
            }
            catch ( NumberFormatException nfe ) {
                ret[2] = -1;
            }
          }
        ;
        return ret;
    }
    
    private static Vector3D readVertex(String lineOfText)
    {
        Vector3D vert = new Vector3D();
        StringTokenizer st = new StringTokenizer(lineOfText);
        st.nextToken();
        vert.x = Double.parseDouble(st.nextToken());
        vert.y = Double.parseDouble(st.nextToken());
        vert.z = Double.parseDouble(st.nextToken());

        return vert;
    }

    private static Vector3D readVertexTexture(String lineOfText) {
        Vector3D vert = new Vector3D();
        StringTokenizer st = new StringTokenizer(lineOfText);
        st.nextToken();
        vert.x = Double.parseDouble(st.nextToken());
        vert.y = Double.parseDouble(st.nextToken());
        try {
            vert.z = Double.parseDouble(st.nextToken());
        }
        catch( Exception e ) {}
        return vert;
    }

    private static HashMap<String, Material>
    readMaterials(String material, String fileName) {
        HashMap<String, Material> ret = new HashMap<String, Material>();
        StringTokenizer st = new StringTokenizer(material, " ");
        st.nextToken(); // "mtlib" token
        File arc=new File(fileName);
        File dirArc=arc.getParentFile();
        String nomArc;
        nomArc = dirArc + System.getProperty("file.separator")+st.nextToken();
        
        try {
            BufferedReader in=new BufferedReader(new FileReader(nomArc));
            String lineOfText="";
            
            Material activeMaterial=new Material();
            activeMaterial.setName("default");
            
            while( (lineOfText = in.readLine()) != null ) {
                if ( lineOfText.startsWith("Ns") ) {
                    StringTokenizer stMat=new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Ns
                    activeMaterial.setPhongExponent(
                        Float.parseFloat(stMat.nextToken()));
                }
                if ( lineOfText.startsWith("Kd") ) {
                    StringTokenizer stMat=new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Kd
                    ColorRgb color=new ColorRgb();
                    color.r=Float.parseFloat(stMat.nextToken());
                    color.g=Float.parseFloat(stMat.nextToken());
                    color.b=Float.parseFloat(stMat.nextToken());
                    activeMaterial.setDiffuse(color);
                }
                if ( lineOfText.startsWith("Ka") ) {
                    StringTokenizer stMat=new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Ka
                    ColorRgb color=new ColorRgb();
                    color.r=Float.parseFloat(stMat.nextToken());
                    color.g=Float.parseFloat(stMat.nextToken());
                    color.b=Float.parseFloat(stMat.nextToken());
                    activeMaterial.setAmbient(color);
                }
                if ( lineOfText.startsWith("Ks") ) {
                    StringTokenizer stMat=new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // Ks
                    ColorRgb color=new ColorRgb();
                    color.r=Float.parseFloat(stMat.nextToken());
                    color.g=Float.parseFloat(stMat.nextToken());
                    color.b=Float.parseFloat(stMat.nextToken());
                    activeMaterial.setSpecular(color);
                }
                if ( lineOfText.startsWith("d") ) {
                    StringTokenizer stMat=new StringTokenizer(lineOfText, " ");
                    stMat.nextToken(); // d
                    //activeMaterial.setAlpha(Float.parseFloat(stMat.nextToken()));
                }
                if ( lineOfText.startsWith("newmtl") ) {
                    StringTokenizer stMat=new StringTokenizer(lineOfText, " ");
                    stMat.nextToken();//newmtl
                    ret.put(activeMaterial.getName(), activeMaterial);
                    activeMaterial = new Material();
                    activeMaterial.setName(stMat.nextToken());
                }
            }
            ret.put(activeMaterial.getName(), activeMaterial);
        }
        catch( IOException ioe ) {
        }
        return ret;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
