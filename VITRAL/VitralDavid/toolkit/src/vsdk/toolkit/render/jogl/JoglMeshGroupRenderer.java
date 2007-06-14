package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.geometry.MeshGroup;
import vsdk.toolkit.environment.geometry.Mesh;
import java.util.Iterator;
import java.util.ArrayList;

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
public class JoglMeshGroupRenderer {

  public static void draw(GL gl, MeshGroup meshGroup, QualitySelection quality) {
    Mesh mesh = null;
    for (Iterator<Mesh> i = meshGroup.getMeshes().iterator(); i.hasNext(); ) {
      mesh = (Mesh) i.next();
      if (quality.isSurfacesSet()) {
          int qt = quality.getShadingType();
          gl.glCullFace(gl.GL_FRONT);

          if ( qt == quality.SHADING_TYPE_GOURAUD || qt == quality.SHADING_TYPE_PHONG ) {
              JoglMeshRenderer.drawSurfacesSolid(gl, mesh, false);
            }
            else {
              JoglMeshRenderer.drawSurfacesSmooth(gl,mesh, false);
          }

          gl.glCullFace(gl.GL_BACK);
          if ( qt == quality.SHADING_TYPE_GOURAUD || qt == quality.SHADING_TYPE_PHONG ) {
              JoglMeshRenderer.drawSurfacesSolid(gl, mesh, true);
            }
            else {
              JoglMeshRenderer.drawSurfacesSmooth(gl,mesh, true);
          }
      }
      if (quality.isWiresSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(1, 1, 1);
          JoglMeshRenderer.drawWires(gl, mesh);
        }
        gl.glPopAttrib();

      }
      if (quality.isBoundingVolumeSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(0, 0, 1);

          //JoglMeshRenderer.drawBoundingVolume(gl, mesh);
        }
                //drawBoundingVolume(gl,meshGroup);
        gl.glPopAttrib();

      }
      if (quality.isTextureSet()) {
        JoglMeshRenderer.drawTexture(gl, mesh);
      }
      if (quality.isBumpMapSet()) {
        JoglMeshRenderer.drawBumpMap(gl,mesh);
      }
      if (quality.isPointsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(1, 0, 0);

          JoglMeshRenderer.drawPoints(gl, mesh);
        }
        gl.glPopAttrib();

      }
      if (quality.isNormalsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(0, 1, 0);

          JoglMeshRenderer.drawNormals(gl, mesh);
        }
        gl.glPopAttrib();

      }
      if (quality.isTrianglesNormalsSet())
      {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(1, 1, 0);

          JoglMeshRenderer.drawNormalsTriangles(gl, mesh);
        }
        gl.glPopAttrib();
      }

      //  drawShading(gl, quality.getShading());

    }

  }

  public static void drawWithSelection(GL gl, MeshGroup meshGroup, QualitySelection quality, ArrayList<int[]> selectedTriangles) {
    Mesh mesh = null;
    for (Iterator<Mesh> i = meshGroup.getMeshes().iterator(); i.hasNext(); ) {
      mesh = (Mesh) i.next();
      if (quality.isSurfacesSet()) {
          int qt = quality.getShadingType();
          gl.glCullFace(gl.GL_FRONT);

          if ( qt == quality.SHADING_TYPE_GOURAUD || qt == quality.SHADING_TYPE_PHONG ) {
              JoglMeshRenderer.drawSurfacesSolid(gl, mesh, false);
            }
            else {
              JoglMeshRenderer.drawSurfacesSmooth(gl,mesh, false);
          }

          gl.glCullFace(gl.GL_BACK);
          if ( qt == quality.SHADING_TYPE_GOURAUD || qt == quality.SHADING_TYPE_PHONG ) {
              JoglMeshRenderer.drawSurfacesSolid(gl, mesh, true);
            }
            else {
              JoglMeshRenderer.drawSurfacesSmooth(gl,mesh, true);
          }

      }
      if (quality.isWiresSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(1, 1, 1);
          JoglMeshRenderer.drawWires(gl, mesh);
        }
        gl.glPopAttrib();

      }
      if (quality.isBoundingVolumeSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(0, 0, 1);

          //JoglMeshRenderer.drawBoundingVolume(gl, mesh);
        }
                //drawBoundingVolume(gl,meshGroup);
        gl.glPopAttrib();

      }
      if (quality.isTextureSet()) {
        JoglMeshRenderer.drawTexture(gl, mesh);
      }
      if (quality.isBumpMapSet()) {
        JoglMeshRenderer.drawBumpMap(gl,mesh);
      }
      if (quality.isPointsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(1, 0, 0);

          JoglMeshRenderer.drawPoints(gl, mesh);
        }
        gl.glPopAttrib();

      }
      if (quality.isNormalsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(0, 1, 0);

          JoglMeshRenderer.drawNormals(gl, mesh);
        }
        gl.glPopAttrib();

      }
      if (quality.isTrianglesNormalsSet())
      {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
          gl.glDisable(gl.GL_LIGHTING);
          gl.glDisable(gl.GL_DEPTH_TEST);
          gl.glDisable(gl.GL_BLEND);
          gl.glDepthMask(false);
          gl.glColor3f(1, 1, 0);

          JoglMeshRenderer.drawNormalsTriangles(gl, mesh);
        }
        gl.glPopAttrib();
      }

      //  drawShading(gl, quality.getShading());

    }

  }


}
