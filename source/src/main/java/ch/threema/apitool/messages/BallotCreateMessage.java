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
import ch.threema.apitool.serializers.BallotCreateMessageSerializer;

import static ch.threema.apitool.utils.StringUtils.toIndentedString;

/**
 * A poll create message that can be sent/received with end-to-end encryption via Threema.
 */
@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2024-03-15T14:00:04.180868814+00:00")
public class BallotCreateMessage extends ThreemaMessage {
	public static final int TYPE_CODE = 0x15;

	private final static String KEY_DESCRIPTION = "d";
	private final static String KEY_STATE = "s";
	private final static String KEY_VOTING_MODE = "a";
	private final static String KEY_RESULTS_DISCLOSURE_TYPE = "t";
	private final static String KEY_ORDER = "o";
	private final static String KEY_DISPLAY_MODE = "u";
	private final static String KEY_CHOICES = "c";
	private final static String KEY_PARTICIPANTS = "p";

	private final byte[] ballotId;
	private final String description;
	private final State state;
	private final VotingMode votingMode;
	private final ResultsDisclosureType resultsDisclosureType;
	private final int order;
	private final DisplayMode displayMode;
	private final List<BallotChoice> choices;
	private final List<String> participants;

	public BallotCreateMessage(byte[] ballotId, String description, State state,
					VotingMode votingMode, ResultsDisclosureType resultsDisclosureType, int order,
					DisplayMode displayMode, List<BallotChoice> choices,
					List<String> participants) {
		this.ballotId = ballotId;
		this.description = description;
		this.state = state;
		this.votingMode = votingMode;
		this.resultsDisclosureType = resultsDisclosureType;
		this.order = order;
		this.displayMode = displayMode;
		this.choices = choices;
		this.participants = participants;
	}

	/**
	 * The poll ballot identifier
	 *
	 * @return ballotId
	 **/
	public byte[] getBallotId() {
		return ballotId;
	}


	/**
	 * The group poll description
	 *
	 * @return description
	 **/
	public String getDescription() {
		return description;
	}


	/**
	 * The group poll state
	 *
	 * @return state
	 **/
	public State getState() {
		return state;
	}


	/**
	 * The voting mode
	 *
	 * @return votingMode
	 **/
	public VotingMode getVotingMode() {
		return votingMode;
	}


	/**
	 * The poll results disclosure type
	 *
	 * @return resultsDisclosureType
	 **/
	public ResultsDisclosureType getResultsDisclosureType() {
		return resultsDisclosureType;
	}


	/**
	 * The poll results order
	 *
	 * @return order
	 * @deprecated
	 **/
	@Deprecated
	public int getOrder() {
		return order;
	}


	/**
	 * The display mode
	 *
	 * @return displayMode
	 **/
	public DisplayMode getDisplayMode() {
		return displayMode;
	}


	/**
	 * The available vote choices
	 *
	 * @return choices
	 **/
	public List<BallotChoice> getChoices() {
		return choices;
	}


	/**
	 * The poll participants
	 *
	 * @return participants
	 **/
	public List<String> getParticipants() {
		return participants;
	}



	@Override
	public int getTypeCode() {
		return TYPE_CODE;
	}

	@Override
	public byte[] getData() throws BadMessageException {
		JSONObject o = new JSONObject();
		try {

			o.put(KEY_DESCRIPTION, this.description);
			o.put(KEY_STATE, this.state.getValue());
			o.put(KEY_VOTING_MODE, this.votingMode.getValue());
			o.put(KEY_RESULTS_DISCLOSURE_TYPE, this.resultsDisclosureType.getValue());
			o.put(KEY_ORDER, this.order);
			if (this.displayMode != null)
				o.put(KEY_DISPLAY_MODE, this.displayMode.getValue());
			JSONArray choicesO = new JSONArray();
			for (var choicesItem : this.choices) {
				choicesO.put(new JSONObject(new String(choicesItem.getData())));
			}
			o.put(KEY_CHOICES, choicesO);
			if (this.participants != null)
				o.put(KEY_PARTICIPANTS, this.participants);
		} catch (Exception e) {
			throw new BadMessageException();
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bos.write(ballotId);
			if (o.length() > 0) {
				bos.write(o.toString().getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return bos.toByteArray();
	}

	public static BallotCreateMessage fromString(byte[] data, int realDataLength)
					throws BadMessageException {
		BallotCreateMessageSerializer.validate(data, realDataLength);
		return BallotCreateMessageSerializer.deserialize(data, realDataLength);

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
		return Objects.hash(Arrays.hashCode(ballotId), description, state, votingMode,
						resultsDisclosureType, order, displayMode,
						Arrays.hashCode(choices.toArray()), Arrays.hashCode(participants.toArray()),
						super.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class BallotCreateMessage {\n");
		sb.append("    ballotId: ")
						.append(toIndentedString(DataUtils.byteArrayToHexString(getBallotId())))
						.append("\n");
		sb.append("    description: ").append(toIndentedString(getDescription())).append("\n");
		sb.append("    state: ").append(toIndentedString(getState())).append("\n");
		sb.append("    votingMode: ").append(toIndentedString(getVotingMode())).append("\n");
		sb.append("    resultsDisclosureType: ")
						.append(toIndentedString(getResultsDisclosureType())).append("\n");
		sb.append("    order: ").append(toIndentedString(getOrder())).append("\n");
		sb.append("    displayMode: ").append(toIndentedString(getDisplayMode())).append("\n");
		sb.append("    choices: ").append(toIndentedString(getChoices())).append("\n");
		sb.append("    participants: ").append(toIndentedString(getParticipants())).append("\n");

		sb.append("    ").append(toIndentedString(super.toString())).append("\n");
		sb.append("}");
		return sb.toString();
	}
}
