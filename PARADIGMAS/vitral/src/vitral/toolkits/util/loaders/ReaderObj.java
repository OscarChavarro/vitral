package vitral.toolkits.util.loaders;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import vitral.toolkits.geometry.Mesh;
import vitral.toolkits.environment.Material;
import vitral.toolkits.common.ColorRgb;
import vitral.toolkits.common.Vertex;
import vitral.toolkits.common.Triangle;
import vitral.toolkits.common.Vector3D;
import vitral.toolkits.media.ImageNotRecognizedException;
import vitral.toolkits.media.RGBAImage;
import vitral.toolkits.media.RGBAImageBuilder;
import vitral.toolkits.geometry.MeshGroup;

public class ReaderObj {
  public static MeshGroup read(String fileName) throws IOException {
   MeshGroup meshGroup = new MeshGroup();

    ArrayList<Vector3D> vertexes = new ArrayList<Vector3D> ();
    ArrayList<Vector3D> normals = new ArrayList<Vector3D> ();
    ArrayList<Vector3D> vertexesTex = new ArrayList<Vector3D> ();
    ArrayList<int[][]> faces = new ArrayList<int[][]> ();

    HashMap<String, RGBAImage> texturasHash = new HashMap<String, RGBAImage> ();
    ArrayList<RGBAImage> texturasList = new ArrayList<RGBAImage> ();

    ArrayList<ArrayList<int[]>> relCarTex = new ArrayList<ArrayList<int[]>> ();
    ArrayList<int[]> inicial = new ArrayList<int[]> ();
    inicial.add(new int[2]);
    relCarTex.add(inicial);

    HashMap<String, Material> materiales = new HashMap<String, Material> ();
    Material matAct = new Material();

    String nomObj = "default";

    int textAct = 0;

    BufferedReader m;
    int cont = 0;
    String cad;
    m = new BufferedReader(new FileReader(fileName));

    Vector3D buffer;
    int[][] bufferT;
    int num = 0;

    /****************Crear Mallas******************/
    while ( (cad = m.readLine()) != null) {
      /****************Agregar Materiales*************/
      if (cad.startsWith("mtllib ")) {
        materiales = readMaterials(cad, fileName);
      }
      /****************Usar Materiale*************/
      if (cad.startsWith("usemtl ")) {
        matAct = materiales.get(cad);
        if (matAct == null) {
          System.out.println(cad + " null");
        }
      }
      /****************Agregar Vertices*************/
      if (cad.startsWith("v ")) {
        buffer = readVertex(cad);
        vertexes.add(buffer);
      }
      /***************Agregar Vector de Normales**************/
      if (cad.startsWith("vn ")) {
        buffer = readVertex(cad);
        normals.add(buffer);
      }
      /***************Agregar Vector de Texturas**************/
      if (cad.startsWith("vt ")) {
        buffer = readVertexTexture(cad);
        vertexesTex.add(buffer);
      }
      /******************Leer triangulos*********************/
      if (cad.startsWith("f ")) {
        bufferT = readTriangle(cad);
        faces.add(bufferT);

        ArrayList<int[]> actRanges = relCarTex.get(textAct);
        int[] lastRange = actRanges.get(actRanges.size() - 1);
        lastRange[1] = faces.size();
      }
      /******************inicializar textura*********************/
      if (cad.startsWith("usemap ")) {
        if (!texturasHash.containsKey(cad)) {
          //System.out.println("doesn't contains key: "+cad);
          RGBAImage texture = obtenerTextura(cad, fileName);
          if (texture == null) {
            //System.out.println("tex null");
            textAct = 0;
          }
          else {
            //System.out.println("tex ok");
            texturasList.add(texture);
            relCarTex.add(new ArrayList<int[]> ());
            textAct = texturasList.size();
          }
          texturasHash.put(cad, texture);
        }
        else {
          //System.out.println("contains key: "+cad);
          RGBAImage texture = texturasHash.get(cad);
          if (texture == null) {
            //System.out.println("tex null");
            textAct = 0;
          }
          else if (!texturasList.contains(texture)) {
            //System.out.println("tex ok, list does no contains it");
            texturasList.add(texture);
            relCarTex.add(new ArrayList<int[]> ());
            textAct = texturasList.size();
          }
          else {
            //System.out.println("tex ok, list contains it");
            textAct = texturasList.indexOf(texture) + 1;
          }
        }
        //System.out.println("texAct: "+textAct);
        ArrayList<int[]> actRanges = relCarTex.get(textAct);
        int[] newRange = new int[2];
        newRange[0] = faces.size();
        actRanges.add(newRange);
      }
      /******************armar objeto*********************/
      if (cad.startsWith("o ")) {
        Mesh object = armarObjeto(vertexes, normals, vertexesTex, faces,
                                  texturasList, relCarTex);
        object.setName( nomObj ) ;
        object.setMaterial(new Material(matAct));
      //  ret.add(object);
        meshGroup.addMesh(object);

        StringTokenizer auxNomObj = new StringTokenizer(cad, " ");
        auxNomObj.nextToken();
        nomObj = auxNomObj.nextToken();

        faces = new ArrayList<int[][]> ();

        texturasList = new ArrayList<RGBAImage> ();
        relCarTex = new ArrayList<ArrayList<int[]>> ();
        inicial = new ArrayList<int[]> ();
        int[] rangoInicial = new int[2];
        rangoInicial[0] = rangoInicial[1] = 0;
        inicial.add(rangoInicial);
        relCarTex.add(inicial);

        textAct = 0;

      }
    }
    Mesh object = armarObjeto(vertexes, normals, vertexesTex, faces,
                              texturasList, relCarTex);
    object.setMaterial(new Material(matAct));
    object.setName(nomObj);
  //  ret.add(object);
  meshGroup.addMesh(object);
  return meshGroup;
  }

  private static RGBAImage obtenerTextura(String cad, String fileName) {
    StringTokenizer st = new StringTokenizer(cad, " ");
    st.nextToken(); //usemap
    String dirObj = new File(fileName).getParentFile().getAbsolutePath();
    String nomImage = st.nextToken();
    if (nomImage.equals("(null)")) {
      return null;
    }
    try {
      return RGBAImageBuilder.buildImage(new File(dirObj +
                                                  System.getProperty("file.separator") +
                                                  nomImage));
    }
    catch (ImageNotRecognizedException inre) {
      return null;
    }
  }

  private static Mesh armarObjeto(ArrayList<Vector3D> vertexes,
                                  ArrayList<Vector3D> normals,
                                  ArrayList<Vector3D> vertexesTex,
                                  ArrayList<int[][]> faces,
                                  ArrayList<RGBAImage> texturas,
                                  ArrayList<ArrayList<int[]>> relCarTex) {
    Mesh m = new Mesh();

    m.setTrianglesSize(faces.size());

    ArrayList<Integer> usedVertexes = new ArrayList<Integer> ();
    ArrayList<Integer> usedTexVertexes = new ArrayList<Integer> ();

    HashMap<Integer, Integer> cambiosVert = new HashMap<Integer, Integer> ();
    HashMap<Integer, Integer> cambiosVertTex = new HashMap<Integer, Integer> ();

    int indexVert = 0;
    int indexVertTex = 0;

    for (int i = 0; i < m.getTriangles().length; i++) {
      int[] p1 = faces.get(i)[0];
      int[] p2 = faces.get(i)[1];
      int[] p3 = faces.get(i)[2];

      if (!cambiosVert.containsKey(p1[0])) {
        cambiosVert.put(p1[0], indexVert);
        indexVert++;
        usedVertexes.add(p1[0]);
      }
      p1[0] = cambiosVert.get(p1[0]);

      if (!cambiosVertTex.containsKey(p1[1])) {
        cambiosVertTex.put(p1[1], indexVertTex);
        indexVertTex++;
        usedTexVertexes.add(p1[1]);
      }
      p1[1] = cambiosVertTex.get(p1[1]);

      if (!cambiosVert.containsKey(p2[0])) {
        cambiosVert.put(p2[0], indexVert);
        indexVert++;
        usedVertexes.add(p2[0]);
      }
      p2[0] = cambiosVert.get(p2[0]);

      if (!cambiosVertTex.containsKey(p2[1])) {
        cambiosVertTex.put(p2[1], indexVertTex);
        indexVertTex++;
        usedTexVertexes.add(p2[1]);
      }
      p2[1] = cambiosVertTex.get(p2[1]);

      if (!cambiosVert.containsKey(p3[0])) {
        cambiosVert.put(p3[0], indexVert);
        indexVert++;
        usedVertexes.add(p3[0]);
      }
      p3[0] = cambiosVert.get(p3[0]);

      if (!cambiosVertTex.containsKey(p3[1])) {
        cambiosVertTex.put(p3[1], indexVertTex);
        indexVertTex++;
        usedTexVertexes.add(p3[1]);
      }
      p3[1] = cambiosVertTex.get(p3[1]);

    }

    m.setVertexesSize(usedVertexes.size());
    for (int i = 0; i < usedVertexes.size(); i++) {
      m.setVertexAt(i,new Vertex(vertexes.get(usedVertexes.get(i) - 1)));
    }

    m.setVerTexureSize(usedTexVertexes.size());
    for (int i = 0; i < m.getVerTexture().length; i++) {
      m.setVerTextureAt(i,vertexesTex.get(usedTexVertexes.get(i) - 1));
    }

    for (int i = 0; i < m.getTriangles().length; i++) {
      m.setTriangleAt(i,new Triangle(faces.get(i)[0][0],faces.get(i)[1][0],faces.get(i)[2][0],faces.get(i)[0][1],faces.get(i)[1][1],faces.get(i)[2][1]));
    }

    m.setTexturesSize(texturas.size());
    for (int i = 0; i < m.getTextures().length; i++) {
      m.setTextureAt(i , texturas.get(i));
    }

    m.setTexTriRelSize(relCarTex.size());
    for (int i = 0; i < m.getTextTriRel().length; i++) {
      ArrayList<int[]> actRanges = relCarTex.get(i);
      m.setTexTriRelSizeAt(i,actRanges.size());
      for (int j = 0; j < m.getTexTriRelAt(i).length; j++) {
        m.setTextTriRelAt(i,j,actRanges.get(j));
      }
    }

    m.calculateNormals();
    m.calculateMinMaxPositions();

    return m;
  }

  private static int[][] readTriangle(String cad) {
    int[][] ret = new int[3][];
    StringTokenizer st = new StringTokenizer(cad, " ");
    st.nextToken(); //the f token
    for (int i = 0; i < 3; i++) {
      String cadAux = st.nextToken();
      int[] indexes = makeFaceVertex(cadAux);
      ret[i] = indexes;
    }
    return ret;
  }

  private static int[] makeFaceVertex(String cad) {
    int[] ret = new int[3];
    StringTokenizer st = new StringTokenizer(cad, "/");
    try {
      ret[0] = Integer.parseInt(st.nextToken());
    }
    catch (NumberFormatException nfe) {
      ret[0] = -1;
    }

    try {
      ret[1] = Integer.parseInt(st.nextToken());
    }
    catch (NumberFormatException nfe) {
      ret[1] = -1;
    }

    try {
      ret[2] = Integer.parseInt(st.nextToken());
    }
    catch (NumberFormatException nfe) {
      ret[2] = -1;
    }

    return ret;
  }

  /*
      private Mesh readTriangles(String cad, Mesh obj)
      {
          Vector<Triangle> triangles = new Vector<Triangle> ();
          Vector3D[] position = new Vector3D[3];
          Vector3D[] normals = new Vector3D[3];
          StringTokenizer token_space = new StringTokenizer(cad);
          int countTokens = token_space.countTokens();
          token_space.nextElement();
          int[] triang = new int[countTokens];
          int[] nor = new int[countTokens];
          int lastIndex;
          String cade;
          int j = 0;
          while (token_space.hasMoreTokens())
          {
              cade = token_space.nextToken();
              StringTokenizer token_faces = new StringTokenizer(cade);
              triang[j] = Integer.parseInt(token_faces.nextToken("/"));
              token_faces.nextToken("/");
              nor[j] = Integer.parseInt(token_faces.nextToken("/"));
              j++;
          }
          for (int k = 1; k < countTokens - 2; k++)
          {
              position[0] = obj.getPositionAt(triang[0] - 1);
              position[1] = obj.getPositionAt(triang[k] - 1);
              position[2] = obj.getPositionAt(triang[k + 1] - 1);
              normals[0] = obj.getNormalAt(nor[0] - 1);
              normals[1] = obj.getNormalAt(nor[k] - 1);
              normals[2] = obj.getNormalAt(nor[k + 1] - 1);
              obj.addVertex(new Vertex(position[0], normals[0]));
              obj.addVertex(new Vertex(position[1], normals[1]));
              obj.addVertex(new Vertex(position[2], normals[2]));
              lastIndex = obj.getVertexes().size();
              obj.addTriangle(lastIndex - 3, lastIndex - 2, lastIndex-1);
          }
          return obj;
      }
   */

  private static Vector3D readVertex(String cad) {
    Vector3D vert = new Vector3D();
    StringTokenizer st = new StringTokenizer(cad);
    st.nextToken();
    vert.x = Double.parseDouble(st.nextToken());
    vert.y = Double.parseDouble(st.nextToken());
    vert.z = Double.parseDouble(st.nextToken());
    return vert;
  }

  private static Vector3D readVertexTexture(String cad) {
    Vector3D vert = new Vector3D();
    StringTokenizer st = new StringTokenizer(cad);
    st.nextToken();
    vert.x = Double.parseDouble(st.nextToken());
    vert.y = Double.parseDouble(st.nextToken());
    try {
      vert.z = Double.parseDouble(st.nextToken());
    }
    catch (Exception e) {}
    return vert;
  }

  private static HashMap<String,
      Material> readMaterials(String material, String fileName) {
    HashMap<String, Material> ret = new HashMap<String, Material> ();

    StringTokenizer st = new StringTokenizer(material, " ");
    st.nextToken(); //mtlib
    File arc = new File(fileName);
    File dirArc = arc.getParentFile();
    String nomArc = dirArc + System.getProperty("file.separator") +
        st.nextToken();

    try {
      BufferedReader in = new BufferedReader(new FileReader(nomArc));
      String cad = "";

      Material matAct = new Material();
      matAct.setName("default");

      while ( (cad = in.readLine()) != null) {
        if (cad.startsWith("Ns")) {
          StringTokenizer stMat = new StringTokenizer(cad, " ");
          stMat.nextToken(); //Ns

          matAct.setPhongExponent(Float.parseFloat(stMat.nextToken()));
        }
        if (cad.startsWith("Kd")) {
          StringTokenizer stMat = new StringTokenizer(cad, " ");
          stMat.nextToken(); //Kd

          ColorRgb color = new ColorRgb();
          color.r = Float.parseFloat(stMat.nextToken());
          color.g = Float.parseFloat(stMat.nextToken());
          color.b = Float.parseFloat(stMat.nextToken());

          matAct.setDiffuse(color);
        }
        if (cad.startsWith("Ka")) {
          StringTokenizer stMat = new StringTokenizer(cad, " ");
          stMat.nextToken(); //Ka

          ColorRgb color = new ColorRgb();
          color.r = Float.parseFloat(stMat.nextToken());
          color.g = Float.parseFloat(stMat.nextToken());
          color.b = Float.parseFloat(stMat.nextToken());

          matAct.setAmbient(color);
        }
        if (cad.startsWith("Ks")) {
          StringTokenizer stMat = new StringTokenizer(cad, " ");
          stMat.nextToken(); //Ks

          ColorRgb color = new ColorRgb();
          color.r = Float.parseFloat(stMat.nextToken());
          color.g = Float.parseFloat(stMat.nextToken());
          color.b = Float.parseFloat(stMat.nextToken());

          matAct.setSpecular(color);
        }
        if (cad.startsWith("d")) {
          StringTokenizer stMat = new StringTokenizer(cad, " ");
          stMat.nextToken(); //d

          matAct.setAlpha(Float.parseFloat(stMat.nextToken()));
        }
        if (cad.startsWith("newmtl")) {
          StringTokenizer stMat = new StringTokenizer(cad, " ");
          stMat.nextToken(); //newmtl

          ret.put("usemtl " + matAct.getName(), matAct);
          matAct = new Material();
          matAct.setName(stMat.nextToken());
        }
      }
      ret.put("usemtl " + matAct.getName(), matAct);
    }
    catch (IOException ioe) {
    }
    return ret;
  }
}
