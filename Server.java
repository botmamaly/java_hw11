package Server;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import org.json.JSONObject;
import org.sqlite.*;

public class Server{
	private ServerSocket myServerSocket;
    public ArrayList<BufferedWriter> userList;
    private Connection con;
	public Server(){
        try {
        	myServerSocket = new ServerSocket(8888);//build a TCP server
        	userList=new ArrayList<BufferedWriter>();
        	con = getConnection();
            createTable(con);/*create table*/
        	System.out.println("Create sever success!");
        } catch (Exception e) {
        	System.out.println("Create sever failure!"+e);
        }
	}
	public Connection getConnection() throws SQLException
    {
        SQLiteConfig config = new SQLiteConfig(); 
        config.setSharedCache(true);
        config.enableRecursiveTriggers(true);   
        SQLiteDataSource ds = new SQLiteDataSource(config); 
        ds.setUrl("jdbc:sqlite:java.db");
        return ds.getConnection();
    }
	public void createTable(Connection con )throws SQLException{
        String sql = "DROP TABLE IF EXISTS test ;create table test (time string,sender string,msg string); ";
        Statement stat = con.createStatement();
        stat.executeUpdate(sql);
    }
	public void insert(Connection con,String time,String sender,String msg)throws SQLException{
        String sql = "insert into test (time,sender,msg) values(?,?,?)";
        PreparedStatement pst = null;
        pst = con.prepareStatement(sql);
        int idx = 1 ; 
        pst.setString(idx++, time);
        pst.setString(idx++, sender);
        pst.setString(idx++, msg);
        pst.executeUpdate();    
    }
	public void selectAll(BufferedWriter wr)throws SQLException, IOException{
        String sql = "select * from test";
        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery(sql);
        int count = 0;
        JSONObject history=new JSONObject();
        while(rs.next()){
        	JSONObject tmp=new JSONObject();
        	tmp.put("Time",rs.getString("time"));
			tmp.put("Sender",rs.getString("sender"));
		    tmp.put("Message",rs.getString("msg"));
		    history.put("history"+Integer.toString(count), tmp);
            count++;
        }
        wr.write(count+"\n");
        wr.write(history+"\n");
    }
	public void excute(){
        while(true)
        {
			try{
				Socket socket = myServerSocket.accept();
				BufferedWriter clientBw=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				userList.add(clientBw);
				ServerThread st=new ServerThread(socket,clientBw);
				st.start();
			}catch (Exception e) {
				System.out.println("Connect to client failed");
				System.out.println(e);
			}
	    }
	}
	public static void main(String[] args){
		
		Server server=new Server();
		server.excute();
    }
	public class ServerThread extends Thread
    {
        private Socket socket;/*server's socket*/
        private BufferedReader reader;/*read from client*/
        private BufferedWriter client;/*write to client*/
        private String time;/*message's time*/
        private String sender;/*who send the message*/
        private String message;
		public ServerThread(Socket socket,BufferedWriter wr){
		    this.socket=socket;
		    this.client=wr;
		    try {
			  reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
		    }catch (Exception e) {
			  e.printStackTrace();
		    }
		}
		public void getMessage() throws IOException{
			time=reader.readLine();
			sender=reader.readLine();
			message=reader.readLine();
		}
		public void run(){
			JSONObject jsonMsg;
			try {
				  getMessage();/*accept the user's name*/
				  selectAll(client);
				  insert(con,time,"Server",sender+message);/*insert message to history*/
				  jsonMsg=new JSONObject();
				  jsonMsg.put("Time",time);
				  jsonMsg.put("Sender","Server");
			      jsonMsg.put("Message",sender+message);
			      tellAll(jsonMsg);
				  while (true)
				  {
					  getMessage();
					  jsonMsg=new JSONObject();
					  jsonMsg.put("Time",time);
					  if(message.equals("bye"))
					  {
					    	userList.remove(client);
					    	jsonMsg.put("Sender","Server");
					    	jsonMsg.put("Message",sender+" out!");
					    	insert(con,time,"Server",sender+" out!");/*insert message to history*/
					    	tellAll(jsonMsg);
					    	break;
					  }
					  jsonMsg.put("Sender",sender);
					  jsonMsg.put("Message",message);
					  insert(con,time,sender,message);/*insert message to history*/
					  tellAll(jsonMsg);
				  }
				  finsih();
			} catch (Exception e) {
				userList.remove(client);
			}
		}
		public void tellAll(JSONObject msg){
			for(int i=0;i<userList.size();i++)
			{
				try {
					BufferedWriter bw=userList.get(i);
					bw.write(msg+"\n");
				    bw.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}	
			}		
		}
		public void finsih(){
			try {
				reader.close();
			    client.close();
			    socket.close();
			} catch (IOException e) {
				System.out.println("Close failed! "+e);
			}	
		}
	 }
}