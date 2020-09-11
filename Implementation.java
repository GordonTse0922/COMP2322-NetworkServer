import jdk.jfr.ContentType;
import java.io.*;
import java.net.*;
import java.net.http.HttpHeaders;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Implementation implements  Runnable {
    Socket clientsocket = null;

    public Implementation(Socket cs) {
        this.clientsocket = cs;
    }

    public void run() {
        try {
            procesing();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void procesing() throws Exception {
        boolean isHEAD = false;
        DataOutputStream outToClient = new DataOutputStream(clientsocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader((clientsocket.getInputStream())));
        String clientrequest = "";
        while (!((clientrequest = in.readLine()) == null)) {//to loop till there is not more request line
            StringTokenizer tokenizedLine = new StringTokenizer(clientrequest);
            String requestType = tokenizedLine.nextToken();//to find out the HTTP request type
            String requestedfile = tokenizedLine.nextToken();//to store the requested file name
            String headerLine = "";
            String temp = "";
            String requestedTime="";
            boolean Check_Modified=false;
            String[] headers=null;
            while ((temp = in.readLine())!=null && temp.length() != 0 ) {//loop to record the request headerline
                headerLine += temp + "\n";
            }
            headers=headerLine.split(" ");
            int size=headers.length;
            for(int i=0;i<size;i++) {//loop to find whether the request headers include "If-Modified-Since"
                if (headers[i].equals("If-Modified-Since:")) {//if yes, compare their date
                    Check_Modified = true;
                    requestedTime = headers[i + 1];
                    File file = new File(requestedfile);
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, MM dd yyyy HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Date LastModifiedtime = sdf.parse(String.valueOf(file.lastModified()));
                    Date time = sdf.parse(requestedTime);
                    System.out.println(requestedTime);
                    System.out.println(file.lastModified());
                    if (LastModifiedtime.before(time)) {//if the Lastmodified time of the file is before the time given by user
                        Check_Modified = false;
                        //System.out.println(Check_Modified);
                    } else if (time.before(LastModifiedtime)) {//vice versus
                        Check_Modified = true;
                       // System.out.println(Check_Modified);
                    }
                    break;
                }
            }
            switch (requestType) {
                case "GET": {//if the HTTP request type is "GET"
                    //System.out.println(clientrequest + "\n");  for testing
                    sendfile(outToClient, requestedfile, isHEAD,Check_Modified);
                }
                break;
                case "HEAD": {//if the HTTP request is "HEAD"
                    isHEAD = true;
                    //System.out.println(clientrequest);
                    sendfile(outToClient, requestedfile, isHEAD,Check_Modified);
                }break;
                default: {//If it is other HTTP request, send Bad Request code
                    send400(outToClient);
                }break;
            }
        }
        System.out.println("Client Disconnected:" + clientsocket);
        outToClient.close();
        in.close();
        clientsocket.close();
    }

    public void send400(DataOutputStream outToClient) throws IOException{//function to construct 400 response
        FileInputStream file = new FileInputStream("/Users/hoyintse/Desktop/COMP2322_Project/src/400.txt");
        int bytes = (int) file.getChannel().size();
        String responseline="HTTP/1.1 400 Bad Request \r\n";
        outToClient.writeBytes(responseline);
        outToClient.writeBytes("Content-Length:" +bytes+ "\r\n");
        outToClient.writeBytes("\r\n");
        byte[] buffer = new byte[bytes];
        file.read(buffer);
        outToClient.write(buffer, 0, bytes);
        file.close();
        String info = clientsocket.toString();
        String ip = info.substring(info.indexOf("/") + 1, info.indexOf(","));
        String Time = getTime(null);
        Log(ip, Time, null, "HTTP/1.1 400 Bad Request \r\n");//Log the actions taken
    }
    public void sendfile(DataOutputStream outToClient, String requestedItem, boolean head, boolean Check_modified) throws Exception {
        boolean fileExist = true;
        try {//this loop is to check whether the file exist
            if (requestedItem.startsWith("/") == true) requestedItem = requestedItem.substring(1);
            FileInputStream infile = new FileInputStream(requestedItem);
        } catch (Exception e) {
            fileExist = false;
        }
        sendBytes(outToClient, requestedItem, fileExist, head,Check_modified);
    }

    public static String getTime(String File) {
        //I found that the HTTP time should use GMT timezone as standard
        //Therefore, all the time would be adjusted and recorded as in GMT time
        if(File!=null) {// if the file do exist, get the last modified time of the file
            File file = new File(File);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MM dd yyyy HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String time= sdf.format(file.lastModified());
            return time;
        }
        //if not, get current time
        SimpleDateFormat Timeformat = new SimpleDateFormat(" dd/MM/yyyy HH:mm:ss");
        Timeformat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = new Date();
        String time = Timeformat.format(date);
        return time;
    }


    public void sendBytes(DataOutputStream outToClient, String requestedItem, boolean fileExist, boolean ishead,boolean Modified) throws Exception {
        String responseline = null;
        int numofBytes = 0;
        if (fileExist) {//if file exist, do the following
            System.out.println("File exist");
            FileInputStream infile = new FileInputStream(requestedItem);
            numofBytes = (int) infile.getChannel().size();
            if(!Modified){//if the last modified time is after the time given by client
                responseline = "HTTP/1.1 200 OK \r\n";//should be responseline = "HTTP/1.1 304 Not Modified \r\n";
                outToClient.writeBytes(responseline);//However, as our server does not have cache it should always be 200 status code
                outToClient.writeBytes("Last-Modified:"+getTime(requestedItem)+"\r\n");//therefore, the code is sending the file as similar as below
                outToClient.writeBytes("Content-Length:" + numofBytes+ "\r\n");
                outToClient.writeBytes("\r\n");
                if (!ishead) {
                    byte[] buffer = new byte[numofBytes];
                    infile.read(buffer);
                    outToClient.write(buffer, 0, numofBytes); //write file data from buffer into output stream to client
                    System.out.println("File successfully sent to client\n");
                    infile.close();
                }
            }
            else{// if the last modified time is before the time given by client
            responseline = "HTTP/1.1 200 OK \r\n";//if file exist, status code would be 200
            outToClient.writeBytes(responseline);
            if (requestedItem.endsWith(".jpg")) {//if the requested file is jpg file, response with the following response line
                outToClient.writeBytes("Content-Type: image/jpeg\r\n");
                outToClient.writeBytes("Last-Modified:"+getTime(requestedItem)+"\r\n");
                outToClient.writeBytes("Content-Length:" + numofBytes + "\r\n");
                outToClient.writeBytes("\r\n");
            } else if (requestedItem.endsWith(".gif")) {//If the requested file is gif file, response with the following response line
                outToClient.writeBytes("Content-Type: image/gif\r\n");
                outToClient.writeBytes("Last-Modified:"+getTime(requestedItem)+"\r\n");
                outToClient.writeBytes("Content-Length:" + numofBytes + "\r\n");
                outToClient.writeBytes("\r\n");
            } else if ((requestedItem.endsWith(".html"))) {//if the requested file is html file, response with the following response line
                outToClient.writeBytes("Content-Type: text/html\r\n");
                outToClient.writeBytes("Last-Modified:"+getTime(requestedItem)+"\r\n");
                outToClient.writeBytes("Content-Length:" + numofBytes + "\r\n");
                outToClient.writeBytes("\r\n");
            } else if (requestedItem.endsWith(".txt")) {//if the requested file is txt file, response with the following response line
                outToClient.writeBytes("Content-Type: text/plain\r\n");
                outToClient.writeBytes("Last-Modified:"+getTime(requestedItem)+"\r\n");
                outToClient.writeBytes("Content-Length:" + numofBytes + "\r\n");
                outToClient.writeBytes("\r\n");
            }
            if (!ishead) {// if the HTTP request is "HEAD",it does not send the data(message-body)
                byte[] buffer = new byte[numofBytes];
                infile.read(buffer);
                outToClient.write(buffer, 0, numofBytes); //write file data from buffer into output stream to client
                System.out.println("File successfully sent to client\n");
                infile.close();
            }
            }
        } else if (!fileExist) {//if the file does not exist, send 404 status code
            FileInputStream file = new FileInputStream("/Users/hoyintse/Desktop/COMP2322_Project/src/404.txt");
            int bytes = (int) file.getChannel().size();
            responseline = "HTTP/1.1 404 NOT FOUND \r\n";
            outToClient.writeBytes(responseline);
            outToClient.writeBytes("Content-Type: text/plain\r\n");
            outToClient.writeBytes("Last-Modified:"+getTime("/Users/hoyintse/Desktop/COMP2322_Project/src/404.txt")+"\r\n");
            outToClient.writeBytes("Content-Length:" + bytes + "\r\n");
            outToClient.writeBytes("\r\n");
            byte[] buffer = new byte[bytes];
            file.read(buffer);
            outToClient.write(buffer, 0, bytes);
            file.close();
            System.out.println("File successfully sent to client\n");
        }
        String info = clientsocket.toString();
        //String ip = info.substring(info.indexOf("/") + 1, info.indexOf(","));
        String ip=clientsocket.getInetAddress().getHostAddress();
        String Time = getTime(null);
        Log(ip, Time, requestedItem, responseline);//Log the actions taken
    }


    public static void Log(String IP, String time, String file, String response) {
        try {
            FileWriter write = new FileWriter("/Users/hoyintse/Desktop/COMP2322_Project/logs/AccessLog.txt", true);
            write.write(IP + "  " + time + "  " + "  " + file + "  " + response);
            write.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

