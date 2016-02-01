package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import server.util.Console;


public class HttpServer {
    public static final AttributeKey<String> REQ_PATH_KEY = AttributeKey.newInstance("REQ_PATH_KEY");
    public static void main(String[] args){

        EventLoopGroup bossGroup = new NioEventLoopGroup(2);
        EventLoopGroup workerGroup = new NioEventLoopGroup(2);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpServerInitializer());

            //ChannelFuture f = b.bind(Integer.valueOf(System.getenv("PORT"))).sync();
            ChannelFuture f = b.bind(9000).sync();

            f.channel().closeFuture().sync();
        } catch(Exception e){
            System.out.println("Http proxy start error: " + e.getMessage());
            System.exit(0);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

            System.out.println("Http Server Exit.");
        }
    }
}
