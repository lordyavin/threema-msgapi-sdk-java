/*
 * $Id$
 *
 * The MIT License (MIT)
 * Copyright (c) 2015 Threema GmbH
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
 */

package net.klesatschke.threema.api;

/** Encapsulates the 8-byte message IDs that Threema uses. */
public class MessageId {

  public static final int MESSAGE_ID_LEN = 8;

  private final byte[] messageId;

  public MessageId(byte[] messageId) {
    if (messageId.length != MESSAGE_ID_LEN) {
      throw new IllegalArgumentException("Bad message ID length");
    }

    this.messageId = messageId;
  }

  public MessageId(byte[] data, int offset) {
    if (offset + MESSAGE_ID_LEN > data.length) {
      throw new IllegalArgumentException("Bad message ID buffer length");
    }

    this.messageId = new byte[MESSAGE_ID_LEN];
    System.arraycopy(data, offset, this.messageId, 0, MESSAGE_ID_LEN);
  }

  public byte[] getMessageId() {
    return messageId;
  }

  @Override
  public String toString() {
    return DataUtils.byteArrayToHexString(messageId);
  }
}