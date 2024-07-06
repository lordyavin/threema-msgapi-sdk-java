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

import ch.threema.apitool.utils.ProtocolConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import ch.threema.apitool.exceptions.BadMessageException;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.messages.GroupSetPhoto;

@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2022-09-12T15:53:56.786299819+00:00")
public class GroupSetPhotoSerializer implements CustomMessageSerializer {

	public static GroupSetPhoto deserialize(byte[] data, int realDataLength)
					throws BadMessageException {
		var groupId = extractGroupId(data);
		byte[] blobId = new byte[16];
		System.arraycopy(data, GroupId.GROUP_ID_LEN + 1, blobId, 0, 16);
		ByteBuffer dataBuf = ByteBuffer.wrap(data);
		dataBuf.order(ByteOrder.LITTLE_ENDIAN);
		dataBuf.rewind();
		int size = dataBuf.getInt(GroupId.GROUP_ID_LEN + 17);
		byte[] encryptionKey = new byte[32];
		System.arraycopy(data, GroupId.GROUP_ID_LEN + 17 + Integer.BYTES, encryptionKey, 0, 32);

		return new GroupSetPhoto(groupId, blobId, size, encryptionKey);
	}

	public static GroupId extractGroupId(byte[] data) {
		return new GroupId(Arrays.copyOfRange(data, 1, 1 + GroupId.GROUP_ID_LEN));
	}

	public static byte[] extractJson(byte[] data, int realDataLength) {
		return new byte[0];
	}

	public static void validate(byte[] data, int realDataLength) throws BadMessageException {
		if (realDataLength != (1 + ProtocolConstants.GROUP_ID_LEN + ProtocolConstants.BLOB_ID_LEN
						+ 4 + ProtocolConstants.BLOB_KEY_LEN)) {
			throw new BadMessageException(
							"Bad length (" + realDataLength + ") for group set photo message");
		}
	}
}
