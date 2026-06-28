package options;

import model.SolidTextureModel;

public class CommandLineOptions {
    private final SolidTextureModel model;

    public CommandLineOptions(SolidTextureModel model) {
        this.model = model;
    }

    public void processArguments(String[] args) {
        if ( args == null ) {
            return;
        }

        for ( int i = 0; i < args.length; i++ ) {
            String arg = args[i];
            if ( "-tangibleServer".equals(arg) && i + 1 < args.length ) {
                model.setTangibleServiceUrl(args[++i]);
            }
        }
    }
}
