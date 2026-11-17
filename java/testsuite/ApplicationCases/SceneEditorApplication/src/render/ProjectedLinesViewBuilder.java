//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          Meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package render;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.geometricProcessing.GeometryCalligraphicExporter;
import vsdk.toolkit.environment.geometry.geometricProcessing.GeometryTriangulator;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.media.Calligraphic3DBuffer;
import vsdk.toolkit.render.SelectionCorners;
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;

import model.Scene;

/**
Builds the drawing of a viewport as projected 2D lines, computed entirely in
the processor: the "calligraphic" approach of vector displays and plotters
[FOLE1992].1, where a 3D scene is presented without any 3D graphics API. The
result is a list of `ProjectedLineLayer`s, so this class does not depend on
the 2D technology that finally draws them (see `render.awt`).

The bodies follow the `RendererConfiguration` of the viewport:
  - With surfaces, the bodies that are (or can be exported to) a
    `PolyhedralBoundedSolid` are drawn with hidden line removal, by the
    quantitative invisibility algorithm of [APPE1967] (`HiddenLineRenderer`):
    contour lines are thicker than the other visible lines, and hidden lines
    are dashed if wires are also asked for. The occlusion is computed among
    all those solids.
  - Curves and surfaces are exported to 3D lines
    (`GeometryCalligraphicExporter`: the surfaces through a mesh, whose
    edges are the lines). With surfaces they are "free lines" of the hidden
    line algorithm: they are hidden by the solids (dashed if wires are asked
    for), but do not hide anything, not even themselves: by design, the
    hidden line removal is limited to the solids, as in [APPE1967]. The
    reference grid is also drawn as free lines. An infinite plane is
    presented as a square of it, centered at the point of the plane viewed
    by the camera (as a hint of its position).
  - Without surfaces, every body is a plain wireframe with no hidden line
    removal (whether wires are asked for or not): the edges of its boundary
    representation, or its lines as above. It is the simplest and fastest
    presentation without a 3D graphics API.
  - Bodies with none of them are represented by their bounding box.
  - Points, bounding volumes and the selection corners of selected bodies
    are added over them.

The light gizmos, the visual debug entities and the feedback of the body
editor are also presented as lines, over the bodies.
*/
public class ProjectedLinesViewBuilder
{
    private static final ColorRgb GRID_COLOR = new ColorRgb(0.37, 0.37, 0.37);
    private static final ColorRgb GRID_AXIS_COLOR = new ColorRgb(0, 0, 0);
    private static final ColorRgb VISIBLE_LINE_COLOR = new ColorRgb(0, 0, 0);
    private static final ColorRgb HIDDEN_LINE_COLOR = new ColorRgb(0.32, 0.32, 0.32);
    private static final ColorRgb BOUNDING_VOLUME_COLOR = new ColorRgb(0.6, 0.9, 1.0);
    private static final ColorRgb POINT_COLOR = new ColorRgb(1.0, 0.0, 0.0);
    private static final ColorRgb LIGHT_COLOR = new ColorRgb(1.0, 1.0, 1.0);
    private static final ColorRgb SELECTED_LIGHT_COLOR = new ColorRgb(1.0, 1.0, 0.0);
    private static final ColorRgb DEBUG_COLOR = new ColorRgb(0.2, 0.8, 1.0);
    private static final ColorRgb EDIT_FEEDBACK_COLOR = new ColorRgb(1.0, 0.55, 0.0);
    private static final double LIGHT_GIZMO_SIZE = 0.15;
    /// Distance an orthogonal camera is moved back for the Appel lines of sight
    private static final double ORTHOGONAL_EYE_DISTANCE = 1000.0;

    private final BrepExportCache brepCache = new BrepExportCache();

    /**
    Lines in world coordinates that are not edges of the solids (the
    reference grid, the curves), drawn with one color: with hidden line
    removal they are hidden by the solids, but do not hide anything.
    */
    private static final class FreeLineSet
    {
        final Calligraphic3DBuffer lines = new Calligraphic3DBuffer();
        final ColorRgb color;

        FreeLineSet(ColorRgb color)
        {
            this.color = color;
        }
    }

    /**
    Builds the lines presenting the scene in a viewport.
    @param scene scene to present
    @param viewport viewport whose active camera, rendering configuration and
    grid are used; its camera must have its viewport size updated
    @param editor editor of the selected body, whose feedback is presented, or
    null if there is none
    @param withBodies true to present the bodies and the grid (hidden lines
    render mode), false to present only the annotations that go over an image
    of the scene computed by other means (raytracing render mode)
    @return the layers, in drawing order
    */
    public List<ProjectedLineLayer> buildView(Scene scene, Viewport viewport,
                                              BodyEditFeedbackProvider editor,
                                              boolean withBodies)
    {
        List<ProjectedLineLayer> layers = new ArrayList<ProjectedLineLayer>();
        Camera camera = viewport.getActiveCamera();
        RendererConfiguration quality = viewport.getRendererConfiguration();
        ClipSpaceLineProjector projector = new ClipSpaceLineProjector(camera);

        if ( withBodies ) {
            List<FreeLineSet> freeLines = new ArrayList<FreeLineSet>();
            if ( viewport.isShowGrid() ) {
                addGrid(freeLines, camera);
            }
            addBodies(layers, freeLines, projector, scene, camera, quality);
        }
        addAnnotations(layers, projector, camera, scene, quality, editor);
        addDebugEntities(layers, projector, camera, scene);
        addLights(layers, projector, scene);
        return layers;
    }

    //= Grid ==============================================================

    /**
    Same reference grid as the OpenGL presentation: 14x14 unit cells on the
    plane z = 0, or on the vertical plane facing the camera for orthogonal
    side views. Its lines are free lines (the solids hide them).
    */
    private void addGrid(List<FreeLineSet> freeLines, Camera camera)
    {
        Matrix4x4d gridTransform = new Matrix4x4d();
        Matrix4x4d rotation = camera.getRotation();
        double yaw = Math.toDegrees(rotation.obtainEulerYawAngle());
        double pitch = Math.toDegrees(rotation.obtainEulerPitchAngle());

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL &&
             (pitch > -45 && pitch < 45) ) {
            if ( (yaw > 45 && yaw < 135) || (yaw < -45 && yaw > -135) ) {
                gridTransform = gridTransform.axisRotation(Math.toRadians(90), 1, 0, 0);
            }
            else {
                gridTransform = gridTransform.axisRotation(Math.toRadians(90), 0, 1, 0);
            }
        }

        FreeLineSet grid = new FreeLineSet(GRID_COLOR);
        FreeLineSet axes = new FreeLineSet(GRID_AXIS_COLOR);
        int n = 14;
        double half = n / 2.0;
        int i;

        for ( i = 0; i <= n; i++ ) {
            double c = -half + i;
            FreeLineSet target = (i == n / 2) ? axes : grid;
            target.lines.add3DLine(gridTransform.multiply(new Vector3Dd(c, -half, 0)),
                gridTransform.multiply(new Vector3Dd(c, half, 0)));
            target.lines.add3DLine(gridTransform.multiply(new Vector3Dd(-half, c, 0)),
                gridTransform.multiply(new Vector3Dd(half, c, 0)));
        }
        freeLines.add(grid);
        freeLines.add(axes);
    }

    //= Bodies ============================================================

    /**
    @param freeLines the free lines already collected (i.e. the grid), to
    which the curves are added; they are drawn here, with the solids
    */
    private void addBodies(List<ProjectedLineLayer> layers,
                           List<FreeLineSet> freeLines,
                           ClipSpaceLineProjector projector, Scene scene,
                           Camera camera, RendererConfiguration quality)
    {
        List<SimpleBody> bodies = scene.scene.getSimpleBodies();
        List<SimpleBody> solids = new ArrayList<SimpleBody>();
        int firstBodyLayer = layers.size();
        ProjectedLineLayer points = new ProjectedLineLayer(POINT_COLOR, 3.0,
            ProjectedLineLayer.Style.POINTS);

        for ( SimpleBody body : bodies ) {
            if ( scene.isHidden(body) ) {
                continue;
            }
            Geometry geometry = body.getGeometry();
            if ( geometry == null ) {
                continue;
            }
            PolyhedralBoundedSolid brep = exportToBrep(geometry);
            Calligraphic3DBuffer lines = null;
            if ( quality.isSurfacesSet() && brep == null ) {
                lines = exportToLines(geometry, body.getTransformationMatrix(),
                    camera);
            }
            if ( quality.isSurfacesSet() && brep != null ) {
                // Drawn below, all at once: they occlude each other
                solids.add(brepBody(body, brep));
            }
            else if ( lines != null ) {
                // Curves and surfaces: hidden by the solids, hide nothing
                addFreeLines(freeLines, lines, body);
            }
            else {
                // Without surfaces (or without a boundary representation to
                // hide with), a plain wireframe, whatever the wires flag is
                ProjectedLineLayer wires = new ProjectedLineLayer(
                    wireColor(body.getMaterial()), 1.0,
                    ProjectedLineLayer.Style.SOLID);
                addWireframe(wires, projector, camera, geometry, brep,
                    body.getTransformationMatrix());
                addIfNotEmpty(layers, wires);
            }
            if ( quality.isPointsSet() ) {
                addPoints(points, projector, geometry, brep,
                    body.getTransformationMatrix());
            }
        }

        if ( !solids.isEmpty() ) {
            addHiddenLineDrawing(layers, solids, freeLines, camera,
                quality.isWiresSet());
        }
        else {
            addFreeLinesWithoutHiding(layers, firstBodyLayer, freeLines,
                projector);
        }
        addIfNotEmpty(layers, points);
    }

    /**
    The lines of a curve or a surface, in world coordinates, with the wire
    color of its body.
    @param local the lines, in the coordinates of the body
    */
    private static void addFreeLines(List<FreeLineSet> freeLines,
                                     Calligraphic3DBuffer local, SimpleBody body)
    {
        Matrix4x4d transform = body.getTransformationMatrix();
        FreeLineSet set = new FreeLineSet(wireColor(body.getMaterial()));
        int i;

        for ( i = 0; i < local.getNumLines(); i++ ) {
            Vector3Dd[] ends = local.get3DLine(i);
            set.lines.add3DLine(transform.multiply(ends[0]),
                transform.multiply(ends[1]));
        }
        freeLines.add(set);
    }

    /**
    Without solids to hide them, the free lines are simply projected, below
    the wireframes of the bodies.
    @param position index in `layers` where their layers are inserted
    */
    private static void addFreeLinesWithoutHiding(List<ProjectedLineLayer> layers,
                                                  int position,
                                                  List<FreeLineSet> freeLines,
                                                  ClipSpaceLineProjector projector)
    {
        for ( FreeLineSet set : freeLines ) {
            ProjectedLineLayer layer = new ProjectedLineLayer(set.color, 1.0,
                ProjectedLineLayer.Style.SOLID);
            int i;
            for ( i = 0; i < set.lines.getNumLines(); i++ ) {
                Vector3Dd[] ends = set.lines.get3DLine(i);
                projector.addSegment(layer.getLines(), ends[0], ends[1]);
            }
            if ( !layer.isEmpty() ) {
                layers.add(position, layer);
                position++;
            }
        }
    }

    /**
    Presents a set of solids with the hidden line removal of [APPE1967],
    and the free lines hidden by them. The hidden parts of all the lines are
    drawn first (dashed, only if asked for), then the visible free lines and
    finally the visible lines of the solids.
    */
    private void addHiddenLineDrawing(List<ProjectedLineLayer> layers,
                                      List<SimpleBody> solids,
                                      List<FreeLineSet> freeLines,
                                      Camera camera, boolean withHiddenLines)
    {
        ProjectedLineLayer contours = new ProjectedLineLayer(VISIBLE_LINE_COLOR,
            2.0, ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer visible = new ProjectedLineLayer(VISIBLE_LINE_COLOR,
            1.0, ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer hidden = new ProjectedLineLayer(HIDDEN_LINE_COLOR,
            1.0, ProjectedLineLayer.Style.DASHED);

        List<Calligraphic3DBuffer> freeInput = new ArrayList<Calligraphic3DBuffer>();
        List<Calligraphic2DBuffer> freeVisibleOutput = new ArrayList<Calligraphic2DBuffer>();
        List<Calligraphic2DBuffer> freeHiddenOutput = new ArrayList<Calligraphic2DBuffer>();
        List<ProjectedLineLayer> freeVisible = new ArrayList<ProjectedLineLayer>();
        List<ProjectedLineLayer> freeHidden = new ArrayList<ProjectedLineLayer>();

        for ( FreeLineSet set : freeLines ) {
            ProjectedLineLayer setVisible = new ProjectedLineLayer(set.color,
                1.0, ProjectedLineLayer.Style.SOLID);
            ProjectedLineLayer setHidden = new ProjectedLineLayer(set.color,
                1.0, ProjectedLineLayer.Style.DASHED);
            freeInput.add(set.lines);
            freeVisibleOutput.add(setVisible.getLines());
            freeHiddenOutput.add(setHidden.getLines());
            freeVisible.add(setVisible);
            freeHidden.add(setHidden);
        }

        HiddenLineRenderer.executeAppelAlgorithm(solids,
            lineOfSightCamera(camera), contours.getLines(), visible.getLines(),
            hidden.getLines(), freeInput, freeVisibleOutput, freeHiddenOutput);

        if ( withHiddenLines ) {
            for ( ProjectedLineLayer layer : freeHidden ) {
                addIfNotEmpty(layers, layer);
            }
            addIfNotEmpty(layers, hidden);
        }
        for ( ProjectedLineLayer layer : freeVisible ) {
            addIfNotEmpty(layers, layer);
        }
        addIfNotEmpty(layers, visible);
        addIfNotEmpty(layers, contours);
    }

    /**
    The quantitative invisibility of [APPE1967] is measured along the line of
    sight from the eye to each point. For an orthogonal camera all the lines
    of sight are parallel, so the eye is moved far back along the viewing
    direction (with its far plane, so the projection does not change).
    @param camera camera of the view
    @return the camera to give to the hidden line algorithm
    */
    private static Camera lineOfSightCamera(Camera camera)
    {
        if ( camera.getProjectionMode() != Camera.PROJECTION_MODE_ORTHOGONAL ) {
            return camera;
        }
        Camera eye = new Camera(camera);
        Vector3Dd front = camera.getFront().normalized();
        eye.setPosition(camera.getPosition().subtract(front.multiply(ORTHOGONAL_EYE_DISTANCE)));
        eye.setNearPlaneDistance(camera.getNearPlaneDistance() + ORTHOGONAL_EYE_DISTANCE);
        eye.setFarPlaneDistance(camera.getFarPlaneDistance() + ORTHOGONAL_EYE_DISTANCE);
        return eye;
    }

    /**
    The quantitative invisibility is answered by the geometry of each body,
    and only a `PolyhedralBoundedSolid` answers it: a body whose geometry is
    exported to one (i.e. a `Box` or a `Sphere`) is replaced, for the hidden
    line algorithm, by a body with the same transformation and the exported
    boundary representation as geometry.
    @param body body of the scene
    @param brep boundary representation of its geometry
    @return the body to give to the hidden line algorithm
    */
    private static SimpleBody brepBody(SimpleBody body, PolyhedralBoundedSolid brep)
    {
        if ( body.getGeometry() == brep ) {
            return body;
        }
        SimpleBody surrogate = new SimpleBody();
        surrogate.setName(body.getName());
        surrogate.setGeometry(brep);
        surrogate.setPosition(body.getPosition());
        surrogate.setRotation(body.getRotation());
        surrogate.setScale(body.getScale());
        return surrogate;
    }

    /**
    @param geometry
    @return the boundary representation of the geometry, or null if it is
    not a volume that can be exported to one (see `BrepExportCache`)
    */
    private PolyhedralBoundedSolid exportToBrep(Geometry geometry)
    {
        return brepCache.get(geometry);
    }

    /**
    @param geometry a geometry
    @param transform transformation from the coordinates of the geometry to
    world coordinates
    @param camera camera of the view
    @return the lines of a curve or of the mesh of a surface, in the
    coordinates of the geometry (see `GeometryCalligraphicExporter`), or null
    if it has none
    */
    private static Calligraphic3DBuffer exportToLines(Geometry geometry,
                                                      Matrix4x4d transform,
                                                      Camera camera)
    {
        if ( geometry instanceof InfinitePlane plane ) {
            return GeometryCalligraphicExporter.exportToCalligraphic3DBuffer(
                plane, viewedPoint(plane, transform, camera));
        }
        return GeometryCalligraphicExporter.exportToCalligraphic3DBuffer(geometry);
    }

    /**
    The point of a plane viewed by a camera: where the line of sight through
    the center of the view meets the plane, or, if it does not meet it in
    front of the camera and before its far plane, the point of the plane
    closest to the camera.
    @param transform transformation from the coordinates of the plane to
    world coordinates
    @return the point, in the coordinates of the plane
    */
    private static Vector3Dd viewedPoint(InfinitePlane plane,
                                         Matrix4x4d transform, Camera camera)
    {
        Matrix4x4d inverse = transform.inverse();
        Vector3Dd eye = inverse.multiply(camera.getPosition());
        Vector3Dd direction = inverse.multiply(
            camera.getPosition().add(camera.getFront())).subtract(eye);
        double towards = plane.getCoefficients().dotProduct(direction);
        double length = direction.length();

        if ( towards != 0 && length > 0 ) {
            double t = -plane.pointDistance(eye) / towards;
            if ( t > 0 && t * length <= camera.getFarPlaneDistance() ) {
                return eye.add(direction.multiply(t));
            }
        }
        return plane.projectPoint(eye);
    }

    /**
    Adds the wireframe of a geometry: the edges of its boundary
    representation, else the lines of a curve or of the mesh of a surface
    (`GeometryCalligraphicExporter`), else its bounding box.
    @param camera camera of the view (to center the square shown of an
    infinite plane)
    @param brep boundary representation of the geometry, or null
    */
    private void addWireframe(ProjectedLineLayer layer,
                              ClipSpaceLineProjector projector, Camera camera,
                              Geometry geometry, PolyhedralBoundedSolid brep,
                              Matrix4x4d transform)
    {
        if ( brep != null ) {
            int i;
            for ( i = 0; i < brep.getEdgesList().size(); i++ ) {
                _PolyhedralBoundedSolidEdge edge = brep.getEdgesList().get(i);
                if ( edge.getStartingVertexId() < 0 || edge.getEndingVertexId() < 0 ||
                     edge.rightHalf == null || edge.leftHalf == null ) {
                    continue;
                }
                Vector3Dd p0 = edge.rightHalf.startingVertex.position;
                Vector3Dd p1 = edge.leftHalf.startingVertex.position;
                if ( p0 != null && p1 != null ) {
                    projector.addSegment(layer.getLines(), transform, p0, p1);
                }
            }
            return;
        }

        Calligraphic3DBuffer lines = exportToLines(geometry, transform, camera);
        if ( lines != null ) {
            int i;
            for ( i = 0; i < lines.getNumLines(); i++ ) {
                Vector3Dd[] ends = lines.get3DLine(i);
                projector.addSegment(layer.getLines(), transform, ends[0], ends[1]);
            }
            return;
        }

        addBox(layer, projector, geometry.getMinMax(), transform);
    }

    private void addPoints(ProjectedLineLayer layer,
                           ClipSpaceLineProjector projector,
                           Geometry geometry, PolyhedralBoundedSolid brep,
                           Matrix4x4d transform)
    {
        if ( brep != null ) {
            int i;
            for ( i = 0; i < brep.getVerticesList().size(); i++ ) {
                _PolyhedralBoundedSolidVertex vertex = brep.getVerticesList().get(i);
                if ( vertex.position != null ) {
                    projector.addPoint(layer.getLines(), transform.multiply(vertex.position));
                }
            }
            return;
        }

        TriangleMeshGroup meshes = GeometryTriangulator.exportToTriangleMeshGroup(geometry);
        if ( meshes == null ) {
            return;
        }
        for ( TriangleMesh mesh : meshes.getMeshes() ) {
            double[] v = mesh.getVertexPositions();
            int i;
            for ( i = 0; v != null && i < mesh.getNumVertices(); i++ ) {
                projector.addPoint(layer.getLines(), transform.multiply(
                    new Vector3Dd(v[3*i], v[3*i + 1], v[3*i + 2])));
            }
        }
    }

    private static ColorRgb wireColor(SimpleMaterial material)
    {
        if ( material == null ) {
            return VISIBLE_LINE_COLOR;
        }
        return material.getDiffuse();
    }

    //= Annotations =======================================================

    private void addAnnotations(List<ProjectedLineLayer> layers,
                                ClipSpaceLineProjector projector,
                                Camera camera, Scene scene,
                                RendererConfiguration quality,
                                BodyEditFeedbackProvider editor)
    {
        List<SimpleBody> bodies = scene.scene.getSimpleBodies();
        ProjectedLineLayer boxes = new ProjectedLineLayer(BOUNDING_VOLUME_COLOR,
            1.0, ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer corners = new ProjectedLineLayer(
            SelectionCorners.getDefaultColor(), 1.0, ProjectedLineLayer.Style.SOLID);
        int i;

        scene.selectedThings.sync();
        for ( i = 0; i < bodies.size(); i++ ) {
            SimpleBody body = bodies.get(i);
            if ( scene.isHidden(body) ) {
                continue;
            }
            Geometry geometry = body.getGeometry();
            if ( geometry == null ) {
                continue;
            }
            if ( quality.isBoundingVolumeSet() ) {
                addBox(boxes, projector, geometry.getMinMax(),
                    body.getTransformationMatrix());
            }
            if ( scene.selectedThings.isSelected(i) ) {
                addSelectionCorners(corners, projector, geometry.getMinMax(),
                    body.getTransformationMatrix());
            }
        }
        addIfNotEmpty(layers, boxes);
        addIfNotEmpty(layers, corners);

        if ( editor != null && editor.getTarget() != null ) {
            ProjectedLineLayer feedback = new ProjectedLineLayer(
                EDIT_FEEDBACK_COLOR, 1.0, ProjectedLineLayer.Style.SOLID);
            for ( RenderPrimitive primitive : editor.buildEditFeedback() ) {
                Geometry geometry = primitive.getGeometry();
                if ( geometry != null ) {
                    addWireframe(feedback, projector, camera, geometry,
                        exportToBrep(geometry), primitive.getTransform());
                }
            }
            addIfNotEmpty(layers, feedback);
        }
    }

    private void addSelectionCorners(ProjectedLineLayer layer,
                                     ClipSpaceLineProjector projector,
                                     double[] minmax, Matrix4x4d transform)
    {
        Vector3Dd[] segments = SelectionCorners.buildSegments(minmax);
        int i;

        for ( i = 0; i + 1 < segments.length; i += 2 ) {
            projector.addSegment(layer.getLines(), transform, segments[i], segments[i + 1]);
        }
    }

    /**
    Adds the twelve edges of an axis aligned box.
    @param minmax minimum x, y, z followed by maximum x, y, z
    */
    private void addBox(ProjectedLineLayer layer,
                        ClipSpaceLineProjector projector, double[] minmax,
                        Matrix4x4d transform)
    {
        if ( minmax == null || minmax.length < 6 ) {
            return;
        }
        for ( double limit : minmax ) {
            // i.e. an infinite plane
            if ( Double.isInfinite(limit) || Double.isNaN(limit) ) {
                return;
            }
        }
        Vector3Dd[] c = new Vector3Dd[8];
        int i;

        for ( i = 0; i < 8; i++ ) {
            c[i] = new Vector3Dd(
                (i & 1) != 0 ? minmax[3] : minmax[0],
                (i & 2) != 0 ? minmax[4] : minmax[1],
                (i & 4) != 0 ? minmax[5] : minmax[2]);
        }
        for ( i = 0; i < 8; i++ ) {
            int bit;
            for ( bit = 1; bit < 8; bit <<= 1 ) {
                if ( (i & bit) == 0 ) {
                    projector.addSegment(layer.getLines(), transform, c[i], c[i | bit]);
                }
            }
        }
    }

    //= Debug entities and lights =========================================

    private void addDebugEntities(List<ProjectedLineLayer> layers,
                                  ClipSpaceLineProjector projector,
                                  Camera camera, Scene scene)
    {
        ProjectedLineLayer debug = new ProjectedLineLayer(DEBUG_COLOR, 1.0,
            ProjectedLineLayer.Style.SOLID);

        for ( SimpleBodyGroup group : scene.debugThingGroups ) {
            Matrix4x4d groupTransform = group.getTransformationMatrix();
            for ( SimpleBody body : group.getBodies() ) {
                Geometry geometry = body.getGeometry();
                if ( geometry != null ) {
                    addWireframe(debug, projector, camera, geometry, exportToBrep(geometry),
                        groupTransform.multiply(body.getTransformationMatrix()));
                }
            }
        }
        addIfNotEmpty(layers, debug);
    }

    /**
    Each light is presented as a small star of three axis aligned segments
    and four diagonals, centered at its position.
    */
    private void addLights(List<ProjectedLineLayer> layers,
                           ClipSpaceLineProjector projector, Scene scene)
    {
        List<Light> lights = scene.scene.getLights();
        ProjectedLineLayer normal = new ProjectedLineLayer(LIGHT_COLOR, 1.0,
            ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer selected = new ProjectedLineLayer(SELECTED_LIGHT_COLOR,
            2.0, ProjectedLineLayer.Style.SOLID);
        double s = LIGHT_GIZMO_SIZE * scene.getLightGizmoScale();
        double d = s / Math.sqrt(3.0);
        Vector3Dd[] directions = {
            new Vector3Dd(s, 0, 0), new Vector3Dd(0, s, 0), new Vector3Dd(0, 0, s),
            new Vector3Dd(d, d, d), new Vector3Dd(-d, d, d),
            new Vector3Dd(d, -d, d), new Vector3Dd(d, d, -d)
        };
        int i;

        scene.selectedLights.sync();
        for ( i = 0; i < lights.size(); i++ ) {
            Vector3Dd center = lights.get(i).getPosition();
            ProjectedLineLayer target = scene.selectedLights.isSelected(i) ? selected : normal;
            if ( center == null ) {
                continue;
            }
            for ( Vector3Dd direction : directions ) {
                projector.addSegment(target.getLines(), center.subtract(direction),
                    center.add(direction));
            }
        }
        addIfNotEmpty(layers, normal);
        addIfNotEmpty(layers, selected);
    }

    private static void addIfNotEmpty(List<ProjectedLineLayer> layers,
                                      ProjectedLineLayer layer)
    {
        if ( !layer.isEmpty() ) {
            layers.add(layer);
        }
    }
}
