import jakarta.servlet.http.HttpSession;
public class SessionAttribute<T> {
    private final Class<T> clazz;
    private final String name;

    public SessionAttribute(Class<T> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    // Get the attribute from the session and safely cast it
    public T get(HttpSession session) {
        Object value = session.getAttribute(name);
        if (value == null) return null;
        return clazz.cast(value);  // Safe casting
    }

    // Set the attribute in the session
    public void set(HttpSession session, T value) {
        session.setAttribute(name, value);
    }
    public void remove(HttpSession session) {
        session.removeAttribute(name);
    }
}
