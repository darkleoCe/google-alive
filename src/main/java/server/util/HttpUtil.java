package server.util;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpUtil {
    public static void ok(ChannelHandlerContext ctx, String content) {
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        setContentLength(res, res.content().readableBytes());
        ctx.writeAndFlush(res);
    }

    public static void okWithPlainText(ChannelHandlerContext ctx, String content) {
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        res.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        setContentLength(res, res.content().readableBytes());
        ctx.writeAndFlush(res);
    }

    public static void send204(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
        HttpHeaders.setContentLength(response, 0);
        ctx.writeAndFlush(response);
    }

    public static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response);
    }

    public static void redirect(ChannelHandlerContext ctx, String uri){
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, SEE_OTHER);
        response.headers().add(LOCATION, uri);
        HttpHeaders.setContentLength(response, 0);
        ctx.channel().writeAndFlush(response);
    }

    public static String getParameter(String name, FullHttpRequest request){
        QueryStringDecoder decoder = null;
        if(request.getMethod() == HttpMethod.POST){
            decoder = new QueryStringDecoder("/?" + request.content().toString(CharsetUtil.UTF_8));
        } else if(request.getMethod() == HttpMethod.GET) {
            decoder = new QueryStringDecoder(request.getUri());
        }

        Map<String, List<String>> map = decoder.parameters();
        if(map.containsKey(name) && map.get(name).size() > 0){
            return map.get(name).get(0);
        }

        return "";
    }

}
