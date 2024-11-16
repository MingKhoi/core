import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private static int peerPort;

    public static void main(String[] args) {
        System.out.print("Nhập port cho peer: ");
        Scanner scanner = new Scanner(System.in);
        peerPort = scanner.nextInt();
        scanner.nextLine(); // Bỏ qua dòng mới

        new PeerServer(peerPort).start(); // Bắt đầu server peer

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String response;
            while ((response = in.readLine()) != null) {
                System.out.println(response);
                String userInput = scanner.nextLine();
                out.println(userInput);

                if (response.contains("Các client đang chia sẻ file")) {
                    // Bắt đầu tải file từ các client khác
                    String[] peers = in.readLine().split(", ");
                    downloadFile(peers, "downloaded_file");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadFile(String[] peers, String fileName) {
        for (String peer : peers) {
            String[] parts = peer.replace("[", "").replace("]", "").split(":");
            if (parts.length != 2) {
                System.out.println("Địa chỉ peer không hợp lệ: " + peer);
                continue;
            }

            String host = parts[0].trim();
            int port;
            try {
                port = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                System.out.println("Lỗi chuyển đổi cổng: " + parts[1]);
                continue;
            }

            System.out.println("Đang kết nối tới peer: " + host + ":" + port);
            try (Socket peerSocket = new Socket(host, port);
                    InputStream in = peerSocket.getInputStream();
                    FileOutputStream fos = new FileOutputStream(fileName, true)) {

                byte[] buffer = new byte[512 * 1024]; // 512kB block
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                System.out.println("Hoàn thành tải từ peer: " + host + ":" + port);
            } catch (IOException e) {
                System.out.println("Lỗi khi tải file từ peer: " + host + ":" + port);
                e.printStackTrace();
            }
        }
    }

    static class PeerServer extends Thread {
        private int port;

        public PeerServer(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handleClient(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleClient(Socket socket) {
            try (OutputStream out = socket.getOutputStream();
                    FileInputStream fis = new FileInputStream("shared_file")) { // Tên file cần chia sẻ
                byte[] buffer = new byte[512 * 1024]; // 512kB block
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
