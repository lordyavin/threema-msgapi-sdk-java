/*
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

package ch.threema.apitool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import ch.threema.apitool.exceptions.InvalidKeyException;

class KeyTest {

  @Test
  void testDecodeWrongKey() {
    assertThatExceptionOfType(InvalidKeyException.class)
        .isThrownBy(() -> Key.decodeKey("imnotarealkey"));
  }

  @Test
  void testDecodeKeyPrivate() throws Exception {
    var key =
        Key.decodeKey("private:1234567890123456789012345678901234567890123456789012345678901234");

    assertThat(key).isNotNull();
    assertThat(key.type).isEqualTo(Key.KeyType.PRIVATE);
    assertThat(key.key)
        .isEqualTo(
            DataUtils.hexStringToByteArray(
                "1234567890123456789012345678901234567890123456789012345678901234"));
  }

  @Test
  void testDecodeKeyPublic() throws Exception {
    var key =
        Key.decodeKey("public:1234567890123456789012345678901234567890123456789012345678901234");
    assertThat(key).isNotNull();
    assertThat(key.type).isEqualTo(Key.KeyType.PUBLIC);
    assertThat(key.key)
        .isEqualTo(
            DataUtils.hexStringToByteArray(
                "1234567890123456789012345678901234567890123456789012345678901234"));
  }

  @Test
  void testEncodePrivate() throws Exception {
    var keyAsByte = DataUtils.hexStringToByteArray(Common.myPrivateKeyExtract);
    var key = new Key(Key.KeyType.PRIVATE, keyAsByte);
    assertThat(key.type).isEqualTo(Key.KeyType.PRIVATE);
    assertThat(key.key).isEqualTo(keyAsByte);
    assertThat(key.encode()).isEqualTo(Common.myPrivateKey);
  }
}
