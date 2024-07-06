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

import ch.threema.apitool.serializers.VoteChoiceSerializer;

import static ch.threema.apitool.utils.StringUtils.toIndentedString;

/**
 * A vote choice item that is part of a vote message.
 */
@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2022-09-12T15:53:56.793026168+00:00")
public class VoteChoice {



	private final int ballotId;
	private final boolean selected;

	public VoteChoice(int ballotId, boolean selected) {
		this.ballotId = ballotId;
		this.selected = selected;
	}

	/**
	 * The ballot identifier
	 *
	 * @return ballotId
	 **/
	public int getBallotId() {
		return ballotId;
	}

	/**
	 * The vote choice name
	 *
	 * @return selected
	 **/
	public boolean getSelected() {
		return selected;
	}



	public byte[] getData() throws BadMessageException {

		return VoteChoiceSerializer.serialize(ballotId, selected);
	}

	public static VoteChoice fromString(byte[] data, int realDataLength)
					throws BadMessageException {

		throw new UnsupportedOperationException("VoteChoice cannot be received from a Gateway ID!");
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
		return Objects.hash(ballotId, selected);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class VoteChoice {\n");
		sb.append("    ballotId: ").append(toIndentedString(getBallotId())).append("\n");
		sb.append("    selected: ").append(toIndentedString(getSelected())).append("\n");
		sb.append("}");
		return sb.toString();
	}
}
