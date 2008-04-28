//===========================================================================
//= This example serves as a testbed for AlgebraicExpression class.           =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 14 2007 - Oscar Chavarro: Original base version               =
//===========================================================================

// VSDK classes
import vsdk.toolkit.common.AlgebraicExpression;

public class AlgebraicExpressionExample 
{
    public static void main (String[] args) {
        AlgebraicExpression regexp = new AlgebraicExpression();

        try {
            if ( args.length <= 0 ) {
                regexp.setExpression("666.0");
            }
            else {
                String joined = "";
                int i;
                for ( i = 0; i < args.length; i++ ) {
                    joined += args[i];
                    if ( i < args.length - 1 ) {
                        joined += " ";
                    }
                }
                System.out.println("Parsing from " + args.length + " parameters with regexp \"" + joined + "\"");
                regexp.setExpression(joined);
            }
            System.out.println("REGEXP:\n" + regexp);
            System.out.println("REGEXP VALUE:\n" + regexp.eval());
        }
        catch ( Exception e ) {
            System.out.println("Error processing regular expression." + e);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
