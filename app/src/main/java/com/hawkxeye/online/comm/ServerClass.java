package com.hawkxeye.online.comm;

import android.os.Handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerClass extends Thread {
    Socket socket;
    ServerSocket serverSocket;
    Handler handler;
    public ServerClass(Handler handler)
    {
        this.handler = handler;
    }

    @Override
    public void run() {
        super.run();
        try {
            serverSocket = new ServerSocket(7878);
            socket = serverSocket.accept();
            SendReceive.getInstance(socket,handler).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

