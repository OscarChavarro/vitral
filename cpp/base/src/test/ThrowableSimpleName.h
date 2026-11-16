#ifndef __THROWABLE_SIMPLE_NAME__
#define __THROWABLE_SIMPLE_NAME__

#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/IndexOutOfBoundsException.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/String.h"
#include "java/lang/Throwable.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/InvalidOperandException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/InvalidSolidResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SolidOperationFailedException.h"

/**
Test helper: Java `t.getClass().getSimpleName()` for the exceptions the
kernel throws (most derived class first).
*/
inline java::String throwableSimpleName(const java::Throwable& t)
{
    if ( dynamic_cast<const NonManifoldBooleanResultException*>(&t) ) {
        return "NonManifoldBooleanResultException";
    }
    if ( dynamic_cast<const InvalidSolidResultException*>(&t) ) {
        return "InvalidSolidResultException";
    }
    if ( dynamic_cast<const InvalidOperandException*>(&t) ) {
        return "InvalidOperandException";
    }
    if ( dynamic_cast<const SolidOperationFailedException*>(&t) ) {
        return "SolidOperationFailedException";
    }
    if ( dynamic_cast<const PolyhedralBoundedSolidOperationException*>(&t) ) {
        return "PolyhedralBoundedSolidOperationException";
    }
    if ( dynamic_cast<const VSDKFatalException*>(&t) ) {
        return "VSDKFatalException";
    }
    if ( dynamic_cast<const java::IndexOutOfBoundsException*>(&t) ) {
        return "IndexOutOfBoundsException";
    }
    if ( dynamic_cast<const java::IllegalArgumentException*>(&t) ) {
        return "IllegalArgumentException";
    }
    if ( dynamic_cast<const java::IllegalStateException*>(&t) ) {
        return "IllegalStateException";
    }
    if ( dynamic_cast<const java::RuntimeException*>(&t) ) {
        return "RuntimeException";
    }
    return "Throwable";
}

#endif
