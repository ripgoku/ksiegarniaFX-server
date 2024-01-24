package com.bookstore.server;

import com.bookstore.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Klasa DataBase odpowiada za zarządzanie połączeniem z bazą danych oraz wykonanie podstawowych operacji CRUD
 * na tabelach związanych z projektowym sklepem książek. Umożliwia rejestrację i logowanie użytkowników,
 * pobieranie danych książek, zarządzanie zamówieniami, aktualizację danych użytkowników i zmianę hasła.
 */
public class DataBase {
    // Stałe przechowujące dane do połączenia z bazą danych
    final private static String url = "jdbc:mysql://localhost:3306/bookstore_project";
    final private static String user = "root";
    final private static String password = "root123";

    /**
     * Konstruktor klasy DataBase. Inicjalizuje sterownik JDBC i ustanawia połączenie z bazą danych.
     */
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

    /**
     * Uzyskuje połączenie z bazą danych.
     *
     * @return Połączenie z bazą danych.
     * @throws SQLException W przypadku problemów z połączeniem.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Rejestruje nowego użytkownika w systemie.
     *
     * @param newUser Dane rejestracyjne użytkownika.
     * @return Status operacji rejestracji.
     * @throws SQLException W przypadku problemów z bazą danych.
     */
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
            connection.setAutoCommit(false);

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

            psInsertCustomer = connection.prepareStatement("INSERT INTO customer (first_name, last_name, email, user_id, address_id) VALUES (?, ?, ?, ?, ?)");
            psInsertCustomer.setString(1, firstName);
            psInsertCustomer.setString(2, lastName);
            psInsertCustomer.setString(3, email);
            psInsertCustomer.setInt(4, loginId);
            psInsertCustomer.setInt(5, addressId);
            psInsertCustomer.executeUpdate();

            connection.commit();

        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
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

    /**
     * Loguje użytkownika do systemu.
     *
     * @param user     Dane logowania użytkownika.
     * @param userData Dane użytkownika do wypełnienia.
     * @return Status operacji logowania.
     * @throws SQLException W przypadku problemów z bazą danych.
     */
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
            connection.setAutoCommit(false);

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
                    "address.street_name, address.postal_code, address.street_number, customer.email " +
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
                userData.setEmail(resultSet.getString(9));
                userData.setLogin(login);
                System.out.println("Zalogowano użytkownika: " + login);
            } else if (!resultSet.isBeforeFirst()) {
                System.out.println("Błąd przy logowaniu. Nie można odnaleźć klienta!");
                return databaseAnswer.ERROR;
            }

            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
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

    /**
     * Pobiera listę książek z bazy danych.
     *
     * @param books Lista, do której zostaną dodane pobrane książki.
     * @return Status operacji pobierania książek.
     * @throws SQLException W przypadku problemów z bazą danych.
     */
    public static databaseAnswer getBooks(List<Book> books)
            throws SQLException{
        Connection connection = null;
        PreparedStatement psBooks = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

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

            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
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

    /**
     * Rejestruje zamówienie w systemie.
     *
     * @param order Dane zamówienia do zarejestrowania.
     * @return Status operacji rejestracji zamówienia.
     * @throws SQLException W przypadku problemów z bazą danych.
     */
    public static databaseAnswer placeOrder(Order order)
            throws SQLException {
        int orderId = 0;
        int customerId = order.getUserId();
        int addressId = order.getAddressId();
        int shippingMethodId = order.getShippingMethod().getMethod_id();
        Map<Book, Integer> books = order.getItems();

        Connection connection = null;
        PreparedStatement psInsertOrder = null;
        PreparedStatement psInsertOrderLine = null;
        PreparedStatement psInsertOrderHistory = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            psInsertOrder = connection.prepareStatement(
                    "INSERT INTO cust_order (order_date, customer_id, shipping_method_id, dest_address_id) VALUES (NOW(), ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            psInsertOrder.setInt(1, customerId);
            psInsertOrder.setInt(2, shippingMethodId);
            psInsertOrder.setInt(3, addressId);
            psInsertOrder.executeUpdate();

            resultSet = psInsertOrder.getGeneratedKeys();
            if (resultSet.next()) {
                orderId = resultSet.getInt(1);
            }

            psInsertOrderLine = connection.prepareStatement(
                    "INSERT INTO order_line (order_id, book_id, quantity, price) VALUES (?, ?, ?, ?)");
            for (Map.Entry<Book, Integer> entry : books.entrySet()) {
                BigDecimal price = BigDecimal.valueOf(entry.getKey().getPrice())
                        .multiply(BigDecimal.valueOf(entry.getValue()))
                        .setScale(2, RoundingMode.HALF_UP);
                psInsertOrderLine.setInt(1, orderId);
                psInsertOrderLine.setInt(2, entry.getKey().getBook_id());
                psInsertOrderLine.setInt(3, entry.getValue());
                psInsertOrderLine.setBigDecimal(4, price);
                psInsertOrderLine.executeUpdate();
            }

            psInsertOrderHistory = connection.prepareStatement(
                    "INSERT INTO order_history (order_id, status_id, status_date) VALUES (?, ?, NOW())");
            psInsertOrderHistory.setInt(1, orderId);
            psInsertOrderHistory.setInt(2, 1);
            psInsertOrderHistory.executeUpdate();
            System.out.println("Złożono zamówienie.");

            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            throw ex;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (psInsertOrder != null) {
                psInsertOrder.close();
            }
            if (psInsertOrderLine != null) {
                psInsertOrderLine.close();
            }
            if (psInsertOrderHistory != null) {
                psInsertOrderHistory.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return databaseAnswer.SUCCES;
    }

    /**
     * Aktualizuje dane użytkownika w systemie.
     *
     * @param userData Zaktualizowane dane użytkownika.
     * @return Status operacji aktualizacji danych.
     * @throws SQLException W przypadku problemów z bazą danych.
     */
    public static databaseAnswer updateUser(UserData userData)
            throws SQLException {
        String firstName = userData.getFirst_name();
        String lastName = userData.getLast_name();
        String email = userData.getEmail();
        String login = userData.getLogin();
        String city = userData.getCity();
        String postalCode = userData.getPostalCode();
        String street = userData.getStreet();
        String houseNumber = userData.getHouseNumber();
        int customerId = userData.getCustomer_id();
        int addressId = userData.getAdres_id();

        Connection connection = null;
        PreparedStatement psUpdateCustomer = null;
        PreparedStatement psUpdateAddress = null;
        int affectedRows = 0;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            psUpdateCustomer = connection.prepareStatement("UPDATE customer SET first_name = ?, last_name = ?, email = ? WHERE customer_id = ?");
            psUpdateCustomer.setString(1, firstName);
            psUpdateCustomer.setString(2, lastName);
            psUpdateCustomer.setString(3, email);
            psUpdateCustomer.setInt(4, customerId);
            affectedRows = psUpdateCustomer.executeUpdate();
            if (affectedRows <= 0) {
                return databaseAnswer.ERROR;
            }
            affectedRows = 0;

            psUpdateAddress = connection.prepareStatement("UPDATE address SET street_number = ?, street_name = ?, city = ?, postal_code = ? WHERE address_id = ?");
            psUpdateAddress.setString(1, houseNumber);
            psUpdateAddress.setString(2, street);
            psUpdateAddress.setString(3, city);
            psUpdateAddress.setString(4, postalCode);
            psUpdateAddress.setInt(5, addressId);
            affectedRows = psUpdateAddress.executeUpdate();
            if (affectedRows <= 0) {
                return databaseAnswer.ERROR;
            }

            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            throw ex;
        } finally {
            if (psUpdateAddress != null) {
                psUpdateAddress.close();
            }
            if (psUpdateCustomer != null) {
                psUpdateCustomer.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return databaseAnswer.SUCCES;
    }

    /**
     * Aktualizuje hasło użytkownika w systemie.
     *
     * @param passwordData Dane do zmiany hasła.
     * @return Status operacji zmiany hasła.
     * @throws SQLException W przypadku problemów z bazą danych.
     */
    public static databaseAnswer updatePassword(PasswordData passwordData)
            throws SQLException {
        String login = passwordData.getLogin();
        String oldPassword = passwordData.getOldPassword();
        String newPassword = passwordData.getNewPassword();

        Connection connection = null;
        PreparedStatement psCheckPassword = null;
        PreparedStatement psUpdatePassword = null;
        ResultSet resultSet = null;
        int affectedRows = 0;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            psCheckPassword = connection.prepareStatement("SELECT * FROM login_details WHERE login = ? AND password = ?");
            psCheckPassword.setString(1, login);
            psCheckPassword.setString(2, oldPassword);
            resultSet = psCheckPassword.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                return databaseAnswer.PASSWORD_ERROR_WRONG;
            }

            psUpdatePassword = connection.prepareStatement("UPDATE login_details SET password = ? WHERE login = ?");
            psUpdatePassword.setString(1, newPassword);
            psUpdatePassword.setString(2, login);
            affectedRows = psUpdatePassword.executeUpdate();
            if (affectedRows <= 0) {
                return databaseAnswer.ERROR;
            }

            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            throw ex;
        } finally {
            if (psCheckPassword != null) {
                psCheckPassword.close();
            }
            if (psUpdatePassword != null) {
                psUpdatePassword.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return databaseAnswer.SUCCES;
    }
}
