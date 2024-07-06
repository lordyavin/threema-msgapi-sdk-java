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

package ch.threema.apitool.types;

import ch.threema.apitool.utils.DataUtils;

import java.nio.charset.StandardCharsets;

/**
 * Encapsulates the 8-byte message IDs that Threema uses.
 */
public class GroupId {

	public static final int GROUP_ID_LEN = 8;
	public static final int CREATOR_ID_LEN = 8;

	private final byte[] groupCreator;
	private final byte[] groupId;

	public GroupId(String groupId) {
		this(groupId.getBytes(StandardCharsets.UTF_8), null);
	}

	public GroupId(String groupId, String groupCreator) {
		this(groupId.getBytes(StandardCharsets.UTF_8),
						groupCreator.getBytes(StandardCharsets.UTF_8));
	}

	public GroupId(byte[] groupId) {
		this(groupId, null);
	}

	public GroupId(byte[] groupId, byte[] groupCreator) {
		if (groupId == null || groupId.length > 0 && groupId.length != GROUP_ID_LEN)
			throw new IllegalArgumentException("Bad group ID length");
		if (groupCreator != null && groupCreator.length > 0
						&& groupCreator.length != CREATOR_ID_LEN)
			throw new IllegalArgumentException("Bad creator ID length");

		this.groupCreator = groupCreator;
		this.groupId = groupId;
	}

	public byte[] getGroupCreator() {
		return groupCreator;
	}

	public byte[] getGroupId() {
		return groupId;
	}

	public long toLong() {
		var longVal = new byte[GROUP_ID_LEN + CREATOR_ID_LEN];
		System.arraycopy(groupCreator, 0, longVal, 0, CREATOR_ID_LEN);
		System.arraycopy(groupId, 0, longVal, CREATOR_ID_LEN, GROUP_ID_LEN);
		return DataUtils.byteArrayToLongBigEndian(longVal);
	}

	@Override
	public String toString() {
		return "GroupId {\n    groupCreator: " + new String(groupCreator) + "\n    groupId: "
						+ new String(groupId) + "\n}";
	}
}
