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
            int port = parsePort(args);
            Console.info("HttpServer", "Start google-alive on " + port);
            ChannelFuture f = b.bind(port).sync();

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

    //env > args > default
    public static int parsePort(String[] args) {
        try {
            //Parse port from env
            String portInEnv = System.getenv("PORT");
            if (portInEnv != null && !portInEnv.trim().equals("")) {
                return Integer.valueOf(System.getenv("PORT"));
            } else {
                //Parse port from args
                if (args.length >= 1) {
                    return Integer.valueOf(args[0].trim());
                } else {
                    return 80;
                }
            }
        } catch (Exception e){
            return 80;
        }
    }
}
