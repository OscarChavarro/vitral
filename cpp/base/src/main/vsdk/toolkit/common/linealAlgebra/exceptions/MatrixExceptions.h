#ifndef __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_EXCEPTIONS_MATRIXEXCEPTIONS_H__
#define __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_EXCEPTIONS_MATRIXEXCEPTIONS_H__

#include <stdexcept>
#include "java/lang/String.h"
#include <string>
#include "java/lang/String.h"

class MatrixDimensionMismatchException : public std::invalid_argument {
public:
    explicit MatrixDimensionMismatchException(const java::String& message) : std::invalid_argument(message) {}
};

class MatrixIndexOutOfBoundsException : public std::out_of_range {
public:
    explicit MatrixIndexOutOfBoundsException(const java::String& message) : std::out_of_range(message) {}
};

class MatrixNotSquareException : public std::invalid_argument {
public:
    explicit MatrixNotSquareException(const java::String& message) : std::invalid_argument(message) {}
};

class MatrixSingularException : public std::runtime_error {
public:
    explicit MatrixSingularException(const java::String& message) : std::runtime_error(message) {}
};

#endif // __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_EXCEPTIONS_MATRIXEXCEPTIONS_H__
