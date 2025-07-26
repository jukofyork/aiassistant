package eclipse.plugin.aiassistant.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.Git;

public class CopyGitDiffHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			String diff = Git.getStagedDiff();
			Eclipse.setClipboardContents(diff);
		} catch (Exception e) {
			Eclipse.setClipboardContents("Error getting git diff: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}