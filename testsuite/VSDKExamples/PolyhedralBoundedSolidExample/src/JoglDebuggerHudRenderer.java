// Java Awt classes
import java.awt.Font;
import models.DebuggerModel;
import java.awt.geom.Rectangle2D;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.awt.TextRenderer;
import models.CsgSampleNames;
import models.SolidModelNames;
import vsdk.toolkit.common.PolyhedralBoundedSolidStatistics;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import com.jogamp.opengl.glu.GLU;

public class JoglDebuggerHudRenderer
{
    private static final int LINE_HEIGHT = 34;

    private final DebuggerModel model;
    private TextRenderer hudTextRenderer;
    private TextRenderer vertexLabelRenderer;
    private int viewportWidth;
    private int viewportHeight;
    public JoglDebuggerHudRenderer(DebuggerModel model)
    {
        this.model = model;
        this.hudTextRenderer = null;
        this.vertexLabelRenderer = null;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        hudTextRenderer = new TextRenderer(
            new Font("SansSerif", Font.BOLD, 18), true, true);
        vertexLabelRenderer = new TextRenderer(
            new Font("SansSerif", Font.PLAIN, 12), true, true);
        updateViewportSize(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    }

    public void updateViewportSize(int width, int height)
    {
        viewportWidth = width;
        viewportHeight = height;
    }

    public void draw(GLAutoDrawable drawable)
    {
        if ( hudTextRenderer == null || vertexLabelRenderer == null ) {
            return;
        }

        int width = viewportWidth > 0 ? viewportWidth : drawable.getSurfaceWidth();
        int height = viewportHeight > 0 ? viewportHeight : drawable.getSurfaceHeight();
        String showingFaceLoopMessage = "Face [1, 2]: " + formatFaceLoopLabel();
        String selectedModelMessage = "Selected model [3, 4]: "
            + model.getSolidModelName().name()
            + " (" + model.getSolidModelName().getDisplayIndex()
            + "/" + SolidModelNames.getTotalModels() + ")";
        String csgSampleMessage = "CSG sample [6]: " + model.getCsgSample().getLabel()
            + " (" + model.getCsgSample().getDisplayIndex()
            + "/" + CsgSampleNames.getTotalSamples() + ")";
        String csgOperationMessage = "CSG op [5]: " + model.getCsgOperation().getLabel();
        String referenceFrameMessage = "Reference frame [Space]: " +
            (model.isShowCoordinateSystem() ? "ON" : "OFF");
        String nrMessage = "NR [q, Q]: " + model.getSubdivisionCircumference();
        String nhMessage = "NH [w, W]: " + model.getSubdivisionHeight();

        hudTextRenderer.beginRendering(width, height);
        hudTextRenderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);
        hudTextRenderer.draw(showingFaceLoopMessage, 16, height - 28);
        hudTextRenderer.draw(selectedModelMessage, 16, height - (28 + LINE_HEIGHT));
        int nextLeftLine = 2;
        if ( model.getSolidModelName().usesCsgDebugControls() ) {
            hudTextRenderer.draw(csgSampleMessage, 16,
                height - (28 + nextLeftLine*LINE_HEIGHT));
            nextLeftLine++;
            hudTextRenderer.draw(csgOperationMessage, 16,
                height - (28 + nextLeftLine*LINE_HEIGHT));
            nextLeftLine++;
        }
        hudTextRenderer.draw(referenceFrameMessage, 16,
            height - (28 + nextLeftLine*LINE_HEIGHT));
        drawTopRight(hudTextRenderer, width, height, nrMessage, 28);
        drawTopRight(hudTextRenderer, width, height, nhMessage,
            28 + LINE_HEIGHT);
        if ( model.isErrorState() ) {
            hudTextRenderer.setColor(1.0f, 0.1f, 0.1f, 1.0f);
            hudTextRenderer.draw(model.getErrorMessage(), 16, 16);
        }
        drawCsgStatisticsSummary(height);
        hudTextRenderer.endRendering();
        drawDebugVertexLabels(drawable, width, height);
    }

    private void drawCsgStatisticsSummary(int height)
    {
        if ( !model.getSolidModelName().usesCsgDebugControls() ) {
            return;
        }
        if ( !PolyhedralBoundedSolidStatistics.isEnabled() ) {
            return;
        }
        if ( PolyhedralBoundedSolidStatistics.getSetOpCalls() <= 0 ) {
            return;
        }

        long failures = PolyhedralBoundedSolidStatistics.getOperationFailureCases();
        long warnings = PolyhedralBoundedSolidStatistics.getConsistencyWarningCases();
        long he1eqhe2 = PolyhedralBoundedSolidStatistics.getHe1EqualsHe2Cases();
        long invalidInputs =
            PolyhedralBoundedSolidStatistics.getInvalidHalfEdgeInputCases();
        long joinIncomplete =
            PolyhedralBoundedSolidStatistics.getJoinIncompleteCases();

        long issueTotal = failures + warnings + he1eqhe2 +
            invalidInputs + joinIncomplete;
        if ( issueTotal <= 0 ) {
            return;
        }

        int lineGap = 22;
        int startY = model.isErrorState() ? (16 + (3 * lineGap)) : 16;

        hudTextRenderer.setColor(1.0f, 0.1f, 0.1f, 1.0f);
        hudTextRenderer.draw("CSG stats issues:", 16, startY);
        hudTextRenderer.draw(
            "fail=" + failures + " warn=" + warnings +
            " he1==he2=" + he1eqhe2,
            16, startY + lineGap);
        hudTextRenderer.draw(
            "joinIncomplete=" + joinIncomplete +
            " invalidHE=" + invalidInputs,
            16, startY + (2 * lineGap));
    }

    private String formatFaceLoopLabel()
    {
        if ( model.getFaceIndex() == -2 ) {
            return "NONE";
        }
        if ( model.getFaceIndex() == -1 ) {
            return "ALL";
        }

        int currentFace = model.getFaceIndex() + 1;
        int totalFaces = 0;
        if ( model.getSolid() != null && model.getSolid().polygonsList != null ) {
            totalFaces = model.getSolid().polygonsList.size();
        }
        return "[" + currentFace + "/" + totalFaces + "]";
    }

    public void dispose(GLAutoDrawable drawable)
    {
        if ( hudTextRenderer != null ) {
            hudTextRenderer.dispose();
            hudTextRenderer = null;
        }
        if ( vertexLabelRenderer != null ) {
            vertexLabelRenderer.dispose();
            vertexLabelRenderer = null;
        }
    }

    private static void drawTopRight(TextRenderer renderer, int width,
        int height, String text, int offsetFromTop)
    {
        Rectangle2D textBounds = renderer.getBounds(text);
        int x = width - 16 - (int)Math.ceil(textBounds.getWidth());
        int y = height - (int)Math.round((double)offsetFromTop);
        renderer.draw(text, x, y);
    }

    private void drawDebugVertexLabels(GLAutoDrawable drawable, int width, int height)
    {
        PolyhedralBoundedSolid solid = model.getSolid();
        if ( !model.isDebugVertices() || solid == null || solid.verticesList == null ) {
            return;
        }

        GL2 gl = drawable.getGL().getGL2();
        double[] modelview = new double[16];
        double[] projection = new double[16];
        int[] viewport = new int[4];

        gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, modelview, 0);
        gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, projection, 0);
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);

        vertexLabelRenderer.beginRendering(width, height);
        vertexLabelRenderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);

        for ( int i = 0; i < solid.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = solid.verticesList.get(i);
            Vector3D projectedPosition = projectVertexToViewport(
                vertex.position, modelview, projection, viewport);
            if ( projectedPosition == null ) {
                continue;
            }
            vertexLabelRenderer.draw(Integer.toString(vertex.id),
                (int)Math.round(projectedPosition.x()) + 4,
                (int)Math.round(projectedPosition.y()) + 4);
        }
        vertexLabelRenderer.endRendering();
    }

    private static Vector3D projectVertexToViewport(
        Vector3D worldPosition,
        double[] modelview,
        double[] projection,
        int[] viewport)
    {
        double[] projected = new double[4];
        if ( !(new GLU()).gluProject(worldPosition.x(), worldPosition.y(),
                worldPosition.z(), modelview, 0, projection, 0,
                viewport, 0, projected, 0) ) {
            return null;
        }
        if ( projected[2] < 0.0 || projected[2] > 1.0 ) {
            return null;
        }
        return new Vector3D(projected[0], projected[1], projected[2]);
    }
}
