package net.klesatschke.threema.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.PortFactory;
import org.mockserver.socket.tls.KeyStoreFactory;

@ExtendWith(MockServerExtension.class)
@ExtendWith(MockitoExtension.class)
@MockServerSettings(ports = {8787, 8888})
class APIConnectorTest {
  private static ClientAndServer client;

  @Mock PublicKeyStore keystore;

  private APIConnector apiConnector;

  @BeforeAll
  static void startMockServer() {
    // ensure all connection using HTTPS will use the SSL context defined by
    // MockServer to allow dynamically generated certificates to be accepted
    HttpsURLConnection.setDefaultSSLSocketFactory(
        new KeyStoreFactory(new MockServerLogger()).sslContext().getSocketFactory());
    client = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
  }

  @BeforeEach
  void setup() {
    client.reset();
    apiConnector =
        new APIConnector("ID", "secret", "https://localhost:" + client.getPort() + "/", keystore);
  }

  @Test
  void testLookupEmail() throws IOException {
    // GIVEN
    var email = "test@mail.box";
    var hash = DataUtils.byteArrayToHexString(CryptTool.hashEmail(email));
    var path = "/lookup/email_hash/" + hash;
    var threemaID = "the ID";
    client.when(request().withMethod("GET").withPath(path)).respond(response().withBody(threemaID));

    assertThat(apiConnector.lookupEmail(email)).isEqualTo(threemaID);
  }

  @Test
  void testLookupPhone() throws IOException {
    // GIVEN
    var number = "+49172989127128";
    var hash = DataUtils.byteArrayToHexString(CryptTool.hashPhoneNo(number));
    var path = "/lookup/phone_hash/" + hash;
    var threemaID = "the ID";
    client.when(request().withMethod("GET").withPath(path)).respond(response().withBody(threemaID));

    assertThat(apiConnector.lookupPhone(number)).isEqualTo(threemaID);
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 404, 500})
  void testLookupPhoneErrors(int httpStatusCode) throws IOException {
    // GIVEN
    var number = "+49172989127128";
    var hash = DataUtils.byteArrayToHexString(CryptTool.hashPhoneNo(number));
    var path = "/lookup/phone_hash/" + hash;
    client
        .when(request().withMethod("GET").withPath(path))
        .respond(response().withStatusCode(httpStatusCode));

    assertThatIOException().isThrownBy(() -> apiConnector.lookupPhone(number));
  }
}
