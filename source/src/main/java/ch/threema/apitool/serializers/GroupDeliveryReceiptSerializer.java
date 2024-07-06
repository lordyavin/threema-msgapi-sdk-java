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

import ch.threema.apitool.messages.DeliveryReceipt;
import ch.threema.apitool.types.MessageId;
import ch.threema.apitool.utils.ProtocolConstants;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.threema.apitool.exceptions.BadMessageException;
import ch.threema.apitool.utils.DataUtils;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.messages.GroupDeliveryReceipt;

@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2023-04-26T11:16:11.724535101+00:00")
public class GroupDeliveryReceiptSerializer implements CustomMessageSerializer {

	public static byte[] serialize(GroupId groupId, DeliveryReceipt.Type receiptType,
					List<MessageId> ackedMessageIds) {
		var bytes = new byte[GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN
						+ ackedMessageIds.size() * ProtocolConstants.MESSAGE_ID_LEN + 1];
		ByteBuffer dataBuf = ByteBuffer.wrap(bytes);
		dataBuf.put(groupId.getGroupCreator());
		dataBuf.put(groupId.getGroupId());
		dataBuf.order(ByteOrder.LITTLE_ENDIAN);
		int offset = 1;
		for (var msgId : ackedMessageIds) {
			System.arraycopy(msgId.getMessageId(), 0, bytes, offset,
							ProtocolConstants.MESSAGE_ID_LEN);
			offset += ProtocolConstants.MESSAGE_ID_LEN;
		}
		dataBuf.rewind();
		dataBuf.put((byte) receiptType.getCode());
		return dataBuf.array();
	}

	public static GroupDeliveryReceipt deserialize(byte[] data, int realDataLength)
					throws BadMessageException {
		var type = DeliveryReceipt.Type
						.get(data[GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN + 1] & 0xFF);
		int numMsgIds = ((realDataLength - GroupId.GROUP_ID_LEN - GroupId.CREATOR_ID_LEN - 2)
						/ MessageId.MESSAGE_ID_LEN);

		List<MessageId> messageIds = new ArrayList<>();
		for (int i = 0; i < numMsgIds; i++) {
			int offset = GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN + 2
							+ i * MessageId.MESSAGE_ID_LEN;
			messageIds.add(new MessageId(data, offset));
		}

		return new GroupDeliveryReceipt(extractGroupId(data), type, messageIds);
	}

	public static GroupId extractGroupId(byte[] data) {
		return new GroupId(
						Arrays.copyOfRange(data, 1 + GroupId.CREATOR_ID_LEN,
										1 + GroupId.CREATOR_ID_LEN + GroupId.GROUP_ID_LEN),
						Arrays.copyOfRange(data, 1, 1 + GroupId.CREATOR_ID_LEN));
	}

	public static byte[] extractJson(byte[] data, int realDataLength) {
		return new byte[0];
	}

	public static void validate(byte[] data, int realDataLength) throws BadMessageException {
		if (realDataLength < (GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN
						+ ProtocolConstants.MESSAGE_ID_LEN - 2)
						|| 0 != ((realDataLength - 2) % ProtocolConstants.MESSAGE_ID_LEN)) {
			throw new BadMessageException();
		}

		var receiptType = DeliveryReceipt.Type
						.get(data[GroupId.CREATOR_ID_LEN + GroupId.GROUP_ID_LEN + 1] & 0xFF);
		if (receiptType == null)
			throw new BadMessageException();
	}
}
