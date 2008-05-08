//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - David Diaz, Lina Rojas, Gabriel Sarmiento: Original   =
//=                                                            base version =
//= - August 7 2006 - Oscar Chavarro: re-structured and tested              =
//= - November 13 2006 - Oscar Chavarro: re-structured and tested           =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Basic Java classes
import java.util.ArrayList;
import java.nio.DoubleBuffer;

// JOGL classes
import javax.media.opengl.GL;
import com.sun.opengl.util.BufferUtil;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.Material;

public class JoglTriangleMeshRenderer extends JoglRenderer {

    private static DoubleBuffer vertexPositionsBuffer;

    private static DoubleBuffer
    allocateVertexPositions(double[] vertexPositions)
    {
        if ( vertexPositionsBuffer == null ) {
            vertexPositionsBuffer = BufferUtil.newDoubleBuffer(vertexPositions.length);

            int i;
            for ( i = 0; i < vertexPositions.length; i++ ) {
                vertexPositionsBuffer.put(vertexPositions[i]);
            }
            vertexPositionsBuffer.rewind();
        }
        return vertexPositionsBuffer;
    }


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
            // This line doesn't make sense (as it will later reactivated),
            // but it is needed in some graphics cards/driver/os combinations
            // like AMD Athlon64 running 32 bit Fedora Core 6 with Nvidia C51
            gl.glDisable(gl.GL_LIGHTING);
            //

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
            ColorRgb c = quality.getWireColor();
            gl.glColor3d(c.r, c.g, c.b);
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

        //-----------------------------------------------------------------
        // Old method
/*
        int i;
        int nv;
        double v[];

        nv = mesh.getNumVertices();
        v = mesh.getVertexPositions();

        gl.glBegin(gl.GL_POINTS);
        for ( i = 0; i < nv; i++ ) {
            gl.glVertex3d(v[3*i], v[3*i+1], v[3*i+2]);
        }
        gl.glEnd();
*/
        // New method
        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, gl.GL_DOUBLE, 0, allocateVertexPositions(mesh.getVertexPositions()));
        gl.glDrawArrays(gl.GL_POINTS, 0, mesh.getNumVertices());
        gl.glDisableClientState(gl.GL_VERTEX_ARRAY);

        //-----------------------------------------------------------------
    }

    private static void drawVertexNormals(GL gl, TriangleMesh mesh) {
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for vertex normals
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);

        gl.glBegin(gl.GL_LINES);
        Vertex vertex = new Vertex(new Vector3D(), new Vector3D());
        int i;
        for ( i = 0; i < mesh.getNumVertices(); i++ ) {
            mesh.getVertexAt(i, vertex);
            JoglGeometryRenderer.drawVertexNormal(gl, vertex);
        }
        gl.glEnd();
    }

    private static void drawTriangleNormals(GL gl, TriangleMesh m) {
        double l = 0.15;

        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for surface normals
        gl.glColor3d(0.9, 0.9, 0.5);
        gl.glLineWidth(2.0f);

        gl.glBegin(gl.GL_LINES);
        double cx, cy, cz;
        double v[];
        double n[];
        int t[];
        v = m.getVertexPositions();
        n = m.getTriangleNormals();
        t = m.getTriangleIndexes();
        for ( int i = 0; i < m.getNumTriangles(); i++ ) {
            cx = (v[3*t[3*i]+0] + v[3*t[3*i+1]+0] + v[3*t[3*i+2]+0]) / 3;
            cy = (v[3*t[3*i]+1] + v[3*t[3*i+1]+1] + v[3*t[3*i+2]+1]) / 3;
            cz = (v[3*t[3*i]+2] + v[3*t[3*i+1]+2] + v[3*t[3*i+2]+2]) / 3;

            gl.glVertex3d(cx, cy, cz);
            gl.glVertex3d(cx + n[3*i+0] * l,
                          cy + n[3*i+1] * l,
                          cz + n[3*i+2] * l);
        }
        gl.glEnd();
    }

    private static void drawSurfacesWithoutTexture(GL gl, 
                                     TriangleMesh mesh, boolean flipNormals) {
        //-----------------------------------------------------------------
        // Warning: Shoult this be here? or not ...
        // Just in case using external texture...
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

        //-----------------------------------------------------------------
        int nt;
        nt = mesh.getNumTriangles();
        if ( nt < 1 ) {
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglTriangleMeshRenderer.activate",
                               "Trying to draw mesh without triangles?");
            return;
        }
        int materialRanges[][] = mesh.getMaterialRanges();

        if ( materialRanges == null ) {
            drawRangeWithoutTexture(gl, mesh,
                                    0, nt, flipNormals);
            return;
        }

        int start = 0;
        int end = 0;
        int materialIndex;
        Material materialsArray[] = mesh.getMaterials();
        for ( int i = 0; i < materialRanges.length; i++ ) {
            end = materialRanges[i][0];
            materialIndex = materialRanges[i][1];
            if ( materialIndex >= 0 && materialIndex < materialsArray.length ) {
                JoglMaterialRenderer.activate(gl,
                    materialsArray[materialIndex]);
            }
            drawRangeWithoutTexture(gl, mesh, start, end, flipNormals);
            start = end;
        }
        if ( end <= nt ) {
            Material m = new Material();
            JoglMaterialRenderer.activate(gl, m);
            drawRangeWithoutTexture(gl, mesh, start, nt, flipNormals);
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
        int t[];

        gl.glBegin(gl.GL_TRIANGLES);
        v0 = new Vertex(new Vector3D(), new Vector3D());
        v1 = new Vertex(new Vector3D(), new Vector3D());
        v2 = new Vertex(new Vector3D(), new Vector3D());
        t = mesh.getTriangleIndexes();
        for ( int i = start; i < end; i++ ) {
            mesh.getVertexAt(t[3*i], v0);
            mesh.getVertexAt(t[3*i+1], v1);
            mesh.getVertexAt(t[3*i+2], v2);

            if ( !flipNormals ) {
                gl.glNormal3d(v0.normal.x, v0.normal.y, v0.normal.z);
              }
              else {
                gl.glNormal3d(-v0.normal.x, -v0.normal.y, -v0.normal.z);
            }

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
        int t[];

        gl.glBegin(gl.GL_TRIANGLES);
        v0 = new Vertex(new Vector3D(), new Vector3D());
        v1 = new Vertex(new Vector3D(), new Vector3D());
        v2 = new Vertex(new Vector3D(), new Vector3D());
        t = mesh.getTriangleIndexes();
        for ( int i = start; i < end; i++ ) {
            mesh.getVertexAt(t[3*i], v0);
            mesh.getVertexAt(t[3*i+1], v1);
            mesh.getVertexAt(t[3*i+2], v2);

            //-----------------------------------------------------------------
            if ( !flipNormals ) {
                gl.glNormal3d(v0.normal.x, v0.normal.y, v0.normal.z);
              }
              else {
                gl.glNormal3d(-v0.normal.x, -v0.normal.y, -v0.normal.z);
            }
            gl.glTexCoord2d(v0.u, v0.v);
            gl.glVertex3d(v0.position.x, v0.position.y, v0.position.z);

            //-----------------------------------------------------------------
            if ( !flipNormals ) {
                gl.glNormal3d(v1.normal.x, v1.normal.y, v1.normal.z);
            }
            else {
                gl.glNormal3d(-v1.normal.x, -v1.normal.y, -v1.normal.z);
            }
            gl.glTexCoord2d(v1.u, v1.v);
            gl.glVertex3d(v1.position.x, v1.position.y, v1.position.z);

            //-----------------------------------------------------------------
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

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
