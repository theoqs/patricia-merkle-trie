package org.serdaroquai.pml;

import java.util.HashMap;
import java.util.Map;

import org.serdaroquai.pml.NodeProto.TrieNode;

import com.google.protobuf.ByteString;

public class MemoryStore implements Store{

	Map<ByteString, ByteString> map = new HashMap<>();
	
	@Override
	public ByteString get(ByteString hash) {
		return map.get(hash);
	}
	
	@Override
	public ByteString put(TrieNode n) {
		byte[] bytes = n.toByteArray();
		byte[] hashBytes = Util.sha256(bytes); 
		ByteString hash = ByteString.copyFrom(hashBytes);
		TrieNode hashNode = TrieNode.newBuilder().addItem(hash).build();
		ByteString hashNodeBytes = hashNode.toByteString();
		map.put(hashNodeBytes, n.toByteString());
		return hashNodeBytes;
	}

	@Override
	public void put(ByteString hash, ByteString encoded) {
		map.put(hash, encoded);
	}

}