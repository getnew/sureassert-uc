package testPackage;

import java.util.Set;

import org.sureassert.uc.annotation.Exemplar;


public class ArrayTest {
	
//	@Exemplar(args={"[m:[m:1=[m:2=3]]=4]"}, expect="")
//	public Map x(Map x) {
//		return x;
//	}
	 
	@Exemplar(args={"s:'123:456', '7:8'"}) 
	public Set testArray(Set x) {
		return x; 
	}    
}
