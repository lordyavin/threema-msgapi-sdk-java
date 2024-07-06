/*
 *  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema Gateway Java SDK
 * This SDK allows for preparing, sending, and receiving Threema messages via Threema Gateway.
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

package ch.threema.apitool.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.EndianUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import ch.threema.apitool.utils.DataUtils;
import ch.threema.apitool.types.QuotePart;
import ch.threema.apitool.exceptions.BadMessageException;

import ch.threema.apitool.types.MessageId;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.types.voting.*;
import ch.threema.apitool.types.*;
import ch.threema.apitool.serializers.FileMessageSerializer;

import static ch.threema.apitool.utils.StringUtils.toIndentedString;

/**
 * A File Message
 */
@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2024-03-15T13:44:24.475245996+00:00")
public class FileMessage extends ThreemaMessage {
	public static final int TYPE_CODE = 0x17;

	private final static String KEY_BLOB_ID = "b";
	private final static String KEY_THUMBNAIL_BLOB_ID = "t";
	private final static String KEY_THUMBNAIL_MEDIA_TYPE = "p";
	private final static String KEY_ENCRYPTION_KEY = "k";
	private final static String KEY_MIME_TYPE = "m";
	private final static String KEY_FILE_NAME = "n";
	private final static String KEY_SIZE = "s";
	private final static String KEY_CAPTION = "d";
	private final static String KEY_RENDERING_TYPE = "j";
	private final static String KEY_CORRELATION_ID = "c";
	private final static String KEY_METADATA = "x";

	private final byte[] blobId;
	private final byte[] thumbnailBlobId;
	private final String thumbnailMediaType;
	private final byte[] encryptionKey;
	private final String mimeType;
	private final String fileName;
	private final int size;
	private final String caption;
	private final FileRenderingType renderingType;
	private final String correlationId;
	private final Map<String, Object> metadata;

	public FileMessage(byte[] blobId, byte[] thumbnailBlobId, String thumbnailMediaType,
					byte[] encryptionKey, String mimeType, String fileName, int size,
					String caption, FileRenderingType renderingType, String correlationId,
					Map<String, Object> metadata) {
		this.blobId = blobId;
		this.thumbnailBlobId = thumbnailBlobId;
		this.thumbnailMediaType = thumbnailMediaType;
		this.encryptionKey = encryptionKey;
		this.mimeType = mimeType;
		this.fileName = fileName;
		this.size = size;
		this.caption = caption;
		this.renderingType = renderingType;
		this.correlationId = correlationId;
		this.metadata = metadata;
	}

	/**
	 * The blob ID
	 *
	 * @return blobId
	 **/
	public byte[] getBlobId() {
		return blobId;
	}


	/**
	 * The thumbnail blob ID
	 *
	 * @return thumbnailBlobId
	 **/
	public byte[] getThumbnailBlobId() {
		return thumbnailBlobId;
	}


	/**
	 * The thumbnail media type
	 *
	 * @return thumbnailMediaType
	 **/
	public String getThumbnailMediaType() {
		return thumbnailMediaType;
	}


	/**
	 * The encryption key
	 *
	 * @return encryptionKey
	 **/
	public byte[] getEncryptionKey() {
		return encryptionKey;
	}


	/**
	 * The mime type
	 *
	 * @return mimeType
	 **/
	public String getMimeType() {
		return mimeType;
	}


	/**
	 * The filename
	 *
	 * @return fileName
	 **/
	public String getFilename() {
		return fileName;
	}


	/**
	 * The file size
	 *
	 * @return size
	 **/
	public int getSize() {
		return size;
	}


	/**
	 * The file caption
	 *
	 * @return caption
	 **/
	public String getCaption() {
		return caption;
	}


	/**
	 * The rendering type
	 *
	 * @return renderingType
	 **/
	public FileRenderingType getRenderingType() {
		return renderingType;
	}


	/**
	 * The correlation identifier
	 *
	 * @return correlationId
	 **/
	public String getCorrelationId() {
		return correlationId;
	}


	/**
	 * The metadata
	 *
	 * @return metadata
	 **/
	public Map<String, Object> getMetadata() {
		return metadata;
	}



	@Override
	public int getTypeCode() {
		return TYPE_CODE;
	}

	@Override
	public byte[] getData() throws BadMessageException {
		JSONObject o = new JSONObject();
		try {

			o.put(KEY_BLOB_ID, DataUtils.byteArrayToHexString(this.blobId));
			if (this.thumbnailBlobId != null)
				o.put(KEY_THUMBNAIL_BLOB_ID, DataUtils.byteArrayToHexString(this.thumbnailBlobId));
			if (this.thumbnailMediaType != null)
				o.put(KEY_THUMBNAIL_MEDIA_TYPE, this.thumbnailMediaType);
			o.put(KEY_ENCRYPTION_KEY, DataUtils.byteArrayToHexString(this.encryptionKey));
			o.put(KEY_MIME_TYPE, this.mimeType);
			if (this.fileName != null)
				o.put(KEY_FILE_NAME, this.fileName);
			o.put(KEY_SIZE, this.size);
			if (this.caption != null)
				o.put(KEY_CAPTION, this.caption);
			if (this.renderingType != null)
				o.put(KEY_RENDERING_TYPE, this.renderingType.getValue());
			if (this.correlationId != null)
				o.put(KEY_CORRELATION_ID, this.correlationId);
			if (this.metadata != null)
				o.put(KEY_METADATA, this.metadata);
		} catch (Exception e) {
			throw new BadMessageException();
		}

		return o.toString().getBytes(StandardCharsets.UTF_8);

	}

	public static FileMessage fromString(byte[] data, int realDataLength)
					throws BadMessageException {
		FileMessageSerializer.validate(data, realDataLength);
		return FileMessageSerializer.deserialize(data, realDataLength);

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return Objects.hash(Arrays.hashCode(blobId), Arrays.hashCode(thumbnailBlobId),
						thumbnailMediaType, Arrays.hashCode(encryptionKey), mimeType, fileName,
						size, caption, renderingType, correlationId, metadata, super.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class FileMessage {\n");
		sb.append("    blobId: ")
						.append(toIndentedString(DataUtils.byteArrayToHexString(getBlobId())))
						.append("\n");
		sb.append("    thumbnailBlobId: ")
						.append(toIndentedString(
										DataUtils.byteArrayToHexString(getThumbnailBlobId())))
						.append("\n");
		sb.append("    thumbnailMediaType: ").append(toIndentedString(getThumbnailMediaType()))
						.append("\n");
		sb.append("    encryptionKey: ")
						.append(toIndentedString(
										DataUtils.byteArrayToHexString(getEncryptionKey())))
						.append("\n");
		sb.append("    mimeType: ").append(toIndentedString(getMimeType())).append("\n");
		sb.append("    fileName: ").append(toIndentedString(getFilename())).append("\n");
		sb.append("    size: ").append(toIndentedString(getSize())).append("\n");
		sb.append("    caption: ").append(toIndentedString(getCaption())).append("\n");
		sb.append("    renderingType: ").append(toIndentedString(getRenderingType())).append("\n");
		sb.append("    correlationId: ").append(toIndentedString(getCorrelationId())).append("\n");
		sb.append("    metadata: ").append(toIndentedString(getMetadata())).append("\n");

		sb.append("    ").append(toIndentedString(super.toString())).append("\n");
		sb.append("}");
		return sb.toString();
	}
}
