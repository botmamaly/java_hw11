package Client;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONObject;
  
public class Client{
	private Socket client;
	private String user;
	private SimpleDateFormat dateaFormat;
	private Thread messageReceive;/*accept from others message*/
	private BufferedReader br;/*from keyboard user's input*/
	private BufferedWriter bw;/*write to server*/
	private BufferedReader socketBr;/*from server*/
	private Client(){
		try {
			client=new Socket("127.0.0.1",8888);
			dateaFormat = new SimpleDateFormat("[yyyy/MM/dd hh:mm:ss]");
			br = new BufferedReader(new InputStreamReader(System.in));
			bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			socketBr=new BufferedReader(new InputStreamReader(client.getInputStream()));
		} catch (UnknownHostException e) {
			System.out.println("Connect failed"+e);
		} catch (Exception e) {
			System.out.println("Connect failed\n"+e);
		}	
	}
	public void excute(){
		try 
		{
			getUserName();
			printHistory();
			/*Get other users message*/
			messageReceive = new Thread(new Runnable(){
	            public void run() {
	                String message;
	                try {
	                    while ((message = socketBr.readLine()) != null) 
	                    {
	                    	JSONObject jsonMessage=new JSONObject(message);
	                        System.out.println(jsonMessage.get("Time")+" "+
	                    	jsonMessage.get("Sender")+": "+jsonMessage.get("Message"));
	                    }
	                } catch (IOException e) {
	                	System.out.println("leave the chat room.");
	                }
	            }
	        });
			messageReceive.start();
			String userMessage;
			while (true) {
				userMessage=br.readLine();
				String time=getDateTime();
				bw.write(time+"\n");
				bw.flush();
				bw.write(user+"\n");
				bw.flush();
				bw.write(userMessage+"\n");
				bw.flush();
				if(userMessage.equals("bye")){
					messageReceive.interrupt();			
					break;
				}
	        }
			finsih();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	public void printHistory() throws IOException{
		System.out.println("-----History Start-----");
		int count=Integer.parseInt(socketBr.readLine());
		String history=socketBr.readLine();
		JSONObject jsonHistory=new JSONObject(history);
		for(int i=0;i<count;i++){
			JSONObject jsonMessage=jsonHistory.getJSONObject("history"+Integer.toString(i));
			System.out.println(jsonMessage.get("Time")+" "+
			    	jsonMessage.get("Sender")+": "+jsonMessage.get("Message"));
		}
		System.out.println("-----History End-----");
	}
	public void getUserName() throws IOException{
		System.out.println("Input your name:");
		user=br.readLine();
		String time=getDateTime();
		bw.write(time+"\n");
		bw.flush();
		bw.write(user+"\n");
		bw.flush();
		bw.write(" in!"+"\n");
		bw.flush();
	}
	public String getDateTime(){
		Date date = new Date();
		String strDate = dateaFormat.format(date);
		return strDate;
    }
	public void finsih(){
		try {
			br.close();
		    bw.close();
		    socketBr.close();
		    client.close();
		} catch (IOException e) {
			System.out.println("Close failed! "+e);
		}
	}
	public static void main(String[] args){
		Client user=new Client();
		user.excute();
	}
}