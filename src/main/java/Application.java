import DBHelper.DBManager;
import DBHelper.DBConnectionProperties;
import Entity.Account;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
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

    public static void crudTest(DBManager connection) throws Exception {
        Account myAccount = new Account("Eric", "abcd", 10000);
        int rowsAffected;
        System.out.println("myAccount before insert is " + myAccount);
        rowsAffected = connection.insert(myAccount);
        System.out.println(rowsAffected + " records have been updated");
        System.out.println("myAccount after insert is " + myAccount);

        rowsAffected = connection.delete(myAccount);
        System.out.println(rowsAffected + " records have been deleted");

        connection.insert(myAccount);

        myAccount.setName("Adam");
        rowsAffected = connection.update(myAccount);
        System.out.println(rowsAffected + " records have been updated");

        List<Account> accounts = connection.queryForAll(Account.class);
        System.out.println("Returning accounts record " + accounts.size());
        for (Account account: accounts) {
            System.out.println(account);
        }
    }

    public static void unsafeTransactionTest(DBManager connection) throws Exception {
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

    public static void safeTransactionTest(DBManager connection) throws Exception {
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

    public static void safeTransactionWithCallableTest(DBManager connection) throws IllegalAccessException, SQLException, InvocationTargetException {
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
        DBManager connection = new DBManager(dbConnectionProperties);
                /*
        connection.createTable(Account.class);
        connection.getMetaData();

        crudTest(connection);
        unsafeTransactionTest(connection);
        safeTransactionTest(connection);
        safeTransactionWithCallableTest(connection);
        */
        storedProcedureTest(connection);
    }

    private static void storedProcedureTest(DBManager connection) throws SQLException {
        Connection rawConnection = connection.getConnection();
        CallableStatement cs = rawConnection.prepareCall("{call update_password(?, ?, ?)}");
        cs.setString(1, "Eric");
        cs.setString(2, "eeee");
        cs.registerOutParameter(3, Types.INTEGER);
        cs.execute();
        int affectedRows = cs.getInt(3);
        System.out.println("Updated password for " + affectedRows + " users");
    }
}
