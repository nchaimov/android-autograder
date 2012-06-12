package edu.uoregon.autograder.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is used statically to execute a command at the shell and capture the stdout and stderr from that
 * command execution. This is used by GraderTask implementations that need to execute Android shell commands
 * (like adb or apktool).
 *
 * @author Kurt Mueller
 */
public class ShellAccess {

	/**
	 * @author Michael C. Daconta
	 * 
	 * copied from here: http://www.javaworld.com/jw-12-2000/jw-1229-traps.html?page=4
	 * with modifications to keep line breaks intact
	 */
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

	/**
	 * Use this to execute a shell command without waiting for it to return a value.
	 * Used, for example, in the StartEmulatorTask, because the shell command to start it up
	 * does not return until the emulator is stopped, so we would be waiting forever if blocked.
	 * Instead, we immediately start a second task (WaitForEmulatorTask) that blocks until the
	 * emulator is ready.
	 * 
	 * @param command the command to execute
	 * @return the output and error object
	 */
	public static ShellOutputError execCommandNoBlocking(String command) {
		return execCommand(command, false);
	}
	
	/**
	 * Same as above, but do block (wait for the command to return). Used in most other tasks.
	 * 
	 * @param command the command to execute
	 * @return the output and error object
	 */
	public static ShellOutputError execCommandBlocking(String command) {
		return execCommand(command, true);
	}
	
	/**
	 * Exec the shell command, blocking or not as directed in waitFor. Capture stdout and
	 * stderr in two StreamGobblers, then return them in ShellOutputError object.
	 * 
	 * @param command the command to execute
	 * @param waitFor block if true, don't block if false
	 * @return the ShellOutputError object containing the stdout and stderr for the execution
	 */
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
		ShellOutputError result = new ShellOutputError();
		if (outputBuff.length() > 0) 
			result.setOutput(outputBuff.toString());
		if (errorBuff.length() > 0)
			result.setError(errorBuff.toString());
		return result;
	}

}
