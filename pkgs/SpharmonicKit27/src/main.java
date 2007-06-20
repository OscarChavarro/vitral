//===========================================================================

import vsdk.toolkit.processing.SpharmonicKitWrapper;

class main {

    public static void
    main(String[] args)
    {
        byte image[];
        double sphericalHarmonicsR[];
        double sphericalHarmonicsI[];
        int i, x, y;

        sphericalHarmonicsR = new double[16];
        sphericalHarmonicsI = new double[16];
        image = new byte[64*64];

        for ( i = 0, y = 0; y < 64; y++ ) {
            for ( x = 0; x < 64; x++, i++ ) {
                if ( x < 32 && y < 32 || x > 32 && y > 32 ) {
                    image[i] = 0;
                }
                else {
                    image[i] = -1;
                }
            }
        }

        boolean status = 
        SpharmonicKitWrapper.calculateSphericalHarmonics(
            image, sphericalHarmonicsR, sphericalHarmonicsI);

        if ( !status ) {
            System.err.println("Error calling native method.\n");
        }
        else {
            for ( i = 0; i < sphericalHarmonicsR.length; i++ ) {
                System.out.printf("  - <%.2f, %.2f>\n",
                    sphericalHarmonicsR[i], sphericalHarmonicsI[i]);
            }
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
