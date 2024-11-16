import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)) {

            System.out.println("Ket noi den server thanh cong.");

            String command;
            while (true) {
                System.out.println("Chon lenh (UPLOAD, LIST_CLIENTS, LIST_FILES, DOWNLOAD, EXIT):");
                command = scanner.nextLine().toUpperCase();

                if (command.equals("EXIT")) {
                    break;
                }

                out.println(command);

                switch (command) {
                    case "UPLOAD":
                        System.out.print("Nhap duong dan file: ");
                        String filePath = scanner.nextLine();
                        out.println(filePath); // Gửi đường dẫn đến server

                        File file = new File(filePath);
                        if (!file.exists() || !file.isFile()) {
                            System.out.println("File khong ton tai.");
                            break; // Dừng nếu file không hợp lệ
                        }

                        // Nhận phản hồi từ server
                        System.out.println("Phan hoi tu server: " + in.readLine());
                        break;

                    case "LIST_CLIENTS":
                    case "LIST_FILES":
                        String response;
                        while ((response = in.readLine()) != null && !response.equals("END")) {
                            System.out.println(response);
                        }
                        break;
                    case "DOWNLOAD":
                        System.out.print("Nhap hash file can tai: ");
                        String fileHash = scanner.nextLine();
                        out.println(fileHash);

                        String serverResponse = in.readLine();
                        if ("DOWNLOAD_OK".equals(serverResponse)) {
                            try (FileOutputStream fileOut = new FileOutputStream("downloaded_" + fileHash)) {
                                byte[] buffer = new byte[512 * 1024]; // Block size 512KB
                                int bytesRead;
                                InputStream socketIn = socket.getInputStream();

                                // Đọc dữ liệu cho đến khi gặp EOF
                                while ((bytesRead = socketIn.read(buffer)) != -1) {
                                    fileOut.write(buffer, 0, bytesRead);
                                }
                                System.out.println("Tai file hoan tat: downloaded_" + fileHash);
                            } catch (IOException e) {
                                System.out.println("Loi khi tai file: " + e.getMessage());
                            }
                        } else {
                            System.out.println("Phan hoi tu server: " + serverResponse);
                        }
                        break;

                    default:
                        System.out.println("Lenh khong hop le.");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}