package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.*;

import model.FileDataResponseType;
import model.FileListResponseType;
import model.FileSizeResponseType;
import model.RequestType;
import model.ResponseType;
import model.ResponseType.RESPONSE_TYPES;

public class dummyClient {

    void sendInvalidRequest(String ip, int port) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(4, 0, 0, 0, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        dsocket.receive(receivePacket);
        ResponseType response=new ResponseType(receivePacket.getData());
        loggerManager.getInstance(this.getClass()).debug(response.toString());
    }

    private void getFileList(String ip, int port) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_LIST, 0, 0, 0, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        dsocket.receive(receivePacket);
        FileListResponseType response=new FileListResponseType(receivePacket.getData());
        loggerManager.getInstance(this.getClass()).debug(response.toString());
    }


    long getFileSize(String ip, int port, int file_id) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_SIZE, file_id, 0, 0, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        dsocket.receive(receivePacket);
        FileSizeResponseType response=new FileSizeResponseType(receivePacket.getData());
        loggerManager.getInstance(this.getClass()).debug(response.toString());
        return response.getFileSize();
    }

    private void getFileData(String ip, int port, int file_id, long start, long end) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        long maxReceivedByte=-1;
        while(maxReceivedByte<end){
            DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
            dsocket.receive(receivePacket);
            FileDataResponseType response=new FileDataResponseType(receivePacket.getData());
            loggerManager.getInstance(this.getClass()).debug(response.toString());
            if (response.getResponseType()!=RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
                break;
            }
            if (response.getEnd_byte()>maxReceivedByte){
                maxReceivedByte=response.getEnd_byte();
            };
        }

    }

    public static long getFileBytes(dummyClient inst, int fileId, int port, String ip) throws IOException {

        return inst.getFileSize(ip, port, fileId);
    }

    //The client must also compute the md5sum of the received file and output it on the screen.
    // We will use this md5 hash to make sure the data was fully downloaded and is identical to the source

    public static String md5Sum(byte[] data) throws IOException, NoSuchAlgorithmException {
        if (data == null)
            return "null";
        try {
            String s = "";
            MessageDigest md = MessageDigest.getInstance("MD5");
            data = md.digest();
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < data.length; ++i) {
                sb.append(Integer.toHexString((data[i] & 0xFF) | 0x100).substring(1,3));
            }

            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    //bu methodu mainde güzel bir yere daha sonra yerleştireceğiz
    public boolean RTT(dummyClient inst, String ip, int port1, int port2) throws IOException{
        long t1_before = System.currentTimeMillis();
        inst.sendInvalidRequest(ip,port1);
        long t1_after = System.currentTimeMillis();

        long t2_before = System.currentTimeMillis();
        inst.sendInvalidRequest(ip,port2);
        long t2_after = System.currentTimeMillis();

        long t1 = t1_after - t1_before;
        long t2 = t2_after - t2_before;
        if (t1 <= t2){ return true;}
        else{ return false;}
    }


    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public static void main(String[] args) throws Exception{
        int theFastestProt;
        long start_byte = 1;
        long packetByte = 1000;

        Scanner scanner = new Scanner(System.in);
        if (args.length<1){
            throw new IllegalArgumentException("ip:port is mandatory");
        }
        String[] adr=args[0].split(":");
        String ip=adr[0];
        int port1=Integer.valueOf(adr[1]);
        int port2=Integer.valueOf(adr[2]);


        dummyClient inst=new dummyClient();


        inst.sendInvalidRequest(ip,port1);
        inst.getFileList(ip,port1);
        System.out.print("Enter a number : ");
        int file_id = scanner.nextInt();
        long file_size = inst.getFileSize(ip, port1, file_id);
        System.out.println("File "+file_id+" has been selected. Getting the size information…");

        if(inst.RTT(inst, ip, port1, port2)){ theFastestProt = port1;}
        else { theFastestProt = port2;}

        byte[] file_size_AsByte = longToBytes(file_size);  //to calculate md5sum

        myThread thread = new myThread(file_id, ip, theFastestProt, inst, -1, -1);
        thread.run();

        System.out.println("File "+file_id+"is "+file_size+" bytes. Starting to download…");

        long end_byte = packetByte;
        int port;
        long timer;
        var x = 1;

        long time = System.currentTimeMillis();

        while (start_byte < file_size) {

            if (file_size - start_byte > 1000) {

                myThread thread2 = new myThread(file_id, ip, theFastestProt, inst, start_byte, end_byte);
                thread2.run();

                timer = thread2.getTimer();

                if (timer == 0) timer = 1;

                System.out.println("File is downloading in port no: " + theFastestProt + "\n" + "Downloading in speed (Bytes / ms): " + (packetByte / timer) + "\n" + "Time passed: " + (System.currentTimeMillis() - time) + " ms \n");

                start_byte += packetByte;
                end_byte += packetByte;
                x++;

            } else {
                myThread thread3 = new myThread(file_id, ip, theFastestProt, inst, start_byte, file_size);
                thread3.run();

                System.out.println("File " + file_id + " has been downloaded in " + (System.currentTimeMillis() - time) + " ms. in port " + theFastestProt);

                System.out.println("Download completed.");
                break;
            }
        }

        String md5sum = md5Sum(file_size_AsByte);

        System.out.println("File " + file_id + " has been downloaded in " + (System.currentTimeMillis() - time) + " ms. The md5 hash is  " + md5sum);
        System.exit(0);

        /*
        if(inst.RTT(inst, ip, port1, port2)){ inst.getFileData(ip, port1, file_id, 1, file_size);}
        else { inst.getFileData(ip, port2, file_id, 1, file_size);}
        System.out.println(file_size);*/

    }
}

class myThread extends Thread {

    int fileId;
    String ip;
    int port;
    dummyClient inst;
    long start;
    long end;
    long size;
    long timer;


    Future<Object> obj; //A Future represents the result of an asynchronous computation. Methods are provided to check if the computation is complete, to wait for its completion, and to retrieve the result of the computation.
    ExecutorService exc;

    public myThread(int fileId, String ip, int port, dummyClient inst, long start, long end) {

        this.fileId = fileId;
        this.ip = ip;
        this.port = port;
        this.inst = inst;
        this.start = start;
        this.end = end;

        exc = Executors.newSingleThreadExecutor();
    }

    public void run() {

        try {
            obj = exc.submit(() -> {
                size = inst.getFileSize(ip, port, fileId);
                return 0;
            });

            obj.get(timer, TimeUnit.MILLISECONDS); //to throw exception
        }

        catch (final TimeoutException e) {

            try {
                size = inst.getFileSize(ip, port, fileId);

            } catch (IOException ex) {
                System.out.println("IO error");
            }
            obj.cancel(true);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        exc.shutdown();
    }

    public long getTimer() {
        return timer;
    }


}


