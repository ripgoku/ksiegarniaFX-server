package com.bookstore.server;

import com.bookstore.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import static com.bookstore.server.DataBase.*;

public class ClientHandler extends Thread{
    final Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message request = (Message) objectInputStream.readObject();
                // Obsługa żądania
                Message response = processRequest(request);
                objectOutputStream.writeObject(response);
            }
        } catch (SocketException e) {
            System.out.println("Connection reset: " + socket);
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + socket);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                objectInputStream.close();
                objectOutputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Message processRequest(Message request) {
        switch (request.getType())
        {
            case REGISTER -> {
                try {
                    RegistrationData registrationData = (RegistrationData) request.getData();

                    // Przetwarzanie danych rejestracyjnych
                    databaseAnswer databaseAns = registerUser(registrationData);

                    // Tworzenie odpowiedzi
                    if (databaseAns == databaseAnswer.SUCCES) {
                        return new Message(MessageType.SERVER_MESSAGE, "Rejestracja zakończona sukcesem");
                    } else if (databaseAns == databaseAnswer.REGISTER_ERROR_LOGIN) {
                        return new Message(MessageType.SERVER_MESSAGE, "Użytkownik o takim loginie już istnieje!");
                    } else if (databaseAns == databaseAnswer.REGISTER_ERROR_EMAIL) {
                        return new Message(MessageType.SERVER_MESSAGE, "Użytkownik o takim emailu już istnieje!");
                    } else if (databaseAns == databaseAnswer.ERROR) {
                        return new Message(MessageType.SERVER_MESSAGE, "Rejestracja nieudana");
                    }
                } catch (Exception e) {
                    return new Message(MessageType.SERVER_MESSAGE, "Błąd podczas rejestracji: " + e.getMessage());
                }
            }
            case LOGIN -> {
                try {
                    LoginData loginData = (LoginData) request.getData();
                    UserData userData = new UserData();

                    databaseAnswer databaseAns = loginUser(loginData, userData);

                    if (databaseAns == databaseAnswer.SUCCES) {
                        return new Message(MessageType.LOGIN, userData);
                    } else if (databaseAns == databaseAnswer.ERROR) {
                        return new Message(MessageType.SERVER_MESSAGE_ERROR, "Nie ma użytkownika z podanym loginem i hasłem!");
                    }
                } catch (Exception e) {
                    return new Message(MessageType.SERVER_MESSAGE, "Błąd podczas logowania: " + e.getMessage());
                }
            }
            case VIEW_BOOKS -> {
                try {
                    List<Book> books = new ArrayList<>();

                    databaseAnswer databaseAns = getBooks(books);
                    if (databaseAns == databaseAnswer.SUCCES) {
                        return new Message(MessageType.VIEW_BOOKS, (Serializable) books);
                    } else if (databaseAns == databaseAnswer.ERROR) {
                        return new Message(MessageType.SERVER_MESSAGE_ERROR, "Brak książek do wyświetlenia!");
                    }
                } catch (Exception e) {
                    return new Message(MessageType.SERVER_MESSAGE, "Błąd: " + e.getMessage());
                }
            }
        }
        return null;
    }


}