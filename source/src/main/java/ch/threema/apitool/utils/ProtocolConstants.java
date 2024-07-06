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

package ch.threema.apitool.utils;

public final class ProtocolConstants {
	// Taken from Android repo's `ProtocolDefines.java`

	/* object lengths */
	public static final int PUSH_FROM_LEN = 32;
	public static final int IDENTITY_LEN = 8;
	public static final int MESSAGE_ID_LEN = 8;
	public static final int BLOB_ID_LEN = 16;
	public static final int BLOB_KEY_LEN = 32;
	public static final int GROUP_ID_LEN = 8;
	public static final int GROUP_INVITE_TOKEN_LEN = 16;
	public static final int BALLOT_ID_LEN = 8;
	public static final int GROUP_JOIN_MESSAGE_LEN = 100;

	/* max message size */
	public static final int MAX_PKT_LEN = 8192;
	public static final int OVERHEAD_NACL_BOX = 16; // Excluding nonce
	public static final int OVERHEAD_PKT_HDR = 4;
	public static final int OVERHEAD_MSG_HDR = 88;
	public static final int OVERHEAD_BOX_HDR = 1;
	public static final int OVERHEAD_MAXPADDING = 255;
	public static final int MAX_MESSAGE_LEN = MAX_PKT_LEN - OVERHEAD_NACL_BOX * 2 // Both
	// app-to-server
	// and
	// end-to-end
					- OVERHEAD_PKT_HDR - OVERHEAD_MSG_HDR - OVERHEAD_BOX_HDR - OVERHEAD_MAXPADDING;
	public static final int MIN_MESSAGE_PADDED_LEN = 32;

	private ProtocolConstants() {

	}
}
