package replicatorg.app;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import replicatorg.app.exceptions.SerialException;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.UsesSerial;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;
import replicatorg.model.BuildModel;
import replicatorg.model.FileGCodeSource;
import replicatorg.model.GCodeSource;
import replicatorg.model.StringListSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Level;

/**
 *  A class to interact with the core functions of ReplicatorG from
 *  the command line.
 *
 *  @author William Brown (me@haldean.org)
 */
public class ShellMode {
  private static Options opts = null;
  private static CommandLine cmd;
		
  public ShellMode(String cmdargs[]) {
    /* Set this to only show severe errors, because otherwise
     * important messages get lost in the clutter. */
    Base.logger.setLevel(Level.SEVERE);

    /* Parse command line arguments and delegate to functions. */
    try {
      cmd = new PosixParser().parse(getOptions(), cmdargs);

      if (cmd.hasOption("help")) usage();

      if (cmd.hasOption("build")) {
        System.out.println("Building your object.");
        String machineName = Base.preferences.get("machine.name", null);
        String serialPort = cmd.hasOption("serial") ? 
                            cmd.getOptionValue("serial") : 
                            Base.preferences.get("serial.last_selected", null);
        build(machineName, serialPort);
      }

      System.exit(0);
    } catch (ParseException e) {
      System.err.println("Unable to parse arguments: " + e.getMessage());
      usage();
    }
  }

  /**
   *  Print out a usage summary and exit.
   */
  public static void usage() {
    new HelpFormatter().printHelp("replicatorg", getOptions());
    System.exit(0);
  }

  /**
   *  Build from a file.
   *
   *  @param machineName The name of the machine, e.g., 'Cupcake CNC'
   *  @param serialPort The serial port that the machine is connected on.
   */
  public void build(String machineName, String serialPort) {
    if (machineName == null || serialPort == null) {
      if (machineName == null) {
        System.err.println("Unable to load a machine. You must load a machine\n" +
                           "through the GUI before you can print from the command line.");
      }
      if (serialPort == null) {
        System.err.println("Please specify a serial port to use to connect to the machine.");
      }
    }

    MachineController mc = connectToMachine(machineName, serialPort);
    mc.reset();

    try {
      mc.getDriver().disableDrives();
      waitForKeypress();
    } catch (RetryException e) {
      System.err.println("Unable to disengage drives. Hit enter to continue, or Ctrl-C to exit.");
      waitForKeypress();
    }

    int repeat = 1;
    if (cmd.hasOption("repeat")) repeat = new Integer(cmd.getOptionValue("repeat"));

    mc.addMachineStateListener(new EchoProgressMachineListener());
    for (int i=0; i<repeat; i++) {
      mc.setCodeSource(getCodeSource());
      mc.execute();
    }

    while (mc.getMachineState().isBuilding()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        ;
      }
    }
  }

  /**
   *  Get the GCode source for this build.
   */
  private GCodeSource getCodeSource() {
    if (cmd.hasOption("file")) {
      String path = cmd.getOptionValue("file");
      if (path.endsWith(".gcode")) {
        try {
          return new FileGCodeSource(path);
        } catch (IOException e) {
          System.err.println("Unable to open " + path + ": " + e.getMessage());
          usage();
        }
      } else {
        System.err.println("You can only print GCode files for now.");
        usage();
      }
    } else {
      System.err.println("You must select a file to build.");
      usage();
    }
    return null;
  }

  /**
   *  Connect to a machine.
   *
   *  @param machineName The name of the machine to connect to.
   *  @param serial The serial port to connect on.
   *  @return A {@link MachineController} to control the connected machine.
   */
  public static MachineController connectToMachine(String machineName, String serial) {
    System.out.println("Loading machine " + machineName);
    MachineController machine = Base.loadMachine(machineName);
    
    if (machine.driver instanceof UsesSerial) {
      UsesSerial driver = (UsesSerial) machine.driver;
      try {
        driver.setSerial(new Serial(serial, driver));
        machine.connect();

        while (! machine.getMachineState().isReady()) {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
            ;
          }
        }
        System.out.println("Successfully connected to machine.");
      } catch (SerialException e) {
        System.err.println("Unable to connect to machine on serial port " + serial);
        usage();
      }
    } else {
      System.err.println("The command-line interface of replicatorg does not support your machine.");
      System.exit(-1);
    }

    return machine;
  }

  /**
   *  Defines the command line options available in shell mode.
   */
  private static Options getOptions() {
    if (opts == null) {
      opts = new Options();
      opts.addOption(OptionBuilder
                     .withLongOpt("nogui")
                     .withDescription("Use the command line interface to replicatorg.")
                     .create());
      /* I haven't quite figured out the skeinforge implementation yet.
      opts.addOption(OptionBuilder
                     .hasArgs()
                     .withArgName("PROFILE")
                     .withLongOpt("skeinforge")
                     .withDescription("The skeinforge profile to use to slice the STL file.")
                     .create("s"));
      */
      opts.addOption(OptionBuilder
                     .hasArgs()
                     .withArgName("FILE")
                     .withLongOpt("file")
                     .withDescription("The file to operate on.")
                     .create("f"));
      opts.addOption(OptionBuilder
                     .hasArgs()
                     .withArgName("TIMES")
                     .withLongOpt("repeat")
                     .withDescription("The number of times to print the object. " +
                                      "Good for automated build platform users.")
                     .create("r"));
      opts.addOption(new Option("b", "build", false, "Build a model."));
      opts.addOption(new Option("h", "help", false, "Show usage summary."));
    }
    return opts;
  }

  private void waitForKeypress() {
    try {
      System.out.println("Center the tool nozzle, then press Enter.");
      /* Wait for enter. */
      new BufferedReader(new InputStreamReader(System.in)).readLine();
    } catch (IOException e) {
      ;
    }
  }

  private void clearTerm() {
    System.out.print("\033[2J");
    System.out.flush();
  }

  private class EchoProgressMachineListener implements MachineListener {
    public void machineStateChanged(MachineStateChangeEvent event) {
      ;
    }

    public void machineProgress(MachineProgressEvent event) {
      clearTerm();
      System.out.println(event.toString());
    }

    public void toolStatusChanged(MachineToolStatusEvent event) {
      ;
    }
  }
}
