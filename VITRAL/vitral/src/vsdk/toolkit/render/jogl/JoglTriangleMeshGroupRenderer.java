package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import java.util.Iterator;
import java.util.ArrayList;

public class JoglTriangleMeshGroupRenderer {

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.

    @return Approximate number of triangles. If non-triangles primitives like
    quads are rendered, this counts as the corresponding number of triangles.
    1D and 0D primitives are not counted.

    @todo Implement triangle count!
    */
    public static int draw(GL gl, TriangleMeshGroup meshGroup, QualitySelection quality) {
    TriangleMesh mesh = null;
    for (Iterator<TriangleMesh> i = meshGroup.getMeshes().iterator(); i.hasNext(); ) {
        mesh = (TriangleMesh) i.next();
        if (quality.isSurfacesSet()) {
        int qt = quality.getShadingType();
        gl.glCullFace(gl.GL_FRONT);

        if (qt == quality.SHADING_TYPE_GOURAUD ||
            qt == quality.SHADING_TYPE_PHONG) {
            JoglTriangleMeshRenderer.drawSurfacesSolid(gl, mesh, false);
        }
        else {
            JoglTriangleMeshRenderer.drawSurfacesSmooth(gl, mesh, false);
        }

        gl.glCullFace(gl.GL_BACK);
        if (qt == quality.SHADING_TYPE_GOURAUD ||
            qt == quality.SHADING_TYPE_PHONG) {
            JoglTriangleMeshRenderer.drawSurfacesSolid(gl, mesh, true);
        }
        else {
            JoglTriangleMeshRenderer.drawSurfacesSmooth(gl, mesh, true);
        }

        }
        if (quality.isWiresSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor3f(1, 1, 1);
            JoglTriangleMeshRenderer.drawWires(gl, mesh);
        }
        gl.glPopAttrib();

        }
        if (quality.isBoundingVolumeSet()) {
        JoglGeometryRenderer.drawMinMaxBox(gl, mesh, quality);
        }
        if (quality.isTextureSet()) {
        JoglTriangleMeshRenderer.drawTexture(gl, mesh);
        }
        if (quality.isBumpMapSet()) {
        JoglTriangleMeshRenderer.drawBumpMap(gl, mesh);
        }
        if (quality.isPointsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor3f(1, 0, 0);

            JoglTriangleMeshRenderer.drawPoints(gl, mesh);
        }
        gl.glPopAttrib();

        }
        if (quality.isNormalsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor3f(0, 1, 0);

            JoglTriangleMeshRenderer.drawNormals(gl, mesh);
        }
        gl.glPopAttrib();

        }
        if (quality.isTrianglesNormalsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor3f(1, 1, 0);

            JoglTriangleMeshRenderer.drawNormalsTriangles(gl, mesh);
        }
        gl.glPopAttrib();
        }

        //  drawShading(gl, quality.getShading());

    }
    return 0;
    }

    public static void drawWithSelection(GL gl, TriangleMeshGroup meshGroup,
                     QualitySelection quality,
                     ArrayList<int[]> selectedTriangles) {
    TriangleMesh mesh = null;

    for (int i = 0; i < meshGroup.getMeshes().size(); i++) {
        mesh = (TriangleMesh) meshGroup.getMeshAt(i);
        boolean selected = false;
        for (int j = 0; j < selectedTriangles.size(); j++) {
        if (i == selectedTriangles.get(j)[0]) {
            selected = true;
        }
        }
        if (quality.isSurfacesSet()) {
        int qt = quality.getShadingType();
        gl.glCullFace(gl.GL_FRONT);

        if (qt == quality.SHADING_TYPE_GOURAUD ||
            qt == quality.SHADING_TYPE_PHONG) {
            JoglTriangleMeshRenderer.drawSurfacesSolid(gl, mesh, false);
        }
        else {
            JoglTriangleMeshRenderer.drawSurfacesSmooth(gl, mesh, false);
        }

        gl.glCullFace(gl.GL_BACK);
        if (qt == quality.SHADING_TYPE_GOURAUD ||
            qt == quality.SHADING_TYPE_PHONG) {
            JoglTriangleMeshRenderer.drawSurfacesSolid(gl, mesh, true);
        }
        else {
            JoglTriangleMeshRenderer.drawSurfacesSmooth(gl, mesh, true);
        }

        }
        if (quality.isWiresSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor3f(1, 1, 1);
            if (selected) {
            JoglTriangleMeshRenderer.drawWiresWithSelection(gl, mesh, selectedTriangles);
            }
            else {
            JoglTriangleMeshRenderer.drawWires(gl, mesh);
            }
        }
        gl.glPopAttrib();

        }
        if (quality.isBoundingVolumeSet()) {
        JoglGeometryRenderer.drawMinMaxBox(gl, mesh, quality);
        }
        if (quality.isTextureSet()) {
        JoglTriangleMeshRenderer.drawTexture(gl, mesh);
        }
        if (quality.isBumpMapSet()) {
        JoglTriangleMeshRenderer.drawBumpMap(gl, mesh);
        }
        if (quality.isPointsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glColor3f(1, 0, 0);

        JoglTriangleMeshRenderer.drawPoints(gl, mesh);
        gl.glPopAttrib();

        }
        if (quality.isNormalsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor3f(0, 1, 0);

            JoglTriangleMeshRenderer.drawNormals(gl, mesh);
        }
        gl.glPopAttrib();

        }
        if (quality.isTrianglesNormalsSet()) {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor3f(1, 1, 0);

            JoglTriangleMeshRenderer.drawNormalsTriangles(gl, mesh);
        }
        gl.glPopAttrib();
        }

        //  drawShading(gl, quality.getShading());

    }

    }

}
