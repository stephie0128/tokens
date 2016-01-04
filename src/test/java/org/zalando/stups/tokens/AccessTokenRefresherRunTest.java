package org.zalando.stups.tokens;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.internal.util.io.IOUtil;

public class AccessTokenRefresherRunTest {

	private static final String CREDENTIALS_DIR = "CREDENTIALS_DIR";
	private static final String HTTP_EXAMPLE_ORG = "http://example.org";
	private URI uri = URI.create(HTTP_EXAMPLE_ORG);
	private ClientCredentialsProvider ccp = Mockito.mock(ClientCredentialsProvider.class);
	private UserCredentialsProvider ucp = Mockito.mock(UserCredentialsProvider.class);
	private HttpProviderFactory hpf = Mockito.mock(HttpProviderFactory.class);

	private AccessTokensBuilder accessTokenBuilder;
	private AccessTokens accessTokens;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Before
	public void createCredentials() throws IOException {
		File tempDir = tempFolder.newFolder();
		File clientJson = new File(tempDir, "client.json");
		IOUtil.writeText("{\"client_id\":\"abcdefg \",\"client_secret\":\"geheim\"}", clientJson);
		File userJson = new File(tempDir, "user.json");
		IOUtil.writeText("{\"application_username\":\"klaus \",\"application_password\":\"geheim\"}", userJson);
		System.setProperty(CREDENTIALS_DIR, tempDir.getAbsolutePath());

		accessTokenBuilder = Tokens.createAccessTokensWithUri(uri).usingClientCredentialsProvider(ccp)
				.usingUserCredentialsProvider(ucp).usingHttpProviderFactory(hpf).manageToken("TR_TEST").done();
	}

	@After
	public void resetSystemProperty() {
		System.getProperties().remove(CREDENTIALS_DIR);
	}

	@Test
	public void runAccessTokenRefresher() throws InterruptedException, UnsupportedEncodingException {
		HttpProvider httpProvider = Mockito.mock(HttpProvider.class);

		Mockito.when(hpf.create(Mockito.any(ClientCredentials.class), Mockito.any(UserCredentials.class),
				Mockito.any(URI.class), Mockito.any(HttpConfig.class))).thenReturn(httpProvider);
		
		Mockito.when(httpProvider.createToken(Mockito.any(AccessTokenConfiguration.class))).thenReturn(new AccessToken("123456789", "BEARER", 100, new Date(System.currentTimeMillis() + 15000)));
		accessTokens = accessTokenBuilder.start();
		Assertions.assertThat(accessTokens).isNotNull();

		TimeUnit.SECONDS.sleep(30);

		Mockito.verify(httpProvider, Mockito.atLeastOnce()).createToken(Mockito.any(AccessTokenConfiguration.class));

	}

}
