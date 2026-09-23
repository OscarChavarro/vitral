#ifndef __DOUBLE__
#define __DOUBLE__

#include "java/lang/String.h"

namespace java {

class Double {
  public:
    static constexpr double MIN_VALUE = 4.9406564584124654e-324;
    static constexpr double MAX_VALUE = 1.7976931348623157e+308;

    /**
    Emulation of `Double.parseDouble`: leading and trailing white space is
    ignored.
    @param text text with a decimal number
    @return the number
    @throws NumberFormatException if the text is not a number
    */
    static double parseDouble(const java::String& text);

    /**
    Emulation of `Double.toString`: the shortest decimal text that reads
    back as the same number, with at least one decimal digit ("10.0"), in
    computerized scientific notation ("1.0E7") for magnitudes out of
    [1e-3, 1e7).
    @param value number to write
    @return the text of the number
    */
    static java::String toString(double value);
};

}

#endif
