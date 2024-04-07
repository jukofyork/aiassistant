package eclipse.plugin.aiassistant.handlers;

import eclipse.plugin.aiassistant.prompt.Prompts;

public class CodeCompletionPromptHandler extends AbstractPromptHandler {
	public CodeCompletionPromptHandler() {
		super(Prompts.CODE_COMPLETION);
	}
}