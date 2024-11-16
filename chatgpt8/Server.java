import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int SERVER_PORT = 12345;
    private static Map<String, List<String>> fileDatabase = new HashMap<>(); // Lưu mã hash và danh sách client

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server đang chạy trên cổng: " + SERVER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String clientAddress = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
                System.out.println("Kết nối mới từ: " + clientAddress);

                out.println("Chọn chức năng: 1. Upload 2. Xem danh sách file 3. Download");

                String option;
                while ((option = in.readLine()) != null) {
                    switch (option) {
                        case "1":
                            handleUpload(clientAddress);
                            break;
                        case "2":
                            sendFileList();
                            break;
                        case "3":
                            handleDownload();
                            break;
                        default:
                            out.println("Lựa chọn không hợp lệ.");
                    }
                    out.println("Chọn tiếp hoặc ngắt kết nối.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleUpload(String clientAddress) throws IOException {
            out.println("Nhập tên file để upload:");
            String fileName = in.readLine();
            String hashKey = hashFileName(fileName, clientSocket.getPort());
            fileDatabase.putIfAbsent(hashKey, new ArrayList<>());
            fileDatabase.get(hashKey).add(clientAddress);
            System.out.println("File " + fileName + " được upload bởi " + clientAddress + " với mã hash " + hashKey);
            out.println("Upload thành công với mã hash: " + hashKey);
        }

        private void sendFileList() {
            if (fileDatabase.isEmpty()) {
                out.println("Chưa có file nào được upload.");
                return;
            }
            out.println("Danh sách file đã upload:");
            for (Map.Entry<String, List<String>> entry : fileDatabase.entrySet()) {
                out.println("Mã hash: " + entry.getKey() + " | Clients: " + entry.getValue());
            }
        }

        private void handleDownload() throws IOException {
            out.println("Nhập mã hash của file cần tải:");
            String hashKey = in.readLine();
            if (fileDatabase.containsKey(hashKey)) {
                List<String> clients = fileDatabase.get(hashKey);
                out.println("Các client đang chia sẻ file: " + clients);
                // Gửi thông tin các client để client khác có thể tự kết nối và tải file
            } else {
                out.println("Không tìm thấy file.");
            }
        }

        private String hashFileName(String fileName, int port) {
            return String.valueOf(fileName.hashCode() + port);
        }
    }
}
