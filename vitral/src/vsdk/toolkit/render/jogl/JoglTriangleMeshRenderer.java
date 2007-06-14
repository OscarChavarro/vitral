//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - David Diaz, Lina Rojas, Gabriel Sarmiento: Original   =
//=                                                            base version =
//= - August 7 2006 - Oscar Chavarro: re-structured and tested              =
//= - November 13 2006 - Oscar Chavarro: re-structured and tested           =
//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

import javax.media.opengl.GL;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.Material;

public class JoglTriangleMeshRenderer extends JoglRenderer {

    private static Vector3D p = new Vector3D();
    private static Vector3D n = new Vector3D();

    /**
    @todo program this!
    */
    public static void drawWithSelection(GL gl, TriangleMesh mesh,
                                         RendererConfiguration quality, 
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
    draw(GL gl, TriangleMesh mesh, RendererConfiguration quality, boolean flip) {
        boolean withTextures = false;
        if ( mesh.getTextures() != null && mesh.getTextures().length > 0 ) {
            withTextures = true;
        }

        gl.glEnable(gl.GL_NORMALIZE);

        //-----------------------------------------------------------------
        if ( quality.isBumpMapSet() ) {
            // Prepare bump mapping and shaders...
            ;
        }

        if ( quality.isSurfacesSet() ) {
            JoglGeometryRenderer.prepareSurfaceQuality(gl, quality);
            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
            gl.glPolygonOffset(0.0f, 0.0f);
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
            gl.glDisable(gl.GL_CULL_FACE);
            gl.glShadeModel(gl.GL_FLAT);

            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
            gl.glEnable(gl.GL_POLYGON_OFFSET_LINE);
            gl.glPolygonOffset(-0.5f, 0.0f);
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
        if ( quality.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, mesh, quality);
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
        p = vertex.getPosition();
        n = vertex.getNormal();

        gl.glVertex3d(p.x + (n.x * l/100),
                      p.y + (n.y * l/100),
                      p.z + (n.z * l/100));
        gl.glVertex3d(p.x + (n.x * l),
                      p.y + (n.y * l),
                      p.z + (n.z * l));
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
        if ( mesh.getTriangles() == null ) {
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglTriangleMeshRenderer.activate",
                               "Trying to draw mesh without triangles?");
            return;
        }
        int materialRanges[][] = mesh.getMaterialRanges();

        if ( materialRanges == null ) {
            drawRangeWithoutTexture(gl, mesh,
                                   0, mesh.getTriangles().length, flipNormals);
            return;
        }

        int start = 0;
        int end = 0;
        int materialIndex;
        Material materialsArray[] = mesh.getMaterials();
        for ( int i = 0; i < materialRanges.length; i++ ) {
            end = materialRanges[i][0];
            materialIndex = materialRanges[i][1];
            if ( materialIndex >= 0 ) {
                JoglMaterialRenderer.activate(gl,
                    materialsArray[materialIndex]);
            }
            drawRangeWithoutTexture(gl, mesh, start, end, flipNormals);
            start = end;
        }
    }

    /**
    Note that current implementation only works for a TriangleMesh that
    contains both getMaterials() and getMaterialRanges() defined!

    Main algorithm is a mixed iterative advanced over 
    texturesRanges and materialsRanges arrays.
    */
    private static void
    drawSurfacesWithTexture(GL gl, TriangleMesh mesh, boolean flip) {
        // Support variables
        Image[] texturesArray = mesh.getTextures();
        Material materialsArray[] = mesh.getMaterials();
        int texturesRanges[][] = mesh.getTextureRanges();
        int materialsRanges[][] = mesh.getMaterialRanges();

        // Main cycle variables / cycle initialization
        int start = 0;
        int end = 0;
        int it = 0;
        int im =  0;
        int currentTextureIndex;
        int currentMaterialIndex;
        int previousTextureIndex = -1;
        int previousMaterialIndex = -1;

        if ( materialsRanges == null ) {
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglTriangleMeshRenderer.drawSurfacesWithTexture",
                "Non implemented support for null materialsRanges.");
            return;
        }

        do {
            //- Cycle body ----------------------------------------------------
            end = Math.min(texturesRanges[it][0], materialsRanges[im][0]);
            currentTextureIndex = texturesRanges[it][1]-1;
            currentMaterialIndex = materialsRanges[im][1];

            if ( currentMaterialIndex != previousMaterialIndex ) {
                if ( currentMaterialIndex >= 0 ) {
                    JoglMaterialRenderer.activate(gl,
                        materialsArray[currentMaterialIndex]);
                }
                previousMaterialIndex = currentMaterialIndex;
            }
            if ( currentTextureIndex != previousTextureIndex ) {
                if ( currentTextureIndex >= 0 ) {
                    gl.glEnable(gl.GL_TEXTURE_2D);
                    JoglImageRenderer.activate(gl,
                        texturesArray[currentTextureIndex]);
                    // Warning: Shoult this be here? or not ...
                    gl.glTexParameteri(gl.GL_TEXTURE_2D,
                        gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);
                    gl.glTexParameteri(gl.GL_TEXTURE_2D,
                        gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);
                    gl.glTexParameterf(gl.GL_TEXTURE_2D,
                        gl.GL_TEXTURE_WRAP_S, gl.GL_REPEAT);
                    gl.glTexParameterf(gl.GL_TEXTURE_2D,
                        gl.GL_TEXTURE_WRAP_T, gl.GL_REPEAT);
                    gl.glTexEnvf(gl.GL_TEXTURE_ENV,
                        gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
                }
                else {
                    gl.glDisable(gl.GL_TEXTURE_2D);
                }
                previousTextureIndex = currentTextureIndex;
            }

            if ( currentTextureIndex >= 0 ) {
                drawRangeWithTexture(gl, mesh, start, end, flip);
              }
              else {
                drawRangeWithoutTexture(gl, mesh, start, end, flip);
            }

            //- Cycle advancment ----------------------------------------------
            if ( texturesRanges[it][0] < materialsRanges[im][0] &&
                 it < texturesRanges.length ) {
                it++;
            }
            else if ( texturesRanges[it][0] > materialsRanges[im][0] &&
                 im < materialsRanges.length ) {
                im++;
            }
            else if ( texturesRanges[it][0] == materialsRanges[im][0] ) {
                if ( it < texturesRanges.length ) it++;
                if ( im < materialsRanges.length ) im++;
            }
            start = end;
        } while ( it < texturesRanges.length && im < materialsRanges.length );
    }

    private static void
    drawRangeWithTexture(GL gl, TriangleMesh mesh, 
                         int start, int end, boolean flipNormals) {
        Vertex v0, v1, v2;

        gl.glBegin(gl.GL_TRIANGLES);
        for ( int i = start; i < end; i++ ) {
            v0 = mesh.getVertexAt(mesh.getTriangleAt(i).p0);
            v1 = mesh.getVertexAt(mesh.getTriangleAt(i).p1);
            v2 = mesh.getVertexAt(mesh.getTriangleAt(i).p2);

            if ( !flipNormals ) {
                gl.glNormal3d(v0.normal.x, v0.normal.y, v0.normal.z);
              }
              else {
                gl.glNormal3d(-v0.normal.x, -v0.normal.y, -v0.normal.z);
            }
            //gl.glTexCoord2d(0, 0);
            //gl.glNormal3d(0, 0, 1);
            gl.glTexCoord2d(v0.u, v0.v);
            gl.glVertex3d(v0.position.x, v0.position.y, v0.position.z);
            if ( !flipNormals ) {
                gl.glNormal3d(v1.normal.x, v1.normal.y, v1.normal.z);
            }
            else {
                gl.glNormal3d(-v1.normal.x, -v1.normal.y, -v1.normal.z);
            }
            gl.glTexCoord2d(v1.u, v1.v);
            gl.glVertex3d(v1.position.x, v1.position.y, v1.position.z);
            if ( !flipNormals ) {
                gl.glNormal3d(v2.normal.x, v2.normal.y, v2.normal.z);
              }
              else {
                gl.glNormal3d(-v2.normal.x, -v2.normal.y, -v2.normal.z);
            }
            gl.glTexCoord2d(v2.u, v2.v);
            gl.glVertex3d(v2.position.x, v2.position.y, v2.position.z);
        }
        gl.glEnd();
    }

    private static void
    drawRangeWithoutTexture(GL gl, TriangleMesh mesh, 
                            int start, int end, boolean flipNormals) {
        Vertex v0, v1, v2;

        gl.glBegin(gl.GL_TRIANGLES);
        for ( int i = start; i < end; i++ ) {
            v0 = mesh.getVertexAt(mesh.getTriangleAt(i).p0);
            v1 = mesh.getVertexAt(mesh.getTriangleAt(i).p1);
            v2 = mesh.getVertexAt(mesh.getTriangleAt(i).p2);

            if ( !flipNormals ) {
                gl.glNormal3d(v0.normal.x, v0.normal.y, v0.normal.z);
              }
              else {
                gl.glNormal3d(-v0.normal.x, -v0.normal.y, -v0.normal.z);
            }
            gl.glVertex3d(v0.position.x, v0.position.y, v0.position.z);

            if ( !flipNormals ) {
                gl.glNormal3d(v1.normal.x, v1.normal.y, v1.normal.z);
            }
            else {
                gl.glNormal3d(-v1.normal.x, -v1.normal.y, -v1.normal.z);
            }
            gl.glVertex3d(v1.position.x, v1.position.y, v1.position.z);

            if ( !flipNormals ) {
                gl.glNormal3d(v2.normal.x, v2.normal.y, v2.normal.z);
              }
              else {
                gl.glNormal3d(-v2.normal.x, -v2.normal.y, -v2.normal.z);
            }
            gl.glVertex3d(v2.position.x, v2.position.y, v2.position.z);
        }
        gl.glEnd();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
