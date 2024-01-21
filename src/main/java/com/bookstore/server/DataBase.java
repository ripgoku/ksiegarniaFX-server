package com.bookstore.server;

import com.bookstore.Book;
import com.bookstore.LoginData;
import com.bookstore.RegistrationData;
import com.bookstore.UserData;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class DataBase {
    final private static String url = "jdbc:mysql://localhost:3306/bookstore_project";
    final private static String user = "root";
    final private static String password = "root123";

    public DataBase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = getConnection();
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connection with " + url + " is successful!");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static databaseAnswer registerUser(RegistrationData newUser)
            throws SQLException {
        String firstName = newUser.getFirstName();
        String lastName = newUser.getLastName();
        String email = newUser.getEmail();
        String login = newUser.getLogin();
        String password = newUser.getPassword();
        String streetNumber = newUser.getHouseNumber();
        String streetName = newUser.getStreet();
        String city = newUser.getCity();
        String postalCode = newUser.getPostalCode();

        Connection connection = null;
        PreparedStatement psInsertLoginDetails = null;
        PreparedStatement psInsertAddress = null;
        PreparedStatement psInsertCustomer = null;
        PreparedStatement psCheckUserExists = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);  // Start transaction

            // Sprawdzenie, czy użytkownik już istnieje
            psCheckUserExists = connection.prepareStatement("SELECT * FROM customer WHERE email = ?");
            psCheckUserExists.setString(1, email);
            resultSet = psCheckUserExists.executeQuery();
            if (resultSet.isBeforeFirst()) {
                System.out.println("Użytkownik o takim emailu już istnieje");
                return databaseAnswer.REGISTER_ERROR_EMAIL;
            }
            psCheckUserExists = connection.prepareStatement("SELECT * FROM login_details WHERE login = ?");
            psCheckUserExists.setString(1, login);
            resultSet = psCheckUserExists.executeQuery();
            if (resultSet.isBeforeFirst()) {
                System.out.println("Użytkownik o takim loginie już istnieje");
                return databaseAnswer.REGISTER_ERROR_LOGIN;
            }

            // Dodanie danych do tabeli login_details
            psInsertLoginDetails = connection.prepareStatement("INSERT INTO login_details (login, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            psInsertLoginDetails.setString(1, login);
            psInsertLoginDetails.setString(2, password);
            int affectedRows = psInsertLoginDetails.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Tworzenie użytkownika nie powiodło się, brak zmian w login_details.");
            }

            int loginId;
            try (ResultSet generatedKeys = psInsertLoginDetails.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    loginId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Tworzenie użytkownika nie powiodło się, nie można uzyskać ID login_details.");
                }
            }

            // Dodanie adresu do tabeli address
            psInsertAddress = connection.prepareStatement("INSERT INTO address (street_number, street_name, city, postal_code) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            psInsertAddress.setString(1, streetNumber);
            psInsertAddress.setString(2, streetName);
            psInsertAddress.setString(3, city);
            psInsertAddress.setString(4, postalCode);
            psInsertAddress.executeUpdate();

            int addressId;
            try (ResultSet generatedKeys = psInsertAddress.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    addressId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Tworzenie użytkownika nie powiodło się, nie można uzyskać ID adresu.");
                }
            }

            // Dodanie klienta do tabeli customer
            psInsertCustomer = connection.prepareStatement("INSERT INTO customer (first_name, last_name, email, user_id, address_id) VALUES (?, ?, ?, ?, ?)");
            psInsertCustomer.setString(1, firstName);
            psInsertCustomer.setString(2, lastName);
            psInsertCustomer.setString(3, email);
            psInsertCustomer.setInt(4, loginId);
            psInsertCustomer.setInt(5, addressId);
            psInsertCustomer.executeUpdate();

            connection.commit();  // Zatwierdzenie transakcji

        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();  // Wycofanie transakcji w przypadku błędu
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            throw ex;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (psCheckUserExists != null) {
                psCheckUserExists.close();
            }
            if (psInsertLoginDetails != null) {
                psInsertLoginDetails.close();
            }
            if (psInsertAddress != null) {
                psInsertAddress.close();
            }
            if (psInsertCustomer != null) {
                psInsertCustomer.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return databaseAnswer.SUCCES;
    }

    public static databaseAnswer loginUser(LoginData user, UserData userData)
            throws SQLException {
        String login = user.getLogin();
        String password = user.getPassword();
        int user_id = 0;

        Connection connection = null;
        PreparedStatement psLoginUser = null;
        PreparedStatement psGetUserData = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);  // Start transaction

            // Sprawdzenie, czy użytkownik już istnieje
            psLoginUser = connection.prepareStatement("SELECT * FROM login_details WHERE login = ? AND password = ?");
            psLoginUser.setString(1, login);
            psLoginUser.setString(2, password);
            resultSet = psLoginUser.executeQuery();
            if (resultSet.next()) {
                user_id = resultSet.getInt(1);
            } else if (!resultSet.isBeforeFirst()) {
                System.out.println("Nie ma użytkownika z podanym loginem i hasłem!");
                return databaseAnswer.ERROR;
            }

            psGetUserData = connection.prepareStatement("SELECT customer.customer_id, address.address_id, " +
                    "customer.first_name, customer.last_name, address.city, " +
                    "address.street_name, address.postal_code, address.street_number " +
                    "FROM customer JOIN address ON customer.address_id = address.address_id " +
                    "WHERE user_id = ?");
            psGetUserData.setInt(1, user_id);
            resultSet = psGetUserData.executeQuery();
            if (resultSet.next()) {
                userData.setCustomer_id(resultSet.getInt(1));
                userData.setAdres_id(resultSet.getInt(2));
                userData.setFirst_name(resultSet.getString(3));
                userData.setLast_name(resultSet.getString(4));
                userData.setCity(resultSet.getString(5));
                userData.setStreet(resultSet.getString(6));
                userData.setPostalCode(resultSet.getString(7));
                userData.setHouseNumber(resultSet.getString(8));
                System.out.println("Zalogowano użytkownika: " + login);
            } else if (!resultSet.isBeforeFirst()) {
                System.out.println("Błąd przy logowaniu. Nie można odnaleźć klienta!");
                return databaseAnswer.ERROR;
            }

            connection.commit();  // Zatwierdzenie transakcji
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();  // Wycofanie transakcji w przypadku błędu
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            throw ex;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (psLoginUser != null) {
                psLoginUser.close();
            }
            if (psGetUserData != null) {
                psGetUserData.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return databaseAnswer.SUCCES;
    }

    public static databaseAnswer getBooks(List<Book> books)
            throws SQLException{
        Connection connection = null;
        PreparedStatement psBooks = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);  // Start transaction

            // Sprawdzenie, czy użytkownik już istnieje
            psBooks = connection.prepareStatement("SELECT book.title, book.isbn13, " +
                    "GROUP_CONCAT(DISTINCT author.author_name SEPARATOR '; ') AS authors, " +
                    "book_language.language_name, book.num_pages, " +
                    "book.publication_date, publisher.publisher_name, " +
                    "GROUP_CONCAT(DISTINCT category.name SEPARATOR '; ') AS categories, " +
                    "book.img , book.price , book.book_id " +
                    "FROM bookstore_project.book " +
                    "JOIN book_author ON book.book_id = book_author.book_id " +
                    "JOIN author ON book_author.author_id = author.author_id " +
                    "JOIN book_language ON book.language_id = book_language.language_id " +
                    "JOIN publisher ON book.publisher_id = publisher.publisher_id " +
                    "JOIN book_categ ON book.book_id = book_categ.book_id " +
                    "JOIN category ON book_categ.category_id = category.category_id " +
                    "GROUP BY book.title, book.isbn13, book_language.language_name, " +
                    "book.num_pages, book.publication_date, publisher.publisher_name, book.img, book.price, book.book_id;");

            resultSet = psBooks.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("Brak książek do wyświetlenia!");
                return databaseAnswer.ERROR;
            }
            while (resultSet.next()) {
                String title = resultSet.getString(1);
                String isbn13 = resultSet.getString(2);
                String author = resultSet.getString(3);
                String language = resultSet.getString(4);
                int num_pages = resultSet.getInt(5);
                Date publication_date = resultSet.getDate(6);
                String publisher = resultSet.getString(7);
                String category = resultSet.getString(8);
                byte[] img = resultSet.getBytes(9);
                float price = resultSet.getFloat(10);
                int book_id = resultSet.getInt(11);

                List<String> authors = Arrays.asList(author.split("; "));
                List<String> categories = Arrays.asList(category.split("; "));

                books.add(new Book(book_id, title, isbn13, authors, language, num_pages, publication_date, publisher, categories, img, price));
            }
            System.out.println("Pobrano asortyment książek.");

            connection.commit();  // Zatwierdzenie transakcji
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();  // Wycofanie transakcji w przypadku błędu
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            throw ex;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (psBooks != null) {
                psBooks.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return databaseAnswer.SUCCES;
    }
}
