package com.bookstore.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Klasa Server służy do uruchamiania serwera aplikacji księgarni.
 * Odpowiada za nasłuchiwanie na określonym porcie i przekazywanie przychodzących połączeń do obsługi przez ClientHandler.
 */
public class Server {
    /**
     * Konstruktor klasy Server.
     * Otwiera ServerSocket na określonym porcie, a następnie w pętli akceptuje przychodzące połączenia.
     * Dla każdego połączenia tworzy nowy wątek ClientHandler.
     *
     * @param port Numer portu, na którym serwer będzie nasłuchiwał połączeń.
     */
    public Server (int port) {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: "+clientSocket);

                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
