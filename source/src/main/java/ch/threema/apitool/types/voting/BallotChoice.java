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

package ch.threema.apitool.types.voting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Arrays;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.EndianUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import ch.threema.apitool.utils.DataUtils;
import ch.threema.apitool.exceptions.BadMessageException;

import java.util.List;
import java.util.ArrayList;
import ch.threema.apitool.types.MessageId;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.types.voting.*;
import ch.threema.apitool.types.*;
import ch.threema.apitool.serializers.BallotChoiceSerializer;

import static ch.threema.apitool.utils.StringUtils.toIndentedString;

/**
 * A poll choice item that is part of a poll message.
 */
@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2022-09-12T15:53:56.786299819+00:00")
public class BallotChoice {


	private final static String KEY_IDENTIFIER = "i";
	private final static String KEY_NAME = "n";
	private final static String KEY_ORDER = "o";
	private final static String KEY_RESULT = "r";
	private final static String KEY_TOTAL_VOTES = "t";

	private final Integer identifier;
	private final String name;
	private final int order;
	private final List<Integer> result;
	private final Integer totalVotes;

	public BallotChoice(Integer identifier, String name, int order, List<Integer> result,
					Integer totalVotes) {
		this.identifier = identifier;
		this.name = name;
		this.order = order;
		this.result = result;
		this.totalVotes = totalVotes;
	}

	/**
	 * The poll choice identifier
	 *
	 * @return identifier
	 **/
	public Integer getIdentifier() {
		return identifier;
	}


	/**
	 * The poll choice name
	 *
	 * @return name
	 **/
	public String getName() {
		return name;
	}


	/**
	 * The poll choice order number
	 *
	 * @return order
	 * @deprecated
	 **/
	@Deprecated
	public int getOrder() {
		return order;
	}


	/**
	 * The poll vote result index array
	 *
	 * @return result
	 **/
	public List<Integer> getResult() {
		return result;
	}


	/**
	 * The total poll votes
	 *
	 * @return totalVotes
	 **/
	public Integer getTotalVotes() {
		return totalVotes;
	}



	public byte[] getData() throws BadMessageException {
		JSONObject o = new JSONObject();
		try {

			o.put(KEY_IDENTIFIER, this.identifier);
			o.put(KEY_NAME, this.name);
			o.put(KEY_ORDER, this.order);
			o.put(KEY_RESULT, this.result);
			if (this.totalVotes != null)
				o.put(KEY_TOTAL_VOTES, this.totalVotes);
		} catch (Exception e) {
			throw new BadMessageException();
		}

		return o.toString().getBytes(StandardCharsets.UTF_8);

	}

	public static BallotChoice fromString(byte[] data, int realDataLength)
					throws BadMessageException {
		BallotChoiceSerializer.validate(data, realDataLength);
		var jsonData = BallotChoiceSerializer.extractJson(data, realDataLength);
		try {
			JSONObject o = new JSONObject(new String(jsonData));

			Integer identifier = o.getInt(KEY_IDENTIFIER);
			String name = o.getString(KEY_NAME);
			int order = o.getInt(KEY_ORDER);
			var resultJ = o.getJSONArray(KEY_RESULT);
			var result = new ArrayList<Integer>();
			if (resultJ != null) {
				for (int i = 0; i < resultJ.length(); i++) {
					result.add(resultJ.getInt(i));
				}
			}
			Integer totalVotes = o.optInt(KEY_TOTAL_VOTES, 0);

			return new BallotChoice(identifier, name, order, result, totalVotes);
		} catch (JSONException e) {
			throw new BadMessageException();
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier, name, order, Arrays.hashCode(result.toArray()), totalVotes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class BallotChoice {\n");
		sb.append("    identifier: ").append(toIndentedString(getIdentifier())).append("\n");
		sb.append("    name: ").append(toIndentedString(getName())).append("\n");
		sb.append("    order: ").append(toIndentedString(getOrder())).append("\n");
		sb.append("    result: ").append(toIndentedString(getResult())).append("\n");
		sb.append("    totalVotes: ").append(toIndentedString(getTotalVotes())).append("\n");
		sb.append("}");
		return sb.toString();
	}
}
