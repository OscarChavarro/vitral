package vsdk.toolkit.environment.scene.csg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
Reads the generated cpp fixture files `cpp/base/src/test/csg_*_reference.inc`
at test time, so the reference values are shared with the cpp suite instead of
being retyped. Each parser accepts only the statement shapes the generators
emit and fails on anything else.
*/
public class CsgReferenceInclude {
    /** One `EXPECT_EQ(expected,CsgExactArithmetic::function(arguments))` case. */
    public static class ExactCase {
        public final String function;
        public final boolean expected;
        public final double[] scalars;
        public final double[][] arrays;
        public final int count;
        ExactCase(String function, boolean expected, double[] scalars, double[][] arrays, int count)
        { this.function = function; this.expected = expected; this.scalars = scalars; this.arrays = arrays; this.count = count; }
    }

    /** One `CsgExactQuadric::world` case with its placement as withVal entries. */
    public static class QuadricCase {
        public final double[] local;
        public final double[] expected;
        public final int[] rows;
        public final int[] columns;
        public final double[] values;
        QuadricCase(double[] local, double[] expected, int[] rows, int[] columns, double[] values)
        { this.local = local; this.expected = expected; this.rows = rows; this.columns = columns; this.values = values; }
    }

    /** The `samples` and `expected` tables of the dyadic fixture. */
    public static class DyadicTables {
        public final double[][] samples;
        public final boolean[] expected;
        DyadicTables(double[][] samples, boolean[] expected) { this.samples = samples; this.expected = expected; }
    }

    public static Path locate(String name)
    {
        Path directory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            final Path candidate = directory.resolve("cpp").resolve("base").resolve("src").resolve("test").resolve(name);
            if (Files.isRegularFile(candidate)) return candidate;
            directory = directory.getParent();
        }
        throw new IllegalStateException("cpp fixture not found: " + name);
    }

    private static List<String> statements(String name)
    {
        final List<String> result = new ArrayList<String>();
        try {
            for (String line : Files.readAllLines(locate(name), StandardCharsets.UTF_8)) {
                final String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("//")) continue;
                result.add(trimmed);
            }
        }
        catch (IOException e) { throw new IllegalStateException(e); }
        return result;
    }

    private static double[] doubles(String list)
    {
        final String[] items = list.split(",");
        final double[] result = new double[items.length];
        for (int i = 0; i < items.length; ++i) result[i] = Double.parseDouble(items[i].trim());
        return result;
    }

    private static final String NUMBER = "[-+0-9.eE]+";
    private static final Pattern SCALAR = Pattern.compile(
        "EXPECT_EQ\\((true|false),CsgExactArithmetic::(equalProducts|equalAffine)\\(([^()]*)\\)\\);");
    private static final Pattern SUM = Pattern.compile(
        "\\{const double a\\[3\\]=\\{([^{}]*)\\},b\\[3\\]=\\{([^{}]*)\\},c\\[3\\]=\\{([^{}]*)\\},d\\[3\\]=\\{([^{}]*)\\}; " +
        "EXPECT_EQ\\((true|false),CsgExactArithmetic::equalSumProducts\\(a,b,c,d,(\\d+)\\)\\);\\}");
    private static final Pattern QUADRIC = Pattern.compile(
        "\\{const double local\\[10\\]=\\{([^{}]*)\\},expected\\[10\\]=\\{([^{}]*)\\}; double result\\[10\\]=\\{\\}; " +
        "ASSERT_TRUE\\(CsgExactQuadric::world\\(local,Matrix4x4d::identityMatrix\\(\\)((?:\\.withVal\\(\\d,\\d," + NUMBER + "\\))*),result\\)\\); " +
        "for\\(int i=0;i<10;\\+\\+i\\) EXPECT_DOUBLE_EQ\\(expected\\[i\\],result\\[i\\]\\);\\}");
    private static final Pattern WITHVAL = Pattern.compile("\\.withVal\\((\\d),(\\d),(" + NUMBER + ")\\)");

    public static List<ExactCase> exactCases()
    {
        final List<ExactCase> result = new ArrayList<ExactCase>();
        for (String statement : statements("csg_exact_reference.inc")) {
            Matcher m = SCALAR.matcher(statement);
            if (m.matches()) {
                result.add(new ExactCase(m.group(2), m.group(1).equals("true"), doubles(m.group(3)), null, 0));
                continue;
            }
            m = SUM.matcher(statement);
            if (m.matches()) {
                result.add(new ExactCase("equalSumProducts", m.group(5).equals("true"), null,
                    new double[][] {doubles(m.group(1)), doubles(m.group(2)), doubles(m.group(3)), doubles(m.group(4))},
                    Integer.parseInt(m.group(6))));
                continue;
            }
            throw new IllegalStateException("unexpected statement in csg_exact_reference.inc: " + statement);
        }
        return result;
    }

    public static List<QuadricCase> quadricCases()
    {
        final List<QuadricCase> result = new ArrayList<QuadricCase>();
        for (String statement : statements("csg_quadric_reference.inc")) {
            final Matcher m = QUADRIC.matcher(statement);
            if (!m.matches()) throw new IllegalStateException("unexpected statement in csg_quadric_reference.inc: " + statement);
            final List<String[]> entries = new ArrayList<String[]>();
            final Matcher w = WITHVAL.matcher(m.group(3));
            while (w.find()) entries.add(new String[] {w.group(1), w.group(2), w.group(3)});
            final int[] rows = new int[entries.size()], columns = new int[entries.size()];
            final double[] values = new double[entries.size()];
            for (int i = 0; i < entries.size(); ++i) {
                rows[i] = Integer.parseInt(entries.get(i)[0]);
                columns[i] = Integer.parseInt(entries.get(i)[1]);
                values[i] = Double.parseDouble(entries.get(i)[2]);
            }
            result.add(new QuadricCase(doubles(m.group(1)), doubles(m.group(2)), rows, columns, values));
        }
        return result;
    }

    public static DyadicTables dyadicTables()
    {
        final StringBuilder text = new StringBuilder();
        for (String statement : statements("csg_dyadic_reference.inc")) text.append(statement).append('\n');
        final String all = text.toString();
        final Matcher s = Pattern.compile("static const double samples\\[\\]\\[8\\]=\\{(.*?)\\};\\n", Pattern.DOTALL).matcher(all);
        final Matcher e = Pattern.compile("static const bool expected\\[\\]=\\{([^{}]*)\\};").matcher(all);
        if (!s.find() || !e.find()) throw new IllegalStateException("unexpected csg_dyadic_reference.inc tables");
        final List<double[]> rows = new ArrayList<double[]>();
        final Matcher row = Pattern.compile("\\{([^{}]*)\\}").matcher(s.group(1));
        while (row.find()) {
            final double[] values = doubles(row.group(1));
            if (values.length != 8) throw new IllegalStateException("dyadic sample row without 8 values");
            rows.add(values);
        }
        final String[] flags = e.group(1).split(",");
        final boolean[] expected = new boolean[flags.length];
        for (int i = 0; i < flags.length; ++i) {
            final String flag = flags[i].trim();
            if (!flag.equals("true") && !flag.equals("false")) throw new IllegalStateException("bad dyadic flag " + flag);
            expected[i] = flag.equals("true");
        }
        return new DyadicTables(rows.toArray(new double[0][]), expected);
    }
}
