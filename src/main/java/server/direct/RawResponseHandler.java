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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import server.util.ChannelUtil;
import server.util.Console;
import java.util.ArrayList;
import java.util.List;


@ChannelHandler.Sharable
public final class RawResponseHandler extends ChannelInboundHandlerAdapter {
    private Channel clientChannel = null;
    private List<ByteBuf> byteBufList = new ArrayList<ByteBuf>();

    public RawResponseHandler(Channel channel){
       this.clientChannel = channel;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //取出原始响应，写回浏览器
        if (clientChannel.isActive()) {
            ByteBuf fullByteBuf = Unpooled.wrappedBuffer(byteBufList.toArray(new ByteBuf[0]));

            //获取当前响应的请求地址
            /*String reqPath = ctx.channel().attr(HttpServer.REQ_PATH_KEY).get();
            Console.info("RawResponseHandler", "<<<<<< response from remote <<<<<<");
            Console.info("RawResponseHandler", fullByteBuf.duplicate().toString(CharsetUtil.UTF_8));
            Console.info("RawResponseHandler", "-----------------------------------------");*/

            clientChannel.writeAndFlush(fullByteBuf);
            byteBufList.clear();
        } else {
            //Log.info("RawResponseHandler - <<<<<< clientChannel not active after response from remote <<<<<<");
            for(ByteBuf b: byteBufList){
                ReferenceCountUtil.release(b);
            }
            byteBufList.clear();

           //clientChannel not active, so close the websocket connection
            ChannelUtil.closeOnFlush(ctx.channel());
        }
    }

    /**
     *  读取从HerokuApp返回的Http Response，并将Body中的Socks Response取出来写回给浏览器
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byteBufList.add((ByteBuf) msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //Log.info("RawResponseHandler - remoteChannel inactive-----------------");
        if(clientChannel != null) {
            ChannelUtil.closeOnFlush(clientChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Console.error("Direct", "RawResponseHandler.exceptionCaught: " + cause.getMessage());
        if(clientChannel != null){
            ChannelUtil.closeOnFlush(clientChannel);
        }
        ChannelUtil.closeOnFlush(ctx.channel());
    }
}
