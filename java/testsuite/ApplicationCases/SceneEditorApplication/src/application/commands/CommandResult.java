package application.commands;

/**
Result of executing a command.
*/
public enum CommandResult
{
    /** The command was executed */
    DONE,
    /** The command was recognized, but it could not be executed */
    FAILED,
    /** The command is not executed by this class */
    NOT_HANDLED
}
