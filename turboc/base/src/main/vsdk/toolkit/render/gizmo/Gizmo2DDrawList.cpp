#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "java/util/Comparator.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"

/**
Order of the depth sorted primitives: from far to near.
*/
class Gizmo2DFarToNearComparator :
    public Comparator<Gizmo2DDrawList::Primitive> {
  public:
    virtual int compare(const Gizmo2DDrawList::Primitive &a,
                        const Gizmo2DDrawList::Primitive &b) const
    {
        if ( a.getDepth() > b.getDepth() ) {
            return -1;
        }
        if ( a.getDepth() < b.getDepth() ) {
            return 1;
        }
        return 0;
    }
};

Gizmo2DDrawList::Primitive::Primitive()
    : kind(Kind::POLYGON), opacity(1.0), width(0), depth(0), overlay(FALSE),
      fontSize(0), alignment(Alignment::LOWER_LEFT)
{
}

Gizmo2DDrawList::Primitive::Primitive(Kind kind,
    const ArrayList<double>& x, const ArrayList<double>& y,
    const ColorRgb& color, double opacity, double width, double depth,
    BOOLEAN overlay, const String& text, int fontSize, Alignment alignment)
    : kind(kind), x(x), y(y), color(color), opacity(opacity), width(width),
      depth(depth), overlay(overlay), text(text), fontSize(fontSize),
      alignment(alignment)
{
}

Gizmo2DDrawList::Gizmo2DDrawList()
{
}

void Gizmo2DDrawList::clear()
{
    primitives.clear();
}

BOOLEAN Gizmo2DDrawList::isEmpty() const
{
    return primitives.isEmpty();
}

void Gizmo2DDrawList::addPolygon(const ArrayList<double>& x,
    const ArrayList<double>& y, double depth, const ColorRgb& color,
    double opacity)
{
    primitives.add(Primitive(Kind::POLYGON, x, y, color, opacity, 0, depth,
        FALSE, String(), 0, Alignment::LOWER_LEFT));
}

void Gizmo2DDrawList::addLine(double x0, double y0, double x1, double y1,
    double depth, const ColorRgb& color, double width)
{
    ArrayList<double> x;
    ArrayList<double> y;

    x.add(x0);
    x.add(x1);
    y.add(y0);
    y.add(y1);
    primitives.add(Primitive(Kind::LINE, x, y, color, 1.0, width, depth,
        FALSE, String(), 0, Alignment::LOWER_LEFT));
}

void Gizmo2DDrawList::addOverlayRectangle(double x, double y, double width,
    double height, const ColorRgb& color)
{
    ArrayList<double> xs;
    ArrayList<double> ys;

    xs.add(x);
    xs.add(x + width);
    xs.add(x + width);
    xs.add(x);
    ys.add(y);
    ys.add(y);
    ys.add(y + height);
    ys.add(y + height);
    primitives.add(Primitive(Kind::POLYGON, xs, ys, color, 1.0, 0, 0, TRUE,
        String(), 0, Alignment::LOWER_LEFT));
}

void Gizmo2DDrawList::addOverlayLabel(double x, double y, const String& text,
    const ColorRgb& color, int fontSize, Alignment alignment)
{
    ArrayList<double> xs;
    ArrayList<double> ys;

    xs.add(x);
    ys.add(y);
    primitives.add(Primitive(Kind::LABEL, xs, ys, color, 1.0, 0, 0, TRUE,
        text, fontSize, alignment));
}

ArrayList<Gizmo2DDrawList::Primitive>
Gizmo2DDrawList::getPrimitivesInDrawingOrder() const
{
    ArrayList<Primitive> sorted;
    ArrayList<Primitive> overlays;
    long i;

    for ( i = 0; i < primitives.size(); i++ ) {
        const Primitive& p = primitives[i];
        if ( p.isOverlay() ) {
            overlays.add(p);
        }
        else {
            sorted.add(p);
        }
    }
    // Stable: equal depths keep the order they were added
    sorted.sort(Gizmo2DFarToNearComparator());
    sorted.addAll(overlays);
    return sorted;
}
