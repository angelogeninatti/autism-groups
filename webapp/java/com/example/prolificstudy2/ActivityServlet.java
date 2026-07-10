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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "ActivityServlet", value = "/activity")
public class ActivityServlet extends HttpServlet {
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
            System.err.println("Exception in init (ActivityServlet)");
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
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

        List<String> availableLanguages = Arrays.asList("it", "en");
        String language = req.getParameter("lang");
        if (language != null && !availableLanguages.contains(language)) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown language: " + language);
            return;
        }

        try {
            switch (action) {
                case "get_questions":
                    handleGetQuestion(resp, language);
                    break;
                case "get_activities":
                    handleGetActivities(resp, language, false);
                    break;
                case "get_new_activity":
                    String excludedActivitiesParam = req.getParameter("excludedActivities");
                    if (excludedActivitiesParam == null || excludedActivitiesParam.isEmpty()) {
                        sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "excludedActivities parameter missing!");
                        return;
                    }
                    ArrayList<Integer> excludedActivitiesIdsParsed = parseExcludedActivities(excludedActivitiesParam);
                    handleGetNewActivity(resp, language, excludedActivitiesIdsParsed);
                    break;
                case "fetch_random_activities":
                    handleGetActivities(resp, language, true);
                    break;
                default:
                    sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
            }
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred");
            System.err.println("Exception in doGet (ActivityServlet)");
            e.printStackTrace();
        }
    }

    private ArrayList<Integer> parseExcludedActivities(String excludedActivities){
        String[] excludedActivitiesIds = excludedActivities.split(",");
        ArrayList<Integer> excludedActivitiesIdsParsed = new ArrayList<>();
        for (String excludedActivitiesId : excludedActivitiesIds) {
            excludedActivitiesIdsParsed.add(Integer.parseInt(excludedActivitiesId));
        }
        return excludedActivitiesIdsParsed;
    }

    private void handleGetNewActivity(HttpServletResponse resp, String lang, ArrayList<Integer> excludedActivitiesIds) {
        try {
            Activity activity = dao.getNewActivity(lang, excludedActivitiesIds);

            if (activity == null) {
                sendJsonResponse(resp, "empty");
                return;
            }

            sendJsonResponse(resp, activity);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve new activity");
            System.err.println("Exception in handleGetNewActivity (ActivityServlet)");
            e.printStackTrace();
        }
    }

    private void handleGetActivities(HttpServletResponse resp, String lang, boolean random) {
        try {
            ArrayList<Activity> activities = dao.getActivities(lang, random);

            if (activities.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            sendJsonResponse(resp, activities);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve activities");
            System.err.println("Exception in handleGetActivities (ActivityServlet)");
            e.printStackTrace();
        }
    }

    private void handleGetQuestion(HttpServletResponse resp, String lang) {
        try {
            ArrayList<Question> questions = dao.getActivityQuestions(lang);

            if (questions.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            sendJsonResponse(resp, questions);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to retrieve Activity questions. Please try again later.");
            System.err.println("Exception in handleGetQuestion (ActivityServlet)");
            e.printStackTrace();
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp){
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

        if ("save_group_formation".equals(action)) {
            handleSaveGroupFormation(req, resp);
            return;
        }

        if("save_liked_activity".equals(action)){
            updateLikedActivity(req, resp);
            return;
        }

        if (!"send_results".equals(action)) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
            return;
        }

        try {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            if (!root.has("answers") || !root.has("attentionChecks") ||
                    !root.has("recaptcha_token") || !root.has("recaptcha_version")) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required fields in request!");
                return;
            }

            JsonArray answersJson = root.getAsJsonArray("answers");
            JsonArray attentionChecksJson = root.getAsJsonArray("attentionChecks");
            String token = root.get("recaptcha_token").getAsString();
            String version = root.get("recaptcha_version").getAsString();

            Object userIdObj = session.getAttribute("userId");
            if (!(userIdObj instanceof Integer)) {
                sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid user session");
                return;
            }
            int userId = (Integer) userIdObj;

            RecaptchaUtils recaptcha = new RecaptchaUtils(RECAPTCHA_V3_SECRET_KEY, RECAPTCHA_V2_SECRET_KEY);
            handleSaveAnswers(recaptcha, token, version, userId, req, resp, answersJson, attentionChecksJson);

        } catch (JsonSyntaxException | IllegalStateException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format");
            System.err.println("Exception in doPost (ActivityServlet)");
            e.printStackTrace();
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred");
            System.err.println("Exception in doPost (ActivityServlet)");
            e.printStackTrace();
        }
    }

    private void handleSaveGroupFormation(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            if (!root.has("activityId") || !root.has("rounds") || !root.has("finalGroupAnswers")) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required fields");
                return;
            }

            Object userIdObj = req.getSession().getAttribute("userId");
            if (!(userIdObj instanceof Integer)) {
                sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid session");
                return;
            }
            int userId = (Integer) userIdObj;
            int activityId = root.get("activityId").getAsInt();
            JsonArray rounds = root.getAsJsonArray("rounds");
            JsonArray finalGroupAnswers = root.getAsJsonArray("finalGroupAnswers");

            // Count final-phase attention check failure
            int attentionFailed = (root.has("attentionCheckPassed") && !root.get("attentionCheckPassed").getAsBoolean()) ? 1 : 0;
            // Add round-level attention check failures
            for (JsonElement roundElem : rounds) {
                JsonObject round = roundElem.getAsJsonObject();
                if (round.has("attentionChecksFailed")) {
                    attentionFailed += round.get("attentionChecksFailed").getAsInt();
                }
            }

            dao.saveGroupFormation(userId, activityId, rounds, finalGroupAnswers, attentionFailed);
            sendJsonResponse(resp, "ok");

        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save group formation");
            System.err.println("Exception in handleSaveGroupFormation (ActivityServlet)");
            e.printStackTrace();
        }
    }

    private void updateLikedActivity(HttpServletRequest req, HttpServletResponse resp) {
        System.out.println(LocalDateTime.now() + " - updateLikedActivity called");
        try {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                System.out.println(body);
            if (!root.has("liked_activities_id") || !root.has("user_id")) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing activityId or like field!");
                return;
            }

            ArrayList<Integer> likedActivityId = new ArrayList<>();
            for (JsonElement id : root.getAsJsonArray("liked_activities_id")) {
                likedActivityId.add(id.getAsInt());
            }
            
            User user = dao.checkExistedUser(root.get("user_id").getAsInt());

            if (user == null) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "User field must be 0 or 1!");
                return;
            }

            dao.updateLikedActivity(likedActivityId, user.getId());
            sendJsonResponse(resp, "ok");

        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to update liked activity");
            System.err.println("Exception in updateLikedActivity (ActivityServlet)");
            e.printStackTrace();
        }
    }

    private void handleSaveAnswers(RecaptchaUtils recaptcha, String token, String version,
                                   int userId, HttpServletRequest req, HttpServletResponse resp,
                                   JsonArray answers, JsonArray attentionChecks) {

        resp.setContentType("application/json");

        boolean captchaOk = recaptcha.verifyRecaptcha(token, version, "submit_activity");
        if (!captchaOk) {
            System.err.println("Invalid CAPTCHA in ActivityServlet (version: " + version + ")");
        }

        ArrayList<ActivityAnswer> answersParsed = parseAnswers(answers);
        if (answersParsed == null) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Some answers are missing!");
            return;
        }

        ArrayList<AttentionCheck> attentionChecksParsed = parseAttentionChecks(attentionChecks);
        int attentionFailed = attentionChecksParsed != null ? validateAttentionChecks(attentionChecksParsed) : 0;

        try {
            dao.saveActivityAnswers(userId, answersParsed);
            dao.saveActivityAttentionChecks(userId, attentionFailed);
        } catch (Exception e) {
            if ("USER_NOT_FOUND".equals(e.getMessage())) {
                sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
            } else {
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to save data. Please try again later.");
            }
            System.err.println("Exception in handleSaveAnswers (ActivityServlet)");
            e.printStackTrace();
            return;
        }

        req.getSession().setAttribute("progress", "activity");
        if (!captchaOk && "v3".equals(version)) {
            sendJsonResponse(resp, Collections.singletonMap("requireV2Verification", true));
        } else {
            sendJsonResponse(resp, "ok");
        }
    }

    private ArrayList<ActivityAnswer> parseAnswers(JsonArray answers) {
        ArrayList<ActivityAnswer> answersParsed = new ArrayList<>();
        for (JsonElement answer : answers) {
            try {
                JsonObject answerObj = answer.getAsJsonObject();
                JsonArray valuesArray = answerObj.getAsJsonArray("values");
                int[] valuesArrayParsed = new Gson().fromJson(valuesArray, int[].class);

                if (Arrays.stream(valuesArrayParsed).anyMatch(n -> n == -1)) {
                    return null;
                }
                answersParsed.add(new ActivityAnswer(
                        answerObj.get("activityId").getAsInt(),
                        valuesArrayParsed));
            } catch (Exception e) {
                System.err.println("Exception in parseAnswers (ActivityServlet)");
                e.printStackTrace();
                return null;
            }
        }
        return answersParsed;
    }

    private ArrayList<AttentionCheck> parseAttentionChecks(JsonArray attentionChecks) {
        try {
            ArrayList<AttentionCheck> checks = new ArrayList<>();
            for (JsonElement answer : attentionChecks) {
                JsonObject obj = answer.getAsJsonObject();
                checks.add(new AttentionCheck(
                        obj.get("userAnswer").getAsInt(),
                        obj.get("correctAnswer").getAsInt()));
            }
            return checks;
        } catch (Exception e) {
            System.err.println("Exception in parseAttentionChecks (ActivityServlet)");
            e.printStackTrace();
            return null;
        }
    }

    private int validateAttentionChecks(ArrayList<AttentionCheck> checks) {
        int failed = 0;
        for (AttentionCheck check : checks) {
            if (!check.checkAttentionChecks()) {
                failed++;
            }
        }
        return failed;
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
