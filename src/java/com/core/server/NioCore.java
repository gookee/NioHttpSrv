package com.core.server;

import com.core.helper.Utility;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.*;

import static com.core.server.HttpRoute.mapRoute;

public class NioCore {
    protected ThreadPoolExecutor pool;
    protected Selector selector;
    public static int bufferSize = 16384;
    public static String charsetName = "utf8";
    public static int REQUEST_TIMEOUT = Utility.toInt(Utility.getConfigValue("REQUEST_TIMEOUT", "60"));

    public NioCore(int port, int corePoolSize, int maxPoolSize, int poolAliveTime) throws IOException {
        selector = this.getSelector(port);
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        pool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, poolAliveTime, TimeUnit.MINUTES, queue, new RejectedExecutionHandler() {

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                Utility.getLogger(this.getClass()).error("The pool RejectedExecutionHandler = " + executor.toString());
                RequestHandle requestHandle = (RequestHandle) r;
                SelectionKey key = requestHandle.getKey();
                if (key.isValid()) {
                    try {
                        key.cancel();
                        SocketChannel channel = (SocketChannel) key.channel();
                        channel.shutdownInput();
                        channel.shutdownOutput();
                        channel.close();
                    } catch (Exception e) {
                    }
                }
            }
        });

        String[] ROUTES = Utility.getConfigValue("ROUTE").trim().split("\\n");
        for (String route : ROUTES) {
            String[] tmp = route.trim().split(",");
            if (tmp.length == 3)
                mapRoute(tmp[0].trim(), tmp[1].trim(), tmp[2].trim());
        }

        new Thread(Session.checkTimeOut()).start();
    }

    protected Selector getSelector(int port) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        Selector sel = Selector.open();
        server.socket().bind(new InetSocketAddress(port));
        server.configureBlocking(false);
        server.register(sel, SelectionKey.OP_ACCEPT);
        Utility.getLogger(this.getClass()).info("server is running on port: " + port);
        return sel;
    }

    public void listen() {
        try {
            while (true) {
                int size = selector.select();
                if (size == 0)
                    continue;

                Iterator iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = (SelectionKey) iter.next();
                    handleKey(key);
                    iter.remove();
                }
            }
        } catch (Exception e) {
            Utility.getLogger(this.getClass()).error("", e);
        }
    }

    protected void handleKey(SelectionKey key) {
        SocketChannel channel = null;
        if (!key.isValid() || !key.channel().isOpen()) {
            return;
        }

        try {
            if (key.isAcceptable()) {
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                try {
                    channel = server.accept();
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);
                } catch (IOException e) {
                    if (channel != null) {
                        key.cancel();
                        channel.close();
                    }
                    Utility.getLogger(this.getClass()).error("", e);
                }
            } else if (key.isReadable()) {
                key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                RequestHandle requestHandle = new RequestHandle(key);
                pool.execute(requestHandle);
            }
        } catch (Exception e) {
            Utility.getLogger(this.getClass()).error("", e);
        }
    }
}
