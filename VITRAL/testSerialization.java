import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import vsdk.toolkit.common.Quaternion;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBAPixel;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.HalfSpace;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.Surface;
import vsdk.toolkit.environment.geometry.ParametricCubicCurve;
import vsdk.toolkit.environment.geometry.Solid;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Curve;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.scene.SimpleThing;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.Camera;

public class testSerialization{
    public static void main(String args[])
    {
        Quaternion quaternion = new Quaternion();
        Matrix4x4 matrix4x4 = new Matrix4x4();
        ColorRgb colorRgb = new ColorRgb();
        QualitySelection qualitySelection = new QualitySelection();
        Triangle triangle = new Triangle();
        Entity entity = new Entity();
        Vertex vertex = new Vertex();
        Vector3D vector3D = new Vector3D();
        Ray ray = new Ray(new Vector3D(0, 0, 0), new Vector3D(1, 1, 1));
        RGBColorPalette rGBColorPalette = new RGBColorPalette();
        RGBAImage rGBAImage = new RGBAImage();
        RGBAPixel rGBAPixel = new RGBAPixel();
        RGBPixel rGBPixel = new RGBPixel();
        RGBImage rGBImage = new RGBImage();
        Sphere sphere = new Sphere(1);
        TriangleMeshGroup triangleMeshGroup = new TriangleMeshGroup();
        Box box = new Box(1, 2, 3);
        TriangleMesh triangleMesh = new TriangleMesh();
        Arrow arrow = new Arrow(1, 2, 3, 4);
        Cone cone = new Cone(1, 2, 3);
        GeometryIntersectionInformation geometryIntersectionInformation = new GeometryIntersectionInformation();
        Material material = new Material();
        SimpleThing simpleThing = new SimpleThing();
        SimpleBackground simpleBackground = new SimpleBackground();
        Camera camera = new Camera();

        //Light light = new Light();
        //CubemapBackground cubemapBackground = new CubemapBackground();
        //InfinitePlane infinitePlane = new InfinitePlane();
        //ZBuffer zBuffer = new ZBuffer();
        //ParametricBiCubicPatch parametricBiCubicPatch = new ParametricBiCubicPatch();
        //ParametricCubicCurve parametricCubicCurve = new ParametricCubicCurve();

        System.out.println("Serialization test");

        String filename = "persisted.rawJavaSerialization";
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try
        {
            fos = new FileOutputStream(filename);
            out = new ObjectOutputStream(fos);

            out.writeObject(quaternion);
            out.writeObject(matrix4x4);
            out.writeObject(colorRgb);
            out.writeObject(qualitySelection);
            out.writeObject(triangle);
            out.writeObject(entity);
            out.writeObject(vertex);
            out.writeObject(vector3D);
            out.writeObject(ray);
            out.writeObject(rGBAImage);
            out.writeObject(rGBAPixel);
            out.writeObject(rGBPixel);
            out.writeObject(rGBImage);
            out.writeObject(camera);
            out.writeObject(material);
            out.writeObject(simpleBackground);
            out.writeObject(simpleThing);
/*
            out.writeObject(sphere);
            out.writeObject(box);
            out.writeObject(arrow);
            out.writeObject(cone);
            out.writeObject(triangleMesh);
            out.writeObject(triangleMeshGroup);
            out.writeObject(rGBColorPalette);
            out.writeObject(geometryIntersectionInformation);
*/

            out.close();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }

    }
}
