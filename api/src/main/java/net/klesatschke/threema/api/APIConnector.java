/*
 * $Id$
 *
 * The MIT License (MIT)
 * Copyright (c) 2015 Threema GmbH
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
 */

package net.klesatschke.threema.api;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import net.klesatschke.threema.api.results.CapabilityResult;
import net.klesatschke.threema.api.results.EncryptResult;
import net.klesatschke.threema.api.results.UploadResult;

/** Facilitates HTTPS communication with the Threema Message API. */
public class APIConnector {
  private static final int BUFFER_SIZE = 16384;

  public interface ProgressListener {

    /**
     * Update the progress of an upload/download process.
     *
     * @param progress in percent (0..100)
     */
    void updateProgress(int progress);
  }

  public class InputStreamLength {
    public final InputStream inputStream;
    public final int length;

    public InputStreamLength(InputStream inputStream, int length) {
      this.inputStream = inputStream;
      this.length = length;
    }
  }

  private final String apiUrl;
  private final PublicKeyStore publicKeyStore;
  private final String apiIdentity;
  private final String secret;

  public APIConnector(String apiIdentity, String secret, PublicKeyStore publicKeyStore) {
    this(apiIdentity, secret, "https://msgapi.threema.ch/", publicKeyStore);
  }

  public APIConnector(
      String apiIdentity, String secret, String apiUrl, PublicKeyStore publicKeyStore) {
    this.apiIdentity = apiIdentity;
    this.secret = secret;
    this.apiUrl = apiUrl;
    this.publicKeyStore = publicKeyStore;
  }

  /**
   * Send a text message with server-side encryption.
   *
   * @param to recipient ID
   * @param text message text (max. 3500 bytes)
   * @return message ID
   * @throws IOException if a communication or server error occurs
   */
  public String sendTextMessageSimple(String to, String text) throws IOException {

    var postParams = makeRequestParams();
    postParams.put("to", to);
    postParams.put("text", text);

    return doPost(new URL(this.apiUrl + "send_simple"), postParams);
  }

  /**
   * Send an end-to-end encrypted message.
   *
   * @param to recipient ID
   * @param nonce nonce used for encryption (24 bytes)
   * @param box encrypted message data (max. 4000 bytes)
   * @return message ID
   * @throws IOException if a communication or server error occurs
   */
  public String sendE2EMessage(String to, byte[] nonce, byte[] box) throws IOException {

    var postParams = makeRequestParams();
    postParams.put("to", to);
    postParams.put("nonce", DataUtils.byteArrayToHexString(nonce));
    postParams.put("box", DataUtils.byteArrayToHexString(box));

    return doPost(new URL(this.apiUrl + "send_e2e"), postParams);
  }

  /**
   * Lookup an ID by phone number. The phone number will be hashed before being sent to the server.
   *
   * @param phoneNumber the phone number in E.164 format
   * @return the ID, or null if not found
   * @throws IOException if a communication or server error occurs
   */
  public String lookupPhone(String phoneNumber) throws IOException {

    try {
      var getParams = makeRequestParams();

      var phoneHash = CryptTool.hashPhoneNo(phoneNumber);

      return doGet(
          new URL(this.apiUrl + "lookup/phone_hash/" + DataUtils.byteArrayToHexString(phoneHash)),
          getParams);
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  /**
   * Lookup an ID by email address. The email address will be hashed before being sent to the
   * server.
   *
   * @param email the email address
   * @return the ID, or null if not found
   * @throws IOException if a communication or server error occurs
   */
  public String lookupEmail(String email) throws IOException {

    try {
      var getParams = makeRequestParams();

      var emailHash = CryptTool.hashEmail(email);

      return doGet(
          new URL(this.apiUrl + "lookup/email_hash/" + DataUtils.byteArrayToHexString(emailHash)),
          getParams);
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  /**
   * Lookup a public key by ID.
   *
   * @param id the ID whose public key is desired
   * @return the corresponding public key, or null if not found
   * @throws IOException if a communication or server error occurs
   */
  public byte[] lookupKey(String id) throws IOException {
    var key = this.publicKeyStore.getPublicKey(id);
    if (key == null) {
      try {
        var getParams = makeRequestParams();
        var pubkeyHex = doGet(new URL(this.apiUrl + "pubkeys/" + id), getParams);
        key = DataUtils.hexStringToByteArray(pubkeyHex);

        if (key != null) {
          this.publicKeyStore.save(id, key);
        }
      } catch (FileNotFoundException e) {
        return new byte[0];
      }
    }
    return key;
  }

  /**
   * Lookup the capabilities of a ID
   *
   * @param threemaId The ID whose capabilities should be checked
   * @return The capabilities, or null if not found
   * @throws IOException
   */
  public CapabilityResult lookupKeyCapability(String threemaId) throws IOException {
    var res = doGet(new URL(this.apiUrl + "capabilities/" + threemaId), makeRequestParams());
    if (res != null) {
      return new CapabilityResult(threemaId, res.split(","));
    }
    return null;
  }

  public Integer lookupCredits() throws IOException {
    var res = doGet(new URL(this.apiUrl + "credits"), makeRequestParams());
    if (res != null) {
      return Integer.valueOf(res);
    }
    return null;
  }
  /**
   * Upload a file.
   *
   * @param fileEncryptionResult The result of the file encryption (i.e. encrypted file data)
   * @return the result of the upload
   * @throws IOException
   */
  public UploadResult uploadFile(EncryptResult fileEncryptionResult) throws IOException {

    var attachmentName = "blob";
    var attachmentFileName = "blob.file";
    var crlf = "\r\n";
    var twoHyphens = "--";

    var chars = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    var rand = new SecureRandom();
    var count = rand.nextInt(11) + 30;
    var boundary = new StringBuilder(count);
    for (var i = 0; i < count; i++) {
      boundary.append(chars[rand.nextInt(chars.length)]);
    }

    var queryString = makeUrlEncoded(makeRequestParams());
    var url = new URL(this.apiUrl + "upload_blob?" + queryString);

    var connection = (HttpsURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setUseCaches(false);

    connection.setRequestMethod("POST");
    connection.setRequestProperty("Connection", "Keep-Alive");
    connection.setRequestProperty("Cache-Control", "no-cache");
    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

    var request = new DataOutputStream(connection.getOutputStream());

    request.writeBytes(twoHyphens + boundary + crlf);
    request.writeBytes(
        "Content-Disposition: form-data; name=\""
            + attachmentName
            + "\";filename=\""
            + attachmentFileName
            + "\""
            + crlf);
    request.writeBytes(crlf);
    request.write(fileEncryptionResult.getResult());
    request.writeBytes(crlf);
    request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

    String response = null;
    var responseCode = connection.getResponseCode();

    if (responseCode == 200) {
      var is = connection.getInputStream();
      var br = new BufferedReader(new InputStreamReader(is));
      response = br.readLine();
      br.close();
    }

    connection.disconnect();

    return new UploadResult(
        responseCode, response != null ? DataUtils.hexStringToByteArray(response) : null);
  }

  /**
   * Download a file given its blob ID.
   *
   * @param blobId The blob ID of the file
   * @return Encrypted file data
   * @throws IOException
   */
  public byte[] downloadFile(byte[] blobId) throws IOException {
    return this.downloadFile(blobId, null);
  }

  /**
   * Download a file given its blob ID.
   *
   * @param blobId The blob ID of the file
   * @param progressListener An object that will receive progress information, or null
   * @return Encrypted file data
   * @throws IOException
   */
  public byte[] downloadFile(byte[] blobId, ProgressListener progressListener) throws IOException {
    var queryString = makeUrlEncoded(makeRequestParams());
    var blobUrl =
        new URL(
            String.format(
                "%sblobs/%s?%s", this.apiUrl, DataUtils.byteArrayToHexString(blobId), queryString));

    var connection = (HttpsURLConnection) blobUrl.openConnection();
    connection.setConnectTimeout(20 * 1000);
    connection.setReadTimeout(20 * 1000);
    connection.setDoOutput(false);

    var inputStream = connection.getInputStream();
    var contentLength = connection.getContentLength();
    var isl = new InputStreamLength(inputStream, contentLength);

    /* Content length known? */
    byte[] blob;
    if (isl.length != -1) {
      blob = new byte[isl.length];
      var offset = 0;
      int readed;

      while (offset < isl.length
          && (readed = isl.inputStream.read(blob, offset, isl.length - offset)) != -1) {
        offset += readed;

        if (progressListener != null) {
          progressListener.updateProgress(100 * offset / isl.length);
        }
      }

      if (offset != isl.length) {
        throw new IOException(
            "Unexpected read size. current: " + offset + ", excepted: " + isl.length);
      }
    } else {
      /* Content length is unknown - need to read until EOF */

      var bos = new ByteArrayOutputStream();
      var buffer = new byte[BUFFER_SIZE];

      int read;
      while ((read = isl.inputStream.read(buffer)) != -1) {
        bos.write(buffer, 0, read);
      }

      blob = bos.toByteArray();
    }
    if (progressListener != null) {
      progressListener.updateProgress(100);
    }

    return blob;
  }

  private Map<String, String> makeRequestParams() {
    return Map.of("from", apiIdentity, "secret", secret);
  }

  private String doGet(URL url, Map<String, String> getParams) throws IOException {

    if (getParams != null) {
      var queryString = makeUrlEncoded(getParams);

      url = new URL(url.toString() + "?" + queryString);
    }

    var connection = (HttpsURLConnection) url.openConnection();
    connection.setDoOutput(false);
    connection.setDoInput(true);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod("GET");
    connection.setUseCaches(false);

    var is = connection.getInputStream();
    var br = new BufferedReader(new InputStreamReader(is));
    var response = br.readLine();
    br.close();

    connection.disconnect();

    return response;
  }

  private String doPost(URL url, Map<String, String> postParams) throws IOException {

    var postData = makeUrlEncoded(postParams).getBytes(StandardCharsets.UTF_8);

    var connection = (HttpsURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setRequestProperty("Charset", "utf-8");
    connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
    connection.setUseCaches(false);

    var os = connection.getOutputStream();
    os.write(postData);
    os.flush();
    os.close();

    var is = connection.getInputStream();
    var br = new BufferedReader(new InputStreamReader(is));
    var response = br.readLine();
    br.close();

    connection.disconnect();

    return response;
  }

  private String makeUrlEncoded(Map<String, String> params) {
    var s = new StringBuilder();

    for (Map.Entry<String, String> param : params.entrySet()) {
      if (s.length() > 0) {
        s.append('&');
      }

      s.append(param.getKey());
      s.append('=');
      try {
        s.append(URLEncoder.encode(param.getValue(), "UTF-8"));
      } catch (UnsupportedEncodingException ignored) {
        // ignored
      }
    }

    return s.toString();
  }
}
