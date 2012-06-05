package edu.uoregon.autograder.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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
	
	public String getFirstFilePath() {
		if (getFiles() != null && getFiles().keySet() != null && !getFiles().keySet().isEmpty())
			return getFiles().get(getFiles().keySet().iterator().next());
		else return null;
	}
	
	// return an array of file paths for input files ending in suffix (like .apk)
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
