This server only accept two HTTP request methods:
1.GET
2.HEAD

HTTP request methods others then the listed two will be regarded as bad request!!

There is a separate log file saving the access logs containing the following:
1. Client IP address
2. Access Time in GMT timezone
3. Requested File
4. Responses


*Cautions*
This server only runs locally. For those who want to connect to the server has to use the localhost IP address("127.0.0.1")and the selected  port number is 2222.

*How to run*
1. Download and save the src file in your designated directory
2. Compile the "Webserver.java" in your console by typing "javac Webserver.java"
3. Run the "Webserver.java" in your console by typing "java Webserver"
4. Keep it running to keep the server working