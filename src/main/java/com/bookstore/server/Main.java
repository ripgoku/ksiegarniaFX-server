package com.bookstore.server;

import java.io.IOException;

/**
 * Główna klasa serwera aplikacji księgarni.
 * Odpowiada za inicjalizację połączenia z bazą danych oraz uruchomienie serwera.
 */
public class Main {
    /**
     * Metoda startowa aplikacji serwerowej.
     * Tworzy instancję bazy danych i uruchamia serwer na określonym porcie.
     *
     * @param args Argumenty linii poleceń. Obecnie nie wykorzystywane.
     * @throws IOException W przypadku wystąpienia błędu wejścia/wyjścia podczas uruchamiania serwera.
     */
    public static void main(String[] args) throws IOException {
        DataBase dataBase = new DataBase();
        Server server = new Server(6666);
    }
}
