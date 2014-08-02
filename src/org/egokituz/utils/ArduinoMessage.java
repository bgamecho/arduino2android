package org.egokituz.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import android.util.Log;


/**
 * @author Xabier Gardeazabal
 *
 * This class builds a message according to the specified message format
 * and retrieves the different fields 
 */
public class ArduinoMessage{
	private static final String TAG = "MessageReading";
	
	public static final int STX = 0x02; // Start of Text flag 
	public static final int MSGID_PING = 0x26; // Message ID: ping type
	public static final int MSGID_DATA = 0x27; // Message ID: Data type
	public static  final int ETX = 0x03; // End of Text flag

	public String devName = "";
	public String devMAC = "";
	public long timestamp;

	private byte stx;
	private byte msgId;
	private byte frameSeqNum;
	private byte dlc;
	private String payload;
	private byte crc_bytes[] = new byte[8];
	private byte etx;
	private long crc;
	//private ByteBuffer auxBuffer = ByteBuffer.allocate(Long.SIZE);
	
	public ArduinoMessage(){
		
	}

	public ArduinoMessage(byte[] buffer) throws BadMessageFrameFormat {
		int bufferIndex = 0;

		try {
			stx 				= buffer[bufferIndex++];
			if(stx != STX)
				throw new BadMessageFrameFormat();
			msgId 				= buffer[bufferIndex++];
			if(msgId != MSGID_DATA && msgId != MSGID_PING)
				throw new UnknownMessageID("Received "+msgId+". Expected MSGID_DATA or _PING");
			frameSeqNum			= buffer[bufferIndex++];
			dlc 				= buffer[bufferIndex++];
			payload = new String(buffer, bufferIndex, dlc);
			bufferIndex+=dlc;
			crc_bytes [0]= buffer[bufferIndex++];
			crc_bytes [1]= buffer[bufferIndex++];
			crc_bytes [2]= buffer[bufferIndex++];
			crc_bytes [3]= buffer[bufferIndex++];
			etx = buffer[bufferIndex];
			if(etx != ETX)
				throw new IncorrectETXbyte();

			ByteBuffer auxBuffer = ByteBuffer.wrap(crc_bytes);
			auxBuffer = ByteBuffer.wrap(crc_bytes);
			auxBuffer.order(ByteOrder.LITTLE_ENDIAN);
			crc = auxBuffer.getLong();

			long checksum = getChecksum(payload.getBytes());

			if(checksum != crc){
				Log.e(TAG, "Payload contains erros: ecpected: "+crc+" calc."+checksum);
				throw new BadMessageFrameFormat();
			}
		} catch (Exception e) {
			/* An exception should only happen if the buffer is too short and we walk off the end of the bytes.
			 * Because of the way we read the bytes from the device this should never happen, but just in case
			 * we'll catch the exception */
			Log.d(TAG, "Failure building MessageReading from byte buffer, probably an incopmplete or corrupted buffer");
			//e.printStackTrace();
			throw new BadMessageFrameFormat();

		}
	}

	public ArduinoMessage(String devName, byte[] auxBuff) throws BadMessageFrameFormat {
		this(auxBuff);
		this.devName = devName;
	}
	
	public List<Byte> pingMessage(int sequenceNumber){
		int size = 0;
		//byte[] outBuffer = new byte[16];
		List<Byte> outBuffer = new ArrayList<Byte>();
		outBuffer.add((byte) STX);
		outBuffer.add((byte) MSGID_PING);
		outBuffer.add((byte) sequenceNumber);
		outBuffer.add((byte) size);
		for(byte b : "".getBytes())
			outBuffer.add((byte) b);
		long CRC = getChecksum("".getBytes());
		for( byte b : longToBytes(CRC))
			outBuffer.add((byte) b);
		outBuffer.add((byte) ETX);
		

		return outBuffer;
	}

	public int size(){
		int result = 1+1+1+4+dlc+4+1; //STX + ID + Seq.Num + 4*DLC + payload size + 4*CRC + ETX
		return result;
	}

	public String getPayload() {
		return payload;
	}

	public int getFrameNum(){
		return frameSeqNum;
	}
	
	public int getMessageID(){
		return msgId;
	}

	/**
	 * Calculates the CRC (Cyclic Redundancy Check) checksum value of the given bytes
	 * according to the CRC32 algorithm.
	 * @param bytes 
	 * @return The CRC32 checksum
	 */
	private long getChecksum(byte bytes[]){

		Checksum checksum = new CRC32();

		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);

		// get the current checksum value
		long checksumValue = checksum.getValue();

		return checksumValue;
	}
	
	public byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(x);

		byte[] result = new byte[4];
		byte[] b = buffer.array();
		for(int i=0; i<4; i++)
			result[i] = b[i];
		return result;
	}
}