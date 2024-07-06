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

import ch.threema.apitool.types.voting.*;
import ch.threema.apitool.utils.ProtocolConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import ch.threema.apitool.exceptions.BadMessageException;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.messages.GroupBallotCreateMessage;

@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2022-09-12T15:53:56.786299819+00:00")
public class GroupBallotCreateMessageSerializer implements CustomMessageSerializer {

	private final static String KEY_DESCRIPTION = "d";
	private final static String KEY_STATE = "s";
	private final static String KEY_VOTING_MODE = "a";
	private final static String KEY_RESULTS_DISCLOSURE_TYPE = "t";
	private final static String KEY_ORDER = "o";
	private final static String KEY_DISPLAY_MODE = "u";
	private final static String KEY_CHOICES = "c";
	private final static String KEY_PARTICIPANTS = "p";

	public static GroupBallotCreateMessage deserialize(byte[] data, int realDataLength)
					throws BadMessageException {
		var groupId = extractGroupId(data);
		var ballotId = Arrays.copyOfRange(data, GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN + 1,
						GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN + 1
										+ ProtocolConstants.BALLOT_ID_LEN);
		var json = extractJson(data, realDataLength);
		try {
			JSONObject o = new JSONObject(new String(json));

			String description = o.getString(KEY_DESCRIPTION);
			State state = State.valueOf(o.getInt(KEY_STATE));
			VotingMode votingMode = VotingMode.valueOf(o.getInt(KEY_VOTING_MODE));
			ResultsDisclosureType resultsDisclosureType =
							ResultsDisclosureType.valueOf(o.getInt(KEY_RESULTS_DISCLOSURE_TYPE));
			int order = o.getInt(KEY_ORDER);
			DisplayMode displayMode;
			if (o.has(KEY_DISPLAY_MODE)) {
				displayMode = DisplayMode.valueOf(o.getInt(KEY_DISPLAY_MODE));
			} else {
				displayMode = DisplayMode.LIST;
			}
			var choicesJ = o.getJSONArray(KEY_CHOICES);
			var choices = new ArrayList<BallotChoice>();
			if (choicesJ != null) {
				for (int i = 0; i < choicesJ.length(); i++) {
					choices.add(BallotChoice.fromString(choicesJ.getJSONObject(i).toString()
									.getBytes(StandardCharsets.UTF_8), realDataLength));
				}
			}
			var participantsJ = o.getJSONArray(KEY_PARTICIPANTS);
			var participants = new ArrayList<String>();
			if (participantsJ != null) {
				for (int i = 0; i < participantsJ.length(); i++) {
					participants.add(participantsJ.getString(i));
				}
			}

			return new GroupBallotCreateMessage(groupId, ballotId, description, state, votingMode,
							resultsDisclosureType, order, displayMode, choices, participants);
		} catch (JSONException e) {
			throw new BadMessageException();
		}
	}

	public static GroupId extractGroupId(byte[] data) {
		return new GroupId(
						Arrays.copyOfRange(data, 1 + GroupId.CREATOR_ID_LEN,
										1 + GroupId.CREATOR_ID_LEN + GroupId.CREATOR_ID_LEN),
						Arrays.copyOfRange(data, 1, 1 + GroupId.CREATOR_ID_LEN));
	}

	public static byte[] extractJson(byte[] data, int realDataLength) {
		return new String(data, GroupId.GROUP_ID_LEN + 2 * GroupId.CREATOR_ID_LEN + 1,
						realDataLength - (GroupId.GROUP_ID_LEN + 2 * GroupId.CREATOR_ID_LEN + 1),
						StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
	}

	public static void validate(byte[] data, int realDataLength) throws BadMessageException {
		// true
	}
}
