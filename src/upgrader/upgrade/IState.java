package upgrader.upgrade;
/**
 * The upgrader application runs in either of following 4 states at a time:
 * <p>
 * <ol>
 * <li>{@link upgrader.upgrade.SearchLibraryState}
 * <li>{@link upgrader.LocateForReplacementState}
 * <li>{@link upgrader.upgrade.LocateReferencedFilesState}
 * <li>{@link upgrader.upgrade.MakeReplacementState}
 * </ol>
 * <p>
 * All of above states implement IState interface.
 * 
 * @author bsanchin
 */
public interface IState {

  /**
   * Proceeds with state specific implementation.
   */
  public void process();
}
