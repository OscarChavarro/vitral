#ifndef __PBS_TEST_FAILURES__
#define __PBS_TEST_FAILURES__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Exception.h"
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
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SolidOperationFailedException.h"

/**
Test helper: the catch clauses that give Java's
`t.getClass().getSimpleName()` and `t.getMessage()` of the exceptions the
polyhedral kernel throws, most derived class first. Written after a `try`
block as `PBS_CATCH_THROWABLE(name, message)`, with two `String` variables.

C++ port note: the legacy compilers have no RTTI (Turbo C++ 3.0), so the
class is told by the catch clause that takes it. Java's `catch (Throwable)`
also takes errors of the virtual machine; here `catch (...)` stands for
them, named "Throwable". Without exceptions (VSDK_WITHOUT_EXCEPTIONS) the
macros are empty (a failure then ends the program).
*/
#ifndef VSDK_WITHOUT_EXCEPTIONS
#define PBS_CATCH_NAMED(type, name, message) \
    catch ( const type & pbsException ) { \
        name = #type; \
        message = pbsException.getMessage(); \
    }

#define PBS_CATCH_OPERATION(name, message) \
    PBS_CATCH_NAMED(NonManifoldBooleanResultException, name, message) \
    PBS_CATCH_OPERATION_REST(name, message)

#define PBS_CATCH_OPERATION_REST(name, message) \
    PBS_CATCH_NAMED(InvalidSolidResultException, name, message) \
    PBS_CATCH_NAMED(InvalidOperandException, name, message) \
    PBS_CATCH_NAMED(SolidOperationFailedException, name, message) \
    PBS_CATCH_NAMED(PolyhedralBoundedSolidOperationException, name, message)

#define PBS_CATCH_THROWABLE(name, message) \
    PBS_CATCH_NAMED(NonManifoldBooleanResultException, name, message) \
    PBS_CATCH_THROWABLE_REST(name, message)

/** The clauses of PBS_CATCH_THROWABLE after a NonManifold... one. */
#define PBS_CATCH_THROWABLE_REST(name, message) \
    PBS_CATCH_OPERATION_REST(name, message) \
    PBS_CATCH_NAMED(VSDKFatalException, name, message) \
    PBS_CATCH_NAMED(IndexOutOfBoundsException, name, message) \
    PBS_CATCH_NAMED(IllegalArgumentException, name, message) \
    PBS_CATCH_NAMED(IllegalStateException, name, message) \
    PBS_CATCH_NAMED(RuntimeException, name, message) \
    PBS_CATCH_NAMED(Exception, name, message) \
    PBS_CATCH_NAMED(Throwable, name, message) \
    catch ( ... ) { \
        name = "Throwable"; \
        message = ""; \
    }
#else
#define PBS_CATCH_OPERATION(name, message)
#define PBS_CATCH_OPERATION_REST(name, message)
#define PBS_CATCH_THROWABLE(name, message)
#define PBS_CATCH_THROWABLE_REST(name, message)
#endif

#endif
