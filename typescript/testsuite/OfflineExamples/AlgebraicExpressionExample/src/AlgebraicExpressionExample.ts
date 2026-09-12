//= This example serves as a testbed for AlgebraicExpression class.           =

// VSDK classes
import { AlgebraicExpression, Double } from "@vitral/base";

export class AlgebraicExpressionExample {
    public static main(args: string[]): void {
        const regexp: AlgebraicExpression = new AlgebraicExpression();

        try {
            if (args.length <= 0) {
                regexp.setExpression("666.0");
            } else {
                let joined = "";
                let i: number;
                for (i = 0; i < args.length; i++) {
                    joined += args[i];
                    if (i < args.length - 1) {
                        joined += " ";
                    }
                }
                console.log("Parsing from " + args.length + ' parameters with regexp "' + joined + '"');
                regexp.setExpression(joined);
            }
            console.log("REGEXP:\n" + regexp);
            console.log("REGEXP VALUE:\n" + Double.toString(regexp.eval()));
        } catch (e) {
            console.log("Error processing regular expression." + e);
        }
    }
}

AlgebraicExpressionExample.main(process.argv.slice(2));
