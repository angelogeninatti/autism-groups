package com.example.prolificstudy2;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "CompletionCodeServlet", value = "/completionCode")
public class CompletionCodeServlet extends HttpServlet {
    private DAO dao;
    private static final Gson gson = new Gson();
    private int maxAttentionFail;
    private String okCompletion;
    private String nokCompletion;


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

            this.maxAttentionFail = Integer.parseInt(ctx.getInitParameter("max_attention_fails"));
            this.okCompletion = ctx.getInitParameter("ok_completion");
            this.nokCompletion = ctx.getInitParameter("nok_completion");
        } catch (Exception e) {
            System.err.println("Exception in init (CompletionCodeServlet)");
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp){
        resp.setContentType("text/plain");
        HttpSession session = req.getSession(false);
        if (session == null) {
            sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Session not found");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        String action = req.getParameter("action");
        if (action == null) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Action parameter missing!");
            return;
        }
        boolean isProlific = (boolean) session.getAttribute("isProlific");

        String progress = (String) session.getAttribute("progress");
        if (action.equals("get_completion_code") && !"activity".equals(progress)) {
            System.err.println("CompletionCodeServlet: user " + userId + " attempted to get completion code with progress='" + progress + "'");
            sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "Study not completed");
            return;
        }

        try {
            if (action.equals("get_completion_code")) {
                handleGetCompletionCode(resp, userId, this.maxAttentionFail, isProlific);
            } else if (action.equals("error")) {
                if (!isProlific) {
                    sendJsonResponse(resp, "not_prolific");
                } else {
                    sendJsonResponse(resp, this.nokCompletion);
                }
            } else {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                return;
            }
            session.invalidate(); //After this I can invalidate the session because the suer has finished the test
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred");
            System.err.println("Exception in doGet (CompletionCodeServlet)");
            e.printStackTrace();
        }
    }

    private void handleGetCompletionCode(HttpServletResponse resp, int userId, int maxAttentionFail, boolean isProlific){
        try {
            boolean status = dao.checkStatus(userId, maxAttentionFail);
            String completionCode = status ? okCompletion : nokCompletion;
            dao.setCompletionCode(userId, completionCode);
            if (!isProlific) {
                sendJsonResponse(resp, "not_prolific");
            } else {
                sendJsonResponse(resp, completionCode);
            }
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve completion code");
            System.err.println("Exception in handleGetCompletionCode (CompletionCodeServlet)");
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
