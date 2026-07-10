package com.example.prolificstudy2;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;

@WebServlet(name = "CheckSessionServlet", value = "/checkSession")
public class CheckSessionServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp){
        System.out.println("Recived Message from get");
        HttpSession session = req.getSession(false);
        resp.setContentType("text/plain");
        try (PrintWriter out = resp.getWriter()) {
            if (session == null) {
                out.print("nok");
            } else {
                String progress = (String) session.getAttribute("progress");
                if (progress == null) {
                    progress = "undefined";
                }
                out.print(session.getAttribute("userId") + ", " + progress);
            }
        } catch (Exception e) {
            System.err.println("Exception in doPost (SPQServlet)");
            e.printStackTrace();
        }
    }
}
