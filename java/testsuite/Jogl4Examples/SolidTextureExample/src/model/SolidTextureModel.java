package model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import vsdk.toolkit.common.color.ColorRgba;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Intersection;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.gizmo.InfinitePlaneGizmo;
import vsdk.toolkit.gui.gizmo.RayGizmo;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImageHDRUncompressed;
import vsdk.toolkit.media.RGBAColorPalette;
import vsdk.toolkit.media.RGBAPixelHDR;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.solidTexture.TextureUtils;
import vsdk.toolkit.media.solidTexture.from2d.ControlledRGBAImageHDRUncompressed;
import vsdk.toolkit.media.solidTexture.from2d.ImageTexture;
import vsdk.toolkit.media.solidTexture.from2d.ImageToSolidTextureInterpolationTypes;
import vsdk.toolkit.media.solidTexture.from2d.ImageToSolidTextureProjectionMethods;
import vsdk.toolkit.media.solidTexture.procedural.ColorTextureFixture;
import vsdk.toolkit.media.solidTexture.procedural.ProceduralNoise;

public class SolidTextureModel {
    private static final int MIN_SOLID_TEXTURE_SIZE = 1;
    private static final int MAX_SOLID_TEXTURE_SIZE = 256;
    private static final double SMALL_TOLERANCE = 1.0e-6;

    private final Camera camera;
    private final List<Light> lights;
    private final SimpleScene scene;
    private final RendererConfiguration qualitySelection;
    private final RayGizmo rayGizmo;
    private final InfinitePlaneGizmo infinitePlaneGizmo;
    private final List<Image> texture2DStack;
    private final ColorTextureFixture colorTextureFixture;
    private final ImageTexture imageTexture;
    private int solidTextureSize;
    private byte[] solidTextureVolumeRgb8;
    private long solidTextureRevision;
    private SolidTextureExampleColorNames selectedSolidTexture;
    private boolean animationEnabled;
    private boolean hudVisible;
    private OperationMode operationMode;
    private String tangibleServiceUrl = "ws://localhost:8090/v1/values";

    public SolidTextureModel() {
        scene = new SimpleScene();
        camera = new Camera();
        qualitySelection = new RendererConfiguration();
        rayGizmo = new RayGizmo(makeIntersectionCallback(), 1);
        infinitePlaneGizmo = new InfinitePlaneGizmo();
        texture2DStack = new ArrayList<>();
        TextureUtils textureUtils = new TextureUtils();
        ProceduralNoise proceduralNoise = textureUtils.getProceduralNoise();
        proceduralNoise.initialize();
        colorTextureFixture = new ColorTextureFixture(proceduralNoise, textureUtils);
        imageTexture = new ImageTexture();
        solidTextureSize = 32;
        solidTextureVolumeRgb8 = new byte[0];
        solidTextureRevision = 0L;
        selectedSolidTexture = SolidTextureExampleColorNames.CHECKER_TEXTURE;
        animationEnabled = false;
        hudVisible = true;
        rebuildTexture2DStack();
        operationMode = OperationMode.MESH_MODEL;
        lights = new ArrayList<>();
        Light light0 = new PointLight(new Vector3Dd(10, -20, 50), new ColorRgb(1, 1, 1));
        light0.setId(0);
        Light light1 = new PointLight(new Vector3Dd(-10, 20, 50), new ColorRgb(1, 1, 1));
        light1.setId(1);
        lights.add(light0);
        lights.add(light1);
    }

    public Camera getCamera() {
        return camera;
    }

    public List<Light> getLights() {
        return lights;
    }

    public SimpleScene getScene() {
        return scene;
    }

    public RendererConfiguration getQualitySelection() {
        return qualitySelection;
    }

    public RayGizmo getRayGizmo() {
        return rayGizmo;
    }

    public InfinitePlaneGizmo getInfinitePlaneGizmo() {
        return infinitePlaneGizmo;
    }

    public List<Image> getTexture2DStack() {
        return texture2DStack;
    }

    public byte[] getSolidTextureVolumeRgb8() {
        return solidTextureVolumeRgb8;
    }

    public long getSolidTextureRevision() {
        return solidTextureRevision;
    }

    public int getSolidTextureSize() {
        return solidTextureSize;
    }

    public SolidTextureExampleColorNames getSelectedSolidTexture() {
        return selectedSolidTexture;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }

    public void toggleAnimationEnabled() {
        animationEnabled = !animationEnabled;
    }

    public boolean isHudVisible() {
        return hudVisible;
    }

    public void toggleHudVisible() {
        hudVisible = !hudVisible;
    }

    public void advanceObjectRotationRadians(double deltaRadians) {
        Matrix4x4d deltaRotation = new Matrix4x4d()
            .axisRotation(deltaRadians, 0.0, 0.0, 1.0);
        for ( SimpleBody body : scene.getSimpleBodies() ) {
            body.setRotation(deltaRotation.multiply(body.getRotation()));
        }
    }

    public void selectNextSolidTexture() {
        selectedSolidTexture = selectedSolidTexture.next();
        rebuildTexture2DStack();
    }

    public void selectPreviousSolidTexture() {
        selectedSolidTexture = selectedSolidTexture.previous();
        rebuildTexture2DStack();
    }

    public void increaseSolidTextureSize() {
        if ( solidTextureSize >= MAX_SOLID_TEXTURE_SIZE ) {
            return;
        }
        solidTextureSize *= 2;
        rebuildTexture2DStack();
    }

    public void decreaseSolidTextureSize() {
        if ( solidTextureSize <= MIN_SOLID_TEXTURE_SIZE ) {
            return;
        }
        solidTextureSize /= 2;
        rebuildTexture2DStack();
    }

    public OperationMode getOperationMode() {
        return operationMode;
    }

    public void rotateOperationMode() {
        OperationMode[] modes = OperationMode.values();
        int next = (operationMode.ordinal() + 1) % modes.length;
        operationMode = modes[next];
    }

    public String getTangibleServiceUrl() {
        return tangibleServiceUrl;
    }

    public void setTangibleServiceUrl(String tangibleServiceUrl) {
        if ( tangibleServiceUrl == null || tangibleServiceUrl.trim().isEmpty() ) {
            return;
        }
        this.tangibleServiceUrl = tangibleServiceUrl;
    }

    private void rebuildTexture2DStack() {
        texture2DStack.clear();
        int side = Math.max(1, solidTextureSize);
        solidTextureVolumeRgb8 = new byte[side * side * side * 3];
        for ( int i = 0; i < solidTextureSize; i++ ) {
            texture2DStack.add(generateTextureSlice(i));
        }
        solidTextureRevision++;
    }

    private Image generateTextureSlice(int sliceIndex) {
        RGBImageUncompressed image = new RGBImageUncompressed();
        int side = Math.max(1, solidTextureSize);
        image.initNoFill(side, side);
        double z = unitCoordinate(sliceIndex, solidTextureSize);
        ControlledRGBAImageHDRUncompressed imageMap = null;
        ControlledRGBAImageHDRUncompressed materialMap = null;

        if ( selectedSolidTexture == SolidTextureExampleColorNames.IMAGE_MAP_TEXTURE ) {
            imageMap = buildImageMapTexture();
        }
        if ( selectedSolidTexture == SolidTextureExampleColorNames.MATERIAL_MAP_TEXTURE ) {
            materialMap = buildMaterialMapTexture();
        }

        for ( int y = 0; y < side; y++ ) {
            double py = unitCoordinate(y, side);
            for ( int x = 0; x < side; x++ ) {
                double px = unitCoordinate(x, side);
                ColorRgba color = colorAt(px, py, z, imageMap, materialMap);
                putColor16As8(image, x, y, color);
                putVolumeColor16As8(side, x, y, sliceIndex, color);
            }
        }
        return image;
    }

    private ColorRgba colorAt(
        double x,
        double y,
        double z,
        ControlledRGBAImageHDRUncompressed imageMap,
        ControlledRGBAImageHDRUncompressed materialMap)
    {
        ColorRgba color = new ColorRgba();
        RGBAColorPalette palette = defaultPalette();

        switch ( selectedSolidTexture ) {
            case NO_TEXTURE -> color.set(new ColorRgba(0.18, 0.18, 0.18, 1.0));
            case COLOUR_TEXTURE -> color.set(new ColorRgba(0.72, 0.38, 0.18, 1.0));
            case BOZO_TEXTURE -> colorTextureFixture.bozo(x * 6.0, y * 6.0, z * 6.0, 1.25, 7, palette, color);
            case MARBLE_TEXTURE -> colorTextureFixture.marble(x * 8.0, y * 8.0, z * 8.0, 1.65, 7, palette, color);
            case WOOD_TEXTURE -> colorTextureFixture.wood(x * 9.0, y * 9.0, z * 9.0, 2.2, 6, woodPalette(), color);
            case CHECKER_TEXTURE -> colorTextureFixture.checker(
                x * 8.0, y * 8.0, z * 8.0, color,
                new ColorRgba(0.95, 0.95, 0.95, 1.0),
                new ColorRgba(0.05, 0.08, 0.12, 1.0),
                SMALL_TOLERANCE);
            case CHECKER_TEXTURE_TEXTURE -> checkerTextureTexture(x, y, z, color);
            case SPOTTED_TEXTURE -> colorTextureFixture.spotted(x * 7.0, y * 7.0, z * 7.0, palette, color);
            case AGATE_TEXTURE -> colorTextureFixture.agate(x * 8.0, y * 8.0, z * 8.0, 7, agatePalette(), color);
            case GRANITE_TEXTURE -> colorTextureFixture.granite(x * 6.0, y * 6.0, z * 6.0, granitePalette(), color);
            case GRADIENT_TEXTURE -> colorTextureFixture.gradient(
                x * 4.0, y * 4.0, z * 4.0, 0.45, palette, new Vector3Dd(1.0, 1.0, 1.0), 5, color);
            case IMAGE_MAP_TEXTURE -> imageTexture.imageMap(x, y, z, imageMap, color, SMALL_TOLERANCE);
            case ONION_TEXTURE -> colorTextureFixture.onion(x * 10.0, y * 10.0, z * 10.0, 0.65, 6, palette, color);
            case LEOPARD_TEXTURE -> colorTextureFixture.leopard(x * 12.0, y * 12.0, z * 12.0, 1.0, 6, leopardPalette(), color);
            case BRICK_TEXTURE -> colorTextureFixture.brick(
                x * 10.0, y * 7.0, z * 5.0, color,
                new ColorRgba(0.78, 0.78, 0.72, 1.0),
                new ColorRgba(0.58, 0.12, 0.07, 1.0),
                0.08);
            case MATERIAL_MAP_TEXTURE -> materialMapTexture(x, y, z, materialMap, color);
        }
        if ( color.getA() == 0.0 ) {
            color.setA(1.0);
        }
        return color;
    }

    private void checkerTextureTexture(double x, double y, double z, ColorRgba color) {
        int index = (int)(TextureUtils.floorInline(x * 8.0 + SMALL_TOLERANCE) +
            TextureUtils.floorInline(y * 8.0 + SMALL_TOLERANCE) +
            TextureUtils.floorInline(z * 8.0 + SMALL_TOLERANCE));
        if ( (index & 1) != 0 ) {
            colorTextureFixture.wood(x * 9.0, y * 9.0, z * 9.0, 1.7, 6, woodPalette(), color);
        }
        else {
            colorTextureFixture.marble(x * 8.0, y * 8.0, z * 8.0, 1.25, 6, defaultPalette(), color);
        }
    }

    private void materialMapTexture(
        double x,
        double y,
        double z,
        ControlledRGBAImageHDRUncompressed materialMap,
        ColorRgba color)
    {
        int material = imageTexture.materialMap(
            new Vector3Dd(x, y, z), null, materialMap, 4, SMALL_TOLERANCE);
        switch ( material ) {
            case 0 -> colorTextureFixture.wood(x * 9.0, y * 9.0, z * 9.0, 1.6, 5, woodPalette(), color);
            case 1 -> colorTextureFixture.granite(x * 6.0, y * 6.0, z * 6.0, granitePalette(), color);
            case 2 -> colorTextureFixture.leopard(x * 12.0, y * 12.0, z * 12.0, 0.9, 5, leopardPalette(), color);
            default -> colorTextureFixture.checker(
                x * 8.0, y * 8.0, z * 8.0, color,
                new ColorRgba(0.12, 0.25, 0.65, 1.0),
                new ColorRgba(0.95, 0.92, 0.4, 1.0),
                SMALL_TOLERANCE);
        }
    }

    private ControlledRGBAImageHDRUncompressed buildImageMapTexture() {
        ControlledRGBAImageHDRUncompressed image = new ControlledRGBAImageHDRUncompressed();
        int side = Math.max(1, solidTextureSize);
        image.allocate(side, side);
        image.setMapType(ImageToSolidTextureProjectionMethods.PLANAR_MAP);
        image.setInterpolationType(ImageToSolidTextureInterpolationTypes.BI_LINEAR);
        image.setImageGradient(new Vector3Dd(1.0, -1.0, 0.0));

        for ( int y = 0; y < side; y++ ) {
            for ( int x = 0; x < side; x++ ) {
                double u = unitCoordinate(x, side);
                double v = unitCoordinate(y, side);
                RGBAPixelHDR pixel = new RGBAPixelHDR();
                pixel.r = toChannel8(u);
                pixel.g = toChannel8(v);
                pixel.b = toChannel8(((x / 8.0) + (y / 8.0)) % 2 == 0 ? 0.85 : 0.2);
                pixel.a = toChannel8(1.0);
                image.setPixel(x, y, pixel);
            }
        }
        return image;
    }

    private ControlledRGBAImageHDRUncompressed buildMaterialMapTexture() {
        ControlledRGBAImageHDRUncompressed image = new ControlledRGBAImageHDRUncompressed();
        int side = Math.max(1, solidTextureSize);
        image.allocate(side, side);
        image.setMapType(ImageToSolidTextureProjectionMethods.PLANAR_MAP);
        image.setInterpolationType(ImageToSolidTextureInterpolationTypes.NO_INTERPOLATION);
        image.setImageGradient(new Vector3Dd(1.0, -1.0, 0.0));
        image.setUseColorFlag(false);

        IndexedColorImageHDRUncompressed indexed = new IndexedColorImageHDRUncompressed();
        indexed.allocate(side, side);
        indexed.setColorMapSize(4);
        RGBAPixelHDR[] table = new RGBAPixelHDR[4];
        for ( int i = 0; i < table.length; i++ ) {
            table[i] = new RGBAPixelHDR();
            table[i].r = toChannel8(i / 3.0);
            table[i].g = toChannel8(1.0 - i / 3.0);
            table[i].b = toChannel8((i & 1) == 0 ? 0.2 : 0.85);
            table[i].a = toChannel8(1.0);
        }
        indexed.setColorTable(table);
        int cell = Math.max(1, side / 5);
        for ( int y = 0; y < side; y++ ) {
            for ( int x = 0; x < side; x++ ) {
                indexed.setPixel(x, y, ((x / cell) + (y / cell)) & 3);
            }
        }
        image.setIndexedData(indexed);
        return image;
    }

    private RGBAColorPalette defaultPalette() {
        RGBAColorPalette palette = new RGBAColorPalette();
        palette.addColor(0.05, 0.12, 0.28, 1.0);
        palette.addColor(0.15, 0.55, 0.75, 1.0);
        palette.addColor(0.95, 0.78, 0.26, 1.0);
        palette.addColor(0.88, 0.18, 0.14, 1.0);
        return palette;
    }

    private RGBAColorPalette woodPalette() {
        RGBAColorPalette palette = new RGBAColorPalette();
        palette.addColor(0.25, 0.11, 0.04, 1.0);
        palette.addColor(0.64, 0.34, 0.13, 1.0);
        palette.addColor(0.88, 0.61, 0.28, 1.0);
        palette.addColor(0.33, 0.16, 0.07, 1.0);
        return palette;
    }

    private RGBAColorPalette agatePalette() {
        RGBAColorPalette palette = new RGBAColorPalette();
        palette.addColor(0.98, 0.92, 0.72, 1.0);
        palette.addColor(0.72, 0.36, 0.18, 1.0);
        palette.addColor(0.28, 0.12, 0.08, 1.0);
        palette.addColor(0.95, 0.78, 0.5, 1.0);
        return palette;
    }

    private RGBAColorPalette granitePalette() {
        RGBAColorPalette palette = new RGBAColorPalette();
        palette.addColor(0.08, 0.08, 0.09, 1.0);
        palette.addColor(0.32, 0.32, 0.34, 1.0);
        palette.addColor(0.7, 0.68, 0.64, 1.0);
        palette.addColor(0.16, 0.14, 0.13, 1.0);
        return palette;
    }

    private RGBAColorPalette leopardPalette() {
        RGBAColorPalette palette = new RGBAColorPalette();
        palette.addColor(0.05, 0.03, 0.015, 1.0);
        palette.addColor(0.86, 0.53, 0.12, 1.0);
        palette.addColor(0.96, 0.76, 0.26, 1.0);
        palette.addColor(0.12, 0.06, 0.02, 1.0);
        return palette;
    }

    private static double unitCoordinate(int index, int count) {
        if ( count <= 1 ) {
            return 0.0;
        }
        return index / (double)(count - 1);
    }

    private static void putColor16As8(RGBImageUncompressed image, int x, int y, ColorRgba color) {
        image.putPixel(
            x,
            y,
            (byte)channel16To8(color.getR()),
            (byte)channel16To8(color.getG()),
            (byte)channel16To8(color.getB()));
    }

    private void putVolumeColor16As8(int side, int x, int y, int z, ColorRgba color) {
        int base = ((z * side * side) + (y * side) + x) * 3;
        solidTextureVolumeRgb8[base] = (byte)channel16To8(color.getR());
        solidTextureVolumeRgb8[base + 1] = (byte)channel16To8(color.getG());
        solidTextureVolumeRgb8[base + 2] = (byte)channel16To8(color.getB());
    }

    private static char toChannel8(double value) {
        int channel = (int)Math.round(clamp01(value) * 255.0);
        return (char)(channel & 0xff);
    }

    private static int channel16To8(double value) {
        int channel16 = (int)Math.round(clamp01(value) * 65535.0);
        return (channel16 >>> 8) & 0xff;
    }

    private static double clamp01(double value) {
        if ( value < 0.0 ) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }

    /**
     * Returns a callback that tests a world-space ray against all bodies in
     * the scene and returns the closest intersection, or null if none.
     */
    private Function<Ray, Intersection> makeIntersectionCallback() {
        return ray -> {
            Intersection closest = null;
            double closestT = Double.MAX_VALUE;
            for ( SimpleBody body : scene.getSimpleBodies() ) {
                RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
                if ( body.doIntersectionFirstHit(ray, hit) && hit.hasHitDistance() ) {
                    double t = hit.hitDistance();
                    if ( t > 1e-6 && t < closestT ) {
                        closestT = t;
                        closest = new Intersection(t, hit.p, hit.n);
                    }
                }
            }
            return closest;
        };
    }

    public void configureInitialViewAndLightToScene() {
        if ( scene.getSimpleBodies().isEmpty() ) {
            return;
        }

        SimpleBodyGroup group = new SimpleBodyGroup();
        group.getBodies().addAll(scene.getSimpleBodies());
        double[] minMax = group.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return;
        }

        Vector3Dd min = new Vector3Dd(minMax[0], minMax[1], minMax[2]);
        Vector3Dd max = new Vector3Dd(minMax[3], minMax[4], minMax[5]);
        Vector3Dd center = min.add(max).multiply(0.5);
        double radius = max.subtract(min).length() * 0.5;
        if ( radius < 0.001 ) {
            radius = 1.0;
        }

        double fovRad = Math.toRadians(camera.getFov());
        double viewDistance = (radius / Math.tan(fovRad * 0.5)) * 1.35;
        if ( viewDistance < radius * 1.5 ) {
            viewDistance = radius * 1.5;
        }

        Vector3Dd eyeDirection = new Vector3Dd(0, -1, 0.35).normalized();
        Vector3Dd eye = center.add(eyeDirection.multiply(viewDistance));
        camera.setPosition(eye);
        camera.setUpMaintainingOrthogonality(new Vector3Dd(0, 0, 1));
        camera.setFocusedPositionMaintainingOrthogonality(center);

        double nearPlane = Math.max(0.01, viewDistance - (radius * 2.2));
        double farPlane = Math.max(nearPlane + 1.0, viewDistance + (radius * 4.0));
        camera.setNearPlaneDistance(nearPlane);
        camera.setFarPlaneDistance(farPlane);
        camera.updateVectors();

        Vector3Dd lightDirection = new Vector3Dd(1, -1, 1).normalized();
        Vector3Dd lightPos0 = center.add(lightDirection.multiply(radius * 3.0));
        Vector3Dd lightPos1 = center.add(new Vector3Dd(-lightDirection.x(), -lightDirection.y(), lightDirection.z())
            .normalized()
            .multiply(radius * 3.0));

        if ( !lights.isEmpty() ) {
            lights.get(0).setPosition(lightPos0);
        }
        if ( lights.size() > 1 ) {
            lights.get(1).setPosition(lightPos1);
        }
    }
}
