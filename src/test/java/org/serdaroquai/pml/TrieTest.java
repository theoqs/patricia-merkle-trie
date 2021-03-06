package org.serdaroquai.pml;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.serdaroquai.pml.NodeProto.TrieNode;

public class TrieTest {
	
	Trie<String,String> t;
	
	@Before
	public void setup() {
		init();
	}
	
	private void init() {
		t = new Trie.TrieBuilder<String,String>()
			.keySerializer(Serializer.STRING_UTF8)
			.valueSerializer(Serializer.STRING_UTF8)
			.build();
	}
	
	@Test
	public void testNodesEmpty() {
		List<TrieNode> actual = t.nodes();
		assertEquals(Collections.emptyList(), actual);
	}
	
	@Test
	public void testNodesSingleKeyValueNode() {
		t.put("key", "value");
		List<TrieNode> actual = t.nodes();
		assertEquals(1, actual.size());
		TrieNode node = actual.get(0);
		assertEquals("[206b6579,value]", Common.toString(ByteBuffer.wrap(node.toByteArray())));
	}
	
	@Test
	public void testNodesWithBranchNode() {
		t.put("key", "value");
		t.put("key2", "value2");
		
		List<TrieNode> actual = t.nodes();
		assertEquals("Incorrect number of nodes", 2, actual.size());
		
		Set<String> expected = new HashSet<>();
		expected.add("[006b6579,(88d2..5a59)]");
		expected.add("[,,,[32,value2],,,,,,,,,,,,,value]");

		for (TrieNode node : actual) {
			assertTrue("Missing Node", expected.contains(Common.toString(ByteBuffer.wrap(node.toByteArray()))));
		}
		
	}
	
	@Test
	public void testNodesWithBiggerExample() {
		
		t.put("do", "verb");
		t.put("dog", "puppy");
		t.put("doge", "coin");
		t.put("horse", "stallion");
		
		List<TrieNode> actual = t.nodes();
		assertEquals("Incorrect number of nodes", 6, actual.size());
		
		Set<String> expected = new HashSet<>();
		expected.add("[16,(0e07..af6f)]");
		expected.add("[,,,,(3540..3302),,,,[206f727365,stallion],,,,,,,,]");
		expected.add("[006f,(f86e..4766)]");
		expected.add("[,,,,,,(9dd0..f5e9),,,,,,,,,,verb]");
		expected.add("[17,(c71b..373b)]");
		expected.add("[,,,,,,[35,coin],,,,,,,,,,puppy]");

		for (TrieNode node : actual) {
			assertTrue("Missing Node", expected.contains(Common.toString(ByteBuffer.wrap(node.toByteArray()))));
		}
		
	}
	
	@Test
	public void testEmptyTrieHash32Bytes() {
		ByteBuffer root =t.getRootHash();
		assertEquals(32, root.limit());
	}
	
	@Test
	public void testRootHash32Bytes() {
		// empty trie should also be 32 bytes
		ByteBuffer root = t.getRootHash();
		assertEquals(32, root.limit());
		
		root = t.put("key", "value");
		assertEquals(32, root.limit());
		
		assertEquals(root, t.getRootHash());
		assertNotNull("missing hash in store", t.getStore().get(root));
	}
	
	@Test
	public void testUnorderedInsertionYieldsSameHash() {
		t.put("do", "verb");
		t.put("dog", "puppy");
		t.put("doge", "coin");
		ByteBuffer root1 = t.put("horse", "stallion");
		
		init();
		
		t.put("horse", "stallion");
		t.put("doge", "coin");
		t.put("dog", "puppy");
		ByteBuffer root2 = t.put("do", "verb");
		
		init();
		
		t.put("dog", "puppy");
		t.put("horse", "stallion");
		t.put("do", "verb");
		ByteBuffer root3 = t.put("doge", "coin");
		
		assertEquals(root1, root2);
		assertEquals(root2, root3);
		assertEquals(root3, root1);
	}
	
	@Test
	public void testToMapOldRootHash() {
		
		t.put("do", "verb");
		t.put("dog", "puppy");
		t.put("doge", "coin");
		ByteBuffer oldRootHash = t.put("horse", "stallion");
		
		t.put("do", "no-verb");
		t.put("dog", "no-puppy");
		t.put("doge", "no-coin");
		t.put("horse", "no-stallion");
		t.put("new", "test");
		
		Map<String, String> oldMap = t.toMap(oldRootHash);
		Map<String, String> map = t.toMap();
		
		assertEquals(5, map.size());
		
		assertEquals(4, oldMap.size());
		assertEquals("verb", oldMap.get("do"));
		assertEquals("puppy", oldMap.get("dog"));
		assertEquals("coin", oldMap.get("doge"));
		assertEquals("stallion", oldMap.get("horse"));

	}
	
	@Test
	public void testToMapUpatedKey() {
		
		t.put("do", "verb");
		Map<String, String> map = t.toMap();
		
		assertEquals(1, map.size());
		assertEquals("verb", map.get("do"));
		
		t.put("do", "no-verb");
		map = t.toMap();
		
		assertEquals(1, map.size());
		assertEquals("no-verb", map.get("do"));
	}
	
	@Test
	public void testToMap() {
		
		t.put("do", "verb");
		t.put("dog", "puppy");
		t.put("doge", "coin");
		t.put("horse", "stallion");
		
		Map<String, String> map = t.toMap();
		
		assertEquals(4, map.size());
		assertEquals("verb", map.get("do"));
		assertEquals("puppy", map.get("dog"));
		assertEquals("coin", map.get("doge"));
		assertEquals("stallion", map.get("horse"));

	}
	
	@Test
	public void testQueryOnOldRootHash() {
		
		t.put("do", "verb");
		t.put("dog", "puppy");
		t.put("doge", "coin");
		ByteBuffer root = t.put("horse", "stallion");
		
		assertEquals("stallion", t.get("horse"));
		assertEquals("stallion", t.get(root, "horse"));
		
		t.put("horse", "no-stallion");
		ByteBuffer rootPrime = t.put("doge", "no-coin");
		
		assertEquals("verb", t.get(rootPrime, "do"));
		assertEquals("puppy", t.get(rootPrime, "dog"));
		assertEquals("no-coin", t.get(rootPrime, "doge"));
		assertEquals("no-stallion", t.get(rootPrime, "horse"));
		
		assertEquals("verb", t.get(root, "do"));
		assertEquals("puppy", t.get(root, "dog"));
		assertEquals("coin", t.get(root, "doge"));
		assertEquals("stallion", t.get(root, "horse"));
		
	}
	
	@Test
	public void testRootHashEquality() {
		// same state = same root hash?
		t.put("do", "verb");
		t.put("dog", "puppy");
		t.put("doge", "coin");
		ByteBuffer expected = t.put("horse", "stallion"); // expected roothash
		
		t.put("do", "no-verb");
		ByteBuffer expected2 = t.put("dog", "no-puppy");
		t.put("doge", "no-coin");
		ByteBuffer diffHash = t.put("horse", "no-stallion");
		
		t.put("doge", "coin");
		ByteBuffer actual2 = t.put("horse", "stallion");
		t.put("do", "verb");
		ByteBuffer actual = t.put("dog", "puppy");
		
		assertNotEquals(expected, diffHash);
		assertEquals(expected, actual);
		assertEquals(expected2, actual2);
	}
	
	@Test
	public void someRandomInsertions() {
		
		/*
		do <64 6f> : 'verb'
		dog <64 6f 67> : 'puppy'
		doge <64 6f 67 65> : 'coin'
		horse <68 6f 72 73 65> : 'stallion'

		rootHash: [ <16>, hashA ]
		hashA:    [ <>, <>, <>, <>, hashB, <>, <>, <>, hashC, <>, <>, <>, <>, <>, <>, <>, <> ]
		hashC:    [ <20 6f 72 73 65>, 'stallion' ]
		hashB:    [ <00 6f>, hashD ]
		hashD:    [ <>, <>, <>, <>, <>, <>, hashE, <>, <>, <>, <>, <>, <>, <>, <>, <>, 'verb' ]
		hashE:    [ <17>, hashF ]
		hashF:    [ <>, <>, <>, <>, <>, <>, hashG, <>, <>, <>, <>, <>, <>, <>, <>, <>, 'puppy' ]
		hashG:    [ <35>, 'coin' ]
		*/
		
//		(ba5d..e55a): [16,(0e07..52af)]
//		(0e07..52af): [,,,,(3540..1733),,,,[206f727365,stallion],,,,,,,,]
//		(3540..1733): [006f,(f86e..8547)]
//		(f86e..8547): [,,,,,,(9dd0..bef5),,,,,,,,,,verb]
//		(9dd0..bef5): [17,(c71b..2737)]
//		(c71b..2737): [,,,,,,[35,coin],,,,,,,,,,puppy]
		
		t.put("do", "verb");
		t.put("dog", "puppy");
		t.put("doge", "coin");
		t.put("horse", "stallion");
		
		assertEquals("verb", t.get("do"));
		assertEquals("puppy", t.get("dog"));
		assertEquals("coin", t.get("doge"));
		assertEquals("stallion", t.get("horse"));
		
	}
	
	@Test
	public void testUpdateBranchNodeInsertLongValue() {
//		(bce6..0419): [006b6579,(2b41..e6be)]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]
//		---
//		(3e03..ed0b): [006b6579,(1256..6a75)]
//		(1256..6a75): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,someValue that is really long that does not fit]
						
						
		t.put("key", "value");
		ByteBuffer rootHash = t.put("keylong", "newValue");
		ByteBuffer newRootHash = t.put("key", "someValue that is really long that does not fit");
		
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		
		assertEquals("newValue", t.get("keylong"));
		assertEquals("someValue that is really long that does not fit", t.get("key"));
		
	}
	
	@Test
	public void testUpdateBranchNodeExistingSlot() {
//		(bce6..0419): [006b6579,(2b41..e6be)]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]
//		---
//		(cba0..29e1): [006b6579,(0c33..b722)]
//		(0c33..b722): [,,,,,,(aa92..f5cd),,,,,,,,,,value]
//		(aa92..f5cd): [1c6f,(6cb2..2962)]
//		(6cb2..2962): [,,,,,,[3e67,newValue],[3265,someValue],,,,,,,,,]
		
		t.put("key", "value");
		ByteBuffer rootHash = t.put("keylong", "newValue");
		ByteBuffer newRootHash = t.put("keylore", "someValue");
		
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		
		assertEquals("value", t.get("key"));
		assertEquals("newValue", t.get("keylong"));
		assertEquals("someValue", t.get("keylore"));
		
	}
	
	@Test
	public void testUpdateBranchNodeNewSlot() {
//		(bce6..0419): [006b6579,(2b41..e6be)]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]
//		---
//		(cc0e..c865): [006b6579,(1aeb..aa54)]
//		(1aeb..aa54): [,,,,,,(c4ea..13cc),,,,,,,,,,value]
//		(c4ea..13cc): [,,,,,,,,,,,,[206f6e67,newValue],[206f7265,someValue],,,]
		
		t.put("key", "value");
		ByteBuffer rootHash = t.put("keylong", "newValue");
		ByteBuffer newRootHash = t.put("keymore", "someValue");
		
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		
		assertEquals("value", t.get("key"));
		assertEquals("newValue", t.get("keylong"));
		assertEquals("someValue", t.get("keymore"));
		
	}
	
	@Test
	public void testUpdateExtensionNodeWithRemainingKeySizeOne() {
//		(bce6..0419): [006b6579,(2b41..e6be)]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]
//		---
//		(cded..ab0f): [16b657,(65ae..11d9)]
//		(65ae..11d9): [,,,,,[20,someValue],,,,(2b41..e6be),,,,,,,]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]
		
		t.put("key", "value");
		ByteBuffer rootHash = t.put("keylong", "newValue");
		ByteBuffer newRootHash = t.put("keu", "someValue"); // key <6b 65 79> keu <6b 65 75>
		
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		
		assertEquals("value", t.get("key"));
		assertEquals("newValue", t.get("keylong"));
		assertEquals("someValue", t.get("keu"));
		
	}
	
	@Test
	public void testUpdateExtensionNode() {
//		(bce6..0419): [006b6579,(2b41..e6be)]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]
//		---
//		(365a..55cb): [006b6579,(a24f..41bd)]
//		(a24f..41bd): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,someValue]
		
		t.put("key", "value");
		ByteBuffer rootHash = t.put("keylong", "newValue");
		ByteBuffer newRootHash = t.put("key", "someValue");
		
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		
		assertEquals("newValue", t.get("keylong"));
		assertEquals("someValue", t.get("key"));
		
	}
	
	@Test
	public void testUpdateExtensionNodeWithSubstringPath() {
//		(bce6..0419): [006b6579,(2b41..e6be)]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]
//		----
//		(a677..f634): [006b,(3895..f01e)]
//		(3895..f01e): [,,,,,,(c849..0f38),,,,,,,,,,someValue]
//		(c849..0f38): [1579,(2b41..e6be)]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]
		
		t.put("key", "value");
		ByteBuffer rootHash = t.put("keylong", "newValue");
		ByteBuffer newRootHash = t.put("k", "someValue");
		
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		
		assertEquals("value", t.get("key"));
		assertEquals("newValue", t.get("keylong"));
		assertEquals("someValue", t.get("k"));
		
	}
	
	@Test
	public void testUpdateLeafNodeWithSupersetPath() {
//		(99ff..d771): [206b6579,value]
//		----
//		(bce6..0419): [006b6579,(2b41..e6be)]
//		(2b41..e6be): [,,,,,,[3c6f6e67,newValue],,,,,,,,,,value]

		ByteBuffer rootHash = t.put("key", "value");
		assertEquals("value", t.get("key"));
		
		ByteBuffer newRootHash = t.put("keylong", "newValue");
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		
		assertEquals("value", t.get("key"));
		assertEquals("newValue", t.get("keylong"));
		
	}
	
	@Test
	public void testUpdateLeafNodeWithSubstringPath() {
//		(99ff..d771): [206b6579,value]
//		----
//		(3b47..d929): [006b,(00d0..6d1b)]
//		(00d0..6d1b): [,,,,,,[3579,value],,,,,,,,,,newValue]


		ByteBuffer rootHash = t.put("key", "value");
		assertEquals("value", t.get("key"));
		
		ByteBuffer newRootHash = t.put("k", "newValue");
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		
		assertEquals("value", t.get("key"));
		assertEquals("newValue", t.get("k"));
		
	}
	
	@Test
	public void testUpdateLeafNode() {
//		(99ff..d771): [206b6579,value]
//		---
//		(8403..2b86): [206b6579,newValue]
		
		ByteBuffer rootHash = t.put("key", "value");
		assertEquals("value", t.get("key"));
		
		ByteBuffer newRootHash = t.put("key", "newValue");
		
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		assertNotEquals("value", t.get("key"));
		assertEquals("newValue", t.get("key"));
		
	}
	
	@Test
	public void testPutToBlankRoot() {
//		insert a leaf node into blank state
//		----
//		(99ff..d771): [206b6579,value]
		
		ByteBuffer rootHash = t.getRootHash();
		ByteBuffer newRootHash = t.put("key", "value");
		
		assertNotNull(newRootHash);
		assertNotEquals(rootHash, newRootHash);
		assertEquals("value", t.get("key"));
		
	}
	
	@Test
	public void testCreation() {
		t = new Trie.TrieBuilder<String,String>()
				.keySerializer(Serializer.STRING_UTF8)
				.valueSerializer(Serializer.STRING_UTF8)
				.build();
		
		assertNotNull(t);
		t.put("a", "va");
		assertEquals("va", t.get("a"));
	}
	
	@Test
	public void testCreationWithInitialValues() {
		
		Map<String,String> map = new HashMap<>();
		map.put("do", "verb");
		map.put("dog", "puppy");
		map.put("doge", "coin");
		map.put("horse", "stallion");
		
		t = new Trie.TrieBuilder<String,String>()
				.keySerializer(Serializer.STRING_UTF8)
				.valueSerializer(Serializer.STRING_UTF8)
				.from(map)
				.build();
		
		Set<String> expected = new HashSet<>();
		expected.add("[16,(0e07..af6f)]");
		expected.add("[,,,,(3540..3302),,,,[206f727365,stallion],,,,,,,,]");
		expected.add("[006f,(f86e..4766)]");
		expected.add("[,,,,,,(9dd0..f5e9),,,,,,,,,,verb]");
		expected.add("[17,(c71b..373b)]");
		expected.add("[,,,,,,[35,coin],,,,,,,,,,puppy]");

		List<TrieNode> actual = t.nodes();
		assertEquals("Incorrect number of nodes", 6, actual.size());
		
		for (TrieNode node : actual) {
			assertTrue("Missing Node", expected.contains(Common.toString(ByteBuffer.wrap(node.toByteArray()))));
		}
	}
	
	@Test
	public void testCreationWithSmallInitialValues() {
		Map<String,Boolean> map = new HashMap<>();
		map.put("SW1", true);
	
		// used to have a bug with encode(node, root == node); => now root.equals(node)
		Trie<String,Boolean> trie = new Trie.TrieBuilder<String,Boolean>()
				.keySerializer(Serializer.STRING_UTF8)
				.valueSerializer(Serializer.BOOLEAN)
				.from(map)
				.build();
		
		assertEquals(true, trie.get("SW1"));
	}
	@Test
	public void testEmptyKeyInsertion() {
		t.put("", "emptyValue");
		Set<String> expected = new HashSet<>();
		expected.add("[20,emptyValue]");

		List<TrieNode> actual = t.nodes();
		assertEquals("Incorrect number of nodes", 1, actual.size());

		for (TrieNode node : actual) {
			assertTrue("Missing Node", expected.contains(Common.toString(ByteBuffer.wrap(node.toByteArray()))));
		}

		assertEquals("emptyValue", t.get(""));
	}


	@Test
	public void testLongTypeKeys() {

		Map<Long,String> map = new HashMap<>();
		map.put(0L, "a very long value which can not be compacted");
		map.put(1L, "another very long value which can not be compacted");
		map.put(17L, "yet another very long value which can not be compacted");

		Trie<Long, String> trie = new Trie.TrieBuilder<Long, String>()
				.keySerializer(Serializer.INT64)
				.valueSerializer(Serializer.STRING_UTF8)
				.from(map)
				.build();

		Set<String> expected = new HashSet<>();
		expected.add("[0000000000000000,(daa2..d6e6)]");
		expected.add("[(fb24..bbc7),(1d0d..b61a),,,,,,,,,,,,,,,]");
		expected.add("[(0a9c..0fe2),(53e0..ee0d),,,,,,,,,,,,,,,]");
		expected.add("[31,yet another very long value which can not be compacted]");
		expected.add("[20,another very long value which can not be compacted]");
		expected.add("[20,a very long value which can not be compacted]");

		List<TrieNode> actual = trie.nodes();
		assertEquals("Incorrect number of nodes", 6, actual.size());

		for (TrieNode node : actual) {
			assertTrue("Missing Node", expected.contains(Common.toString(ByteBuffer.wrap(node.toByteArray()))));
		}
	}

	@Test
	public void testDifferenceSameTrie() {
		t.put("do", "verb");
		t.put("dog", "puppy");
		t.put("doge", "coin");
		ByteBuffer rootHash = t.put("horse", "stallion");

		Map<String, String> remove = new HashMap<>();
		Map<String, String> update = new HashMap<>();
		t.difference(rootHash, remove, update);

		assertEquals(Collections.emptyMap(), remove);
		assertEquals(Collections.emptyMap(), update);

	}

	@Test
	public void testDifferenceEmptyTrie() {
		Map<String, String> remove = new HashMap<>();
		Map<String, String> update = new HashMap<>();
		t.difference(t.getRootHash(), remove, update);

		assertEquals(Collections.emptyMap(), remove);
		assertEquals(Collections.emptyMap(), update);

	}

	@Test
	public void testDifferenceSimpleSingleValue() {
		t.put("do", "verb");
		t.put("dog", "puppy");
		ByteBuffer oldHash = t.put("doge", "coin");
		//		(827f..2f44): [00646f,(f86e..4766)]
		//		(f86e..4766): [,,,,,,(9dd0..f5e9),,,,,,,,,,verb]
		//		(9dd0..f5e9): [17,(c71b..373b)]
		//		(c71b..373b): [,,,,,,[35,coin],,,,,,,,,,puppy]

		t.put("horse", "stallion");
		//		(ba5d..e55a): [16,(0e07..52af)]
		//		(0e07..52af): [,,,,(3540..1733),,,,[206f727365,stallion],,,,,,,,]
		//		(3540..1733): [006f,(f86e..8547)]
		//		(f86e..8547): [,,,,,,(9dd0..bef5),,,,,,,,,,verb]
		//		(9dd0..bef5): [17,(c71b..2737)]
		//		(c71b..2737): [,,,,,,[35,coin],,,,,,,,,,puppy]

		Map<String, String> remove = new HashMap<>();
		Map<String, String> update = new HashMap<>();
		t.difference(oldHash, remove, update);

		Map<String, String> expectedUpdate = new HashMap<>();
		expectedUpdate.put("horse", "stallion");

		assertEquals(Collections.emptyMap(), remove);
		assertEquals(expectedUpdate, update);

	}

	@Test
	public void testDifferenceRandom() {
		int maxInitialElements = 500;
		int maxDifferentElements = 100;
		int maxValue = 5;
		int times = 1000;

		Random r = new Random();

		while (times-- > 0) {
			Map<Long, Long> initialValues = new HashMap<>();
			int initialSize = r.nextInt(maxInitialElements);
			while (initialSize-- > 0) {
				initialValues.put(r.nextInt(maxInitialElements) + 0L, r.nextInt(maxValue) + 0L);
			}

			Trie<Long, Long> trie = new Trie.TrieBuilder()
					.keySerializer(Serializer.INT64)
					.valueSerializer(Serializer.INT64)
					.from(initialValues)
					.build();

			ByteBuffer hash = trie.getRootHash();

			Map<Long, Long> finalValues = new HashMap<>(initialValues);
			int changeCount = r.nextInt(maxDifferentElements);
			while (changeCount-- > 0) {
				long key = r.nextInt(maxInitialElements) + 0L;
				long value = r.nextInt(maxValue) + 0L;
				finalValues.put(key, value);
				trie.put(key, value);
			}

			Map<Long, Long> expectedUpdate = finalValues.entrySet().stream()
					.filter(e -> initialValues.get(e.getKey()) != e.getValue())
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

			Map<Long, Long> expectedRemove = initialValues.entrySet().stream()
					.filter(e -> finalValues.get(e.getKey()) != e.getValue())
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

			Map<Long, Long> update = new HashMap<>();
			Map<Long, Long> remove = new HashMap<>();

			trie.difference(hash, remove, update);

			assertEquals(expectedUpdate, update);
			assertEquals(expectedRemove, remove);

		}


	}

}
