package edu.uoregon.autograder.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class holds lists of input strings and input files for use by a Grader and its GraderTasks.
 * Input strings are set by the init parameters of the servlet (in web.xml) as well as by form fields
 * on the web page used to submit a new Grader job or jobs. Input file entries are created by the 
 * servlet when it processes multi-part form data file uploads. Robotium tester files stored on the
 * server are also added to the files list, as they form part of the input data.
 * 
 * These are Maps from Strings to Strings in both cases. An example of a string pair is
 * 
 * TMP_DIR : /tmp
 * 
 * An example of a file pair is
 * 
 * RobotiumTest.apk : /tmp/robotium/RobotiumTest.apk
 *
 * @author Kurt Mueller
 */
public class GraderData {

	protected Map<String, String> strings;
	protected Map<String, String> files;
	
	public GraderData() {
		
	}
	
	public void addString(String key, String value) {
		if (strings == null)
			strings = new LinkedHashMap<String, String>();
		strings.put(key, value);
	}
	
	public Map<String, String> getStrings() {
		return strings;
	}
	
	public String getString(String key) {
		if (strings != null && strings.containsKey(key))
			return strings.get(key);
		else return null;
	}
	
	public void addFile(String fileName, String fullPathToFile) {
		if (files == null)
			files = new LinkedHashMap<String, String>();
		files.put(fileName, fullPathToFile);
	}
	
	public Map<String, String> getFiles() {
		return files;
	}
	
	/**
	 * @return a String representing the path to the first file in the input files list, if such a file exists.
	 */
	public String getFirstFilePath() {
		if (getFiles() != null && getFiles().keySet() != null && !getFiles().keySet().isEmpty())
			return getFiles().get(getFiles().keySet().iterator().next());
		else return null;
	}
	
	/**
	 * @param suffix the filename suffix to search for in the input files
	 * 
	 * @return an array of file paths for input files ending in a particular suffix (like .apk)
	 */
	public ArrayList<String> getFilePathsByFileSuffix(String suffix) {
		ArrayList<String> result = new ArrayList<String>();
		if (files != null && !files.isEmpty()) {
			Iterator<String> it = getFiles().keySet().iterator();
			while (it.hasNext()) {
				String filePath = getFiles().get(it.next());
				if (filePath.endsWith(suffix))
					result.add(filePath);
			}
		}
		return result;
	}
	
	
	/**
	 * @return the name of the first file in the input files list, if there is a file
	 */
	public String getFirstFileName() {
		if (getFiles() != null && getFiles().keySet() != null && !getFiles().keySet().isEmpty())
			return getFiles().keySet().iterator().next();
		else return null;
	}
	
	public String toString() {
		StringBuffer buff = new StringBuffer();
		Iterator<String> it;
		String name;
		buff.append("Strings:\n");
		if (strings != null && !strings.isEmpty()) {
			it = strings.keySet().iterator();
			while (it.hasNext()) {
				name = it.next();
				buff.append(name + ": " + strings.get(name) + "\n");
			}
			buff.append("\n");
		} else {
			buff.append("<none>\n\n");
		}
		buff.append("Files:\n");
		if (files != null && !files.isEmpty()) {
			it = files.keySet().iterator();
			while (it.hasNext()) {
				name = it.next();
				buff.append(name + ": " + files.get(name) + "\n");
			}
		} else {
			buff.append("<none>\n");
		}
		return buff.toString();
	}

}
