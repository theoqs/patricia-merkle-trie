package org.serdaroquai.pml;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.protobuf.ByteString;

public class NibbleStringTest {
	
	private static final byte[] ODD_LENGTH_BYTES = new byte[]{(byte) 0x12, (byte) 0x34};
	private static final byte[] EVEN_LENGTH_BYTES = new byte[]{(byte) 0x00, (byte) 0x34};
	private static final byte[] SAME_BYTES = new byte[]{(byte) 0x00, (byte) 0x34, (byte) 0x34};
	private static final byte[] LONG_BYTES = new byte[]{(byte) 0x10, (byte) 0x12, (byte) 0x34,
			(byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde};
	private static final byte[] EMPTY_BYTES = new byte[0];

	@Test
	public void testSubstring() {
		NibbleString n = NibbleString.unpack(ByteBuffer.wrap(ODD_LENGTH_BYTES));
		final NibbleString s = n.substring(1); // 34
//		assertThrows(IllegalArgumentException.class, () -> s.nibbleAt(-1));
		assertTrue(s.nibbleAsChar(0) == '3');
		assertTrue(s.nibbleAsChar(1) == '4');
//		assertThrows(IllegalArgumentException.class, () -> s.nibbleAt(2));
	}
	
	@Test
	public void testSubstringEquals() {
		NibbleString n = NibbleString.unpack(ByteBuffer.wrap(SAME_BYTES));
		final NibbleString s1 = n.substring(0,2); // 34
		final NibbleString s2 = n.substring(2,4); // 34
		final NibbleString s3 = n.substring(1,3); // 43
		
		assertEquals(s1,s2);
		assertNotEquals(s2, s3);
		assertNotEquals(s1, s3);
	}
	
	@Test
	public void testSubstringOfSubstring2() {
		ByteBuffer b = ByteBuffer.wrap("doge".getBytes(StandardCharsets.UTF_8)); // 64 6f 67 65
		NibbleString n = NibbleString.from(b);
		
		assertEquals("646f6765", n.toString());
		n = n.substring(1);
		assertEquals("46f6765", n.toString());
		n = n.substring(1);
		assertEquals("6f6765", n.toString());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSubstringOfSubstring() {
		NibbleString n = NibbleString.unpack(ByteBuffer.wrap(LONG_BYTES));
		final NibbleString s = n.substring(2,9); // 23 45 67 8
		
		assertEquals(7, s.size());
//		assertThrows(IllegalArgumentException.class, () -> s.nibbleAt(-1));
		assertTrue(s.nibbleAsChar(0) == '2');
		assertTrue(s.nibbleAsChar(1) == '3');
		assertTrue(s.nibbleAsChar(2) == '4');
		assertTrue(s.nibbleAsChar(3) == '5');
		assertTrue(s.nibbleAsChar(4) == '6');
		assertTrue(s.nibbleAsChar(5) == '7');
		assertTrue(s.nibbleAsChar(6) == '8');
//		assertThrows(IllegalArgumentException.class, () -> s.nibbleAt(7));
		
		final NibbleString s2 = s.substring(2,5); // 45 6
		assertEquals(3, s2.size());
//		assertThrows(IllegalArgumentException.class, () -> s2.nibbleAt(-1));
		assertTrue(s2.nibbleAsChar(0) == '4');
		assertTrue(s2.nibbleAsChar(1) == '5');
		assertTrue(s2.nibbleAsChar(2) == '6');
		s2.nibbleAsChar(3); // throws
//		assertThrows(IllegalArgumentException.class, () -> s2.nibbleAt(3));
	}
	
	@Test
	public void testOddLengthCompactDecode() {
		NibbleString n = NibbleString.unpack(ByteBuffer.wrap(ODD_LENGTH_BYTES));
		assertTrue(n.nibbleAsChar(0) == '2');
		assertTrue(n.nibbleAsChar(1) == '3');
		assertTrue(n.nibbleAsChar(2) == '4');
		assertEquals(n.size(), 3);
	}
	
	@Test
	public void testEvenLengthCompactDecode() {
		NibbleString n = NibbleString.unpack(ByteBuffer.wrap(EVEN_LENGTH_BYTES));
		assertTrue(n.nibbleAsChar(0) == '3');
		assertTrue(n.nibbleAsChar(1) == '4');
		assertEquals(n.size(), 2);
	}
	
	@Test
	public void testOddLengthCompactEncode() {
		ByteBuffer expected = ByteBuffer.wrap(ODD_LENGTH_BYTES);
		NibbleString n = NibbleString.unpack(expected);
		ByteBuffer actual = NibbleString.pack(n, false);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testEvenLengthCompactEncode() {
		ByteBuffer expected = ByteBuffer.wrap(EVEN_LENGTH_BYTES);
		NibbleString n = NibbleString.unpack(expected);
		ByteBuffer actual = NibbleString.pack(n, false);
		assertEquals(expected, actual);
	}

	@Test
	public void testEmptyBytesPack() {
		NibbleString n = NibbleString.from(ByteBuffer.wrap(EMPTY_BYTES));
		ByteBuffer actual = NibbleString.pack(n, true);
		ByteBuffer expected = ByteBuffer.wrap(new byte[]{0x20});
		assertEquals(expected, actual);
	}

	@Test
	public void testEmptyBytesUnpack() {
		NibbleString expected = NibbleString.from(Common.EMPTY);
		NibbleString actual = NibbleString.unpack(ByteBuffer.wrap(new byte[]{0x20}));
		assertEquals(expected, actual);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBoundsCheck() {
		final NibbleString n = NibbleString.unpack(ByteBuffer.wrap(EVEN_LENGTH_BYTES));
		n.nibbleAsChar(2);
	}
	
	@Test
	public void testByteBufferEquality() {
		ByteBuffer b1 = ByteBuffer.wrap(new byte[] {0,1,2});
		ByteBuffer b2 = ByteBuffer.wrap(new byte[] {3,4,5});
		ByteBuffer b3 = ByteBuffer.wrap(new byte[] {0,1,2});
		assertNotEquals(b1, b2);
		assertEquals(b1, b3);
		
		b1 = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));
		b2 = ByteString.copyFromUtf8("test").asReadOnlyByteBuffer();
		
		assertEquals(b1, b2);
	}

	@Test
	public void testNibbleEquality() {
		NibbleString n1 = NibbleString.from(ByteBuffer.wrap(new byte[]{0,1,2,3,4,5}));
		NibbleString n1Prime = NibbleString.from(ByteBuffer.wrap(new byte[]{0,1,2,3,4,5}));
		NibbleString n2 = NibbleString.from(ByteBuffer.wrap(new byte[]{5,4,3,2,1,0}));

		assertEquals(n1, n1Prime);
		assertNotEquals(n1, n2);
		assertNotEquals(n1Prime, n2);
	}

	@Test
	public void testByteArrayEquality() {
		List<Byte> b1 = Arrays.asList(Byte.valueOf((byte) 0x01), Byte.valueOf((byte) 0x02));
		List<Byte> b1Prime = Arrays.asList(Byte.valueOf((byte) 0x01), Byte.valueOf((byte) 0x02));
		List<Byte> b2 = Arrays.asList(Byte.valueOf((byte) 0x03), Byte.valueOf((byte) 0x04));

		assertEquals(b1, b1Prime);
		assertNotEquals(b1, b2);
		assertNotEquals(b1Prime, b2);
	}
}
