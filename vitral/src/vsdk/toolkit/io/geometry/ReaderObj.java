//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - David Diaz: Original base version                     =
//= - May 18 2006 - David Diaz: bug fixes                                   =
//= - May 22 2006 - David Diaz/Oscar Chavarro: documentation added          =
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
        ArrayList<TriangleMesh> meshGroup=new ArrayList<TriangleMesh>();
        ArrayList<Vector3D> vertexes=new ArrayList<Vector3D>();
        ArrayList<Vector3D> normals=new ArrayList<Vector3D>();
        ArrayList<Vector3D> vertexesTex=new ArrayList<Vector3D>();
        ArrayList<int[][]> faces=new ArrayList<int[][]>();

        HashMap<String, RGBAImage> texturasHash;
        texturasHash=new HashMap<String, RGBAImage>();
        ArrayList<RGBAImage> texturesList=new ArrayList<RGBAImage>();
        ArrayList<Material> materialsList=new ArrayList<Material>();
        
        ArrayList<ArrayList<int[]>> relCarTex;
        relCarTex = new ArrayList<ArrayList<int[]>>();
        ArrayList<int[]> inicial=new ArrayList<int[]>();
        inicial.add(new int[2]);
        relCarTex.add(inicial);
        
        HashMap<String, Material> materialsHashMap;
        materialsHashMap = new HashMap<String, Material>();
        String nomObj = "default";
        
        int textAct = 0;

        BufferedReader m;
        int cont = 0;
        String lineOfText;
        m = new BufferedReader(new FileReader(fileName));

        Vector3D buffer;
        ArrayList<int[][]> bufferT;
        int num = 0;

        //- Crear Mallas -----------------------------------------------------
        while ( (lineOfText = m.readLine()) != null ) {
            // Build material library
            if ( lineOfText.startsWith("mtllib ") ) {
                materialsHashMap = readMaterials(lineOfText, fileName);
            }
            // Change active material
            if ( lineOfText.startsWith("usemtl ") ) {
                materialsList.add(materialsHashMap.get(lineOfText));
            }
            // Add a vertex
            if ( lineOfText.startsWith("v ") ) {
                buffer = readVertex(lineOfText);
                vertexes.add(buffer);
            }
            // Add a normal
            if ( lineOfText.startsWith("vn ") ) {
                buffer = readVertex(lineOfText);
                normals.add(buffer);
            }
            // Add a texture coordinate
            if ( lineOfText.startsWith("vt ") ) {
                buffer = readVertexTexture(lineOfText);
                vertexesTex.add(buffer);
            }
            // Read faces (only triangles)
            if ( lineOfText.startsWith("f ") ) {
                try {
                    // Note that only first 3 vertexes for each polygon are
                    // processed
                    bufferT = readPolygon(lineOfText);
                    for( int[][] elem : bufferT ) {
                        faces.add(elem);
                    }
                    ArrayList<int[]> actRanges = relCarTex.get(textAct);
                    int[] lastRange = actRanges.get(actRanges.size()-1);
                    lastRange[1] = faces.size();
                }
                catch(NoSuchElementException nsee) {
                }
            }
            // Inicializar textura
            if ( lineOfText.startsWith("usemap ") ) {
                if ( !texturasHash.containsKey(lineOfText) ) {
                    RGBAImage texture;
                    texture = obtainTextureFromFile(lineOfText, fileName);
                    if ( texture == null ) {
                        textAct = 0;
                    }
                    else {
                        texturesList.add(texture);
                        relCarTex.add(new ArrayList<int[]>());
                        textAct = texturesList.size();
                    }
                    texturasHash.put(lineOfText, texture);
                }
                else {
                    RGBAImage texture = texturasHash.get(lineOfText);
                    if( texture == null ) {
                        textAct=0;
                    }
                    else if( !texturesList.contains(texture) ) {
                        texturesList.add(texture);
                        relCarTex.add(new ArrayList<int[]>());
                        textAct=texturesList.size();
                    }
                    else {
                        textAct=texturesList.indexOf(texture)+1;
                    }
                }
                ArrayList<int[]> actRanges=relCarTex.get(textAct);
                int[] newRange=new int[2];
                newRange[0]=faces.size();
                actRanges.add(newRange);
            }
            // Armar objeto
            if ( lineOfText.startsWith("o ") || lineOfText.startsWith("g ") ) {
                if ( vertexes.size() > 0 ) {
                    TriangleMesh object;
                    if ( materialsList.size() == 0 ) {
                        materialsList.add(new Material());
            }
                    object = buildGeometry(vertexes, normals,
                                           vertexesTex, faces,
                                           texturesList, relCarTex,
                                           materialsList);
                    object.setName(nomObj);
                    meshGroup.add(object);
                }
                StringTokenizer auxNomObj;
                auxNomObj = new StringTokenizer(lineOfText, " ");
                auxNomObj.nextToken();
                nomObj=auxNomObj.nextToken();
                faces=new ArrayList<int[][]>();
                texturesList=new ArrayList<RGBAImage>();
                materialsList=new ArrayList<Material>();
                relCarTex=new ArrayList<ArrayList<int[]>>();
                inicial=new ArrayList<int[]>();
                int[] rangoInicial=new int[2];
                rangoInicial[0]=rangoInicial[1]=0;
                inicial.add(rangoInicial);
                relCarTex.add(inicial);
                textAct=0;
            }
        }

        // Build the last mesh from remaining vertexes, if any
        if ( vertexes.size() > 0 ) {
            TriangleMesh object;
            if ( materialsList.size() == 0 ) {
                materialsList.add(new Material());
        }
            object = buildGeometry(vertexes, normals, vertexesTex, 
                                   faces, texturesList, relCarTex,
                                   materialsList);
            object.setName(nomObj);
            meshGroup.add(object);
        }

        TriangleMeshGroup mgRet=new TriangleMeshGroup();
        for( TriangleMesh mAux:meshGroup ) {
            Vertex[] test = mAux.getVertexes();
            if ( test.length > 0 ) {
                mgRet.addMesh(mAux);
            }
        }
        return mgRet;
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
        ArrayList<Vector3D> vertexes, 
        ArrayList<Vector3D> normals,
        ArrayList<Vector3D> vertexesTex,
        ArrayList<int[][]> faces,
        ArrayList<RGBAImage> textures,
        ArrayList<ArrayList<int[]>> relCarTex,
        ArrayList<Material> materials)
    {
        TriangleMesh m = new TriangleMesh();

        m.setTriangles(new Triangle[faces.size()]);

        ArrayList<Integer> usedVertexes = new ArrayList<Integer>();
        ArrayList<Integer> usedTexVertexes = new ArrayList<Integer>();
        
        HashMap<Integer, Integer> cambiosVert;
        cambiosVert = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> cambiosVertTex;
        cambiosVertTex = new HashMap<Integer, Integer>();
        int indexVert = 0;
        int indexVertTex = 0;

        for( int i = 0; i < m.getTriangles().length; i++ ) {
            int[] p1 = faces.get(i)[0];
            int[] p2 = faces.get(i)[1];
            int[] p3 = faces.get(i)[2];

            if ( !cambiosVert.containsKey(p1[0]) ) {
                cambiosVert.put(p1[0], indexVert);
                indexVert++;
                usedVertexes.add(p1[0]);
            }
            p1[0] = cambiosVert.get(p1[0]);
            
            if( !cambiosVertTex.containsKey(p1[1]) && p1[1] != -1 ) {
                cambiosVertTex.put(p1[1], indexVertTex);
                indexVertTex++;
                usedTexVertexes.add(p1[1]);
            }
            if( p1[1]!=-1 ) {
                p1[1]=cambiosVertTex.get(p1[1]);
            }
            
            if( !cambiosVert.containsKey(p2[0]) ) {
                cambiosVert.put(p2[0], indexVert);
                indexVert++;
                usedVertexes.add(p2[0]);
            }
            p2[0] = cambiosVert.get(p2[0]);
            
            if( !cambiosVertTex.containsKey(p2[1]) && p2[1] != -1 ) {
                cambiosVertTex.put(p2[1], indexVertTex);
                indexVertTex++;
                usedTexVertexes.add(p2[1]);
            }
            if( p2[1] != -1 ) {
                p2[1]=cambiosVertTex.get(p2[1]);
            }
            
            if( !cambiosVert.containsKey(p3[0]) ) {
                cambiosVert.put(p3[0], indexVert);
                indexVert++;
                usedVertexes.add(p3[0]);
            }
            p3[0] = cambiosVert.get(p3[0]);
            
            if( !cambiosVertTex.containsKey(p3[1]) && p3[1]!=-1 ) {
                cambiosVertTex.put(p3[1], indexVertTex);
                indexVertTex++;
                usedTexVertexes.add(p3[1]);
            }
            if( p3[1] != -1 ) {
                p3[1] = cambiosVertTex.get(p3[1]);
            }
        }
        
        m.setVertexes(new Vertex[usedVertexes.size()]);
        for ( int i = 0; i < usedVertexes.size(); i++ ) {
            m.getVertexes()[i]=new Vertex();
            m.getVertexes()[i].setPosition(new Vector3D());
            m.getVertexes()[i].setPosition(
                vertexes.get(usedVertexes.get(i)-1));
        }
        
        m.setVerTexture(new Vector3D[usedTexVertexes.size()]);
        for ( int i = 0; i < m.getVerTexture().length; i++ ) {
            m.getVerTexture()[i]=vertexesTex.get(usedTexVertexes.get(i)-1);
        }
        
        for ( int i = 0; i < m.getTriangles().length; i++ ) {
            m.getTriangles()[i]=new Triangle();
            m.getTriangles()[i].setPoint0(faces.get(i)[0][0]);
            m.getTriangles()[i].setPoint1(faces.get(i)[1][0]);
            m.getTriangles()[i].setPoint2(faces.get(i)[2][0]);
            
            m.getTriangles()[i].setVt0(faces.get(i)[0][1]);
            m.getTriangles()[i].setVt1(faces.get(i)[1][1]);
            m.getTriangles()[i].setVt2(faces.get(i)[2][1]);
        }
        
        m.setTextures(new RGBAImage[textures.size()]);
        for ( int i = 0; i < m.getTextures().length; i++ ) {
            m.getTextures()[i]=textures.get(i);
        }

        m.setMaterials(new Material[materials.size()]);
        for ( int i = 0; i < m.getMaterials().length; i++ ) {
            m.getMaterials()[i]=materials.get(i);
        }

        m.setTexTriRel(new int[relCarTex.size()][][]);
        for ( int i = 0; i < m.getTextTriRel().length; i++ ) {
            ArrayList<int[]> actRanges = relCarTex.get(i);
            m.getTextTriRel()[i]=new int[actRanges.size()][2];
            for( int j = 0; j < m.getTextTriRel()[i].length; j++ ) {
                m.getTextTriRel()[i][j]=actRanges.get(j);
            }
        }
        
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
    This method reads a polygon from a face line. The returned array of arrays
    has a row element for each vertex polygon. Note that this method is
    compatible with polygon of any number of vertexes.
    */
    private static ArrayList<int[][]> readPolygon(String lineOfText) {
        ArrayList<int[][]> ret = new ArrayList<int[][]>();
        StringTokenizer st = new StringTokenizer(lineOfText, " \n\r\t");
        st.nextToken(); //the f token
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
                continue;
            }
            if( i == 1 ) {
                p1=indexes;
                continue;
            }
            p2 = indexes;

            aux = new int[3][];
            aux[0] = copyLength3IntArray(p0);
            aux[1] = copyLength3IntArray(p1);
            aux[2] = copyLength3IntArray(p2);
            ret.add(aux);

            p1 = new int[3];
            p1[0] = p2[0];
            p1[1] = p2[1];
            p1[2] = p2[2];
        }
        return ret;
    }
    
    private static int[] readFaceVertex(String lineOfText)
    {
        int[] ret=new int[3];
        StringTokenizer st=new StringTokenizer(lineOfText, "/");
        if ( st.countTokens() == 2 ) {
            if( lineOfText.endsWith("/") ) {
                //Has vertex and texture
                try {
                    ret[0]=Integer.parseInt(st.nextToken());
                }
                catch(NumberFormatException nfe) {
                    ret[0]=-1;
                }
                try {
                    ret[1]=Integer.parseInt(st.nextToken());
                }
                catch(NumberFormatException nfe) {
                    ret[1]=-1;
                }
                ret[2]=-1;
              }
              else {
                //Has vertex and normal
                try {
                    ret[0]=Integer.parseInt(st.nextToken());
                }
                catch(NumberFormatException nfe) {
                    ret[0]=-1;
                }
                ret[1]=-1;
                try {
                    ret[2]=Integer.parseInt(st.nextToken());
                }
                catch(NumberFormatException nfe) {
                    ret[2]=-1;
                }
            }
          }
          else {
            // Has all
            try {
                ret[0]=Integer.parseInt(st.nextToken());
            }
            catch(NumberFormatException nfe) {
                ret[0]=-1;
            }
            try {
                ret[1]=Integer.parseInt(st.nextToken());
            }
            catch(NumberFormatException nfe) {
                ret[1]=-1;
            }
            try {
                ret[2]=Integer.parseInt(st.nextToken());
            }
            catch(NumberFormatException nfe) {
                ret[2]=-1;
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
                    ret.put("usemtl "+activeMaterial.getName(),
                            activeMaterial);
                    activeMaterial = new Material();
                    activeMaterial.setName(stMat.nextToken());
                }
            }
            ret.put("usemtl "+activeMaterial.getName(), activeMaterial);
        }
        catch( IOException ioe ) {
        }
        return ret;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
