#ifndef __GCC2_TYPE_INFO__
#define __GCC2_TYPE_INFO__

/**
Forced into every file by the legacy Makefiles with gcc 2.x (`-include`):
gcc 2.7.2 only catches an exception by a base class with RTTI (-frtti), and
then it needs the type_info declarations in each file. The port itself does
not use RTTI.
*/
#include <typeinfo>

#endif
