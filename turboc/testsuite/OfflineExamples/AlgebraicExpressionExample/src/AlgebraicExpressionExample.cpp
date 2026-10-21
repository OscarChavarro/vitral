//= This example serves as a testbed for AlgebraicExpression class.           =

#include <cstdio>

#include "java/lang/Double.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.h"
#include "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpressionException.h"

int main(int argc, char** argv)
{
    AlgebraicExpression regexp;

    try {
        if ( argc <= 1 ) {
            regexp.setExpression("666.0");
        }
        else {
            java::String joined = "";
            int i;
            for ( i = 1; i < argc; i++ ) {
                joined += argv[i];
                if ( i < argc - 1 ) {
                    joined += " ";
                }
            }
            std::printf("Parsing from %d parameters with regexp \"%s\"\n",
                        argc - 1, joined.c_str());
            regexp.setExpression(joined);
        }
        std::printf("REGEXP:\n%s\n", regexp.toString().c_str());
        std::printf("REGEXP VALUE:\n%s\n",
                    java::Double::toString(regexp.eval()).c_str());
    }
    catch ( const AlgebraicExpressionException& e ) {
        // Same text as the Java version prints for the exception
        std::printf("Error processing regular expression."
                    "vsdk.toolkit.common.symbolicAlgebra.AlgebraicExpressionException: %s\n",
                    e.what());
    }
    return 0;
}
