#!/bin/bash
set -e

# A space delimited list of feature IDs
features_whose_bundles_we_will_deploy="org.eclipse.ecf.remoteservice.sdk.feature
org.eclipse.ecf.core.feature
org.eclipse.ecf.core.ssl.feature
org.eclipse.ecf.filetransfer.feature
org.eclipse.ecf.filetransfer.ssl.feature
org.eclipse.ecf.filetransfer.httpclient5.feature
org.eclipse.ecf.filetransfer.httpclientjava.feature
org.eclipse.ecf.remoteservice.examples.feature
org.eclipse.ecf.remoteservices.tooling.bndtools.feature 
org.eclipse.ecf.ai.mcp.tools.feature
org.eclipse.ecf.ai.a2a.feature"

# Exclude orbit bundles, they should be done separately
orbit_bundles="javax.servlet
com.sun.jna
com.sun.jna.platform
org.apache.commons.codec
org.apache.commons.logging
org.apache.httpcomponents.httpclient
org.apache.httpcomponents.httpclient.win
org.apache.httpcomponents.httpcore
org.apache.httpcomponents.client5.httpclient5
org.apache.httpcomponents.client5.httpclient5-win
org.json
org.objectweb.asm"

wget 'https://downloads.sourceforge.net/project/xmltask/xmltask/1.16.1/xmltask.jar?r=https%3A%2F%2Fsourceforge.net%2Fprojects%2Fxmltask%2Ffiles%2Fxmltask%2F1.16.1%2Fxmltask.jar' -O xmltask.jar
cat << EOF > xpath.xml
<project xmlns:if="ant:if" xmlns:unless="ant:unless" name="xpath" default="xpath">
        <target name="init">
                <taskdef name="xmltask" classname="com.oopsconsultancy.xmltask.ant.XmlTask" classpath="xmltask.jar"/>
        </target>
        <target name="xpath" depends="init">
                <xmltask source="\${xml.file}">
                        <copy path="\${xml.path}" property="results" append="true"/>
                </xmltask>
                <echo if:set="results" message="\${results}" file="results" append="false"/>
                <echo unless:set="results" message="" file="results" append="false"/>
        </target>
</project>
EOF

deploy () {
	local target=$1
	mkdir -p $target
	sed -e 's/-SNAPSHOT//' $(dirname $target)/pom.xml > $target/pom.xml
	local pom=$target/pom.xml
	local file="$(ls $target/*-SNAPSHOT.jar 2>/dev/null)"
	local sources="${file%.jar}-sources.jar"
	local javadoc="${file%.jar}-javadoc.jar"

	# Create dummy javadoc jar
	if [ -f "$file" -a ! -f "$javadoc" ] ; then
		echo "Javadoc for ECF is distributed separately, please see: http://download.eclipse.org/rt/ecf/latest/javadoc/" > $target/README.txt
		(cd $target && jar -cf $(basename $javadoc) README.txt)
	fi

	# Build properties list
	local props="-DpomFile=$pom"
	if [ -n "$file" ] ; then
		props="$props -Dfile=$file"
	else
		props="$props -Dfile=$pom"
	fi
	if [ -f "$sources" ] ; then
		props="$props -Dsources=$sources"
	fi
	if [ -f "$javadoc" ] ; then
		props="$props -Djavadoc=$javadoc"
	fi

	#local settings=/opt/public/hipp/homes/genie.ecf/.m2/settings-deploy-ossrh.xml
	#local settings=~/.m2/settings.xml
	echo "mvn --projects $2 deploy"
	mvn --projects "$2" deploy
}

bundles=""
_add_bundle_to_list() {
	local plugin=$1
	local match=0
	for b in $bundles; do
		if [ "$b" = "$plugin" ] ; then
			match=1
		fi
	done
	if [ "$match" != "1" ] ; then
		echo "    $plugin"
		local orbit_match=0
		for o_b in $orbit_bundles ; do
			if [ "$o_b" = "$plugin" ] ; then
				orbit_match=1
				break
			fi
		done
		if [ "$orbit_match" != "1" ] ; then
			bundles="$bundles $plugin"
		fi
	fi
	return $match
}

features=""
_add_feature_to_list() {
	local feature=$1
	local match=0
	for f in $features ; do
		if [ "$f" = "$feature" ] ; then
			match=1
		fi
	done
	if [ "$match" != "1" ] ; then
		echo "  $feature"
		features="$features $feature"
	fi
	return $match
}

xpath_call() {
	local xmlfile=$1
	local xmlquery=$2
	ant -f xpath.xml -Dxml.file=$xmlfile -Dxml.path="$xmlquery" >/dev/null
	cat results
	rm -f results
}

parse_feature() {
	local xml=$(find -path "*/$1/feature.xml")

	# Process included plugins
	local i=0
	while true ; do
		i=$(( $i + 1 ))
		local plugin=$(xpath_call $xml "string(//plugin[$i]/@id)")
		if [ -z "$plugin" ] ; then
			break
		else
			_add_bundle_to_list $plugin || :
		fi
	done

	# Process plugins specified as "requires"
	local i=0
	while true ; do
		i=$(( $i + 1 ))
		local entry=$(xpath_call $xml "//requires/import[$i]")
		if [ -z "$entry" ] ; then
			break
		fi
		local plugin=$(xpath_call $xml "string(//requires/import[$i]/@plugin)")
		if [ -n "$plugin" ] ; then
			_add_bundle_to_list $plugin || :
		fi
	done

	# Process included features
	local i=0
	while true ; do
		i=$(( $i + 1 ))
		local subfeature=$(xpath_call $xml "string(//includes[$i]/@id)")
		if [ -z "$subfeature" ] ; then
			break
		else
			_add_feature_to_list $subfeature && parse_feature $subfeature
		fi
	done

	# Process features specified as "requires"
	local i=0
	while true ; do
		i=$(( $i + 1 ))
		local entry=$(xpath_call $xml "//requires/import[$i]")
		if [ -z "$entry" ] ; then
			break
		fi
		local subfeature=$(xpath_call $xml "string(//requires/import[$i]/@feature)")
		if [ -n "$subfeature" ] ; then
			_add_feature_to_list $subfeature && parse_feature $subfeature
		fi
	done
}

bundles_deploy=""
check_maven_central() {
	local central_metadata="$(curl https://repo1.maven.org/maven2/org/eclipse/ecf/$1/maven-metadata.xml 2>/dev/null | grep '<latest>')"
	local central_version=0.0.0
	if [ -n "$central_metadata" ] ; then
		local central_version=$(echo $central_metadata | sed -e 's/.*>\(.*\)<.*/\1/')
	fi
	local jar_version=$(find -name "$1-*-SNAPSHOT.jar" | tail -n1 | sed -e "s/.*$1-\(.*\)-SNAPSHOT.jar/\1/")
	local op="="
	if [ -z "$jar_version" ] ; then
	   jar_version="0.0.0"
	fi
	if [ $(echo $jar_version | cut -f1 -d.) -gt "$(echo $central_version | cut -f1 -d.)" ] ; then
		op=">"
	elif [ $(echo $jar_version | cut -f2 -d.) -gt "$(echo $central_version | cut -f2 -d.)" ] ; then
		op=">"
	elif [ $(echo $jar_version | cut -f3 -d.) -gt "$(echo $central_version | cut -f3 -d.)" ] ; then
		op=">"
	fi
	if [ "$central_version" = "0.0.0" ] ; then
		central_version="N/A"
	fi
	echo "  $1 $jar_version $op $central_version"
	if [ "$op" != "=" ] ; then
		bundles_deploy="$bundles_deploy $1"
	fi
}

# set PREVIOUS_JAVA_HOME to $JAVA_HOME
PREVIOUS_JAVA_HOME=$JAVA_HOME
PREVIOUS_PATH=$PATH
export JAVA_HOME="/opt/tools/java/openjdk/jdk-11/latest"
PATH=$JAVA_HOME/bin:$PATH
echo
echo "Parsing the following features:"
echo "==============================="
for feature in $features_whose_bundles_we_will_deploy ; do
	_add_feature_to_list $feature && parse_feature $feature
done
export JAVA_HOME=$PREVIOUS_JAVA_HOME
export PATH=$PREVIOUS_PATH

echo
echo "Checking the following found bundles against maven central:"
echo "==========================================================="
bundles_sorted="$(for b in $bundles ; do echo $b ; done | sort)"
for bundle in $bundles_sorted ; do
	check_maven_central $bundle
done

echo
echo "Will deploy the following bundles with newer versions:"
echo "======================================================"
for bundle in $bundles_deploy ; do
	echo "  $bundle"
done

# Deploy parent pom
# deploy "./target"

# Deploy all discovered bundles
echo
for b in $bundles_deploy ; do
	jars=$(find -name "$b-*-SNAPSHOT.jar" | tail -n1)
	for jar in $jars ; do
		deploy "$(dirname $jar)" "$b"
	done
done

