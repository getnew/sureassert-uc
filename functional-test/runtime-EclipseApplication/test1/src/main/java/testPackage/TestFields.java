package testPackage;

import org.sureassert.uc.annotation.Exemplar;

public class TestFields {

	@Exemplar(expect="10")
	public int test() {
		return Fields.TEST_FIELD_1;
	}      

	@Exemplar(expect="10")
	public int test2() {
		return Fields.get1();
	} 
 
	@Exemplar(expect="10")
	public int test3() {
		return new Fields().TEST_FIELD_2;
	}

	@Exemplar(expect="10")
	public int test4() {
		return new Fields().get2();
	}
} 
