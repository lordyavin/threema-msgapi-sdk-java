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

package ch.threema.apitool.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.neilalexander.jnacl.NaCl;

import net.klesatschke.threema.api.APIConnector;
import net.klesatschke.threema.api.CryptTool;
import net.klesatschke.threema.api.exceptions.InvalidKeyException;
import net.klesatschke.threema.api.exceptions.MessageParseException;
import net.klesatschke.threema.api.exceptions.NotAllowedException;
import net.klesatschke.threema.api.messages.FileMessage;
import net.klesatschke.threema.api.messages.ImageMessage;
import net.klesatschke.threema.api.messages.ThreemaMessage;
import net.klesatschke.threema.api.results.CapabilityResult;
import net.klesatschke.threema.api.results.EncryptResult;
import net.klesatschke.threema.api.results.UploadResult;

/** Helper to handle Threema end-to-end encryption. */
public class E2EHelper {
  private final APIConnector apiConnector;
  private final byte[] privateKey;

  public class ReceiveMessageResult {
    private final String messageId;
    private final ThreemaMessage message;
    protected List<File> files = new ArrayList<>();
    protected List<String> errors = new ArrayList<>();

    public ReceiveMessageResult(String messageId, ThreemaMessage message) {
      this.messageId = messageId;
      this.message = message;
    }

    public List<File> getFiles() {
      return this.files;
    }

    public List<String> getErrors() {
      return this.errors;
    }

    public String getMessageId() {
      return messageId;
    }

    public ThreemaMessage getMessage() {
      return message;
    }
  }

  public E2EHelper(APIConnector apiConnector, byte[] privateKey) {
    this.apiConnector = apiConnector;
    this.privateKey = privateKey;
  }

  /**
   * Encrypt a text message and send it to the given recipient.
   *
   * @param threemaId target Threema ID
   * @param text the text to send
   * @return generated message ID
   */
  public String sendTextMessage(String threemaId, String text) throws Exception {
    // fetch public key
    var publicKey = this.apiConnector.lookupKey(threemaId);

    if (publicKey == null) {
      throw new Exception("invalid threema id");
    }
    var res = CryptTool.encryptTextMessage(text, this.privateKey, publicKey);

    return this.apiConnector.sendE2EMessage(threemaId, res.getNonce(), res.getResult());
  }

  /**
   * Encrypt an image message and send it to the given recipient.
   *
   * @param threemaId target Threema ID
   * @param imageFilePath path to read image data from
   * @return generated message ID
   * @throws NotAllowedException
   * @throws IOException
   * @throws InvalidKeyException
   */
  public String sendImageMessage(String threemaId, String imageFilePath)
      throws NotAllowedException, IOException, InvalidKeyException {
    // fetch public key
    var publicKey = this.apiConnector.lookupKey(threemaId);

    if (publicKey == null) {
      throw new InvalidKeyException("invalid threema id");
    }

    // check capability of a key
    var capabilityResult = this.apiConnector.lookupKeyCapability(threemaId);
    if (capabilityResult == null || !capabilityResult.canImage()) {
      throw new NotAllowedException();
    }

    var fileData = Files.readAllBytes(Paths.get(imageFilePath));
    if (fileData == null) {
      throw new IOException("invalid file");
    }

    // encrypt the image
    var encryptResult = CryptTool.encrypt(fileData, this.privateKey, publicKey);

    // upload the image
    var uploadResult = apiConnector.uploadFile(encryptResult);

    if (!uploadResult.isSuccess()) {
      throw new IOException(
          "could not upload file (upload response " + uploadResult.getResponseCode() + ")");
    }

    // send it
    var imageMessage =
        CryptTool.encryptImageMessage(encryptResult, uploadResult, privateKey, publicKey);

    return apiConnector.sendE2EMessage(
        threemaId, imageMessage.getNonce(), imageMessage.getResult());
  }

  /**
   * Encrypt a file message and send it to the given recipient. The thumbnailMessagePath can be
   * null.
   *
   * @param threemaId target Threema ID
   * @param fileMessageFile the file to be sent
   * @param thumbnailMessagePath file for thumbnail; if not set, no thumbnail will be sent
   * @return generated message ID
   * @throws InvalidKeyException
   * @throws IOException
   * @throws NotAllowedException
   */
  public String sendFileMessage(String threemaId, File fileMessageFile, File thumbnailMessagePath)
      throws InvalidKeyException, IOException, NotAllowedException {
    // fetch public key
    var publicKey = this.apiConnector.lookupKey(threemaId);

    if (publicKey == null) {
      throw new InvalidKeyException("invalid threema id");
    }

    // check capability of a key
    var capabilityResult = this.apiConnector.lookupKeyCapability(threemaId);
    if (capabilityResult == null || !capabilityResult.canImage()) {
      throw new NotAllowedException();
    }

    if (!fileMessageFile.isFile()) {
      throw new IOException("invalid file");
    }

    var fileData = this.readFile(fileMessageFile);

    if (fileData == null) {
      throw new IOException("invalid file");
    }

    // encrypt the image
    var encryptResult = CryptTool.encryptFileData(fileData);

    // upload the image
    var uploadResult = apiConnector.uploadFile(encryptResult);

    if (!uploadResult.isSuccess()) {
      throw new IOException(
          "could not upload file (upload response " + uploadResult.getResponseCode() + ")");
    }

    UploadResult uploadResultThumbnail = null;

    if (thumbnailMessagePath != null && thumbnailMessagePath.isFile()) {
      var thumbnailData = this.readFile(thumbnailMessagePath);
      if (thumbnailData == null) {
        throw new IOException("invalid thumbnail file");
      }

      // encrypt the thumbnail
      var encryptResultThumbnail =
          CryptTool.encryptFileThumbnailData(fileData, encryptResult.getSecret());

      // upload the thumbnail
      uploadResultThumbnail = this.apiConnector.uploadFile(encryptResultThumbnail);
    }

    // send it
    var fileMessage =
        CryptTool.encryptFileMessage(
            encryptResult,
            uploadResult,
            Files.probeContentType(fileMessageFile.toPath()),
            fileMessageFile.getName(),
            (int) fileMessageFile.length(),
            uploadResultThumbnail,
            privateKey,
            publicKey);

    return this.apiConnector.sendE2EMessage(
        threemaId, fileMessage.getNonce(), fileMessage.getResult());
  }

  /**
   * Decrypt a Message and download the blobs of the Message (e.g. image or file)
   *
   * @param threemaId Threema ID of the sender
   * @param messageId Message ID
   * @param box Encrypted box data of the file/image message
   * @param nonce Nonce that was used for message encryption
   * @param outputFolder Output folder for storing decrypted images/files
   * @return result of message reception
   * @throws IOException
   * @throws InvalidKeyException
   * @throws MessageParseException
   */
  public ReceiveMessageResult receiveMessage(
      String threemaId, String messageId, byte[] box, byte[] nonce, Path outputFolder)
      throws IOException, InvalidKeyException, MessageParseException {
    // fetch public key
    var publicKey = this.apiConnector.lookupKey(threemaId);

    if (publicKey == null) {
      throw new InvalidKeyException("invalid threema id");
    }

    var message = CryptTool.decryptMessage(box, this.privateKey, publicKey, nonce);
    if (message == null) {
      return null;
    }

    var result = new ReceiveMessageResult(messageId, message);

    if (message instanceof ImageMessage imageMessage) {
      var fileData = this.apiConnector.downloadFile(imageMessage.getBlobId());

      if (fileData == null) {
        throw new MessageParseException();
      }

      var decryptedFileContent =
          CryptTool.decrypt(fileData, privateKey, publicKey, imageMessage.getNonce());
      var imageFile = new File(outputFolder.toString() + "/" + messageId + ".jpg");
      var fos = new FileOutputStream(imageFile);
      fos.write(decryptedFileContent);
      fos.close();

      result.files.add(imageFile);
    } else if (message instanceof FileMessage fileMessage) {
      var fileData = this.apiConnector.downloadFile(fileMessage.getBlobId());

      var decryptedFileData =
          CryptTool.decryptFileData(fileData, fileMessage.getEncryptionKey());
      var file =
          new File(outputFolder.toString() + "/" + messageId + "-" + fileMessage.getFileName());
      var fos = new FileOutputStream(file);
      fos.write(decryptedFileData);
      fos.close();

      result.files.add(file);

      if (fileMessage.getThumbnailBlobId() != null) {
        var thumbnailData = this.apiConnector.downloadFile(fileMessage.getThumbnailBlobId());

        var decryptedThumbnailData =
            CryptTool.decryptFileThumbnailData(thumbnailData, fileMessage.getEncryptionKey());
        var thumbnailFile = new File(outputFolder.toString() + "/" + messageId + "-thumbnail.jpg");
        fos = new FileOutputStream(thumbnailFile);
        fos.write(decryptedThumbnailData);
        fos.close();

        result.files.add(thumbnailFile);
      }
    }

    return result;
  }

  /**
   * Read file data from file - store at offset in byte array for in-place encryption
   *
   * @param file input file
   * @return file data with padding/offset for NaCl
   * @throws IOException
   */
  private byte[] readFile(File file) throws IOException {
    var fileLength = (int) file.length();
    var fileData = new byte[fileLength + NaCl.BOXOVERHEAD];
    IOUtils.readFully(new FileInputStream(file), fileData, NaCl.BOXOVERHEAD, fileLength);
    return fileData;
  }
}
