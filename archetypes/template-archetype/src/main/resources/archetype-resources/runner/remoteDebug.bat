java -Dfelix.config.properties=file:felix/config.ini -Dfelix.system.properties=file:felix/system.properties -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5015 -jar bundles/org.apache.felix.main_*.jar
