package upgrader.upgrade;

import static upgrader.upgrade.Upgrader.getUserInputThenLog;
import static upgrader.upgrade.Upgrader.plog;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * The application state where you locate the new library that is to replace old
 * ones.
 * 
 * @author bsanchin
 */
public class LocateReplacementState extends AbstractApplicationState {

	protected Upgrader up;
	protected String oldQuery;
	protected String oldFilename;
	protected List<Path> oldFiles;
	protected Path newFile;
	protected String newFileName;
	boolean isRenamedUpgrade = false;

	public LocateReplacementState(Upgrader argUp) {
		this.up = argUp;
	}

	@Override
	public void process() {

		// Search for a library (*.jar) in new.library dir.
		List<Path> matchingFiles = null;
		if (isRenamedUpgrade) {
			matchingFiles = walkTreeAndRetrieveFiles(up.getReplacementLibrariesDir(), ".jar", newFileName);
		} else {
			matchingFiles = walkTreeAndRetrieveFiles(up.getReplacementLibrariesDir(), ".jar", oldQuery);
		}
		if (matchingFiles.size() > 0) {
			plog("Select a replacement file to proceed with upgrade. Or simply search for other replacement library.");
		} else {
			plog("No files found.");
			plog("What do you want to do? (Retry|New Search)");
			String input = getUserInputThenLog().trim().toLowerCase();

			// The user wants to repeat the same search. Maybe he has placed a new jar file
			// just a second ago.
			if (input.equals("r") || input.equals("retry")) {
				newFile = null;
				process();
			}
			// Let's start a brand new search.
			else {
				SearchLibraryState newState = new SearchLibraryState(up);
				up.currentState = newState;
				newState.process();
				return;
			}
		}

		int count = 0;
		HashMap<Integer, Path> options = new HashMap<Integer, Path>();

		// Show the possible candidates.
		for (Path jarfile : matchingFiles) {
			plog("[" + count + "] " + jarfile.toAbsolutePath().toString() + SPACE_SEP
					+ md5(jarfile.toAbsolutePath().toString()) + SPACE_SEP
					+ getHumanReadableByteCount(jarfile.toAbsolutePath().toString()));
			options.put(count, jarfile);
			count++;
		}

		String userInput = getUserInputThenLog().trim();
		// User selected a library that would replace the old ones.
		if (NumberUtils.isNumber(userInput) && Integer.parseInt(userInput) >= 0
				&& Integer.parseInt(userInput) <= count) {
			newFile = options.get(new Integer(userInput));
			plog("Selected: " + newFile.getFileName().toString());

			LocateReferencedFilesState newState = new LocateReferencedFilesState(up);
			newState.oldFilename = oldFilename;
			newState.oldFiles = oldFiles;
			newState.newFile = newFile;
			newState.isRenamedUpgrade = isRenamedUpgrade;
			up.currentState = newState;
			up.currentState.process();
		}
		// The user wants to search for another candidate library.
		else {
			oldQuery = userInput;
			process();
		}
	}

}
