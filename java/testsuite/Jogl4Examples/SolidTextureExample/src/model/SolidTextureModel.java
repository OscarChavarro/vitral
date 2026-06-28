package model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Intersection;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.LightType;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.gizmo.RayGizmo;
import vsdk.toolkit.media.Image;

public class SolidTextureModel {
    private static final int MIN_SOLID_TEXTURE_SIZE = 1;
    private static final int MAX_SOLID_TEXTURE_SIZE = 128;

    private final Camera camera;
    private final List<Light> lights;
    private final SimpleScene scene;
    private final RendererConfiguration qualitySelection;
    private final RayGizmo rayGizmo;
    private final List<Image> texture2DStack;
    private int solidTextureSize;
    private OperationMode operationMode;
    private String tangibleServiceUrl = "ws://localhost:8090/v1/values";

    public SolidTextureModel() {
        scene = new SimpleScene();
        camera = new Camera();
        qualitySelection = new RendererConfiguration();
        rayGizmo = new RayGizmo(makeIntersectionCallback(), 1);
        texture2DStack = new ArrayList<>();
        solidTextureSize = 32;
        rebuildTexture2DStack();
        operationMode = OperationMode.MESH_MODEL;
        lights = new ArrayList<>();
        Light light0 = new Light(LightType.POINT, new Vector3Dd(10, -20, 50), new ColorRgb(1, 1, 1));
        light0.setId(0);
        Light light1 = new Light(LightType.POINT, new Vector3Dd(-10, 20, 50), new ColorRgb(1, 1, 1));
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

    public List<Image> getTexture2DStack() {
        return texture2DStack;
    }

    public int getSolidTextureSize() {
        return solidTextureSize;
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

    public void setOperationMode(OperationMode operationMode) {
        if ( operationMode == null ) {
            return;
        }
        this.operationMode = operationMode;
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
        for ( int i = 0; i < solidTextureSize; i++ ) {
            texture2DStack.add(null);
        }
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
        double[] minmax = group.getMinMax();
        if ( minmax == null || minmax.length < 6 ) {
            return;
        }

        Vector3Dd min = new Vector3Dd(minmax[0], minmax[1], minmax[2]);
        Vector3Dd max = new Vector3Dd(minmax[3], minmax[4], minmax[5]);
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
