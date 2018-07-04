package trans;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.*;
import java.net.*;

public class SocketThread extends Thread {
    public SocketThread() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public final static int CACHE_SIZE = 10240; //????Π΅

    public final static int FILE_TRANS_STATUS_FILENAME = 0x01; //???????Χ΄Μ¬

    public final static int FILE_TRANS_STATUS_CONTEXT = 0x02; //????????Χ΄Μ¬

    public final static int FILE_TRANS_STATUS_WAITFORCONFIRM = 0x03; //?Θ΄?Θ·????ΔΌ?

    public final static int FILE_TRANS_STATUS_SUCCESS = 0x04; //???????ΙΉ?

    public final static int FILE_TRANS_STATUS_FAIL = 0x05; //??????Κ§??

    public final static int FILE_TRANS_STATUS_CANCELTRANS = 0x06; //Θ΅???????

    public final static int PACKAGE_TYPE_FILENAME = 0x01; //??????

    public final static int PACKAGE_TYPE_CONTEXT = 0x02; //???????

    public final static int PACKAGE_TYPE_CONFIRMRECEIVE = 0x03; //???????

    private Socket aSocket; //????

    private String serverName; //???????

    private DataInputStream dis; //????

    private DataOutputStream dos; //?????

    private DataInputStream fDis; //???????

    private RandomAccessFile raf; //????????

    private boolean fileSender = false; //???????

    private boolean running = false; //?????

    public int fileTransStatus = 0x0; //??????Χ΄Μ¬

    private File aFile; //?????ΔΌ?

    public long fileSize; //??????

    private String fileName; //??????

    private String errorMessage; //????ΟΆ

    private long transFileLength = 0; //????????

    private byte [] dataBuf;

    private String message; //?Φ¤?ΟΆ

    private String IP; //ΔΏ??P

    private int port; //ΔΏ????

    private boolean fileTransed=false; //??????ΡΎ???ΚΌ???

    int count=0;

    //???ίΉ?????
    public SocketThread(Socket aSocket) {
        this.aSocket = aSocket;
        try {
            aSocket.setSoTimeout(300000);
        } catch (SocketException ex) {
        }
        byte[] address = aSocket.getInetAddress().getAddress();
        IP = (address[0] & 0xff) + "." + (address[1] & 0xff) + "." +
             (address[2] & 0xff) + "." + (address[3] & 0xff);
        try {
            dis = new DataInputStream(aSocket.getInputStream());
            dos = new DataOutputStream(aSocket.getOutputStream());
            fileTransStatus = FILE_TRANS_STATUS_FILENAME;
        } catch (IOException ex) {
            setError("???????Σ΄??);
        }

        try {
            aSocket.setReceiveBufferSize(CACHE_SIZE*2);
        } catch (SocketException ex1) {
            ex1.printStackTrace();
        }
        dataBuf=new byte[CACHE_SIZE+100];
    }

    //??????????
    public SocketThread(String serverName, int portNo, String fileName,
                        String message) {
            aFile = new File(fileName);
            this.fileName = aFile.getName();
            this.fileSize = fileSize;
            fileSender = true;
            if (message != null) {
                this.message = message;
            }
            this.IP = serverName;
            this.port = portNo;
            dataBuf=new byte[CACHE_SIZE];
    }

    //????????
    public void run() {
        running = true;
        if (fileSender) {
            try {
                aSocket = new Socket(IP, port);
                aSocket.setSoTimeout(300000);
                aSocket.setSendBufferSize(CACHE_SIZE*2);
                dos = new DataOutputStream(aSocket.getOutputStream());
                dis = new DataInputStream(aSocket.getInputStream());
                fDis = new DataInputStream(new FileInputStream(aFile));
                fileTransStatus = FILE_TRANS_STATUS_FILENAME;
            } catch (UnknownHostException ex1) {
                ex1.printStackTrace();
                setError("???Σ·??????);
            } catch (IOException ex1) {
                ex1.printStackTrace();
                setError("???????Σ΄??);
            }

        } while (running) {
            if (fileSender) {
                sendFile();
            } else {
                receiveFile();
            }
            try {
                Thread.sleep(6);
            } catch (InterruptedException ex) {
            }
        }
    }

    //??ocket??
    private int readFromSocket(byte[] data) throws IOException {
        int length = 0;
        length = fDis.read(data);
        return length;
    }

    //??ocket??
    private int readFromSocket() throws IOException {
        int buf = 0;
        buf = dis.readInt();
        return buf;
    }

    //??ΔΌ???
    private int readFromFile(byte[] data,int off,int length) {
        int len=0;
        try {
            len = fDis.read(data,off,length);
        } catch (IOException ex) {
            setError("?????Θ΅???);
        }
        return len;
    }

    //Π΄?socket
    private void writeToSocket(byte[] data) throws IOException {
        dos.write(data);
    }

    //Π΄????
    private void writeToFile(byte[] data,int off,int length) throws IOException {
        raf.write(data,off,length);
    }

    //Π΄?socket
    private void writeToSocket(int data) throws IOException {
        dos.writeInt(data);
    }

    private void writeToSocket(long data) throws IOException {
        dos.writeLong(data);
    }

    private long readLongFromSocket() throws IOException {
        return dis.readLong();
    }

    //??
    private byte[] doPackage(byte[] data, int length) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream bufDos = new DataOutputStream(buf);
        DataOutputStream baosDos = new DataOutputStream(baos);
        switch (fileTransStatus) {
        case FILE_TRANS_STATUS_FILENAME: {
            bufDos.writeInt(PACKAGE_TYPE_FILENAME);
            bufDos.writeInt(fileName.getBytes().length);
            bufDos.write(fileName.getBytes());
            bufDos.writeLong(fileSize);
            if (message!=null) {
                bufDos.writeInt(message.getBytes().length);
                bufDos.write(message.getBytes());
            } else {
                bufDos.writeInt(-1);
            }
            baosDos.writeInt(buf.toByteArray().length);
            baosDos.write(buf.toByteArray());
            break;
        }
        case FILE_TRANS_STATUS_CONTEXT: {
            bufDos.writeInt(PACKAGE_TYPE_CONTEXT);
            if ((transFileLength + length) >= fileSize) {
                bufDos.writeInt(0);
            } else {
                bufDos.writeInt(1);
            }
            bufDos.writeInt(length);
            bufDos.write(data, 0, length);
            baosDos.writeInt(buf.toByteArray().length);
            baosDos.write(buf.toByteArray());
            break;
        }
        }
        return baos.toByteArray();
    }

    //Ν£ΦΉ???
    public void stopThread() {
        running = false;
        try {
            if (dis != null) {
                dis.close();
            }
            if (dos != null) {
                dos.close();
            }
            if (fDis != null) {
                fDis.close();
            }
            if (raf != null) {
                raf.close();
            }
        } catch (Exception ex) {
        }
    }

    //??
    private void upPackage(byte[] data) {
        ByteArrayInputStream bias = new ByteArrayInputStream(data);
        DataInputStream biasDis = new DataInputStream(bias);
        int type = 0;
        try {
            type = biasDis.readInt();
        } catch (SocketTimeoutException ex) {
            setError("????Κ±??");
        } catch (IOException ex1) {
            setError("?Τ·?Θ΅???ΔΌ???????????);
        }
        switch (type) {
        case PACKAGE_TYPE_FILENAME: {
            try {
                int length = biasDis.readInt();
                bias.read(dataBuf,0,length);
                fileName = new String(dataBuf,0,length);
                fileSize = biasDis.readLong();
                length = biasDis.readInt();
                if (length !=-1) {
                    bias.read(dataBuf,0,length);
                    message = new String(dataBuf,0,length);
                }
                fileTransStatus = FILE_TRANS_STATUS_WAITFORCONFIRM;
            } catch (SocketTimeoutException ex) {
                setError("????Κ±??");
            } catch (IOException ex) {
                setError("?Τ·?Θ΅???ΔΌ???????????);
            }
            break ;
        }
        case PACKAGE_TYPE_CONTEXT: {
            try {
                int flag = biasDis.readInt();
                int length = biasDis.readInt();
                bias.read(dataBuf,0,length);
                writeToFile(dataBuf,0,length);
                transFileLength += length;
                if (flag == 0) {
                    fileTransStatus = FILE_TRANS_STATUS_SUCCESS;
                    stopThread();
                }
            } catch (SocketTimeoutException ex) {
                setError("????Κ±??");
            } catch (IOException ex) {
                setError("?Τ·?Θ΅???ΔΌ???????????);
            }
            break ;
        }
        }
    }

    //??????
    private void sendFile() {
        int length;
        switch (fileTransStatus) {
        case FILE_TRANS_STATUS_FILENAME: {
            try {
                byte [] buf;
                fileName = aFile.getName();
                fileSize = aFile.length();
                buf = doPackage(null, 0);
                writeToSocket(buf);
                fileTransStatus = FILE_TRANS_STATUS_WAITFORCONFIRM;
            } catch (IOException ex) {
                setError("?Τ·?Θ΅???ΔΌ???????????);
            }
            break ;
        }
        case FILE_TRANS_STATUS_WAITFORCONFIRM: {
            int flag;
            try {
                flag = readFromSocket();
                if (flag == 0) {
                    setError("?Τ·??άΎ???ΔΌ??????");
                } else {
                    fileTransStatus = FILE_TRANS_STATUS_CONTEXT;
                    transFileLength = readLongFromSocket();
                    fDis.skip(transFileLength);
                    aSocket.setSoTimeout(30000);
                }
            } catch (SocketTimeoutException ex) {
                setError("????Κ±??");
            } catch (IOException ex) {
                setError("?Τ·?Θ΅???ΔΌ???????????);
            }
            break ;
        }
        case FILE_TRANS_STATUS_CONTEXT: {
            length = readFromFile(dataBuf,0,CACHE_SIZE);
            try {
                writeToSocket(doPackage(dataBuf, length));
                transFileLength += length;
                if (transFileLength >= fileSize) {
                    fileTransStatus = FILE_TRANS_STATUS_SUCCESS;
                    Thread.sleep(1000);
                    stopThread();
                }
            } catch (IOException ex) {
                setError("?Τ·?Θ΅???ΔΌ???????????);
            } catch (InterruptedException ex1) {

            }
            count++;
            if(count==2){
                //stopThread();
            }
            break ;
        }
        }
    }

    //???ΔΌ?
    private void receiveFile() {
        if (fileTransStatus == FILE_TRANS_STATUS_CONTEXT ||
            fileTransStatus == FILE_TRANS_STATUS_FILENAME) {
            try {
                int length = dis.readInt();
                int len=dis.read(dataBuf,0,length);
                while(len<length){
                    len=len+dis.read(dataBuf,len,length-len);
                }
                    upPackage(dataBuf);
            } catch (SocketTimeoutException ex) {
                setError("????Κ±??");
                ex.printStackTrace();
            } catch (IOException ex) {
                setError("?Τ·?Θ΅???ΔΌ???????????);
            }
        } else if (fileTransStatus == FILE_TRANS_STATUS_CANCELTRANS) {
            try {
                doPackage(null, 0);
            } catch (IOException ex1) {
            }
            setError("?Θ΅?????????");
        }
    }

    //Θ·????????
    public void confirmReceiveFile(boolean flag, String fileName, long off) {
        if (flag) {
            try {

                writeToSocket(1);
                if (off >0) {
                    writeToSocket(off);
                } else {
                    writeToSocket(0L);
                    File aFile=new File(fileName);
                    if(aFile.exists()){
                        System.out.println("*");
                        aFile.delete();
                    }
                }
                raf = new RandomAccessFile(fileName, "rws");
                this.fileName = fileName;
                fileTransStatus = FILE_TRANS_STATUS_CONTEXT;
                fileTransed=true;
                raf.seek(off);
            } catch (FileNotFoundException ex) {
                setError("??????????);
            } catch (IOException ex) {
                setError("?Τ·?Θ΅???ΔΌ???????????);
            }
            transFileLength = off;
        } else {
            try {
                writeToSocket(0);
                writeToSocket(0L);
            } catch (IOException ex) {
                setError("?Τ·?Θ΅???ΔΌ???????????);
            }
        }
    }


//????Ρ΄??????
    public long getTransFileLength() {
        return transFileLength;
    }

//??????ΟΆ
    public void setError(String errorMessage) {
        fileTransStatus = FILE_TRANS_STATUS_FAIL;
        this.errorMessage = errorMessage;
        if (!fileSender&&fileTransed) {
            File tmpFile = new File(fileName + ".tmp");
            try {
                DataOutputStream dos = new DataOutputStream(new
                        FileOutputStream(
                                tmpFile));
                dos.writeLong(transFileLength);
                dos.writeLong(fileSize);
                dos.close();
            } catch (IOException ex) {
            }
        }
        stopThread();
    }


//???Ψ΄???ΟΆ
    public String getErrorMessage() {
        return errorMessage;
    }

//???Ψ΄??Χ΄Μ¬
    public int getStatus() {
        return fileTransStatus;
    }

//????ΔΌ?????
    public boolean isFileSender() {
        return fileSender;
    }

    public void cancelTrans() {
        if (fileTransStatus == FILE_TRANS_STATUS_WAITFORCONFIRM) {
            confirmReceiveFile(false, null, 0);
        }
        setError("?Θ΅?????????");
    }

    public String getMessage() {
        return message;
    }

    public String getIP() {
        return IP;
    }

    public int getPort() {
        return port;
    }

    public String getFileName() {
        if (fileName.length() > 10) {
            return fileName.substring(0, 10) + "..." +
                    fileName.substring(fileName.lastIndexOf('.'),
                                       fileName.length());
        } else {
            return fileName;
        }
    }

    public String getFileTransMessage(){
        if(fileSender){
            return "?????? ΔΏ??P: "+getIP()+" ?ΛΏ?"+getPort();
        }
        else{
            return "?????????: "+getIP();
        }
    }

    private void jbInit() throws Exception {
    }
}
