package test2;

import java.util.Arrays;

import org.sureassert.uc.annotation.NamedClass;
import org.sureassert.uc.annotation.Exemplar;

public class GenericDTO extends ValueObject {
	
	private final String myField1; 
	  
	private final int myField2;
  
	private final boolean[] myField3; 
	   
	private boolean switched = false;        
	   
	@Exemplar(args={"s:val1", "i:2", "pa:[b:true],[b:false],[b:true]"}, //
	name="myGenericDTO1")    
	public GenericDTO(String myField1, int myField2, boolean[] myField3) {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","<init>","java.lang.String,int,[Z"), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();
		this.myField1 = myField1; 
		this.myField2 = myField2;   
		this.myField3 = myField3;       
	}              
	 
	public String testNew(String x) {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testNew","java.lang.String"), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testNew","java.lang.String"))) return (java.lang.String)com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testNew","java.lang.String"));
		 return x; 
	}  
	
	public void testLoop() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop",""))) { com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop","")); return; }
		// method start
		for (int x = 0; x < 10; x++) {com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();
			System.out.println("first line");
			System.out.println("second line");
			System.out.println(x);
		}
	}
	
	public void testLoop2() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop2",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop2",""))) { com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop2","")); return; }
		// method start
		for (int x = 0; x < 10; x++)com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();
	}
	
	public void testLoop3() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop3",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop3",""))) { com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","testLoop3","")); return; }
		// method start
		int x = 0;
		while (x < 10)com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();
	}
    
	@Exemplar(expect="retval.equals(new java/lang/String('val1').toLowerCase())")
	public String getMyField1() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField1",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField1",""))) return (java.lang.String)com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField1","")); 
		return myField1;
	}  
	  
	@Exemplar(expect="1") 
	public int return1() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","return1",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","return1",""))) return Integer.parseInt(com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","return1","")).toString());
		return 1;
	}
  
	@Exemplar(expect="2") 
	public int getMyField2() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField2",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField2",""))) return Integer.parseInt(com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField2","")).toString());
		return myField2;
	}

	public boolean[] getMyField3() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField3",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField3",""))) return (boolean[])com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getMyField3",""));
		return myField3;
	}
	
	public void setSwitched() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","setSwitched",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","setSwitched",""))) { com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","setSwitched","")); return; }
		switched = true;
	}
	
	public boolean isSwitched() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","isSwitched",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","isSwitched",""))) return Boolean.parseBoolean(com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","isSwitched","")).toString());
		return switched;
	}  

	@Override
	protected Object[] getImmutableState() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getImmutableState",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getImmutableState",""))) return (java.lang.Object[])com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","getImmutableState",""));

		return new Object[] {myField1, myField2, Arrays.toString(myField3)};
	}
	
	public String toString() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","toString",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","toString",""))) return (java.lang.String)com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTO","toString",""));
		return super.toString();
	}
}
