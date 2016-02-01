package server.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import server.HttpServer;
import server.util.Console;
import server.direct.RawResponseHandler;
import server.util.ChannelUtil;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HttpDirectConnectRemoteHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private Channel directChannel = null;

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest req) throws Exception {
        //留着request握手成功后使用,refCount也会复制，write后变成0
        final FullHttpRequest rawReq = req.copy();
        final String remoteHost = getHost(req);
        final Integer remotePort = getPort(req);

        final Bootstrap b = new Bootstrap();
        b.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast("HttpRequestEncoder", new HttpRequestEncoder());
                        /*ch.pipeline().addLast("HttpClientCodec", new HttpClientCodec());
                        ch.pipeline().addLast("decompressor", new HttpContentDecompressor());
                        ch.pipeline().addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));*/

                        //ch.pipeline().addLast(new RemoteFullHttpResponseHandler(ctx.channel()));
                        ch.pipeline().addLast(new RawResponseHandler(ctx.channel()));
                    }
                });

        b.connect("www.google.com", 80).addListener(new ChannelFutureListener(){
            public void operationComplete(ChannelFuture future) throws Exception {
                //检查浏览器的连接是否已经断开
                if(future.isSuccess() && ctx.channel().isActive()) {

                    final Channel directChannel = future.channel();
                    /******
                     * 此处小心诡异的浏览器会刚刚发起请求，却又瞬间关闭连接。例如自动完成功能，快速输入多个字符，对于前面几个字符的自动完成列表请求会被瞬间取消
                     * 如果写入失败，则代表连接已经被关闭了。
                     */
                    //Tell browser connect successfully.
                    if (HttpMethod.CONNECT == req.getMethod()) {
                        Console.info("HttpDirectConnectRemoteHandler", "Connect Request Found XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
                        ChannelUtil.closeOnFlush(directChannel);
                        ChannelUtil.closeOnFlush(ctx.channel());
                        return ;
                    }
                    /***
                     * 为了保证http连接可以重用，保留HttpRequestDecoder和HttpObjectAggregator
                     */
                    /***
                     * 针对第2次及以后的请求，不需要解析成FullHttpRequest，直接转发给remote处理。 - 2014-09-28
                     * 结果：失败。在HttpRawRequestHandler中使用byteBufList收集请求后一起发送也不行。
                     */
                    if(directChannel.isActive() && ctx.channel().isActive()){
                        //必须从ctx的pipeline中移除HttpResponseEncoder，因为ctx.channel从directChannel端收到的是原始响应ByteBuf
                        //ctx.pipeline().replace("HttpServerCodec", "HttpRequestDecoder", new HttpRequestDecoder());

                        //增加一个用于处理60秒空闲的Handler
                        ctx.pipeline().addLast(new IdleStateHandler(0, 0, 120, TimeUnit.SECONDS));
                        ctx.pipeline().addLast(new HttpFullRequestHandler(directChannel));

                        ctx.pipeline().remove(HttpDirectConnectRemoteHandler.this);

                        //向directChannel添加一个HttpRequestEncoder，用来encode FullHttpRequest
                        //directChannel.pipeline().addLast("HttpRequestEncoder", new HttpRequestEncoder());

                        /***
                         *  向directChannel发送第1个Http Request，后续的Http Request由HttpFullRequestHandler(directChannel)负责发送
                         */
                        directChannel.attr(HttpServer.REQ_PATH_KEY).set(rawReq.getUri());
                        directChannel.writeAndFlush(rawReq).addListener(new ChannelFutureListener() {
                            public void operationComplete(ChannelFuture future) {
                                if (!future.isSuccess()){
                                    Console.info("Connect-Remote", "Close short live connection.");
                                    ChannelUtil.closeOnFlush(directChannel);
                                    ChannelUtil.closeOnFlush(ctx.channel());
                                }

                            }
                        });
                    } else {
                        Console.info("Http", "HttpDirectConnectRemoteHandler: serverChannel inactive while send connect request.");
                        ChannelUtil.closeOnFlush(directChannel);
                        ChannelUtil.closeOnFlush(ctx.channel());
                    }
                } else {
                    // Close the connection if the websocket handshake attempt has failed.
                    Console.error("Http", "Direct connect error: " + remoteHost + ":" + remotePort);

                    ChannelUtil.closeOnFlush(ctx.channel());
                    ChannelUtil.closeOnFlush(future.channel());
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(directChannel != null){
            ChannelUtil.closeOnFlush(directChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Console.error("Http", "HttpDirectConnectRemoteHandler.exceptionCaught: " + cause.getMessage());
        ChannelUtil.closeOnFlush(ctx.channel());
        if(directChannel != null){
            ChannelUtil.closeOnFlush(directChannel);
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
    private static String getHost(FullHttpRequest req) {
        String host = HttpHeaders.getHost(req);
        return host.split(":")[0];
    }
}
