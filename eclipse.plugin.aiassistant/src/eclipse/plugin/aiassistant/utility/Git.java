package eclipse.plugin.aiassistant.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.ui.IEditorPart;

/**
 * This class provides utility methods for interacting with Git repositories in Eclipse.
 */
@SuppressWarnings("restriction")
public class Git {

	/**
	 * Private constructor to prevent instantiation of the class.
	 */
	private Git() {
	}

	/**
	 * Returns the Git repository for the active editor's resource.
	 *
	 * @return the Git repository or null if not found
	 */
	public static Repository getActiveRepository() {
		IEditorPart activeEditor = Eclipse.getActiveEditor();
		if (activeEditor == null) {
			return null;
		}

		IResource activeResource = activeEditor.getEditorInput().getAdapter(IResource.class);
		if (activeResource == null) {
			return null;
		}

		return getRepository(activeResource);
	}

	/**
	 * Returns the Git repository for the given resource.
	 *
	 * @param resource the resource to get the repository for
	 * @return the Git repository or null if not found
	 */
	public static Repository getRepository(IResource resource) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		if (mapping == null) {
			return null;
		}
		return mapping.getRepository();
	}

	/**
	 * Returns the staged changes as a formatted diff string.
	 *
	 * @return the staged changes diff or an error message
	 */
	public static String getStagedDiff() {
		Repository repository = getActiveRepository();
		if (repository == null) {
			return "No Git repository found";
		}

		try (org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repository)) {
			ObjectId head = repository.resolve("HEAD");
			if (Objects.isNull(head)) {
				return "Initial commit: No previous commits found.";
			}

			AbstractTreeIterator headTree = prepareTreeParser(repository, head);
			AbstractTreeIterator indexTree = prepareIndexTreeParser(repository);
			List<DiffEntry> stagedChanges = git.diff().setOldTree(headTree).setNewTree(indexTree).call();

			String patch = formatDiff(repository, stagedChanges);
			return patch.isEmpty() ? "No staged changes found" : patch;
		} catch (Exception e) {
			throw new RuntimeException("Error getting staged diff", e);
		}
	}

	/**
	 * Returns the working directory changes as a formatted diff string.
	 *
	 * @return the working directory changes diff or an error message
	 */
	public static String getWorkingDiff() {
		Repository repository = getActiveRepository();
		if (repository == null) {
			return "No Git repository found";
		}

		try (org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repository)) {
			AbstractTreeIterator indexTree = prepareIndexTreeParser(repository);
			List<DiffEntry> workingChanges = git.diff().setOldTree(indexTree).call();

			String patch = formatDiff(repository, workingChanges);
			return patch.isEmpty() ? "No working directory changes found" : patch;
		} catch (Exception e) {
			throw new RuntimeException("Error getting working diff", e);
		}
	}

	/**
	 * Returns a diff between two commits as a formatted diff string.
	 *
	 * @param oldCommit the old commit ID (e.g., "HEAD~1")
	 * @param newCommit the new commit ID (e.g., "HEAD")
	 * @return the diff between commits or an error message
	 */
	public static String getCommitDiff(String oldCommit, String newCommit) {
		Repository repository = getActiveRepository();
		if (repository == null) {
			return "No Git repository found";
		}

		try (org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repository)) {
			ObjectId oldId = repository.resolve(oldCommit);
			ObjectId newId = repository.resolve(newCommit);

			if (oldId == null || newId == null) {
				return "Invalid commit references: " + oldCommit + " or " + newCommit;
			}

			AbstractTreeIterator oldTree = prepareTreeParser(repository, oldId);
			AbstractTreeIterator newTree = prepareTreeParser(repository, newId);
			List<DiffEntry> changes = git.diff().setOldTree(oldTree).setNewTree(newTree).call();

			String patch = formatDiff(repository, changes);
			return patch.isEmpty() ? "No changes found between commits" : patch;
		} catch (Exception e) {
			throw new RuntimeException("Error getting commit diff", e);
		}
	}

	/**
	 * Prepares a tree parser for the given object ID.
	 *
	 * @param repository the Git repository
	 * @param objectId   the object ID to prepare the tree parser for
	 * @return the tree parser
	 * @throws IOException if an error occurs while preparing the tree parser
	 */
	public static AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
			var commit = walk.parseCommit(objectId);
			var treeId = commit.getTree().getId();

			try (var reader = repository.newObjectReader()) {
				return new CanonicalTreeParser(null, reader, treeId);
			}
		}
	}

	/**
	 * Prepares a tree parser for the index (staged changes).
	 *
	 * @param repository the Git repository
	 * @return the tree parser for the index
	 * @throws IOException if an error occurs while preparing the tree parser
	 */
	public static AbstractTreeIterator prepareIndexTreeParser(Repository repository) throws IOException {
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
	public static String formatDiff(Repository repository, List<DiffEntry> diffEntries) throws IOException {
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