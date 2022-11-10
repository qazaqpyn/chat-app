import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class EchoViewer {
    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public EchoViewer(String serverIP, int port) throws IOException {
        print("Connecting to %s:%d\n", serverIP, port);

        Socket socket = new Socket(serverIP, port);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        Scanner scanner = new Scanner(System.in);
        boolean loggedIn = false;

        while(!loggedIn){
            loggedIn=login(out, scanner, in);
        }

        while(true) {
            String receivedUser = receiveString(in);
            String receivedMessage = receiveString(in);
            print(receivedUser+": " + receivedMessage + "\n");
        }
    }

    public boolean login(DataOutputStream out, Scanner scanner, DataInputStream in) throws IOException {
        print("Input login and press ENTER\n");
        String message = scanner.nextLine();
        sendString(message, out);
        print("Input password and press ENTER\n");
        String password = scanner.nextLine();
        sendString(password, out);
        String receivedMessage = receiveString(in);
        if(receivedMessage.equals("Y")){
            return true;
        }else{
            return false;
        }
    }

    public void sendString(String string, DataOutputStream out) throws IOException {
        int len = string.length();
        out.writeInt(len);
        out.write(string.getBytes(), 0, len);
        out.flush();
    }

    public String receiveString(DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        String str = "";
        int len = in.readInt();
        while (len > 0) {
            int l = in.read(buffer, 0, Math.min(len, buffer.length));
            str += new String(buffer, 0, l);
            len -= l;
        }
        return str;
    }

    public static void main(String[] args) throws IOException {
        String serverIP = "127.0.0.1";
        int port = Integer.parseInt("12345");
        new EchoViewer(serverIP, port);
    }
}
