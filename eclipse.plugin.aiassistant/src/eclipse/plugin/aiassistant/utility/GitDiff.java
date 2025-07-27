package eclipse.plugin.aiassistant.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;

/**
 * This class provides utility methods for interacting with Git repositories in Eclipse.
 */
@SuppressWarnings("restriction")
public class GitDiff {

	private static final String HEAD_REF = "HEAD";
	private static final String INDEX_REF = "INDEX";

	/**
	 * Private constructor to prevent instantiation of the class.
	 */
	private GitDiff() {
	}

	/**
	 * Checks if a Git repository is available for the active editor.
	 *
	 * @return true if a Git repository is found, false otherwise
	 */
	public static boolean isRepositoryAvailable() {
		try {
			getActiveRepository();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	/**
	 * Returns the staged changes for the current project as a formatted diff string.
	 *
	 * @return the staged changes diff for current project
	 */
	public static String getCurrentProjectStagedDiff() {
		return generateDiff(HEAD_REF, INDEX_REF, false);
	}

	/**
	 * Returns a diff for the current project between two commits as a formatted diff string.
	 *
	 * @param oldCommit the old commit ID (e.g., "HEAD~1")
	 * @param newCommit the new commit ID (e.g., "HEAD")
	 * @return the diff for current project between commits
	 */
	public static String getCurrentProjectCommitDiff(String oldCommit, String newCommit) {
		return generateDiff(oldCommit, newCommit, false);
	}

	/**
	 * Returns the staged changes for the current file as a formatted diff string.
	 *
	 * @return the staged changes diff for current file
	 */
	public static String getCurrentFileStagedDiff() {
		return generateDiff(HEAD_REF, INDEX_REF, true);
	}

	/**
	 * Returns a diff for the current file between two commits as a formatted diff string.
	 *
	 * @param oldCommit the old commit ID (e.g., "HEAD~1")
	 * @param newCommit the new commit ID (e.g., "HEAD")
	 * @return the diff for current file between commits
	 */
	public static String getCurrentFileCommitDiff(String oldCommit, String newCommit) {
		return generateDiff(oldCommit, newCommit, true);
	}

	/**
	 * Generates a diff based on the specified references and scope.
	 *
	 * @param oldRef the old reference ("HEAD", "INDEX", or commit ID)
	 * @param newRef the new reference ("HEAD", "INDEX", or commit ID)
	 * @param currentFileOnly if true, limit diff to current file; if false, include entire repository
	 * @return the formatted diff string
	 */
	private static String generateDiff(String oldRef, String newRef, boolean currentFileOnly) {
		Repository repository = getActiveRepository();

		try (Git git = new Git(repository)) {
			AbstractTreeIterator oldTree = getTreeIterator(repository, oldRef);
			AbstractTreeIterator newTree = getTreeIterator(repository, newRef);

			var diffCommand = git.diff().setOldTree(oldTree).setNewTree(newTree);

			if (currentFileOnly) {
				String filePath = getCurrentFilePath(repository);
				diffCommand.setPathFilter(PathFilter.create(filePath));
			}

			List<DiffEntry> changes = diffCommand.call();
			return formatDiff(repository, changes);
		} catch (Exception e) {
			throw new RuntimeException("Error generating diff", e);
		}
	}

	/**
	 * Gets the appropriate tree iterator for the given reference.
	 *
	 * @param repository the Git repository
	 * @param ref the reference ("HEAD", "INDEX", or commit ID)
	 * @return the tree iterator
	 */
	private static AbstractTreeIterator getTreeIterator(Repository repository, String ref) {
		try {
			if (INDEX_REF.equals(ref)) {
				return prepareIndexTreeParser(repository);
			} else {
				return prepareTreeParser(repository, ref);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error creating tree iterator for " + ref, e);
		}
	}

	/**
	 * Gets the repository-relative path of the current file.
	 *
	 * @param repository the Git repository
	 * @return the repository-relative path of the current file
	 */
	private static String getCurrentFilePath(Repository repository) {
		IResource activeResource = Eclipse.getActiveResource();
		if (activeResource == null) {
			throw new RuntimeException("No active resource found");
		}

		String workingTreePath = repository.getWorkTree().getAbsolutePath();
		String resourcePath = activeResource.getLocation().toOSString();

		if (!resourcePath.startsWith(workingTreePath)) {
			throw new RuntimeException("Current file is not in the Git repository");
		}

		return resourcePath.substring(workingTreePath.length() + 1).replace('\\', '/');
	}

	/**
	 * Returns the Git repository for the active editor's resource.
	 * Falls back to the selected project if no active editor is available.
	 *
	 * @return the Git repository
	 * @throws RuntimeException if no Git repository is found
	 */
	private static Repository getActiveRepository() {
		IResource activeResource = Eclipse.getActiveResource();

		// Fallback to selected project if no active editor
		if (activeResource == null) {
			IProject selectedProject = Eclipse.getSelectedProject();
			if (selectedProject != null) {
				activeResource = selectedProject;
			}
		}

		if (activeResource == null) {
			throw new RuntimeException("No active resource or selected project found");
		}

		RepositoryMapping mapping = RepositoryMapping.getMapping(activeResource);
		if (mapping == null) {
			throw new RuntimeException("Resource is not in a Git repository");
		}

		Repository repository = mapping.getRepository();
		if (repository == null) {
			throw new RuntimeException("Git repository is not accessible");
		}

		return repository;
	}

	/**
	 * Prepares a tree parser for the given commit reference.
	 *
	 * @param repository the Git repository
	 * @param commitRef the commit reference (e.g., "HEAD", "HEAD~1", commit hash)
	 * @return the tree parser
	 * @throws IOException if an error occurs while preparing the tree parser
	 * @throws RuntimeException if the commit reference is invalid
	 */
	private static AbstractTreeIterator prepareTreeParser(Repository repository, String commitRef) throws IOException {
		ObjectId objectId = repository.resolve(commitRef);
		if (objectId == null) {
			throw new RuntimeException("Invalid commit reference: " + commitRef);
		}

		try (RevWalk walk = new RevWalk(repository)) {
			var commit = walk.parseCommit(objectId);
			var treeId = commit.getTree().getId();

			try (var reader = repository.newObjectReader()) {
				return new CanonicalTreeParser(null, reader, treeId);
			}
		}
	}

	/**
	 * Prepares a tree parser for the index (staging area).
	 *
	 * @param repository the Git repository
	 * @return the tree parser for the index
	 * @throws IOException if an error occurs while preparing the tree parser
	 */
	private static AbstractTreeIterator prepareIndexTreeParser(Repository repository) throws IOException {
		try (var inserter = repository.newObjectInserter();
				var reader = repository.newObjectReader()) {
			var treeId = repository.readDirCache().writeTree(inserter);
			return new CanonicalTreeParser(null, reader, treeId);
		}
	}

	/**
	 * Formats a list of diff entries into a unified diff string.
	 *
	 * @param repository the Git repository
	 * @param diffEntries the list of diff entries to format
	 * @return the formatted diff string
	 * @throws IOException if an error occurs while formatting the diff
	 */
	private static String formatDiff(Repository repository, List<DiffEntry> diffEntries) throws IOException {
		try (var out = new ByteArrayOutputStream();
				var formatter = new DiffFormatter(out)) {
			formatter.setRepository(repository);
			formatter.setDiffComparator(RawTextComparator.DEFAULT);
			formatter.setDetectRenames(true);

			for (DiffEntry diff : diffEntries) {
				formatter.format(diff);
			}
			return out.toString("UTF-8");
		}
	}
}