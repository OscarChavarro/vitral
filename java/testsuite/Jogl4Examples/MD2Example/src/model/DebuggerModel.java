package model;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.elements.Vertex2D;
import vsdk.toolkit.environment.geometry.surface.Md2Mesh;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.LightType;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

public class DebuggerModel {
    public Camera camera;
    public List<Light> lights;
    public RendererConfiguration qualitySelection;
    public SimpleScene scene;
    public Md2Mesh md2Mesh;

    public double x;
    public Vertex2D p0;
    public Vertex2D p1;
    public Vertex2D p2;

    public DebuggerModel() {
        scene = new SimpleScene();
        md2Mesh = new Md2Mesh();
        camera = new Camera();
        camera.setPosition(new Vector3Dd(0, -100, 0));
        camera.setFarPlaneDistance(4000);
        qualitySelection = new RendererConfiguration();

        lights = new ArrayList<Light>();

        Light frontLeft = new Light(LightType.POINT, new Vector3Dd(-35, -20, 50), new ColorRgb(1, 1, 1));
        frontLeft.setId(0);
        lights.add(frontLeft);

        Light frontRight = new Light(LightType.POINT, new Vector3Dd(35, -20, 50), new ColorRgb(1, 1, 1));
        frontRight.setId(1);
        lights.add(frontRight);

        Light back = new Light(LightType.POINT, new Vector3Dd(0, 20, 50), new ColorRgb(1, 1, 1));
        back.setId(2);
        lights.add(back);
    }
}
