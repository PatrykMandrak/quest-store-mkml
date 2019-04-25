package com.queststore.Controller;

import com.queststore.DAO.*;
import com.queststore.Model.Card;
import com.queststore.Model.Transaction;
import com.queststore.Model.TransactionStatus;
import com.queststore.Model.User;
import com.queststore.Services.CardService;
import com.queststore.Services.UserService;
import com.queststore.helpers.CookieHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Scanner;

public class StudentArtifactTransaction implements HttpHandler {

    private UserService userService = new UserService(new UserDAOSql(), new ClassDAOSql(), new TransactionDAOSql());
    private CardService cardService = new CardService(new CardDAOSql());
    private Session session = new Session(new CookieHelper(), new UserDAOSql());
    private TransactionDAO transactionDAO = new TransactionDAOSql();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (exchange.getRequestMethod().equals("POST")) {
            JSONObject jsonObject = parseJSON(exchange);
            int id = jsonObject.getInt("id");
            try {
                Card card = cardService.getCardById(id);
                Optional<User> user = session.getUserOfSession(exchange);
                if (user.isPresent()
                        && session.isUserOfType("student", user.get())
                        && card != null
                        && canBeBought(card, user.get())) {
                    saveTransaction(card, user.get());
                    sendSuccess(exchange);
                } else {
                    send500(exchange);
                }
            } catch (DaoException e) {
                e.printStackTrace();
                send500(exchange);
            }
        }
    }

    private void sendSuccess(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "student-item-store");
        exchange.sendResponseHeaders(303, 0);
    }

    private void saveTransaction(Card card, User user) throws DaoException {
        Transaction transaction = new Transaction(null, null, user, card,
                new TransactionStatus(1, "accepted"), card.getValue());
        transactionDAO.saveTransaction(transaction);
    }

    private JSONObject parseJSON(HttpExchange exchange) {
        Scanner scanner = new Scanner(exchange.getRequestBody());
        return new JSONObject(scanner.nextLine());
    }

    private boolean canBeBought(Card card, User user) throws DaoException {
        int balance = userService.getCoinBalance(user.getId());
        return balance >= card.getValue();
    }

    private void send500(HttpExchange httpExchange) throws IOException {
        String response = "500 Server internal error\n";
        httpExchange.getResponseHeaders().set("Status", "500");
        httpExchange.sendResponseHeaders(500, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
}
