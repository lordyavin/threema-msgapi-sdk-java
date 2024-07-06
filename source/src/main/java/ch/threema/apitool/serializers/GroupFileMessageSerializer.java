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

package ch.threema.apitool.serializers;

import ch.threema.apitool.exceptions.BadMessageException;
import ch.threema.apitool.messages.GroupFileMessage;
import ch.threema.apitool.types.FileRenderingType;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.utils.DataUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2022-09-12T15:53:56.786299819+00:00")
public class GroupFileMessageSerializer implements CustomMessageSerializer {

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

	public static GroupFileMessage deserialize(byte[] data, int realDataLength)
					throws BadMessageException {
		var jsonData = GroupFileMessageSerializer.extractJson(data, realDataLength);
		try {
			JSONObject o = new JSONObject(new String(jsonData));

			byte[] blobId = DataUtils.hexStringToByteArray(o.getString(KEY_BLOB_ID));
			byte[] thumbnailBlobId =
							DataUtils.hexStringToByteArray(o.optString(KEY_THUMBNAIL_BLOB_ID, ""));
			String thumbnailMediaType = o.optString(KEY_THUMBNAIL_MEDIA_TYPE, "");
			byte[] encryptionKey = DataUtils.hexStringToByteArray(o.getString(KEY_ENCRYPTION_KEY));
			String mimeType = o.getString(KEY_MIME_TYPE);
			String fileName = o.optString(KEY_FILE_NAME, "unnamed");
			int size = o.getInt(KEY_SIZE);
			String caption = o.optString(KEY_CAPTION, "");
			FileRenderingType renderingType =
							FileRenderingType.valueOf(o.optInt(KEY_RENDERING_TYPE, 0));
			String correlationId = o.optString(KEY_CORRELATION_ID, "");
			Map<String, Object> metadata = null;
			if (o.optJSONObject(KEY_METADATA) != null)
				metadata = o.optJSONObject(KEY_METADATA).toMap();

			return new GroupFileMessage(extractGroupId(data), blobId, thumbnailBlobId,
							thumbnailMediaType, encryptionKey, mimeType, fileName, size, caption,
							renderingType, correlationId, metadata);
		} catch (JSONException e) {
			throw new BadMessageException();
		}
	}

	public static GroupId extractGroupId(byte[] data) {
		return new GroupId(
						Arrays.copyOfRange(data, 1 + GroupId.CREATOR_ID_LEN,
										1 + GroupId.CREATOR_ID_LEN + GroupId.GROUP_ID_LEN),
						Arrays.copyOfRange(data, 1, 1 + GroupId.CREATOR_ID_LEN));
	}

	public static byte[] extractJson(byte[] data, int realDataLength) {
		return new String(data, GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN + 1,
						realDataLength - (GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN + 1),
						StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
	}

	public static void validate(byte[] data, int realDataLength) throws BadMessageException {
		// true
	}
}
