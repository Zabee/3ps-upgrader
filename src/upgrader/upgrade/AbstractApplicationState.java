package upgrader.upgrade;

import static upgrader.upgrade.Upgrader.log;

import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;

/**
 * Base implementation for the application states. It has the implementation of walk the tree and get the
 * necessary files implementation.
 * @author bsanchin
 */
public abstract class AbstractApplicationState
    implements IState {

  public static final String SPACE_SEP = "    ";

  /**
   * Returns human readable byte count
   * 
   * @param bytes bytes to be converted
   * @return bytes in human readable count
   */
  public static String getHumanReadableByteCount(long bytes) {
    int unit = 1024;
    if (bytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    char pre = "KMGTPE".charAt(exp - 1);
    return String.format("%.1f%sB (%,d)", bytes / Math.pow(unit, exp), pre, bytes);
  }

  /**
   * Returns the size of a file in human readable format
   * 
   * @param argFile file
   * @return size of the file in human readable format
   */
  public static String getHumanReadableByteCount(String argFile) {
    File file = new File(argFile);
    return getHumanReadableByteCount(file.length());
  }

  protected String md5(File argFile)
      throws Exception {

    byte[] buffer = new byte[8192];
    MessageDigest md = MessageDigest.getInstance("md5");

    try (DigestInputStream dis = new DigestInputStream(new FileInputStream(argFile), md)) {

      while (dis.read(buffer) != -1) {
        ;
      }
      byte[] bytes = md.digest();
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < bytes.length; i++ ) {
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
      }

      return sb.toString();
    }
  }

  protected String md5(String argFile) {
    try {
      return md5(new File(argFile));
    }
    catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * Visits given directory recursively and returns set of files that match given criterias.
   * @param rootDir root of the directory
   * @param fileExtension file extension that we would use for the search
   * @param filename portion of the filename we would use for the search
   * @return set of files that match the search criterias
   */
  protected List<Path> walkTreeAndRetrieveFiles(String rootDir, String fileExtension, String filename) {

    List<Path> matchingFiles = new LinkedList<Path>();
    try {
      Files.walkFileTree(Paths.get(rootDir), new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs)
            throws IOException {
          String currentFilename = file.getFileName().toString().toLowerCase();
          if (currentFilename.endsWith(fileExtension) && currentFilename.contains(filename)) {
            matchingFiles.add(file);
          }
          return FileVisitResult.CONTINUE;
        };
      });
    }
    // We are ok with dumping out some nasty exception.
    catch (IOException ex) {
      ex.printStackTrace();
      log(ex.getMessage());
      System.exit(1);
    }

    return matchingFiles;
  }

}
