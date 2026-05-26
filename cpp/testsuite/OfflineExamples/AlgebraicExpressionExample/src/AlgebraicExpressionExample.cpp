#include <cstdio>
#include <sstream>
#include "java/lang/String.h"

#include "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.h"
#include "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpressionException.h"

int main(int argc, char** argv)
{
    AlgebraicExpression regexp;
    try {
        java::String expression;
        if (argc <= 1) {
            expression = "666.0";
        }
        else {
            std::ostringstream joined;
            for (int i = 1; i < argc; i++) {
                joined << argv[i];
                if (i < argc - 1) joined << " ";
            }
            expression = joined.str();
            std::printf(
                "Parsing from %d parameters with regexp \"%s\"\n",
                argc - 1,
                expression.c_str());
        }

        regexp.setExpression(expression);
        std::printf("REGEXP:\n%s\n", expression.c_str());
        std::printf("REGEXP VALUE:\n%.17g\n", regexp.eval());
    }
    catch (const AlgebraicExpressionException& e) {
        std::printf("Error processing regular expression. %s\n", e.what());
        return 1;
    }
    catch (const std::exception& e) {
        std::printf("Error processing regular expression. %s\n", e.what());
        return 1;
    }
    catch (...) {
        std::printf("Error processing regular expression.\n");
        return 1;
    }
    return 0;
}
