package server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import server.http.HttpDirectConnectRemoteHandler;
import server.util.ChannelUtil;
import server.util.Console;
import server.util.HttpUtil;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private Channel serverChannel = null;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest req) throws Exception {
        //留着request握手成功后使用,refCount也会复制，write后变成0
        final FullHttpRequest rawReq = req.copy();

        //修正Host
        String oldHost = rawReq.headers().get("Host");
        rawReq.headers().remove("Host");
        rawReq.headers().set("Host", "www.google.com");
        //修正Referer
        if(rawReq.headers().contains("Referer")){
            String r = rawReq.headers().get("Referer");
            rawReq.headers().set("Referer", r.replace(oldHost, "www.google.com"));
        }
        //修正首页地址
        if(rawReq.getUri().trim().equals("/")){
            rawReq.setUri("/?gws_rd=ssl");
        }
        Console.debug("Http", "Request " + rawReq.getUri());
        /*Console.debug("", "----------------------------------------------------------------");
        for(Map.Entry<String,String> entry: rawReq.headers().entries()){
            Console.debug("HttpServerHandler", entry.getKey() + ": " + entry.getValue());
        }
        Console.debug("", "----------------------------------------------------------------");*/

        //无用请求直接响应
        if(req.getUri().startsWith("/gen_204")){
            HttpUtil.send204(ctx);
            Console.debug("HttpServerHandler", "204----------------------------------------------");
        } else if(req.getUri().startsWith("/url?")){
            String redirectURL = HttpUtil.getParameter("url", req);
            HttpUtil.redirect(ctx, redirectURL);
            Console.debug("HttpServerHandler", "302------------------" + redirectURL);
        } else {
            //直接连接Google服务器
            ctx.pipeline().addLast("connect", new HttpDirectConnectRemoteHandler());

            ctx.pipeline().remove(this);
            ctx.fireChannelRead(rawReq);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Console.error("HttpServerHandler", "exceptionCaught: " + cause.getMessage());
        ChannelUtil.closeOnFlush(ctx.channel());
        ChannelUtil.closeOnFlush(serverChannel);
    }

    private static String getHost(FullHttpRequest req) {
        String host = HttpHeaders.getHost(req);
        int pos = host.indexOf(":");
        if(pos > 0){
          return host.substring(0, pos);
        } else {
          return host;
        }
    }

    private static Pattern p = Pattern.compile("\\.*?:(\\d+)\\.*?");
    private static int getPort(FullHttpRequest req) {
        Matcher m = p.matcher(HttpHeaders.getHost(req));
        if(m.find()){
            return Integer.valueOf(m.group(1));
        }
        Matcher m1 = p.matcher(req.getUri());
        if(m1.find()){
            return Integer.valueOf(m1.group(1));
        }
        return 80;
    }

}
