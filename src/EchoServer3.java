import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class EchoServer3 {
//    ArrayList<Socket> socketList = new ArrayList<Socket>();
    HashMap<String, Socket> socketList = new HashMap<>();
    HashMap<Socket, String> socketListReverse = new HashMap<>();
    static HashMap<String, String> userDB = new HashMap<>();

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public EchoServer3(int port) throws IOException {
        ServerSocket srvSocket = new ServerSocket(port);

        while(true) {
            print("Listening at port %d...\n", port);
            Socket clientSocket = srvSocket.accept();

            Thread t = new Thread(()-> {
                try {
                    serve(clientSocket);
                } catch (IOException ex) {
                    print("Connection drop!");
                }

                synchronized (socketList) {
                    socketList.remove(socketListReverse.get(socketList));
                    socketListReverse.remove(socketList);
                }
            });
            t.start();
        }
    }

    private void serve(Socket clientSocket) throws IOException {
        byte[] buffer = new byte[1024];
        print("Established a connection to host %s:%d\n\n",
                clientSocket.getInetAddress(), clientSocket.getPort());

        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        if(authenticate(in, out, clientSocket)){
            while(true){
                String userReceiver = "";
                String msg = "";
                int size = in.readInt();
                while(size > 0) {
                    int len = in.read(buffer, 0, Math.min(size, buffer.length));
                    userReceiver += new String(buffer, 0, len);
                    size -= len;
                }
                size = in.readInt();
                while(size > 0) {
                    int len = in.read(buffer, 0, Math.min(size, buffer.length));
                    msg += new String(buffer, 0, len);
                    size -= len;
                }
                forward(socketListReverse.get(clientSocket), userReceiver);
                forward(msg, userReceiver);
            }
        }
    }

    public boolean authenticate(DataInputStream in, DataOutputStream out, Socket clientSocket) throws IOException {
        //getting user
        String user = "";
        String password = "";
        byte[] buffer = new byte[1024];
        int size = in.readInt();
        while(size > 0) {
            int len = in.read(buffer, 0, Math.min(size, buffer.length));
            user += new String(buffer, 0, len);
            size -= len;
        }
        buffer = new byte[1024];
        size = in.readInt();
        while(size > 0) {
            int len = in.read(buffer, 0, Math.min(size, buffer.length));
            password += new String(buffer, 0, len);
            size -= len;
        }
        //check if there's user in DB and check password of user
        String msg = "";
        Boolean inDB = false;
        if(userDB.containsKey(user) && userDB.get(user).equals(password)){
            inDB = true;
            msg = "Y";
            synchronized (socketList) {
                socketList.put(user,clientSocket);
                socketListReverse.put(clientSocket, user);
            }
        }else{
            msg = "N";
        }
        try {
            out.writeInt(msg.length());
            out.write(msg.getBytes(), 0, msg.length());
        } catch (IOException ex) {
            print("Unable to forward message\n");
        }
        return inDB;
    }

    private void forward(String msg, String user){
        synchronized (socketList) {
            if(!socketList.containsKey(user)){
                //user is not online
            }else{
                Socket socket = socketList.get(user);
                try {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeInt(msg.length());
                    out.write(msg.getBytes(), 0, msg.length());
                } catch (IOException ex) {
                    print("Unable to forward message to %s:%d\n",
                            socket.getInetAddress().getHostName(), socket.getPort());
                }
            }

        }
    }

    public static void main(String[] args) throws IOException {
        userDB.put("ali","12345");
        userDB.put("bula","qwerty");
        int port = Integer.parseInt("12345");
        new EchoServer3(port);
    }
}