//= This example serves as a testbed for AlgebraicExpression class.           =

#include <stdio.h>

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
            String joined = "";
            int i;
            for ( i = 1; i < argc; i++ ) {
                joined += argv[i];
                if ( i < argc - 1 ) {
                    joined += " ";
                }
            }
            printf("Parsing from %d parameters with regexp \"%s\"\n",
                        argc - 1, joined.c_str());
            regexp.setExpression(joined);
        }
        printf("REGEXP:\n%s\n", regexp.toString().c_str());
        printf("REGEXP VALUE:\n%s\n",
                    Double::toString(regexp.eval()).c_str());
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( const AlgebraicExpressionException& e ) {
        // Same text as the Java version prints for the exception
        printf("Error processing regular expression."
                    "vsdk.toolkit.common.symbolicAlgebra.AlgebraicExpressionException: %s\n",
                    e.getMessage().c_str());
    }
#endif
    return 0;
}
