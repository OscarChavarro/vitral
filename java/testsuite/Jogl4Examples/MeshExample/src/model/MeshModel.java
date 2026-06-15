package model;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.LightType;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.scene.SimpleScene;

public class MeshModel {
    private final Camera camera;
    private final List<Light> lights;
    private final SimpleScene scene;
    private final RendererConfiguration qualitySelection;
    private String tangibleServiceUrl = "ws://localhost:8090/v1/values";

    public MeshModel() {
        scene = new SimpleScene();
        camera = new Camera();
        qualitySelection = new RendererConfiguration();
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

    public String getTangibleServiceUrl() {
        return tangibleServiceUrl;
    }

    public void setTangibleServiceUrl(String tangibleServiceUrl) {
        if ( tangibleServiceUrl == null || tangibleServiceUrl.trim().isEmpty() ) {
            return;
        }
        this.tangibleServiceUrl = tangibleServiceUrl;
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

        if ( lights.size() > 0 ) {
            lights.get(0).setPosition(lightPos0);
        }
        if ( lights.size() > 1 ) {
            lights.get(1).setPosition(lightPos1);
        }
    }
}
