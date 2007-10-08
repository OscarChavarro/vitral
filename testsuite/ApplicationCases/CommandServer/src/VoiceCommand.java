//===========================================================================

// Java basic classes
import java.util.ArrayList;

public class VoiceCommand
{
    private String commandName;
    private ArrayList<String> speechSet;

    public VoiceCommand(String name)
    {
        commandName = new String(name);
        speechSet = new ArrayList<String>();
    }

    public String getName()
    {
        return commandName;
    }

    public void addSpeech(String speech)
    {
        speechSet.add(speech);
    }

    public boolean selectFromSpeech(String speech)
    {
        int i;
        String speechToBeTested;
        CharSequence subString;

        speech = speech.toLowerCase();

        for ( i = 0; i < speechSet.size(); i++ ) {
            speechToBeTested = speechSet.get(i);

            int j;
            for ( j = 0; j < speech.length(); j++ ) {
                if ( speech.charAt(j) != ' ' ) break;
            }
            subString = speech.subSequence(j, speech.length());
            
            if ( speechToBeTested.contains(subString) ) {
                return true;
            }
        }
        return false;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
