package test4;

import java.sql.SQLException;
import java.sql.Wrapper;

import org.sureassert.uc.annotation.Exemplar;

public class Test6 {
//	
//	public int x(int y) {
//		if (y > 5) 
//			return (int)new Integer(y);
//		else 
//			return 5 - y;  
//	}  
	 
	@Exemplar(args="2")
	public void test(int x) {
		
		if (x == 1) {
			return;
		} else if (x == 2) {
			return;
		} else if (x == 3) {
			return; 
		}
	}  
	
	public class ClassX implements Wrapper {

		public <T> T unwrap(Class<T> iface) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
} 
