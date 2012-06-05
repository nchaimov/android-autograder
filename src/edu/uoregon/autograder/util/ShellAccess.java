package edu.uoregon.autograder.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.util.logging.Logger;

public class ShellAccess {

	//private static final Logger log = Logger.getLogger(ShellAccess.class.getName());

	static class StreamGobbler extends Thread {
		InputStream is;

		String type;
		StringBuffer buff = new StringBuffer();

		StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					//System.out.println(type + ">" + line);
					if (buff.length() > 0) buff.append("\n");
					buff.append(line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	public static ShellOutputError execCommandNoBlocking(String command) {
		return execCommand(command, false);
	}
	
	public static ShellOutputError execCommandBlocking(String command) {
		return execCommand(command, true);
	}
	
	private static ShellOutputError execCommand(String command, Boolean waitFor) {
		StringBuffer outputBuff = new StringBuffer();
		StringBuffer errorBuff = new StringBuffer();
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec(command);
			
			// any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc
                            .getErrorStream(), "ERROR");

            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc
                            .getInputStream(), "OUTPUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error???
            Thread.sleep(1000);	// sleep for 1 second; otherwise gobblers don't have enough time to finish
            if (waitFor) { 
            	proc.waitFor();
            }
            outputBuff.append(outputGobbler.buff);
            errorBuff.append(errorGobbler.buff);
            //System.out.println("ExitValue: " + exitVal);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			errorBuff.append(e.getLocalizedMessage());
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			errorBuff.append(e.getLocalizedMessage());
			e.printStackTrace();
		}
		//System.out.println("ShellAccess output: " + outputBuff.toString());
		//System.out.println("ShellAccess error: " + errorBuff.toString());
		ShellOutputError result = new ShellOutputError();
		if (outputBuff.length() > 0) 
			result.setOutput(outputBuff.toString());
		if (errorBuff.length() > 0)
			result.setError(errorBuff.toString());
		return result;
	}

}
