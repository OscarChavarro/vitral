#include "java/lang/Math.h"
#include "vsdk/toolkit/numericalAnalysis/lookUpTables/LookUpTableSine.h"
LookUpTableSine::LookUpTableSine()
{
    for (int i = 0; i < SIZE; i++) {
        table[i] = Math::sin(i / (double)SIZE * (Math::PI * 2.0));
    }
}

LookUpTableSine::LookUpTableSine(int numberOfApproximationDecimals)
{
    const double scale = Math::pow(10.0, numberOfApproximationDecimals);
    const double pi = Math::round(Math::PI * scale) / scale;

    for (int i = 0; i < SIZE; i++) {
        table[i] = Math::sin(i / (double)SIZE * (pi * 2.0));
    }
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
