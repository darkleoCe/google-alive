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
package server.direct;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import server.util.Console;
import server.util.ChannelUtil;

import java.util.ArrayList;
import java.util.List;

/***
 * 该项目中未使用
 */
public final class RawRequestHandler extends ChannelInboundHandlerAdapter {
    private Channel serverChannel = null;
    private List<ByteBuf> byteBufList = new ArrayList<ByteBuf>();

    public RawRequestHandler(Channel channel){
        this.serverChannel = channel;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //直接将ByteBuf包裹在BinaryWebSocketFrame中，传递给serverChannel
        if (serverChannel.isActive()) {
            ByteBuf fullByteBuf = Unpooled.wrappedBuffer(byteBufList.toArray(new ByteBuf[0]));

            //Console.debug("GogoSocksRawRequestHandler - send request, len: " + fullByteBuf.readableBytes() );
            //System.out.println(this.hashCode() + ": >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>request from browser");
            //System.out.println(fullByteBuf.duplicate().toString(CharsetUtil.UTF_8));
            //System.out.println("-----------------------------------------");

            //serverChannel.writeAndFlush之后，byteBufList里的ByteBuf的refCount都-1变成0
            serverChannel.writeAndFlush(fullByteBuf);
            byteBufList.clear();
        } else {
            Console.info("GoGo", "RawRequestHandler's websocket channel is not active.");
            for(ByteBuf b: byteBufList){
              ReferenceCountUtil.release(b);
            }
            byteBufList.clear();

            //websocket channel is not active, so close the browser connection
            ChannelUtil.closeOnFlush(ctx.channel());
        }
    }

    /**
     *  缓存从浏览器过来的原始请求，放入byteBufList
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byteBufList.add((ByteBuf) msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //如果浏览器连过来的Channel空闲60秒，则关闭链路： browser - gogo - gogoserver - remote
        if(evt instanceof IdleStateEvent) {

            //System.out.println(new Date().toLocaleString() + "GogoSocksRawRequestHandler IdleStateEvent ******************************************");

            //关闭browser - gogo
            ChannelUtil.closeOnFlush(ctx.channel());
            //关闭gogo - gogoserver
            ChannelUtil.closeOnFlush(serverChannel);

        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelUtil.closeOnFlush(serverChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Console.error("GoGo", "RawRequestHandler.exceptionCaught: " + cause.getMessage());
        //cause.printStackTrace();
        ChannelUtil.closeOnFlush(ctx.channel());
        ChannelUtil.closeOnFlush(serverChannel);
    }
}
