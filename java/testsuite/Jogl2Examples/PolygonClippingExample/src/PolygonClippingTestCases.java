public class PolygonClippingTestCases
{
    public static final PolygonClippingTestCase[] CASES = {
        new PolygonClippingTestCase("TRIANGLE_VS_QUAD",
            new double[][] {
                {3, 0, 6, 4, 2, 6, 1, 3}
            },
            new double[][] {
                {3, 3, 8, 1, 9, 5, 7, 7}
            }),
        new PolygonClippingTestCase("TRIANGLE_VS_QUAD_WITH_HOLE",
            new double[][] {
                {3, 0, 6, 4, 2, 6, 1, 3}
            },
            new double[][] {
                {3, 3, 8, 1, 9, 5, 7, 7},
                {4, 3, 6, 5, 8, 3, 7, 2}
            }),
        new PolygonClippingTestCase("DIAMOND_VS_STAR_CHAIN",
            new double[][] {
                {0, 1, 11, 8, 6, 11, -3, 5}
            },
            new double[][] {
                {4, 0, 5, 1, 1, 5, 6, 9, 9, 5, 6, 2, 3, 5, 6, 7, 7, 5,
                    6, 4, 5, 5, 6, 5, 6, 6, 4, 5, 6, 3, 8, 5, 6, 8,
                    2, 5, 6, 1, 11, 5, 6, 10, -1, 5}
            }),
        new PolygonClippingTestCase("DIAMOND_VS_STAR_CHAIN_HOLE",
            new double[][] {
                {0, 1, 11, 8, 6, 11, -3, 5}
            },
            new double[][] {
                {4, 0, 5, 1, 1, 5, 6, 9, 9, 5, 6, 2, 3, 5, 6, 7, 7, 5,
                    6, 4, 5, 5, 6, 5, 6, 6, 4, 5, 6, 3, 8, 5, 6, 8,
                    2, 5, 6, 1, 11, 5, 6, 10, -1, 5},
                {2.5, 2, 2, 3, 3, 2}
            }),
        new PolygonClippingTestCase("VERTICAL_STRIPPER",
            new double[][] {
                {3, -1, 4, -1, 4, 8, 3, 8}
            },
            new double[][] {
                {0, 0, 5, 0, 5, 1, 2, 1, 2, 2, 5, 2, 5, 3, 2, 3,
                    2, 4, 5, 4, 5, 5, 2, 5, 2, 6, 5, 6, 5, 7, 0, 7}
            }),
        new PolygonClippingTestCase("MULTI_LOOP_CLIP",
            new double[][] {
                {1.69, 5.81, 17.63, 4.44, 17.88, 14.75, 1.88, 14.75},
                {3.31, 11.97, 7.09, 11.97, 7.31, 7.88, 3.38, 8.16},
                {11.91, 10.78, 15.09, 10.66, 14.78, 8.16, 12.00, 8.38}
            },
            new double[][] {
                {2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34,
                    2.69, 13.28},
                {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91,
                    9.63, 8.88}
            }),
        new PolygonClippingTestCase("MULTI_LOOP_CLIP_OUTER_ONLY",
            new double[][] {
                {1.69, 5.81, 17.63, 4.44, 17.88, 14.75, 1.88, 14.75},
                {3.31, 11.97, 7.09, 11.97, 7.31, 7.88, 3.38, 8.16},
                {11.91, 10.78, 15.09, 10.66, 14.78, 8.16, 12.00, 8.38}
            },
            new double[][] {
                {2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34,
                    2.69, 13.28}
            }),
        new PolygonClippingTestCase("SUBJECT_AS_OUTER_FRAME",
            new double[][] {
                {2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34,
                    2.69, 13.28},
                {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91,
                    9.63, 8.88}
            },
            new double[][] {
                {1.69, 5.81, 17.63, 4.44, 17.88, 14.75, 1.88, 14.75}
            }),
        new PolygonClippingTestCase("FRAME_WITH_INNER_SUBJECTS",
            new double[][] {
                {1.69, 5.81, 17.63, 4.44, 17.88, 14.75, 1.88, 14.75},
                {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97,
                    2.44, 7.09}
            },
            new double[][] {
                {3.38, 8.16, 7.31, 7.88, 7.09, 11.97, 3.31, 11.97},
                {9.63, 8.88, 13.06, 6.91, 16.06, 7.97, 15.22, 12.03,
                    9.88, 11.38}
            }),
        new PolygonClippingTestCase("SUBJECT_WITH_BANDS",
            new double[][] {
                {2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34,
                    2.69, 13.28},
                {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91,
                    9.63, 8.88}
            },
            new double[][] {
                {1.69, 13.5, 17.63, 13.5, 17.88, 14.75, 1.88, 14.75},
                {3.38, 8.16, 7.31, 7.88, 7.09, 11.97, 3.31, 11.97},
                {12.00, 8.38, 14.78, 8.16, 15.09, 10.66, 11.91, 10.78}
            }),
        new PolygonClippingTestCase("NESTED_SQUARES",
            new double[][] {
                {2, 2, 10, 2, 10, 10, 2, 10},
                {3, 3, 3, 9, 9, 9, 9, 3},
                {4, 4, 8, 4, 8, 8, 4, 8},
                {5, 5, 6, 7, 7, 5}
            },
            new double[][] {
                {0, 0, 12, 0, 12, 11, 0, 11}
            }),
        new PolygonClippingTestCase("WINDOW_WITH_ISLANDS",
            new double[][] {
                {0, 0, 12, 0, 12, 11, 0, 11},
                {2.2, 6, 2.5, 7, 2.8, 6},
                {3.2, 5, 3.5, 6, 3.8, 5},
                {4.2, 6, 4.5, 7, 4.8, 6},
                {5.5, 5.5, 6, 6.5, 6.5, 5.5}
            },
            new double[][] {
                {2, 2, 10, 2, 10, 10, 2, 10},
                {3, 3, 3, 9, 9, 9, 9, 3},
                {4, 4, 8, 4, 8, 8, 4, 8},
                {5, 5, 6, 7, 7, 5}
            }),
        new PolygonClippingTestCase("RECT_WITH_MATCHING_HOLE",
            new double[][] {
                {0, 0, 10, 0, 10, 11, 0, 11},
                {1.23, 1.65, 1.19, 9.61, 9.42, 9.57, 9.23, 1.68}
            },
            new double[][] {
                {1.23, 1.65, 1.19, 9.61, 9.42, 9.57, 9.23, 1.68}
            }),
        new PolygonClippingTestCase("OUTER_ONLY_BAND",
            new double[][] {
                {1.69, 5.81, 17.63, 4.44, 17.88, 14.75, 1.88, 14.75},
                {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97,
                    2.44, 7.09}
            },
            new double[][] {
                {2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34,
                    2.69, 13.28}
            }),
        new PolygonClippingTestCase("TRAPEZOID_VS_WINDOW",
            new double[][] {
                {-5, 5, 5, 0, 7, 10, -3, 15}
            },
            new double[][] {
                {0, 0, 15, 0, 15, 10, 0, 10}
            }),
        new PolygonClippingTestCase("RECT_VS_WINDOW",
            new double[][] {
                {-5, 0, 10, 0, 10, 10, -5, 10}
            },
            new double[][] {
                {0, 0, 15, 0, 15, 10, 0, 10}
            }),
        new PolygonClippingTestCase("SLANTED_RECT",
            new double[][] {
                {0, 0, 10, 4, 10, 8, 0, 12}
            },
            new double[][] {
                {5, 0, 10, 0, 10, 12, 5, 12}
            }),
        new PolygonClippingTestCase("ARROW_A",
            new double[][] {
                {0, 0, 3, 0, 6, 4, 0, 4}
            },
            new double[][] {
                {3, 0, 6, 4, 3, 8, 0, 4}
            }),
        new PolygonClippingTestCase("ARROW_B",
            new double[][] {
                {3, 0, 6, 4, 3, 8, 0, 4}
            },
            new double[][] {
                {0, 0, 3, 0, 6, 4, 0, 4}
            }),
        new PolygonClippingTestCase("SQUARE_VS_STAR",
            new double[][] {
                {0, 0, 3, 0, 3, 3, 0, 3}
            },
            new double[][] {
                {-3, 1.5, 0, 0, 3, -3, 3, 0, 3, 3, 3, 6, 0, 3}
            }),
        new PolygonClippingTestCase("QUAD_VS_TRIANGLE",
            new double[][] {
                {3, 0, 5, 2, 4, 6, 0, 3}
            },
            new double[][] {
                {0, 0, 3, 0, 3, 3, 0, 3}
            }),
        new PolygonClippingTestCase("HEX_VS_TRIANGLE",
            new double[][] {
                {3, -3, 6, 0, 4, 2, 6, 4, 3, 7, 0, 2}
            },
            new double[][] {
                {4, 2, 12, 3, 6, 4}
            }),
        new PolygonClippingTestCase("HEX_VS_TRIANGLE_ALT",
            new double[][] {
                {3, -3, 6, 0, 4, 2, 6, 4, 3, 7, 0, 2}
            },
            new double[][] {
                {4, 2, 12, 3, 8, 4}
            }),
        new PolygonClippingTestCase("DIAMOND_VS_DIAMOND",
            new double[][] {
                {5, 2, 8, -1, 11, 3, 7, 6}
            },
            new double[][] {
                {3, 0, 5, 2, 4, 6, 0, 3}
            }),
        new PolygonClippingTestCase("KITE_VS_COMPLEX",
            new double[][] {
                {4, 3, 7, 4, 5, 8, 1, 7}
            },
            new double[][] {
                {4, 3, 2, 4, 4, 6, 1, 7, -1, 6, 0, 0}
            }),
        new PolygonClippingTestCase("DIAMOND_VS_RECT",
            new double[][] {
                {3, 0, 0, -3, 3, -6, 6, -3}
            },
            new double[][] {
                {0, 0, 6, 0, 6, 4, 0, 4}
            }),
        new PolygonClippingTestCase("RECT_VS_HALF_RECT",
            new double[][] {
                {0, 0, 6, 0, 6, 4, 0, 4}
            },
            new double[][] {
                {0, 0, 3, 0, 3, 4, 0, 4}
            }),
        new PolygonClippingTestCase("NOTCH_SELF_SIMILAR",
            new double[][] {
                {0, 4, 1, 1, 5, 1, 5, 3}
            },
            new double[][] {
                {0, 4, 1, 1, 5, 1, 5, 3}
            }),
        new PolygonClippingTestCase("RECT_DISJOINT_RIGHT",
            new double[][] {
                {0, 0, 6, 0, 6, 4, 0, 4}
            },
            new double[][] {
                {6, 0, 10, 0, 10, 4, 6, 4}
            }),
        new PolygonClippingTestCase("RECT_DISJOINT_TOP",
            new double[][] {
                {0, 0, 7, 0, 7, 4, 0, 4}
            },
            new double[][] {
                {3, 4, 6, 4, 6, 8, 3, 8}
            }),
        new PolygonClippingTestCase("SKEW_RECT_SELF",
            new double[][] {
                {0, 0, 3, -2, 3, 6, 0, 4}
            },
            new double[][] {
                {0, 0, 3, -2, 3, 6, 0, 4}
            }),
        new PolygonClippingTestCase("PENTAGON_SELF",
            new double[][] {
                {0, 0, 3, -3, 3, 7, 0, 4, -4, 2}
            },
            new double[][] {
                {0, 0, 3, -3, 3, 7, 0, 4, -4, 2}
            }),
        new PolygonClippingTestCase("PENTAGON_VS_RECT",
            new double[][] {
                {0, 0, 7, 0, 7, 4, 0, 4}
            },
            new double[][] {
                {0, 0, 3, -3, 3, 7, 0, 4, 2, 2}
            }),
        new PolygonClippingTestCase("CONCAVE_WINDOW_VS_BAR",
            new double[][] {
                {0, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2,
                    8, 4, 0, 4}
            },
            new double[][] {
                {0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}
            }),
        new PolygonClippingTestCase("BAR_VS_CONCAVE_WINDOW",
            new double[][] {
                {0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}
            },
            new double[][] {
                {0, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2,
                    8, 4, 0, 4}
            }),
        new PolygonClippingTestCase("CONCAVE_FRAME_VS_ROOF",
            new double[][] {
                {-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2,
                    8, 5, -1, 5}
            },
            new double[][] {
                {0, 3, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 7, 3}
            }),
        new PolygonClippingTestCase("LOWER_BAR_VS_FRAME",
            new double[][] {
                {0, -1, 7, -1, 7, 2, 6.5, 2, 6, 1, 5, 0, 2, 0, 1, 1,
                    0.5, 2, 0, 2}
            },
            new double[][] {
                {-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2,
                    8, 5, -1, 5}
            }),
        new PolygonClippingTestCase("INNER_BAR_OFFSET",
            new double[][] {
                {-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2,
                    8, 5, -1, 5}
            },
            new double[][] {
                {0.5, 2, 1, 1, 1.5, 0.5, 5.5, 0.5, 6, 1, 6.5, 2, 6.5, 3,
                    0.5, 3}
            }),
        new PolygonClippingTestCase("INNER_BAR_SHARP",
            new double[][] {
                {-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2,
                    8, 5, -1, 5}
            },
            new double[][] {
                {0.5, 2, 1, 1, 3, -1, 4, -1, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}
            }),
        new PolygonClippingTestCase("INNER_BAR_TOUCHING_RIGHT",
            new double[][] {
                {-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2,
                    8, 5, -1, 5}
            },
            new double[][] {
                {0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 8, 1, 6.5, 3, 0.5, 3}
            }),
        new PolygonClippingTestCase("FRAME_WITH_SHIFTED_BAR",
            new double[][] {
                {-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2,
                    8, 5, -1, 5}
            },
            new double[][] {
                {-1, 1, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}
            })
    };

    private PolygonClippingTestCases()
    {
    }
}
