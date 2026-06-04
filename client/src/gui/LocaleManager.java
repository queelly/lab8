package gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.util.*;

public class LocaleManager {

    public static final Locale LOCALE_RU    = new Locale("ru", "RU");
    public static final Locale LOCALE_CS    = new Locale("cs", "CZ");
    public static final Locale LOCALE_PL    = new Locale("pl", "PL");
    public static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    public static final Locale[] SUPPORTED_LOCALES = {
            LOCALE_RU, LOCALE_CS, LOCALE_PL, LOCALE_ES_CO
    };

    public static final String[] LOCALE_LABELS = {
            "Русский", "Čeština", "Polski", "Español (Colombia)"
    };

    private static final LocaleManager INSTANCE = new LocaleManager();

    private Locale currentLocale = LOCALE_RU;
    private ResourceBundle bundle;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private LocaleManager() {
        loadBundle();
    }

    public static LocaleManager getInstance() {
        return INSTANCE;
    }

    public void setLocale(Locale locale) {
        Locale old = this.currentLocale;
        this.currentLocale = locale;
        loadBundle();
        pcs.firePropertyChange("locale", old, locale);
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    public String get(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    public String formatNumber(Object number) {
        if (number == null) return "";
        return NumberFormat.getNumberInstance(currentLocale).format(number);
    }

    public String formatDate(Date date) {
        if (date == null) return "";
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, currentLocale).format(date);
    }

    public void addChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener("locale", listener);
    }

    public void removeChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener("locale", listener);
    }

    private void loadBundle() {
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }
}