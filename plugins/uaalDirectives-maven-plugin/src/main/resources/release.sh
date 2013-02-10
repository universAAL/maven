# clean SVN update
svn cleanup ../
svn revert ../
svn update ../

# run Directives Check
mvn org.universAAL.support:uaalDirectives-maven-plugin:svnIgnore-check \
org.universAAL.support:uaalDirectives-maven-plugin:svn-check \
org.universAAL.support:uaalDirectives-maven-plugin:decouple-check \
org.universAAL.support:uaalDirectives-maven-plugin:dependency-check \
org.universAAL.support:uaalDirectives-maven-plugin:parent-check \
org.universAAL.support:uaalDirectives-maven-plugin:name-check -fn -DfailOnMissMatch=true

# Tag last SNAPSHOT
mvn org.universAAL.support:uaalDirectives-maven-plugin:tag

# Change ALL versions to nextReleaseVersion
mvn org.universAAL.support:uaalDirectives-maven-plugin:change-version -DnewVersion=<NEW_VERSION>
# use versions:update-children-modules to set the new parentPOM version
mvn versions:update-child-modules versions:commit
# Fix DependencyManagement
mvn org.universAAL.support:uaalDirectives-maven-plugin:dependency-check -Ddirective.fix
# update DependencyManagement imports to point to latest root pom, and Parent for latest uAAL.pom
# and itest dependencyManagement (if proceeds)
mvn org.universAAL.support:uaalDirectives-maven-plugin:update-roots -DnewVersion=<NEW_VERSION>

#########
# Check it installs correctly
#########
mvn clean install

# Tag Working Copy
mvn org.universAAL.support:uaalDirectives-maven-plugin:tag

# Deploy
mvn clean deploy -Prelease -DignoreLock

# Increase ALL versions to development version
mvn org.universAAL.support:uaalDirectives-maven-plugin:increase-version
# use versions:update-children-modules to set parentPOM version
mvn versions:update-child-modules versions:commit
# Fix DependencyManagement
mvn org.universAAL.support:uaalDirectives-maven-plugin:dependency-check -Ddirective.fix
# update DependencyManagement imports to point to latest root pom, and Parent for latest uAAL.pom
# and itest dependencyManagement (if proceeds)
mvn org.universAAL.support:uaalDirectives-maven-plugin:update-roots -DnewVersion=<NEW_RELEASE_VERSION>

#########
# Check it installs correctly, again
#########
mvn clean install

# commit new Development version
svn commit ../ -m "New Development Version"