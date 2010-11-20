package replicatorg.plugin.toolpath;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import replicatorg.app.Base;
import replicatorg.plugin.toolpath.SkeinforgeGenerator.SkeinforgePreference;

public class ToolpathGeneratorFactory {
	public static class ToolpathGeneratorDescriptor {
		public String name;
		public String description;
		public Class<?> tpClass;
		
		public ToolpathGeneratorDescriptor(String name, String description, 
				Class<?> tpClass) {
			this.name = name;
			this.description = description;
			this.tpClass = tpClass;
		}
	
		public ToolpathGenerator instantiate() {
			try {
				return (ToolpathGenerator)tpClass.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	static private Vector<ToolpathGeneratorDescriptor> generatorList = null;
	
	public static Vector<ToolpathGeneratorDescriptor> getGeneratorList() {
		if (generatorList == null) {
			generatorList = buildGeneratorList();
		}
		return generatorList;
	}
	static private Vector<ToolpathGeneratorDescriptor> buildGeneratorList() {
		Vector<ToolpathGeneratorDescriptor> list = new Vector<ToolpathGeneratorDescriptor>();
		class Skeinforge6 extends SkeinforgeGenerator {
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-0006");
			}
			File getUserProfilesDir() {
		    	return Base.getUserFile("sf_profiles");
			}
			public List<SkeinforgePreference> getPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				return prefs;
			}
		};
		class Skeinforge31 extends SkeinforgeGenerator {
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-31/skeinforge_application");
			}
			File getUserProfilesDir() {
		    	return Base.getUserFile("sf_31_profiles");
			}
			public List<SkeinforgePreference> getPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				SkeinforgeBooleanPreference raftPref = 			
					new SkeinforgeBooleanPreference("Use raft",
						"replicatorg.skeinforge.useRaft", true,
						"If this option is checked, skeinforge will lay down a rectangular 'raft' of plastic before starting the build.  "
						+ "Rafts increase the build size slightly, so you should avoid using a raft if your build goes to the edge of the platform.");
				raftPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Activate Raft", "true"));
				prefs.add(raftPref);
				return prefs;
			}
		};
		class Skeinforge35 extends Skeinforge31 {
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-35/skeinforge_application");
			}
			File getUserProfilesDir() {
		    	return Base.getUserFile("sf_35_profiles");
			}
			public List<SkeinforgePreference> getPreferences() {
				List <SkeinforgePreference> prefs = super.getPreferences();
				SkeinforgeBooleanPreference supportPref =
					new SkeinforgeBooleanPreference("Use support material",
							"replicatorg.skeinforge.useSupport", false,
							"If this option is checked, skeinforge will attempt to support large overhangs by laying down a support "+
							"structure that you can later remove.");
				supportPref.addTrueOption(new SkeinforgeOption("raft.csv","Support Material Choice:", "Exterior Only"));
				supportPref.addFalseOption(new SkeinforgeOption("raft.csv","Support Material Choice:", "None"));
				prefs.add(supportPref);
				return prefs;
			}
		};

		list.add(new ToolpathGeneratorDescriptor("Skeinforge (standard)", 
				"This is the standard version of skeinforge that has shipped with "+
				"ReplicatorG since 0016.", Skeinforge6.class));
		list.add(new ToolpathGeneratorDescriptor("Skeinforge (31)", 
				"This is Skeinforge version 31.", Skeinforge31.class));
		list.add(new ToolpathGeneratorDescriptor("Skeinforge (35)", 
				"This is the latest version of skeinforge.", Skeinforge35.class));
		
		return list;
	}

	static public String getSelectedName() {
		String name = Base.preferences.get("replicatorg.generator.name", "Skeinforge (standard)");
		return name;
	}

	static public void setSelectedName(String name) {
		Base.preferences.put("replicatorg.generator.name", name);
	}

	static public ToolpathGenerator createSelectedGenerator() {
		String name = getSelectedName();
		Vector<ToolpathGeneratorDescriptor> list = getGeneratorList();
		ToolpathGenerator tg = null;
		for (ToolpathGeneratorDescriptor tgd : list) {
			if (name.equals(tgd.name)) { tg = tgd.instantiate(); break; }
		}
		return tg;
	}
}
