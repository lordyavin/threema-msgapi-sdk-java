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

package net.klesatschke.threema.api.messages;

import java.io.UnsupportedEncodingException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.klesatschke.threema.api.DataUtils;
import net.klesatschke.threema.api.exceptions.BadMessageException;

/** A file message that can be sent/received with end-to-end encryption via Threema. */
public class FileMessage extends ThreemaMessage {
  private static final String KEY_BLOB_ID = "b";
  private static final String KEY_THUMBNAIL_BLOB_ID = "t";
  private static final String KEY_ENCRYPTION_KEY = "k";
  private static final String KEY_MIME_TYPE = "m";
  private static final String KEY_FILE_NAME = "n";
  private static final String KEY_FILE_SIZE = "s";
  private static final String KEY_TYPE = "i";

  public static final int TYPE_CODE = 0x17;

  private final byte[] blobId;
  private final byte[] encryptionKey;
  private final String mimeType;
  private final String fileName;
  private final int fileSize;
  private final byte[] thumbnailBlobId;

  public FileMessage(
      byte[] blobId,
      byte[] encryptionKey,
      String mimeType,
      String fileName,
      int fileSize,
      byte[] thumbnailBlobId) {
    this.blobId = blobId;
    this.encryptionKey = encryptionKey;
    this.mimeType = mimeType;
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.thumbnailBlobId = thumbnailBlobId;
  }

  public byte[] getBlobId() {
    return this.blobId;
  }

  public byte[] getEncryptionKey() {
    return this.encryptionKey;
  }

  public String getMimeType() {
    return this.mimeType;
  }

  public String getFileName() {
    return this.fileName;
  }

  public int getFileSize() {
    return this.fileSize;
  }

  public byte[] getThumbnailBlobId() {
    return this.thumbnailBlobId;
  }

  @Override
  public int getTypeCode() {
    return TYPE_CODE;
  }

  @Override
  public String toString() {
    return "file message " + this.fileName;
  }

  @Override
  public byte[] getData() throws BadMessageException {
    var o = new JsonObject();
    try {
      o.addProperty(KEY_BLOB_ID, DataUtils.byteArrayToHexString(this.blobId));
      o.addProperty(
          KEY_THUMBNAIL_BLOB_ID,
          this.thumbnailBlobId != null
              ? DataUtils.byteArrayToHexString(this.thumbnailBlobId)
              : null);
      o.addProperty(KEY_ENCRYPTION_KEY, DataUtils.byteArrayToHexString(this.encryptionKey));
      o.addProperty(KEY_MIME_TYPE, this.mimeType);
      o.addProperty(KEY_FILE_NAME, this.fileName);
      o.addProperty(KEY_FILE_SIZE, this.fileSize);
      o.addProperty(KEY_TYPE, 0);
    } catch (Exception e) {
      throw new BadMessageException();
    }

    try {
      return o.toString().getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  public static FileMessage fromString(String json) throws BadMessageException {
    try {
      var o = new Gson().fromJson(json, JsonObject.class);
      byte[] encryptionKey =
          DataUtils.hexStringToByteArray(o.get(KEY_ENCRYPTION_KEY).getAsString());
      var mimeType = o.get(KEY_MIME_TYPE).getAsString();
      var fileSize = o.get(KEY_FILE_SIZE).getAsInt();
      byte[] blobId = DataUtils.hexStringToByteArray(o.get(KEY_BLOB_ID).getAsString());

      String fileName;
      byte[] thumbnailBlobId = null;

      // optional field
      if (o.has(KEY_THUMBNAIL_BLOB_ID)) {
        thumbnailBlobId =
            DataUtils.hexStringToByteArray(o.get(KEY_THUMBNAIL_BLOB_ID).getAsString());
      }

      if (o.has(KEY_FILE_NAME)) {
        fileName = o.get(KEY_FILE_NAME).getAsString();
      } else {
        fileName = "unnamed";
      }

      return new FileMessage(blobId, encryptionKey, mimeType, fileName, fileSize, thumbnailBlobId);
    } catch (JsonSyntaxException e) {
      throw new BadMessageException();
    }
  }
}
