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

import ch.threema.apitool.types.voting.VoteChoice;
import ch.threema.apitool.utils.ProtocolConstants;
import org.json.JSONException;
import org.json.JSONArray;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.threema.apitool.exceptions.BadMessageException;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.messages.GroupBallotVoteMessage;

@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2022-09-12T15:53:56.786299819+00:00")
public class GroupBallotVoteMessageSerializer implements CustomMessageSerializer {

	public static byte[] serialize(GroupId groupId, byte[] creator, byte[] ballotId,
					List<VoteChoice> votes) throws BadMessageException {
		JSONArray a = new JSONArray();
		try {
			for (var item : votes) {
				a.put(new JSONArray(new String(item.getData())));
			}
		} catch (Exception e) {
			throw new BadMessageException();
		}
		byte[] preamble = new byte[GroupId.GROUP_ID_LEN + GroupId.CREATOR_ID_LEN];
		System.arraycopy(creator, 0, preamble, 0, GroupId.CREATOR_ID_LEN);
		System.arraycopy(ballotId, 0, preamble, GroupId.CREATOR_ID_LEN,
						ProtocolConstants.BALLOT_ID_LEN);
		byte[] arrayBytes = a.toString().getBytes(StandardCharsets.UTF_8);
		var data = new byte[arrayBytes.length + preamble.length];
		System.arraycopy(preamble, 0, data, 0, preamble.length);
		System.arraycopy(arrayBytes, 0, data, preamble.length, arrayBytes.length);
		return data;
	}

	public static GroupBallotVoteMessage deserialize(byte[] data, int realDataLength)
					throws BadMessageException {
		var groupId = extractGroupId(data);
		var ballotId = Arrays.copyOfRange(data,
						GroupId.GROUP_ID_LEN + 2 * GroupId.CREATOR_ID_LEN + 1,
						GroupId.GROUP_ID_LEN + 2 * GroupId.CREATOR_ID_LEN + 1
										+ ProtocolConstants.BALLOT_ID_LEN);
		var json = extractJson(data, realDataLength);
		try {
			var a = new JSONArray(new String(json));

			var votes = new ArrayList<VoteChoice>();
			for (int i = 0; i < a.length(); i++) {
				votes.add(new VoteChoice(a.getJSONArray(i).getInt(0),
								a.getJSONArray(i).getInt(1) == 1));
			}

			return new GroupBallotVoteMessage(groupId, groupId.getGroupCreator(), ballotId, votes);
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
		return new String(data, GroupId.GROUP_ID_LEN + 2 * GroupId.CREATOR_ID_LEN + 9,
						realDataLength - (GroupId.GROUP_ID_LEN + 2 * GroupId.CREATOR_ID_LEN + 9),
						StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
	}

	public static void validate(byte[] data, int realDataLength) throws BadMessageException {
		// true
	}
}
