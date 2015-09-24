package DBHelper;

import DBAnnotation.DatabaseTable;
import Entity.Account;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cuiwei on 9/24/15.
 */

public class DBConnection {

    private Connection mConnection;
    private static Map<Class<?>, String> ormTypeMapping = new HashMap<Class<?>, String>();
    static {
        ormTypeMapping.put(String.class, "varchar(50");
        ormTypeMapping.put(Integer.class, "integer");
    }


    public DBConnection(DBConnectionProperties dbConnectionProperties) throws SQLException {
        mConnection = DriverManager.getConnection(dbConnectionProperties.getDataSourceUrl(),
                dbConnectionProperties.getDataSourceUserName(),
                dbConnectionProperties.getDataSourcePassword());
    }

    public void createTable(Class<?> tableClass) throws SQLException {
        /*
        Annotation[] annotations = tableClass.getAnnotations();
        for (Annotation annotation: annotations) {
            System.out.println("annotation " + annotation);
        }
        for (Method m: table.getDeclaredMethods()) {
            if (m.isAnnotationPresent(table)) {
                System.out.println("Annotation found " + m);
            }
        }*/
        //String tableName = tableClass.getSimpleName();
        if (tableClass.isAnnotationPresent(DatabaseTable.class)) {
            System.out.println("table class is annotated with DatabaseTable");
            DatabaseTable tableAnnotation = (DatabaseTable)tableClass.getAnnotation(DatabaseTable.class);
            String tableName = tableAnnotation.tableName();
            String query = "create table if not exists " + tableName + " ";
            Field[] fields = tableClass.getDeclaredFields();
            int fieldsLength = fields.length;
            for (int i = 0; i < fieldsLength; i++ ) {
                Field field = fields[i];
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                System.out.println("field name = " + fieldName + " type = " + fieldType);
                query += ((i == 0) ? "(" : "") + fieldName + " " + ormTypeMapping.get(fieldType)  + ((i == fieldsLength-1) ? "));" : "),");
            }
            System.out.println("query = " + query);
            Statement stmt = null;
            stmt = mConnection.createStatement();
            stmt.executeUpdate(query);
            stmt.close();
        }
    }
}
