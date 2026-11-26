// Covers the TypeScript port of cpp/base/src/main/vsdk/toolkit/common/color/Cie.{h,cpp}.
// The C++ port has no unit test for this class; the expected values below
// were printed (with %.17g) by a C++ driver compiled against Cie.cpp, so
// they check bit-level parity of the conversion matrices and gray factors.
import { describe, expect, it } from "vitest";

import { Cie } from "vsdk/toolkit/common/color/Cie.js";

describe("CieTest", () => {
    function expectTriple(actual: number[], x: number, y: number, z: number): void {
        expect(actual[0]).toBe(x);
        expect(actual[1]).toBe(y);
        expect(actual[2]).toBe(z);
    }

    it("given_defaultPrimaries_when_constructed_then_keepsNominalCrtChromaticities", () => {
        const cie = new Cie();
        expect(cie.getRedX()).toBe(0.64);
        expect(cie.getRedY()).toBe(0.33);
        expect(cie.getGreenX()).toBe(0.29);
        expect(cie.getGreenY()).toBe(0.6);
        expect(cie.getBlueX()).toBe(0.15);
        expect(cie.getBlueY()).toBe(0.06);
        expect(cie.getWhiteX()).toBe(0.3333333333);
        expect(cie.getWhiteY()).toBe(0.3333333333);
        expect(cie.getLuminousEfficacy()).toBe(183.07);
    });

    it("given_defaultPrimaries_when_transformed_then_matchesCppValues", () => {
        const cie = new Cie();
        const rgb = [0, 0, 0];
        const back = [0, 0, 0];
        cie.transformColorFromXYZ2RGB([0.25, 0.5, 0.75], rgb);
        expectTriple(rgb, -0.24094401759191947, 0.76648243188885778, 0.77562270287021751);
        cie.transformColorFromRGB2XYZ(rgb, back);
        expectTriple(back, 0.24999999999999989, 0.49999999999999978, 0.74999999999999967);
        // Out of gamut components are not clamped
        expect(rgb[0]!).toBeLessThan(0);

        const white = [0, 0, 0];
        cie.transformColorFromRGB2XYZ([1, 1, 1], white);
        expectTriple(white, 0.99999999999999989, 0.99999999999999967, 1.0000000003);

        expect(cie.spectrumGray(1, 0, 0)).toBe(0.26510582007412969);
        expect(cie.spectrumGray(0, 1, 0)).toBe(0.67010582011462916);
        expect(cie.spectrumGray(0, 0, 1)).toBe(0.064788359811240759);
        expect(cie.spectrumGray(0.2, 0.4, 0.6)).toBe(0.35993650794742205);
        expect(cie.spectrumLuminance(0.2, 0.4, 0.6)).toBe(65.893576509934547);
    });

    it("given_ntscPrimaries_when_transformed_then_matchesCppValues", () => {
        const cie = new Cie(0.67, 0.33, 0.21, 0.71, 0.14, 0.08, 0.31, 0.316);
        const rgb = [0, 0, 0];
        const back = [0, 0, 0];
        cie.transformColorFromXYZ2RGB([0.25, 0.5, 0.75], rgb);
        expectTriple(rgb, -0.0048840803709427894, 0.73240978445144111, 0.62785255515219196);
        cie.transformColorFromRGB2XYZ(rgb, back);
        expectTriple(back, 0.25000000000000006, 0.50000000000000011, 0.74999999999999989);

        // White maps to the white point, with Y = 1
        const white = [0, 0, 0];
        cie.transformColorFromRGB2XYZ([1, 1, 1], white);
        expectTriple(white, 0.98101265822784822, 1, 1.1835443037974684);

        // The familiar NTSC luminance weights 0.299, 0.587, 0.114
        expect(cie.spectrumGray(1, 0, 0)).toBe(0.29896661812478997);
        expect(cie.spectrumGray(0, 1, 0)).toBe(0.58642121013298343);
        expect(cie.spectrumGray(0, 0, 1)).toBe(0.11461217174222664);
        expect(cie.spectrumGray(0.2, 0.4, 0.6)).toBe(0.36312911072348741);
        expect(cie.spectrumLuminance(0.2, 0.4, 0.6)).toBe(66.478046300148833);
    });
});
