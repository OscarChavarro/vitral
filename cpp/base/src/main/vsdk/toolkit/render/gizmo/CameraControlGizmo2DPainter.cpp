//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,          =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
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
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/render/gizmo/CameraControlGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"

const int CameraControlGizmo2DPainter::COMPASS_SLICES =
    CameraControlGizmo::COMPASS_SLICES;
const int CameraControlGizmo2DPainter::BASE_LABEL_FONT_SIZE;
const double CameraControlGizmo2DPainter::MIN_LABEL_VISIBILITY = 0.3;

CameraControlGizmo2DPainter::CameraControlGizmo2DPainter()
{
    cachedGizmo = nullptr;
    hasCachedRotation = false;
    cachedSize = -1;
    contourLines = new Calligraphic2DBuffer();
    visibleLines = new Calligraphic2DBuffer();
}

CameraControlGizmo2DPainter::~CameraControlGizmo2DPainter()
{
    clearBreps();
    delete contourLines;
    delete visibleLines;
}

void
CameraControlGizmo2DPainter::paint(Gizmo2DDrawList* list,
    CameraControlGizmo* gizmo, const ViewportElementScaler* scaler)
{
    if ( !gizmo->isVisible() ) {
        return;
    }
    prepareBreps(gizmo);
    updateHiddenLines(gizmo);

    Camera* hud = gizmo->getHudCamera();
    Matrix4x4d projection = hud->calculateProjectionMatrix();
    Vector3Dd viewDirection = hud->getFront();
    double opacity = gizmo->getOpacity();

    //- Faces, depth sorted ------------------------------------------
    for ( int r = 0; r < CameraControlGizmo::NUMBER_OF_PARTS; r++ ) {
        ColorRgb color = gizmo->getPartColor(r);
        if ( r == CameraControlGizmo::COMPASS_REGION ) {
            addCompassFaces(list, gizmo, projection, viewDirection, color,
                opacity);
        }
        else {
            addBrepFaces(list, gizmo, brepParts.get(r), projection,
                viewDirection, color, opacity);
        }
    }

    //- Visible lines from the hidden line removal --------------------
    double lineWidth = scaler != nullptr ? scaler->scaleLength(1.0) : 1.0;
    ColorRgb lineColor = gizmo->getLineColor();
    addLines(list, gizmo, visibleLines, lineColor, lineWidth);
    addLines(list, gizmo, contourLines, lineColor, 1.5 * lineWidth);

    //- Labels --------------------------------------------------------
    int fontSize = scaler != nullptr ?
        scaler->scaleSize(BASE_LABEL_FONT_SIZE) : BASE_LABEL_FONT_SIZE;
    int faces[6];
    gizmo->getFaceRegions(faces);
    for ( int i = 0; i < 6; i++ ) {
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

void
CameraControlGizmo2DPainter::clearBreps()
{
    for ( long i = 0; i < brepParts.size(); i++ ) {
        delete brepParts[i];
    }
    brepParts.clear();
}

void
CameraControlGizmo2DPainter::prepareBreps(CameraControlGizmo* gizmo)
{
    if ( cachedGizmo == gizmo ) {
        return;
    }
    cachedGizmo = gizmo;
    hasCachedRotation = false;
    clearBreps();
    for ( int p = 0; p < CameraControlGizmo::NUMBER_OF_PARTS; p++ ) {
        SimpleBody* part = gizmo->getParts().get(p);
        PolyhedralBoundedSolid* brep =
            gizmo->exportPartToPolyhedralBoundedSolid(p);
        SimpleBody* body = new SimpleBody();
        body->setGeometry(brep);
        body->setPosition(part->getPosition());
        body->setRotation(Matrix4x4d());
        body->setRotationInverse(Matrix4x4d());
        body->setScale(Vector3Dd(1, 1, 1));
        // C++ port: a copy, as the gizmo replaces the materials of its parts
        body->setMaterial(new SimpleMaterial(*part->getMaterial()));
        brepParts.add(body);
    }
}

void
CameraControlGizmo2DPainter::updateHiddenLines(CameraControlGizmo* gizmo)
{
    Camera* hud = gizmo->getHudCamera();
    Matrix4x4d rotation = hud->getRotation();

    if ( hasCachedRotation && cachedSize == gizmo->getSizeInPixels() &&
         cachedRotation.epsilonEquals(rotation, 1e-9) ) {
        return;
    }
    hasCachedRotation = true;
    cachedRotation = rotation;
    cachedSize = gizmo->getSizeInPixels();
    delete contourLines;
    delete visibleLines;
    contourLines = new Calligraphic2DBuffer();
    visibleLines = new Calligraphic2DBuffer();
    Calligraphic2DBuffer hiddenLines;

    HiddenLineRenderer::executeAppelAlgorithm(brepParts, hud, contourLines,
        visibleLines, &hiddenLines);
}

//= Primitives ============================================================

void
CameraControlGizmo2DPainter::addLines(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, const Calligraphic2DBuffer* lines,
    const ColorRgb& color, double width)
{
    double size = gizmo->getSizeInPixels();

    for ( int i = 0; i < lines->getNumLines(); i++ ) {
        double x0;
        double y0;
        double x1;
        double y1;
        if ( !lines->get2DLine(i, x0, y0, x1, y1) ) {
            continue;
        }
        list->addOverlayLine(
            gizmo->getHudX() + (x0 + 1.0) / 2.0 * size,
            gizmo->getHudY() + (1.0 - y0) / 2.0 * size,
            gizmo->getHudX() + (x1 + 1.0) / 2.0 * size,
            gizmo->getHudY() + (1.0 - y1) / 2.0 * size,
            color, width);
    }
}

double
CameraControlGizmo2DPainter::lambert(const Vector3Dd& normal,
    const Vector3Dd& viewDirection)
{
    double c = -normal.normalized().dotProduct(viewDirection.normalized());
    return 0.65 + 0.35 * java::Math::max(0.0, c);
}

void
CameraControlGizmo2DPainter::addPolygon(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, const Matrix4x4d& projection,
    const Vector3Dd& viewDirection, const java::ArrayList<Vector3Dd>& vertices,
    const Vector3Dd& normal, const ColorRgb& color, double opacity)
{
    if ( normal.dotProduct(viewDirection) >= 0.0 ) {
        return;
    }
    long n = vertices.size();
    java::ArrayList<double> x;
    java::ArrayList<double> y;
    double depth = 0;
    double size = gizmo->getSizeInPixels();

    for ( long i = 0; i < n; i++ ) {
        Vector4Dd ndc = projection.multiply(Vector4Dd(vertices.get(i))).dividedByW();
        x.add(gizmo->getHudX() + (ndc.x() + 1.0) / 2.0 * size);
        y.add(gizmo->getHudY() + (1.0 - ndc.y()) / 2.0 * size);
        depth += ndc.z();
    }
    list->addPolygon(x, y, depth / n,
        color.multiply(lambert(normal, viewDirection)), opacity);
}

void
CameraControlGizmo2DPainter::addBrepFaces(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, SimpleBody* body,
    const Matrix4x4d& projection, const Vector3Dd& viewDirection,
    const ColorRgb& color, double opacity)
{
    PolyhedralBoundedSolid* solid =
        static_cast<PolyhedralBoundedSolid*>(body->getGeometry());
    Vector3Dd offset = body->getPosition();

    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        if ( face->boundariesList.size() != 1 ) {
            continue;
        }
        java::ArrayList<Vector3Dd> points;
        _PolyhedralBoundedSolidHalfEdge* start =
            face->boundariesList.get(0)->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        do {
            points.add(he->startingVertex->position.add(offset));
            he = he->next();
        } while ( he != start );
        addPolygon(list, gizmo, projection, viewDirection, points,
            face->containingPlaneNormal(), color, opacity);
    }
}

void
CameraControlGizmo2DPainter::addCompassFaces(Gizmo2DDrawList* list,
    const CameraControlGizmo* gizmo, const Matrix4x4d& projection,
    const Vector3Dd& viewDirection, const ColorRgb& color, double opacity)
{
    SimpleBody* part = gizmo->getParts().get(CameraControlGizmo::COMPASS_REGION);
    Tube* tube = static_cast<Tube*>(part->getGeometry());
    double ri = tube->getInternalRadius();
    double re = tube->getExternalRadius();
    double z0 = part->getPosition().z();
    double z1 = z0 + tube->getHeight();

    for ( int i = 0; i < COMPASS_SLICES; i++ ) {
        double a0 = 2 * java::Math::PI * i / COMPASS_SLICES;
        double a1 = 2 * java::Math::PI * (i + 1) / COMPASS_SLICES;
        double c0 = java::Math::cos(a0);
        double s0 = java::Math::sin(a0);
        double c1 = java::Math::cos(a1);
        double s1 = java::Math::sin(a1);
        double cm = java::Math::cos((a0 + a1) / 2);
        double sm = java::Math::sin((a0 + a1) / 2);
        java::ArrayList<Vector3Dd> quad;

        // Caps
        quad.clear();
        quad.add(Vector3Dd(ri * c0, ri * s0, z1));
        quad.add(Vector3Dd(re * c0, re * s0, z1));
        quad.add(Vector3Dd(re * c1, re * s1, z1));
        quad.add(Vector3Dd(ri * c1, ri * s1, z1));
        addPolygon(list, gizmo, projection, viewDirection, quad,
            Vector3Dd(0, 0, 1), color, opacity);
        quad.clear();
        quad.add(Vector3Dd(ri * c0, ri * s0, z0));
        quad.add(Vector3Dd(ri * c1, ri * s1, z0));
        quad.add(Vector3Dd(re * c1, re * s1, z0));
        quad.add(Vector3Dd(re * c0, re * s0, z0));
        addPolygon(list, gizmo, projection, viewDirection, quad,
            Vector3Dd(0, 0, -1), color, opacity);
        // Walls
        quad.clear();
        quad.add(Vector3Dd(re * c0, re * s0, z0));
        quad.add(Vector3Dd(re * c1, re * s1, z0));
        quad.add(Vector3Dd(re * c1, re * s1, z1));
        quad.add(Vector3Dd(re * c0, re * s0, z1));
        addPolygon(list, gizmo, projection, viewDirection, quad,
            Vector3Dd(cm, sm, 0), color, opacity);
        quad.clear();
        quad.add(Vector3Dd(ri * c1, ri * s1, z0));
        quad.add(Vector3Dd(ri * c0, ri * s0, z0));
        quad.add(Vector3Dd(ri * c0, ri * s0, z1));
        quad.add(Vector3Dd(ri * c1, ri * s1, z1));
        addPolygon(list, gizmo, projection, viewDirection, quad,
            Vector3Dd(-cm, -sm, 0), color, opacity);
    }
}
