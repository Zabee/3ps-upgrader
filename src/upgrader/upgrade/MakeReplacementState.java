package upgrader.upgrade;

import static upgrader.upgrade.Upgrader.plog;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.TreeMap;

/**
 * The application state where all replacement/upgrade takes place.
 * @author bsanchin
 */
public class MakeReplacementState
    extends AbstractApplicationState {

  protected String oldFilename;
  protected List<Path> oldFiles;
  protected Path newFile;
  protected TreeMap<Path, List<String>> occurences;
  protected Upgrader up;
  boolean isRenamedUpgrade = false;
  public MakeReplacementState(Upgrader argUp) {
    this.up = argUp;
  }

  @Override
  public void process() {

    // First let's upgrade the jar files.
    plog("Replacement is in progress...");
    for (Path oldFile : oldFiles) {
      try {
        // Delete the old library.
        Files.delete(oldFile);
        // Copy the new library in the same folder where the old library was in.
        Files.copy(newFile, Paths.get(oldFile.getParent().toString(), newFile.getFileName().toString()),
            StandardCopyOption.REPLACE_EXISTING);
        plog("  Replaced " + oldFile.toString() + " with " + newFile.toString());
      }
      catch (IOException ex) {
        ex.printStackTrace();
        plog(ex.getMessage());
        System.exit(1);
      }
    }

    // Update .classpath files that have refernces to the old library.
    for (Path cp : occurences.keySet()) {
      try {
        byte[] bytes = Files.readAllBytes(cp);
        String fileContent = new String(bytes);
        // The old library name should be replaced by the library name.
        fileContent = fileContent.replace(oldFilename, newFile.getFileName().toString());
        // Overwrite the existing .classpath file.
        Files.write(cp, fileContent.getBytes(), StandardOpenOption.WRITE);
        List<String> list = occurences.get(cp);
        // Show what it updated.
        for (String line : list) {
          plog("  Updated => " + cp + ":" + line.replace(oldFilename, newFile.getFileName().toString()));
        }
      }
      catch (IOException ex) {
        ex.printStackTrace();
        plog(ex.getMessage());
        System.exit(1);
      }
    }

    plog("*** Replacement is complete! ***");

    // So replacement/upgrade is complete now. User may want to do another replacement/upgrade.
    SearchLibraryState newState = new SearchLibraryState(up);
    up.currentState = newState;
    up.currentState.process();

  }

}
