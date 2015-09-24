import DBHelper.DBConnection;
import DBHelper.DBConnectionProperties;
import Entity.Account;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by cuiwei on 9/24/15.
 */
public class Application {
    public static Properties getProperties() throws IOException {
        Properties defaultProps = new Properties();
        /*
        FileInputStream in = new FileInputStream("resources/application.properties");
        defaultProps.load(in);
        in.close(); */
        InputStream inputStream = Application.class.getClassLoader().getResourceAsStream("application.properties");
        defaultProps.load(inputStream);
        return defaultProps;
    }

    public static void main(String[] args) throws IOException, SQLException {
        Properties defaultProps = getProperties();
        DBConnectionProperties dbConnectionProperties = new DBConnectionProperties(
                defaultProps.getProperty("liteorm.datasource.driverClassName"),
                defaultProps.getProperty("liteorm.datasource.username"),
                defaultProps.getProperty("liteorm.datasource.password"),
                defaultProps.getProperty("liteorm.datasource.url"));
        System.out.println("db connection properties = " + dbConnectionProperties);
        DBConnection connection = new DBConnection(dbConnectionProperties);
        connection.createTable(Account.class);
        Account myAccount = new Account("Eric", "abcd");

    }
}
