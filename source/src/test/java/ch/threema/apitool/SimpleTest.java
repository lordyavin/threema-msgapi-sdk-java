package ch.threema.apitool;

import ch.threema.apitool.exceptions.ApiException;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.IOException;

public class SimpleTest {

	public static void main(String[] args) {
		if (args == null || args.length < 3 || args.length > 4) {
			System.out.printf("Usage: %s Threema-ID Gateway-ID Secret [ApiUrl]%n",
							new java.io.File(E2ETest.class.getProtectionDomain().getCodeSource()
											.getLocation().getPath()).getName());
			System.exit(-1);
		}
		var threemaId = args[0];
		String gatewayId = args[1];
		var secret = args[2];
		var apiUrl = args.length > 3 ? args[3] : null;
		var reader = new MavenXpp3Reader();
		APIConnector connector;
		try {
			connector = new APIConnector(gatewayId, secret, apiUrl, new PublicKeyStore() {
				@Override
				protected byte[] fetchPublicKey(String threemaId) {
					return null;
				}

				@Override
				protected void save(String threemaId, byte[] publicKey) {

				}
			});
			connector.setUserAgent(String.format("threema-msgapi-sdk-java/%s-test",
							reader.read(new FileReader("pom.xml")).getVersion()));

			var res = connector.sendTextMessageSimple(threemaId, "Simple text message");
			System.out.println(res.getData() + " " + res.getStatusCode()); // OK (, no receive)
		} catch (IOException | XmlPullParserException | ApiException e) {
			throw new RuntimeException(e);
		}
	}
}
