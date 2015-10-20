import DBHelper.DBConnection;
import DBHelper.DBConnectionProperties;
import Entity.Account;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

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

    public static void crudTest(DBConnection connection) throws Exception {
        Account myAccount = new Account("Eric", "abcd", 10000);
        System.out.println("myAccount before insert is " + myAccount);
        connection.insert(myAccount);
        System.out.println("myAccount after insert is " + myAccount);

        connection.delete(myAccount);

        connection.insert(myAccount);

        myAccount.setName("Adam");
        connection.update(myAccount);

        List<Account> accounts = connection.queryForAll(Account.class);
        System.out.println("Account list-------------------");
        for (Account account: accounts) {
            System.out.println(account);
        }
    }

    public static void unsafeTransactionTest(DBConnection connection) throws Exception {
        Account account1 = new Account("account1", "abcd", 1000);
        Account account2 = new Account("account2", "abcd", 0);
        connection.insert(account1);
        connection.insert(account2);
        try {
            for (int i = 0; i < 10; i++) {
                account1.setNumber(account1.getNumber() - 100);
                connection.update(account1);
                account2.setNumber(account2.getNumber() + 100);
                connection.update(account2);
                if (i == 4)
                    throw new Exception("Throwing exception, now check the accounts");
            }
        } catch (Exception e) {

        }
    }

    public static void safeTransactionTest(DBConnection connection) throws Exception {
        Account account1 = new Account("account3", "abcd", 1000);
        Account account2 = new Account("account4", "abcd", 0);
        connection.insert(account1);
        connection.insert(account2);
        connection.getConnection().setAutoCommit(false);
        try {
            for (int i = 0; i < 10; i++) {
                account1.setNumber(account1.getNumber() - 100);
                connection.update(account1);
                account2.setNumber(account2.getNumber() + 100);
                connection.update(account2);
                if (i == 4)
                    throw new Exception("Throwing exception, now check the accounts");
            }
        } catch (Exception e) {
            connection.getConnection().rollback();
        } finally {
            connection.getConnection().setAutoCommit(true);
        }
    }

    public static void safeTransactionWithCallableTest(DBConnection connection) throws IllegalAccessException, SQLException, InvocationTargetException {
        Account account1 = new Account("account5", "abcd", 1000);
        Account account2 = new Account("account6", "abcd", 0);
        connection.insert(account1);
        connection.insert(account2);
        connection.executeTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                System.out.println("Callable called");
                for (int i = 0; i < 10; i++) {
                    account1.setNumber(account1.getNumber() - 100);
                    connection.update(account1);
                    account2.setNumber(account2.getNumber() + 100);
                    connection.update(account2);
                    if (i == 4)
                        throw new Exception("Throwing exception, now check the accounts");
                }
                return null;
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Properties defaultProps = getProperties();
        DBConnectionProperties dbConnectionProperties = new DBConnectionProperties(
                defaultProps.getProperty("liteorm.datasource.driverClassName"),
                defaultProps.getProperty("liteorm.datasource.username"),
                defaultProps.getProperty("liteorm.datasource.password"),
                defaultProps.getProperty("liteorm.datasource.url"));
        System.out.println("db connection properties = " + dbConnectionProperties);
        DBConnection connection = new DBConnection(dbConnectionProperties);
        connection.createTable(Account.class);

        //crudTest(connection);
        unsafeTransactionTest(connection);
        safeTransactionTest(connection);
        safeTransactionWithCallableTest(connection);
    }
}
