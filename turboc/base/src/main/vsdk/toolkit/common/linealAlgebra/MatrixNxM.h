#ifndef __MATRIX_NX_M__
#define __MATRIX_NX_M__
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
class MatrixNxM {
private:
    int numRows_;
    int numColumns_;
    double* m_;

    int index(int row, int column) const;

public:
    MatrixNxM(int n, int m);
    MatrixNxM(const MatrixNxM& other);
    ~MatrixNxM();
    MatrixNxM& operator=(const MatrixNxM& other);

    static MatrixNxM copyOf(const MatrixNxM& other);

    MatrixNxM identity() const;
    int getNumRows() const;
    int getNumColumns() const;
    double getVal(int row, int column) const;
    MatrixNxM withVal(int row, int column, double val) const;

    MatrixNxM inverse() const;
    MatrixNxM cofactors() const;
    MatrixNxM transpose() const;
    MatrixNxM multiply(double a) const;
    MatrixNxM multiply(const MatrixNxM& other) const;
    MatrixNxM buildMinor(int row, int column) const;
    double determinant() const;
    String* toString() const;

    BOOLEAN epsilonEquals(const MatrixNxM& other) const;
    BOOLEAN epsilonEquals(const MatrixNxM& other, double epsilon) const;

    BOOLEAN operator==(const MatrixNxM& other) const;
    BOOLEAN equals(const MatrixNxM& other) const { return (*this) == other; }
    int hashCode() const;
};

/** Equality of the elements of ArrayList (see vsdkEquals in ArrayList.h) */
inline BOOLEAN
vsdkEquals(const MatrixNxM &a, const MatrixNxM &b)
{
    return a == b;
}


#endif
