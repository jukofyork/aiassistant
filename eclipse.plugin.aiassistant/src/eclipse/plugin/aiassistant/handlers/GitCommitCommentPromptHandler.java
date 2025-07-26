package eclipse.plugin.aiassistant.handlers;

import eclipse.plugin.aiassistant.prompt.Prompts;

public class GitCommitCommentPromptHandler extends AbstractPromptHandler {
	public GitCommitCommentPromptHandler() {
		super(Prompts.GIT_COMMIT_COMMENT);
	}
}