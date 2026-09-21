package application.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import application.framework.Scene;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.media.RGBImageUncompressed;

public class ApplicationModel
{
    private Scene scene;
    private RGBImageUncompressed raytracedImage;
    private RGBImageUncompressed zbufferImage;
    private int raytracedImageWidth;
    private int raytracedImageHeight;
    private RGBColorPalette palette;
    private boolean withVisualDebugRay;
    private Ray visualDebugRay;
    private int visualDebugRayLevels;
    private final List<ViewportSet> viewportSets;
    private int activeViewportSetIndex;
    private Widget i18nContext;
    private final SceneLightFactory lightFactory = new SceneLightFactory();

    /**
    Creates the model with one standard `ViewportSet`. More sets can be added
    to support users working with several displays.
    */
    public ApplicationModel()
    {
        viewportSets = new ArrayList<ViewportSet>();
        viewportSets.add(ViewportSet.createStandardSet("Display 1"));
        activeViewportSetIndex = 0;
    }

    /**
    @return a read-only view of the viewport sets, one for each display
    */
    public List<ViewportSet> getViewportSets()
    {
        return Collections.unmodifiableList(viewportSets);
    }

    public void addViewportSet(ViewportSet viewportSet)
    {
        if ( viewportSet != null ) {
            viewportSet.setI18nContext(i18nContext);
            viewportSets.add(viewportSet);
        }
    }

    /**
    @return the I18N context (GUI definition in the current language) shared by
    the viewport sets, or null if there is none yet
    */
    public Widget getI18nContext()
    {
        return i18nContext;
    }

    /**
    Sets the I18N context and propagates it to every viewport set, so they are
    presented with the messages of the language currently selected by the
    user. It must be called each time the GUI definition is loaded.
    @param i18nContext
    */
    public void setI18nContext(Widget i18nContext)
    {
        this.i18nContext = i18nContext;
        for ( ViewportSet viewportSet : viewportSets ) {
            viewportSet.setI18nContext(i18nContext);
        }
    }

    public int getActiveViewportSetIndex()
    {
        return activeViewportSetIndex;
    }

    public void setActiveViewportSetIndex(int activeViewportSetIndex)
    {
        if ( activeViewportSetIndex >= 0 && activeViewportSetIndex < viewportSets.size() ) {
            this.activeViewportSetIndex = activeViewportSetIndex;
        }
    }

    /**
    @return the viewport set of the display currently receiving interaction
    */
    public ViewportSet getActiveViewportSet()
    {
        return viewportSets.get(activeViewportSetIndex);
    }

    public Scene getScene()
    {
        return scene;
    }

    public void setScene(Scene scene)
    {
        this.scene = scene;
    }

    public Camera getCamera()
    {
        return scene.camera;
    }

    public Camera getActiveCamera()
    {
        return scene.activeCamera;
    }

    public void setActiveCamera(Camera activeCamera)
    {
        scene.activeCamera = activeCamera;
    }

    public List<Light> getLights()
    {
        return scene.scene.getLights();
    }

    /**
    Adds a new point light to the scene, placed inside the view volume of a
    camera of the active viewport set (see `SceneLightFactory`).
    @return the added light, or null if no viewport is visible
    */
    public PointLight addNewLight()
    {
        PointLight light = lightFactory.createLight(getLights(), getActiveViewportSet());

        if ( light != null ) {
            getLights().add(light);
        }
        return light;
    }

    public List<SimpleBody> getSimpleBodies()
    {
        return scene.scene.getSimpleBodies();
    }

    public RGBImageUncompressed getRaytracedImage()
    {
        return raytracedImage;
    }

    public void setRaytracedImage(RGBImageUncompressed raytracedImage)
    {
        this.raytracedImage = raytracedImage;
    }

    public RGBImageUncompressed getZbufferImage()
    {
        return zbufferImage;
    }

    public void setZbufferImage(RGBImageUncompressed zbufferImage)
    {
        this.zbufferImage = zbufferImage;
    }

    public int getRaytracedImageWidth()
    {
        return raytracedImageWidth;
    }

    public void setRaytracedImageWidth(int raytracedImageWidth)
    {
        this.raytracedImageWidth = raytracedImageWidth;
    }

    public int getRaytracedImageHeight()
    {
        return raytracedImageHeight;
    }

    public void setRaytracedImageHeight(int raytracedImageHeight)
    {
        this.raytracedImageHeight = raytracedImageHeight;
    }

    public RGBColorPalette getPalette()
    {
        return palette;
    }

    public void setPalette(RGBColorPalette palette)
    {
        this.palette = palette;
    }

    public boolean isWithVisualDebugRay()
    {
        return withVisualDebugRay;
    }

    public void setWithVisualDebugRay(boolean withVisualDebugRay)
    {
        this.withVisualDebugRay = withVisualDebugRay;
    }

    public Ray getVisualDebugRay()
    {
        return visualDebugRay;
    }

    public void setVisualDebugRay(Ray visualDebugRay)
    {
        this.visualDebugRay = visualDebugRay;
    }

    public int getVisualDebugRayLevels()
    {
        return visualDebugRayLevels;
    }

    public void setVisualDebugRayLevels(int visualDebugRayLevels)
    {
        this.visualDebugRayLevels = visualDebugRayLevels;
    }
}
