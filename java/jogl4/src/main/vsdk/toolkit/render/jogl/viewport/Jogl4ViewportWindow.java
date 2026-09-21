//=   previous "JoglView" class at SceneEditorApplication example).         =

package vsdk.toolkit.render.jogl.viewport;

// Java basic classes
import java.util.ArrayList;
import java.util.List;

// JOGL classes
import com.jogamp.opengl.GL4;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.gui.gizmo.ReferenceFrameGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.gizmo.Jogl4ReferenceFrameGizmoRenderer;

// Framework classes
import vsdk.toolkit.gui.viewport.ViewportElementScaler;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;

/**
JOGL presentation of one `Viewport` of a `ViewportSet`: grid, reference base,
title and gizmo labels. It holds the JOGL/image resources needed for that
drawing, and reads everything else (cameras, areas, selection) from the
injected model. It does not depend on AWT/Swing: user interaction is processed
in the `application.gui` package, and label images are provided by the
injected `Jogl4LabelImageProvider`.
*/
public class Jogl4ViewportWindow
{
    // Sizes, in pixels, designed for legacy resolutions; they are enlarged
    // for bigger screens by the element scaler of the viewport set
    private static final int BASE_TITLE_FONT_SIZE = 14;
    private static final int BASE_TITLE_BORDER_X = 4;
    private static final int BASE_TITLE_BORDER_Y = 1;
    // A bit smaller than the titles
    private static final int BASE_REFERENCE_FRAME_LABEL_FONT_SIZE = 12;
    private static final int BASE_TRANSLATE_GIZMO_LABEL_FONT_SIZE = Jogl4LabelImageProvider.DEFAULT_FONT_SIZE;
    // Position of the labels of the translate gizmo, with respect to the tip
    // of its arrows
    private static final int BASE_TRANSLATE_GIZMO_LABEL_OFFSET_X = -3;
    private static final int BASE_TRANSLATE_GIZMO_LABEL_OFFSET_Y = 12;

    private final ViewportSet viewportSet;
    private final Viewport viewport;
    private final Jogl4LabelImageProvider labelImageProvider;

    private String title;
    private ColorRgb titleColor;
    private int titleFontSize;
    private RGBAImageUncompressed titleImage;
    // Label images no longer used, whose textures must be released
    private final List<RGBAImageUncompressed> discardedLabelImages =
        new ArrayList<RGBAImageUncompressed>();
    private final ReferenceFrameGizmo referenceFrameGizmo = new ReferenceFrameGizmo();
    private RGBAImageUncompressed[] referenceFrameLabelImages;
    private int referenceFrameLabelFontSize;
    // Labels of the translate gizmo, by axis, normal and selected (yellow)
    private RGBAImageUncompressed[] translateGizmoLabelImages;
    private RGBAImageUncompressed[] translateGizmoSelectedLabelImages;
    private int translateGizmoLabelFontSize;

    // Each Jogl4ViewportWindow can call a different visualization algorithm
    public static final int RENDER_MODE_ZBUFFER = Viewport.RENDER_MODE_Z_BUFFER;
    public static final int RENDER_MODE_RAYTRACING = Viewport.RENDER_MODE_RAYTRACING;

    public Jogl4ViewportWindow(ViewportSet viewportSet,
                               Viewport viewport,
                               Jogl4LabelImageProvider labelImageProvider)
    {
        this.viewportSet = viewportSet;
        this.viewport = viewport;
        this.labelImageProvider = labelImageProvider;

        updateTitleImage();
    }

    public Viewport getViewport()
    {
        return viewport;
    }

    public int getRenderMode()
    {
        return viewport.getRenderMode();
    }

    public int getViewportStartX()
    {
        return viewport.getPixelStartX();
    }

    public int getViewportStartY()
    {
        return viewport.getPixelStartY();
    }

    public int getViewportSizeX()
    {
        return viewport.getPixelSizeX();
    }

    public int getViewportSizeY()
    {
        return viewport.getPixelSizeY();
    }

    public boolean isSelected()
    {
        return viewportSet.isSelected(viewport);
    }

    public boolean isActive()
    {
        return viewport.isActive();
    }

    public Camera getCamera()
    {
        return viewport.getActiveCamera();
    }

    public RendererConfiguration getRendererConfiguration()
    {
        return viewport.getRendererConfiguration();
    }

    /**
    Draws the reference frame gizmo at the lower left corner of the viewport.
    Its size, line width and labels follow the screen resolution (see
    `ViewportElementScaler`), so it keeps a similar apparent size.
    */
    public void drawReferenceBase(GL4 gl)
    {
        ViewportElementScaler elementScaler = viewportSet.getElementScaler();

        referenceFrameGizmo.applyScale(elementScaler);
        updateReferenceFrameLabelImages(elementScaler);

        Jogl4ReferenceFrameGizmoRenderer.draw(gl, referenceFrameGizmo,
            viewport.getActiveCamera().getRotation(),
            viewport.getPixelStartX(), viewport.getPixelStartY(),
            (glContext, axis, label, windowX, windowY) ->
                drawLabelImage(glContext, referenceFrameLabelImages[axis],
                    windowX, windowY));
    }

    /**
    The label images of the reference frame depend on the screen resolution,
    so they are regenerated when the font size they need changes.
    */
    private void updateReferenceFrameLabelImages(ViewportElementScaler elementScaler)
    {
        int currentFontSize = elementScaler.scaleSize(BASE_REFERENCE_FRAME_LABEL_FONT_SIZE);

        if ( referenceFrameLabelImages != null &&
             currentFontSize == referenceFrameLabelFontSize ) {
            return;
        }
        if ( referenceFrameLabelImages != null ) {
            for ( RGBAImageUncompressed image : referenceFrameLabelImages ) {
                discardedLabelImages.add(image);
            }
        }
        referenceFrameLabelFontSize = currentFontSize;
        referenceFrameLabelImages = new RGBAImageUncompressed[ReferenceFrameGizmo.NUMBER_OF_AXES];
        for ( int axis = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++ ) {
            referenceFrameLabelImages[axis] = labelImageProvider.createLabelImage(
                referenceFrameGizmo.getAxisLabel(axis),
                referenceFrameGizmo.getAxisColor(axis),
                currentFontSize);
        }
    }

    /**
    The label images of the translate gizmo depend on the screen resolution,
    so they are regenerated when the font size they need changes.
    */
    private void updateTranslateGizmoLabelImages(ViewportElementScaler elementScaler)
    {
        int currentFontSize = elementScaler.scaleSize(BASE_TRANSLATE_GIZMO_LABEL_FONT_SIZE);

        if ( translateGizmoLabelImages != null &&
             currentFontSize == translateGizmoLabelFontSize ) {
            return;
        }
        if ( translateGizmoLabelImages != null ) {
            for ( int axis = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++ ) {
                discardedLabelImages.add(translateGizmoLabelImages[axis]);
                discardedLabelImages.add(translateGizmoSelectedLabelImages[axis]);
            }
        }
        translateGizmoLabelFontSize = currentFontSize;
        translateGizmoLabelImages = new RGBAImageUncompressed[ReferenceFrameGizmo.NUMBER_OF_AXES];
        translateGizmoSelectedLabelImages = new RGBAImageUncompressed[ReferenceFrameGizmo.NUMBER_OF_AXES];
        for ( int axis = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++ ) {
            translateGizmoLabelImages[axis] = labelImageProvider.createLabelImage(
                referenceFrameGizmo.getAxisLabel(axis),
                referenceFrameGizmo.getAxisColor(axis),
                currentFontSize);
            translateGizmoSelectedLabelImages[axis] = labelImageProvider.createLabelImage(
                referenceFrameGizmo.getAxisLabel(axis),
                new ColorRgb(1, 1, 0),
                currentFontSize);
        }
    }

    private void drawGridRectangle(GL4 gl)
    {
        Matrix4x4d R;
        double yaw, pitch;
        Matrix4x4d gridTransform = new Matrix4x4d();

        R = viewport.getActiveCamera().getRotation();
        yaw = Math.toDegrees(R.obtainEulerYawAngle());
        pitch = Math.toDegrees(R.obtainEulerPitchAngle());

        if ( viewport.getActiveCamera().getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL &&
              (pitch > -45 && pitch < 45) ) {
            if ( (yaw > 45 && yaw < 135) ||
                 (yaw < -45 && yaw > -135) ) {
                gridTransform = new Matrix4x4d().axisRotation(Math.toRadians(90), 1, 0, 0);
            }
            else {
                gridTransform = new Matrix4x4d().axisRotation(Math.toRadians(90), 0, 1, 0);
            }
        }

        //-----------------------------------------------------------------
        int nx = 14; // Must be an even number
        int ny = 14; // Must be an even number
        double dx = 1.0;
        double dy = 1.0;
        int x, y;
        double minx = -(((double)nx)/2) * dx;
        double maxx = (((double)nx)/2) * dx;
        double miny = -(((double)ny)/2) * dy;
        double maxy = (((double)ny)/2) * dy;

        ArrayList<Float> p = new ArrayList<Float>();
        ArrayList<Float> c = new ArrayList<Float>();

        for ( x = 0; x <= nx; x++ ) {
            if ( x == nx/2 ) continue;
            addGridLine(p, c, minx + ((double)x)*dx, miny, minx + ((double)x)*dx, maxy, 0.37f);
        }
        for ( y = 0; y <= ny; y++ ) {
            if ( y == ny/2 ) continue;
            addGridLine(p, c, minx, minx + ((double)y)*dy, maxx, minx + ((double)y)*dy, 0.37f);
        }
        addGridLine(p, c, minx + ((double)(nx/2))*dx, miny, minx + ((double)(nx/2))*dx, maxy, 0.0f);
        addGridLine(p, c, minx, minx + ((double)(ny/2))*dy, maxx, minx + ((double)(ny/2))*dy, 0.0f);

        float[] positions = new float[p.size()];
        float[] colors = new float[c.size()];
        for ( int i = 0; i < positions.length; i++ ) {
            positions[i] = p.get(i);
            colors[i] = c.get(i);
        }

        gl.glEnable(GL4.GL_DEPTH_TEST);
        Jogl4LineRenderer.drawLines(gl,
            viewport.getActiveCamera().calculateProjectionMatrix().multiply(gridTransform),
            positions, colors, 1.0f);
    }

    private static void addGridLine(ArrayList<Float> p, ArrayList<Float> c,
        double x0, double y0, double x1, double y1, float gray)
    {
        p.add((float)x0); p.add((float)y0); p.add(0.0f);
        p.add((float)x1); p.add((float)y1); p.add(0.0f);
        for ( int i = 0; i < 2; i++ ) {
            c.add(gray); c.add(gray); c.add(gray);
        }
    }

    public void toggleGrid()
    {
        viewport.toggleGrid();
    }

    public void drawGrid(GL4 gl)
    {
        //- Draw reference grid plane -------------------------------------
        if ( viewport.isShowGrid() ) drawGridRectangle(gl);
    }

    /**
    Draws a label image with its upper left corner at a position of the
    viewport, measured in pixels from its upper left corner.
    */
    public void drawTextureString2D(GL4 gl, int x, int y, RGBAImageUncompressed i)
    {
        drawLabelImage(gl, i,
            viewport.getPixelStartX() + x,
            viewport.getPixelStartY() + (viewport.getPixelSizeY() - y));
    }

    /**
    Draws a label image with its lower left corner at a position of the
    surface, in window coordinates.

    The image is uploaded to a texture the first time it is used and drawn as a
    textured quad in window coordinates, over the whole surface of the
    viewport set, with an explicitly configured state (alpha blending, no
    depth test). Both the color and the transparency of the label come only
    from the image. The GL viewport does not clip 2D quads drawn over the whole
    surface, so the label is clipped to the area of this viewport with the
    scissor test, and the parts that fall outside are not drawn over its
    neighbors. The state changed is restored.

    @param gl OpenGL context
    @param image label image
    @param windowX horizontal position of the lower left corner
    @param windowY vertical position of the lower left corner, from the bottom
    */
    private void drawLabelImage(GL4 gl, RGBAImageUncompressed image, double windowX, double windowY)
    {
        int[] currentViewport = new int[4];

        gl.glGetIntegerv(GL4.GL_VIEWPORT, currentViewport, 0);

        int surfaceWidth = viewportSet.getSizeXInPixels();
        int surfaceHeight = viewportSet.getSizeYInPixels();
        if ( surfaceWidth <= 0 || surfaceHeight <= 0 ) {
            surfaceWidth = currentViewport[0] + currentViewport[2];
            surfaceHeight = currentViewport[1] + currentViewport[3];
        }

        releaseDiscardedLabelTextures(gl);
        int texture = Jogl4ImageRenderer.activate(gl, image);
        double x0 = Math.round(windowX);
        double y0 = Math.round(windowY);
        double x1 = x0 + image.getXSize();
        double y1 = y0 + image.getYSize();

        // Window coordinates to clip space
        Matrix4x4d toClip = Matrix4x4d.identityMatrix()
            .withVal(0, 0, 2.0 / surfaceWidth).withVal(0, 3, -1.0)
            .withVal(1, 1, 2.0 / surfaceHeight).withVal(1, 3, -1.0);
        float[] positions = new float[] {
            (float)x0, (float)y0, 0,
            (float)x1, (float)y0, 0,
            (float)x1, (float)y1, 0,
            (float)x0, (float)y0, 0,
            (float)x1, (float)y1, 0,
            (float)x0, (float)y1, 0
        };
        float[] uvs = new float[] {
            0, 0,  1, 0,  1, 1,
            0, 0,  1, 1,  0, 1
        };

        boolean scissorWasEnabled = gl.glIsEnabled(GL4.GL_SCISSOR_TEST);
        int[] previousScissor = new int[4];

        gl.glGetIntegerv(GL4.GL_SCISSOR_BOX, previousScissor, 0);

        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);
        gl.glEnable(GL4.GL_SCISSOR_TEST);
        gl.glScissor(viewport.getPixelStartX(), viewport.getPixelStartY(),
            viewport.getPixelSizeX(), viewport.getPixelSizeY());
        gl.glDisable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);

        Jogl4ImageRenderer.drawTexturedQuad(gl, texture, toClip, positions, uvs, 1.0f, 1.0f, 1.0f);

        gl.glDisable(GL4.GL_BLEND);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glScissor(previousScissor[0], previousScissor[1], previousScissor[2], previousScissor[3]);
        if ( !scissorWasEnabled ) {
            gl.glDisable(GL4.GL_SCISSOR_TEST);
        }
        gl.glViewport(currentViewport[0], currentViewport[1], currentViewport[2], currentViewport[3]);
    }

    /**
    Releases the textures of the label images that are no longer used.
    */
    private void releaseDiscardedLabelTextures(GL4 gl)
    {
        for ( RGBAImageUncompressed discarded : discardedLabelImages ) {
            Jogl4ImageRenderer.unload(gl, discarded);
        }
        discardedLabelImages.clear();
    }

    /**
    Deletes the OpenGL resources (label textures) of this window.
    PRE: the OpenGL context that created them is current, i.e. when it is
    about to be destroyed.
    @param gl OpenGL context
    */
    public void disposeGlResources(GL4 gl)
    {
        releaseDiscardedLabelTextures(gl);
        if ( titleImage != null ) {
            Jogl4ImageRenderer.unload(gl, titleImage);
        }
        if ( referenceFrameLabelImages != null ) {
            for ( RGBAImageUncompressed image : referenceFrameLabelImages ) {
                Jogl4ImageRenderer.unload(gl, image);
            }
        }
        if ( translateGizmoLabelImages != null ) {
            for ( int axis = 0; axis < translateGizmoLabelImages.length; axis++ ) {
                Jogl4ImageRenderer.unload(gl, translateGizmoLabelImages[axis]);
                Jogl4ImageRenderer.unload(gl, translateGizmoSelectedLabelImages[axis]);
            }
        }
    }

    /**
    Forgets the pending release of label textures, because the context that
    owned them is gone. The label images themselves are kept, and their
    textures are created again when needed.
    */
    public void invalidateGlResources()
    {
        discardedLabelImages.clear();
    }

    public void drawTitle(GL4 gl)
    {
        updateTitleImage();

        ViewportElementScaler elementScaler = viewportSet.getElementScaler();
        int borderx = elementScaler.scaleSize(BASE_TITLE_BORDER_X);
        int bordery = elementScaler.scaleSize(BASE_TITLE_BORDER_Y);

        // The area of the title (its border included, so it can be easily
        // pointed) is informed to the model, for interaction
        viewport.setTitleArea(0, 0,
            titleImage.getXSize() + 2*borderx, titleImage.getYSize() + 2*bordery);

        drawTextureString2D(gl, borderx, titleImage.getYSize() + bordery, titleImage);
    }

    /**
    The title is given by the viewport set (it follows the active camera and
    the language selected by the user), its color is configured in the viewport
    set (it depends on whether the viewport is selected) and its size depends
    on the screen resolution (see `ViewportElementScaler`), so the image is
    regenerated whenever any of them changes.
    */
    private void updateTitleImage()
    {
        String currentTitle = viewportSet.getTitleFor(viewport);
        ColorRgb currentColor = viewportSet.getTitleColorFor(viewport);
        int currentFontSize = viewportSet.getElementScaler().scaleSize(BASE_TITLE_FONT_SIZE);

        if ( titleImage == null || !currentTitle.equals(title) ||
             !currentColor.equals(titleColor) ||
             currentFontSize != titleFontSize ) {
            title = currentTitle;
            titleColor = currentColor;
            titleFontSize = currentFontSize;
            if ( titleImage != null ) {
                discardedLabelImages.add(titleImage);
            }
            titleImage = labelImageProvider.createLabelImage(title, titleColor, titleFontSize);
        }
    }

    public void drawLabelsForTranslateGizmo(GL4 gl, TranslateGizmo gizmo)
    {
        ViewportElementScaler elementScaler = viewportSet.getElementScaler();
        int offsetX = (int)Math.round(elementScaler.scaleLength(BASE_TRANSLATE_GIZMO_LABEL_OFFSET_X));
        int offsetY = (int)Math.round(elementScaler.scaleLength(BASE_TRANSLATE_GIZMO_LABEL_OFFSET_Y));

        updateTranslateGizmoLabelImages(elementScaler);

        ArrayList<SimpleBody> things = gizmo.getElements();
        int i;
        Vector3Dd lv = new Vector3Dd();
        Vector3Dd p;
        Vector3Dd tp;
        Matrix4x4d R;
        boolean yellow;
        ColorRgb c = new ColorRgb(1, 1, 0);

        for ( i = 0; i < things.size() && i < 3; i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3Dd position;

            if ( g != null ) {
                lv = new Vector3Dd(0, 0, lv.z());
                if ( g instanceof Arrow ) {
                    Arrow a = ((Arrow)g);
                    lv = lv.withZ((a.getHeadLength() + a.getBaseLength()) * 1.1);
                }
                else {
                    lv = lv.withZ(1);
                }

                R = new Matrix4x4d();
                R = R.translation(r.getPosition());
                R = R.multiply(r.getRotation());
                p = R.multiply(lv);
                tp = viewport.getActiveCamera().projectPointUsingRayMethod(p);

                if ( tp != null ) {
                    yellow = ColorRgb.distance(c, r.getMaterial().getDiffuse()) <
                        VSDK.EPSILON;

                    drawTextureString2D(gl,
                        (int)tp.x() + offsetX,
                        (int)tp.y() + offsetY,
                        yellow ? translateGizmoSelectedLabelImages[i] : translateGizmoLabelImages[i]);
                }
            }
        }
    }
}
