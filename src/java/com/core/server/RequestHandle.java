package com.core.server;

import com.core.helper.Utility;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class RequestHandle extends Thread {
    private SelectionKey key;
    protected ByteBuffer clientBuffer = ByteBuffer.allocate(NioCore.bufferSize);
    private long createTime = System.currentTimeMillis();

    public long getCreateTime() {
        return createTime;
    }

    public SelectionKey getKey() {
        return key;
    }

    public RequestHandle(SelectionKey key) {
        this.key = key;
    }

    @Override
    public void run() {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            Map<String, Object> obj = (Map<String, Object>) key.attachment();
            if (obj == null) {
                obj = new HashMap<>();
                obj.put("createTime", System.currentTimeMillis());
                obj.put("baos", new ByteArrayOutputStream());
            }
            long createTime = (Long) obj.get("createTime");
            ByteArrayOutputStream baos = (ByteArrayOutputStream) (obj.get("baos"));
            int size = 0;
            while ((size = channel.read(clientBuffer)) > 0) {
                clientBuffer.flip();
                baos.write(clientBuffer.array(), 0, size);
                clientBuffer.clear();
            }

            if (baos.size() > 0) {
                obj.put("baos", baos);
                key.attach(obj);
                NioRequest request = new NioRequest(key);
                if (NioCore.REQUEST_TIMEOUT > 0 && System.currentTimeMillis() - createTime > NioCore.REQUEST_TIMEOUT * 1000) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(("HTTP/1.1 500 OK\r\n\r\nrequest timeout").getBytes(NioCore.charsetName));
                    while (byteBuffer.hasRemaining())
                        channel.write(byteBuffer);

                    if (key.isValid())
                        key.cancel();
                    channel.shutdownInput();
                    channel.shutdownOutput();
                    channel.close();
                    return;
                }

                String result = request.isReceiveOver();
                if (result == null) {
                    request.doSrv();
                } else if (result.equals("")) {
                    if (size == -1) {
                        channel.close();
                        return;
                    } else {
                        key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                        key.selector().wakeup();
                    }
                } else {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(("HTTP/1.1 500 OK\r\n\r\n" + result).getBytes(NioCore.charsetName));
                    while (byteBuffer.hasRemaining())
                        channel.write(byteBuffer);

                    if (key.isValid())
                        key.cancel();
                    channel.shutdownInput();
                    channel.shutdownOutput();
                    channel.close();
                    return;
                }
            }

            if (size == -1) {
                channel.close();
            }
        } catch (Exception e) {
            Utility.getLogger(this.getClass()).error("", e);
        }
    }
}
