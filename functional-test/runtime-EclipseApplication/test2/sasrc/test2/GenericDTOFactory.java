package test2;

import org.sureassert.uc.annotation.NamedInstance;
import org.sureassert.uc.annotation.Exemplar;

public class GenericDTOFactory {   
    
	private static GenericDTO myGenericDTO2 = new GenericDTO("val15", 16, new boolean[] {false, true, true});
	     
	@NamedInstance(name="test123") 
	public static GenericDTO myGenericDTO3 = new GenericDTO("val15", 18, new boolean[] {false, true, true});
	 
	public static final int myInt1 = 18;                
	     
	@Exemplar(name="testDTO3") 
	private static GenericDTO getTestDTO3() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","getTestDTO3",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","getTestDTO3",""))) return (test2.GenericDTO)com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","getTestDTO3",""));  
		return new GenericDTO("val15", 18, new boolean[] {false, true, true});
	}        
	 
	public static GenericDTO getMyGenericDTO3() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","getMyGenericDTO3",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","getMyGenericDTO3",""))) return (test2.GenericDTO)com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","getMyGenericDTO3",""));
		return myGenericDTO3;
	}    
	   
	public void x() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","x",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","x",""))) { com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.GenericDTOFactory","x","")); return; }
		
	}
} 