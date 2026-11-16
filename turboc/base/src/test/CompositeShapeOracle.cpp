#include "CompositeShapeOracle.h"

#include "java/util/ArrayList.txx"
#include "java/util/Comparator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;

CompositeBox::CompositeBox()
{
    int i;

    for ( i = 0; i < 6; i++ ) {
        v[i] = 0.0;
    }
}

CompositeBox::CompositeBox(double x0, double y0, double z0, double x1,
    double y1, double z1)
{
    v[0] = x0;
    v[1] = y0;
    v[2] = z0;
    v[3] = x1;
    v[4] = y1;
    v[5] = z1;
}

BOOLEAN CompositeBox::operator==(const CompositeBox& other) const
{
    int i;

    for ( i = 0; i < 6; i++ ) {
        if ( v[i] != other.v[i] ) {
            return FALSE;
        }
    }
    return TRUE;
}

CompositeShapeOracle::Shape::Shape()
{
}

CompositeShapeOracle::Shape::Shape(const String& inName,
    const ArrayList<CompositeBox>& inPositive,
    const ArrayList<CompositeBox>& inNegative, const Matrix4x4d& inFrame) :
    name(inName), positive(inPositive), negative(inNegative), frame(inFrame)
{
}

CompositeShapeOracle::Shape CompositeShapeOracle::Shape::moved(
    const Matrix4x4d& motion) const
{
    return Shape(name, positive, negative, motion.multiply(frame));
}

ArrayList<OrientedBox> CompositeShapeOracle::Shape::cells() const
{
    ArrayList<OrientedBox> cells;
    ArrayList<double> xs = coordinates(0);
    ArrayList<double> ys = coordinates(1);
    ArrayList<double> zs = coordinates(2);
    long i;
    long j;
    long k;

    for ( i = 0; i + 1 < xs.size(); i++ ) {
        for ( j = 0; j + 1 < ys.size(); j++ ) {
            for ( k = 0; k + 1 < zs.size(); k++ ) {
                double c[3];
                c[0] = (xs[i] + xs[i + 1]) / 2.0;
                c[1] = (ys[j] + ys[j + 1]) / 2.0;
                c[2] = (zs[k] + zs[k + 1]) / 2.0;
                if ( !insideLocal(c) ) {
                    continue;
                }
                cells.add(OrientedBox(
                    xs[i + 1] - xs[i],
                    ys[j + 1] - ys[j],
                    zs[k + 1] - zs[k],
                    frame.multiply(Oracle::translation(c[0], c[1], c[2]))));
            }
        }
    }
    return cells;
}

BOOLEAN CompositeShapeOracle::Shape::insideLocal(const double* p) const
{
    BOOLEAN in = FALSE;
    long i;

    for ( i = 0; i < positive.size(); i++ ) {
        in = in || insideBox(positive[i], p);
    }
    for ( i = 0; i < negative.size(); i++ ) {
        in = in && !insideBox(negative[i], p);
    }
    return in;
}

BOOLEAN CompositeShapeOracle::Shape::insideBox(const CompositeBox& b,
    const double* p)
{
    return p[0] > b.v[0] && p[0] < b.v[3] && p[1] > b.v[1] &&
        p[1] < b.v[4] && p[2] > b.v[2] && p[2] < b.v[5];
}

/**
Java `new ArrayList<>(treeSet)`: the distinct values in increasing order.
*/
static void
addDistinct(ArrayList<double>& values, double value)
{
    if ( !values.contains(value) ) {
        values.add(value);
    }
}

ArrayList<double> CompositeShapeOracle::Shape::coordinates(int axis) const
{
    ArrayList<double> values;
    long i;

    for ( i = 0; i < positive.size(); i++ ) {
        addDistinct(values, positive[i].v[axis]);
        addDistinct(values, positive[i].v[axis + 3]);
    }
    for ( i = 0; i < negative.size(); i++ ) {
        addDistinct(values, negative[i].v[axis]);
        addDistinct(values, negative[i].v[axis + 3]);
    }
    values.sort(NaturalOrderComparator<double>());
    return values;
}

PolyhedralBoundedSolid* CompositeShapeOracle::Shape::toSolid() const
{
    PolyhedralBoundedSolid* solid = box(positive[0]);
    PolyhedralBoundedSolid* operand;
    PolyhedralBoundedSolid* next;
    long i;

    // C++ port note: Java leaves the intermediate solids to the garbage
    // collector; here each one is deleted once the next exists
    for ( i = 1; i < positive.size(); i++ ) {
        operand = box(positive[i]);
        next = Modeler::setOp(solid, operand, Modeler::UNION);
        delete solid;
        delete operand;
        solid = next;
    }
    for ( i = 0; i < negative.size(); i++ ) {
        operand = box(negative[i]);
        next = Modeler::setOp(solid, operand, Modeler::SUBTRACT);
        delete solid;
        delete operand;
        solid = next;
    }
    Modeler::applyTransformation(solid, frame);
    return solid;
}

static BOOLEAN
firstMember(const BOOLEAN* m)
{
    return m[0];
}

double CompositeShapeOracle::Shape::volume() const
{
    ArrayList<Shape> self;

    self.add(*this);
    return CompositeShapeOracle::volume(self, firstMember);
}

String CompositeShapeOracle::Shape::toString() const
{
    return name;
}

BOOLEAN CompositeShapeOracle::Shape::operator==(const Shape& other) const
{
    return this == &other;
}

PolyhedralBoundedSolid* CompositeShapeOracle::Shape::box(
    const CompositeBox& b)
{
    return OrientedBox(b.v[3] - b.v[0], b.v[4] - b.v[1], b.v[5] - b.v[2],
        Oracle::translation((b.v[0] + b.v[3]) / 2.0,
            (b.v[1] + b.v[4]) / 2.0, (b.v[2] + b.v[5]) / 2.0)).toSolid();
}

/** Java `Integer.bitCount`. */
static int
bitCount(int value)
{
    int count = 0;

    while ( value != 0 ) {
        count += value & 1;
        value >>= 1;
    }
    return count;
}

double CompositeShapeOracle::volume(const ArrayList<Shape>& shapes,
    CompositeMembership expression)
{
    int n = (int)shapes.size();
    ArrayList<OrientedBox> cells[MAXIMUM_SHAPES];
    ArrayList<double> intersection;
    double total = 0.0;
    int set;
    int i;

    for ( i = 0; i < n; i++ ) {
        cells[i] = shapes[i].cells();
    }
    intersection.add(0.0);
    for ( set = 1; set < (1 << n); set++ ) {
        intersection.add(intersectionVolume(cells, n, set));
    }
    for ( set = 1; set < (1 << n); set++ ) {
        BOOLEAN member[MAXIMUM_SHAPES];
        for ( i = 0; i < n; i++ ) {
            member[i] = (set & (1 << i)) != 0;
        }
        if ( !expression(member) ) {
            continue;
        }
        // Points in exactly the shapes of `set`: Moebius inversion.
        int superset;
        double exact = 0.0;
        for ( superset = set; superset < (1 << n); superset++ ) {
            if ( (superset & set) != set ) {
                continue;
            }
            int extra = bitCount(superset) - bitCount(set);
            exact += ((extra % 2) == 0 ? 1.0 : -1.0) *
                intersection[superset];
        }
        total += exact;
    }
    return total;
}

double CompositeShapeOracle::intersectionVolume(
    const ArrayList<OrientedBox>* cells, int count, int set)
{
    const ArrayList<OrientedBox>* chosen[MAXIMUM_SHAPES];
    int chosenCount = 0;
    int i;
    ArrayList<OracleHalfSpace> planes;

    for ( i = 0; i < count; i++ ) {
        if ( (set & (1 << i)) != 0 ) {
            chosen[chosenCount] = &cells[i];
            chosenCount++;
        }
    }
    return sumOverTuples(chosen, chosenCount, 0, planes);
}

double CompositeShapeOracle::sumOverTuples(
    const ArrayList<OrientedBox>** chosen, int chosenCount, int depth,
    const ArrayList<OracleHalfSpace>& planes)
{
    if ( depth == chosenCount ) {
        return Oracle::convexVolume(planes);
    }
    double sum = 0.0;
    long i;
    for ( i = 0; i < chosen[depth]->size(); i++ ) {
        ArrayList<OracleHalfSpace> next = planes;
        next.addAll((*chosen[depth])[i].halfSpaces());
        sum += sumOverTuples(chosen, chosenCount, depth + 1, next);
    }
    return sum;
}
