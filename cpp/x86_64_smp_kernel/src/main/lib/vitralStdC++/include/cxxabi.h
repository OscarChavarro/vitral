#ifndef __CXXABI__
#define __CXXABI__
#include <cstddef>
namespace __cxxabiv1 {
char *
__cxa_demangle(const char *mangled, char *buffer, size_t *length, int *status) asm("__cxa_demangle");
}
namespace abi = __cxxabiv1;

#endif
