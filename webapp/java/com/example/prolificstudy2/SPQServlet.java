package com.example.prolificstudy2;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import datamodel.AttentionCheck;
import datamodel.DAO;
import datamodel.SPQQuestion;
import datamodel.SPQResult;
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

@WebServlet(name = "SPQServlet", value = "/spq")
public class SPQServlet extends HttpServlet {
    private DAO dao;
    private static final Gson gson = new Gson();
    private String RECAPTCHA_V3_SECRET_KEY;
    private String RECAPTCHA_V2_SECRET_KEY;

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

            RECAPTCHA_V3_SECRET_KEY = ctx.getInitParameter("RECAPTCHA_V3_SECRET_KEY");
            RECAPTCHA_V2_SECRET_KEY = ctx.getInitParameter("RECAPTCHA_V2_SECRET_KEY");

            if (RECAPTCHA_V3_SECRET_KEY == null || RECAPTCHA_V3_SECRET_KEY.isEmpty() ||
                    RECAPTCHA_V2_SECRET_KEY == null || RECAPTCHA_V2_SECRET_KEY.isEmpty()) {
                System.err.println("Exception in init (ActivityServlet)");
                throw new ServletException("reCAPTCHA secret keys not properly configured");
            }
        } catch (Exception e) {
            System.err.println("Exception in init (SPQServlet)");
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp){
        resp.setContentType("application/json");
        HttpSession session = req.getSession(false);
        if (session == null) {
            sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Session not found");
            return;
        }

        String action = req.getParameter("action");
        if (action == null) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Action parameter missing!");
            return;
        }

        if (!action.equals("get_questions")) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
            return;
        }

        try {
            String language = req.getParameter("lang");
            List<String> availableLanguages = Arrays.asList("it", "en");
            if (language != null && !availableLanguages.contains(language)) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown language: " + language);
                return;
            }
            handleGetQuestion(resp, language);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred");
            System.err.println("Exception in doGet (SPQServlet)");
            e.printStackTrace();
        }
    }

    private void handleGetQuestion(HttpServletResponse resp, String lang){
        try {
            ArrayList<SPQQuestion> questions = dao.getSPQQuestions(lang);

            if (questions.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
            sendJsonResponse(resp, questions);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to retrieve SPQ questions. Please try again later.");
            System.err.println("Exception in handleGetQuestion (SPQServlet)");
            e.printStackTrace();
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        HttpSession session = req.getSession(false);
        if (session == null) {
            sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Session not found");
            return;
        }

        String action = req.getParameter("action");
        if (action == null) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Action parameter missing!");
            return;
        }

        if (!action.equals("send_results")) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
            return;
        }

        try {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray answersJson = root.getAsJsonArray("answers");
            ArrayList<Integer> answers = new ArrayList<>();
            answersJson.forEach(answer -> answers.add(answer.getAsInt()));
            if (answers.size() != 35) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Some answers are missing!");
                return;
            }
            if (answers.contains(-1)) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Some answers are missing!");
                return;
            }

            JsonArray attentionChecksJson = root.getAsJsonArray("attentionChecks");
            ArrayList<AttentionCheck> attentionChecks = new ArrayList<>();
            attentionChecksJson.forEach(answer -> attentionChecks.add(new AttentionCheck(answer.getAsJsonObject().get("userAnswer").getAsInt(), answer.getAsJsonObject().get("correctAnswer").getAsInt())));
            int attentionFailed = 0;
            for (AttentionCheck check : attentionChecks) {
                if (!check.checkAttentionChecks()) {
                    attentionFailed++;
                }
            }
            String token = String.valueOf(root.get("recaptcha_token")).replaceAll("\"", "");
            String version = String.valueOf(root.get("recaptcha_version")).replaceAll("\"", "");

            RecaptchaUtils recaptcha = new RecaptchaUtils(RECAPTCHA_V3_SECRET_KEY, RECAPTCHA_V2_SECRET_KEY);


            handleSaveAnswers(recaptcha, token, version, (Integer) session.getAttribute("userId"), answers, attentionFailed, req, resp);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred");
            System.err.println("Exception in doPost (BigFServlet)");
            e.printStackTrace();
        }
    }

    private void handleSaveAnswers(RecaptchaUtils recaptcha, String token, String version, int userId, ArrayList<Integer> answers, int attentionFailed, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        boolean captchaOk = recaptcha.verifyRecaptcha(token, version, "submit_spq");
        if (!captchaOk) {
            System.err.println("Invalid CAPTCHA for user: " + userId + " (version: " + version + ")");
        }

        try {
            ArrayList<SPQQuestion> questions = dao.getSPQQuestions("it");

            int total = 0;
            int touch = 0;
            int smell = 0;
            int vision = 0;
            int hearing = 0;
            int taste = 0;

            for (int i = 0; i < answers.size(); i++) {
                SPQQuestion question = questions.get(i);
                int score = question.isInverted() ? (3 - answers.get(i)) : answers.get(i);
                total += score;
                switch (question.getMainCategory()) {
                    case "Touch":
                        touch += score;
                        break;
                    case "Smell":
                        smell += score;
                        break;
                    case "Vision":
                        vision += score;
                        break;
                    case "Hearing":
                        hearing += score;
                        break;
                    case "Taste":
                        taste += score;
                        break;
                }
            }

            SPQResult result = new SPQResult(total, touch, smell, vision, hearing, taste);
            String resultJson = gson.toJson(result);
            dao.saveSQPAnswers(userId, answers);
            dao.saveSPQResult(userId, resultJson, attentionFailed);

            HttpSession session = req.getSession();
            session.setAttribute("progress", "spq");

            if (!captchaOk && "v3".equals(version)) {
                JsonObject responseObj = JsonParser.parseString(resultJson).getAsJsonObject();
                responseObj.addProperty("requireV2Verification", true);
                sendJsonResponse(resp, responseObj.toString());
            } else {
                sendJsonResponse(resp, resultJson);
            }
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve questions. Please try again later.");
            System.err.println("Exception in handleSaveAnswers (SPQServlet) for userId: " + userId);
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
