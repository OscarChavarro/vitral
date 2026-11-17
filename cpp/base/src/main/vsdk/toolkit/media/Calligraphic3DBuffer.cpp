#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/media/Calligraphic3DBuffer.h"

Calligraphic3DBuffer::Calligraphic3DBuffer() : lineData(LINE_SIZE * 1000)
{
}

void Calligraphic3DBuffer::init()
{
    lineData.clear();
}

void Calligraphic3DBuffer::add3DLine(const Vector3Dd& p0, const Vector3Dd& p1)
{
    add3DLine(p0.x(), p0.y(), p0.z(), p1.x(), p1.y(), p1.z());
}

void Calligraphic3DBuffer::add3DLine(double x0, double y0, double z0,
                                     double x1, double y1, double z1)
{
    lineData.add(x0);
    lineData.add(y0);
    lineData.add(z0);
    lineData.add(x1);
    lineData.add(y1);
    lineData.add(z1);
    lineData.add(0.0);  // R
    lineData.add(0.0);  // G
    lineData.add(0.0);  // B
    lineData.add(1.0); // Width
}

void Calligraphic3DBuffer::get3DLine(int i, Vector3Dd& outP0,
                                     Vector3Dd& outP1) const
{
    long base = (long)LINE_SIZE * i;
    outP0 = Vector3Dd(lineData.get(base),
        lineData.get(base + 1), lineData.get(base + 2));
    outP1 = Vector3Dd(lineData.get(base + 3),
        lineData.get(base + 4), lineData.get(base + 5));
}

int Calligraphic3DBuffer::getNumLines() const
{
    return (int)(lineData.size() / LINE_SIZE);
}
