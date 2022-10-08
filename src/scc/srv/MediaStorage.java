package scc.srv;

import java.util.List;

public interface MediaStorage {
	String upload(byte[] contents);

	byte[] download(String media_id);

	List<String> list();
}
