package model;

public class PolygonClippingFixtures
{
    public static final PolygonClippingTestCase[] CASES = {
        new PolygonClippingTestCase("TRIANGLE_VS_QUAD",               "example03.polygon", "example04.polygon"),
        new PolygonClippingTestCase("TRIANGLE_VS_QUAD_WITH_HOLE",     "example03.polygon", "example05.polygon"),
        new PolygonClippingTestCase("DIAMOND_VS_STAR_CHAIN",          "example06.polygon", "example07.polygon"),
        new PolygonClippingTestCase("DIAMOND_VS_STAR_CHAIN_HOLE",     "example06.polygon", "example08.polygon"),
        new PolygonClippingTestCase("VERTICAL_STRIPPER",              "example09.polygon", "example10.polygon"),
        new PolygonClippingTestCase("MULTI_LOOP_CLIP",                "example11.polygon", "example12.polygon"),
        new PolygonClippingTestCase("MULTI_LOOP_CLIP_OUTER_ONLY",     "example11.polygon", "example13.polygon"),
        new PolygonClippingTestCase("SUBJECT_AS_OUTER_FRAME",         "example12.polygon", "example14.polygon"),
        new PolygonClippingTestCase("FRAME_WITH_INNER_SUBJECTS",      "example15.polygon", "example16.polygon"),
        new PolygonClippingTestCase("SUBJECT_WITH_BANDS",             "example12.polygon", "example17.polygon"),
        new PolygonClippingTestCase("NESTED_SQUARES",                 "example18.polygon", "example19.polygon"),
        new PolygonClippingTestCase("WINDOW_WITH_ISLANDS",            "example20.polygon", "example18.polygon"),
        new PolygonClippingTestCase("RECT_WITH_MATCHING_HOLE",        "example21.polygon", "example22.polygon"),
        new PolygonClippingTestCase("OUTER_ONLY_BAND",                "example15.polygon", "example13.polygon"),
        new PolygonClippingTestCase("TRAPEZOID_VS_WINDOW",            "example23.polygon", "example24.polygon"),
        new PolygonClippingTestCase("RECT_VS_WINDOW",                 "example25.polygon", "example24.polygon"),
        new PolygonClippingTestCase("SLANTED_RECT",                   "example26.polygon", "example27.polygon"),
        new PolygonClippingTestCase("ARROW_A",                        "example28.polygon", "example29.polygon"),
        new PolygonClippingTestCase("ARROW_B",                        "example29.polygon", "example28.polygon"),
        new PolygonClippingTestCase("SQUARE_VS_STAR",                 "example30.polygon", "example31.polygon"),
        new PolygonClippingTestCase("QUAD_VS_TRIANGLE",               "example32.polygon", "example30.polygon"),
        new PolygonClippingTestCase("HEX_VS_TRIANGLE",                "example33.polygon", "example34.polygon"),
        new PolygonClippingTestCase("HEX_VS_TRIANGLE_ALT",            "example33.polygon", "example35.polygon"),
        new PolygonClippingTestCase("DIAMOND_VS_DIAMOND",             "example36.polygon", "example32.polygon"),
        new PolygonClippingTestCase("KITE_VS_COMPLEX",                "example37.polygon", "example38.polygon"),
        new PolygonClippingTestCase("DIAMOND_VS_RECT",                "example39.polygon", "example40.polygon"),
        new PolygonClippingTestCase("RECT_VS_HALF_RECT",              "example40.polygon", "example41.polygon"),
        new PolygonClippingTestCase("NOTCH_SELF_SIMILAR",             "example42.polygon", "example42.polygon"),
        new PolygonClippingTestCase("RECT_DISJOINT_RIGHT",            "example40.polygon", "example43.polygon"),
        new PolygonClippingTestCase("RECT_DISJOINT_TOP",              "example44.polygon", "example45.polygon"),
        new PolygonClippingTestCase("SKEW_RECT_SELF",                 "example46.polygon", "example46.polygon"),
        new PolygonClippingTestCase("PENTAGON_SELF",                  "example47.polygon", "example47.polygon"),
        new PolygonClippingTestCase("PENTAGON_VS_RECT",               "example44.polygon", "example48.polygon"),
        new PolygonClippingTestCase("CONCAVE_WINDOW_VS_BAR",          "example49.polygon", "example50.polygon"),
        new PolygonClippingTestCase("BAR_VS_CONCAVE_WINDOW",          "example50.polygon", "example49.polygon"),
        new PolygonClippingTestCase("CONCAVE_FRAME_VS_ROOF",          "example51.polygon", "example52.polygon"),
        new PolygonClippingTestCase("LOWER_BAR_VS_FRAME",             "example53.polygon", "example51.polygon"),
        new PolygonClippingTestCase("INNER_BAR_OFFSET",               "example51.polygon", "example54.polygon"),
        new PolygonClippingTestCase("INNER_BAR_SHARP",                "example51.polygon", "example55.polygon"),
        new PolygonClippingTestCase("INNER_BAR_TOUCHING_RIGHT",       "example51.polygon", "example56.polygon"),
        new PolygonClippingTestCase("FRAME_WITH_SHIFTED_BAR",         "example51.polygon", "example57.polygon"),
        new PolygonClippingTestCase("UNION_SQUARES_SHARED_EDGE",      "example58.polygon", "example59.polygon"),
        new PolygonClippingTestCase("UNION_SQUARES_SHARED_CORNER",    "example58.polygon", "example60.polygon")
    };

    private PolygonClippingFixtures()
    {
    }
}
