package upgrader.update;

import static upgrader.upgrade.Upgrader.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import upgrader.upgrade.*;

/**
 * The application state where removal takes place.
 *
 * @author Zabee
 * 
 */
public class RemoveReferencedLibrary
    extends AbstractApplicationState {
  static final String LINE_SEPARATOR = System.getProperty("line.separator");
  static protected String toDoFile = "LauncherToDoFile.txt";
  static protected List<String> filesForAction = new ArrayList<String>();
  protected Upgrader up;
  public String oldQuery;
  public String oldFilename;
  public List<Path> oldFiles;
  protected Path newFile;
  protected TreeMap<Path, List<String>> occurences;

  public RemoveReferencedLibrary(Upgrader argUp) {
    this.up = argUp;
  }

  @Override
  public void process() {
    Set<Path> matchingFiles = new TreeSet<Path>();
    // Search for references in all files that have pre-configured extension in the
    // library.dir.
    for (String extension : up.getScanForReference()) {
      List<Path> temp = walkTreeAndRetrieveFiles(up.getLibrariesDir(), extension, extension);
      // filter out files
      for (Path p : temp) {
        if (!p.toString().matches(up.getSkipIfFilenameRegex())) {
          matchingFiles.add(p);
        }
      }
    }

    storeOccurrences(matchingFiles);
    removeLibraryAndUpdateFiles();

    // So replacement/upgrade is complete now. User may want to do another
    // replacement/upgrade/removal.
    SearchLibraryState newState = new SearchLibraryState(up);
    up.currentState = newState;
    up.currentState.process();
  }

  private void removeLibraryAndUpdateFiles() {
    plog("Shall we proceed with the removal task? (Yes|No)");
    String userInput = getUserInputThenLog();
    // User approves the upgrade plan, so let's do the upgrade/replacement.
    if (userInput.equalsIgnoreCase("yes") || userInput.equalsIgnoreCase("y")) {
      plog("Removal is in progress...");
      for (Path oldFile : oldFiles) {
        try {
          // Delete the library.
          Files.delete(oldFile);
          plog("  Removed " + oldFile.toString());
        }
        catch (IOException ex) {
          plog("Looks like the file already deleted. We will try to remove the references if any.");
        }
      }

    }
    // Update .classpath and other files that have references to the old library.
    boolean launcherFileInvolved = false;
    boolean isLaunchFileCandidate = false;
    for (Path cp : occurences.keySet()) {
      try {
        isLaunchFileCandidate = false;
        File file = new File(cp.toString());
        List<String> lines = FileUtils.readLines(file, "UTF-8");
        String fileName = cp.getFileName().toString();
        List<String> updatedLines = new ArrayList<String>();
        if (!isLaunchFileCandidate && fileName.endsWith(".launch")) {
          for (String line : lines) {
            if (line.startsWith("<stringAttribute") && line.contains(oldFilename)) {
              // process and remove the entry.
              filesForAction.add(file.getAbsolutePath());

              // Once found, no need to visit the .launch file again
              isLaunchFileCandidate = true;
              launcherFileInvolved = true;
            }
          }

        }
        else {
          updatedLines = lines.stream().filter(s -> !s.contains((oldFilename))).collect(Collectors.toList());
          FileUtils.writeLines(file, updatedLines, false);
        }
      }
      catch (IOException ex) {
        ex.printStackTrace();
        plog("LauncherToDoFile.txt file doesn't exist. Please create one with the name");
        System.exit(1);
      }
    }
    if (launcherFileInvolved) {
      try {
        Files.write(Paths.get(toDoFile),
            ("\n \n" + oldFilename.toUpperCase() + " FOR REMOVAL:" + filesForAction.toString()).getBytes(),
            StandardOpenOption.APPEND);
        launcherFileInvolved = false;
      }
      catch (IOException e) {
        // TODO Auto-generated catch block
        plog();
        System.exit(-1);
      }
    }
    plog("*** Removal is complete! ***\"" + Paths.get(toDoFile).getFileName().toString().toUpperCase()
        + "\" is for your action");
  }

  private void storeOccurrences(Set<Path> argMatchingFiles) {
    // We need to store information on where the occurrences are.
    occurences = new TreeMap<Path, List<String>>();

    for (Path cpFile : argMatchingFiles) {
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

  }
}
