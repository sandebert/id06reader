public class Apdu {

	public static final int MAX_EXPECTED_LENGTH = 256;
	public static final int MAX_EXPECTED_LENGTH_LONG = 65536;
	public static final byte INS_ERASE_BINARY = (byte)0x0E;
	public static final byte INS_VERIFY = (byte)0x20;
	public static final byte INS_MANAGE_CHANNEL = (byte)0x70;
	public static final byte INS_EXTERNAL_AUTHENTICATE = (byte)0x82;
	public static final byte INS_GET_CHALLENGE = (byte)0x84;
	public static final byte INS_INTERNAL_AUTHENTICATE = (byte)0x88;
	public static final byte INS_INTERNAL_AUTHENTICATE_ACS = (byte)0x86;
	public static final byte INS_SELECT_FILE = (byte)0xA4;
	public static final byte INS_READ_BINARY = (byte)0xB0;
	public static final byte INS_READ_RECORDS = (byte)0xB2;
	public static final byte INS_GET_RESPONSE = (byte)0xC0;
	public static final byte INS_ENVELOPE = (byte)0xC2;
	public static final byte INS_GET_DATA = (byte)0xCA;
	public static final byte INS_WRITE_BINARY = (byte)0xD0;
	public static final byte INS_WRITE_RECORD = (byte)0xD2;
	public static final byte INS_UPDATE_BINARY = (byte)0xD6;
	public static final byte INS_PUT_DATA = (byte)0xDA;
	public static final byte INS_UPDATE_DATA = (byte)0xDC;
	public static final byte INS_APPEND_RECORD = (byte)0xE2;
	public static final byte CLS_PTS = (byte)0xFF; // Class for PTS
}
