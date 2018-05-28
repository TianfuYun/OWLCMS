1) Copy the pushFTP folder to your desktop

2) Double-click on the pushFTP folder to open it.


3) copy the index.html file to your public Web site using FTP. Note the user name and password you are using.
- the simplest way is to copy it to the root of your Web site
- BUT you can put the file wherever you want.  The location where you copy the index.html is the URL you will be handing out.
- ALSO, if you put the file other than at the root, you will need to edit the index.html file so it can reach the scoreboard.html file,
or else edit the config.properties file to change the ftpRemoteFileName=scoreBoard.html entry so index.html finds the scoreboad copy


4) Edit the config.properties file to reflect your FTP site
- change the FTP site, user and password values to be those you used in step 3
ftpConnect=ftp.owlcms.altervista.org
ftpUser=userNameAsUsedInStep3
ftpPass=passwordAsUsedInStep3

5) Edit config.properties to make sure you are reading the live competition scoreboard correctly
Make sure the competition site scoreboard is running
- Look at config.properties after the scoreBoardURL=
- edit config.properties to ajust the localhost:8080 part with what is appropriate at your site
- use a web browser from the machine where pushFTP is running to check that you can reach the competition scoreboard using that URL


6) double click on the "Command" link and launch the program with the following command

java -jar pushFTP-1.2.0.jar


stop it with ctrl-C once you have checked that the scoreboard is getting copied.



7) edit config.properties and change the first line to

debug=false



8) run the program again.
