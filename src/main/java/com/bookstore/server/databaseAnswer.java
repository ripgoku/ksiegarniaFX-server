package com.bookstore.server;

/**
 * Enum reprezentujący status odpowiedzi bazy danych.
 * Używany do wskazania wyniku operacji na bazie danych.
 */
public enum databaseAnswer {
    /**
     * Wystąpił błąd podczas operacji.
     */
    ERROR,

    /**
     * Operacja zakończona sukcesem.
     */
    SUCCES,

    /**
     * Błąd rejestracji - email już istnieje.
     */
    REGISTER_ERROR_EMAIL,

    /**
     * Błąd rejestracji - login już istnieje.
     */
    REGISTER_ERROR_LOGIN,

    /**
     * Błąd zmiany hasła - nieprawidłowe hasło.
     */
    PASSWORD_ERROR_WRONG
}
