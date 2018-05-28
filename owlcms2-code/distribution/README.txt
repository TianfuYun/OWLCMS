Steps to create a release.

-1) Run the JUnit tests (right-click on the owlcms/src/test/java folder and Run As Junit Test)

0)  In SourceTree,
	- use Hg Flow to close all features,
	  start a new release with the expected number (e.g. 2.14.1)
	  MAKE SURE you push changes.
	- Back in Eclipse, on the owlcmsParent folder, right-click and  select "Team/Refresh Status"

1) Prepare the distribution.

a. Make sure that
		owlcms/pom.xml
		distribution/src/site/ReleaseNotes.md (update the Release notes !)
		distribution/pom.xml

   all have correct version numbers.

b. Clean everything:

	if under Eclipse
		stop the Tomcat server if running
		right-click on owlcmsParent project and select "run as/maven clean"
	else
		cd to owlcmsParent
		mvn clean

c. IF FIRST BUILD on a new checkout perform step "FIRST BUILD" at bottom of this file

d. Recompile, run tests, create artifacts:
		if under Eclipse,
			1- menu Project/Clean/Clean all projects + rebuild automatically
			2- right-click on owlcmsParent project and select "run as/maven install"
		else run
			mvn install


e. Create distribution files:

	if under Eclipse
		inside distribution project, right-click on  "prepare uploads.launch" and run it.
	else
		cd to distribution
		mvn clean assembly:single

2) Use NSIS to create the installer

  - go to the distribution project and refresh (F5)
  - right-click on nsis.launch and run it ("run as" "nsis")
  - refresh the target directory (F5) and test the installer (it is created as target/setup_2.X.Y.exe)

    *** if owlcms does not start (black command shell immediately closes),
       a. right click on the launch directory and do a a "run as" "maven install"
       b. start step 2 again.


3) Finalize and publish
  - commit all modified directories with "Release x.y.z" as comment
  - Use SourceTree HgFlow to finalize the code release
  	- Use Hg Flow to finish release
  	- push the tag (make sure checkbox is checked)
  - Right-click on distribution, Team, Refresh Status
  - In the distribution directory, right-click on the "upload to sourceforge.net" entry and run it.


==================================================================================

FIRST BUILD additional steps:

 	Only needed	IF FIRST BUILD at step 1b.

				c. If the widgetset needs building (first build, or Vaadin version has changed.)

				   cd to owlcms
						mvn clean compile vaadin:update-widgetset gwt:compile -P compile-widgetset
						(if under Eclipse, run  widgetset.launch  using right-click on the .launch file)

				d. cd back to owlcmsParent

		END IF FIRST BUILD
