/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package com.datastax.oss.driver.internal.core.type.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Variable length encoding inspired from Google <a
 * href='https://developers.google.com/protocol-buffers/docs/encoding#varints'>varints</a>.
 *
 * <p>Cassandra vints are encoded with the most significant group first. The most significant byte
 * will contains the information about how many extra bytes need to be read as well as the most
 * significant bits of the integer. The number of extra bytes to read is encoded as 1 bit on the
 * left side. For example, if we need to read 3 more bytes the first byte will start with 1110. If
 * the encoded integer is 8 bytes long the vint will be encoded on 9 bytes and the first byte will
 * be: 11111111
 *
 * <p>Signed integers are (like protocol buffer varints) encoded using the ZigZag encoding so that
 * numbers with a small absolute value have a small vint encoded value too.
 *
 * <p>Note that there is also a type called {@code varint} in the CQL protocol specification. This
 * is completely unrelated.
 */
public class VIntCoding {

  private static long readUnsignedVInt(DataInput input) throws IOException {
    int firstByte = input.readByte();

    // Bail out early if this is one byte, necessary or it fails later
    if (firstByte >= 0) {
      return firstByte;
    }

    int size = numberOfExtraBytesToRead(firstByte);
    long retval = firstByte & firstByteValueMask(size);
    for (int ii = 0; ii < size; ii++) {
      byte b = input.readByte();
      retval <<= 8;
      retval |= b & 0xff;
    }

    return retval;
  }

  public static long readVInt(DataInput input) throws IOException {
    return decodeZigZag64(readUnsignedVInt(input));
  }

  // & this with the first byte to give the value part for a given extraBytesToRead encoded in the
  // byte
  private static int firstByteValueMask(int extraBytesToRead) {
    // by including the known 0bit in the mask, we can use this for encodeExtraBytesToRead
    return 0xff >> extraBytesToRead;
  }

  private static byte encodeExtraBytesToRead(int extraBytesToRead) {
    // because we have an extra bit in the value mask, we just need to invert it
    return (byte) ~firstByteValueMask(extraBytesToRead);
  }

  private static int numberOfExtraBytesToRead(int firstByte) {
    // we count number of set upper bits; so if we simply invert all of the bits, we're golden
    // this is aided by the fact that we only work with negative numbers, so when upcast to an int
    // all
    // of the new upper bits are also set, so by inverting we set all of them to zero
    return Integer.numberOfLeadingZeros(~firstByte) - 24;
  }

  private static final ThreadLocal<byte[]> encodingBuffer =
      ThreadLocal.withInitial(() -> new byte[9]);

  private static void writeUnsignedVInt(long value, DataOutput output) throws IOException {
    int size = VIntCoding.computeUnsignedVIntSize(value);
    if (size == 1) {
      output.write((int) value);
      return;
    }

    output.write(VIntCoding.encodeVInt(value, size), 0, size);
  }

  private static byte[] encodeVInt(long value, int size) {
    byte encodingSpace[] = encodingBuffer.get();
    int extraBytes = size - 1;

    for (int i = extraBytes; i >= 0; --i) {
      encodingSpace[i] = (byte) value;
      value >>= 8;
    }
    encodingSpace[0] |= encodeExtraBytesToRead(extraBytes);
    return encodingSpace;
  }

  public static void writeVInt(long value, DataOutput output) throws IOException {
    writeUnsignedVInt(encodeZigZag64(value), output);
  }

  /**
   * Decode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n an unsigned 64-bit integer, stored in a signed int because Java has no explicit
   *     unsigned support.
   * @return a signed 64-bit integer.
   */
  private static long decodeZigZag64(final long n) {
    return (n >>> 1) ^ -(n & 1);
  }

  /**
   * Encode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n a signed 64-bit integer.
   * @return an unsigned 64-bit integer, stored in a signed int because Java has no explicit
   *     unsigned support.
   */
  private static long encodeZigZag64(final long n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 63);
  }

  /** Compute the number of bytes that would be needed to encode a varint. */
  public static int computeVIntSize(final long param) {
    return computeUnsignedVIntSize(encodeZigZag64(param));
  }

  /** Compute the number of bytes that would be needed to encode an unsigned varint. */
  private static int computeUnsignedVIntSize(final long value) {
    int magnitude =
        Long.numberOfLeadingZeros(
            value | 1); // | with 1 to ensure magnitude <= 63, so (63 - 1) / 7 <= 8
    return (639 - magnitude * 9) >> 6;
  }
}
