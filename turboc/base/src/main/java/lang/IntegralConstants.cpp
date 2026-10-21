#include "java/lang/Byte.h"
#include "java/lang/Integer.h"
#include "java/lang/Long.h"
#include "java/lang/Short.h"
#include "java/util/Random.h"

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const char Byte::MIN_VALUE;
const char Byte::MAX_VALUE;
const int Integer::MIN_VALUE;
const int Integer::MAX_VALUE;
const long long Long::MIN_VALUE;
const long long Long::MAX_VALUE;
const short Short::MIN_VALUE;
const short Short::MAX_VALUE;
const unsigned long long Random::MULTIPLIER;
const unsigned long long Random::ADDEND;
const unsigned long long Random::MASK;
