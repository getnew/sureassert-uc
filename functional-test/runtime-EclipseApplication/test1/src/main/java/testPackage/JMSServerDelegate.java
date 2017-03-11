package testPackage;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JMSServerDelegate {
	
	private final String host; 
	private final int port;  
  
	public JMSServerDelegate(String host, int port) {
		this.host = host;        
		this.port = port; 
	}   
	         
	public List<JMSMessage> findMessagesContaining(String matchStr) throws RemoteException {
		  
		throw new RemoteException("coudn't connect to server");
	}    
 
	public int sendMessage(String payload, Map<String, String> properties) throws RemoteException {
		
		throw new RemoteException("coudn't connect to server");
	}  

	public void sendMessage(ConnectParams connectParams, String payload, Map<String, String> properties) throws RemoteException {

		throw new RemoteException("coudn't connect to server");
	}
	     
	public static class JMSMessage {
		 
		private String payload; 
 
		private Map<String, String> properties; 
    
		private int number1;  
		 
		public JMSMessage(String payload, Map<String, String> properties, int number1) {
			this.number1 = number1;
			this.payload = payload;
			this.properties = new HashMap<String, String>(properties);
		} 
                
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
	
//	public String toString() {
//		return "host " + host + " | port " + port;
//	} 
}
