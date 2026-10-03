#ifndef __COMMAND_RESULT__
#define __COMMAND_RESULT__

/**
Result of executing a command.
*/
enum class CommandResult {
    /** The command was executed */
    DONE,
    /** The command was recognized, but it could not be executed */
    FAILED,
    /** The command is not executed by this class */
    NOT_HANDLED
};

/**
@param result result of a command
@return the name of the result, as Java `CommandResult.toString`
*/
inline const char* commandResultName(CommandResult result)
{
    switch ( result ) {
      case CommandResult::DONE: return "DONE";
      case CommandResult::FAILED: return "FAILED";
      default: return "NOT_HANDLED";
    }
}

#endif
