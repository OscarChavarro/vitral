package vsdk.toolkit.render.jogl;

import java.util.HashMap;
import java.nio.ByteBuffer;
import javax.media.opengl.GL;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.environment.geometry.Mesh;
import vsdk.toolkit.common.QualitySelection;

public class JoglMeshRenderer {
  private static HashMap<Mesh, TextureDecorator> objTex = new HashMap<Mesh,
      TextureDecorator> ();
  private static class TextureDecorator {
    Mesh mesh;
    int[] textObjects;

    TextureDecorator(Mesh m) {
      mesh = m;
    }
  }

  public static void initTexture(GL gl, Mesh m) {
    TextureDecorator td = new TextureDecorator(m);
    td.textObjects = new int[m.getTextures().length];
    gl.glGenTextures(td.textObjects.length, td.textObjects, 0);

    for (int i = 0; i < td.textObjects.length; i++) {
      int colors = gl.GL_RGBA;

      if (m.getTextureAt(i).getPixelDepth() == 24) {
        colors = gl.GL_RGB;
      }
      gl.glBindTexture(gl.GL_TEXTURE_2D, td.textObjects[i]);

      gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER,
                         gl.GL_LINEAR);
      gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER,
                         gl.GL_LINEAR);
      gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_REPEAT);
      gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_REPEAT);

      gl.glTexImage2D(gl.GL_TEXTURE_2D,
                      0,
                      colors,
                      m.getTextureAt(i).getXSize(), m.getTextureAt(i).getYSize(),
                      0,
                      colors,
                      gl.GL_UNSIGNED_BYTE,
                      ByteBuffer.wrap(m.getTextureAt(i).getRawImage())
          );
    }

    objTex.put(m, td);
  }

  public static void drawVertexNormal(GL gl, Vertex vertex) {
    gl.glBegin(gl.GL_LINES);
    gl.glVertex3d(vertex.getPosition().x, vertex.getPosition().y,
                  vertex.getPosition().z);
    gl.glVertex3d(vertex.getPosition().x + (vertex.getNormal().x * 0.1),
                  vertex.getPosition().y + (vertex.getNormal().y * 0.1),
                  vertex.getPosition().z + (vertex.getNormal().z * 0.1));
    gl.glEnd();
  }

  /**
   *
   * @param gl GL
   * @param mesh Mesh
   * @param quality QualitySelection
   */
  public static void draw(GL gl, Mesh mesh, QualitySelection quality,
                          boolean flip) {
    if (quality.isSurfacesSet()) {
      int qt = quality.getShadingType();
      if (qt == quality.SHADING_TYPE_GOURAUD ||
          qt == quality.SHADING_TYPE_PHONG) {
        drawSurfacesSolid(gl, mesh, flip);
      }
      else {
        drawSurfacesSmooth(gl, mesh, flip);
      }
    }
    if (quality.isWiresSet()) {
      drawWires(gl, mesh);
    }
    if (quality.isBoundingVolumeSet()) {
      //drawBoundingVolume(gl, mesh);
    }
    if (quality.isTextureSet()) {
      drawTexture(gl, mesh);
    }
    if (quality.isBumpMapSet()) {
      drawBumpMap(gl, mesh);
    }
    if (quality.isPointsSet()) {
      drawPoints(gl, mesh);
    }
    if (quality.isNormalsSet()) {
      drawNormals(gl, mesh);
    }
    if (quality.isTrianglesNormalsSet()) {
      drawNormalsTriangles(gl, mesh);
    }
    if (quality.isBoundingVolumeSet()) {
      JoglGeometryRenderer.drawMinMaxBox(gl, mesh, quality);
    }

    drawShading(gl, quality.getShadingType());
  }

  public static void drawWithSelection(GL gl, Mesh mesh,
                                       QualitySelection quality, boolean flip,
                                       int[] selectedTriangles) {
    if (quality.isSurfacesSet()) {
      int qt = quality.getShadingType();
      if (qt == quality.SHADING_TYPE_GOURAUD ||
          qt == quality.SHADING_TYPE_PHONG) {
        drawSurfacesSolid(gl, mesh, flip);
      }
      else {
        drawSurfacesSmooth(gl, mesh, flip);
      }
    }
    if (quality.isWiresSet()) {
      drawWires(gl, mesh);
    }
    if (quality.isBoundingVolumeSet()) {
      //drawBoundingVolume(gl, mesh);
    }
    if (quality.isTextureSet()) {
      drawTexture(gl, mesh);
    }
    if (quality.isBumpMapSet()) {
      drawBumpMap(gl, mesh);
    }
    if (quality.isPointsSet()) {
      drawPoints(gl, mesh);
    }
    if (quality.isNormalsSet()) {
      drawNormals(gl, mesh);
    }
    if (quality.isTrianglesNormalsSet()) {
      drawNormalsTriangles(gl, mesh);
    }
    if (quality.isBoundingVolumeSet()) {
      JoglGeometryRenderer.drawMinMaxBox(gl, mesh, quality);
    }

    drawShading(gl, quality.getShadingType());

  }

  public static void drawSurfacesSmooth(GL gl, Mesh mesh, boolean flip) {
    JoglMaterialRenderer.activate(gl, mesh.getMaterial());
    for (int i = 0; i < mesh.getTriangles().length; i++) {
      gl.glBegin(gl.GL_TRIANGLES);
      {
        if (flip == false) {
          gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().x,
                        mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().y,
                        mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().z);
        }
        else {
          gl.glNormal3d( -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().x,
                        -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().y,
                        -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().z);
        }

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().z);

        if (flip == false) {
          gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                        getNormal().x,
                        mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                        getNormal().y,
                        mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                        getNormal().z);
        }
        else {
          gl.glNormal3d( -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                        getNormal().x,
                        -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                        getNormal().y,
                        -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                        getNormal().z);

        }
        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().z);

        if (flip == false) {
          gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                        getNormal().x,
                        mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                        getNormal().y,
                        mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                        getNormal().z);
        }
        else {
          gl.glNormal3d( -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                        getNormal().x,
                        -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                        getNormal().y,
                        -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                        getNormal().z);
        }

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().z);
      }
      gl.glEnd();
    }
  }

  public static void drawSurfacesSolid(GL gl, Mesh mesh, boolean flip) {
    JoglMaterialRenderer.activate(gl, mesh.getMaterial());
    for (int i = 0; i < mesh.getTriangles().length; i++) {
      gl.glBegin(gl.GL_TRIANGLES);
      {
        if (flip == false) {
          gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().x,
                        mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().y,
                        mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().z);
        }
        else {
          gl.glNormal3d( -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().x,
                        -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().y,
                        -mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                        getNormal().z);
        }

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().z);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().z);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().z);
      }
      gl.glEnd();
    }
  }

  public static void drawWires(GL gl, Mesh mesh) {

    JoglMaterialRenderer.activate(gl, mesh.getMaterial());
    for (int i = 0; i < mesh.getTriangles().length; i++) {
      gl.glBegin(gl.GL_LINE_LOOP);
      {
        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().z);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().z);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().z);
      }
      gl.glEnd();
    }
  }

  public static void drawTexture(GL gl, Mesh mesh) {
    JoglMaterialRenderer.activate(gl, mesh.getMaterial());
    int[] textObjs = null;
    if (objTex.containsKey(mesh)) {
      textObjs = objTex.get(mesh).textObjects;
    }
    else {
      initTexture(gl, mesh);
      textObjs = objTex.get(mesh).textObjects;
    }
    gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
    {

      for (int i = 0; i < mesh.getTextTriRel().length && i < 1; i++) {
        for (int j = 0; j < mesh.getTexTriRelAt(i).length; j++) {
          drawSolidRange(gl, mesh, mesh.getTextTriRelAt(i, j, 0),
                         mesh.getTextTriRelAt(i, j, 1));
        }
      }

      gl.glEnable(gl.GL_TEXTURE_2D);

      for (int i = 1; i < mesh.getTextTriRel().length; i++) {
        for (int j = 0; j < mesh.getTexTriRelAt(i).length; j++) {
          drawTextureRange(gl, mesh, mesh.getTextTriRelAt(i, j, 0),
                           mesh.getTextTriRelAt(i, j, 1), textObjs[i - 1]);
        }
      }
    }
    gl.glPopAttrib();
  }

  private static void drawTextureRange(GL gl, Mesh mesh, int ini, int fin,
                                       int texObj) {
    gl.glBindTexture(gl.GL_TEXTURE_2D, texObj);
    for (int i = ini; i < fin; i++) {
      gl.glBegin(gl.GL_TRIANGLES);
      {
        gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getNormal().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getNormal().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getNormal().z);

        gl.glTexCoord2d(mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt0()).x,
                        mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt0()).y);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().z);

        gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getNormal().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getNormal().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getNormal().z);

        gl.glTexCoord2d(mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt1()).x,
                        mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt1()).y);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().z);

        gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getNormal().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getNormal().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getNormal().z);

        gl.glTexCoord2d(mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt2()).x,
                        mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt2()).y);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().z);
      }
      gl.glEnd();
    }
  }

  private static void drawSolidRange(GL gl, Mesh mesh, int ini, int fin) {
    for (int i = ini; i < fin; i++) {
      gl.glBegin(gl.GL_TRIANGLES);
      {
        gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getNormal().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getNormal().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getNormal().z);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0()).
                      getPosition().z);

        gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getNormal().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getNormal().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getNormal().z);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1()).
                      getPosition().z);

        gl.glNormal3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getNormal().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getNormal().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getNormal().z);

        gl.glVertex3d(mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().x,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().y,
                      mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2()).
                      getPosition().z);
      }
      gl.glEnd();
    }
  }

  public static void drawBumpMap(GL gl, Mesh mesh) {
    JoglMaterialRenderer.activate(gl, mesh.getMaterial());
  }

  private static void drawShading(GL gl, int shadingType) {
    switch (shadingType) {
      case QualitySelection.SHADING_TYPE_FLAT:
        break;
      case QualitySelection.SHADING_TYPE_GOURAUD:
        break;
      case QualitySelection.SHADING_TYPE_PHONG:
        break;

    }
  }

  public static void drawPoints(GL gl, Mesh mesh) {
    JoglMaterialRenderer.activate(gl, mesh.getMaterial());
    for (int i = 0; i < mesh.getVertexes().length; i++) {
      gl.glBegin(gl.GL_POINTS);
      {
        gl.glVertex3d(mesh.getVertexAt(i).getPosition().x,
                      mesh.getVertexAt(i).getPosition().y,
                      mesh.getVertexAt(i).getPosition().z);
      }
      gl.glEnd();
    }
  }

  public static void drawNormals(GL gl, Mesh mesh) {
    for (int i = 0; i < mesh.getTriangles().length; i++) {
      Vertex vertex = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0());
      drawVertexNormal(gl, vertex);
      vertex = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1());
      drawVertexNormal(gl, vertex);
      vertex = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2());
      drawVertexNormal(gl, vertex);
    }
  }

  public static void drawNormalsTriangles(GL gl, Mesh m) {
    for (int i = 0; i < m.getTriangles().length; i++) {
      double cx = m.getVertexAt(m.getTriangleAt(i).getPoint0()).getPosition().x;
      cx += m.getVertexAt(m.getTriangleAt(i).getPoint1()).getPosition().x;
      cx += m.getVertexAt(m.getTriangleAt(i).getPoint2()).getPosition().x;

      double cy = m.getVertexAt(m.getTriangleAt(i).getPoint0()).getPosition().y;
      cy += m.getVertexAt(m.getTriangleAt(i).getPoint1()).getPosition().y;
      cy += m.getVertexAt(m.getTriangleAt(i).getPoint2()).getPosition().y;

      double cz = m.getVertexAt(m.getTriangleAt(i).getPoint0()).getPosition().z;
      cz += m.getVertexAt(m.getTriangleAt(i).getPoint1()).getPosition().z;
      cz += m.getVertexAt(m.getTriangleAt(i).getPoint2()).getPosition().z;

      cx /= 3;
      cy /= 3;
      cz /= 3;

      gl.glBegin(gl.GL_LINES);
      {
        gl.glVertex3d(cx, cy, cz);
        gl.glVertex3d(cx + m.getTriangleAt(i).normal.x * 0.1,
                      cy + m.getTriangleAt(i).normal.y * 0.1,
                      cz + m.getTriangleAt(i).normal.z * 0.1);
      }
      gl.glEnd();
    }
  }
}
