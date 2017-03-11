package testPackage;

import java.util.List;
import java.util.Map;

import org.sureassert.uc.annotation.Exemplar;

public class PostalCodeService {

	public Address getAddress(String houseNumber, String postalCode) {
		return null;
	}

	@Exemplar(args = "m:'key1'=[l:1,2,3],'key2'=[l:4,5,6]", expect = "$arg1.get('key2').get(2).equals(7)", debug = "$arg1")
	public static void incrementAll(Map<String, List<Integer>> map) {

		for (List<Integer> intList : map.values()) {
			for (int i = 0; i < intList.size(); i++) {
				intList.set(i, intList.get(i) + 1);
			}
		}
	}

}
