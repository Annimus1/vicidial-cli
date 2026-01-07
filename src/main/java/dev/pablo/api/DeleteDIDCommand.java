package dev.pablo.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import dev.pablo.models.DidModel;
import dev.pablo.models.HtmlParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * CLI command responsible for deleting DIDs from a Vicidial instance.
 *
 * <p>
 * Supported modes:
 * - SINGLE   : delete a single DID that exactly matches the provided phone number.
 * - MULTIPLE : delete DIDs listed (one per line) in a text file.
 * - GROUP    : planned feature to delete DIDs by user group (not implemented).
 * </p>
 *
 * <p>
 * Workflow:
 * 1) Fetch DID table HTML from Vicidial admin page using VicidialClientSingleton.
 * 2) Parse HTML into DidModel objects using HtmlParser.ParseDIDs.
 * 3) Remove matched DIDs according to the selected mode (simulated removal).
 * </p>
 *
 * Examples:
 *   vicidial-cli deleteDIDs --did 15551234567
 *   vicidial-cli deleteDIDs -m MULTIPLE -l /path/to/dids.txt
 *   vicidial-cli deleteDIDs -m GROUP  -g SALES_TEAM
 */
@Command(name = "deleteDIDs", description = {
    "Delete DIDs from a Vicidial instance. Modes:",
    "  SINGLE   - remove one DID that exactly matches the provided phone number.",
    "  MULTIPLE - remove DIDs listed (one per line) in a text file.",
    "  GROUP    - (planned) remove all DIDs for a given user group.",
    "",
    "Examples:",
    "  vicidial-cli deleteDIDs --did 15551234567",
    "  vicidial-cli deleteDIDs -m MULTIPLE -l /path/to/dids.txt",
    "  vicidial-cli deleteDIDs -m GROUP --group SALES_TEAM",
    " "
}, mixinStandardHelpOptions = true)
public class DeleteDIDCommand implements Callable<Integer> {

  /**
   * Modes supported by the command.
   */
  public enum MODE {
    SINGLE,
    MULTIPLE,
    GROUP
  }

  private final int DEFAULT_DID_ID = 1;

  /**
   * Mode selector. Required option.
   *
   * Example: -m SINGLE
   */
  @Option(names = { "-m",
      "--mode" }, required = true, description = "Operation mode: 'SINGLE', 'MULTIPLE' or 'GROUP'.\n"
          + "SINGLE  - remove the DID that matches the provided --did value.\n"
          + "MULTIPLE- remove DIDs listed in the file passed with --list.\n"
          + "GROUP   - planned: remove DIDs for a specific user group.", defaultValue = "SINGLE")
  private MODE mode;

  /**
   * Group name used by GROUP mode (currently unused).
   */
  @Option(names = { "-g",
      "--group" }, description = "Group name used by GROUP mode. Ignored for SINGLE/MULTIPLE. (optional)", defaultValue = "")
  private String group;

  /**
   * Single DID value used with SINGLE mode.
   *
   * Mandatory when mode == SINGLE.
   */
  @Option(names = {
      "--did" }, description = "Phone number to remove when using SINGLE mode. Format: start with '1', 11 digits (e.g. 15551234567).", defaultValue = "")
  private String did;

  /**
   * Path to newline-separated file used with MULTIPLE mode.
   *
   * Mandatory when mode == MULTIPLE.
   */
  @Option(names = { "-l",
      "--list" }, description = "Path to a newline-separated text file containing DIDs to remove (one DID per line). Used with MULTIPLE mode.", defaultValue = "")
  private String list;

  private List<DidModel> dids = new ArrayList<>();

  private final VicidialClientSingleton client;

  public DeleteDIDCommand() {
    this.client = VicidialClientSingleton.getInstance();
  }

  /**
   * Entry point executed by picocli.
   *
   * Steps:
   * 1. Fetch DIDs HTML via VicidialClientSingleton#GetDIDs
   * 2. Parse DIDs into DidModel list using HtmlParser
   * 3. Execute removal flow depending on selected mode
   *
   * Returns exit code (0 success, 1 on error).
   */
  @Override
  public Integer call() {
    try {
      String html = client.GetDIDs("https://cloud.yourserviceva.net/vicidial/admin.php?ADD=1300");
      dids = HtmlParser.ParseDIDs(html);
      System.out.println(Ansi.AUTO.text("@|blue Total of #️⃣ " + dids.size() + " DIDs Found.|@"));

      // Check mode
      if (mode.equals(MODE.SINGLE)) {
        if (!did.isBlank()) {
          removeSingleDID();
        } else {
          System.err.println(Ansi.AUTO.text("❌ @|red Caller id not provided. |@"));
        }
      }

      if (mode.equals(MODE.MULTIPLE)) {
        if (!list.isBlank()) {
          removeMultipleDID();
        } else {
          System.err.println(Ansi.AUTO.text("❌ @|red List path is missing. |@"));
        }
      }

      // GROUP mode: planned implementation.
      if (mode.equals(MODE.GROUP)) {
        if (!group.isBlank()) {
          removeGroupDID();
        } else {
          System.err.println(Ansi.AUTO.text("❌ @|red Group name is missing. |@"));
        }
      }

    } catch (IOException e) {
      System.out.println(Ansi.AUTO.text("❌ @|red API or Reading Error:|@ " + e.getMessage()));
      return 1;
    } catch (InterruptedException e) {
      System.out.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
      return 1;
    }
    return 0;
  }

  private void removeGroupDID() {
    List<DidModel> didsMatch = didMatchGroup(group);

    for (DidModel d : didsMatch) {
      removeDid(d);
    }

  }

  /**
   * Read the provided file and remove each DID present in it.
   *
   * @throws IOException if reading the file fails.
   */
  private void removeMultipleDID() throws IOException {
    Set<String> didsProvided = readFile(list);

    for (String s : didsProvided) {
      try {
        checkDidFormat(s);
        DidModel didModel = didMatch(s);
        if (didModel == null) {
          System.out.println(Ansi.AUTO.text("❌ @|red Phone not found in Vicidial: " + s + "|@ "));
          continue;
        }
        removeDid(didModel);
      } catch (IllegalArgumentException e) {
        System.out.println(Ansi.AUTO.text("❌ @|red Invalid Phone number: " + s + "|@ "));
      }
    }
  }

  /**
   * Reads a newline-separated file and returns a set of DID strings.
   *
   * @param listPath file path to read
   * @return set of DIDs (unique)
   * @throws IOException              when file access fails
   * @throws IllegalArgumentException when path is invalid
   */
  private Set<String> readFile(String listPath) throws IOException {
    Path path = Paths.get(listPath);
    Set<String> dids = new HashSet<>();

    // check if file exists
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      throw new IllegalArgumentException("Invalid path: " + path.toAbsolutePath());
    }

    List<String> lineas = Files.readAllLines(path);
    lineas.forEach(l -> dids.add(l.trim()));

    return dids;
  }

  /**
   * Remove a single DID after validating its format.
   *
   * Throws IllegalArgumentException when DID format is invalid (caught by caller).
   */
  private void removeSingleDID() {
    // check if phone number is valid
    checkDidFormat(did);

    DidModel didModel = didMatch(did);

    if (didModel == null) {
      System.out.println(Ansi.AUTO.text("❌ @|red Phone not found in Vicidial: " + did + "|@ "));
      return;
    }

    removeDid(didModel);
  }

  /**
   * Find a DidModel matching the provided caller id.
   *
   * @param s caller id to match
   * @return matched DidModel or null if not found
   */
  private DidModel didMatch(String s) {
    DidModel didModel = null;
    Optional<DidModel> didMatch = dids.stream().filter(d -> d.getCallerId().equals(s)).findFirst();

    if (didMatch.isPresent()) {
      didModel = didMatch.get();
    }

    return didModel;
  }

  private List<DidModel> didMatchGroup(String s) {
    List<DidModel> didsMatch = dids.stream().filter(d -> d.getGroup().equals(s)).toList();

    System.out
        .println(Ansi.AUTO.text("@|blue Total of #️⃣ " + didsMatch.size() + " dids Found from " + s + " group.|@"));
    return didsMatch;
  }

  /**
   * Performs safety checks and prints removal result.
   *
   * - ignores null matches
   * - protects the default DID id (DEFAULT_DID_ID)
   *
   * @param did DidModel to remove (simulated)
   */
  private void removeDid(DidModel did) {
    if (did == null) {
      System.out.println(Ansi.AUTO.text("@|red Can't remove DID from null. |@"));
      return;
    }

    if (did.getId() == DEFAULT_DID_ID) {
      System.out.println(Ansi.AUTO.text("@|red Can't remove default DID. |@"));
      return;
    }

    System.out.println(
        Ansi.AUTO.text("@|green ID: " + did.getId() + " DID: " + did.getCallerId() + " removed successfully. |@"));

  }

  /**
   * Validates the DID format expected by the system.
   *
   * Rules:
   * - Not blank
   * - Starts with '1'
   * - Length is 11 digits
   *
   * @param did phone number string to validate
   * @throws IllegalArgumentException if the DID is invalid
   */
  private void checkDidFormat(String did) throws IllegalArgumentException {
    if (did == null || did.isBlank() || !did.startsWith("1") || did.length() != 11) {
      throw new IllegalArgumentException("Invalid Phone Number.");
    }
    return;
  }
}
