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

package ch.threema.apitool;

import ch.threema.apitool.exceptions.InvalidHexException;
import ch.threema.apitool.types.QuotePart;
import ch.threema.apitool.utils.DataUtils;
import org.junit.Test;

public class DataUtilsTest {
	@Test
	public void testHexStringToByteArraySuccess() throws InvalidHexException {
		final byte[] decoded = DataUtils.hexStringToByteArray("0011AAff");
		Assert.assertEquals(new byte[] {(byte) 0x00, (byte) 0x11, (byte) 0xaa, (byte) 0xff},
						decoded);
	}

	@Test
	public void testHexStringToByteArrayStripWhitespace() throws InvalidHexException {
		final byte[] decoded = DataUtils.hexStringToByteArray("0011 	\n\rAAff");
		Assert.assertEquals(new byte[] {(byte) 0x00, (byte) 0x11, (byte) 0xaa, (byte) 0xff},
						decoded);
	}

	@Test(expected = InvalidHexException.class)
	public void testHexStringToByteArrayRejectOddLength() {
		DataUtils.hexStringToByteArray("00112");
	}

	@Test(expected = InvalidHexException.class)
	public void testHexStringToByteArrayRejectInvalid() {
		DataUtils.hexStringToByteArray("0011aaffgg");
	}

	@Test
	public void testExtractQuote() {
		String msgText = "> quote #f053a613ff24aeb8\n\nTest";
		String quotedMessageId = DataUtils.extractQuote(msgText, QuotePart.QUOTED_MESSAGE_ID);
		String quoteText = DataUtils.extractQuote(msgText, QuotePart.QUOTE_TEXT);

		Assert.assertEquals("f053a613ff24aeb8", quotedMessageId);
		Assert.assertEquals("Test", quoteText);
	}
}
