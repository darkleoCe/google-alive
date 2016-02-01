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
package server.http;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import server.HttpServer;
import server.util.Console;
import server.util.ChannelUtil;
import server.util.HttpUtil;

import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.SEE_OTHER;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public final class HttpFullRequestHandler extends ChannelInboundHandlerAdapter {
    private Channel directChannel = null;

    public HttpFullRequestHandler(Channel channel){
        this.directChannel = channel;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object fullHttpRequest) throws Exception {
        //System.out.println("HttpFullRequestHandler : " + fullHttpRequest.getClass().toString());
        FullHttpRequest req = (FullHttpRequest)fullHttpRequest;

        directChannel.attr(HttpServer.REQ_PATH_KEY).set(req.getUri());

        //修正Host
        String oldHost = req.headers().get("Host");
        req.headers().set("Host", "www.google.com");
        //修正Referer
        if(req.headers().contains("Referer")){
            String r = req.headers().get("Referer");
            req.headers().set("Referer", r.replace(oldHost, "www.google.com"));
        }
        //修正首页地址
        if(req.getUri().trim().equals("/")){
            req.setUri("/?gws_rd=ssl");
        }

        Console.debug("HttpFullRequestHandler", "Request " + req.getUri());
        /*Console.debug("", "----------------------------------------------------------------");
        for(Map.Entry<String,String> entry: req.headers().entries()){
            Console.debug("HttpFullRequestHandler", entry.getKey() + ": " + entry.getValue());
        }
        Console.debug("", "----------------------------------------------------------------");*/

        //无用请求直接响应
        if(req.getUri().startsWith("/gen_204")){
            Console.debug("HttpFullRequestHandler", "204----------------------------------------------");
            ctx.pipeline().addFirst("tmp-response-encoder", new HttpResponseEncoder());
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            HttpHeaders.setContentLength(response, 0);
            ctx.channel().writeAndFlush(response).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    ctx.pipeline().remove("tmp-response-encoder");
                }
            });
        } else if(req.getUri().startsWith("/url?")){
            String redirectURL = HttpUtil.getParameter("url", req);
            Console.debug("HttpFullRequestHandler", "302------------------" + redirectURL);
            ctx.pipeline().addFirst("tmp-response-encoder", new HttpResponseEncoder());
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, SEE_OTHER);
            response.headers().add(LOCATION, redirectURL);
            HttpHeaders.setContentLength(response, 0);
            ctx.channel().writeAndFlush(response).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    ctx.pipeline().remove("tmp-response-encoder");
                }
            });
        } else {
            if(directChannel.isActive()){
                directChannel.writeAndFlush(fullHttpRequest);
            } else {
                Console.debug("HttpFullRequestHandler", "directChannel inactive while write raw request.");
                ChannelUtil.closeOnFlush(ctx.channel());
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Console.error("HttpFullRequestHandler", "Error: " + cause.getMessage());
        ChannelUtil.closeOnFlush(ctx.channel());
    }
}
