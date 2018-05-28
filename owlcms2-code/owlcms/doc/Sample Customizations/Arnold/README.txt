If running the Windows stand-alone version
- Open the Competition Management folder
- Follow the Configuration shortcup
- copy the Arnold_en.xls file in the current directory to the one found in the Configuration shortcut\templates\competitionBook

- Open the Competition Management folder again
- Follow the Documentation shortcut
- go up one level
- Use the sinclair.properties file to overwrite the corresponding file in owlcms/WEB-INF/classes
- Use the resultBoard_en.jsp file to overwrite the corresponding file in owlcms/jsp




If running the Tomcat services version
- locate the owlcms folder where the application has been deployed
- add Arnold_en.xls to WEB-INF/classes/templates/competitionBook
- overwrite sinclair.propeties in WEB-INF/classes/sinclair.properties
- overwrite resultBoard_en.jsp in the jsp directory