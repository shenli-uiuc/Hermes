package hermes.util;

import java.util.Arrays;

public class ByteKeeper{
    private static final int BASE_LEN = 1024;
    private byte [] data = null;
    private int length = 0;
    private int maxLen = 0;

    public ByteKeeper(byte [] data){
        this.data = Arrays.copyOf(data, data.length);     
        this.length = data.length;
        this.maxLen = data.length;   
    }

    public ByteKeeper(){
        this.data = new byte[BASE_LEN];
        this.length = 0;
        this.maxLen = this.BASE_LEN;
    }

    public int read(byte [] buf, int bufLen, int offset){
        int from = offset;
        int readCnt = 0;
        if(offset + bufLen <= this.length){
            readCnt = bufLen;
        }
        else if (offset < this.length){
            readCnt = this.length - offset;
        }
        else{
            return -1;
        }
        System.arraycopy(this.data, offset, buf, 0, readCnt);
        return readCnt;
    }

    public void write(byte [] buf, int bufLen, int offset){
       //TODO 
    }

    public void append(byte [] buf, int bufLen){
        if(this.maxLen < bufLen + this.length){
            int newMaxLen = (bufLen + this.length ) * 2;
            byte [] tmp = new byte[newMaxLen];
            System.arraycopy(this.data, 0, tmp, 0, this.length);
            this.maxLen = newMaxLen;
            this.data = tmp;            
        }

        System.arraycopy(buf, 0, this.data, this.length, bufLen);
        this.length = this.length + bufLen;
    }

    public byte [] getBytes(){
        return Arrays.copyOf(this.data, this.length);
    }

    public static void main(String argv[]){
        String str = "ccnx:/ndn/uiuc.edu";

        byte [] strByte = str.getBytes();
        ByteKeeper sender = new ByteKeeper(strByte);
        ByteKeeper receiver = new ByteKeeper();

        int readCnt = 0;
        int bufLen = 3;
        byte [] buf = new byte[bufLen];
        int offset = 0;
        while(true){
            readCnt = sender.read(buf, bufLen, offset);
            if(readCnt < 0)
                break;
            offset += readCnt;
            receiver.append(buf, bufLen);
        }
        String res = new String(receiver.getBytes());
        System.out.println(res);
    }
    
}
