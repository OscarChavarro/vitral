package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

/**
Names the boolean fixture corpus used by regression and diagnostic tests.

<p>Traceability: [MANT1988] Ch. 15 boolean set-operation examples and
project-local fixtures derived from the book's CSG discussion.</p>
 */
enum CsgSampleCorpus
{
    HOLLOW_BRICK(1),
    MANT1986_2(2),
    STACKED_BLOCKS(3),
    CROSS_PAIR(4),
    MOON_BLOCK(5),
    MANT1988_6_13(6),
    MANT1988_3(7),
    MANT1988_15_2_HOLED(8),
    MANT1988_15_1(9);

    private final int id;

    CsgSampleCorpus(int id)
    {
        this.id = id;
    }

    int id()
    {
        return id;
    }
}
