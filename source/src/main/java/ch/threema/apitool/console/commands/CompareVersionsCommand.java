package ch.threema.apitool.console.commands;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.IOException;

public class CompareVersionsCommand {
	public static void main(String[] args) {
		String newVersion = args[0];

		var reader = new MavenXpp3Reader();
		String currentVersion;
		try {
			currentVersion = reader.read(new FileReader("pom.xml")).getVersion();
		} catch (IOException | XmlPullParserException e) {
			throw new RuntimeException(e);
		}
		ComparableVersion currentVersionComp = new ComparableVersion(currentVersion);

		System.exit((currentVersionComp.compareTo(new ComparableVersion(newVersion)) < 0) ? 0 : -1);
	}
}
