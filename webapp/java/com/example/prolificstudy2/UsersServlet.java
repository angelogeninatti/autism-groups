package com.example.prolificstudy2;

import com.google.gson.*;
import datamodel.*;
import utils.RecaptchaUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "UsersServlet", value = "/users")
public class UsersServlet extends HttpServlet {
    private DAO dao;
    private static final Gson gson = new Gson();
    private int maxAttentionFail;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            ServletContext ctx = config.getServletContext();

            Object tempDAO = ctx.getAttribute("dao");
            if (!(tempDAO instanceof DAO)) {
                throw new ServletException("DAO not found or invalid in servlet context");
            }
            dao = (DAO) tempDAO;

        } catch (Exception e) {
            System.err.println("Exception in init (ActivityServlet)");
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp){
            resp.setContentType("application/json");
            HttpSession session = req.getSession(false);
            if (session == null) {
                System.err.println("[UsersServlet] Session not found");
                sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Session not found");
                return;
            }

            String action = req.getParameter("action");
            if (action == null) {
                System.err.println("[UsersServlet] Action parameter missing!");
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Action parameter missing!");
                return;
            }

            List<String> availableLanguages = Arrays.asList("it", "en");
            String language = req.getParameter("lang");
            if (language != null && !availableLanguages.contains(language)) {
                System.err.println("[UsersServlet] Unknown language: " + language);
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown language: " + language);
                return;
            }

            try {
                switch (action) {
                    case "fetch_random_users":
                        System.err.println("[UsersServlet] fetch_random_users called, lang=" + language);
                        handleRandomUsers(resp, language);
                        break;
                    case "fetch_all_simulated_users":
                        handleAllSimulatedUsers(resp, language);
                        break;
                    case "get_my_profile":
                        handleMyProfile(req, resp);
                        break;
                    default:
                        System.err.println("[UsersServlet] Unknown action: " + action);
                        sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                        break;
                }
            } catch (IOException e) {
                System.err.println("[UsersServlet] IOException: " + e.getMessage());
                e.printStackTrace();
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
                return;
            } catch (Exception e) {
                System.err.println("[UsersServlet] Exception: " + e.getMessage());
                e.printStackTrace();
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
                return;
            }
        }

        private void handleAllSimulatedUsers(HttpServletResponse resp, String lang) throws IOException {
            try {
                ArrayList<datamodel.User> users = dao.getAllSimulatedUsers(lang);
                resp.setContentType("application/json");
                try (java.io.PrintWriter out = resp.getWriter()) {
                    out.print(gson.toJson(users));
                }
            } catch (Exception e) {
                System.err.println("[UsersServlet] Exception in handleAllSimulatedUsers: " + e.getMessage());
                e.printStackTrace();
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }

        private void handleMyProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                Object userIdObj = req.getSession().getAttribute("userId");
                if (!(userIdObj instanceof Integer)) {
                    sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid session");
                    return;
                }
                int userId = (Integer) userIdObj;
                datamodel.User profile = dao.getUserProfile(userId);
                if (profile == null) {
                    sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                    return;
                }
                resp.setContentType("application/json");
                try (java.io.PrintWriter out = resp.getWriter()) {
                    out.print(gson.toJson(profile));
                }
            } catch (Exception e) {
                System.err.println("[UsersServlet] Exception in handleMyProfile: " + e.getMessage());
                e.printStackTrace();
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }

        private void handleRandomUsers(HttpServletResponse resp, String lang) throws IOException {
            try{
                System.err.println("[UsersServlet] handleRandomUsers: lang=" + lang);
                ArrayList<User> randomUsers = dao.getRandomUsers(10, lang);
                System.err.println("[UsersServlet] handleRandomUsers: users fetched=" + (randomUsers != null ? randomUsers.size() : "null"));
                String jsonResponse = gson.toJson(randomUsers);
                resp.setContentType("application/json");
                try (PrintWriter out = resp.getWriter()) {
                    out.print(jsonResponse);
                }
            } catch (Exception e) {
                System.err.println("[UsersServlet] Exception in handleRandomUsers: " + e.getMessage());
                e.printStackTrace();
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
            }
        }

    private void sendJsonError(HttpServletResponse resp, int status, String message) {
        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(Collections.singletonMap("error", message)));
        } catch (IOException e) {
            System.err.println("Exception in sendJsonError (ActivityServlet)");
            e.printStackTrace();
        }
    }

    private void sendJsonResponse(HttpServletResponse resp, Object message) {
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(message));
        } catch (IOException e) {
            System.err.println("Exception in sendJsonResponse (ActivityServlet)");
            e.printStackTrace();
        }
    }
}
