package upgrader.upgrade;

import static upgrader.upgrade.Upgrader.getUserInputThenLog;
import static upgrader.upgrade.Upgrader.plog;

import java.nio.file.Path;
import java.util.*;

import org.apache.commons.lang3.math.NumberUtils;

import upgrader.update.RemoveReferencedLibrary;
import upgrader.update.SplitLibraries;

/**
 * The initial state of the application. The whole purpose of this state is to locate libraries that need an
 * upgrade.
 * 
 * @author bsanchin
 */
public class SearchLibraryState
    extends AbstractApplicationState {

  protected Upgrader up;
  private String query;
  private String command;
  private List<String> cmdList = new ArrayList<String>();
  boolean removeCommand = false, splitCommand = false, renamedUpgrade = false, sourcesAdd = false;;
  String splCmd;
  boolean isRenamedUpgrade = false;

  public SearchLibraryState(Upgrader argUp) {
    this.up = argUp;
  }

  @Override
  public void process() {
    if (query == null) {
      plog("What you want to do?");
      plog("	Upgrade                 : library-name");
      plog("	Replaced/Renamed upgrade: oldlibrary-name newlibrary-name -ru");
      plog("	Adding as an existing   : oldlibrary-name newlibrary-name -a ");
      plog("	Remove existing         : library-name -r");
      plog("	Exit from tool          : exit");
      System.out.print("Please input your command:");
      query = getUserInputThenLog().toLowerCase();
      if (query.isEmpty()) {
        process();
        return;
      }
    }
    command = query;
    parseForCommands();
    proceedToUpgrade();

  }

  private void parseForCommands() {
    StringTokenizer cmdTokenizer = new StringTokenizer(command, " ");
    if (cmdTokenizer.countTokens() == 1 && cmdTokenizer.nextToken().contains("exit")) {
      plog("Upgrader terminated");
      System.exit(-1);
    }
    while (cmdTokenizer.hasMoreTokens()) {
      cmdList.add(cmdTokenizer.nextToken());
    }

    if (cmdList.contains("-r")) {
      query = cmdList.get(0);
      removeCommand = true;
    }
    else if (cmdList.contains("-a")) {
      query = cmdList.get(0);
      splCmd = cmdList.get(1);
      splitCommand = true;
    }
    else if (cmdList.contains("-ru")) {
      query = cmdList.get(0);
      splCmd = cmdList.get(1);
      isRenamedUpgrade = true;
      renamedUpgrade = true;
    }
    else if (cmdList.contains("-as")) {
      query = cmdList.get(0); // sources library
      splCmd = cmdList.get(1);
      sourcesAdd = true;
    }
  }

  private void proceedToUpgrade() {
    // Look for jar files in libraries.dir whose names contain user query.
    List<Path> matchingFiles = walkTreeAndRetrieveFiles(up.getLibrariesDir(), ".jar", query);
    // We will be grouping the matching files based on their filename.
    // Assumption here is all jar files having the same name are identical.
    TreeMap<String, List<Path>> groups = new TreeMap<String, List<Path>>();

    for (Path jarfile : matchingFiles) {
      List<Path> list = groups.get(jarfile.getFileName().toString());
      if (list == null) {
        list = new ArrayList<Path>();
      }
      list.add(jarfile);
      groups.put(jarfile.getFileName().toString(), list);
    }

    int count = 0;

    // Prompt user to select from the options.
    if (groups.size() > 0) {
      plog("Select a group to proceed with upgrade.");
    }
    else {
      // No file is found, we need do another search.
      plog("No files found.");
      query = null;
      process();
      return;
    }

    // Show the groups of file to the screen.
    HashMap<Integer, String> options = new HashMap<Integer, String>();
    for (String filename : groups.keySet()) {
      options.put(count, filename);
      List<Path> list = groups.get(filename);
      String currentFile = list.get(0).toString();
      plog("[" + count + "] " + currentFile + SPACE_SEP + md5(currentFile) + SPACE_SEP
          + getHumanReadableByteCount(currentFile));
      if (list.size() > 1) {
        for (int i = 1; i < list.size(); i++ ) {
          plog(SPACE_SEP + list.get(i).toString() + SPACE_SEP + md5(currentFile) + SPACE_SEP
              + getHumanReadableByteCount(currentFile));
        }
      }
      plog();
      count++ ;
    }

    // Get the user selection.
    String userInput = getUserInputThenLog().trim();

    // If user input is a number and is within options, go locate the replacement
    if (NumberUtils.isNumber(userInput) && Integer.parseInt(userInput) >= 0
        && Integer.parseInt(userInput) <= count) {
      processForNextState(groups, options, userInput);
      up.currentState.process();
    }
    // User wants to search for another library.
    else {
      query = userInput;
      process();
    }
  }

  private void processForNextState(TreeMap<String, List<Path>> groups, HashMap<Integer, String> options,
      String userInput) {
    if (removeCommand) {
      RemoveReferencedLibrary removeState = new RemoveReferencedLibrary(up);
      removeState.oldQuery = query;
      removeState.oldFilename = options.get(new Integer(userInput));
      removeState.oldFiles = groups.get(removeState.oldFilename);
      plog("Selected: " + removeState.oldFilename);
      up.currentState = removeState;
    }
    else if (splitCommand) {
      SplitLibraries splitState = new SplitLibraries(up);
      splitState.oldQuery = query;
      splitState.splittedLibrary = splCmd;
      splitState.oldFilename = options.get(new Integer(userInput));
      splitState.oldFiles = groups.get(splitState.oldFilename);
      plog("Selected: " + splitState.oldFilename);
      up.currentState = splitState;
    }
    else {
      LocateReplacementState newState = new LocateReplacementState(up);
      newState.oldQuery = query;
      newState.oldFilename = options.get(new Integer(userInput));
      newState.oldFiles = groups.get(newState.oldFilename);
      newState.newFileName = splCmd;
      newState.isRenamedUpgrade = isRenamedUpgrade;
      plog("Selected: " + newState.oldFilename);
      up.currentState = newState;
    }
  }
}
