package com.example.prolificstudy2;


import datamodel.DAO;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

@WebServlet(name = "initializationServlet", value = "/initialization", loadOnStartup = 0)
public class InitializationServlet extends HttpServlet {
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext ctx = config.getServletContext();

        String URL = ctx.getInitParameter("DB-URL");
        String user = ctx.getInitParameter("user");
        String pwd = ctx.getInitParameter("pwd");

        DAO dao = new DAO(URL, user, pwd);
        ctx.setAttribute("dao", dao);
    }
}
