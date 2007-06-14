//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - David Diaz, Lina Rojas, Gabriel Sarmiento: Original   =
//=                                                            base version =
//= - August 7 2006 - Oscar Chavarro: re-structured and tested              =
//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

import javax.media.opengl.GL;

import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.geometry.TriangleMesh;

public class JoglTriangleMeshRenderer extends JoglRenderer {
    /**
    @todo program this!
    */
    public static void drawWithSelection(GL gl, TriangleMesh mesh,
                                         QualitySelection quality, 
                                         boolean flip,
                                         ArrayList<int[]> selectedTriangles) {
        draw(gl, mesh, quality, flip);
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.

    @todo Handle PHONG and BUMPMAPPING cases, via vertex/program shaders
    */
    public static void
    draw(GL gl, TriangleMesh mesh, QualitySelection quality, boolean flip) {
        boolean withTextures = (mesh.getTextures().length > 0)?true:false;

        gl.glEnable(gl.GL_NORMALIZE);
        //-----------------------------------------------------------------
        if ( quality.isBumpMapSet() ) {
            // Prepare bump mapping and shaders...
            ;
        }

        if ( quality.isSurfacesSet() ) {
            int shadingType = quality.getShadingType();

            switch ( shadingType ) {
              case QualitySelection.SHADING_TYPE_NOLIGHT:
                gl.glDisable(gl.GL_LIGHTING);
                gl.glShadeModel(gl.GL_FLAT);
                // Warning: Change with configured color for ambient lightning
                gl.glColor3d(1, 1, 1);
                break;
              case QualitySelection.SHADING_TYPE_FLAT:
                gl.glEnable(gl.GL_LIGHTING);
                gl.glShadeModel(gl.GL_FLAT);
                JoglMaterialRenderer.activate(gl, mesh.getMaterial());
                break;
              case QualitySelection.SHADING_TYPE_PHONG:
              case QualitySelection.SHADING_TYPE_GOURAUD: default:
                gl.glEnable(gl.GL_LIGHTING);
                gl.glShadeModel(gl.GL_SMOOTH);
                JoglMaterialRenderer.activate(gl, mesh.getMaterial());
                break;
            }
            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
            gl.glDisable(gl.GL_TEXTURE_2D);
            if ( quality.isTextureSet() && withTextures ) {
                // drawSurfacesWithTexture can enable GL_TEXTURE_2D
                drawSurfacesWithTexture(gl, mesh, flip);
              }
              else {
                drawSurfacesWithoutTexture(gl, mesh, flip);
            }
        }
        if ( quality.isWiresSet() ) {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glShadeModel(gl.GL_FLAT);
            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
            gl.glLineWidth(1.0f);

            // Warning: Change with configured color for borders
            gl.glColor3d(1, 1, 1);
            gl.glDisable(gl.GL_TEXTURE_2D);

            // Warning: pending definition of this behavior...
            drawSurfacesWithoutTexture(gl, mesh, flip);
/*
            if ( quality.isTextureSet() && withTextures ) {
                // drawSurfacesWithTexture can enable GL_TEXTURE_2D
                drawSurfacesWithTexture(gl, mesh, flip);
              }
              else {
                drawSurfacesWithoutTexture(gl, mesh, flip);
            }
*/
        }

        //- Drawing control of elements with no surface -------------------
        if ( quality.isPointsSet() ) {
            drawPoints(gl, mesh);
        }
        if ( quality.isNormalsSet() ) {
            drawVertexNormals(gl, mesh);
        }
        if ( quality.isTrianglesNormalsSet() ) {
            drawTriangleNormals(gl, mesh);
        }
        if ( quality.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, mesh, quality);
        }
    }

    private static void drawPoints(GL gl, TriangleMesh mesh) {
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for point
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(2.0f);

        for ( int i = 0; i < mesh.getVertexes().length; i++ ) {
            gl.glBegin(gl.GL_POINTS);
            gl.glVertex3d(mesh.getVertexAt(i).getPosition().x,
                          mesh.getVertexAt(i).getPosition().y,
                          mesh.getVertexAt(i).getPosition().z);
            gl.glEnd();
        }
    }

    private static void drawVertexNormals(GL gl, TriangleMesh mesh) {
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for vertex normals
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);

        gl.glBegin(gl.GL_LINES);
        for ( int i = 0; i < mesh.getTriangles().length; i++ ) {
            Vertex vertex = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0());
            drawVertexNormal(gl, vertex);
            vertex = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1());
            drawVertexNormal(gl, vertex);
            vertex = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2());
            drawVertexNormal(gl, vertex);
        }
        gl.glEnd();
    }

    private static void drawVertexNormal(GL gl, Vertex vertex) {
        double l = 0.2;

        gl.glVertex3d(vertex.getPosition().x, vertex.getPosition().y,
                      vertex.getPosition().z);
        gl.glVertex3d(vertex.getPosition().x + (vertex.getNormal().x * l),
                      vertex.getPosition().y + (vertex.getNormal().y * l),
                      vertex.getPosition().z + (vertex.getNormal().z * l));
    }

    private static void drawTriangleNormals(GL gl, TriangleMesh m) {
        double l = 0.15;

        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for surface normals
        gl.glColor3d(0.9, 0.9, 0.5);
        gl.glLineWidth(2.0f);

        gl.glBegin(gl.GL_LINES);
        for ( int i = 0; i < m.getTriangles().length; i++ ) {
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

            gl.glVertex3d(cx, cy, cz);
            gl.glVertex3d(cx + m.getTriangleAt(i).normal.x * l,
                          cy + m.getTriangleAt(i).normal.y * l,
                          cz + m.getTriangleAt(i).normal.z * l);
        }
        gl.glEnd();
    }

    private static void drawSurfacesWithoutTexture(GL gl, 
                                     TriangleMesh mesh, boolean flipNormals) {
        drawRange(gl, mesh, 
                                   0, mesh.getTriangles().length, flipNormals);
    }

    private static void drawSurfacesWithTexture(GL gl, TriangleMesh mesh, boolean flip) {
        Image[] textureArray = mesh.getTextures();
        int i, j;

        for ( i = 0; i < mesh.getTextTriRel().length; i++ ) {
            for ( j = 0; j < mesh.getTexTriRelAt(i).length; j++ ) {
                if ( i >= 1 ) {
                    gl.glEnable(gl.GL_TEXTURE_2D);
                    JoglImageRenderer.activate(gl, textureArray[i - 1]);
                    // Warning: Shoult this be here? or not ...
                    gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);
                    gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);
                    gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_REPEAT);
                    gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_REPEAT);
                    gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_DECAL);
                }
                else {
                    gl.glDisable(gl.GL_TEXTURE_2D);
                }
                drawRange(gl, mesh, mesh.getTextTriRelAt(i, j, 0),
                                           mesh.getTextTriRelAt(i, j, 1), flip);
            }
        }
    }

    private static void
    drawRange(GL gl, TriangleMesh mesh, 
                               int ini, int fin, boolean flipNormals) {
        Vertex v0, v1, v2;

        gl.glBegin(gl.GL_TRIANGLES);
        for ( int i = ini; i < fin; i++ ) {
            v0 = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint0());
            v1 = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint1());
            v2 = mesh.getVertexAt(mesh.getTriangleAt(i).getPoint2());

            if ( !flipNormals ) {
                gl.glNormal3d(v0.getNormal().x,
                              v0.getNormal().y,
                              v0.getNormal().z);
              }
              else {
                gl.glNormal3d(-v0.getNormal().x,
                              -v0.getNormal().y,
                              -v0.getNormal().z);
            }
            gl.glTexCoord2d(mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt0()).x,
                            mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt0()).y);
            gl.glVertex3d(v0.getPosition().x,
                          v0.getPosition().y,
                          v0.getPosition().z);

            if ( !flipNormals ) {
                gl.glNormal3d(v1.getNormal().x,
                              v1.getNormal().y,
                              v1.getNormal().z);
            }
            else {
                gl.glNormal3d(-v1.getNormal().x,
                              -v1.getNormal().y,
                              -v1.getNormal().z);
            }
            gl.glTexCoord2d(mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt1()).x,
                            mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt1()).y);

            gl.glVertex3d(v1.getPosition().x,
                          v1.getPosition().y,
                          v1.getPosition().z);

            if ( !flipNormals ) {
                gl.glNormal3d(v2.getNormal().x,
                              v2.getNormal().y,
                              v2.getNormal().z);
              }
              else {
                gl.glNormal3d(-v2.getNormal().x,
                              -v2.getNormal().y,
                              -v2.getNormal().z);
            }
            gl.glTexCoord2d(mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt2()).x,
                            mesh.getVerTextureAt(mesh.getTriangleAt(i).getVt2()).y);
            gl.glVertex3d(v2.
                          getPosition().x,
                          v2.
                          getPosition().y,
                          v2.
                          getPosition().z);
        }
        gl.glEnd();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
