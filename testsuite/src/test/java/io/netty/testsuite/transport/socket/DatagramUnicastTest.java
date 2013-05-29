/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.testsuite.transport.socket;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class DatagramUnicastTest extends AbstractDatagramTest {

    @Test
    public void testSimpleSend() throws Throwable {
        run();
    }

    public void testSimpleSend(Bootstrap sb, Bootstrap cb) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);

        sb.handler(new ChannelInboundHandlerAdapter() {
            @Override
            public void messageReceived(ChannelHandlerContext ctx, Object[] msgs, int index, int length) throws Exception {
                if (length > 1) {
                    throw new AssertionError();
                }
                assertEquals(1, ((DatagramPacket) msgs[index]).content().readInt());
                latch.countDown();
            }
        });

        cb.handler(new ChannelInboundHandlerAdapter() {
            @Override
            public void messageReceived(ChannelHandlerContext ctx, Object[] msgs, int index, int length) throws Exception {
                // Nothing will be sent.
            }
        });

        Channel sc = sb.bind().sync().channel();
        Channel cc = cb.bind().sync().channel();

        cc.write(new DatagramPacket(Unpooled.copyInt(1), addr)).sync();
        assertTrue(latch.await(10, TimeUnit.SECONDS));

        sc.close().sync();
        cc.close().sync();
    }
}
