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

package net.klesatschke.threema.cli.console.commands;

import java.io.File;

import com.neilalexander.jnacl.NaCl;

import net.klesatschke.threema.api.CryptTool;
import net.klesatschke.threema.api.DataUtils;
import net.klesatschke.threema.api.Key;
import net.klesatschke.threema.api.Key.KeyType;
import net.klesatschke.threema.cli.console.commands.fields.TextField;

public class GenerateKeyPairCommand extends Command {
  private final TextField privateKeyPath;
  private final TextField publicKeyPath;

  public GenerateKeyPairCommand() {
    super(
        "Generate Key Pair",
        "Generate a new key pair and write the private and public keys to the respective files (in hex).");
    this.privateKeyPath = this.createTextField("privateKeyFile");
    this.publicKeyPath = this.createTextField("publicKeyPath");
  }

  @Override
  protected void execute() throws Exception {
    var privateKey = new byte[NaCl.SECRETKEYBYTES];
    var publicKey = new byte[NaCl.PUBLICKEYBYTES];

    CryptTool.generateKeyPair(privateKey, publicKey);

    // Write both keys to file
    DataUtils.writeKeyFile(
        new File(this.privateKeyPath.getValue()), new Key(KeyType.PRIVATE, privateKey));
    DataUtils.writeKeyFile(
        new File(this.publicKeyPath.getValue()), new Key(KeyType.PUBLIC, publicKey));
  }
}
