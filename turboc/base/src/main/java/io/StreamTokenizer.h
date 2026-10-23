#ifndef __STREAM_TOKENIZER__
#define __STREAM_TOKENIZER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/Reader.h"
#include "java/lang/String.h"

/**
Port of the subset of java.io.StreamTokenizer (table-driven, as in OpenJDK)
used by Vitral: resetSyntax, eolIsSignificant, slashSlashComments,
slashStarComments, commentChar, whitespaceChars, wordChars, parseNumbers,
nextToken, ttype, nval, sval. The slash comment flags are stored but the
"/" comment handling is not implemented (Vitral only disables them).

Omitted: quoteChar/ordinaryChar(s)/lowerCaseMode/pushBack/lineno/toString,
and the quoted string tokens. The reader is not owned.
*/
class StreamTokenizer {
  private:
    Reader &reader;
    int peekc;
    BOOLEAN eolIsSignificantP;
    BOOLEAN slashSlashCommentsP;
    BOOLEAN slashStarCommentsP;
    unsigned char ctype[256];

    enum {
        CT_WHITESPACE = 1, CT_DIGIT = 2, CT_ALPHA = 4, CT_COMMENT = 16
    };

    int
    typeOf(int c) const;

  public:
    enum {
        TT_EOF = -1,
        TT_EOL = '\n',
        TT_NUMBER = -2,
        TT_WORD = -3
    };

    int ttype;
    double nval;
    String sval;

    explicit StreamTokenizer(Reader &reader);

    void resetSyntax();
    void eolIsSignificant(BOOLEAN flag);
    void slashSlashComments(BOOLEAN flag);
    void slashStarComments(BOOLEAN flag);
    void commentChar(int ch);
    void whitespaceChars(int low, int hi);
    void wordChars(int low, int hi);
    void parseNumbers();
    int nextToken();
};


#endif
