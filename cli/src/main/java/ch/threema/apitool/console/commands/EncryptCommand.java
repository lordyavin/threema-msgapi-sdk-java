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

package ch.threema.apitool.console.commands;

import ch.threema.apitool.console.commands.fields.PrivateKeyField;
import ch.threema.apitool.console.commands.fields.PublicKeyField;
import net.klesatschke.threema.api.CryptTool;
import net.klesatschke.threema.api.DataUtils;
import net.klesatschke.threema.api.results.EncryptResult;

public class EncryptCommand extends Command {
  private final PrivateKeyField privateKeyField;
  private final PublicKeyField publicKeyField;

  public EncryptCommand() {
    super(
        "Encrypt",
        "Encrypt standard input using the given sender private key and recipient public key. Prints two lines to standard output: first the nonce (hex), and then the box (hex).");

    this.privateKeyField = this.createPrivateKeyField("privateKey");
    this.publicKeyField = this.createPublicKeyField("publicKey");
  }

  @Override
  protected void execute() throws Exception {
    byte[] privateKey = this.privateKeyField.getValue();
    byte[] publicKey = this.publicKeyField.getValue();

    /* read text from stdin */
    String text = readStream(System.in, "UTF-8").trim();

    EncryptResult res = CryptTool.encryptTextMessage(text, privateKey, publicKey);

    System.out.println(DataUtils.byteArrayToHexString(res.getNonce()));
    System.out.println(DataUtils.byteArrayToHexString(res.getResult()));
  }
}