package com.bookstore.server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        DataBase dataBase = new DataBase();
        Server server = new Server(6666);

    }
}
