//=   previous "JoglView" class at SceneEditorApplication example).         =

package framework.render.jogl4;

// Java basic classes
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

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
import vsdk.toolkit.render.jogl.Jogl2MatrixRenderer;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;

// Framework classes
import framework.model.TextScalerForScreen;
import framework.model.Viewport;
import framework.model.ViewportSet;

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
    // for bigger screens by the text scaler of the viewport set
    private static final int BASE_TITLE_FONT_SIZE = 14;
    private static final int BASE_TITLE_BORDER_X = 4;
    private static final int BASE_TITLE_BORDER_Y = 1;

    private final ViewportSet viewportSet;
    private final Viewport viewport;
    private final Jogl4LabelImageProvider labelImageProvider;

    private String title;
    private ColorRgb titleColor;
    private int titleFontSize;
    private RGBAImageUncompressed titleImage;
    private final Map<RGBAImageUncompressed, Integer> labelTextures =
        new IdentityHashMap<RGBAImageUncompressed, Integer>();
    private final List<RGBAImageUncompressed> discardedLabelImages =
        new ArrayList<RGBAImageUncompressed>();
    // OpenGL context owning the textures in `labelTextures`
    private GLContext labelTexturesContext;
    private final RGBAImageUncompressed xLabelImage;
    private final RGBAImageUncompressed yLabelImage;
    private final RGBAImageUncompressed zLabelImage;
    private final RGBAImageUncompressed xLabelImageSelected;
    private final RGBAImageUncompressed yLabelImageSelected;
    private final RGBAImageUncompressed zLabelImageSelected;

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

        xLabelImage = labelImageProvider.createLabelImage("X", new ColorRgb(0.78, 0, 0));
        yLabelImage = labelImageProvider.createLabelImage("Y", new ColorRgb(0, 0.61, 0));
        zLabelImage = labelImageProvider.createLabelImage("Z", new ColorRgb(0, 0, 0.76));

        xLabelImageSelected = labelImageProvider.createLabelImage("X", new ColorRgb(1, 1, 0));
        yLabelImageSelected = labelImageProvider.createLabelImage("Y", new ColorRgb(1, 1, 0));
        zLabelImageSelected = labelImageProvider.createLabelImage("Z", new ColorRgb(1, 1, 0));

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

    public void drawReferenceBase(GL2 gl)
    {
        //-----------------------------------------------------------------
        int basesize = 64;
        gl.glPushAttrib(GL2.GL_VIEWPORT_BIT);
        gl.glPushAttrib(GL2.GL_DEPTH_TEST);
        gl.glPushAttrib(GL2.GL_TEXTURE_2D);
        gl.glPushAttrib(GL2.GL_LIGHTING);
        gl.glViewport(viewport.getPixelStartX(), viewport.getPixelStartY(), basesize, basesize);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_DEPTH_TEST);

        //-----------------------------------------------------------------
        Matrix4x4d R = viewport.getActiveCamera().getRotation();

        gl.glLoadIdentity();
        R = R.invert();
        gl.glRotated(90, -1, 0, 0);
        gl.glRotated(90, 0, 0, 1);
        Jogl2MatrixRenderer.activate(gl, R);

        gl.glPushMatrix();
        gl.glTranslated(2, 1, 0);
        drawTextureString3D(gl, xLabelImage);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslated(1, 2, 0);
        drawTextureString3D(gl, yLabelImage);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslated(1, 1, 1);
        drawTextureString3D(gl, zLabelImage);
        gl.glPopMatrix();

        //gl.glLoadIdentity();
        gl.glBegin(GL2.GL_LINES);
            gl.glColor3d(0.78, 0, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(1, 0, 0);
            gl.glColor3d(0, 0.61, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 1, 0);
            gl.glColor3d(0, 0, 0.76);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 0, 1);
        gl.glEnd();

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
    }

    private void drawGridRectangle(GL2 gl)
    {
        gl.glPushMatrix();

        Matrix4x4d R;
        double yaw, pitch;

        R = viewport.getActiveCamera().getRotation();
        yaw = Math.toDegrees(R.obtainEulerYawAngle());
        pitch = Math.toDegrees(R.obtainEulerPitchAngle());

        if ( viewport.getActiveCamera().getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL &&
              (pitch > -45 && pitch < 45) ) {
            if ( (yaw > 45 && yaw < 135) ||
                 (yaw < -45 && yaw > -135) ) {
                gl.glRotated(90, 1, 0, 0);
            }
            else {
                gl.glRotated(90, 0, 1, 0);
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

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3d(0.37, 0.37, 0.37);
        for ( x = 0; x <= nx; x++ ) {
            if ( x == nx/2 ) continue;
            gl.glVertex3d(minx + ((double)x)*dx, miny, 0);
            gl.glVertex3d(minx + ((double)x)*dx, maxy, 0);
        }
        for ( y = 0; y <= ny; y++ ) {
            if ( y == ny/2 ) continue;
            gl.glVertex3d(minx, minx + ((double)y)*dy, 0);
            gl.glVertex3d(maxx, minx + ((double)y)*dy, 0);
        }
        gl.glColor3d(0, 0, 0);
        gl.glVertex3d(minx + ((double)(nx/2))*dx, miny, 0);
        gl.glVertex3d(minx + ((double)(nx/2))*dx, maxy, 0);
        gl.glVertex3d(minx, minx + ((double)(ny/2))*dy, 0);
        gl.glVertex3d(maxx, minx + ((double)(ny/2))*dy, 0);

        gl.glEnd();
        gl.glPopMatrix();
    }

    public void toggleGrid()
    {
        viewport.toggleGrid();
    }

    public void drawGrid(GL2 gl)
    {
        //- Draw reference grid plane -------------------------------------
        if ( viewport.isShowGrid() ) drawGridRectangle(gl);
    }

    public void drawTextureString2D(GL2 gl, int x, int y, RGBAImageUncompressed i)
    {
        double dx;
        double dy;

        dx = ((double)(2*x)) / ((double)viewport.getPixelSizeX());
        dy = ((double)(2*(viewport.getPixelSizeY() - y))) / ((double)viewport.getPixelSizeY());

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslated(dx, dy, 0);

        drawTextureString3D(gl, i);

        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    /**
    Draws a label image at the current modelview origin. The image is drawn
    with `glDrawPixels`, so its colors come only from the image data: texturing
    is explicitly disabled, otherwise the fragments would be modified by
    whatever texture (and texture environment) was left bound by previous
    drawing, making the label color depend on the OpenGL state.
    */
    /**
    Draws a label image with its lower left corner at the projection of the
    point (-1, -1, 0) of the current modelview / projection matrices.

    The image is uploaded to a texture owned by this window and drawn as a
    textured quad in window coordinates, over the whole surface of the
    viewport set (labels are not clipped by the current GL viewport, as they
    were not when drawn with `glDrawPixels`), with an explicitly configured state
    (replace texture environment, alpha blending, no lighting nor depth test).
    Both the color and the transparency of the label come only from the image,
    so the result does not depend on whatever OpenGL state was left by
    previous drawing, and all OpenGL state changed here is restored.
    */
    private void drawTextureString3D(GL2 gl, RGBAImageUncompressed i)
    {
        float[] rasterPosition = new float[4];
        int[] currentViewport = new int[4];

        gl.glRasterPos3d(-1, -1, 0);
        gl.glGetFloatv(GL2.GL_CURRENT_RASTER_POSITION, rasterPosition, 0);
        gl.glGetIntegerv(GL2.GL_VIEWPORT, currentViewport, 0);

        int surfaceWidth = viewportSet.getSizeXInPixels();
        int surfaceHeight = viewportSet.getSizeYInPixels();
        if ( surfaceWidth <= 0 || surfaceHeight <= 0 ) {
            surfaceWidth = currentViewport[0] + currentViewport[2];
            surfaceHeight = currentViewport[1] + currentViewport[3];
        }

        int texture = obtainLabelTexture(gl, i);
        double x0 = Math.round(rasterPosition[0]);
        double y0 = Math.round(rasterPosition[1]);
        double x1 = x0 + i.getXSize();
        double y1 = y0 + i.getYSize();

        gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_COLOR_BUFFER_BIT |
            GL2.GL_TEXTURE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_VIEWPORT_BIT);
        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(0, surfaceWidth, 0, surfaceHeight, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_ALPHA_TEST);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);

        gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2d(0, 0);
            gl.glVertex2d(x0, y0);
            gl.glTexCoord2d(1, 0);
            gl.glVertex2d(x1, y0);
            gl.glTexCoord2d(1, 1);
            gl.glVertex2d(x1, y1);
            gl.glTexCoord2d(0, 1);
            gl.glVertex2d(x0, y1);
        gl.glEnd();

        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopAttrib();
    }

    /**
    @return the texture holding the given label image, uploading it the first
    time it is used; also releases the textures of discarded label images
    */
    private int obtainLabelTexture(GL2 gl, RGBAImageUncompressed image)
    {
        int[] id = new int[1];

        // Textures belong to the OpenGL context that created them: if it is
        // not the current one (i.e. the GUI was rebuilt, destroying the
        // canvas), their ids are meaningless and the images must be uploaded
        // again
        if ( labelTexturesContext != gl.getContext() ) {
            invalidateGlResources();
            labelTexturesContext = gl.getContext();
        }

        for ( RGBAImageUncompressed discarded : discardedLabelImages ) {
            Integer oldTexture = labelTextures.remove(discarded);
            if ( oldTexture != null ) {
                id[0] = oldTexture;
                gl.glDeleteTextures(1, id, 0);
            }
        }
        discardedLabelImages.clear();

        Integer texture = labelTextures.get(image);
        if ( texture != null ) {
            return texture;
        }

        gl.glGenTextures(1, id, 0);
        gl.glPushAttrib(GL2.GL_TEXTURE_BIT);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, id[0]);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA8,
            image.getXSize(), image.getYSize(), 0,
            GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, image.getRawImageDirectBuffer());
        gl.glPopAttrib();

        labelTextures.put(image, id[0]);
        return id[0];
    }

    /**
    Deletes the OpenGL resources (label textures) of this window.
    PRE: the OpenGL context that created them is current, i.e. when it is
    about to be destroyed.
    @param gl
    */
    public void disposeGlResources(GL2 gl)
    {
        int[] id = new int[1];

        if ( labelTexturesContext == gl.getContext() ) {
            for ( Integer texture : labelTextures.values() ) {
                id[0] = texture;
                gl.glDeleteTextures(1, id, 0);
            }
        }
        invalidateGlResources();
    }

    /**
    Forgets the OpenGL resources (label textures) of this window without
    releasing them, because the context that owned them is already gone (or
    is not the current one). They are created again when needed. The label
    images themselves are kept.
    */
    public void invalidateGlResources()
    {
        labelTextures.clear();
        discardedLabelImages.clear();
        labelTexturesContext = null;
    }

    public void drawTitle(GL2 gl)
    {
        updateTitleImage();

        TextScalerForScreen textScaler = viewportSet.getTextScaler();
        int borderx = textScaler.scaleSize(BASE_TITLE_BORDER_X);
        int bordery = textScaler.scaleSize(BASE_TITLE_BORDER_Y);

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
    on the screen resolution (see `TextScalerForScreen`), so the image is
    regenerated whenever any of them changes.
    */
    private void updateTitleImage()
    {
        String currentTitle = viewportSet.getTitleFor(viewport);
        ColorRgb currentColor = viewportSet.getTitleColorFor(viewport);
        int currentFontSize = viewportSet.getTextScaler().scaleSize(BASE_TITLE_FONT_SIZE);

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

    public void drawLabelsForTranslateGizmo(GL2 gl, TranslateGizmo gizmo)
    {
        ArrayList<SimpleBody> things = gizmo.getElements();
        int i;
        Vector3Dd lv = new Vector3Dd();
        Vector3Dd p;
        Vector3Dd tp = new Vector3Dd();
        Matrix4x4d R;
        boolean yellow;
        ColorRgb c = new ColorRgb(1, 1, 0);

        for ( i = 0; i < things.size() && i < 3; i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3Dd position;

            if ( g != null ) {
                gl.glPushMatrix();
                gl.glLoadIdentity();

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
                viewport.getActiveCamera().projectPoint(p, tp);

                //---------------------------------------------
                yellow = false;
                if ( ColorRgb.distance(c, r.getMaterial().getDiffuse()) <
                     VSDK.EPSILON ) {
                yellow = true;
                }

                //---------------------------------------------
                switch ( i ) {
                  case 0:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            xLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            xLabelImage);
                    }
                    break;
                  case 1:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            yLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            yLabelImage);
                    }
                    break;
                  case 2:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            zLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            zLabelImage);
                    }
                    break;
                }
                gl.glPopMatrix();
            }
        }
    }
}
