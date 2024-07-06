/*
 *  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema Gateway Java SDK
 * This SDK allows for preparing, sending and receiving of Threema Messages via Threema Gateway.
 *
 * The MIT License (MIT)
 * Copyright (c) 2015-2024 Threema GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE
 *
 *
 *
 *
 */

package ch.threema.apitool;

import ch.threema.apitool.results.CapabilityResult;
import ch.threema.apitool.results.EncryptResult;
import ch.threema.apitool.results.UploadResult;
import ch.threema.apitool.exceptions.ApiException;
import ch.threema.apitool.utils.ApiResponse;
import ch.threema.apitool.utils.DataUtils;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.json.JSONArray;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Consumer;

/**
 * Facilitates HTTPS communication with the Threema Message API.
 */
public class APIConnector {
	private final String apiUrl;
	private final PublicKeyStore publicKeyStore;
	private final String apiIdentity;
	private final String secret;
	private final HttpClient httpClient;
	private final Consumer<HttpRequest.Builder> requestInterceptor;

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String userAgent;

	public APIConnector(String apiIdentity, String secret, PublicKeyStore publicKeyStore) {
		this(apiIdentity, secret, null, publicKeyStore);
	}

	public APIConnector(String apiIdentity, String secret, String apiUrl,
					PublicKeyStore publicKeyStore) {
		if (apiUrl != null && !apiUrl.endsWith("/")) {
			apiUrl += "/";
		}
		var reader = new MavenXpp3Reader();
		try {
			var model = reader.read(new FileReader("pom.xml"));
			setUserAgent("threema-msgapi-sdk-java/" + model.getVersion());
		} catch (IOException | XmlPullParserException e) {
			setUserAgent("threema-msgapi-sdk-java");
		}
		this.apiIdentity = apiIdentity;
		this.secret = secret;
		this.apiUrl = apiUrl != null ? apiUrl : "https://msgapi.threema.ch/";
		this.publicKeyStore = publicKeyStore;

		httpClient = HttpClient.newHttpClient();
		requestInterceptor = null;
	}

	/**
	 * Send a text message with server-side encryption.
	 *
	 * @param to recipient ID
	 * @param text message text (max. 3500 bytes)
	 *
	 * @return message ID
	 *
	 * @throws ApiException if a communication or server error occurs
	 */
	public ApiResponse<String> sendTextMessageSimple(String to, String text) throws ApiException {

		Map<String, Object> postParams = makeRequestParams();
		postParams.put("to", to);
		postParams.put("text", text);

		HttpRequest.Builder builder = httpPostRequestBuilder("send_simple", postParams);
		try {
			HttpResponse<InputStream> response = httpClient.send(builder.build(),
							HttpResponse.BodyHandlers.ofInputStream());
			return new ApiResponse<>(response.statusCode(), response.headers().map(),
							new String(response.body().readAllBytes()));
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}

	public ApiResponse<String> sendE2EMessage(String to, byte[] nonce, byte[] box)
					throws ApiException {
		return sendE2EMessage(to, nonce, box, Map.of());
	}

	public ApiResponse<String> sendE2EMessage(String to, byte[] nonce, byte[] box,
					Map<String, ?> options) throws ApiException {

		Map<String, Object> postParams = makeRequestParams();
		postParams.put("to", to);
		postParams.put("nonce", DataUtils.byteArrayToHexString(nonce));
		postParams.put("box", DataUtils.byteArrayToHexString(box));
		if (!options.isEmpty())
			postParams.putAll(options);

		HttpRequest.Builder requestBuilder = httpPostRequestBuilder("send_e2e", postParams);
		try {
			HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(),
							HttpResponse.BodyHandlers.ofInputStream());
			try {
				if (response.statusCode() / 100 != 2) {
					throw getApiException("sendMsgE2E", response);
				}
				return new ApiResponse<>(response.statusCode(), response.headers().map(),
								new String(response.body().readAllBytes()));
			} finally {
				response.body().close();
			}
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}

	public ApiResponse<JSONArray> sendE2EBulkMessage(String[] toArr, byte[][] nonces,
					byte[][] boxes) throws ApiException {
		return sendE2EBulkMessage(toArr, nonces, boxes, Map.of());
	}

	public ApiResponse<JSONArray> sendE2EBulkMessage(String[] toArr, byte[][] nonces,
					byte[][] boxes, Map<String, ?> options) throws ApiException {

		var payloads = new JSONArray();
		for (int i = 0; i < toArr.length; i++) {
			Map<String, Object> postParams = new HashMap<>();
			postParams.put("to", toArr[i]);
			postParams.put("nonce", Base64.getEncoder().encodeToString(nonces[i]));
			postParams.put("box", Base64.getEncoder().encodeToString(boxes[i]));
			postParams.put("group", true);
			postParams.putAll(options);
			payloads.put(postParams);
		}

		HttpRequest.Builder requestBuilder = httpPostRequestBuilder("send_e2e_bulk", payloads);
		try {
			HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(),
							HttpResponse.BodyHandlers.ofInputStream());
			try {
				if (response.statusCode() / 100 != 2) {
					throw getApiException("sendMsgE2EBulk", response);
				}
				return new ApiResponse<>(response.statusCode(), response.headers().map(),
								new JSONArray(new String(response.body().readAllBytes())));
			} finally {
				response.body().close();
			}
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}

	private HttpRequest.Builder createDefaultHttpBuilder() {
		HttpRequest.Builder builder = HttpRequest.newBuilder();

		builder.header("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
		builder.header("Charset", "utf-8");
		builder.header("Cache-control", "no-cache");
		builder.header("Pragma", "no-cache");
		builder.setHeader("User-Agent", userAgent);

		return builder;
	}

	/**
	 * Lookup an ID by phone number. The phone number will be hashed before being sent to the
	 * server.
	 *
	 * @param phoneNumber the phone number in E.164 format
	 * @return the ID, or null if not found
	 * @throws ApiException if a communication or server error occurs
	 */
	public ApiResponse<String> lookupPhone(String phoneNumber) throws ApiException {

		Map<String, Object> getParams = makeRequestParams();

		byte[] phoneHash = CryptTool.hashPhoneNo(phoneNumber);

		HttpRequest.Builder builder = httpGetRequestBuilder("lookup/phone_hash/" + DataUtils
						.byteArrayToHexString(phoneHash != null ? phoneHash : new byte[0]),
						getParams);
		try {
			HttpResponse<InputStream> response = httpClient.send(builder.build(),
							HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() / 100 != 2) {
				throw getApiException("lookup/phone_hash", response);
			}
			return new ApiResponse<>(response.statusCode(), response.headers().map(),
							new String(response.body().readAllBytes()));
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}

	/**
	 * Lookup an ID by email address. The email address will be hashed before being sent to the
	 * server.
	 *
	 * @param email the email address
	 *
	 * @return the ID, or null if not found
	 *
	 * @throws ApiException if a communication or server error occurs
	 */
	public ApiResponse<String> lookupEmail(String email) throws ApiException {

		Map<String, Object> getParams = makeRequestParams();

		byte[] emailHash = CryptTool.hashEmail(email);

		HttpRequest.Builder builder = null;
		if (emailHash != null) {
			builder = httpGetRequestBuilder(
							"lookup/email_hash/" + DataUtils.byteArrayToHexString(emailHash),
							getParams);
		}
		try {
			HttpResponse<InputStream> response = null;
			if (builder != null) {
				response = httpClient.send(builder.build(),
								HttpResponse.BodyHandlers.ofInputStream());
			}
			if (response != null) {
				if (response.statusCode() / 100 != 2) {
					throw getApiException("lookup/email_hash", response);
				}
				return new ApiResponse<>(response.statusCode(), response.headers().map(),
								new String(response.body().readAllBytes()));
			}
			throw new ApiException("emailHash was empty or something.");
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}

	/**
	 * Lookup a public key by ID.
	 *
	 * @param id the ID whose public key is desired
	 *
	 * @return the corresponding public key, or null if not found
	 *
	 * @throws ApiException if a communication or server error occurs
	 */
	public byte[] lookupKey(String id) throws ApiException {
		byte[] key = this.publicKeyStore.getPublicKey(id);
		if (key == null) {
			Map<String, Object> getParams = makeRequestParams();
			HttpRequest.Builder builder = httpGetRequestBuilder("pubkeys/" + id, getParams);
			try {
				HttpResponse<InputStream> response = httpClient.send(builder.build(),
								HttpResponse.BodyHandlers.ofInputStream());
				if (response.statusCode() / 100 != 2) {
					throw getApiException("pubkeys", response);
				}
				return DataUtils.hexStringToByteArray(new String(response.body().readAllBytes()));
			} catch (IOException e) {
				throw new ApiException(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ApiException(e);
			}
		}
		this.publicKeyStore.save(id, key);
		return key;
	}

	/**
	 * Lookup the capabilities of a ID
	 *
	 * @param threemaId The ID whose capabilities should be checked
	 *
	 * @return The capabilities, or null if not found
	 *
	 * @throws IOException
	 * @throws ApiException
	 */
	public CapabilityResult lookupKeyCapability(String threemaId) throws IOException, ApiException {
		HttpRequest.Builder requestBuilder =
						httpGetRequestBuilder("capabilities/" + threemaId, makeRequestParams());
		try {
			HttpResponse<String> response = httpClient.send(requestBuilder.build(),
							HttpResponse.BodyHandlers.ofString());
			return new CapabilityResult(threemaId, response.body().split(","));

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public ApiResponse<Integer> lookupCredits() throws ApiException {
		HttpRequest.Builder builder = httpGetRequestBuilder("credits", makeRequestParams());
		try {
			HttpResponse<InputStream> response = httpClient.send(builder.build(),
							HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() / 100 != 2) {
				throw getApiException("credits", response);
			}
			return new ApiResponse<>(response.statusCode(), response.headers().map(),
							Integer.parseInt(new String(response.body().readAllBytes())));
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}

	/**
	 * Upload a file.
	 *
	 * @param fileEncryptionResult The result of the file encryption (i.e. encrypted file data)
	 *
	 * @return the result of the upload
	 *
	 * @throws IOException
	 */
	public UploadResult uploadFile(EncryptResult fileEncryptionResult) throws IOException {
		return uploadFile(fileEncryptionResult, false);
	}

	/**
	 * Upload a file.
	 *
	 * @param fileEncryptionResult The result of the file encryption (i.e. encrypted file data)
	 * @param persist Whether the uploaded file blob should be persisted
	 * @return the result of the upload
	 * @throws IOException
	 */
	public UploadResult uploadFile(EncryptResult fileEncryptionResult, boolean persist)
					throws IOException {

		String attachmentName = "blob";
		String attachmentFileName = "blob.file";
		char[] chars = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
						.toCharArray();
		StringBuilder boundary = new StringBuilder();
		SecureRandom rand = new SecureRandom();
		int count = rand.nextInt(11) + 30;
		for (int i = 0; i < count; i++) {
			boundary.append(chars[rand.nextInt(chars.length)]);
		}

		var params = makeRequestParams();
		if (persist)
			params.put("persist", "true");
		String queryString = makeUrlEncoded(params);
		URI url = URI.create(this.apiUrl + "upload_blob?" + queryString);

		HttpRequest.Builder builder = createDefaultHttpBuilder();
		builder.uri(url);
		builder.setHeader("Content-Type", "multipart/form-data;boundary=" + boundary);
		Map<Object, Object> multipartMap = Map.of(attachmentName, attachmentFileName);
		HttpRequest.BodyPublisher content = ofMimeMultipartData(multipartMap, boundary.toString(),
						fileEncryptionResult.getResult());
		HttpRequest request = builder.POST(content).build();
		int responseCode;
		HttpResponse<String> response;
		try {
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			responseCode = response.statusCode();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return new UploadResult(responseCode,
						responseCode == 200 ? DataUtils.hexStringToByteArray(response.body())
										: null);
	}

	public static HttpRequest.BodyPublisher ofMimeMultipartData(Map<Object, Object> data,
					String boundary, byte[] fileEncryptionResultData) throws IOException {
		var byteArrays = new ArrayList<byte[]>();
		byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=")
						.getBytes(StandardCharsets.UTF_8);
		for (Map.Entry<Object, Object> entry : data.entrySet()) {
			byteArrays.add(separator);

			if (entry.getValue() instanceof Path) {
				var path = (Path) entry.getValue();
				String mimeType = Files.probeContentType(path);
				byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()
								+ "\"\r\nContent-Type: " + mimeType + "\r\n\r\n")
								.getBytes(StandardCharsets.UTF_8));
				byteArrays.add(Files.readAllBytes(path));
				byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
			} else {
				byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + entry.getValue()
								+ "\"\r\n").getBytes(StandardCharsets.UTF_8));
			}
		}
		byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
		byteArrays.add(fileEncryptionResultData);
		byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
		byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

		return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
	}

	/**
	 * Download a file given its blob ID.
	 *
	 * @param blobId The blob ID of the file
	 *
	 * @return Encrypted file data
	 *
	 * @throws IOException
	 */
	public byte[] downloadFile(byte[] blobId) throws IOException {
		String queryString = makeUrlEncoded(makeRequestParams());
		URL blobUrl = new URL(String.format(this.apiUrl + "blobs/%s?%s",
						DataUtils.byteArrayToHexString(blobId), queryString));

		return blobUrl.openConnection().getInputStream().readAllBytes();
	}

	private Map<String, Object> makeRequestParams() {
		Map<String, Object> postParams = new HashMap<>();

		postParams.put("from", apiIdentity);
		postParams.put("secret", secret);
		return postParams;
	}

	private HttpRequest.Builder httpPostRequestBuilder(String uri, Map<String, Object> postData)
					throws ApiException {
		// verify the required parameter 'postData' is set
		if (postData == null) {
			throw new ApiException(400,
							"Missing the required parameter 'postData' when making post request");
		}

		HttpRequest.Builder requestBuilder = createDefaultHttpBuilder();

		String path = String.format("%s", uri);

		requestBuilder.uri(URI.create(apiUrl + path));

		requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");
		byte[] urlencoded = makeUrlEncoded(postData).getBytes(StandardCharsets.UTF_8);

		// byte[] localVarPostBody = memberVarObjectMapper.writeValueAsBytes(postData);
		requestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(urlencoded));
		// if (memberVarReadTimeout != null) {
		// requestBuilder.timeout(memberVarReadTimeout);
		// }
		if (requestInterceptor != null) {
			requestInterceptor.accept(requestBuilder);
		}
		return requestBuilder;
	}

	private HttpRequest.Builder httpPostRequestBuilder(String path, JSONArray postData)
					throws ApiException {
		// verify the required parameter 'postData' is set
		if (postData == null) {
			throw new ApiException(400,
							"Missing the required parameter 'urlencoded' when making post request");
		}

		HttpRequest.Builder requestBuilder = createDefaultHttpBuilder();

		var params = makeUrlEncoded(makeRequestParams());
		path = String.format("%s?%s", path, params);

		requestBuilder.uri(URI.create(apiUrl + path));

		requestBuilder.header("Content-Type", "application/json");

		requestBuilder.method("POST", HttpRequest.BodyPublishers.ofString(postData.toString()));

		if (requestInterceptor != null) {
			requestInterceptor.accept(requestBuilder);
		}
		return requestBuilder;
	}

	private HttpRequest.Builder httpGetRequestBuilder(String url, Map<String, Object> getParams)
					throws ApiException {
		// verify the required parameter 'postData' is set
		if (getParams == null) {
			throw new ApiException(400,
							"Missing the required parameter 'getParams' when making get request");
		}

		HttpRequest.Builder requestBuilder = createDefaultHttpBuilder();

		String path = String.format("%s?%s", url, makeUrlEncoded(getParams));

		requestBuilder.uri(URI.create(apiUrl + path));

		requestBuilder.method("GET", HttpRequest.BodyPublishers.noBody());
		if (requestInterceptor != null) {
			requestInterceptor.accept(requestBuilder);
		}
		return requestBuilder;
	}

	private String makeUrlEncoded(Map<String, Object> params) {
		StringBuilder s = new StringBuilder();

		for (Map.Entry<String, Object> param : params.entrySet()) {
			if (s.length() > 0)
				s.append('&');

			s.append(param.getKey());
			s.append('=');
			s.append(URLEncoder.encode(String.valueOf(param.getValue()), StandardCharsets.UTF_8));
		}

		return s.toString();
	}

	protected ApiException getApiException(String operationId, HttpResponse<InputStream> response)
					throws IOException {
		String body = response.body() == null ? null : new String(response.body().readAllBytes());
		String message = formatExceptionMessage(operationId, response.statusCode(), body);
		return new ApiException(response.statusCode(), message, response.headers(), body);
	}

	private String formatExceptionMessage(String operationId, int statusCode, String body) {
		if (body == null || body.isEmpty()) {
			body = "[no body]";
		}
		return operationId + " call failed with: " + statusCode + " - " + body;
	}
}
