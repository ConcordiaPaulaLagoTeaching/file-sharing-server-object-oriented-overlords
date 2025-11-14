import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoadTest {

    public static void main(String[] args) throws Exception {
        int threads = 1000;
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    Socket s = new Socket("localhost", 12345);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    in.readLine();
                    in.readLine();
                    out.println("LIST");
                    in.readLine();
                    s.close();
                } catch (Exception e) {
                }
            }).start();
        }
        System.out.println("Started " + threads + " clients.");
    }
}
