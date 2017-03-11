package testPackage;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.sureassert.uc.annotation.Exemplars;
import org.sureassert.uc.annotation.SINType;
import org.sureassert.uc.annotation.Exemplar;

import test2.GenericDTO;

public class TestClassB {
	
	private final String x;
	
	@SINType(prefix="wm")
	public static <K, V> WeakHashMap<K, V> getWeakHashMap(Map<K, V> map) {
		
		return new WeakHashMap<K, V>(map);
	}

	@SINType(prefix="gd")
	public static GenericDTO getGenericDTO(String myField1, int myField2, boolean[] myField3) {
		
		return new GenericDTO(myField1, myField2, myField3);
	}
	
	@Exemplar(name="TestClassB/1", args="'test'")
	public TestClassB(String x) {
		this.x = x;
	}  
	    
	@Exemplar(instance="new TestClassB('abc')", args="'5'", name="testClassB", //
			expect="'My First Error Message':retval.equals(5)")
	public int getANamedInstance(String num) {
		return Integer.parseInt(num); 
	}       
	 
	@Exemplars(set={
	@Exemplar(args="3", expect="2"),
	@Exemplar(args="6", expect="6")
	})
	public int x(int y) {
		if (y > 5) 
			return (int)new Integer(y);
		else 
			return 5 - y;  
	}  
	   
	@Exemplar(instance="TestClassB/1!")
	public String toString() { 
		return x;   
	}  
	  
	@Exemplar(args="new java/lang/String('test')")
	public String newTest(String str) {
		return str;
	}
	   
	@Exemplar  
	public void sendAMessage() throws RemoteException {
		JMSServerDelegate jms = new JMSServerDelegate("somewhere", 567);
		jms.sendMessage("testPayload1", Collections.<String,String>emptyMap());
	}
 
	@Exemplar(args={"new JMSServerDelegate() {messages=[l:'mes1']}", "new ConnectParams() {host='testHost',port=291}", "'payload123'"}, //
			expect={"retval.host.equals('testHost')", "=(retval.port,291)"}) 
	public ConnectParams sendAMessage2(JMSServerDelegate jmsServer, ConnectParams connectParams, String payload) throws RemoteException {
		assert jmsServer != null : "jmsServer cannot be null";
		jmsServer.sendMessage(connectParams, payload, Collections.<String,String>emptyMap());
		assert jmsServer.toString().equals("numMessages=1") : "jmsServer.toString=" + jmsServer.toString();
		return connectParams;    
	}
}          

 
