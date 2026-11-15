package render;

import java.io.File;
import java.util.ArrayList;

import model.ShadersModel;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.camera.CameraSnapshot;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.AmbientLight;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.material.MicroFacetedMaterial;
import vsdk.toolkit.environment.background.SimpleBackground;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.raytracing.ParallelRaytracer;

public class SoftwareRaycaster
{
    private static final Vector3Dd DEFAULT_BUMP_SCALE = new Vector3Dd(1.0, 1.0, 1.0);

    /// One thread per available processor, reused by every frame
    private final ParallelRaytracer parallelRaytracer;
    private final NormalMap bumpNormalMap;

    public SoftwareRaycaster()
    {
        parallelRaytracer = new ParallelRaytracer();
        bumpNormalMap = loadBumpNormalMap();
    }

    public void invalidateSnapshot()
    {
        // No-op by design: the snapshot is rebuilt on every software render
        // to keep camera/light/object transforms fully synchronized with the
        // interactive OpenGL path.
    }

    public void render(
        ShadersModel model,
        Camera activeCamera,
        Matrix4x4d modelRotation)
    {
        RGBImageUncompressed outputImage = model.getSoftwareFrameImage();
        if ( outputImage == null ) {
            return;
        }

        SimpleSceneSnapshot snapshot = buildSceneSnapshot(
            model,
            activeCamera,
            modelRotation,
            outputImage);
        parallelRaytracer.execute(
            outputImage,
            model.getQuality(),
            snapshot,
            false);
    }

    private SimpleSceneSnapshot buildSceneSnapshot(
        ShadersModel model,
        Camera activeCamera,
        Matrix4x4d modelRotation,
        RGBImageUncompressed outputImage)
    {
        int viewportWidth = outputImage.getXSize();
        int viewportHeight = outputImage.getYSize();
        CameraSnapshot cameraSnapshot = activeCamera.exportToCameraSnapshot(
            viewportWidth,
            viewportHeight);

        // The frame's body shares the model's geometries and maps; it owns only
        // its material copy.
        SimpleMaterial activeMaterial = model.getActiveMaterialForCurrentShading();
        SimpleMaterial materialCopy;
        if ( activeMaterial instanceof MicroFacetedMaterial microFacetedMaterial ) {
            materialCopy = new MicroFacetedMaterial(microFacetedMaterial);
        }
        else {
            materialCopy = new SimpleMaterial(activeMaterial);
        }
        ArrayList<SimpleBody> bodies = new ArrayList<SimpleBody>(1);
        if ( model.getGeometryFixture() != null ) {
            bodies.add(model.getGeometryFixture().createBody(materialCopy, modelRotation));
        }
        else {
            SimpleBody sphereBody = new SimpleBody();
            sphereBody.setGeometry(model.getSphere());
            sphereBody.setMaterial(materialCopy);
            sphereBody.setTexture(model.getTextureMap());
            sphereBody.setNormalMap(bumpNormalMap);
            sphereBody.setRotation(modelRotation);
            bodies.add(sphereBody);
        }

        ArrayList<Light> lights = new ArrayList<Light>(2);
        Light ambientLight = new AmbientLight(new ColorRgb(1, 1, 1));
        ambientLight.setId(0);
        lights.add(ambientLight);
        Light pointLight = model.getLight().copy();
        pointLight.setId(1);
        lights.add(pointLight);

        SimpleBackground background = new SimpleBackground();
        background.setColor(0, 0, 0);

        return new SimpleSceneSnapshot(
            bodies,
            lights,
            background,
            cameraSnapshot);
    }

    private static NormalMap loadBumpNormalMap()
    {
        try {
            IndexedColorImageUncompressed bumpMap = ImagePersistence.importIndexedColor(
                new File("../../../../etc/bumpmaps/earth.bw"));
            NormalMap normalMap = new NormalMap();
            normalMap.importBumpMap(bumpMap, DEFAULT_BUMP_SCALE);
            return normalMap;
        }
        catch ( Exception e ) {
            throw new IllegalStateException("Failed loading software bump map", e);
        }
    }
}
