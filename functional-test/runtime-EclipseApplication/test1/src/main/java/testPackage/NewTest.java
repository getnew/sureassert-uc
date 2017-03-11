package testPackage;

import java.util.Map;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;

import test2.GenericDTO;

//@NamedInstance(name="xyz")
public class NewTest { 
  
	public static void main(String[] args) {
		
	} 
 
	@Exemplars(set={
	@Exemplar(args={"null"}, expect=""),
	@Exemplar(args={"null"}, expect="") })
	public Map[] testArray(Map[] x) {
		return x;
	}
	
	@Exemplar(args={"gd:'test1',2,[pa:true,false]"}, expect="")
	public GenericDTO getADTO(GenericDTO dto) {
		return dto;
	} 
	
	@Exemplar(args="'test '", expect="retval.equals('test stub [']with quotes['] and [[brackets]]')", //
			stubs="ConnectParams.toString='stub [']with quotes['] and [[brackets]]'")
	public String getMyField1(String x) {
		return x + new ConnectParams().toString();         
	} 
	
	@Exemplar(args={"'/[test/] //test// [']test[']'"}, expect="")
	public void x(String x) {
		if (!x.equals("[test] /test/ 'test'"))
			throw new IllegalArgumentException(x);
	}
	     
	@Exemplars(s = { 
			@Exemplar(args="sf:resources/testfile.xml")
	})    
	public String returnAString(String x) {    
		return x; 
	}         
	    
	@Exemplar(args="pa:1,3,2")
	public int[] testIntArray(int[] x) {
		
		return x;  
	}  
	
	@Exemplars(set={
	@Exemplar(args={"null","0","0.0","0s","0f","false","0l","0c","0b","null","null"}, expect=""),
	@Exemplar(args={"null","0","0.0","0s","0f","false","0l","0c","0b","null","null"}, expect="") })
	public String doSomething(String x, int y, double a, short b, float c, boolean d, long e, char f, byte g, int[] h, String[] i) {
		return x;
	}
	
	@Exemplar(args={"null","0","0.0","0s","0f","false","0l","0c","0b","null","null"}, expect="")
	public String doSomething2(String x, int y, double a, short b, float c, boolean d, long e, char f, byte g, int[] h, String[] i) {
		return x;
	}

	public void testInterrupted1() {
	 	 long x = 0;   
		 
		 while (x < 300000000l)
			 x++;  
		  
		 for (long i = 0; i < 30000000000l; i++) {
			 int y = 0;     
			 y++;   
		 }   
		   
		 while (true);//do something else 
	}   
   
	public void testInterrupted2() { 
		while(true);}   
	


    /**
     * <p>Checks whether the String a valid Java number.</p>
     *
     * <p>Valid numbers include hexadecimal marked with the <code>0x</code>
     * qualifier, scientific notation and numbers marked with a type
     * qualifier (e.g. 123L).</p>
     *
     * <p><code>Null</code> and empty String will return
     * <code>false</code>.</p>
     *
     * @param str  the <code>String</code> to check
     * @return <code>true</code> if the string is a correctly formatted number
     */
    public static boolean isNumber(String str) {
        if (str == null || str.equals("")) {
            return false; 
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // deal with any possible sign up front
        int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1) {
            if (chars[start] == '0' && chars[start + 1] == 'x') {
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9')
                        && (chars[i] < 'a' || chars[i] > 'f') 
                        && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    } 
                }
                return true;
            }
        }
        sz--; // don't want to loop to the last char, check it afterwords
              // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) { 
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent   
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp) {
                    // two E's
                    return false;
                }
                if (!foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // we need a digit after the E
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            } 
            if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                // single trailing decimal point after non-exponent is ok
                return foundDigit;
            }
            if (!allowSigns
                && (chars[i] == 'd'
                    || chars[i] == 'D'
                    || chars[i] == 'f'
                    || chars[i] == 'F')) {
                return foundDigit;
            }
            if (chars[i] == 'l'
                || chars[i] == 'L') {
                // not allowing L with an exponent
                return foundDigit && !hasExp;
            }
            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }
    
    public class Counter {
    	
    	private int value;
    	
    	public Counter(int value) {
    		
    		this.value = value;
    	}
    	
    	public int increment() {
    		
    		return ++value;
    	}
    	
    }
}