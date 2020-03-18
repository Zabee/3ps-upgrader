package upgrader.update;

import static upgrader.upgrade.Upgrader.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import upgrader.upgrade.*;

/**
 * The application state it searches for references to old libraries and shows confirmation for the
 * upgrade/changes that would take.
 * 
 * @author Zabee
 */
public class LocateForAddition
    extends AbstractApplicationState {

  static final String CLASSPATH = ".classpath";
  static final String LINE_SEPARATOR = System.getProperty("line.separator");
  static final String FILE_SEPARATOR = System.getProperty("file.separator");
  static final String UNIT_TEST_PATTERN = FILE_SEPARATOR + "TEST-";
  protected String oldFilename;
  protected List<Path> oldFiles;
  protected Path newFile;
  protected Upgrader up;

  LocateForAddition(Upgrader argUp) {
    this.up = argUp;
  }

  @Override
  public void process() {

    TreeSet<Path> matchingFiles = new TreeSet<Path>();

    // Search for references in all files that have pre-configured extension in the library.dir.
    for (String extension : up.getScanForReference()) {
      List<Path> temp = walkTreeAndRetrieveFiles(up.getLibrariesDir(), extension, extension);

      // filter out files
      for (Path p : temp) {
        if (!p.toString().matches(up.getSkipIfFilenameRegex())) {
          matchingFiles.add(p);
        }
      }
    }

    // We need to store information on where the occurrences are.
    TreeMap<Path, List<String>> occurences = new TreeMap<Path, List<String>>();

    for (Path cpFile : matchingFiles) {
      byte[] bytes = new byte[] {};
      try {
        bytes = Files.readAllBytes(cpFile);
      }
      catch (IOException ex) {
        ex.printStackTrace();
        log(ex.getMessage());
        System.exit(1);
      }
      String fileContent = new String(bytes);
      // If the file has reference to the library, we want to know on which line
      // it is.
      if (fileContent.contains(oldFilename)) {
        String[] lines = fileContent.split(LINE_SEPARATOR);
        int lineNo = 1;
        for (String line : lines) {
          if (line.contains(oldFilename)) {
            List<String> list = occurences.get(cpFile);
            if (list == null) {
              list = new ArrayList<String>();
            }
            list.add(lineNo + ": " + line);
            occurences.put(cpFile, list);
          }
          lineNo++ ;
        }
      }
    }
    plog();
    plog("Newly getting added file: ");
    plog("  " + newFile);

    if (occurences.size() > 0) {
      plog();
      plog("Classpath entries will be affected:");
      for (Path cp : occurences.keySet()) {
        for (String occ : occurences.get(cp)) {
          plog("  " + cp + ":" + occ);
        }
      }
    }

    plog();
    plog("Shall we proceed with the replacement task? (Yes|No)");
    String userInput = getUserInputThenLog();

    // User approves the upgrade plan, so let's do the upgrade/replacement.
    if (userInput.equalsIgnoreCase("yes") || userInput.equalsIgnoreCase("y")) {
      AddNewEntries newState = new AddNewEntries(up);
      newState.oldFilename = oldFilename;
      newState.oldFiles = oldFiles;
      newState.newFile = newFile;
      newState.occurences = occurences;
      up.currentState = newState;
      up.currentState.process();
    }
    // User did not approve, therefore, let's start a brand new search.
    else {
      SearchLibraryState newState = new SearchLibraryState(up);
      up.currentState = newState;
      up.currentState.process();
    }

  }

}
