package eclipse.plugin.aiassistant.handlers;

import eclipse.plugin.aiassistant.prompt.Prompts;

public class CodeGenerationPromptHandler extends AbstractPromptHandler {
	public CodeGenerationPromptHandler() {
		super(Prompts.CODE_GENERATION);
	}
}