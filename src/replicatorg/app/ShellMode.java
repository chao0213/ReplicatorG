package replicatorg.app;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class ShellMode {
  private static Options opts = null;
		
  public ShellMode(String cmdargs[]) {
    try {
      CommandLine cmd = new PosixParser().parse(getOptions(), cmdargs);

      if (cmd.hasOption("help")) usage();

      if (cmd.hasOption("skein")) {
        System.out.println("Running model through Skeinforge with profile " + 
                           cmd.getOptionValue("skein"));
      } 

      if (cmd.hasOption("build")) {
        System.out.println("Building your object.");
        String machineName = Base.preferences.get("machine.name", null);
        String serialPort = cmd.hasOption("serial") ? 
                            cmd.getOptionValue("serial") : 
                            Base.preferences.get("serial.last_selected", null);
        build(machineName, serialPort);
      }
    } catch (ParseException e) {
      System.err.println("Unable to parse arguments: " + e.getMessage());
      usage();
    }
  }

  public static void skein(String file, String profile) {
    ;
  }

  public static void usage() {
    new HelpFormatter().printHelp("replicatorg", getOptions());
    System.exit(0);
  }

  public static void build(String machineName, String serialPort) {
    if (machineName == null || serialPort == null) {
      if (machineName == null) {
        System.err.println("Unable to load a machine. You must load a machine\n" +
                           "through the GUI before you can print from the command line.");
      }
      if (serialPort == null) {
        System.err.println("Please specify a serial port to use to connect to the machine.");
      }

      loadMachine(machineName, serialPort);
    }
  }

  public static void connectToMachine(String machineName, String serial) {
    System.out.println("Loading machine " + machineName);
    MachineController machine = Base.loadMachine(machineName);
    
    if (machine.driver instanceof UsesSerial) {
      UsesSerial driver = (UsesSerial) machine.driver;
      try {
        driver.setSerial(new Serial(serial, driver));
        machine.connect();
        System.out.println("Successfully connected to machine.");
      } catch (SerialException e) {
        System.err.println("Unable to connect to machine on serial port " + serial);
        usage();
      }
    } else {
      System.err.println("The command-line interface of replicatorg does not support your machine.");
    }
  }

  private static Options getOptions() {
    if (opts == null) {
      opts = new Options();
      opts.addOption(OptionBuilder
                     .withLongOpt("nogui")
                     .withDescription("Use the command line interface to replicatorg.")
                     .create());
      opts.addOption(OptionBuilder
                     .hasArgs()
                     .withArgName("PROFILE")
                     .withLongOpt("skeinforge")
                     .withDescription("The skeinforge profile to use to slice the STL file.")
                     .create("s"));
      opts.addOption(new Option("b", "build", false, "Build a model."));
      opts.addOption(new Option("h", "help", false, "Show usage summary."));
    }
    return opts;
  }
}
