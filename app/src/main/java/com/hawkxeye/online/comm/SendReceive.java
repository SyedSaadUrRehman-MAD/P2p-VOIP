package com.hawkxeye.online.comm;

import android.os.Handler;
import android.os.ParcelFileDescriptor;

import com.hawkxeye.online.utils.Constants;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SendReceive extends Thread {
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Handler handler;
    private static SendReceive instance;

    public static SendReceive getInstance(Socket skt, Handler handler) {
        instance = new SendReceive(skt, handler);
        return instance;
    }

    public static SendReceive getInstance() {
        return instance;
    }

    private SendReceive(Socket skt, Handler handler) {
        this.handler = handler;
        this.socket = skt;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        int bytes;
        byte[] buffer = new byte[1024];
        while (socket != null && socket.isConnected()) {
            try {
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    if (handler != null)
                        handler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

//            byte[] buffer2 = new byte[1024];
//            try {
//                bytes = fileInputStream.read(buffer2);
//                if (bytes > 0) {
//                    if (handler != null)
//                        handler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer2).sendToTarget();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    public void write(byte[] buffer) {
        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileDescriptor getFileDescriptor()
    {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
        return pfd.getFileDescriptor();
    }
}
