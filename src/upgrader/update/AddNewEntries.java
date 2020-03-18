package upgrader.update;

import static upgrader.upgrade.Upgrader.plog;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.FileUtils;

import upgrader.upgrade.*;

/**
 * Add new entries to .classpath and build.xml
 * @author Zabee
 */
public class AddNewEntries
    extends AbstractApplicationState {

  static private List<String> filesForAction = new ArrayList<String>();
  static private String toDoFile = "LauncherToDoFile.txt";
  protected String oldFilename;
  protected List<Path> oldFiles;
  protected Path newFile;
  protected TreeMap<Path, List<String>> occurences;
  protected Upgrader up;

  public AddNewEntries(Upgrader argUp) {
    this.up = argUp;
  }

  @Override
  public void process() {

    // First let's upgrade the jar files.
    plog("Replacement is in progress...");
    for (Path oldFile : oldFiles) {
      try {
        // Copy the new library in the same folder where the referenced library is in.
        Files.copy(newFile, Paths.get(oldFile.getParent().toString(), newFile.getFileName().toString()),
            StandardCopyOption.REPLACE_EXISTING);
        plog(newFile.toString() + "	Copied");
      }
      catch (IOException ex) {
        ex.printStackTrace();
        plog(ex.getMessage());
        System.exit(1);
      }
    }

    boolean launcherFileInvolved = false;
    boolean isLaunchFileCandidate = false;
    for (Path cp : occurences.keySet()) {
      try {
        isLaunchFileCandidate = false;
        File file = new File(cp.toString());
        List<String> lines = new CopyOnWriteArrayList<String>(FileUtils.readLines(file, "UTF-8"));
        String fileName = cp.getFileName().toString();
        if (!isLaunchFileCandidate && fileName.endsWith(".launch")) {
          for (String line : lines) {
            if (line.startsWith("<stringAttribute") && line.contains(oldFilename)) {
              filesForAction.add(file.getAbsolutePath());

              // Once found, no need to visit the .launch file again
              isLaunchFileCandidate = true;
              launcherFileInvolved = true;
            }
          }

        }
        else {
          String fileType = fileName.substring(fileName.indexOf("."));
          StringBuffer newEntry = new StringBuffer();
          boolean anyChanges = false;
          int index = 0;
          for (String line : lines) {
            index++ ;
            if (line.contains(oldFilename)) {
              anyChanges = true;
              // (1) classpath
              if (fileType.equalsIgnoreCase(".classpath") && line.contains("<classpathentry")) {
                try {
                  newEntry.append(line);
                  int startingIndex = line.indexOf(oldFilename);
                  newEntry.replace(startingIndex, startingIndex + oldFilename.length(),
                      newFile.getFileName().toString());
                  // What if the new jar has sources associated?
                  // Then let me remove the source part from <classpath> tag and let the user to
                  // add sources separately
                  String tempEntry = newEntry.toString();
                  if (tempEntry.contains("sourcepath")) {
                    tempEntry = tempEntry.substring(0, tempEntry.indexOf("sourcepath") - 1);
                    tempEntry = tempEntry.concat("/>");
                  }
                  lines.add(index, tempEntry);
                }
                catch (IndexOutOfBoundsException e) {}
              }
              else if (fileName.equalsIgnoreCase("build.xml")) {
                // (2) build.xml
                newEntry.append(line);
                int startingIndex = line.indexOf(oldFilename);
                newEntry.replace(startingIndex, startingIndex + oldFilename.length(),
                    newFile.getFileName().toString());
                lines.add(index, newEntry.toString());
              }
            }
          }
          if (anyChanges) {
            FileUtils.writeLines(file, lines, false);
          }
        }
      }
      catch (IOException ex) {
        ex.printStackTrace();
        plog(ex.getMessage());
        System.exit(1);
      }
    }
    if (launcherFileInvolved) {
      try {
        Files.write(Paths.get(toDoFile), ("\n \n" + newFile.getFileName().toString().toUpperCase()
            + " - FOR ADDITION:" + filesForAction.toString()).getBytes(), StandardOpenOption.APPEND);
      }
      catch (IOException e) {
        plog();
        System.exit(-1);
      }
    }
    plog("");
    plog("***Successfully completed the adding process*****\""
        + Paths.get(toDoFile).getFileName().toString().toUpperCase() + "\" is for your action");

    // So replacement/upgrade is complete now. User may want to do another
    // replacement/upgrade.
    SearchLibraryState newState = new SearchLibraryState(up);
    up.currentState = newState;
    up.currentState.process();

  }

}
