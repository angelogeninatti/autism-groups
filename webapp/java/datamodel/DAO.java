package datamodel;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DAO {
    String url;
    String user;
    String pwd;
    String dbNameDev = "prolific_study";
    String dbName = "activities_prolific_study";

    public DAO(String url, String user, String pwd) {
        registerDriver();
        this.url = url;
        this.user = user;
        this.pwd = pwd;
    }

    private void registerDriver() {
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (SQLException e) {
            System.err.println("Error in registerDriver");
            e.printStackTrace();
        }
    }

    public ArrayList<SPQQuestion> getSPQQuestions(String lang) {
        ArrayList<SPQQuestion> questions = new ArrayList<>();

        String sql = "SELECT * FROM spq_question";
        if (Objects.equals(lang, "it")) {
            sql = "SELECT id, text_italian AS text, inverted, main_category FROM spq_question";
        } else if (Objects.equals(lang, "en")) {
            sql = "SELECT id, text_english AS text, inverted, main_category FROM spq_question";
        }
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                boolean inverted = rs.getBoolean("inverted");
                if (lang == null) {
                    String textItalian = rs.getString("text_italian");
                    String textEnglish = rs.getString("text_english");
                    String mainCategory = rs.getString("main_category");
                    questions.add(new SPQQuestion(id, textItalian, textEnglish, inverted, mainCategory));
                } else {
                    String mainCategory = rs.getString("main_category");
                    String text = rs.getString("text");
                    if (Objects.equals(lang, "it")) {
                        questions.add(new SPQQuestion(id, text, null, inverted, mainCategory));
                    } else {
                        questions.add(new SPQQuestion(id, null, text, inverted, mainCategory));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getSPQQuestions");
            e.printStackTrace();
        }
        return questions;
    }

    public int signUp(boolean isProlific, String specificId) {
        String sql = "INSERT INTO " + dbName + ".user (prolific_id) VALUES (?)";
        if (!isProlific) {
            sql = "INSERT INTO " + dbName + ".user (unique_id) VALUES (?)";
        }
        System.out.println("SQL used: " + sql);
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            st.setString(1, specificId);
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("No generated key returned");
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) {
                System.err.println("Duplicate specificId: " + specificId);
                System.err.println("isProlific: " + isProlific);
                throw new RuntimeException("DUPLICATE_PROLIFIC_ID");
            } else {
                System.err.println("Error during signUp, specificId: " + specificId);
                System.err.println(e.getMessage());
                throw new RuntimeException("Error during signUp");
            }
        }
    }

    public void saveSQPAnswers(int userId, ArrayList<Integer> answers) {
        String sql = "INSERT INTO spq_answer (user_id, question_id, answer) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE answer = VALUES(answer)";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < answers.size(); i++) {
                    st.setInt(1, userId);
                    st.setInt(2, i + 1);
                    st.setInt(3, answers.get(i));
                    st.addBatch();
                }

                st.executeBatch();
                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Batch insert failed for user: " + userId);
                throw new RuntimeException("Failed to save SPQ answers", e);
            }
        } catch (SQLException e) {
            System.err.println("Database connection error for user: " + userId);
            throw new RuntimeException("Database error", e);
        }
    }

    public void saveSPQResult(int userId, String result, int attentionFailed) {
        String sql = "UPDATE  " + dbName + ".user SET SPQResult = ?, attentionFailed = attentionFailed + ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, result);
            st.setInt(2, attentionFailed);
            st.setInt(3, userId);
            int rowsUpdated = st.executeUpdate();

            if (rowsUpdated == 0) {
                System.err.println("No user found with ID: " + userId);
                throw new RuntimeException("USER_NOT_FOUND");
            }
        } catch (SQLException e) {
            System.err.println("Error during saveSPQResult, userId: " + userId + "\nresult: " + result);
            System.out.println(e.getMessage());
        }
    }

    public void saveActivityAttentionChecks(int userId, int attentionFailed) {
        String sql = "UPDATE  " + dbName + ".user SET attentionFailed = attentionFailed + ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, attentionFailed);
            st.setInt(2, userId);
            int rowsUpdated = st.executeUpdate();

            if (rowsUpdated == 0) {
                System.err.println("No user found with ID: " + userId);
                throw new RuntimeException("USER_NOT_FOUND");
            }
        } catch (SQLException e) {
            System.err.println("Error during saveActivityAttentionChecks, userId: " + userId);
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<BigFQuestion> getBigFQuestions(String lang) {
        ArrayList<BigFQuestion> questions = new ArrayList<>();
        String sql = "SELECT * FROM bigfive_question";
        if (Objects.equals(lang, "it")) {
            sql = "SELECT id, text_italian AS text, inverted, category FROM bigfive_question";
        } else if (Objects.equals(lang, "en")) {
            sql = "SELECT id, text_english AS text, inverted, category FROM bigfive_question";
        }
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                boolean inverted = rs.getBoolean("inverted");
                if (lang == null) {
                    String textItalian = rs.getString("text_italian");
                    String textEnglish = rs.getString("text_english");
                    String category = rs.getString("category");
                    questions.add(new BigFQuestion(id, textItalian, textEnglish, inverted, category));
                } else {
                    String category = rs.getString("category");
                    String text = rs.getString("text");
                    if (Objects.equals(lang, "it")) {
                        questions.add(new BigFQuestion(id, text, null, inverted, category));
                    } else {
                        questions.add(new BigFQuestion(id, null, text, inverted, category));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getBigFQuestions");
            System.out.println(e.getMessage());
        }
        return questions;
    }

    public void saveBigFAnswers(int userId, ArrayList<Integer> answers) {
        String sql = "INSERT INTO bigfive_answer (user_id, question_id, answer) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE answer = VALUES(answer)";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < answers.size(); i++) {
                    st.setInt(1, userId);
                    st.setInt(2, i + 1);
                    st.setInt(3, answers.get(i));
                    st.addBatch();
                }

                st.executeBatch();
                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Batch insert failed for user: " + userId);
                throw new RuntimeException("Failed to save BigF answers", e);
            }
        } catch (SQLException e) {
            System.err.println("Database connection error for user: " + userId);
            throw new RuntimeException("Database error", e);
        }
    }

    public void saveBigFResult(int userId, String resultJson, int attentionFailed) {
        String sql = "UPDATE  " + dbName + ".user SET BigFResult = ?, attentionFailed = attentionFailed + ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, resultJson);
            st.setInt(2, attentionFailed);
            st.setInt(3, userId);
            int rowsUpdated = st.executeUpdate();

            if (rowsUpdated == 0) {
                System.err.println("No user found with ID: " + userId);
                throw new RuntimeException("USER_NOT_FOUND");
            }
        } catch (SQLException e) {
            System.err.println("Error during saveBigFResult, userId: " + userId + "\nresult: " + resultJson);
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<Question> getActivityQuestions(String lang) {
        ArrayList<Question> questions = new ArrayList<>();

        String sql = "SELECT * FROM activity_question";
        if (Objects.equals(lang, "it")) {
            sql = "SELECT id, text_italian AS text, inverted FROM activity_question";
        } else if (Objects.equals(lang, "en")) {
            sql = "SELECT id, text_english AS text, inverted FROM activity_question";
        }
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                boolean inverted = rs.getBoolean("inverted");
                if (lang == null) {
                    String textItalian = rs.getString("text_italian");
                    String textEnglish = rs.getString("text_english");
                    questions.add(new Question(id, textItalian, textEnglish, inverted));
                } else {
                    String text = rs.getString("text");
                    if (Objects.equals(lang, "it")) {
                        questions.add(new Question(id, text, null, inverted));
                    } else {
                        questions.add(new Question(id, null, text, inverted));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getActivityQuestions");
            System.out.println(e.getMessage());
        }
        return questions;
    }

    public void saveActivityAnswers(int userId, ArrayList<ActivityAnswer> activityAns) {
        String sql = "INSERT INTO activity_answer (activity_id, user_id, question_id, answer) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (ActivityAnswer activityAn : activityAns) {
                    int[] ans = activityAn.getAnswer();
                    for (int j = 0; j < ans.length; j++) {
                        st.setInt(1, activityAn.getActivityId());
                        st.setInt(2, userId);
                        st.setInt(3, j + 1);
                        st.setInt(4, ans[j]);
                        st.addBatch();
                    }
                }

                st.executeBatch();
                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Batch insert failed for user: " + userId);
                throw new RuntimeException("Failed to save Activity answers", e);
            }
        } catch (SQLException e) {
            System.err.println("Database connection error for user: " + userId);
            throw new RuntimeException("Database error", e);
        }
    }

    public Activity getNewActivity(String lang, ArrayList<Integer> excludedActivitiesIds) {
        Activity activity = null;

        String sql = "WITH random_activities AS ( SELECT * FROM activity WHERE id NOT IN (#) ORDER BY RAND() LIMIT 1 ) SELECT * FROM random_activities ra JOIN activity_spq sa ON ra.id = sa.activity_id ORDER BY ra.id;";
        for (int i = 0; i < excludedActivitiesIds.size(); i++) {
            sql = sql.replace("#", "?, #");
        }
        sql = sql.replace(", #", "");

        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            for (int i = 0; i < excludedActivitiesIds.size(); i++) {
                st.setInt(i + 1, excludedActivitiesIds.get(i));
            }
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String titleItalian = rs.getString("title_italian");
                String titleEnglish = rs.getString("title_english");
                String descriptionItalian = rs.getString("description_italian");
                String descriptionEnglish = rs.getString("description_english");
                String categoryItalian = rs.getString("category_italian");
                String categoryEnglish = rs.getString("category_english");
                String city = rs.getString("city");
                String language = rs.getString("language");
                SpqEvaluation sqp_eval = null;
                String visionScore;
                String hearingScore;
                String smellScore;
                String tasteScore;
                String touchScore;
                String visionDescriptionItalian;
                String hearingDescriptionItalian;
                String smellDescriptionItalian;
                String tasteDescriptionItalian;
                String touchDescriptionItalian;
                String visionDescriptionEnglish;
                String hearingDescriptionEnglish;
                String smellDescriptionEnglish;
                String tasteDescriptionEnglish;
                String touchDescriptionEnglish;
                int like;
                if (Objects.equals(language, "it")) {
                    visionScore = rs.getString("vision_score");
                    hearingScore = rs.getString("hearing_score");
                    smellScore = rs.getString("smell_score");
                    tasteScore = rs.getString("taste_score");
                    touchScore = rs.getString("touch_score");
                    visionDescriptionItalian = rs.getString("vision_description");
                    hearingDescriptionItalian = rs.getString("hearing_description");
                    smellDescriptionItalian = rs.getString("smell_description");
                    tasteDescriptionItalian = rs.getString("taste_description");
                    touchDescriptionItalian = rs.getString("touch_description");
                    like = rs.getInt("nr_like");
                    if (rs.next()) {
                        visionDescriptionEnglish = rs.getString("vision_description");
                        hearingDescriptionEnglish = rs.getString("hearing_description");
                        smellDescriptionEnglish = rs.getString("smell_description");
                        tasteDescriptionEnglish = rs.getString("taste_description");
                        touchDescriptionEnglish = rs.getString("touch_description");
                        sqp_eval = new SpqEvaluation(visionScore, hearingScore, smellScore, tasteScore, touchScore, visionDescriptionItalian, hearingDescriptionItalian, smellDescriptionItalian, tasteDescriptionItalian, touchDescriptionItalian, visionDescriptionEnglish, hearingDescriptionEnglish, smellDescriptionEnglish, tasteDescriptionEnglish, touchDescriptionEnglish);
                    }
                }

                activity = new Activity(id, titleItalian, titleEnglish, descriptionItalian, descriptionEnglish, categoryItalian, categoryEnglish, city, sqp_eval, 0);
            }
        } catch (SQLException e) {
            System.err.println("Error in getActivityQuestions");
            System.out.println(e.getMessage());
        }
        return activity;
    }


    public boolean checkStatus(int userId, int maxAttentionFail) {
        String sql = "SELECT u.SPQResult, u.BigFResult, u.attentionFailed, " +
                     "COUNT(DISTINCT fga.activity_id) AS completedActivities " +
                     "FROM " + dbName + ".user u " +
                     "LEFT JOIN " + dbName + ".final_group_answer fga ON u.id = fga.user_id " +
                     "WHERE u.id = ? GROUP BY u.id";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, userId);
            try(ResultSet rs = st.executeQuery()) {
                if(rs.next()) {
                    String SPQResult = rs.getString("SPQResult");
                    String BigFResult = rs.getString("BigFResult");
                    int attentionFailed = rs.getInt("attentionFailed");
                    int completedActivities = rs.getInt("completedActivities");
                    return SPQResult != null && BigFResult != null
                        && attentionFailed >= 0 && attentionFailed < maxAttentionFail
                        && completedActivities >= 2;
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<Activity> getActivities(String lang, boolean random) {
        ArrayList<Activity> activities = new ArrayList<>();
        String sql;

        if (random) {
            if(Objects.equals(lang, "en")){
                sql = "SELECT x.*, sa.* " +
                    "FROM (" +
                    "    SELECT a.*, ROW_NUMBER() OVER (PARTITION BY a.category_english ORDER BY RAND()) AS rn " +
                    "    FROM " + dbName + ".activity a" +
                    ") x " +
                    "JOIN activity_spq sa ON x.id = sa.activity_id " +
                    "WHERE x.rn <= 3 ORDER BY x.id, sa.id;";
            } else {
                sql = "SELECT x.*, sa.* " +
                    "FROM (" +
                    "    SELECT a.*, ROW_NUMBER() OVER (PARTITION BY a.category_italian ORDER BY RAND()) AS rn " +
                    "    FROM " + dbName + ".activity a" +
                    ") x " +
                    "JOIN activity_spq sa ON x.id = sa.activity_id " +
                    "WHERE x.rn <= 3 ORDER BY x.id, sa.id;";
            }
        } else {
            sql = "WITH random_activities AS (    SELECT * FROM " + dbName + ".activity ORDER BY RAND() LIMIT 10 ) SELECT ra.*, sa.* FROM random_activities ra JOIN " + dbName + ".activity_spq sa ON ra.id = sa.activity_id ORDER BY ra.id, sa.id";
        }


        try (Connection conn = DriverManager.getConnection(url, user, pwd);
            PreparedStatement st = conn.prepareStatement(sql)) {

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                int id = rs.getInt("id");
                String titleItalian = rs.getString("title_italian");
                String titleEnglish = rs.getString("title_english");
                String descriptionItalian = rs.getString("description_italian");
                String descriptionEnglish = rs.getString("description_english");
                String categoryItalian = rs.getString("category_italian");
                String categoryEnglish = rs.getString("category_english");
                String city = rs.getString("city");
                String language = rs.getString("language");
                SpqEvaluation sqp_eval = null;
                String visionScore;
                String hearingScore;
                String smellScore;
                String tasteScore;
                String touchScore;
                String visionDescriptionItalian;
                String hearingDescriptionItalian;
                String smellDescriptionItalian;
                String tasteDescriptionItalian;
                String touchDescriptionItalian;
                String visionDescriptionEnglish;
                String hearingDescriptionEnglish;
                String smellDescriptionEnglish;
                String tasteDescriptionEnglish;
                String touchDescriptionEnglish;
                int like = 0;
                if (Objects.equals(language, "it")) {
                    visionScore = rs.getString("vision_score");
                    hearingScore = rs.getString("hearing_score");
                    smellScore = rs.getString("smell_score");
                    tasteScore = rs.getString("taste_score");
                    touchScore = rs.getString("touch_score");
                    visionDescriptionItalian = rs.getString("vision_description");
                    hearingDescriptionItalian = rs.getString("hearing_description");
                    smellDescriptionItalian = rs.getString("smell_description");
                    tasteDescriptionItalian = rs.getString("taste_description");
                    touchDescriptionItalian = rs.getString("touch_description");
                    like = rs.getInt("nr_like");
                    if (rs.next()) {
                        visionDescriptionEnglish = rs.getString("vision_description");
                        hearingDescriptionEnglish = rs.getString("hearing_description");
                        smellDescriptionEnglish = rs.getString("smell_description");
                        tasteDescriptionEnglish = rs.getString("taste_description");
                        touchDescriptionEnglish = rs.getString("touch_description");
                        sqp_eval = new SpqEvaluation(visionScore, hearingScore, smellScore, tasteScore, touchScore, visionDescriptionItalian, hearingDescriptionItalian, smellDescriptionItalian, tasteDescriptionItalian, touchDescriptionItalian, visionDescriptionEnglish, hearingDescriptionEnglish, smellDescriptionEnglish, tasteDescriptionEnglish, touchDescriptionEnglish);
                    }
                }

                activities.add(new Activity(id, titleItalian, titleEnglish, descriptionItalian, descriptionEnglish, categoryItalian, categoryEnglish, city, sqp_eval, like));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error in getActivities");
            e.printStackTrace();
        }

        return activities;
    }

    public void setCompletionCode(int userId, String completionCode) {
        String sql = "UPDATE " + dbName + ".user SET completionCode = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, completionCode);
            st.setInt(2, userId);
            int rowsUpdated = st.executeUpdate();

            if (rowsUpdated == 0) {
                System.err.println("No user found with ID: " + userId);
                throw new RuntimeException("USER_NOT_FOUND");
            }
        } catch (SQLException e) {
            System.err.println("Error during setCompletionCode, userId: " + userId + "\ncompletionCode: " + completionCode);
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<User> getRandomUsers(int count, String lang) {
        ArrayList<User> users = new ArrayList<>();
        String sql = "SELECT * FROM " + dbName + ".simulated_user " +
                    "ORDER BY RAND() LIMIT ?";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
            PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, count);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String prolificId = rs.getString("prolific_id");
                    String uniqueId = rs.getString("unique_id");

                    String spqJson = rs.getString("SPQResult");
                    SPQResult spqResult = spqJson != null ? new Gson().fromJson(spqJson, SPQResult.class) : null;

                    String bigfJson = rs.getString("BigFResult");
                    BigFResult bigfResult = bigfJson != null ? new Gson().fromJson(bigfJson, BigFResult.class) : null;

                    int attentionFailed = rs.getInt("attentionFailed");
                    String completionCode = rs.getString("completionCode");

                    users.add(new User(id, prolificId, uniqueId, spqResult, bigfResult, attentionFailed, completionCode));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getRandomUsers");
            e.printStackTrace();
        }
        return users;
    }

    public void updateLikedActivity(ArrayList<Integer> activityIds, int user_id) {
        String sqlInsert = "INSERT INTO " + dbName + ".liked_activities (id_activity, user_id) VALUES (?, ?)";
        String sqlUpdate = "UPDATE activity SET nr_like = nr_like + 1 WHERE id = ?";

        System.out.println(LocalDateTime.now() + " - updateLikedActivity: activityIds=" + activityIds + ", user_id=" + user_id);

        try (Connection conn = DriverManager.getConnection(url, user, pwd)) {
            conn.setAutoCommit(false); 

            try (PreparedStatement stInsert = conn.prepareStatement(sqlInsert);
                PreparedStatement stUpdate = conn.prepareStatement(sqlUpdate)) {

                for (Integer activityId : activityIds) {
                    try {
                        stInsert.setInt(1, activityId);
                        stInsert.setInt(2, user_id);
                        stInsert.executeUpdate();

                        System.out.println(LocalDateTime.now() + " - Like registered: user_id=" + user_id + ", activityId=" + activityId);

                        stUpdate.setInt(1, activityId);
                        int rowsUpdated = stUpdate.executeUpdate();

                        if (rowsUpdated == 0) {
                            throw new SQLException("No activity found with ID: " + activityId);
                        }

                        System.out.println(LocalDateTime.now() + " - nr_like updated for activityId=" + activityId);
                    } catch (SQLException e) {
                        if ("23000".equals(e.getSQLState())) {
                            System.err.println("Duplicate like by user: " + user_id + " for activityId: " + activityId);
                        } else {
                            System.err.println("Error processing activityId=" + activityId + ", user_id=" + user_id);
                            e.printStackTrace();
                            conn.rollback();
                            return; 
                        }
                    }
                }
                conn.commit(); 
            } catch (SQLException e) {
                conn.rollback(); 
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Error during updateLikedActivity, user_id=" + user_id);
            e.printStackTrace();
        }
    }


    public User getUserProfile(int userId) {
        String sql = "SELECT id, prolific_id, unique_id, SPQResult, BigFResult FROM " + dbName + ".user WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, userId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    String spqJson = rs.getString("SPQResult");
                    SPQResult spqResult = spqJson != null ? new Gson().fromJson(spqJson, SPQResult.class) : null;
                    String bigfJson = rs.getString("BigFResult");
                    BigFResult bigfResult = bigfJson != null ? new Gson().fromJson(bigfJson, BigFResult.class) : null;
                    return new User(rs.getInt("id"), rs.getString("prolific_id"), rs.getString("unique_id"), spqResult, bigfResult, 0, null);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void saveGroupFormation(int userId, int activityId, JsonArray rounds, JsonArray finalGroupAnswers, int attentionFailed) {
        String sqlRound = "INSERT INTO group_formation_round (user_id, activity_id, round_number, candidates, assigned_candidate_id) VALUES (?, ?, ?, ?, ?)";
        String sqlRating = "INSERT IGNORE INTO candidate_rating (user_id, activity_id, round_number, candidate_id, question_id, answer) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlFinal = "INSERT INTO final_group_answer (user_id, activity_id, question_id, answer) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE answer = VALUES(answer)";

        try (Connection conn = DriverManager.getConnection(url, user, pwd)) {
            conn.setAutoCommit(false);
            try {
                // Save rounds
                try (PreparedStatement stRound = conn.prepareStatement(sqlRound);
                     PreparedStatement stRating = conn.prepareStatement(sqlRating)) {
                    for (JsonElement roundElem : rounds) {
                        JsonObject round = roundElem.getAsJsonObject();
                        int roundNumber = round.get("roundNumber").getAsInt();
                        int assignedId = round.get("assignedCandidateId").getAsInt();
                        JsonArray candidates = round.getAsJsonArray("candidates");

                        // Build candidates summary JSON (id + type only)
                        JsonArray candidatesSummary = new JsonArray();
                        for (JsonElement ce : candidates) {
                            JsonObject cObj = ce.getAsJsonObject();
                            JsonObject summary = new JsonObject();
                            summary.addProperty("id", cObj.get("id").getAsInt());
                            summary.addProperty("type", cObj.get("type").getAsString());
                            candidatesSummary.add(summary);
                        }

                        stRound.setInt(1, userId);
                        stRound.setInt(2, activityId);
                        stRound.setInt(3, roundNumber);
                        stRound.setString(4, candidatesSummary.toString());
                        stRound.setInt(5, assignedId);
                        stRound.addBatch();

                        // Save ratings for each candidate
                        for (JsonElement ce : candidates) {
                            JsonObject cObj = ce.getAsJsonObject();
                            int candidateId = cObj.get("id").getAsInt();
                            JsonArray ratings = cObj.getAsJsonArray("ratings");
                            for (int q = 0; q < ratings.size(); q++) {
                                stRating.setInt(1, userId);
                                stRating.setInt(2, activityId);
                                stRating.setInt(3, roundNumber);
                                stRating.setInt(4, candidateId);
                                stRating.setInt(5, q + 1);
                                stRating.setInt(6, ratings.get(q).getAsInt());
                                stRating.addBatch();
                            }
                        }
                    }
                    stRound.executeBatch();
                    stRating.executeBatch();
                }

                // Save final group answers
                try (PreparedStatement stFinal = conn.prepareStatement(sqlFinal)) {
                    for (int i = 0; i < finalGroupAnswers.size(); i++) {
                        stFinal.setInt(1, userId);
                        stFinal.setInt(2, activityId);
                        stFinal.setInt(3, i + 1);
                        stFinal.setInt(4, finalGroupAnswers.get(i).getAsInt());
                        stFinal.addBatch();
                    }
                    stFinal.executeBatch();
                }

                conn.commit();

                if (attentionFailed > 0) {
                    saveActivityAttentionChecks(userId, attentionFailed);
                }

            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Failed to save group formation data", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    public ArrayList<User> getAllSimulatedUsers(String lang) {
        ArrayList<User> users = new ArrayList<>();
        String sql = "SELECT * FROM " + dbName + ".simulated_user";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
            PreparedStatement st = conn.prepareStatement(sql)) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String spqJson = rs.getString("SPQResult");
                    SPQResult spqResult = spqJson != null ? new Gson().fromJson(spqJson, SPQResult.class) : null;
                    String bigfJson = rs.getString("BigFResult");
                    BigFResult bigfResult = bigfJson != null ? new Gson().fromJson(bigfJson, BigFResult.class) : null;
                    users.add(new User(id, rs.getString("prolific_id"), rs.getString("unique_id"), spqResult, bigfResult, 0, null));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getAllSimulatedUsers");
            e.printStackTrace();
        }
        return users;
    }

    public void saveNeurotype(int userId, String neurotype) {
        String sql = "UPDATE " + dbName + ".user SET neurotype = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, neurotype);
            st.setInt(2, userId);
            int rowsUpdated = st.executeUpdate();
            if (rowsUpdated == 0) {
                System.err.println("No user found with ID: " + userId);
                throw new RuntimeException("USER_NOT_FOUND");
            }
        } catch (SQLException e) {
            System.err.println("Error during saveNeurotype, userId: " + userId);
            throw new RuntimeException("Error during saveNeurotype", e);
        }
    }

    public User checkExistedUser(int userId) {
        String sql = "SELECT id FROM user WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, userId);
            try(ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), null, null, null, null, 0, null);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}