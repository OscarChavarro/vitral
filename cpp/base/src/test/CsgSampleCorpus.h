#ifndef __CSG_SAMPLE_CORPUS__
#define __CSG_SAMPLE_CORPUS__

/**
Names the boolean fixture corpus used by regression and diagnostic tests.

Traceability: [MANT1988] Ch. 15 boolean set-operation examples and
project-local fixtures derived from the book's CSG discussion.
C++ counterpart of Java's `CsgSampleCorpus` (test code). Java enum values
are kept in declaration order; `CSG_SAMPLE_CORPUS_VALUES` and
`csgSampleCorpusName` stand for `values()` and `name()`.
*/
enum CsgSampleCorpus
{
    HOLLOW_BRICK,
    MANT1986_2,
    STACKED_BLOCKS,
    CROSS_PAIR,
    MOON_BLOCK,
    MANT1988_6_13,
    MANT1988_3,
    MANT1988_15_2_HOLED,
    MANT1988_15_1
};

const int CSG_SAMPLE_CORPUS_COUNT = 9;

const CsgSampleCorpus CSG_SAMPLE_CORPUS_VALUES[CSG_SAMPLE_CORPUS_COUNT] = {
    HOLLOW_BRICK, MANT1986_2, STACKED_BLOCKS, CROSS_PAIR, MOON_BLOCK,
    MANT1988_6_13, MANT1988_3, MANT1988_15_2_HOLED, MANT1988_15_1
};

/** Java `CsgSampleCorpus.id()`. */
inline int csgSampleCorpusId(CsgSampleCorpus sample)
{
    return (int)sample + 1;
}

/** Java `CsgSampleCorpus.name()`. */
inline const char* csgSampleCorpusName(CsgSampleCorpus sample)
{
    static const char* NAMES[CSG_SAMPLE_CORPUS_COUNT] = {
        "HOLLOW_BRICK", "MANT1986_2", "STACKED_BLOCKS", "CROSS_PAIR",
        "MOON_BLOCK", "MANT1988_6_13", "MANT1988_3", "MANT1988_15_2_HOLED",
        "MANT1988_15_1"
    };
    return NAMES[(int)sample];
}

#endif
