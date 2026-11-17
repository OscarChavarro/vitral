//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,          =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#include <stddef.h>
#include <math.h>

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/render/gizmo/CameraControlGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"

const double CameraControlGizmo2DPainter::MIN_LABEL_VISIBILITY = 0.3;

CameraControlGizmo2DPainter::CameraControlGizmo2DPainter()
{
    cachedGizmo = NULL;
    hasCachedRotation = FALSE;
    cachedSize = -1;
}

CameraControlGizmo2DPainter::~CameraControlGizmo2DPainter()
{
    clearBrepParts();
}

void CameraControlGizmo2DPainter::clearBrepParts()
{
    for ( long i = 0; i < brepParts.size(); i++ ) {
        delete brepParts[i];
    }
    brepParts.clear();
}

void CameraControlGizmo2DPainter::paint(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, const ViewportElementScaler* scaler)
{
    if ( !gizmo->isVisible() ) {
        return;
    }
    prepareBreps(gizmo);
    updateHiddenLines(gizmo);

    const Camera* hud = gizmo->getHudCamera();
    Matrix4x4d projection = hud->calculateProjectionMatrix();
    Vector3Dd viewDirection = hud->getFront();
    double opacity = gizmo->getOpacity();

    //- Faces, depth sorted ----------------------------------------------
    for ( int r = 0; r < CameraControlGizmo::NUMBER_OF_PARTS; r++ ) {
        ColorRgb color = gizmo->getPartColor(r);
        if ( r == CameraControlGizmo::COMPASS_REGION ) {
            addCompassFaces(list, gizmo, projection, viewDirection, color,
                opacity);
        }
        else {
            addBrepFaces(list, gizmo, brepParts[r], projection,
                viewDirection, color, opacity);
        }
    }

    //- Visible lines from the hidden line removal -----------------------
    double lineWidth = scaler != NULL ? scaler->scaleLength(1.0) : 1.0;
    ColorRgb lineColor = gizmo->getLineColor();
    addLines(list, gizmo, visibleLines, lineColor, lineWidth);
    addLines(list, gizmo, contourLines, lineColor, 1.5 * lineWidth);

    //- Labels -----------------------------------------------------------
    int fontSize = scaler != NULL ? scaler->scaleSize(BASE_LABEL_FONT_SIZE) :
        (int)BASE_LABEL_FONT_SIZE;
    ArrayList<int> faces = gizmo->getFaceRegions();
    for ( long i = 0; i < faces.size(); i++ ) {
        int face = faces[i];
        if ( gizmo->faceVisibility(face) < MIN_LABEL_VISIBILITY ) {
            continue;
        }
        double p[2];
        gizmo->projectToViewport(gizmo->getFaceLabelPosition(face), p);
        list->addOverlayLabel(p[0], p[1], gizmo->getFaceLabel(face),
            gizmo->getLabelColor(), fontSize,
            Gizmo2DDrawList::Alignment::CENTER);
    }
}

//= Caches ================================================================

void CameraControlGizmo2DPainter::prepareBreps(const CameraControlGizmo* gizmo)
{
    if ( cachedGizmo == gizmo ) {
        return;
    }
    cachedGizmo = gizmo;
    hasCachedRotation = FALSE;
    clearBrepParts();
    for ( int p = 0; p < CameraControlGizmo::NUMBER_OF_PARTS; p++ ) {
        SimpleBody* part = gizmo->getParts()[p];
        PolyhedralBoundedSolid* brep = gizmo->exportPartToPolyhedralBoundedSolid(p);
        SimpleBody* body = new SimpleBody();
        body->setGeometry(brep);
        body->setPosition(part->getPosition());
        body->setRotation(Matrix4x4d());
        body->setRotationInverse(Matrix4x4d());
        body->setScale(Vector3Dd(1, 1, 1));
        // C++ port note: a copy, as the gizmo replaces the materials of its
        // parts when its state changes
        body->setMaterial(new SimpleMaterial(*part->getMaterial()));
        brepParts.add(body);
    }
}

void CameraControlGizmo2DPainter::updateHiddenLines(
    const CameraControlGizmo* gizmo)
{
    const Camera* hud = gizmo->getHudCamera();
    Matrix4x4d rotation = hud->getRotation();

    if ( hasCachedRotation && cachedSize == gizmo->getSizeInPixels() &&
         cachedRotation.epsilonEquals(rotation, 1e-9) ) {
        return;
    }
    hasCachedRotation = TRUE;
    cachedRotation = rotation;
    cachedSize = gizmo->getSizeInPixels();
    contourLines.init();
    visibleLines.init();
    Calligraphic2DBuffer hiddenLines;

    HiddenLineRenderer::executeAppelAlgorithm(brepParts, hud, &contourLines,
        &visibleLines, &hiddenLines);
}

//= Primitives ============================================================

void CameraControlGizmo2DPainter::addLines(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, const Calligraphic2DBuffer& lines,
    const ColorRgb& color, double width)
{
    double size = gizmo->getSizeInPixels();

    for ( int i = 0; i < lines.getNumLines(); i++ ) {
        Vector3Dd* p0 = lines.get2DLinePoint0(i);
        Vector3Dd* p1 = lines.get2DLinePoint1(i);
        list->addOverlayLine(
            gizmo->getHudX() + (p0->x() + 1.0) / 2.0 * size,
            gizmo->getHudY() + (1.0 - p0->y()) / 2.0 * size,
            gizmo->getHudX() + (p1->x() + 1.0) / 2.0 * size,
            gizmo->getHudY() + (1.0 - p1->y()) / 2.0 * size,
            color, width);
        delete p0;
        delete p1;
    }
}

double CameraControlGizmo2DPainter::lambert(const Vector3Dd& normal,
    const Vector3Dd& viewDirection)
{
    double c = -normal.normalized().dotProduct(viewDirection.normalized());
    return 0.65 + 0.35 * Math::max(0.0, c);
}

void CameraControlGizmo2DPainter::addPolygon(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, const Matrix4x4d& projection,
    const Vector3Dd& viewDirection, const Vector3Dd* vertices, int n,
    const Vector3Dd& normal, const ColorRgb& color, double opacity)
{
    if ( normal.dotProduct(viewDirection) >= 0.0 ) {
        return;
    }
    ArrayList<double> x;
    ArrayList<double> y;
    double depth = 0;
    double size = gizmo->getSizeInPixels();

    for ( int i = 0; i < n; i++ ) {
        Vector4Dd ndc = projection.multiply(Vector4Dd(vertices[i])).dividedByW();
        x.add(gizmo->getHudX() + (ndc.x() + 1.0) / 2.0 * size);
        y.add(gizmo->getHudY() + (1.0 - ndc.y()) / 2.0 * size);
        depth += ndc.z();
    }
    list->addPolygon(x, y, depth / n,
        color.multiply(lambert(normal, viewDirection)), opacity);
}

void CameraControlGizmo2DPainter::addBrepFaces(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, SimpleBody* body,
    const Matrix4x4d& projection, const Vector3Dd& viewDirection,
    const ColorRgb& color, double opacity)
{
    PolyhedralBoundedSolid* solid = (PolyhedralBoundedSolid*)body->getGeometry();
    Vector3Dd offset = body->getPosition();

    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        if ( face->boundariesList.size() != 1 ) {
            continue;
        }
        ArrayList<Vector3Dd> points;
        _PolyhedralBoundedSolidHalfEdge* start =
            face->boundariesList.get(0)->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        do {
            points.add(he->startingVertex->position.add(offset));
            he = he->next();
        } while ( he != start );
        addPolygon(list, gizmo, projection, viewDirection,
            points.data(), (int)points.size(),
            face->containingPlaneNormal(), color, opacity);
    }
}

void CameraControlGizmo2DPainter::addCompassFaces(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, const Matrix4x4d& projection,
    const Vector3Dd& viewDirection, const ColorRgb& color, double opacity)
{
    const int slices = CameraControlGizmo::COMPASS_SLICES;
    SimpleBody* part = gizmo->getParts()[CameraControlGizmo::COMPASS_REGION];
    Tube* tube = (Tube*)part->getGeometry();
    double ri = tube->getInternalRadius();
    double re = tube->getExternalRadius();
    double z0 = part->getPosition().z();
    double z1 = z0 + tube->getHeight();

    for ( int i = 0; i < slices; i++ ) {
        double a0 = 2 * Math::PI * i / slices;
        double a1 = 2 * Math::PI * (i + 1) / slices;
        double c0 = Math::cos(a0), s0 = Math::sin(a0);
        double c1 = Math::cos(a1), s1 = Math::sin(a1);
        double cm = Math::cos((a0 + a1) / 2), sm = Math::sin((a0 + a1) / 2);
        Vector3Dd q[4];

        // Caps
        q[0] = Vector3Dd(ri * c0, ri * s0, z1);
        q[1] = Vector3Dd(re * c0, re * s0, z1);
        q[2] = Vector3Dd(re * c1, re * s1, z1);
        q[3] = Vector3Dd(ri * c1, ri * s1, z1);
        addPolygon(list, gizmo, projection, viewDirection, q, 4,
            Vector3Dd(0, 0, 1), color, opacity);
        q[0] = Vector3Dd(ri * c0, ri * s0, z0);
        q[1] = Vector3Dd(ri * c1, ri * s1, z0);
        q[2] = Vector3Dd(re * c1, re * s1, z0);
        q[3] = Vector3Dd(re * c0, re * s0, z0);
        addPolygon(list, gizmo, projection, viewDirection, q, 4,
            Vector3Dd(0, 0, -1), color, opacity);
        // Walls
        q[0] = Vector3Dd(re * c0, re * s0, z0);
        q[1] = Vector3Dd(re * c1, re * s1, z0);
        q[2] = Vector3Dd(re * c1, re * s1, z1);
        q[3] = Vector3Dd(re * c0, re * s0, z1);
        addPolygon(list, gizmo, projection, viewDirection, q, 4,
            Vector3Dd(cm, sm, 0), color, opacity);
        q[0] = Vector3Dd(ri * c1, ri * s1, z0);
        q[1] = Vector3Dd(ri * c0, ri * s0, z0);
        q[2] = Vector3Dd(ri * c0, ri * s0, z1);
        q[3] = Vector3Dd(ri * c1, ri * s1, z1);
        addPolygon(list, gizmo, projection, viewDirection, q, 4,
            Vector3Dd(-cm, -sm, 0), color, opacity);
    }
}
