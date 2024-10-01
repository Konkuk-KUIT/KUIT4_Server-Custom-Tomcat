package webserver;

import db.MemoryUserRepository;
import db.Repository;
import http.util.HttpRequestUtils;
import http.util.IOUtils;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static http.util.HttpRequestUtils.parseQueryParameter;
import static http.util.IOUtils.readData;

public class RequestHandler implements Runnable{
    Socket connection;
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());
    private final Repository repository;
    public RequestHandler(Socket connection) {
        this.connection = connection;
        this.repository = MemoryUserRepository.getInstance();
    }

    @Override
    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()){
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            DataOutputStream dos = new DataOutputStream(out);

            String startLine = br.readLine();
            String[] startLines = startLine.split(" ");
            String method = startLines[0];
            String targetUrl = startLines[1];
            int requestContentLength = 0;
            String cookie = "";
            byte[] body = new byte[0];
            while (true) {
                final String line = br.readLine();
                if (line.equals("")) {
                    break;
                }
                // header info
                if (line.startsWith("Content-Length")) {
                    requestContentLength = Integer.parseInt(line.split(": ")[1]);
                }
                if (line.startsWith("Cookie")) {
                    cookie = line.split(": ")[1].split(";")[0];
                }
            }

            Path targetPath = Paths.get("./webapp" + targetUrl);
            if(method.equals("GET") && targetUrl.equals("/index.html")){
                body = Files.readAllBytes(targetPath);
            }

            if(method.equals("GET") && targetUrl.equals("/")){
                body = Files.readAllBytes(Paths.get("webapp/index.html" ));
            }
            if (targetUrl.equals("/user/form.html")) {
                body = Files.readAllBytes(targetPath);
            }

            if (targetUrl.equals("/user/signup")) {
                String query = readData(br,requestContentLength);
                Map<String, String> queryParameter = parseQueryParameter(query);
                User user = new User(queryParameter.get("userId"), queryParameter.get("password"), queryParameter.get("name"), queryParameter.get("email"));
                repository.addUser(user);
                response302Header(dos,"/index.html");
                return;
            }

            if(targetUrl.equals("/user/login.html")){
                body = Files.readAllBytes(Paths.get("webapp/user/login.html"));
            }
            if(targetUrl.equals("/login_failed.html")){
                body = Files.readAllBytes(Paths.get("./webapp/user/login_failed.html"));
            }

            if(targetUrl.equals("/user/login")){
                String query = readData(br,requestContentLength);
                Map<String, String> queryParameter = parseQueryParameter(query);
                User loginUser = new User(queryParameter.get("userId"), queryParameter.get("password"), queryParameter.get("name"), queryParameter.get("email"));
                User userById = repository.findUserById(loginUser.getUserId());
                if(userById != null){
                    if(userById.getPassword().equals(loginUser.getPassword())){
                        response302HeaderWithCookie(dos,"/index.html");
                        System.out.println("cookie = " + cookie);
                    }
                }
                else{
                    response302Header(dos,"/login_failed.html");
                }
                return;
            }
            if(targetUrl.equals("/user/userList")){
                if(!cookie.equals("logined=true")){
                    response302Header(dos,"/user/login.html");
                    return;
                }
                body = Files.readAllBytes(Paths.get("./webapp/user/list.html"));
            }

            if(method.equals("GET") && targetUrl.endsWith(".css")){
                body = Files.readAllBytes(targetPath);
                response200HeaderWithCss(dos, body.length);
                responseBody(dos,body);
                return;
            }
            response200Header(dos, body.length);
            responseBody(dos, body);

        } catch (IOException e) {
            log.log(Level.SEVERE,e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String path)  {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: " + path + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void response200HeaderWithCss(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }
    private void response302HeaderWithCookie(DataOutputStream dos, String path) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: " + path + "\r\n");
            dos.writeBytes("Set-Cookie: logined=true" + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }


}
