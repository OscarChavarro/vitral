#include <stddef.h>
#include <math.h>

#include "java/lang/Double.h"
#include "java/lang/NumberFormatException.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.h"
#include "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpressionException.h"
#include "vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionBinaryOperatorNode.h"
#include "vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionConstantNode.h"
#include "vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionUnaryOperatorNode.h"
#include "vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionVariableNode.h"


static String charToString(char c)
{
    char buffer[2] = {c, '\0'};
    return String(buffer);
}

//= Character classes of java.io.StreamTokenizer, as configured by the Java
//= version of setExpression: resetSyntax, whitespaceChars(' ', ' '),
//= wordChars for letters, digits and '_', ordinaryChar for the operators
//= and parenthesis, and parseNumbers (which adds the "digit" class to the
//= digits, '.' and '-').
static BOOLEAN isWhitespaceClass(char c)
{
    return c == ' ';
}

static BOOLEAN isDigitClass(char c)
{
    return (c >= '0' && c <= '9') || c == '.' || c == '-';
}

static BOOLEAN isAlphaClass(char c)
{
    return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
        (c >= '0' && c <= '9') || c == '_';
}

/**
@return the value of a constant token, as `Double.parseDouble`
@throws AlgebraicExpressionException when it is not a number

C++ port note: out of `buildExpressionTree`, as gcc 2.7.2 crashes on the
try blocks there.
*/
static double parseConstant(const String& token)
{
    try {
        return Double::parseDouble(token);
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( const NumberFormatException& e ) {
        throw AlgebraicExpressionException(e.getMessage());
    }
#endif
    return 0.0;
}

static void deleteNode(_AlgebraicExpressionNode* node)
{
    delete node;
}

/**
Deletes a node under construction when an exception leaves its scope,
unless it was released. C++ port note: `buildExpressionTree` uses it instead
of try/catch blocks, which gcc 2.7.2 fails to compile there.
*/
class _AlgebraicExpressionNodeGuard {
  private:
    _AlgebraicExpressionNode* node;

    _AlgebraicExpressionNodeGuard(const _AlgebraicExpressionNodeGuard& other);
    _AlgebraicExpressionNodeGuard& operator=(
        const _AlgebraicExpressionNodeGuard& other);

  public:
    explicit _AlgebraicExpressionNodeGuard(_AlgebraicExpressionNode* inNode) :
        node(inNode)
    {
    }

    ~_AlgebraicExpressionNodeGuard()
    {
        deleteNode(node);
    }

    void release()
    {
        node = NULL;
    }
};


AlgebraicExpression::AlgebraicExpression() : root(NULL)
{
    defineValue("PI", M_PI);
    defineValue("E", M_E);
}

AlgebraicExpression::~AlgebraicExpression()
{
    deleteNode(root);
}

void AlgebraicExpression::defineValue(const String& name, double val)
{
    values.put(name, val);
}

String AlgebraicExpression::toString() const
{
    String msg;

    if ( root == NULL ) {
        msg = "<Invalid Expression>";
    }
    else {
        msg = root->toString();
    }

    return msg;
}

double AlgebraicExpression::getVariableValue(const String& name) const
{
    const double* val = values.get(name);
    if ( val == NULL ) {
        throw AlgebraicExpressionException(
            String("AlgebraicExpression.getVariableValue: Variable \"") +
            name + "\" not defined");
    }
    return *val;
}

BOOLEAN AlgebraicExpression::isOperator(const String& cad)
{
    if ( cad.length() != 1 ) return FALSE;
    switch ( cad.charAt(0) ) {
      case '+':
      case '-':
      case '*':
      case '/':
      case '^':
        return TRUE;
      default:
        return FALSE;
    }
}

ArrayList<String> AlgebraicExpression::tokenize(
    const String& text)
{
    // Emulation of java.io.StreamTokenizer.nextToken for the syntax set by
    // the Java version (see character classes above). Number tokens are
    // stored as Java does, with `"" + parser.nval`.
    ArrayList<String> tokens;
    int n = text.length();
    int position = 0;

    while ( TRUE ) {
        while ( position < n && isWhitespaceClass(text.charAt(position)) ) {
            position++;
        }
        if ( position >= n ) {
            break;
        }
        char c = text.charAt(position);

        if ( isDigitClass(c) ) {
            BOOLEAN negative = FALSE;
            if ( c == '-' ) {
                char next = position + 1 < n ? text.charAt(position + 1) : '\0';
                if ( next != '.' && (next < '0' || next > '9') ) {
                    // A minus sign that does not start a number
                    tokens.add(charToString('-'));
                    position++;
                    continue;
                }
                negative = TRUE;
                position++;
            }
            double v = 0;
            int decexp = 0;
            int seendot = 0;
            while ( position < n ) {
                c = text.charAt(position);
                if ( c == '.' && seendot == 0 ) {
                    seendot = 1;
                }
                else if ( c >= '0' && c <= '9' ) {
                    v = v * 10 + (c - '0');
                    decexp += seendot;
                }
                else {
                    break;
                }
                position++;
            }
            if ( decexp != 0 ) {
                double denom = 10;
                decexp--;
                while ( decexp > 0 ) {
                    denom *= 10;
                    decexp--;
                }
                v = v / denom;
            }
            tokens.add(Double::toString(negative ? -v : v));
        }
        else if ( isAlphaClass(c) ) {
            // Words go on with letters, digits, '_', and also '.' and '-',
            // as parseNumbers gives them the digit class
            int start = position;
            while ( position < n && (isAlphaClass(text.charAt(position)) ||
                                     isDigitClass(text.charAt(position))) ) {
                position++;
            }
            tokens.add(text.substring(start, position));
        }
        else {
            // Ordinary character (quote characters are skipped, as the
            // Java version does)
            if ( c != '\"' ) {
                tokens.add(charToString(c));
            }
            position++;
        }
    }
    return tokens;
}

/**
C++ port note: the tokens are read with `operator[]` (a reference) instead
of `get` (a copy), as gcc 2.7.2 fails to compile this function with so many
`String` temporaries.
*/
_AlgebraicExpressionNode* AlgebraicExpression::buildExpressionTree(
    ArrayList<String>& tokens) const
{
    //-----------------------------------------------------------------
    int i;
    int level;
    char c;

    // The Java version fails here with an IndexOutOfBoundsException
    if ( tokens.size() == 0 ) {
        throw AlgebraicExpressionException("Parse error, empty expression");
    }

    //- Trim out embracing parenthesis from expression ----------------
    while ( tokens[0].equals("(") &&
            tokens[tokens.size()-1].equals(")") ) {
        level = 0;
        BOOLEAN trim = TRUE;

        for ( i = 0; i < tokens.size(); i++ ) {
            c = tokens[i].charAt(0);
            if ( c == '(' ) {
                level++;
            }
            else if ( c == ')' ) {
                level--;
            }
            if ( level == 0 && i < (tokens.size()-1) ) {
                trim = FALSE;
            }
        }

        if ( !trim ) {
            break;
        }
        tokens.remove((long)(tokens.size()-1));
        tokens.remove((long)0);
        if ( tokens.size() == 0 ) {
            throw AlgebraicExpressionException("Parse error, empty expression");
        }
    }

    //- Trivial case: if I am a single token, I create myself here ----
    if ( tokens.size() == 1 ) {
        String token = tokens[0];
        if ( isOperator(token) ) {
            throw AlgebraicExpressionException(
                String("Parse error, invalid placement for operator \'") +
                token + "\'");
        }
        else if ( (token.charAt(0) >= '0' && token.charAt(0) <= '9') ||
                  token.charAt(0) == '-' ) {
            return new _AlgebraicExpressionConstantNode(
                parseConstant(token));
        }
        else {
            return new _AlgebraicExpressionVariableNode(this, token);
        }
    }

    //- Recursive cases -----------------------------------------------
    ArrayList<int> topLevelConnectorIndexes;

    // Search for top level connector operators
    level = 0;
    for ( i = 0; i < tokens.size(); i++ ) {
        c = tokens[i].charAt(0);
        if ( c == '(' ) {
            level++;
            continue;
        }
        else if ( c == ')' ) {
            level--;
            continue;
        }
        if ( level == 0 && isOperator(tokens[i]) ) {
            topLevelConnectorIndexes.add(i);
        }
    }

    // Determine the top level connector
    if ( topLevelConnectorIndexes.size() <= 0 ) {
        // Try a funcional form expression
        _AlgebraicExpressionUnaryOperatorNode* uo;
        uo = new _AlgebraicExpressionUnaryOperatorNode(this, tokens[0]);
        tokens.remove((long)0);
        _AlgebraicExpressionNodeGuard guard(uo);
        uo->setOperand(buildExpressionTree(tokens));
        guard.release();
        return uo;
    }

    int mainConnector = 0;

    // '^' goes with lower priority, will be overwritten if lower
    // precedent operator is later founded
    for ( i = 0; i < topLevelConnectorIndexes.size(); i++ ) {
        c = tokens[topLevelConnectorIndexes.get(i)].charAt(0);
        if ( c == '^' ) {
            mainConnector = topLevelConnectorIndexes.get(i);
            break;
        }
    }

    // '*' y '/' goes before `^`
    for ( i = 0; i < topLevelConnectorIndexes.size(); i++ ) {
        c = tokens[topLevelConnectorIndexes.get(i)].charAt(0);
        if ( c == '*' || c == '/' ) {
            mainConnector = topLevelConnectorIndexes.get(i);
            break;
        }
    }

    // '+' y '-' goes before the others
    for ( i = 0; i < topLevelConnectorIndexes.size(); i++ ) {
        c = tokens[topLevelConnectorIndexes.get(i)].charAt(0);
        if ( c == '+' || c == '-' ) {
            mainConnector = topLevelConnectorIndexes.get(i);
            break;
        }
    }

    char op = tokens[mainConnector].charAt(0);

    if ( op == '-' && mainConnector == 0 ) {
        _AlgebraicExpressionUnaryOperatorNode* uo;
        uo = new _AlgebraicExpressionUnaryOperatorNode(this, "-");
        tokens.remove((long)0);
        _AlgebraicExpressionNodeGuard guard(uo);
        uo->setOperand(buildExpressionTree(tokens));
        guard.release();
        return uo;
    }

    ArrayList<String> leftTokens;
    for ( i = 0; i < mainConnector && i < tokens.size(); i++ ) {
        leftTokens.add(tokens[i]);
    }

    ArrayList<String> rightTokens;
    for ( i = mainConnector + 1; i < tokens.size(); i++ ) {
        rightTokens.add(tokens[i]);
    }

    _AlgebraicExpressionBinaryOperatorNode* bo;
    bo = new _AlgebraicExpressionBinaryOperatorNode(this, op);
    _AlgebraicExpressionNodeGuard guard(bo);
    bo->setLeftOperand(buildExpressionTree(leftTokens));
    bo->setRightOperand(buildExpressionTree(rightTokens));
    guard.release();
    return bo;
}

void AlgebraicExpression::setExpression(const String& regexp)
{
    ArrayList<String> tokens = tokenize(regexp);

    //-----------------------------------------------------------------
    _AlgebraicExpressionNode* newRoot = buildExpressionTree(tokens);
    deleteNode(root);
    root = newRoot;
}

double AlgebraicExpression::eval() const
{
    if ( root == NULL ) {
        throw AlgebraicExpressionException(
            "Null expression, can not evaluate.");
    }
    return root->eval();
}

//= Nodes ===================================================================

_AlgebraicExpressionConstantNode::_AlgebraicExpressionConstantNode(double val)
    : val(val)
{
}

double _AlgebraicExpressionConstantNode::eval() const
{
    return val;
}

String _AlgebraicExpressionConstantNode::toString() const
{
    return VSDK::formatDouble(val);
}

_AlgebraicExpressionVariableNode::_AlgebraicExpressionVariableNode(
    const AlgebraicExpression* parent, const String& name)
    : parent(parent), name(name)
{
}

double _AlgebraicExpressionVariableNode::eval() const
{
    return parent->getVariableValue(name);
}

String _AlgebraicExpressionVariableNode::toString() const
{
    return name;
}

_AlgebraicExpressionUnaryOperatorNode::_AlgebraicExpressionUnaryOperatorNode(
    const AlgebraicExpression* parent, const String& operatorName)
    : parent(parent), operatorName(operatorName), operand(NULL)
{
}

_AlgebraicExpressionUnaryOperatorNode::~_AlgebraicExpressionUnaryOperatorNode()
{
    delete operand;
}

void _AlgebraicExpressionUnaryOperatorNode::setOperand(
    _AlgebraicExpressionNode* operand)
{
    if ( this->operand != operand ) {
        delete this->operand;
    }
    this->operand = operand;
}

double _AlgebraicExpressionUnaryOperatorNode::eval() const
{
    double operandValue = operand->eval();
    double val = Cpp98Support::nan();

    if ( operatorName.equals("sin") ) {
        val = sin(operandValue);
    }
    else if ( operatorName.equals("cos") ) {
        val = cos(operandValue);
    }
    else if ( operatorName.equals("tan") ) {
        val = tan(operandValue);
    }
    else if ( operatorName.equals("asin") ) {
        val = asin(operandValue);
    }
    else if ( operatorName.equals("acos") ) {
        val = acos(operandValue);
    }
    else if ( operatorName.equals("atan") ) {
        val = atan(operandValue);
    }
    else if ( operatorName.equals("abs") ) {
        val = fabs(operandValue);
    }
    else if ( operatorName.equals("cbrt") ) {
        //val = Math.cbrt(operandValue);
        val = pow(operandValue, 1.0/3.0);
    }
    else if ( operatorName.equals("ceil") ) {
        val = ceil(operandValue);
    }
    else if ( operatorName.equals("sinh") ) {
        val = sinh(operandValue);
    }
    else if ( operatorName.equals("cosh") ) {
        val = cosh(operandValue);
    }
    else if ( operatorName.equals("tanh") ) {
        val = tanh(operandValue);
    }
    else if ( operatorName.equals("toDegrees") ) {
        // Same constant factors as java.lang.Math
        val = operandValue * (180.0 / M_PI);
    }
    else if ( operatorName.equals("toRadians") ) {
        val = operandValue * (M_PI / 180.0);
    }
    else if ( operatorName.equals("exp") ) {
        val = exp(operandValue);
    }
    else if ( operatorName.equals("floor") ) {
        val = floor(operandValue);
    }
    else if ( operatorName.equals("log") ) {
        val = log(operandValue);
    }
    else if ( operatorName.equals("ln") ) {
        val = log(operandValue);
    }
    else if ( operatorName.equals("log10") ) {
        val = log10(operandValue);
    }
    else if ( operatorName.equals("sqrt") ) {
        val = sqrt(operandValue);
    }
    else if ( operatorName.equals("-") ) {
        val = -operandValue;
    }
    else {
        throw AlgebraicExpressionException(
            String("Unknown unary operator or function \"") +
            operatorName + "\"");
    }
    return val;
}

String _AlgebraicExpressionUnaryOperatorNode::toString() const
{
    String msg;

    msg = operatorName + "(";
    msg += operand->toString();
    msg += ")";
    return msg;
}

_AlgebraicExpressionBinaryOperatorNode::_AlgebraicExpressionBinaryOperatorNode(
    const AlgebraicExpression* parent, char op)
    : parent(parent), operatorChar(op), leftOperand(NULL),
      rightOperand(NULL)
{
}

_AlgebraicExpressionBinaryOperatorNode::~_AlgebraicExpressionBinaryOperatorNode()
{
    delete leftOperand;
    delete rightOperand;
}

void _AlgebraicExpressionBinaryOperatorNode::setLeftOperand(
    _AlgebraicExpressionNode* operand)
{
    if ( leftOperand != operand ) {
        delete leftOperand;
    }
    leftOperand = operand;
}

void _AlgebraicExpressionBinaryOperatorNode::setRightOperand(
    _AlgebraicExpressionNode* operand)
{
    if ( rightOperand != operand ) {
        delete rightOperand;
    }
    rightOperand = operand;
}

double _AlgebraicExpressionBinaryOperatorNode::eval() const
{
    double lval = leftOperand->eval();
    double rval = rightOperand->eval();
    double val;

    switch ( operatorChar ) {
      case '+':    val = lval + rval;    break;
      case '-':    val = lval - rval;    break;
      case '*':    val = lval * rval;    break;
      case '/':    val = lval / rval;    break;
      case '^':    val = pow(lval, rval);    break;
      default:
        throw AlgebraicExpressionException(
            String("Unknown binary operator \"") +
            charToString(operatorChar) + "\"");
    }
    return val;
}

String _AlgebraicExpressionBinaryOperatorNode::toString() const
{
    return String("(") + leftOperand->toString() + ") " +
        charToString(operatorChar) + " (" + rightOperand->toString() + ")";
}
