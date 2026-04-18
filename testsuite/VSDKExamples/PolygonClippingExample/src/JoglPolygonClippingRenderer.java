import java.io.File;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.Polygon2D;
import vsdk.toolkit.environment.geometry._Polygon2DContour;
import vsdk.toolkit.processing._DoubleLinkedListNode;
import vsdk.toolkit.processing._Polygon2DContourWA;
import vsdk.toolkit.processing._Polygon2DWA;
import vsdk.toolkit.processing._VertexNode2D;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.render.jogl._JoglPolygonTesselatorRoutines;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImage;

public class JoglPolygonClippingRenderer implements GLEventListener
{
    private final PolygonClippingDebuggerModel model;
    private final JoglPolygonClippingHudRenderer hudRenderer;
    private final GLU glu;
    private _JoglPolygonTesselatorRoutines tesselatorProcessor;

    public JoglPolygonClippingRenderer(PolygonClippingDebuggerModel model)
    {
        this.model = model;
        hudRenderer = new JoglPolygonClippingHudRenderer(model);
        glu = new GLU();
        tesselatorProcessor = null;
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        hudRenderer.init(drawable);
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        hudRenderer.dispose(drawable);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(0.93f, 0.95f, 0.98f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, model.camera);
        drawObjects(gl);
        hudRenderer.draw(drawable);

        if ( model.takeSnapshot ) {
            model.takeSnapshot = false;
            RGBImage snapshot = JoglRGBImageRenderer.getImageJOGL(gl);
            File output = new File("frame"
                + VSDK.formatNumberWithinZeroes(model.snapshotNumber, 4)
                + ".png");
            ImagePersistence.exportPNG(output, snapshot);
            model.snapshotNumber++;
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
        int height)
    {
        drawable.getGL().getGL2().glViewport(0, 0, width, height);
        model.camera.updateViewportResize(width, height);
        hudRenderer.updateViewportSize(width, height);
    }

    public void refreshCanvasAfterWindowModeChange()
    {
        if ( model.canvas != null ) {
            model.canvas.repaint();
        }
    }

    private void drawObjects(GL2 gl)
    {
        gl.glLoadIdentity();
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        if ( model.showReferenceFrame ) {
            drawReferenceFrame(gl);
        }

        Bounds2D bounds = calculateBounds();
        double panelWidth = Math.max(6.0, bounds.width());
        double panelDepth = Math.max(6.0, bounds.height());

        if ( model.showClipPolygon ) {
            drawPolygonWA(gl, model.clipPolygonWA, 0.20, 0.75, 0.25, 0.70, 0.20);
        }
        if ( model.showSubjectPolygon ) {
            drawPolygonWA(gl, model.subjectPolygonWA, 0.80, 0.74, 0.20, 0.82, 0.56);
        }

        gl.glPushMatrix();
        gl.glTranslated(0.0, 0.0, -panelDepth * 1.25);
        if ( model.showInnerPolygon ) {
            drawResultPolygon(gl, model.innerPolygon, 0.65, 0.65, 0.70,
                0.82, 0.58, 0.36);
        }
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslated(panelWidth * 1.25, 0.0, 0.0);
        if ( model.showOuterPolygon ) {
            drawResultPolygon(gl, model.outerPolygon, 0.68, 0.78, 0.68,
                0.18, 0.72, 0.24);
        }
        gl.glPopMatrix();
    }

    private void drawReferenceFrame(GL2 gl)
    {
        gl.glLineWidth(3.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3d(1, 0, 0);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3d(2, 0, 0);
        gl.glColor3d(0, 1, 0);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3d(0, 2, 0);
        gl.glColor3d(0, 0, 1);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3d(0, 0, 2);
        gl.glEnd();
    }

    private void drawPolygonWA(GL2 gl, _Polygon2DWA polygon, double lineR,
        double lineG, double lineB, double pointR, double pointG)
    {
        int i;

        if ( polygon == null || polygon.loops == null ) {
            return;
        }

        gl.glLineWidth(2.0f);
        gl.glColor3d(lineR, lineG, lineB);
        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContourWA contour = polygon.loops.get(i);
            if ( contour.vertices == null || contour.vertices.getHead() == null ) {
                continue;
            }
            _DoubleLinkedListNode<_VertexNode2D> head = contour.vertices.getHead();
            _DoubleLinkedListNode<_VertexNode2D> cursor = head;
            gl.glBegin(GL2.GL_LINE_LOOP);
            do {
                gl.glVertex3d(cursor.data.x, 0.0, cursor.data.y);
                cursor = cursor.next;
            } while ( cursor != head );
            gl.glEnd();
        }

        if ( !model.showIntersections ) {
            return;
        }

        gl.glPointSize(8.0f);
        gl.glBegin(GL2.GL_POINTS);
        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContourWA contour = polygon.loops.get(i);
            if ( contour.vertices == null || contour.vertices.getHead() == null ) {
                continue;
            }
            _DoubleLinkedListNode<_VertexNode2D> head = contour.vertices.getHead();
            _DoubleLinkedListNode<_VertexNode2D> cursor = head;
            do {
                if ( cursor.data.pairNode == null ) {
                    gl.glColor3d(pointR, pointG, 0.45);
                }
                else {
                    gl.glColor3d(0.15, 0.85, 0.25);
                }
                gl.glVertex3d(cursor.data.x, 0.0, cursor.data.y);
                cursor = cursor.next;
            } while ( cursor != head );
        }
        gl.glEnd();
    }

    private void drawResultPolygon(GL2 gl, Polygon2D polygon, double fillR,
        double fillG, double fillB, double lineR, double lineG, double lineB)
    {
        int i;
        int j;

        if ( polygon == null || polygon.loops == null || polygon.loops.isEmpty() ) {
            return;
        }

        if ( model.showFilledPolygons ) {
            gl.glColor4d(fillR, fillG, fillB, 1.0);
            drawWithTesselator(gl, polygon);
        }

        gl.glLineWidth(2.0f);
        gl.glColor3d(lineR, lineG, lineB);
        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            gl.glBegin(GL2.GL_LINE_LOOP);
            for ( j = 0; j < contour.vertices.size(); j++ ) {
                gl.glVertex3d(contour.vertices.get(j).x, 0.0,
                    contour.vertices.get(j).y);
            }
            gl.glEnd();
        }
    }

    private void drawWithTesselator(GL2 gl, Polygon2D polygon)
    {
        GLUtessellator tesselator;
        double[][] list;
        int i;
        int j;

        if ( tesselatorProcessor == null ) {
            tesselatorProcessor = new _JoglPolygonTesselatorRoutines(gl, glu);
        }

        tesselator = GLU.gluNewTess();
        GLU.gluTessCallback(tesselator, GLU.GLU_TESS_VERTEX,
            tesselatorProcessor);
        GLU.gluTessCallback(tesselator, GLU.GLU_TESS_BEGIN,
            tesselatorProcessor);
        GLU.gluTessCallback(tesselator, GLU.GLU_TESS_END,
            tesselatorProcessor);
        GLU.gluTessCallback(tesselator, GLU.GLU_TESS_ERROR,
            tesselatorProcessor);

        GLU.gluTessBeginPolygon(tesselator, null);
        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            GLU.gluTessBeginContour(tesselator);
            list = new double[contour.vertices.size()][3];
            for ( j = 0; j < contour.vertices.size(); j++ ) {
                list[j][0] = contour.vertices.get(j).x;
                list[j][1] = 0.0;
                list[j][2] = contour.vertices.get(j).y;
                GLU.gluTessVertex(tesselator, list[j], 0, list[j]);
            }
            GLU.gluTessEndContour(tesselator);
        }
        GLU.gluTessEndPolygon(tesselator);
        GLU.gluDeleteTess(tesselator);
    }

    private Bounds2D calculateBounds()
    {
        Bounds2D bounds = new Bounds2D();

        expandBounds(bounds, model.clipPolygonWA);
        expandBounds(bounds, model.subjectPolygonWA);
        expandBounds(bounds, model.innerPolygon);
        expandBounds(bounds, model.outerPolygon);

        if ( !bounds.initialized ) {
            bounds.include(0.0, 0.0);
            bounds.include(8.0, 8.0);
        }
        return bounds;
    }

    private void expandBounds(Bounds2D bounds, _Polygon2DWA polygon)
    {
        int i;
        int guard;

        if ( polygon == null || polygon.loops == null ) {
            return;
        }

        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContourWA contour = polygon.loops.get(i);
            if ( contour.vertices == null || contour.vertices.getHead() == null ) {
                continue;
            }
            _DoubleLinkedListNode<_VertexNode2D> head = contour.vertices.getHead();
            _DoubleLinkedListNode<_VertexNode2D> cursor = head;
            guard = 0;
            do {
                bounds.include(cursor.data.x, cursor.data.y);
                cursor = cursor.next;
                guard++;
            } while ( cursor != head && guard <= contour.vertices.size() + 1 );
        }
    }

    private void expandBounds(Bounds2D bounds, Polygon2D polygon)
    {
        int i;
        int j;

        if ( polygon == null || polygon.loops == null ) {
            return;
        }

        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            for ( j = 0; j < contour.vertices.size(); j++ ) {
                bounds.include(contour.vertices.get(j).x,
                    contour.vertices.get(j).y);
            }
        }
    }

    private static final class Bounds2D
    {
        private boolean initialized;
        private double minX;
        private double maxX;
        private double minY;
        private double maxY;

        private Bounds2D()
        {
            initialized = false;
            minX = 0.0;
            maxX = 0.0;
            minY = 0.0;
            maxY = 0.0;
        }

        private void include(double x, double y)
        {
            if ( !initialized ) {
                initialized = true;
                minX = maxX = x;
                minY = maxY = y;
                return;
            }
            if ( x < minX ) {
                minX = x;
            }
            if ( x > maxX ) {
                maxX = x;
            }
            if ( y < minY ) {
                minY = y;
            }
            if ( y > maxY ) {
                maxY = y;
            }
        }

        private double width()
        {
            return maxX - minX;
        }

        private double height()
        {
            return maxY - minY;
        }
    }
}
