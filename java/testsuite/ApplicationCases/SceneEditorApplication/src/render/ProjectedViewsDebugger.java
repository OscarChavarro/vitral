//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =

package render;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Quaterniond;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Triangle;
import vsdk.toolkit.environment.geometry.element.Vertex;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.processing.ImageProcessing;

import model.Scene;
import model.ApplicationModel;
import model.DrawingArea;
import model.ProjectedViewsDebugPlan;

/**
Debugging tool that renders the projected views of the selected bodies (13
views from the faces and corners of their bounding cube) and presents them as
textured boxes in a visual debug group of the scene. Views are rendered
through a `ProjectedViewRenderer`, so this class does not depend on the
rendering technology.
*/
public class ProjectedViewsDebugger
{
    private static final boolean DO_DISTANCE_FIELD = false;
    private static final int DISTANCE_FIELD_SIDE = 320;

    private final Scene scene;
    private final DrawingArea drawingArea;
    private final DrawingAreaHost host;
    private final RendererConfiguration quality;
    private final int viewSize;
    private final boolean isTransparent;

    public ProjectedViewsDebugger(ApplicationModel model,
                                  DrawingAreaHost host)
    {
        this.scene = model.getScene();
        this.drawingArea = model.getDrawingArea();
        this.host = host;

        quality = new RendererConfiguration();
        quality.setWires(false);
        quality.setSurfaces(true);
        isTransparent = true;

        if ( DO_DISTANCE_FIELD ) {
            viewSize = DISTANCE_FIELD_SIDE;
        }
        else {
            viewSize = 640;
        }
    }

    /**
    Creates the debug group with the projected views of the selected bodies,
    if it was requested in the drawing area.
    @param renderer renders the projected views
    */
    public void debugIfNeeded(ProjectedViewRenderer renderer)
    {
        //-----------------------------------------------------------------
        if ( !drawingArea.isProjectedViewsDebugRequested() ) {
            return;
        }
        drawingArea.setProjectedViewsDebugRequested(false);

        //-----------------------------------------------------------------
        int selectedThing = scene.selectedThings.firstSelected();
        SimpleBody referenceBody = null;
        int i;

        if ( selectedThing >= 0 ) {
            referenceBody = scene.scene.getSimpleBodies().get(selectedThing);
        }

        if ( referenceBody == null ) {
            host.showStatusMessage("ERROR: An object must be selected for projected views debugging to be created");
        }
        else {
            SimpleBodyGroup group;
            SimpleBodyGroup bodySet;
            bodySet = new SimpleBodyGroup();

            for ( i = 0; i < scene.selectedThings.size(); i++ ) {
                if ( scene.selectedThings.isSelected(i) ) {
                    referenceBody = scene.scene.getSimpleBodies().get(i);
                    bodySet.getBodies().add(referenceBody);
                }
            }

            group = addDebugProjectedView(renderer, bodySet);

            if ( group != null ) {
                scene.debugThingGroups.add(group);
            }
            else {
                host.showStatusMessage("ERROR: cannot create Pbuffer, you need recent 3D hardware acceleration for this function");
            }
        }
    }

    private Image createProjectedView(ProjectedViewRenderer renderer,
                                      SimpleBodyGroup referenceBodies, int cam)
    {
        //- Will render a normalized body inside the unit cube ------------
        double minmax[];
        SimpleBodyGroup bodySet = new SimpleBodyGroup();
        int i;

        Vector3Dd p;
        {
            //-----------------------------------------------------------------
            minmax = referenceBodies.getMinMax();
            Vector3Dd min, max, s;
            min = new Vector3Dd(minmax[0], minmax[1], minmax[2]);
            max = new Vector3Dd(minmax[3], minmax[4], minmax[5]);
            s = new Vector3Dd(max.x() - min.x(), max.y() - min.y(), max.z() - min.z());

            double maxsize = s.x();
            if ( s.y() > maxsize ) maxsize = s.y();
            if ( s.z() > maxsize ) maxsize = s.z();
            // The 95% scale factor is to allow a full render of the object to
            // fit inside the rendered view
            s = new Vector3Dd((2/maxsize) * 0.95, (2/maxsize) * 0.95,
                (2/maxsize) * 0.95);

            p = max.add(min);
            p = p.multiply(-1/maxsize);

            bodySet.setPosition(p);
            bodySet.setScale(s);
            //-----------------------------------------------------------------
            SimpleBody referenceBody;
            SimpleBody framedBody;

            for ( i = 0; i < referenceBodies.getBodies().size(); i++ ) {
                referenceBody = referenceBodies.getBodies().get(i);
                framedBody = new SimpleBody();
                framedBody.setGeometry(referenceBody.getGeometry());
                framedBody.setPosition(referenceBody.getPosition());
                framedBody.setRotation(referenceBody.getRotation());
                framedBody.setRotationInverse(referenceBody.getRotationInverse());
                framedBody.setMaterial(scene.defaultMaterial());
                bodySet.getBodies().add(framedBody);
            }
            //-----------------------------------------------------------------
            Matrix4x4d Mset = bodySet.getTransformationMatrix(), R, Ri, Mbody, S, M;
            SimpleBody copiedBody;
            Quaterniond q;

            for ( i = 0; i < referenceBodies.getBodies().size(); i++ ) {
                referenceBody = referenceBodies.getBodies().get(i);
                if ( cam == 1 ) {
                    copiedBody = scene.addThing(referenceBody.getGeometry());
                    Mbody = referenceBody.getTransformationMatrix();
                    M = Mset.multiply(Mbody);
                    p = M.extractTranslation();
                    M = M.withVal(0, 3, 0.0);
                    M = M.withVal(1, 3, 0.0);
                    M = M.withVal(2, 3, 0.0);
                    q = M.exportToQuaternion().normalized();
                    R = new Matrix4x4d();
                    R = R.importFromQuaternion(q);
                    Ri = R.inverse();
                    S = Ri.multiply(M);
                    s = new Vector3Dd(M.get(0, 0), M.get(1, 1), M.get(2, 2));

                    copiedBody.setPosition(p);
                    copiedBody.setScale(s);
                    copiedBody.setRotation(R);
                }
            }

            //-----------------------------------------------------------------
        }

        //- Render will proceed in a PBuffer ------------------------------
        IndexedColorImageUncompressed distanceFieldIndexed;

        Image projectedView = createContourImage(renderer.renderDepth(bodySet,
            ProjectedViewsDebugPlan.createCamera(cam), quality, viewSize, viewSize));

        //-----------------------------------------------------------------
        Image finalImage;
        if ( !DO_DISTANCE_FIELD ) {
            finalImage = projectedView;
        }
        else {
            System.out.print("Processing maps for view " + cam + "... ");
            distanceFieldIndexed = new IndexedColorImageUncompressed();
            distanceFieldIndexed.init(DISTANCE_FIELD_SIDE, DISTANCE_FIELD_SIDE);
            ImageProcessing.processDistanceFieldWithArray(projectedView, distanceFieldIndexed, 1);
            ImageProcessing.gammaCorrection(distanceFieldIndexed, 2.0);

            RGBAImageUncompressed distanceFieldRgba;
            distanceFieldRgba = distanceFieldIndexed.exportToRgbaImage();
            int x, y;

            for ( x = 0; x < distanceFieldRgba.getXSize(); x++ ) {
                for ( y = 0; y < distanceFieldRgba.getYSize(); y++ ) {
                    if ( distanceFieldIndexed.getPixel(x, y) < 1 ) {
                        distanceFieldRgba.putPixel(x, y,
                                                   (byte)255, (byte)0, (byte)0, (byte)128);
                    }
                }
            }
            finalImage = distanceFieldRgba;
            System.out.println("Ok!");
        }

        //- Obtain Pbuffer's rendered image -------------------------------
        return finalImage;
    }

    /**
    Keeps just the silhouette of a rendered view: its depth frontier border.
    @param depth depth buffer of the rendered view
    @return contour image, as the gradient of the silhouette
    */
    private Image createContourImage(ZBuffer depth)
    {
        IndexedColorImageUncompressed zbuffer;
        NormalMap nm;
        zbuffer = depth.exportIndexedColorImage();

        //- Erase internal details: keep just the depth frontier border ---
        int x, y;
        int val;
        for ( x = 0; x < zbuffer.getXSize(); x++ ) {
            for ( y = 0; y < zbuffer.getYSize(); y++ ) {
                val = zbuffer.getPixel(x, y);
                if ( val < 255 ) {
                    zbuffer.putPixel(x, y, (byte)0);
                }
                else {
                    zbuffer.putPixel(x, y, (byte)255);
                }

            }
        }

        //- Get contourns from depth buffer's gradient --------------------
        nm = new NormalMap();
        nm.importBumpMap(zbuffer, new Vector3Dd(1, 1, 0.1));

        //- Calculate borders (contourns) ---------------------------------
        if ( isTransparent ) {
            return nm.exportToRgbaImageGradient();
        }
        return nm.exportToRgbImageGradient();
    }

    private SimpleBodyGroup addDebugProjectedView(ProjectedViewRenderer renderer,
                                                  SimpleBodyGroup referenceBodies)
    {
        SimpleBody boxBody;
        Image texture;
        SimpleBodyGroup group;
        int i;
        TriangleMesh mesh;
        Vertex[] vertexArray;
        Triangle[] triangleArray;
        Vector3Dd n;
        Image textureArray[];
        SimpleMaterial materialArray[];
        int materialRanges[][];
        int textureRanges[][];

        group = new SimpleBodyGroup();
        for ( i = 1; i <= ProjectedViewsDebugPlan.VIEW_COUNT; i++ ) {
            ProjectedViewsDebugPlan.ViewPlacement placement =
                ProjectedViewsDebugPlan.getPlacement(i);
            Matrix4x4d R = placement.getRotation();

            //-----------------------------------------------------------------
            texture = createProjectedView(renderer, referenceBodies, i);
            if ( texture == null ) {
                return null;
            }

            //-----------------------------------------------------------------
            n = new Vector3Dd(0, 0, 1);
            vertexArray = new Vertex[4];
            vertexArray[0] = new Vertex(new Vector3Dd(-1, -1, 0), n, 0.0, 0.0);
            vertexArray[1] = new Vertex(new Vector3Dd(1, -1, 0), n, 1.0, 0.0);
            vertexArray[2] = new Vertex(new Vector3Dd(1, 1, 0), n, 1.0, 1.0);
            vertexArray[3] = new Vertex(new Vector3Dd(-1, 1, 0), n, 0.0, 1.0);
            triangleArray = new Triangle[2];
            triangleArray[0] = new Triangle(0, 1, 2);
            triangleArray[1] = new Triangle(2, 3, 0);
            textureArray = new Image[1];
            textureArray[0] = texture;
            textureRanges = new int[1][2];
            textureRanges[0][0] = 2;
            textureRanges[0][1] = 1;
            materialArray = new SimpleMaterial[1];
            materialArray[0] = scene.defaultMaterial();
            materialArray[0] = materialArray[0].withDoubleSided(true);
            materialArray[0] = materialArray[0].withAmbient(new ColorRgb(1, 1, 1));
            materialArray[0] = materialArray[0].withDiffuse(new ColorRgb(1, 1, 1));
            materialArray[0] = materialArray[0].withSpecular(new ColorRgb(1, 1, 1));
            materialRanges = new int[1][2];
            materialRanges[0][0] = 2;
            materialRanges[0][1] = 0;

            mesh = new TriangleMesh();
            mesh.setVertexes(vertexArray, true, false, false, true);
            mesh.setTriangles(triangleArray);
            mesh.setTextures(textureArray);
            mesh.setTextureRanges(textureRanges);
            mesh.setMaterials(materialArray);
            mesh.setMaterialRanges(materialRanges);

            //-----------------------------------------------------------------
            boxBody = new SimpleBody();
            boxBody.setGeometry(mesh);
            boxBody.setPosition(placement.getPosition());
            boxBody.setScale(placement.getScale());
            boxBody.setRotation(R);
            boxBody.setRotationInverse(R.inverse());
            boxBody.setMaterial(scene.defaultMaterial());
            boxBody.setMaterial(boxBody.getMaterial().withDoubleSided(true));
            boxBody.setMaterial(boxBody.getMaterial().withAmbient(new ColorRgb(1, 1, 1)));
            boxBody.setMaterial(boxBody.getMaterial().withDiffuse(new ColorRgb(1, 1, 1)));
            boxBody.setMaterial(boxBody.getMaterial().withSpecular(new ColorRgb(1, 1, 1)));
            boxBody.setName("Proyected view box");
            boxBody.setTexture(texture);
            //-----------------------------------------------------------------
            group.getBodies().add(boxBody);
        }
        return group;
    }
}
