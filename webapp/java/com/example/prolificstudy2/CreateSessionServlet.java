package com.example.prolificstudy2;

import com.google.gson.Gson;
import datamodel.DAO;

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
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@WebServlet(name = "CreateSessionServlet", value = "/createSession")
public class CreateSessionServlet extends HttpServlet {
    DAO dao;
    private static final Gson gson = new Gson();
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
            System.err.println("Exception in init (CreateSessionServlet)");
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/plain");
        HttpSession session = req.getSession(false);
        if (session != null) {
            sendJsonResponse(resp, "ok");
            return;
        }

        String typeParam = req.getParameter("type");
        String neurotype;
        if ("typeat".equals(typeParam)) {
            neurotype = "autistic";
        } else if ("typenu".equals(typeParam)) {
            neurotype = "neurotypical";
        } else {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing type parameter. Use 'typeat' or 'typenu'.");
            return;
        }

        String specificId = req.getParameter("prolificId");
        boolean isProlific = true;
        if (Objects.equals(specificId, "undefined") || specificId == null) {
            isProlific = false;
            UUID uuid = UUID.randomUUID();
            specificId = uuid.toString();
        }

        resp.setContentType("text/plain");
        int userId = -1;
        try {
            userId = dao.signUp(isProlific, specificId);
        } catch (Exception e){
            if (e.getMessage().equals("DUPLICATE_PROLIFIC_ID")) {
                sendJsonResponse(resp, "nok");
            } else {
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred");
                System.err.println("Exception in doGet (ActivityServlet)");
                e.printStackTrace();
            }
            return;
        }

        try {
            dao.saveNeurotype(userId, neurotype);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save neurotype");
            System.err.println("Exception saving neurotype for userId: " + userId);
            e.printStackTrace();
            return;
        }

        session = req.getSession();
        session.setAttribute("isProlific", isProlific);
        session.setAttribute("userSpecificId", specificId);
        session.setAttribute("userId", userId);

        sendJsonResponse(resp, userId);

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

    private void sendJsonError(HttpServletResponse resp, int status, String message) {
        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(Collections.singletonMap("error", message)));
        } catch (IOException e) {
            System.err.println("Exception in sendJsonError (ActivityServlet)");
            e.printStackTrace();
        }
    }
}
