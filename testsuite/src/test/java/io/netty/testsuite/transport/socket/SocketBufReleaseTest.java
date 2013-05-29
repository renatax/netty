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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class SocketBufReleaseTest extends AbstractSocketTest {

    private static final EventExecutor executor =
            new DefaultEventExecutorGroup(1, new DefaultThreadFactory(SocketBufReleaseTest.class, true)).next();

    @Test
    public void testBufRelease() throws Throwable {
        run();
    }

    public void testBufRelease(ServerBootstrap sb, Bootstrap cb) throws Throwable {
        BufWriterHandler serverHandler = new BufWriterHandler();
        BufWriterHandler clientHandler = new BufWriterHandler();

        sb.childHandler(serverHandler);
        cb.handler(clientHandler);

        Channel sc = sb.bind().sync().channel();
        Channel cc = cb.connect().sync().channel();

        // Ensure the server socket accepted the client connection *and* initialized pipeline successfully.
        serverHandler.channelFuture.sync();

        // and then close all sockets.
        sc.close().sync();
        cc.close().sync();

        serverHandler.check();
        clientHandler.check();
    }

    private static class BufWriterHandler extends ChannelInboundHandlerAdapter{

        private final Random random = new Random();
        private final CountDownLatch latch = new CountDownLatch(1);
        private ByteBuf buf;
        private final Promise<Channel> channelFuture = new DefaultPromise<Channel>(executor);

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            channelFuture.setSuccess(ctx.channel());
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            byte[] data = new byte[1024];
            random.nextBytes(data);

            buf = ctx.alloc().buffer();
            buf.writeBytes(data);

            ctx.channel().write(buf).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    latch.countDown();
                }
            });
        }


        public void check() throws InterruptedException {
            latch.await();
            assertEquals(0, buf.refCnt());
        }
    }
}
