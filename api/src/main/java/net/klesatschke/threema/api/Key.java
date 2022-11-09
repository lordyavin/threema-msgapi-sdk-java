/*
 * $Id$
 *
 * The MIT License (MIT)
 * Copyright (c) 2015 Threema GmbH
 * Copyright (c) 2022 Kai Klesatschke
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

import lombok.Value;
import net.klesatschke.threema.api.exceptions.InvalidKeyException;

/** Encapsulates an asymmetric key, either public or private. */
@Value
public class Key {
  public static final String SEPARATOR = ":";

  public enum KeyType {
    PRIVATE,
    PUBLIC;
  }

  private final KeyType type;
  private final byte[] key;

  /**
   * Decodes and validates an encoded key. Encoded key format: type:hex_key
   *
   * @param encodedKey an encoded key
   * @throws ch.threema.apitool.exceptions.InvalidKeyException
   */
  public static Key decodeKey(String encodedKey) throws InvalidKeyException {
    // Split key and check length
    var keyArray = encodedKey.split(Key.SEPARATOR);
    if (keyArray.length != 2) {
      throw new InvalidKeyException("Does not contain a valid key format");
    }

    // Unpack key
    var keyType = KeyType.valueOf(keyArray[0].toUpperCase());
    var keyContent = keyArray[1];

    // Is this a valid hex key?
    if (!keyContent.matches("[0-9a-fA-F]{64}")) {
      throw new InvalidKeyException("Does not contain a valid key");
    }

    return new Key(keyType, DataUtils.hexStringToByteArray(keyContent));
  }

  /**
   * Decodes and validates an encoded key. Encoded key format: type:hex_key
   *
   * @param encodedKey an encoded key
   * @param expectedKeyType the expected type of the key
   * @throws InvalidKeyException
   */
  public static Key decodeKey(String encodedKey, KeyType expectedKeyType)
      throws InvalidKeyException {
    var key = decodeKey(encodedKey);

    // Check key type
    if (!key.type.equals(expectedKeyType)) {
      throw new InvalidKeyException("Expected key type: " + expectedKeyType + ", got: " + key.type);
    }

    return key;
  }

  /**
   * Encodes a key.
   *
   * @return an encoded key
   */
  public String encode() {
    return this.type.toString().toLowerCase()
        + Key.SEPARATOR
        + DataUtils.byteArrayToHexString(this.key);
  }
}
