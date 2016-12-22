package TestClass;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class HttpServer {
//channel就是送货员（或则开完末各区域的配货车），Buffer即使所要送的货物，selector就是中转站的分拣员
//NioSocket中服务端的处理过程可以分为5步：
//    1)创建ServerSocketChannel并设置相应的参数
//    2)创建selector并注册到serversocketchannel上
//    3)调用selector的select方法等待请求
//    4)selector接收到请求后是以哦那个selectedkeys返回SelectionKey集合
//    5)使用SelectionKey获取到Channel、Selector和操作类型并进行具体操作

    public static void main(String[] args) throws Exception {
        //创建ServerSocketChannel，监听8080端口
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();//送货员
        serverSocketChannel.socket().bind((new InetSocketAddress("192.168.1.103",8080)));
        //设置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        //为ssc注册选择器
        Selector selector = Selector.open();//中转站的分拣员
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);//SelectionKey.OP_ACCEPT; 接受请求参数 SelectionKey.OP_CONNECT;连接操作  SelectionKey.OP_READ; 读操作 SelectionKey.OP_WRITE;写操作
        //创建处理器
        while (true) {
            // 等待请求，每次等待阻塞3s，超过3s后线程继续向下运行，如果传入0或者不传参数将一直阻塞
            if (selector.select(3000) == 0) {//用select方法等待请求，并且返回请求的数量
                continue;
            }
            // 获取待处理的SelectionKey
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

            while (keyIter.hasNext()) {
                SelectionKey selectionKey = keyIter.next();//SelectionKey保存了处理当前请求的Channel和selector
                // 启动新线程处理SelectionKey
                new Thread(new HttpHandler(selectionKey)).run();
                // 处理完后，从待处理的SelectionKey迭代器中移除当前所使用的key
                keyIter.remove();
            }
        }
    }

    private static class HttpHandler implements Runnable {

        private int bufferSize = 1024;
        private String localCharset = "UTF-8";
        private SelectionKey selectionKey;

        public HttpHandler(SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
        }

        public void handleAccept() throws IOException {
            SocketChannel clientChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
            clientChannel.configureBlocking(false);//非阻塞线程设置
            clientChannel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
        }

        public void handleRead() throws IOException {
            // 获取channel
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            // 获取buffer并重置
            ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
            buffer.clear();
            // 没有读到内容则关闭
            if (socketChannel.read(buffer) == -1) {
                socketChannel.close();
            } else {
                // 接收请求数据
                buffer.flip();
                String receivedString = Charset.forName(localCharset).newDecoder().decode(buffer).toString();

                // 控制台打印请求报文头
                String[] requestMessage = receivedString.split("\r\n");//每行以\r\n分割
                for (String s : requestMessage) {
                    System.out.println(s);
                    // 遇到空行说明报文头已经打印完
                    if (s.isEmpty()) {
                        break;
                    }
                }

                // 控制台打印首行信息
                String[] firstLine = requestMessage[0].split(" ");//行信息中以空格分隔
                System.out.println();
                System.out.println("Method:\t" + firstLine[0]);
                System.out.println("url:\t" + firstLine[1]);
                System.out.println("HTTP Version:\t" + firstLine[2]);
                System.out.println();

                // 返回客户端
                StringBuilder sendString = new StringBuilder();
                sendString.append("HTTP/1.1 200 OK\r\n");//响应报文首行，200表示处理成功
                sendString.append("Content-Type:text/html;charset=" + localCharset + "\r\n");
                sendString.append("\r\n");// 报文头结束后加一个空行

                sendString.append("<html><head><title>显示报文</title></head><body>");
                sendString.append("接收到请求报文是：<br/>");
                for (String everyRowRequestMessage : requestMessage) {
                    sendString.append(everyRowRequestMessage + "<br/>");
                }
                sendString.append("</body></html>");
                buffer = ByteBuffer.wrap(sendString.toString().getBytes(localCharset));
                socketChannel.write(buffer);
                socketChannel.close();
            }
        }

        @Override
        public void run() {
            try {
                // 接收到连接请求时
                if (selectionKey.isAcceptable()) {
                    handleAccept();
                }
                // 读数据
                if (selectionKey.isReadable()) {
                    handleRead();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
