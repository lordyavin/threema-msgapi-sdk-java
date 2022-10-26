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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.EndianUtils;

import com.neilalexander.jnacl.NaCl;

import net.klesatschke.threema.api.exceptions.BadMessageException;
import net.klesatschke.threema.api.exceptions.DecryptionFailedException;
import net.klesatschke.threema.api.exceptions.MessageParseException;
import net.klesatschke.threema.api.exceptions.UnsupportedMessageTypeException;
import net.klesatschke.threema.api.messages.DeliveryReceipt;
import net.klesatschke.threema.api.messages.FileMessage;
import net.klesatschke.threema.api.messages.ImageMessage;
import net.klesatschke.threema.api.messages.TextMessage;
import net.klesatschke.threema.api.messages.ThreemaMessage;
import net.klesatschke.threema.api.results.EncryptResult;
import net.klesatschke.threema.api.results.UploadResult;

/** Contains static methods to do various Threema cryptography related tasks. */
public class CryptTool {
  private static final String HMAC_SHA256 = "HmacSHA256";

  private CryptTool() {}

  /* HMAC-SHA256 keys for email/mobile phone hashing */
  private static final byte[] EMAIL_HMAC_KEY = {
    (byte) 0x30,
    (byte) 0xa5,
    (byte) 0x50,
    (byte) 0x0f,
    (byte) 0xed,
    (byte) 0x97,
    (byte) 0x01,
    (byte) 0xfa,
    (byte) 0x6d,
    (byte) 0xef,
    (byte) 0xdb,
    (byte) 0x61,
    (byte) 0x08,
    (byte) 0x41,
    (byte) 0x90,
    (byte) 0x0f,
    (byte) 0xeb,
    (byte) 0xb8,
    (byte) 0xe4,
    (byte) 0x30,
    (byte) 0x88,
    (byte) 0x1f,
    (byte) 0x7a,
    (byte) 0xd8,
    (byte) 0x16,
    (byte) 0x82,
    (byte) 0x62,
    (byte) 0x64,
    (byte) 0xec,
    (byte) 0x09,
    (byte) 0xba,
    (byte) 0xd7
  };
  private static final byte[] PHONENO_HMAC_KEY = {
    (byte) 0x85,
    (byte) 0xad,
    (byte) 0xf8,
    (byte) 0x22,
    (byte) 0x69,
    (byte) 0x53,
    (byte) 0xf3,
    (byte) 0xd9,
    (byte) 0x6c,
    (byte) 0xfd,
    (byte) 0x5d,
    (byte) 0x09,
    (byte) 0xbf,
    (byte) 0x29,
    (byte) 0x55,
    (byte) 0x5e,
    (byte) 0xb9,
    (byte) 0x55,
    (byte) 0xfc,
    (byte) 0xd8,
    (byte) 0xaa,
    (byte) 0x5e,
    (byte) 0xc4,
    (byte) 0xf9,
    (byte) 0xfc,
    (byte) 0xd8,
    (byte) 0x69,
    (byte) 0xe2,
    (byte) 0x58,
    (byte) 0x37,
    (byte) 0x07,
    (byte) 0x23
  };

  private static final byte[] FILE_NONCE = {
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
  };
  private static final byte[] FILE_THUMBNAIL_NONCE = {
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02
  };

  private static final SecureRandom random = new SecureRandom();

  /**
   * Encrypt a text message.
   *
   * @param text the text to be encrypted (max. 3500 bytes)
   * @param senderPrivateKey the private key of the sending ID
   * @param recipientPublicKey the public key of the receiving ID
   * @return encrypted result
   */
  public static EncryptResult encryptTextMessage(
      String text, byte[] senderPrivateKey, byte[] recipientPublicKey) {
    return encryptMessage(new TextMessage(text), senderPrivateKey, recipientPublicKey);
  }

  /**
   * Encrypt an image message.
   *
   * @param encryptResult result of the image encryption
   * @param uploadResult result of the upload
   * @param senderPrivateKey the private key of the sending ID
   * @param recipientPublicKey the public key of the receiving ID
   * @return encrypted result
   */
  public static EncryptResult encryptImageMessage(
      EncryptResult encryptResult,
      UploadResult uploadResult,
      byte[] senderPrivateKey,
      byte[] recipientPublicKey) {
    return encryptMessage(
        new ImageMessage(
            uploadResult.getBlobId(), encryptResult.getSize(), encryptResult.getNonce()),
        senderPrivateKey,
        recipientPublicKey);
  }

  /**
   * Encrypt a file message.
   *
   * @param encryptResult result of the file data encryption
   * @param uploadResult result of the upload
   * @param mimeType MIME type of the file
   * @param fileName File name
   * @param fileSize Size of the file, in bytes
   * @param uploadResultThumbnail result of thumbnail upload
   * @param senderPrivateKey Private key of sender
   * @param recipientPublicKey Public key of recipient
   * @return Result of the file message encryption (not the same as the file data encryption!)
   */
  public static EncryptResult encryptFileMessage(
      EncryptResult encryptResult,
      UploadResult uploadResult,
      String mimeType,
      String fileName,
      int fileSize,
      UploadResult uploadResultThumbnail,
      byte[] senderPrivateKey,
      byte[] recipientPublicKey) {
    return encryptMessage(
        new FileMessage(
            uploadResult.getBlobId(),
            encryptResult.getSecret(),
            mimeType,
            fileName,
            fileSize,
            uploadResultThumbnail != null ? uploadResultThumbnail.getBlobId() : null),
        senderPrivateKey,
        recipientPublicKey);
  }

  private static EncryptResult encryptMessage(
      ThreemaMessage threemaMessage, byte[] privateKey, byte[] publicKey) {
    /* determine random amount of PKCS7 padding */
    var padbytes = random.nextInt(254) + 1;

    byte[] messageBytes;
    try {
      messageBytes = threemaMessage.getData();
    } catch (BadMessageException e) {
      return null;
    }

    /* prepend type byte (0x02) to message data */
    var data = new byte[1 + messageBytes.length + padbytes];
    data[0] = (byte) threemaMessage.getTypeCode();

    System.arraycopy(messageBytes, 0, data, 1, messageBytes.length);

    /* append padding */
    for (var i = 0; i < padbytes; i++) {
      data[i + 1 + messageBytes.length] = (byte) padbytes;
    }

    return encrypt(data, privateKey, publicKey);
  }

  /**
   * Decrypt an NaCl box using the recipient's private key and the sender's public key.
   *
   * @param box The box to be decrypted
   * @param privateKey The private key of the recipient
   * @param publicKey The public key of the sender
   * @param nonce The nonce that was used for encryption
   * @return The decrypted data, or null if decryption failed
   */
  public static byte[] decrypt(byte[] box, byte[] privateKey, byte[] publicKey, byte[] nonce) {
    return new NaCl(privateKey, publicKey).decrypt(box, nonce);
  }

  /**
   * Decrypt symmetrically encrypted file data.
   *
   * @param fileData The encrypted file data
   * @param secret The symmetric key that was used for encryption
   * @return The decrypted file data, or null if decryption failed
   */
  public static byte[] decryptFileData(byte[] fileData, byte[] secret) {
    return NaCl.symmetricDecryptData(fileData, secret, FILE_NONCE);
  }

  /**
   * Decrypt symmetrically encrypted file thumbnail data.
   *
   * @param fileData The encrypted thumbnail data
   * @param secret The symmetric key that was used for encryption
   * @return The decrypted thumbnail data, or null if decryption failed
   */
  public static byte[] decryptFileThumbnailData(byte[] fileData, byte[] secret) {
    return NaCl.symmetricDecryptData(fileData, secret, FILE_THUMBNAIL_NONCE);
  }

  /**
   * Decrypt a message.
   *
   * @param box the box to be decrypted
   * @param recipientPrivateKey the private key of the receiving ID
   * @param senderPublicKey the public key of the sending ID
   * @param nonce the nonce that was used for the encryption
   * @return decrypted message (text or delivery receipt)
   */
  public static ThreemaMessage decryptMessage(
      byte[] box, byte[] recipientPrivateKey, byte[] senderPublicKey, byte[] nonce)
      throws MessageParseException {

    var data = decrypt(box, recipientPrivateKey, senderPublicKey, nonce);
    if (data == null) {
      throw new DecryptionFailedException();
    }

    /* remove padding */
    var padbytes = data[data.length - 1] & 0xFF;
    var realDataLength = data.length - padbytes;
    if (realDataLength < 1) {
      throw new BadMessageException(); /* Bad message padding */
    }

    /* first byte of data is type */
    var type = data[0] & 0xFF;

    switch (type) {
      case TextMessage.TYPE_CODE:
        /* Text message */
        if (realDataLength < 2) {
          throw new BadMessageException();
        }

        return new TextMessage(new String(data, 1, realDataLength - 1, StandardCharsets.UTF_8));

      case DeliveryReceipt.TYPE_CODE:
        /* Delivery receipt */
        if (realDataLength < MessageId.MESSAGE_ID_LEN + 2
            || (realDataLength - 2) % MessageId.MESSAGE_ID_LEN != 0) {
          throw new BadMessageException();
        }

        var receiptType = DeliveryReceipt.Type.get(data[1] & 0xFF);
        if (receiptType == null) {
          throw new BadMessageException();
        }

        List<MessageId> messageIds = new LinkedList<>();

        var numMsgIds = (realDataLength - 2) / MessageId.MESSAGE_ID_LEN;
        for (var i = 0; i < numMsgIds; i++) {
          messageIds.add(new MessageId(data, 2 + i * MessageId.MESSAGE_ID_LEN));
        }

        return new DeliveryReceipt(receiptType, messageIds);

      case ImageMessage.TYPE_CODE:
        if (realDataLength != 1 + ThreemaMessage.BLOB_ID_LEN + 4 + NaCl.NONCEBYTES) {
          System.out.println(String.valueOf(realDataLength));
          System.out.println(String.valueOf(1 + ThreemaMessage.BLOB_ID_LEN + 4 + NaCl.NONCEBYTES));
          throw new BadMessageException();
        }
        var blobId = new byte[ThreemaMessage.BLOB_ID_LEN];
        System.arraycopy(data, 1, blobId, 0, ThreemaMessage.BLOB_ID_LEN);
        var size = EndianUtils.readSwappedInteger(data, 1 + ThreemaMessage.BLOB_ID_LEN);
        var fileNonce = new byte[NaCl.NONCEBYTES];
        System.arraycopy(data, 1 + 4 + ThreemaMessage.BLOB_ID_LEN, fileNonce, 0, nonce.length);

        return new ImageMessage(blobId, size, fileNonce);

      case FileMessage.TYPE_CODE:
        try {
          return FileMessage.fromString(new String(data, 1, realDataLength - 1, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          throw new BadMessageException();
        }

      default:
        throw new UnsupportedMessageTypeException();
    }
  }

  /**
   * Generate a new key pair.
   *
   * @param privateKey is used to return the generated private key (length must be
   *     NaCl.PRIVATEKEYBYTES)
   * @param publicKey is used to return the generated public key (length must be
   *     NaCl.PUBLICKEYBYTES)
   */
  public static void generateKeyPair(byte[] privateKey, byte[] publicKey) {
    if (publicKey.length != NaCl.PUBLICKEYBYTES || privateKey.length != NaCl.SECRETKEYBYTES) {
      throw new IllegalArgumentException("Wrong key length");
    }

    NaCl.genkeypair(publicKey, privateKey);
  }

  /**
   * Encrypt data using NaCl asymmetric ("box") encryption.
   *
   * @param data the data to be encrypted
   * @param privateKey is used to return the generated private key (length must be
   *     NaCl.PRIVATEKEYBYTES)
   * @param publicKey is used to return the generated public key (length must be
   *     NaCl.PUBLICKEYBYTES)
   */
  public static EncryptResult encrypt(byte[] data, byte[] privateKey, byte[] publicKey) {
    if (publicKey.length != NaCl.PUBLICKEYBYTES || privateKey.length != NaCl.SECRETKEYBYTES) {
      throw new IllegalArgumentException("Wrong key length");
    }

    var nonce = randomNonce();
    var naCl = new NaCl(privateKey, publicKey);
    return new EncryptResult(naCl.encrypt(data, nonce), null, nonce);
  }

  /**
   * Encrypt file data using NaCl symmetric encryption with a random key.
   *
   * @param data the file contents to be encrypted
   * @return the encryption result including the random key
   */
  public static EncryptResult encryptFileData(byte[] data) {
    // create random key
    var rnd = new SecureRandom();
    var encryptionKey = new byte[NaCl.SYMMKEYBYTES];
    rnd.nextBytes(encryptionKey);

    // encrypt file data in-place
    NaCl.symmetricEncryptDataInplace(data, encryptionKey, FILE_NONCE);

    return new EncryptResult(data, encryptionKey, FILE_NONCE);
  }

  /**
   * Encrypt file thumbnail data using NaCl symmetric encryption with a random key.
   *
   * @param data the file contents to be encrypted
   * @return the encryption result including the random key
   */
  public static EncryptResult encryptFileThumbnailData(byte[] data, byte[] encryptionKey) {
    // encrypt file data in-place
    NaCl.symmetricEncryptDataInplace(data, encryptionKey, FILE_THUMBNAIL_NONCE);

    return new EncryptResult(data, encryptionKey, FILE_THUMBNAIL_NONCE);
  }

  /**
   * Hashes an email address for identity lookup.
   *
   * @param email the email address
   * @return the raw hash
   */
  public static byte[] hashEmail(String email) {
    try {
      var emailMac = Mac.getInstance(HMAC_SHA256);
      emailMac.init(new SecretKeySpec(EMAIL_HMAC_KEY, HMAC_SHA256));
      var normalizedEmail = email.toLowerCase().trim();
      return emailMac.doFinal(normalizedEmail.getBytes("US-ASCII"));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Hashes a phone number for identity lookup.
   *
   * @param phoneNo the phone number
   * @return the raw hash
   */
  public static byte[] hashPhoneNo(String phoneNo) {
    try {
      var phoneMac = Mac.getInstance(HMAC_SHA256);
      phoneMac.init(new SecretKeySpec(PHONENO_HMAC_KEY, HMAC_SHA256));
      var normalizedPhoneNo = phoneNo.replaceAll("[^0-9]", "");
      return phoneMac.doFinal(normalizedPhoneNo.getBytes("US-ASCII"));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Generate a random nonce.
   *
   * @return random nonce
   */
  public static byte[] randomNonce() {
    var nonce = new byte[NaCl.NONCEBYTES];
    random.nextBytes(nonce);
    return nonce;
  }

  /**
   * Return the public key that corresponds with a given private key.
   *
   * @param privateKey The private key whose public key should be derived
   * @return The corresponding public key.
   */
  public static byte[] derivePublicKey(byte[] privateKey) {
    return NaCl.derivePublicKey(privateKey);
  }
}
