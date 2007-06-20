//===========================================================================
import java.io.File;

import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Vector4D;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.io.geometry.ReaderObj;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;

public class WireframeOfflineExample {
    private Camera camera;
    private TriangleMeshGroup meshGroup;
    private RendererConfiguration qualitySelection;
    private Calligraphic2DBuffer lineSet;
    private RGBImage img;

    public WireframeOfflineExample() {
        //-----------------------------------------------------------------
        camera = new Camera();
        camera.setPosition(new Vector3D(7, -4, 4));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(140),
                              Math.toRadians(-30), 0);
        camera.setRotation(R);
        camera.updateViewportResize(640, 480);

        qualitySelection = new RendererConfiguration();
        qualitySelection.setSurfaces(false);
        qualitySelection.setWires(true);

        lineSet = new Calligraphic2DBuffer();

        img = new RGBImage();

        //-----------------------------------------------------------------
        try {
            meshGroup = ReaderObj.read("../../../etc/geometry/cow.obj");
        }
        catch ( Exception ex ) {
            System.err.println("Failed to read file");
            System.exit(0);
            return;
        }

    }

    private void addLine(Calligraphic2DBuffer lineSet,
                         Vector3D cp0, Vector3D cp1, Matrix4x4 R) {
        Vector4D hp0, hp1; // Clipped points in homogeneous space
        Vector4D pp0, pp1; // Projected points

        hp0 = new Vector4D(cp0);
        hp1 = new Vector4D(cp1);
        pp0 = R.multiply(hp0);
        pp0.divideByW();
        pp1 = R.multiply(hp1);
        pp1.divideByW();
        lineSet.add2DLine(pp0.x, pp0.y, pp1.x, pp1.y);
    }

    public void display() {
        TriangleMesh mesh = null;
        int i;
        Vertex[] arrVertexes;
        Triangle[] arrTriangles;
        int t;
        int p0, p1, p2;
        Vector3D mp0, mp1; // Mesh points
        Vector3D cp0, cp1; // Clipped points
        Matrix4x4 R;

        cp0 = new Vector3D();
        cp1 = new Vector3D();
        R = camera.calculateProjectionMatrix(camera.STEREO_MODE_CENTER);

        for ( i = 0; i < meshGroup.getMeshes().size(); i++ ) {
            mesh = meshGroup.getMeshes().get(i);
            arrVertexes = mesh.getVertexes();
            arrTriangles = mesh.getTriangles();
            for ( t = 0; t < arrTriangles.length; t++ ) {
                p0 = arrTriangles[t].p0;
                p1 = arrTriangles[t].p1;
                p2 = arrTriangles[t].p2;

                mp0 = arrVertexes[p0].position;
                mp1 = arrVertexes[p1].position;
                if ( camera.clipLineCohenSutherland(mp0, mp1, cp0, cp1) ) {
                    addLine(lineSet, cp0, cp1, R);
                }

                mp0 = arrVertexes[p1].position;
                mp1 = arrVertexes[p2].position;
                if ( camera.clipLineCohenSutherland(mp0, mp1, cp0, cp1) ) {
                    addLine(lineSet, cp0, cp1, R);
                }

                mp0 = arrVertexes[p2].position;
                mp1 = arrVertexes[p0].position;
                if ( camera.clipLineCohenSutherland(mp0, mp1, cp0, cp1) ) {
                    addLine(lineSet, cp0, cp1, R);
                }
            }
        }

        //-----------------------------------------------------------------
        double xt = camera.getViewportXSize();
        double yt = camera.getViewportYSize();

        Vector3D e0 = new Vector3D();
        Vector3D e1 = new Vector3D();
        int x0, y0, x1, y1;
        RGBPixel pixel = new RGBPixel();

        img.init((int)xt, (int)yt);
        pixel.r = (byte)255;
        pixel.g = (byte)255;
        pixel.b = (byte)255;

        for ( int j = 0; j < lineSet.getNumLines(); j++ ) {
            lineSet.get2DLine(j, e0, e1);
            x0 = (int)((xt-1)*((e0.x+1)/2));
            y0 = (int)((yt-1)*(1-((e0.y+1)/2)));
            x1 = (int)((xt-1)*((e1.x+1)/2));
            y1 = (int)((yt-1)*(1-((e1.y+1)/2)));
            img.drawLine(x0, y0, x1, y1, pixel);
        }

        lineSet.init();

        //-----------------------------------------------------------------
        ImagePersistence.exportJPG(new File("output.jpg"), img);
    }

    public static void main (String[] args) {
        WireframeOfflineExample instance = new WireframeOfflineExample();
        instance.display();
    }

}

//===========================================================================
