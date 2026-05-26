package model;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
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
    public int selectedObject;

    public DebuggerModel() {
        scene = new SimpleScene();
        md2Mesh = new Md2Mesh();
        camera = new Camera();
        camera.setPosition(new Vector3Dd(0, -100, 0));
        camera.setFarPlaneDistance(4000);
        qualitySelection = new RendererConfiguration();

        lights = new ArrayList<Light>();

        Light lightOne = new Light(LightType.POINT, new Vector3Dd(65, 20, 20), new ColorRgb(1, 1, 1));
        lightOne.setId(0);
        lights.add(lightOne);

        Light lightTwo = new Light(LightType.POINT, new Vector3Dd(20, 20, 10), new ColorRgb(1, 1, 1));
        lightTwo.setId(1);
        lights.add(lightTwo);

        Light lightThree = new Light(LightType.POINT, new Vector3Dd(0, -20, 20), new ColorRgb(1, 1, 1));
        lightThree.setId(2);
        lights.add(lightThree);

        selectedObject = -1;
    }
}
