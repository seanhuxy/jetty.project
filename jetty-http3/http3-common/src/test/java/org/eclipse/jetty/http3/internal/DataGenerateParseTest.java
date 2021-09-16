//
// ========================================================================
// Copyright (c) 1995-2021 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.http3.internal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.jetty.http3.frames.DataFrame;
import org.eclipse.jetty.http3.frames.Frame;
import org.eclipse.jetty.http3.internal.generator.MessageGenerator;
import org.eclipse.jetty.http3.internal.parser.MessageParser;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.NullByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataGenerateParseTest
{
    @Test
    public void testGenerateParseEmpty() throws Exception
    {
        testGenerateParse(BufferUtil.EMPTY_BUFFER);
    }

    @Test
    public void testGenerateParse() throws Exception
    {
        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        testGenerateParse(ByteBuffer.wrap(bytes));
    }

    private void testGenerateParse(ByteBuffer byteBuffer) throws Exception
    {
        byte[] inputBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(inputBytes);
        DataFrame input = new DataFrame(ByteBuffer.wrap(inputBytes), true);

        ByteBufferPool.Lease lease = new ByteBufferPool.Lease(new NullByteBufferPool());
        new MessageGenerator(null, 8192, true).generate(lease, 0, input);

        List<Frame> frames = new ArrayList<>();
        MessageParser parser = new MessageParser(0, null);
        for (ByteBuffer buffer : lease.getByteBuffers())
        {
            while (buffer.hasRemaining())
            {
                Frame frame = parser.parse(buffer);
                if (frame != null)
                    frames.add(frame);
            }
        }

        assertEquals(1, frames.size());
        DataFrame output = (DataFrame)frames.get(0);
        byte[] outputBytes = new byte[output.getData().remaining()];
        output.getData().get(outputBytes);
        assertArrayEquals(inputBytes, outputBytes);
    }
}
