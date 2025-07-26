package eclipse.plugin.aiassistant.prompt;

public enum Prompts {

	SYSTEM("System Message", "system.txt"),
	DEVELOPER("Developer Message (OpenAI reasoning models ONLY)", "developer.txt"),
	DEFAULT("Default", "default.txt"),
	DEFAULT_DELAYED("Default Delayed", "default-delayed.txt"),
	ADD_MESSAGE("Add To Message", "add-message.txt"),
	ADD_CONTEXT("Add As Context", "add-context.txt"),
	DISCUSS("Discuss", "discuss.txt"),
	EXPLAIN("Explain", "explain.txt"),
	CODE_REVIEW("Code Review", "code-review.txt"),
	BEST_PRACTCES("Best Practices", "best-practices.txt"),
	ROBUSTIFY("Robustify", "robustify.txt"),
	OPTIMIZE("Optimize", "optimize.txt"),
	DEBUG("Debug", "debug.txt"),
	CODE_GENERATION("Code Generation", "code-generation.txt"),
	CODE_COMPLETION("Code Completion", "code-completion.txt"),
	WRITE_COMMENTS("Write Comments", "write-comments.txt"),
	REFACTOR("Refactor", "refactor.txt"),
	FIX_ERRORS("Fix Errors", "fix-errors.txt"),
	FIX_WARNINGS("Fix Warnings", "fix-warnings.txt"),
	PASTE_MESSAGE("Paste To Message", "paste-message.txt"),
	PASTE_CONTEXT("Paste As Context", "paste-context.txt"),
	ADD_GIT_DIFF("Add Git Diff", "add-git-diff.txt"),
	GIT_COMMIT_COMMENT("Git Commit Comment", "git-commit-comment.txt");

	private final String taskName;
	private final String fileName;

	/**
	 * Constructs a new Prompts enum constant with the specified task name and file name.
	 *
	 * @param taskName the name of the task associated with this prompt type
	 * @param fileName the name of the file containing the prompt text for this type
	 */
	private Prompts(String taskName, String fileName) {
		this.taskName = taskName;
		this.fileName = fileName;
	}

	/**
	 * Returns the preference name for this prompt type.
	 *
	 * @return the preference name for this prompt type
	 */
	public String preferenceName() {
		return "preference.prompt." + name();
	}

	/**
	 * Returns the task name associated with this prompt type.
	 *
	 * @return the task name associated with this prompt type
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * Returns the file name containing the prompt text for this type.
	 *
	 * @return the file name containing the prompt text for this type
	 */
	public String getFileName() {
		return fileName;
	}

}