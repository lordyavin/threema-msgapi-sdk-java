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
import ch.threema.apitool.serializers.GroupTextMessageSerializer;

import static ch.threema.apitool.utils.StringUtils.toIndentedString;

/**
 * A group text message that can be sent/received with end-to-end encryption via Threema.
 */
@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2024-03-15T13:44:24.497104708+00:00")
public class GroupTextMessage extends ThreemaGroupMessage {
	public static final int TYPE_CODE = 0x41;


	private final GroupId groupId;
	private final String text;

	public GroupTextMessage(GroupId groupId, String text) {
		super(groupId);
		this.groupId = groupId;
		this.text = text;
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
	 * The message text
	 *
	 * @return text
	 **/
	public String getText() {
		return text;
	}

	/**
	 * Whether or not the message is a quote
	 *
	 * @return Whether or not the message is a quote
	 */
	public boolean IsQuote() {
		var pattern = Pattern.compile(DataUtils.QUOTE_PATTERN, Pattern.DOTALL);
		return pattern.matcher(text).matches();
	}

	/**
	 * Gets quoted message id if the message is a quote
	 *
	 * @return Quoted message id
	 */
	public String getQuotedMessageId() {
		return DataUtils.extractQuote(text, QuotePart.QUOTED_MESSAGE_ID);
	}

	/**
	 * Gets quote text if the message is a quote
	 *
	 * @return Quote text
	 */
	public String getQuoteText() {
		return DataUtils.extractQuote(text, QuotePart.QUOTE_TEXT);
	}


	@Override
	public int getTypeCode() {
		return TYPE_CODE;
	}

	@Override
	public byte[] getData() throws BadMessageException {

		return text.getBytes(StandardCharsets.UTF_8);

	}

	public static GroupTextMessage fromString(byte[] data, int realDataLength)
					throws BadMessageException {
		GroupTextMessageSerializer.validate(data, realDataLength);
		return GroupTextMessageSerializer.deserialize(data, realDataLength);

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
		return Objects.hash(groupId, text, super.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class GroupTextMessage {\n");
		sb.append("    text: ").append(toIndentedString(getText())).append("\n");

		sb.append("    ").append(toIndentedString(super.toString())).append("\n");
		sb.append("}");
		return sb.toString();
	}
}
