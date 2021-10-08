package com.hawkxeye.online.comm;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientClass extends Thread {
    private static final String TAG = ClientClass.class.getName();
    Socket socket;
    String hostAddress;
    Handler handler;

    public ClientClass(InetAddress hostAddress, Handler handler) {
        this.handler = handler;
        this.hostAddress = hostAddress.getHostAddress();
        socket = new Socket();
    }

    @Override
    public void run() {
        while (!socket.isConnected()) {
            if (isInterrupted()) {
                break;
            }
            try {
                socket.connect(new InetSocketAddress(hostAddress, 7878), 500);
            } catch (Exception e) {
                Log.d(TAG, "client socket error");
                try {
                    socket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }finally {
                    socket = null;
                    socket = new Socket();
                    try {
                        sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
            }
        }
        if (socket.isConnected())
            SendReceive.getInstance(socket, handler).start();
    }
}
