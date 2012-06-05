package edu.uoregon.autograder.android.task;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import edu.uoregon.autograder.appinventor.task.AIConstants;
import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;

public class AddLinesToAndroidManifestTask extends GraderTask{
	
	public static final String APKTOOL_INIT_PARAM_NAME = "APKTOOL_PATH";	
	public static final String TASK_NAME = "PreprocessAppAndTesterAPKsTask";
	public static final String APKTOOL_DECODE_COMMAND = " decode -s -f ";
	public static final String APKTOOL_BUILD_COMMAND = " build ";
	public static final String TMP_PATH = "TMP_DIR_PATH";
	public static final String KEYSTORE_PATH = "DEBUG_KEYSTORE_PATH";
	public static final String TESTER_APK = "robotiumfile";	// from index.jsp form
	public static final String ROBOTIUM_DIR = "ROBOTIUM_DIR";
	
	public static final String NO_APK_ERROR = "Must upload an APK file!";
	
	public static final String DIR_EXTENSION = ".tmp";	// RobotiumTest.apk will be exploded into RobotiumTest.apk.tmp dir
	
	public AddLinesToAndroidManifestTask(Grader grader) {
		super(TASK_NAME, grader);
	}
	
	public void doTask() {
		logStarted();
		String tmpPath = getInput().getString(TMP_PATH);	// /tmp/autograder
		String graderBaseDir = tmpPath + File.separator + getGraderId(); // /tmp/autograder/<graderId>
		
		String testerFile = getInput().getString(TESTER_APK); // RobotiumTest.apk
		String testerSourceFile = getInput().getString(ROBOTIUM_DIR) + File.separator + testerFile;	// /tmp/auotgrader/robotium/RobotiumTest.apk
		String testerOutputFile = graderBaseDir + File.separator + testerFile;	// /tmp/<graderId>/RobotiumTest.apk
		String testerDir = graderBaseDir + File.separator + testerFile + DIR_EXTENSION; // /tmp/<graderId>/RobotiumTest.apk.tmp
		
		String testedFileFullPath = getInput().getFirstFilePath();		// /tmp/<graderId>/<app_inventor_filename>.apk
		String testedDir = testedFileFullPath + DIR_EXTENSION; // /tmp/<graderId>/<app_inventor_filename>.tmp
		
		if (testedFileFullPath != null) {
			// Decode APK
			String command = getInput().getString(APKTOOL_INIT_PARAM_NAME) + APKTOOL_DECODE_COMMAND + 
					testedFileFullPath + " " + testedDir;
	
			ShellOutputError result = ShellAccess.execCommandBlocking(command);
			//getOutput().setOutputAndError(getTaskName(), result);
			log("Decoding app APK", result);
			
			/*
			 * Adds <uses-sdk android:targetSdkVersion="YOUR_VERSION" /> 
			 * to the APK we are testing.
			 */
			try {
				
				// Adds the proper line to the app being tested
				String packageName = addLineToTestedManifest(testedDir + "/AndroidManifest.xml");
				
				// Add tested apk package name to inputs; needed by uninstall task later in workflow
				getInput().addString(AIConstants.TESTED_APK_PACKAGE_KEY, packageName);
				
				// Rebuilds the apk in the grader's tmp dir - /tmp/<graderId>/RobotiumTest/ for instance
				command = getInput().getString(APKTOOL_INIT_PARAM_NAME) + APKTOOL_BUILD_COMMAND + 
						testedDir + " " + testedFileFullPath;
				result = ShellAccess.execCommandBlocking(command);
				//getOutput().setOutputAndError(getTaskName(), result);
				log("Rebuilding app APK", result);
				
				signAPK(testedFileFullPath);
				
				// Pulls apart Testing APK
				command = getInput().getString(APKTOOL_INIT_PARAM_NAME) + APKTOOL_DECODE_COMMAND + 
						testerSourceFile + " " + testerDir;
		
				result = ShellAccess.execCommandBlocking(command);
				//getOutput().setOutputAndError(getTaskName(), result);
				log("Decoding testing APK", result);
				
				String robotiumPackageName = addLineToTestingManifest(testerDir + "/AndroidManifest.xml", packageName);
				// Add robotium package name to inputs; needed by uninstall task later in workflow
				getInput().addString(AIConstants.ROBOTIUM_APK_PACKAGE_KEY, robotiumPackageName);
				
				// Rebuilds the apk in the grader's tmp dir - /tmp/<graderId>/RobotiumTest/ for instance
				command = getInput().getString(APKTOOL_INIT_PARAM_NAME) + APKTOOL_BUILD_COMMAND + 
						testerDir + " " + testerOutputFile;

				result = ShellAccess.execCommandBlocking(command);
				//getOutput().setOutputAndError(getTaskName(), result);
				log("Rebuilding testing APK", result);
				
				signAPK(testerOutputFile);
				
				// add newly-created tester APK to input files, for later consumption by APK installer
				getInput().addFile(testerFile, testerOutputFile);
				
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerFactoryConfigurationError e) {
				e.printStackTrace();
			}

			//getOutput().addOutputString(getTaskName(), "Updated AndroidManifest.xml");
			logFinished();
			
		} else {
			//getOutput().addErrorString(getTaskName(), NO_APK_ERROR);
			logError("Error locating app APK; ensure that paths in web.xml are correct");
		}
		
	}
	
	private void signAPK(String apkFile) { 
		String command = "jarsigner -keypass android -storepass android -keystore " + 
				getInput().getString(KEYSTORE_PATH) + " " + apkFile + " androiddebugkey";
		
		ShellOutputError result = ShellAccess.execCommandBlocking(command);
		//getOutput().setOutputAndError(getTaskName(), result);
		log("Signing app APK", result);
	}

	private String addLineToTestedManifest(String dirName) 
			throws ParserConfigurationException, SAXException, 
			IOException, TransformerException  {
		File xmlFile = new File(dirName);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	
			
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);
		Node manifest = doc.getElementsByTagName("manifest").item(0);
		
		String testedPackageName = manifest.getAttributes().getNamedItem("package").getNodeValue();
	
		Node usesSDKNode = ((Element)manifest).getElementsByTagName("uses-sdk").item(0);
		String SDKStr = usesSDKNode.getAttributes().getNamedItem("android:minSdkVersion").getNodeValue();
		
		Element targetSDKElement = doc.createElement("uses-sdk");
		targetSDKElement.setAttribute("android:targetSdkVersion",SDKStr);
		manifest.appendChild(targetSDKElement);
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult streamResult = new StreamResult(new File(dirName));
		transformer.transform(source, streamResult);
	
		return testedPackageName;
		
	}

	private String addLineToTestingManifest(String dirName, String packageName) 
			throws ParserConfigurationException, SAXException, 
			IOException, TransformerException  {
		File xmlFile = new File(dirName);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	
			
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);
		Node manifest = doc.getElementsByTagName("manifest").item(0);
			
		String testingPackageName = manifest.getAttributes().getNamedItem("package").getNodeValue();

		// Adding <supports-screens android:anyDensity="true"/> 
		Element supportsScreensElement = doc.createElement("supports-screens");
		supportsScreensElement.setAttribute("android:anyDensity","true");
		manifest.appendChild(supportsScreensElement);

		Node usesSDKNode = ((Element)manifest).getElementsByTagName("instrumentation").item(0);
		usesSDKNode.getAttributes().getNamedItem("android:targetPackage").setNodeValue(packageName);
		

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult streamResult = new StreamResult(new File(dirName));
		transformer.transform(source, streamResult);
		
		return testingPackageName;
	}
}
