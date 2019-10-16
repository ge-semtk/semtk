package comge.research.semtk.standaloneExecutables.util;

import java.util.ArrayList;
import java.util.HashMap;

public class ArgParser {
	
	String [] requiredFlagsWithParams = new String [] {};   // e.g. -p param
	String [] requiredFlagsParamNames = new String [] {};
	String [] requiredArgs =            new String [] {};              // order-based arguments
	String [] optionalFlags =           new String [] {};             // optional flags like -f
	String [] optionalFlagsWithParams = new String [] {};    // optional -f param
	String [] optionalFlagsParamNames = new String [] {};
	String [] optionalArgs =            new String [] {};              // optional order-based args at the end
	ArrayList<String> argList = null;
	HashMap<String,String> hash = null;
	String programName = "";
	
	public ArgParser(
			String[] requiredFlagsWithParams, String[] requiredFlagsParamNames, 
			String[] requiredArgs,
			String[] optionalFlags, 
			String[] optionalFlagsWithParams, String[] optionalFlagsParamNames,
			String[] optionalArgs) 
					throws Exception {
		super();
		if (requiredFlagsWithParams != null )
			this.requiredFlagsWithParams = requiredFlagsWithParams;
		if (requiredFlagsParamNames != null )
			this.requiredFlagsParamNames = requiredFlagsParamNames;
		if (requiredArgs != null )
			this.requiredArgs = requiredArgs;
		if (optionalFlags != null )
			this.optionalFlags = optionalFlags;
		if (optionalFlagsWithParams != null )
			this.optionalFlagsWithParams = optionalFlagsWithParams;
		if (optionalFlagsParamNames != null )
			this.optionalFlagsParamNames = optionalFlagsParamNames;
		if (optionalArgs != null )
			this.optionalArgs = optionalArgs;
		
		if (this.requiredFlagsParamNames.length < this.requiredFlagsWithParams.length) 
			throw new Exception("Internal error: all 'required flags with params' have not been assigned names");
		
		if (this.optionalFlagsParamNames.length < this.optionalFlagsWithParams.length) 
			throw new Exception("Internal error: all 'optional flags with params' have not been assigned names");
	}

	
	/**
	 * For missing optional : null
	 * For arg : value
	 * For flag with no param: flag
	 * For flag with param: param
	 * 
	 * @param argName
	 * @return
	 */
	public String get(String argName) {
		return hash.get(argName);
	}

	/**
	 * Call this before checkUsage or retrieving args
	 * @param args
	 */
	public void parse(String programName, String [] args) throws Exception {
		ArrayList<String> remainingArgs = new ArrayList<String>();
		this.argList = new ArrayList<String>();
		this.hash = new HashMap<String,String>();
		
		this.programName = programName;
		for (String a : args) {
			this.argList.add(a);
			remainingArgs.add(a);
		}


		// required flags with params
		for (String p : this.requiredFlagsWithParams) {
			int i = remainingArgs.indexOf(p);
			// false if missing or no param or param is another flag
			if (i < 0 || i == remainingArgs.size() - 1 || remainingArgs.get(i + 1).startsWith("-")) {
				throw new Exception(this.getUsageString(programName));
			} else {
				hash.put(p, remainingArgs.get(i+1));
				// remove flag and param from remainingArgs
				remainingArgs.remove(i);
				remainingArgs.remove(i);
			}
		}

		// required Args
		for (int i=0; i < this.requiredArgs.length; i++) {

			// find and remove first non-flag
			boolean found = false;
			for (int j=0; j < remainingArgs.size(); j++) {
				if (! remainingArgs.get(j).startsWith("-")) {
					hash.put(this.requiredArgs[i], remainingArgs.get(j));
					remainingArgs.remove(j);
					found = true;
					break;
				}
			}
			if (! found) {
				throw new Exception(this.getUsageString(programName));
			}
		}

		// optional flags
		for (String p : this.optionalFlags) {

			// find and remove 
			int i = remainingArgs.indexOf(p);
			if (i > -1) {
				hash.put(p, remainingArgs.get(i));
				remainingArgs.remove(i);
			}
		}

		// optional flags with params
		for (String p : this.optionalFlagsWithParams) {

			// find and remove 
			int i = remainingArgs.indexOf(p);
			if (i > -1) {
				// error if there is no param or param is another flag
				if (i == remainingArgs.size() -1 && remainingArgs.get(i+1).startsWith("-")) {
					throw new Exception(this.getUsageString(programName));
				} else {
					hash.put(p, remainingArgs.get(i+1));
					remainingArgs.remove(i);
					remainingArgs.remove(i);
				}
			}
		}

		// optional flags
		for (String p : this.optionalFlags) {

			// find and remove 
			int i = remainingArgs.indexOf(p);
			if (i > -1) {
				hash.put(p, remainingArgs.get(1));
				remainingArgs.remove(i);
			}
		}

		// left-over args
		if (remainingArgs.size() > this.optionalArgs.length) {
			throw new Exception(this.getUsageString(programName));
		} else {
			for (int i=0; i < remainingArgs.size(); i++) {
				hash.put(this.optionalArgs[i], remainingArgs.get(i));
			}
		}
	
	}
	
	public void throwUsageException(String message) throws Exception {
		throw new Exception(getUsageString(this.programName) + "\n         " + message);
	}
	private String getUsageString(String programName) {
		StringBuilder usage = new StringBuilder();
		usage.append(programName + ":");
		
		for (int i=0; i < this.requiredFlagsWithParams.length; i++) {
			usage.append(" -" + this.requiredFlagsWithParams[i] + " " + this.requiredFlagsParamNames[i]);
		}
		
		for (int i=0; i < this.optionalFlags.length; i++) {
			usage.append(" [-" + this.optionalFlags[i] + "]");
		}
		
		for (int i=0; i < this.optionalFlagsWithParams.length; i++) {
			usage.append(" [-" + this.optionalFlagsWithParams[i] + " " + this.optionalFlagsParamNames[i] + "]");
		}
		
		for (int i=0; i < this.requiredArgs.length; i++) {
			usage.append(" " + this.requiredArgs[i]);
		}
	
		for (int i=0; i < this.optionalArgs.length; i++) {
			usage.append(" [");
		}
		for (int i=0; i < this.optionalArgs.length; i++) {
			usage.append(" " + this.optionalArgs[i] + "]");
		}
		
		return usage.toString();
	}
}
