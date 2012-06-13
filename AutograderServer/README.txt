AutograderServer Installation and Configuration Notes
-----------------------------------------------------
Kurt Mueller
6/13/12

The AutograderServer component is a Tomcat web application that serves
as a front-end to the Android emulator, and allows submission of
App Inventor (AI) APKs for testing in the emulator with Robotium test APKs.
There are two interfaces to the web application, each with a corresponding
servlet.

The AutograderServlet interface, at
http://localhost:8080/autograder/autograderservlet by default, is intended
for single AI APK submission and testing. The user can select an AI APK
from their local filesystem and select an appropriate Robotium test APK from
the server's repository, then have the web application test the AI APK with the
Robotium APK and provide results. The server can only run a single emulator at
a time, so submitted test requests are queued if the emulator is busy.

The MultigraderServlet interface, at
http://localhost:8080/autograder/MultigraderServlet by default, is for grading
of multiple AI APKs at once, against the same Robotium APK. The user can select
multiple AI APKs from their local filesystem and select an appropriate Robotium
test APK from the server's repository, then have the web application test the
submitted AI APKs with the Robotium APK and provide results. In this case, the
server will queue up all submitted AI APKs and run then consecutively in the
single emulator spawned on the server.

Prerequisites
-------------

1. The Android platform tools must be installed on the server and available at
the command prompt. These tools include "android", "adb", and "apktool".

2. An Android Virtual Device (AVD) must be setup on the server, using the "android"
executable. The AVD should be configured to use an appropriate Android OS version.
We have tested this software with 2.3.3. The default AVD name used by this software
is "robotium", though you may call it something else and change the name in the 
web app's web.xml file (see configuration, below).

3. Apache Tomcat version 7.x must be installed on the server, and started before
deploying this web app.

4. Apache ant must be installed to run the ant build script provided.

Installation
------------

You should edit build.properties to match the configuration of your Tomcat
server. Specifically, your Tomcat server should have a user with manager-gui,
manager-script, and manager-jmx roles configured in its tomcat-users.xml file,
and this user and its password should be in build.properties. You may also need
to edit the tomcat server information in build.xml if it is not running locally
and on the default port 8080.

If you have the source distribution, containing this README.txt file, you may
build and install the software by running

$ ant deploy

at the command prompt from the root of the distribution (in the directory
containing build.xml and build.properties). This will compile, package, and 
deploy the web application to the running Tomcat instance.

Configuration
-------------

The web application is configured in the web.xml file, in /WEB-INF. The items to
configure for each servlet are:

TMP_DIR_PATH - this directory will be used for storing temporary files created 
during processing of the AI and Robotium APKs. It must be writable by the Tomcat
process.

ADB_PATH - this specifies the location of the "adb" executable file.

EMULATOR_PATH - this specifies the location of the "emulator" executable file.

DEFAULT_AVD_NAME - this specifies the name of the default avd to use.

APKTOOL_PATH - this specifies the location of the "apktool" executable file.

DEBUG_KEYSTORE_PATH - this specifies the location of the Android debug keystore.

ROBOTIUM_DIR - this specifies the location of Robotium test APKs that you want
to be available through the web app interface. Any Robotium APKs you place in this
directory will be visible to users of the web app. You must place at least one Robotium
APK in this directory.

When you make changes to this file, or to any file in the source tree, you should
run 

$ ant deploy

again, which will build the web app, undeploy the current running version from Tomcat,
and deploy the newly-build version.

At this point you should be able to go to the URLs listed for the AutograderServlet
and MultigraderServlet.

Documentation
-------------

The "deploy" ant task builds javadocs for the project, which are available in 
dist/docs/api. Open index.html in that directory with a web browser to view the full
API docs for the server component.

Known issues and limitations
----------------------------

Because we only run a single emulator, we have to keep a queue of submitted grader
requests. This is implemented through a separate thread, in the GraderRunner class.
This class has a static queue of Graders to run, which means that there are not
separate queues for each servlet (i.e., they share queues). This is not ideal, and
should be rectified in a production deployment.

Also, there is a concurrency issue in the GraderRunner class that manifests itself
in the MultigraderServlet interface. If you submit multiple AI files at once,
the status page that is then displayed at first shows all the jobs as queued,
which is correct. When it pulls the first job from the queue and starts to run it,
that job is no longer visible on the status page though it is in fact being tested.
Once testing of the job is complete, it reappears on the status page with the 
test output visible, as designed. In summary, whatever job is currently being tested
is not visible on the status page, because the GraderRunner currentGrader object is
seen as null by the MultigraderServlet even though the GraderRunner is actively testing
it. I tried to wrap various methods and blocks with "synchronized" statements to
ensure consistency, but was unable to fix this bug.