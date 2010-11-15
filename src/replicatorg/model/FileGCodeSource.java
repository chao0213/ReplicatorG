package replicatorg.model;

import replicatorg.app.Base;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class FileGCodeSource implements GCodeSource {
    private ArrayList<String> lines;

    public FileGCodeSource(String path) throws IOException {
	String[] fileContents = Base.loadFile(new File(path)).split("\n");
	lines = new ArrayList<String>(fileContents.length + 1);
	for (int i=0; i<fileContents.length; i++) {
	    lines.add(fileContents[i]);
	}
    }

    public Iterator<String> iterator() {
	return lines.iterator();
    }

    public int getLineCount() {
	return lines.size();
    }
}
    