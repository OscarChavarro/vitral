// Java basic classes
import java.util.ArrayList;

public class VoiceCommandSet
{
    private ArrayList<VoiceCommand> commandSet;

    public VoiceCommandSet()
    {
        commandSet = new ArrayList<VoiceCommand>();
    }

    private VoiceCommand searchWithCreate(String commandName)
    {
        VoiceCommand candidate;
        int i;
        for ( i = 0; i < commandSet.size(); i++ ) {
            candidate = commandSet.get(i);
            if ( candidate.getName().equals(commandName) ) {
                return candidate;
            }
        }
        candidate = new VoiceCommand(commandName);
        commandSet.add(candidate);
        return candidate;
    }

    public void addSpeech(String commandName, String speech)
    {
        VoiceCommand vc;
        vc = searchWithCreate(commandName);
        vc.addSpeech(speech);
    }

    public int size()
    {
        return commandSet.size();
    }

    public VoiceCommand get(int i)
    {
        return commandSet.get(i);
    }

    public VoiceCommand searchVoiceCommandFromSpeech(String speech)
    {
        int i;
        VoiceCommand candidate;

        System.out.println("Testing for speech [" + speech + "]");

        for ( i = 0; i < commandSet.size(); i++ ) {
            candidate = commandSet.get(i);
            if ( candidate.selectFromSpeech(speech) ) {
                return candidate;
            }
        }
        return null;
    }
}
