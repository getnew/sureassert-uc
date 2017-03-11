package testPackage;
  
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.NamedClass;
import org.sureassert.uc.annotation.TestDouble;
import org.sureassert.uc.annotation.TestState;
      
@TestDouble(replaces=JMSServerDelegate.class)
public class JMSServerDelegate { 
         
	@TestState(value="l:JMSMessage/instance1")
	private List<JMSMessage> messages = new ArrayList<JMSMessage>();
	
//	public JMSServerDelegate() { 
//		messages.add(new JMSMessage("testHost", Collections.<String, String>emptyMap(), 1));    
//	}  
	
	public JMSServerDelegate() {
	}     

	public JMSServerDelegate(String host, int port) {
	}      
	
	public void sendMessage(ConnectParams connectParams, String payload, Map<String, String> properties) throws RemoteException {
	}
	
	@Exemplar(name="theNum2")
	public int x() {
		return 2;
	}   
	    
	@Exemplar(name="JMSServerDelegate/1", args={"'testPayload1'", "m:'k1'='v1'"}, //
			expect={"theNum2", "=(this.messages.get(0).getNumber1(), 105)", "=(this.messages.get(1).getNumber1(), 2)"})
	public int sendMessage(String payload, Map<String, String> properties) {
		       
		assert payload != null;  
		messages.add(new JMSMessage(payload, properties, 2));    
                                    
		assert !findMessagesContaining(payload).isEmpty();  
		return messages.size();
	}
              
	@Exemplar(args = "'test'") 
	public List<JMSMessage> findMessagesContaining(String matchStr) {
		        
		List<JMSMessage> matchedMessages = new ArrayList<JMSMessage>();
		for (JMSMessage message : messages) {
			if (message.getPayload().contains(matchStr)) {
				matchedMessages.add(message);     
			}   
		}        
		return matchedMessages;  
	}        
	
	public String toString() { 
		return "numMessages=" + messages.size();
	} 

	@NamedClass(name="JMSMessage2")
	public static class JMSMessage {
		
		private String payload; 
 
		private Map<String, String> properties; 
    
		private int number1;  
		
		@Exemplar(args = {"'testPayload'", "m:['k1']=['v1']", "i:105"}, //
				name="JMSMessage/instance1")    
		public JMSMessage(String payload, Map<String, String> properties, int number1) {
			this.number1 = number1;
			this.payload = payload;
			this.properties = new HashMap<String, String>(properties);
		}
                
		@Exemplar(instance="new JMSMessage2('testPayload2', m:'k3'='v8', 110)")
		public String toString() {   
			return "payload: " + payload.trim() + "; properties: " + properties.toString();
		}        
  
		public String getPayload() { 
			return payload;
		}    
		
		public Map<String, String> getProperties() {
			return properties;
		}

		public int getNumber1() {
			return number1;
		}  
	}
}
