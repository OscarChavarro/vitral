package application.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import application.framework.Scene;
import framework.model.ViewportSet;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.light.Light;
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
            viewportSets.add(viewportSet);
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
