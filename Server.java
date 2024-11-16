import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static List<String> connectedClients = new ArrayList<>();
    private static Map<String, File> fileList = new HashMap<>(); // HashMap chứa file hash và đối tượng file

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server dang lang nghe tren cong " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            String clientAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            System.out.println("Client ket noi: " + clientAddress);
            connectedClients.add(clientAddress);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String command;
                while ((command = in.readLine()) != null) {
                    switch (command) {
                        case "UPLOAD":
                            String filePath = in.readLine(); // Nhận đường dẫn từ Client
                            File file = new File(filePath);

                            if (file.exists() && file.isFile()) {
                                String fileName = file.getName(); // Lấy tên file từ đường dẫn
                                String fileHash = generateFileHash(fileName, socket.getPort());
                                fileList.put(fileHash, file); // Lưu file vào danh sách
                                System.out.println("File uploaded: " + fileName + " voi hash: " + fileHash + " tu "
                                        + clientAddress);
                                out.println("File uploaded voi hash: " + fileHash);
                            } else {
                                out.println("File khong ton tai hoac khong phai file hop le.");
                            }
                            out.println("END"); // Thông điệp kết thúc danh sách
                            break;

                        case "LIST_CLIENTS":
                            if (connectedClients.isEmpty()) {
                                out.println("Khong co client nao ket noi.");
                            } else {
                                out.println("Danh sach client ket noi:");
                                for (String client : connectedClients) {
                                    out.println(client);
                                }
                            }
                            out.println("END"); // Thông điệp kết thúc danh sách
                            break;
                        case "LIST_FILES":
                            if (fileList.isEmpty()) {
                                out.println("Khong co file nao duoc upload.");
                            } else {
                                out.println("Danh sach file da upload:");
                                for (Map.Entry<String, File> entry : fileList.entrySet()) {
                                    out.println("Hash: " + entry.getKey() + ", File: " + entry.getValue().getName());
                                }
                            }
                            out.println("END"); // Thông điệp kết thúc danh sách
                            break;

                        case "DOWNLOAD":
                            String requestedFileHash = in.readLine();
                            File requestedFile = fileList.get(requestedFileHash);

                            if (requestedFile != null) {
                                out.println("DOWNLOAD_OK");
                                try (FileInputStream fileIn = new FileInputStream(requestedFile)) {
                                    byte[] buffer = new byte[512 * 1024]; // Block size 512KB
                                    int bytesRead;
                                    OutputStream outSocket = socket.getOutputStream();
                                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                                        outSocket.write(buffer, 0, bytesRead);
                                    }
                                    outSocket.flush(); // Đảm bảo dữ liệu được gửi đi hoàn toàn
                                    socket.shutdownOutput(); // Đánh dấu kết thúc truyền dữ liệu
                                } catch (IOException e) {
                                    out.println("Loi khi gui file: " + e.getMessage());
                                }
                            } else {
                                out.println("File khong ton tai hoac khong co quyen tai xuong.");
                            }
                            out.println("END");
                            break;

                        default:
                            out.println("Lenh khong hop le.");
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                connectedClients.remove(clientAddress);
                System.out.println("Client ngat ket noi: " + clientAddress);
            }
        }

        private String generateFileHash(String fileName, int port) {
            return Integer.toHexString((fileName + port).hashCode());
        }
    }
}
