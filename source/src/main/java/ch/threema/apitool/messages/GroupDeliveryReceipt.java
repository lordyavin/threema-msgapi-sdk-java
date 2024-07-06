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
import ch.threema.apitool.serializers.GroupDeliveryReceiptSerializer;

import static ch.threema.apitool.utils.StringUtils.toIndentedString;

/**
 * A group delivery receipt message that can be sent/received with end-to-end encryption via
 * Threema. Each delivery receipt message confirms the receipt of one or multiple group messages.
 */
@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2024-03-15T14:13:28.154411894+00:00")
public class GroupDeliveryReceipt extends ThreemaGroupMessage {
	public static final int TYPE_CODE = 0x81;


	private final GroupId groupId;
	private final DeliveryReceipt.Type receiptType;
	private final List<MessageId> ackedMessageIds;

	public GroupDeliveryReceipt(GroupId groupId, DeliveryReceipt.Type receiptType,
					List<MessageId> ackedMessageIds) {
		super(groupId);
		this.groupId = groupId;
		this.receiptType = receiptType;
		this.ackedMessageIds = ackedMessageIds;
	}

	/**
	 * The group identifier
	 *
	 * @return groupId
	 **/
	public GroupId getGroupId() {
		return groupId;
	}

	/**
	 * The message receipt type
	 *
	 * @return receiptType
	 **/
	public DeliveryReceipt.Type getReceiptType() {
		return receiptType;
	}

	/**
	 * The acked message ids
	 *
	 * @return ackedMessageIds
	 **/
	public List<MessageId> getAckedMessageIds() {
		return ackedMessageIds;
	}



	@Override
	public int getTypeCode() {
		return TYPE_CODE;
	}

	/**
	 * A delivery receipt type. The following types are defined:
	 *
	 * RECEIVED: the message has been received and decrypted on the recipient's device
	 * READ: the message has been shown to the user in the chat view (note that this status can be
	 * disabled)
	 * USER_ACK: the user has explicitly acknowledged the message (usually by
	 * long-pressing it and choosing the "acknowledge" option)
	 * USER_DEC: the user has explicitly declined the message
	 */
	public enum Type {
		RECEIVED(1), READ(2), USER_ACK(3), USER_DEC(4);

		private final int code;

		Type(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}

		public static Type get(int code) {
			for (Type t : values()) {
				if (t.code == code) {
					return t;
				}
			}
			return null;
		}
	}

	@Override
	public byte[] getData() throws BadMessageException {

		return GroupDeliveryReceiptSerializer.serialize(groupId, receiptType, ackedMessageIds);
	}

	public static GroupDeliveryReceipt fromString(byte[] data, int realDataLength)
					throws BadMessageException {
		GroupDeliveryReceiptSerializer.validate(data, realDataLength);
		return GroupDeliveryReceiptSerializer.deserialize(data, realDataLength);

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
		return Objects.hash(groupId, receiptType, Arrays.hashCode(ackedMessageIds.toArray()),
						super.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class GroupDeliveryReceipt {\n");
		sb.append("    receiptType: ").append(toIndentedString(getReceiptType())).append("\n");
		sb.append("    ackedMessageIds: ").append(toIndentedString(getAckedMessageIds()))
						.append("\n");

		sb.append("    ").append(toIndentedString(super.toString())).append("\n");
		sb.append("}");
		return sb.toString();
	}
}
