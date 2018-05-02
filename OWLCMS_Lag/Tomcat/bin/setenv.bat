@echo off
IF exist "C:\Program Files\Java" (
	PUSHD "C:\Program Files\Java\jre*"
	GOTO setJavaHome
) ELSE (
	IF exist "C:\Program Files (x86)\Java" (
		PUSHD "C:\Program Files (x86)\Java\jre*"
		GOTO setJavaHome
	) ELSE (
		ECHO PLEASE BROWSE FOR THE JAVA INSTALLATION FOLDER. Example: C:\Program Files\Java\jre1.8.0_144.
		For /F "Tokens=1 Delims=" %%I In ('cscript //nologo BrowseFolder.vbs') Do Set _FolderName=%%I
		GOTO PushToFolder
	)
)
:PushToFolder
PUSHD %_FolderName%
GOTO setJavaHome

:setJavaHome
set JRE_HOME=%cd%
POPD