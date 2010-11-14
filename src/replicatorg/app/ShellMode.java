package replicatorg.app;

import java.util.HashMap;

public class ShellMode {
		HashMap<String> args;
    enum Operations { SKEIN, PRINT };

    public ShellMode(String cmdargs[]) {
				
				args = new HashSet<String>();
				for (int i=0; i<cmdargs.length; i++) args.add(cmdargs[i]);

				if (! validArgs(args)) {
						System.err.println(usage());
						System.exit(-1);
				}
    }

    private boolean validArgs(HashSet<String> args) {
				;
		}				
}
