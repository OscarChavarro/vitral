#include "CompositeShapeOracle.h"

#include "java/util/ArrayList.txx"
#include "java/util/TreeSet.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef java::ArrayList<OrientedBox> CellList;

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

double& CompositeBox::operator[](int i)
{
    return v[i];
}

double CompositeBox::operator[](int i) const
{
    return v[i];
}

bool CompositeBox::operator==(const CompositeBox& other) const
{
    int i;
    for ( i = 0; i < 6; i++ ) {
        if ( v[i] != other.v[i] ) {
            return false;
        }
    }
    return true;
}

CompositeShapeOracle::Shape::Shape()
{
}

CompositeShapeOracle::Shape::Shape(const java::String& name,
    const java::ArrayList<CompositeBox>& positive,
    const java::ArrayList<CompositeBox>& negative,
    const Matrix4x4d& frame) :
    name(name), positive(positive), negative(negative), frame(frame)
{
}

bool CompositeShapeOracle::Shape::operator==(const Shape& other) const
{
    return this == &other;
}

CompositeShapeOracle::Shape CompositeShapeOracle::Shape::moved(
    const Matrix4x4d& motion) const
{
    return Shape(name, positive, negative, motion.multiply(frame));
}

CellList CompositeShapeOracle::Shape::cells() const
{
    CellList cells;
    java::ArrayList<double> xs = coordinates(0);
    java::ArrayList<double> ys = coordinates(1);
    java::ArrayList<double> zs = coordinates(2);
    long i;
    long j;
    long k;

    for ( i = 0; i + 1 < xs.size(); i++ ) {
        for ( j = 0; j + 1 < ys.size(); j++ ) {
            for ( k = 0; k + 1 < zs.size(); k++ ) {
                double c[3] = {
                    (xs[i] + xs[i + 1]) / 2.0,
                    (ys[j] + ys[j + 1]) / 2.0,
                    (zs[k] + zs[k + 1]) / 2.0 };
                if ( !insideLocal(c) ) {
                    continue;
                }
                cells.add(OrientedBox(
                    xs[i + 1] - xs[i],
                    ys[j + 1] - ys[j],
                    zs[k + 1] - zs[k],
                    frame.multiply(PolyhedralBoundedSolidVolumeOracle
                        ::translation(c[0], c[1], c[2]))));
            }
        }
    }
    return cells;
}

bool CompositeShapeOracle::Shape::insideLocal(const double* p) const
{
    bool in = false;
    long i;

    for ( i = 0; i < positive.size(); i++ ) {
        in |= insideBox(positive[i], p);
    }
    for ( i = 0; i < negative.size(); i++ ) {
        in &= !insideBox(negative[i], p);
    }
    return in;
}

bool CompositeShapeOracle::Shape::insideBox(const CompositeBox& b,
    const double* p)
{
    return p[0] > b[0] && p[0] < b[3] && p[1] > b[1] &&
        p[1] < b[4] && p[2] > b[2] && p[2] < b[5];
}

java::ArrayList<double> CompositeShapeOracle::Shape::coordinates(
    int axis) const
{
    java::TreeSet<double> values;
    java::ArrayList<double> list;
    long i;

    for ( i = 0; i < positive.size(); i++ ) {
        values.add(positive[i][axis]);
        values.add(positive[i][axis + 3]);
    }
    for ( i = 0; i < negative.size(); i++ ) {
        values.add(negative[i][axis]);
        values.add(negative[i][axis + 3]);
    }
    for ( i = 0; i < values.size(); i++ ) {
        list.add(values.get(i));
    }
    return list;
}

PolyhedralBoundedSolid* CompositeShapeOracle::Shape::toSolid() const
{
    PolyhedralBoundedSolid* solid = box(positive[0]);
    long i;

    for ( i = 1; i < positive.size(); i++ ) {
        PolyhedralBoundedSolid* other = box(positive[i]);
        PolyhedralBoundedSolid* next = nullptr;
        try {
            next = PolyhedralBoundedSolidModeler::setOp(solid, other,
                PolyhedralBoundedSolidModeler::UNION);
        }
        catch ( ... ) {
            delete solid;
            delete other;
            throw;
        }
        // C++ ownership: the operands are released (Java: garbage collector)
        delete solid;
        delete other;
        solid = next;
    }
    for ( i = 0; i < negative.size(); i++ ) {
        PolyhedralBoundedSolid* other = box(negative[i]);
        PolyhedralBoundedSolid* next = nullptr;
        try {
            next = PolyhedralBoundedSolidModeler::setOp(solid, other,
                PolyhedralBoundedSolidModeler::SUBTRACT);
        }
        catch ( ... ) {
            delete solid;
            delete other;
            throw;
        }
        delete solid;
        delete other;
        solid = next;
    }
    PolyhedralBoundedSolidModeler::applyTransformation(solid, frame);
    return solid;
}

static bool firstMember(const bool* m)
{
    return m[0];
}

double CompositeShapeOracle::Shape::volume() const
{
    java::ArrayList<Shape> self;
    self.add(*this);
    return CompositeShapeOracle::volume(self, firstMember);
}

java::String CompositeShapeOracle::Shape::toString() const
{
    return name;
}

PolyhedralBoundedSolid* CompositeShapeOracle::Shape::box(
    const CompositeBox& b)
{
    return OrientedBox(b[3] - b[0], b[4] - b[1], b[5] - b[2],
        PolyhedralBoundedSolidVolumeOracle::translation(
            (b[0] + b[3]) / 2.0, (b[1] + b[4]) / 2.0,
            (b[2] + b[5]) / 2.0)).toSolid();
}

static int bitCount(int value)
{
    int count = 0;
    unsigned int bits = (unsigned int)value;
    while ( bits != 0 ) {
        count += (int)(bits & 1u);
        bits >>= 1;
    }
    return count;
}

double CompositeShapeOracle::volume(const java::ArrayList<Shape>& shapes,
    Membership expression)
{
    int n = (int)shapes.size();
    java::ArrayList<CellList> cells;
    double* intersection = new double[1 << n];
    double total = 0.0;
    int set;
    long s;

    for ( s = 0; s < shapes.size(); s++ ) {
        cells.add(shapes[s].cells());
    }
    for ( set = 1; set < (1 << n); set++ ) {
        intersection[set] = intersectionVolume(cells, set);
    }
    for ( set = 1; set < (1 << n); set++ ) {
        bool* member = new bool[n];
        int i;
        for ( i = 0; i < n; i++ ) {
            member[i] = (set & (1 << i)) != 0;
        }
        bool selected = expression(member);
        delete[] member;
        if ( !selected ) {
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
    delete[] intersection;
    return total;
}

double CompositeShapeOracle::intersectionVolume(
    const java::ArrayList<CellList>& cells, int set)
{
    java::ArrayList<CellList> chosen;
    long i;

    for ( i = 0; i < cells.size(); i++ ) {
        if ( (set & (1 << i)) != 0 ) {
            chosen.add(cells[i]);
        }
    }
    return sumOverTuples(chosen, 0, java::ArrayList<OracleHalfSpace>());
}

double CompositeShapeOracle::sumOverTuples(
    const java::ArrayList<CellList>& chosen, int depth,
    const java::ArrayList<OracleHalfSpace>& planes)
{
    if ( depth == chosen.size() ) {
        return PolyhedralBoundedSolidVolumeOracle::convexVolume(planes);
    }
    double sum = 0.0;
    long c;
    for ( c = 0; c < chosen[depth].size(); c++ ) {
        java::ArrayList<OracleHalfSpace> next = planes;
        next.addAll(chosen[depth][c].halfSpaces());
        sum += sumOverTuples(chosen, depth + 1, next);
    }
    return sum;
}
