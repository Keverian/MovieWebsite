import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import JwtInfo.JwtUtil;
import io.jsonwebtoken.Claims;

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();
    private final ArrayList<String> allowedPaths = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        System.out.println("LoginFilter: " + httpRequest.getRequestURI());

        // Check if this URL is allowed to access without logging in
        if (this.isUrlAllowedWithoutLogin(httpRequest.getRequestURI())) {
            // Keep default action: pass along the filter chain
            chain.doFilter(request, response);
            return;
        }

        //proj 5 task 4 change to store tokens for validation
        String token = JwtUtil.getCookieValue(httpRequest, "jwtToken");
        Claims claims = JwtUtil.validateToken(token);

        // Redirect to login page if the "user" attribute doesn't exist in session
        if (claims == null) {

            //be sure to change this if we decide to change the name of our root directory
            String warFileName = request.getServletContext().getContextPath().substring(1);
            httpResponse.sendRedirect("/" + warFileName + "/login.html");
        } else {
            //proj 5 task 4 change to store tokens for validation
            //store claims in request attributes
            // down stream servlets can use claims as the session storage
            httpRequest.setAttribute("claims", claims);

            chain.doFilter(request, response);
        }
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        /*
         Setup your own rules here to allow accessing some resources without logging in
         Always allow your own login related requests(html, js, servlet, etc..)
         You might also want to allow some CSS files, etc..

         Updated to include paths
         */
        return allowedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith) ||
                allowedPaths.stream().anyMatch(requestURI.toLowerCase()::contains);
    }

    public void init(FilterConfig fConfig) {
        allowedURIs.add("login.html");
        allowedURIs.add("login.js");
        allowedURIs.add("api/login");
        allowedURIs.add("recaptcha-form");

        //for the employee login
        allowedPaths.add("_dashboard/");
    }

    public void destroy() {
        // ignored.
    }

}
