cd /d %~dp0
move competition.data.db old
java -cp h2-1.1.116.jar org.h2.tools.Script -user sa -url jdbc:h2:old/competition -script old/competition.data.sql
java -cp h2-1.3.175.jar org.h2.tools.RunScript -url jdbc:h2:new/competition -script old/competition.data.sql
move new\competition.h2.db .
