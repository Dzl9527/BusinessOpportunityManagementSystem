$env:JAVA_HOME = 'D:\java\jdk'
$env:PATH = "D:\java\jdk\bin;" + $env:PATH
$mvn = 'C:\Users\Administrator\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd'
& $mvn spring-boot:run
