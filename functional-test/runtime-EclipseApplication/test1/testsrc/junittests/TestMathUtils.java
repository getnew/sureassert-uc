package junittests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sureassert.uc.annotation.Stubs;

import test2.GenericDTO;
import testPackage.MathUtils;

/**
 * 
 * @author Nathan
 * 
 */
public class TestMathUtils {
     
  //private static final Logger logger = LoggerFactory.getLogger(TestMathUtils.class);  
	
	MathUtils __instance_;  
	 
	@Before  
	public void setup() {
		__instance_ = new MathUtils();
	}        
	
	//@Test
  public void hogMemory()
  {
     ArrayList<byte[]> buffers;
     int i;
     byte buffer[];

    buffers = (ArrayList<byte[]>) new ArrayList<byte[]>();
     
     for (i = 1024 * 1024; i > 0; )
     {
        try
        {
           buffer = new byte[i];
           
           buffers.add(buffer);
        }
        catch (OutOfMemoryError e)
        {
           i >>= 1;
        }
     }
  }

	      
	@Test  
	public void testAdd() {
		long result = (long) MathUtils.add(1, 2);
		assertTrue("Can't add", result == 3); 
	}   
	  
	@Test 
	public void testMod() { 
		int mod = MathUtils.mod(10, 4);
		assertTrue("testMod returned " + mod, mod == 2);
	}
	  
	/**
	 * Test the calculate PI function but stub out the 
	 * remote PI record service call.
	 */
	@Test 
	@Stubs(stubs="MathUtils.testX()=18")
	public void testCalculatePI() { 
		
		int x = MathUtils.testX();
		assertTrue("testX returned " + x, x == 18);
	}
	  
	@Test 
	public void testTestX() { 
		int x = MathUtils.testX();
		assertTrue("testX returned " + x, x == 18);
	}
  
	@Test  
	public void testAdd2() {
		long result = (long) MathUtils.add(1, 2l);
		assertTrue("a2", result == 3);   
	}
	
	@Test
	@Stubs(stubs="GenericDTO.toString()='llama'")
	public void testStubbing() {  
		        
		GenericDTO genericDTO = new GenericDTO("test1", 2, new boolean[] {true, false});
		String toStr = genericDTO.toString();
		assertTrue(toStr, toStr.equals("llama")); 
	}            
	 
	public static void main(String[] args) {
		  
		Result result = JUnitCore.runClasses(TestMathUtils.class);
		System.out.println(result.toString());
	}

    @Test
    public void German_Umlauts_are_treated_correctly() throws Exception {
        assertThat("\u00C4".getBytes(), is(new byte[] { (byte) 0x0c3, (byte)0x084 }));
    }

}
