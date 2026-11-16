#ifndef __CSG_SAMPLE_CORPUS__
#define __CSG_SAMPLE_CORPUS__

/**
Names the boolean fixture corpus used by regression and diagnostic tests.

Traceability: [MANT1988] Ch. 15 boolean set-operation examples and
project-local fixtures derived from the book's CSG discussion.
C++ counterpart of Java's `CsgSampleCorpus` enum (test code); `id()` is
`csgSampleCorpusId`.
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
    MANT1988_15_1,
    CSG_SAMPLE_CORPUS_COUNT
};

inline int
csgSampleCorpusId(CsgSampleCorpus sample)
{
    return (int)sample + 1;
}

inline const char*
csgSampleCorpusName(CsgSampleCorpus sample)
{
    static const char* const NAMES[] = {
        "HOLLOW_BRICK", "MANT1986_2", "STACKED_BLOCKS", "CROSS_PAIR",
        "MOON_BLOCK", "MANT1988_6_13", "MANT1988_3", "MANT1988_15_2_HOLED",
        "MANT1988_15_1" };
    return NAMES[(int)sample];
}

#endif
