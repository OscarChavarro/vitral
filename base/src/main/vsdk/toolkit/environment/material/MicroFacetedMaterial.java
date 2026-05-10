//= References:                                                             =
//= [COOK1982] Cook, Robert L.; Torrance, Kenneth E. "A Reflectance Model   =
//= for Computer Graphics", ACM Transactions on Graphics, 1982.             =
//= [TORR1967] Torrance, K. E.; Sparrow, E. M. "Theory for Off-Specular     =
//= Reflection from Roughened Surfaces", JOSA, 1967.                        =
//= [BECK1963] Beckmann, P.; Spizzichino, A. "The Scattering of              =
//= Electromagnetic Waves from Rough Surfaces", 1963.                        =

package vsdk.toolkit.environment.material;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serial;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.io.PersistenceElement;

/**
Cook-Torrance oriented extension of {@link SimpleMaterial}.

This class stores the per-object microfacet parameters shared by CPU and GPU
shaders. It does not implement shading by itself.

The target BRDF structure is the Cook-Torrance microfacet model
f_r ~ D * F * G / (4 * (N.L) * (N.V)), where:
- D: normal distribution function (NDF)
- F: Fresnel reflectance
- G: geometric attenuation / masking-shadowing
*/
public final class MicroFacetedMaterial extends SimpleMaterial
{
    @Serial
    private static final long serialVersionUID = 20260506L;

    public static final int FRESNEL_MODEL_SCHLICK = 0;
    public static final int FRESNEL_MODEL_CONDUCTOR = 1;

    public static final int NDF_MODEL_BECKMANN = 0;
    public static final int NDF_MODEL_GGX = 1;

    public static final int GEOMETRY_MODEL_SMITH = 0;
    public static final int GEOMETRY_MODEL_IMPLICIT = 1;

    // [COOK1982] RMS slope proxy ("m") controlling micro-facet spread.
    private final double roughness;
    // Helper used by common NDF/G parameterization (often alpha = m^2).
    private final double alpha;

    // [COOK1982] Reflectance at normal incidence (per channel), used as
    // practical Fresnel anchor when full spectral data is unavailable.
    private final ColorRgb fresnelF0;
    // [COOK1982] Complex refractive index terms for conductor Fresnel:
    // n (eta) and k (kappa), per channel.
    private final ColorRgb eta;
    private final ColorRgb kappa;

    // Runtime diffuse/specular energy split (implementation policy).
    private final double kd;
    private final double ks;

    // Optional model selectors for runtime shader branching:
    // D term (Beckmann/GGX), F term (Schlick/Conductor), G term.
    private final int fresnelModel;
    private final int ndfModel;
    private final int geometryModel;

    public MicroFacetedMaterial()
    {
        this(defaultConfig());
    }

    public MicroFacetedMaterial(MicroFacetedMaterial other)
    {
        super(
            other.getName(),
            other.getAmbientReference(),
            other.getDiffuseReference(),
            other.getSpecularReference(),
            other.isDoubleSided(),
            other.getReflectionCoefficient(),
            other.getRefractionCoefficient(),
            other.getOpacity(),
            other.getPhongExponent());
        roughness = other.roughness;
        alpha = other.alpha;
        fresnelF0 = new ColorRgb(other.fresnelF0);
        eta = new ColorRgb(other.eta);
        kappa = new ColorRgb(other.kappa);
        kd = other.kd;
        ks = other.ks;
        fresnelModel = other.fresnelModel;
        ndfModel = other.ndfModel;
        geometryModel = other.geometryModel;
    }

    public MicroFacetedMaterial(String csvFileName, String materialName)
    {
        this(loadConfigFromCsv(csvFileName, materialName));
    }

    private MicroFacetedMaterial(MicrofacetConfig config)
    {
        super(
            config.name,
            config.ambient,
            config.diffuse,
            config.specular,
            config.doubleSided,
            config.reflectionCoefficient,
            config.refractionCoefficient,
            config.opacity,
            config.phongExponent);
        roughness = config.roughness;
        alpha = config.alpha;
        fresnelF0 = new ColorRgb(config.fresnelF0);
        eta = new ColorRgb(config.eta);
        kappa = new ColorRgb(config.kappa);
        kd = config.kd;
        ks = config.ks;
        fresnelModel = config.fresnelModel;
        ndfModel = config.ndfModel;
        geometryModel = config.geometryModel;
    }

    public double getRoughness()
    {
        return roughness;
    }

    public double getAlpha()
    {
        return alpha;
    }

    public ColorRgb getFresnelF0()
    {
        return new ColorRgb(fresnelF0);
    }

    public ColorRgb getEta()
    {
        return new ColorRgb(eta);
    }

    public ColorRgb getKappa()
    {
        return new ColorRgb(kappa);
    }

    public double getKd()
    {
        return kd;
    }

    public double getKs()
    {
        return ks;
    }

    public int getFresnelModel()
    {
        return fresnelModel;
    }

    public int getNdfModel()
    {
        return ndfModel;
    }

    public int getGeometryModel()
    {
        return geometryModel;
    }

    private static double clampToUnitInterval(double value)
    {
        if ( value < 0.0 ) {
            return 0.0;
        }
        if ( value > 1.0 ) {
            return 1.0;
        }
        return value;
    }

    private static MicrofacetConfig loadConfigFromCsv(String csvFileName, String materialName)
    {
        if ( csvFileName == null || materialName == null ) {
            return defaultConfig();
        }
        File csvFile = resolveCsvFile(csvFileName);
        if ( csvFile == null || !csvFile.exists() ) {
            throw new IllegalArgumentException("Microfacet CSV file not found: " + csvFileName);
        }

        MicrofacetConfig defaultConfig = defaultConfig();
        String normalizedName = materialName.trim().toLowerCase(Locale.ROOT);
        try (InputStream inputStream = new FileInputStream(csvFile)) {
            String headerLine = PersistenceElement.readAsciiLine(inputStream);
            if ( headerLine == null || headerLine.isBlank() ) {
                throw new IllegalArgumentException("Empty CSV header in file: " + csvFile.getAbsolutePath());
            }
            Map<String, Integer> headerIndex = parseHeaderIndex(headerLine);

            while ( true ) {
                String line;
                try {
                    line = PersistenceElement.readAsciiLine(inputStream);
                }
                catch (Exception eofLike) {
                    break;
                }
                if ( line == null || line.isBlank() ) {
                    continue;
                }
                String[] row = splitCsv(line);
                String rowName = field(row, headerIndex, "material_name");
                if ( rowName == null ) {
                    continue;
                }
                if ( !rowName.trim().toLowerCase(Locale.ROOT).equals(normalizedName) ) {
                    continue;
                }

                double roughness = clampToUnitInterval(doubleField(
                    row, headerIndex, "roughness", defaultConfig.roughness));
                double alpha = clampToUnitInterval(doubleField(
                    row, headerIndex, "alpha", roughness * roughness));
                ColorRgb diffuseColor = rgbField(
                    row, headerIndex, "diffuse_r", "diffuse_g", "diffuse_b", defaultConfig.diffuse);
                ColorRgb f0 = rgbField(
                    row, headerIndex, "f0_r", "f0_g", "f0_b", defaultConfig.fresnelF0);
                ColorRgb eta = rgbField(
                    row, headerIndex, "eta_r", "eta_g", "eta_b", defaultConfig.eta);
                ColorRgb kappa = rgbField(
                    row, headerIndex, "kappa_r", "kappa_g", "kappa_b", defaultConfig.kappa);
                return new MicrofacetConfig(
                    rowName.trim(),
                    defaultConfig.ambient,
                    diffuseColor,
                    f0,
                    defaultConfig.doubleSided,
                    defaultConfig.reflectionCoefficient,
                    defaultConfig.refractionCoefficient,
                    defaultConfig.opacity,
                    defaultConfig.phongExponent,
                    roughness,
                    alpha,
                    f0,
                    eta,
                    kappa,
                    clampToUnitInterval(doubleField(row, headerIndex, "kd", defaultConfig.kd)),
                    clampToUnitInterval(doubleField(row, headerIndex, "ks", defaultConfig.ks)),
                    intField(row, headerIndex, "fresnel_model", FRESNEL_MODEL_SCHLICK),
                    intField(row, headerIndex, "ndf_model", NDF_MODEL_BECKMANN),
                    intField(row, headerIndex, "geometry_model", GEOMETRY_MODEL_SMITH));
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Failed reading microfacet material '" + materialName + "' from " + csvFile.getAbsolutePath(),
                e);
        }

        throw new IllegalArgumentException(
            "SimpleMaterial '" + materialName + "' not found in CSV: " + csvFile.getAbsolutePath());
    }

    private static File resolveCsvFile(String csvFileName)
    {
        File direct = new File(csvFileName);
        if ( direct.exists() ) {
            return direct;
        }
        File fromEtc = new File("etc/materials", csvFileName);
        if ( fromEtc.exists() ) {
            return fromEtc;
        }
        return direct;
    }

    private static Map<String, Integer> parseHeaderIndex(String headerLine)
    {
        String[] header = splitCsv(headerLine);
        Map<String, Integer> headerIndex = new HashMap<String, Integer>();
        for ( int i = 0; i < header.length; i++ ) {
            headerIndex.put(header[i].trim().toLowerCase(Locale.ROOT), Integer.valueOf(i));
        }
        return headerIndex;
    }

    private static String[] splitCsv(String line)
    {
        return line.split(",", -1);
    }

    private static String field(String[] row, Map<String, Integer> headerIndex, String key)
    {
        Integer index = headerIndex.get(key);
        if ( index == null ) {
            return null;
        }
        int i = index.intValue();
        if ( i < 0 || i >= row.length ) {
            return null;
        }
        return row[i].trim();
    }

    private static double doubleField(
        String[] row,
        Map<String, Integer> headerIndex,
        String key,
        double defaultValue)
    {
        String value = field(row, headerIndex, key);
        if ( value == null || value.isBlank() ) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    private static int intField(
        String[] row,
        Map<String, Integer> headerIndex,
        String key,
        int defaultValue)
    {
        String value = field(row, headerIndex, key);
        if ( value == null || value.isBlank() ) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static ColorRgb rgbField(
        String[] row,
        Map<String, Integer> headerIndex,
        String keyR,
        String keyG,
        String keyB,
        ColorRgb defaultValue)
    {
        double r = doubleField(row, headerIndex, keyR, defaultValue.r());
        double g = doubleField(row, headerIndex, keyG, defaultValue.g());
        double b = doubleField(row, headerIndex, keyB, defaultValue.b());
        return new ColorRgb(r, g, b);
    }

    private static MicrofacetConfig defaultConfig()
    {
        return new MicrofacetConfig(
            "VSDK_default_material",
            new ColorRgb(0.1, 0.1, 0.1),
            new ColorRgb(0.9, 0.5, 0.5),
            new ColorRgb(1, 1, 1),
            true,
            0.0,
            0.0,
            1.0,
            128.0,
            0.35,
            0.35 * 0.35,
            new ColorRgb(0.04, 0.04, 0.04),
            new ColorRgb(1.5, 1.5, 1.5),
            new ColorRgb(0.0, 0.0, 0.0),
            1.0,
            1.0,
            FRESNEL_MODEL_SCHLICK,
            NDF_MODEL_BECKMANN,
            GEOMETRY_MODEL_SMITH);
    }

    private record MicrofacetConfig(
        String name,
        ColorRgb ambient,
        ColorRgb diffuse,
        ColorRgb specular,
        boolean doubleSided,
        double reflectionCoefficient,
        double refractionCoefficient,
        double opacity,
        double phongExponent,
        double roughness,
        double alpha,
        ColorRgb fresnelF0,
        ColorRgb eta,
        ColorRgb kappa,
        double kd,
        double ks,
        int fresnelModel,
        int ndfModel,
        int geometryModel)
    {
    }
}
