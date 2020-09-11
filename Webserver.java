import java.io.*;
import java.net.*;
import java.net.http.HttpRequest;

public class Webserver {
     public static void main(String[] args) throws IOException{
         ServerSocket ss=null;
         int port=2222;
         try {
             ss = new ServerSocket(port);
         }
         catch(IOException e){
             System.out.println("Could not listen on port:3333");
             System.exit(1);
         }
         while(true) {
                 Socket cs=ss.accept();
                 System.out.println("A new client is connected : "+cs+"\n");
                 Thread thread = new Thread(new Implementation(cs));
                 thread.start();
                 //System.out.println("A new thread for client is started \n");
         }

     }
}

