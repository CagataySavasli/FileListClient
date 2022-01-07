package client;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Scanner;

import model.FileDataResponseType;
import model.FileListResponseType;
import model.FileSizeResponseType;
import model.RequestType;
import model.ResponseType;
import model.ResponseType.RESPONSE_TYPES;
import client.loggerManager;

public class dummyClient {

	private void sendInvalidRequest(String ip, int port) throws IOException{
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

	
	private long getFileSize(String ip, int port, int file_id) throws IOException{
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
/*
    public String MD5(String md5) {
        String md5string = "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }
    }

*/

    public static void main(String[] args) throws Exception{
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
        System.out.println("File "+file_id+"is "+file_size+" bytes. Starting to download…");
        if(inst.RTT(inst, ip, port1, port2)){ inst.getFileData(ip, port1, file_id, 1, file_size);}
        else { inst.getFileData(ip, port2, file_id, 1, file_size);}

        System.out.println(file_size);


        /*
        System.out.println(System.getProperty(String.valueOf(file_id)));
        System.out.println("file id aldi");
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream is = Files.newInputStream(Paths.get(String.valueOf(file_id)));
        DigestInputStream dis = new DigestInputStream(is, md);
        byte[] digest = md.digest();
        System.out.println("File" +file_id+ "has been downloaded in 32345 ms. The md5 hash is " + md.digest());
        //kod bu. bunun çalışması için download edilen kodu bulmamız ve sonra Paths.get() içerisine path'ini yazmamız gerekiyor.
        //uzaktaki dosyayı hashleyemiyoruz
        */

        /*
        inst.sendInvalidRequest(ip1,port1);
		inst.getFileList(ip1,port1);
		inst.getFileSize(ip1,port1,0);
		long size=inst.getFileSize(ip1,port1,1);
		inst.getFileData(ip1,port1,0,0,1);
		inst.getFileData(ip1,port1,1,30,20);
		inst.getFileData(ip1,port1,1,1,size);
        */
	}
}
