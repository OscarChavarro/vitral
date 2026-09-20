package application.model;

import java.util.List;

import application.framework.Scene;
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
