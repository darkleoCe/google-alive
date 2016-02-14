# google-alive
---------------------------------------------------
## 简介
google-alive可以帮助用户快速搭建一个Google搜索服务，前提是运行google-alive的机器必须可以直接访问Google搜索服务。google-alive的实现原理是为用户浏览器和Google服务器之间建立一个高效的数据传输通道，利用该通道用户可以发送搜索请求并接收Google的响应。

## 开始使用
###  部署到个人服务器或VPS
1. 安装Java运行环境(Java 1.8 以上)
2. 下载[google-alive-1.0.0.jar](https://raw.githubusercontent.com/gogotunnel/google-alive/master/bin/google-alive-1.0.0.jar)
3. 启动google-alive
```
	//在80端口上启动google-alive
	java -jar google-alive-1.0.0.jar 80
```

###  部署到支持Java的Paas平台
1. 部署到Heroku平台请参考[这里](https://devcenter.heroku.com/articles/getting-started-with-java#introduction)
2. 部署到CloudControl平台请参考[这里](https://www.cloudcontrol.com/dev-center/guides)

## 演示地址
 [http://ggalive.herokuapp.com/](http://ggalive.herokuapp.com/)
