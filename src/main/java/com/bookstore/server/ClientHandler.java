package com.bookstore.server;

import com.bookstore.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import static com.bookstore.server.DataBase.*;

/**
 * Klasa ClientHandler odpowiada za obsługę komunikacji między klientem a serwerem w kontekście sklepu książek.
 * Przetwarza żądania od klienta, wykonuje odpowiednie operacje na bazie danych i zwraca odpowiedzi do klienta.
 */
public class ClientHandler extends Thread{
    // Pola klasy
    final Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    /**
     * Konstruktor ClientHandler inicjalizuje strumienie danych i ustawia gniazdo do komunikacji.
     *
     * @param socket Gniazdo połączenia z klientem.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda run uruchamiana przez wątek, służy do ciągłej obsługi komunikacji z klientem.
     */
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

    /**
     * Przetwarza otrzymane żądanie od klienta i generuje odpowiedź.
     *
     * @param request Żądanie od klienta.
     * @return Odpowiedź do wysłania do klienta.
     */
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
            case ORDER_PRODUCT -> {
                try {
                    Order order = (Order) request.getData();

                    databaseAnswer databaseAns = placeOrder(order);

                    if (databaseAns == databaseAnswer.SUCCES) {
                        return new Message(MessageType.SERVER_MESSAGE_SUCCES, "Złożono zamówienie.");
                    } else if (databaseAns == databaseAnswer.ERROR) {
                        return new Message(MessageType.SERVER_MESSAGE_ERROR, "Nie udało się złożyć zamówienia.");
                    }
                } catch (Exception e) {
                    return new Message(MessageType.SERVER_MESSAGE_ERROR, "Błąd: " + e.getMessage());
                }
            }
            case UPDATE_USER_DETAILS -> {
                try {
                    UserData userData = (UserData) request.getData();

                    databaseAnswer databaseAns = updateUser(userData);

                    if (databaseAns == databaseAnswer.SUCCES) {
                        return new Message(MessageType.SERVER_MESSAGE_SUCCES, "Zmieniono dane.");
                    } else if (databaseAns == databaseAnswer.ERROR) {
                        return new Message(MessageType.SERVER_MESSAGE_ERROR, "Nie udało się zmienić danych.");
                    }
                } catch (Exception e) {
                    return new Message(MessageType.SERVER_MESSAGE_ERROR, "Błąd: " + e.getMessage());
                }
            }
            case UPDATE_PASSWORD -> {
                try {
                    PasswordData passwordData = (PasswordData) request.getData();

                    databaseAnswer databaseAns = updatePassword(passwordData);

                    if (databaseAns == databaseAnswer.SUCCES) {
                        return new Message(MessageType.SERVER_MESSAGE_SUCCES, "Zmieniono hasło.");
                    } else if (databaseAns == databaseAnswer.ERROR) {
                        return new Message(MessageType.SERVER_MESSAGE_ERROR, "Nie udało się zmienić hasła.");
                    } else if (databaseAns == databaseAnswer.PASSWORD_ERROR_WRONG) {
                        return new Message(MessageType.SERVER_MESSAGE_ERROR, "Błędne stare hasło!");
                    }
                } catch (Exception e) {
                    return new Message(MessageType.SERVER_MESSAGE_ERROR, "Błąd: " + e.getMessage());
                }
            }
        }
        return null;
    }
}